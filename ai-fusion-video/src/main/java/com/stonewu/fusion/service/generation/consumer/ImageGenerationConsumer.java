package com.stonewu.fusion.service.generation.consumer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.entity.ai.ApiConfig;
import com.stonewu.fusion.entity.generation.ImageItem;
import com.stonewu.fusion.entity.generation.ImageTask;
import com.stonewu.fusion.infrastructure.queue.RedisTaskQueue;
import com.stonewu.fusion.service.ai.AiModelService;
import com.stonewu.fusion.service.ai.ApiConfigService;
import com.stonewu.fusion.service.generation.ImageGenerationService;
import com.stonewu.fusion.service.generation.strategy.ImageGenerationStrategy;
import com.stonewu.fusion.service.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生图任务消费器
 * <p>
 * 从 Redis 队列中取出任务，通过对应的策略执行生图
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationConsumer {

    private static final String QUEUE_NAME = "image_generation";

    private final RedisTaskQueue taskQueue;
    private final ImageGenerationService imageGenerationService;
    private final AiModelService aiModelService;
    private final ApiConfigService apiConfigService;
    private final List<ImageGenerationStrategy> strategies;
    private final MediaStorageService mediaStorageService;

    private Map<String, ImageGenerationStrategy> strategyMap;

    /**
     * 获取策略映射（懒加载）
     */
    private Map<String, ImageGenerationStrategy> getStrategyMap() {
        if (strategyMap == null) {
            strategyMap = strategies.stream()
                    .collect(Collectors.toMap(ImageGenerationStrategy::getName, s -> s));
        }
        return strategyMap;
    }

    /**
     * 提交生图任务到队列
     */
    public String submitTask(ImageTask task) {
        String taskId = IdUtil.fastSimpleUUID();
        task.setTaskId(taskId);
        task.setStatus(0); // PENDING
        imageGenerationService.create(task);

        // 为每个 count 创建 ImageItem
        for (int i = 0; i < task.getCount(); i++) {
            ImageItem item = ImageItem.builder()
                    .taskId(task.getId())
                    .status(0)
                    .build();
            imageGenerationService.createItem(item);
        }

        // 入队
        taskQueue.push(QUEUE_NAME, taskId);
        log.info("[ImageConsumer] 任务入队: taskId={}", taskId);
        return taskId;
    }

    /**
     * 提交任务并同步等待结果（阻塞当前线程）
     * <p>
     * 适用于 AI Agent 工具等需要同步获取生图结果的场景。
     * 内部流程：submitTask() 入队 → Consumer 定时取出执行 → 本方法轮询 DB 等待完成。
     *
     * @param task      生图任务（需设置好 prompt、width、height、modelId 等）
     * @param timeoutMs 最大等待时间（毫秒）
     * @return 完成的 ImageTask（含结果图片在 ImageItem 中）
     * @throws RuntimeException 超时或任务失败时抛出
     * @throws InterruptedException 等待过程中线程被中断
     */
    public ImageTask submitAndWait(ImageTask task, long timeoutMs) throws InterruptedException {
        String taskId = submitTask(task);

        long deadline = System.currentTimeMillis() + timeoutMs;
        long pollInterval = 2000L;

        log.info("[ImageConsumer] 同步等待任务完成: taskId={}, timeout={}ms", taskId, timeoutMs);

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollInterval);

            ImageTask current = imageGenerationService.getByTaskId(taskId);
            switch (current.getStatus()) {
                case 2: // 已完成
                    log.info("[ImageConsumer] 任务已完成: taskId={}", taskId);
                    return current;
                case 3: // 失败
                    String errorMsg = current.getErrorMsg() != null ? current.getErrorMsg() : "未知错误";
                    throw new RuntimeException("生图任务失败: " + errorMsg);
                default:
                    // 0-排队中 1-处理中，继续等待
                    break;
            }
        }

        // 超时：标记任务失败，避免 consumer 继续执行无意义的任务
        imageGenerationService.updateStatus(task.getId(), 3, "同步等待超时");
        throw new RuntimeException("生图任务排队超时（等待 " + (timeoutMs / 1000) + " 秒），当前任务较多，请稍后重试");
    }

    /**
     * 定时消费队列
     */
    @Scheduled(fixedDelay = 3000)
    public void consume() {
        String taskId = taskQueue.acquireAndPop(QUEUE_NAME, 1);
        if (taskId == null) {
            return;
        }

        try {
            taskQueue.markRunning(QUEUE_NAME, taskId, 30);
            processTask(taskId);
        } catch (Exception e) {
            log.error("[ImageConsumer] 任务处理失败: taskId={}", taskId, e);
        } finally {
            taskQueue.markComplete(QUEUE_NAME, taskId);
            taskQueue.release(QUEUE_NAME);
        }
    }

    private void processTask(String taskId) {
        ImageTask task;
        try {
            task = imageGenerationService.getByTaskId(taskId);
        } catch (Exception e) {
            log.error("[ImageConsumer] 任务不存在: taskId={}", taskId);
            return;
        }

        // 更新状态为处理中
        imageGenerationService.updateStatus(task.getId(), 1, null);

        // 根据任务关联的模型选择策略
        Map<String, ImageGenerationStrategy> map = getStrategyMap();
        if (map.isEmpty()) {
            imageGenerationService.updateStatus(task.getId(), 3, "没有可用的图片生成策略");
            log.error("[ImageConsumer] 没有可用的图片生成策略");
            return;
        }

        // 优先按模型 code 匹配策略名，否则使用第一个策略
        ImageGenerationStrategy strategy = null;
        ApiConfig apiConfig = null;
        if (task.getModelId() != null) {
            try {
                AiModel model = aiModelService.getById(task.getModelId());
                if (model != null) {
                    strategy = map.get(model.getCode());
                    if (model.getApiConfigId() != null) {
                        apiConfig = apiConfigService.getById(model.getApiConfigId());
                        if (strategy == null && apiConfig != null) {
                            strategy = map.get(apiConfig.getPlatform());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[ImageConsumer] 模型配置获取失败: modelId={}", task.getModelId());
            }
        }
        if (strategy == null) {
            strategy = map.values().iterator().next();
        }
        if (apiConfig == null) {
            List<ApiConfig> configs = apiConfigService.getEnabledList();
            for (ApiConfig cfg : configs) {
                if (strategy.getName().equals(cfg.getPlatform())) {
                    apiConfig = cfg;
                    break;
                }
            }
        }
        if (apiConfig == null) {
            imageGenerationService.updateStatus(task.getId(), 3, "找不到匹配的 API 配置");
            log.error("[ImageConsumer] 找不到匹配的 API 配置: strategy={}", strategy.getName());
            return;
        }

        try {
            String platformTaskId = strategy.submit(task, apiConfig);
            log.info("[ImageConsumer] 任务已提交到平台: taskId={}, platformTaskId={}", taskId, platformTaskId);

            // 轮询结果
            strategy.poll(platformTaskId, task, apiConfig);

            // 持久化远程图片到本地/OSS 存储
            persistImageItems(task);

            // 更新为完成
            imageGenerationService.updateStatus(task.getId(), 2, null);
        } catch (Exception e) {
            log.error("[ImageConsumer] 任务执行失败: taskId={}", taskId, e);
            imageGenerationService.updateStatus(task.getId(), 3, e.getMessage());
        }
    }

    /**
     * 将远程图片 URL 下载到持久化存储（本地磁盘 / OSS），
     * 并替换 ImageItem 中的 URL 为永久可访问地址。
     */
    private void persistImageItems(ImageTask task) {
        List<ImageItem> items = imageGenerationService.listItems(task.getId());
        for (ImageItem item : items) {
            if (StrUtil.isNotBlank(item.getImageUrl())) {
                try {
                    String persistedUrl = mediaStorageService.downloadAndStore(item.getImageUrl(), "images");
                    item.setImageUrl(persistedUrl);
                    imageGenerationService.updateItem(item);
                    log.info("[ImageConsumer] 图片已持久化: {} -> {}", item.getId(), persistedUrl);
                } catch (Exception e) {
                    log.warn("[ImageConsumer] 图片持久化失败（保留原始 URL）: itemId={}, error={}",
                            item.getId(), e.getMessage());
                }
            }
        }
    }
}
