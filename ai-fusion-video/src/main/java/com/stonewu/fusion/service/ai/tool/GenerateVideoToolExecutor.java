package com.stonewu.fusion.service.ai.tool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.entity.generation.VideoItem;
import com.stonewu.fusion.entity.generation.VideoTask;
import com.stonewu.fusion.service.ai.AiModelService;
import com.stonewu.fusion.service.ai.ToolExecutionContext;
import com.stonewu.fusion.service.ai.ToolExecutor;
import com.stonewu.fusion.service.generation.VideoGenerationService;
import com.stonewu.fusion.service.generation.consumer.VideoGenerationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 生视频工具（generate_video）
 * <p>
 * 职责：解析参数 → 构建 VideoTask → 提交到队列并同步等待结果。
 * <p>
 * 排队、并发控制、策略路由等全部由 {@link VideoGenerationConsumer} 统一处理，
 * 本工具通过 {@link VideoGenerationConsumer#submitAndWait} 复用其完整流程。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateVideoToolExecutor implements ToolExecutor {

    /** 模型类型常量：视频生成 */
    private static final int MODEL_TYPE_VIDEO = 3;

    /** 同步等待超时时间（10 分钟，视频生成耗时较长） */
    private static final long WAIT_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AiModelService aiModelService;
    private final VideoGenerationService videoGenerationService;
    private final VideoGenerationConsumer videoGenerationConsumer;

    @Override
    public String getToolName() {
        return "generate_video";
    }

    @Override
    public String getDisplayName() {
        return "AI 生成视频";
    }

    @Override
    public String getToolDescription() {
        return """
                生成AI视频。根据提示词和可选的参考图片生成视频片段。

                适用场景：
                1. 为分镜镜头生成视频：根据画面描述、运镜指令和参考首帧图生成视频
                2. 文本生成视频：纯文字描述生成视频
                3. 多模态参考生视频：传入角色/道具/场景的参考图片、参考视频、参考音频，提升画面中人物和物品的一致性

                重要提示：
                - 提示词应详细描述画面内容、运镜方式、环境氛围
                - 提供 firstFrameImageUrl 可大幅提升画面一致性
                - 提供 referenceImageUrls 可锁定角色形象、道具外观等（Seedance 2.0 多模态参考特性）
                - 提供 referenceVideoUrls 可参考视频的动作、运镜、特效等
                - 提供 referenceAudioUrls 可参考音色、音乐旋律、对话内容等
                - 使用多模态参考时，提示词中用`图片1`/`视频1`/`音频1`等指代对应的参考素材
                - 生成耗时较长（通常 1-5 分钟），请耐心等待
                """;
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "prompt": { "type": "string", "description": "视频生成提示词，描述画面内容和运镜方式" },
                        "firstFrameImageUrl": { "type": "string", "description": "首帧参考图片URL（图生视频模式，强烈建议提供）" },
                        "lastFrameImageUrl": { "type": "string", "description": "尾帧参考图片URL（可选）" },
                        "referenceImageUrls": {
                            "type": "array",
                            "items": { "type": "string" },
                            "description": "多模态参考图片URL列表（Seedance 2.0 特性）。用于传入角色形象、道具外观、场景参考等图片，提升视频中人物和物品的一致性。提示词中按上传顺序用`图片1`、`图片2`...`图片N`指代，最多9张"
                        },
                        "referenceVideoUrls": {
                            "type": "array",
                            "items": { "type": "string" },
                            "description": "参考视频URL列表（Seedance 2.0 特性）。用于参考视频的动作表现、运镜方式、特效风格等。提示词中用`视频1`、`视频2`指代，最多3个"
                        },
                        "referenceAudioUrls": {
                            "type": "array",
                            "items": { "type": "string" },
                            "description": "参考音频URL列表（Seedance 2.0 特性）。用于参考音色、音乐旋律、对话内容等。提示词中用`音频1`、`音频2`指代，最多3个"
                        },
                        "ratio": { "type": "string", "description": "画面比例，如 16:9、9:16、1:1（默认 16:9）" },
                        "duration": { "type": "integer", "description": "视频时长（秒），默认 5" },
                        "cameraFixed": { "type": "boolean", "description": "是否固定镜头（不做运动），默认 false" }
                    },
                    "required": ["prompt"]
                }
                """;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String execute(String toolInput, ToolExecutionContext context) {
        try {
            JSONObject params = JSONUtil.parseObj(toolInput);
            String prompt = params.getStr("prompt");
            if (StrUtil.isBlank(prompt)) {
                return errorResult("缺少 prompt");
            }

            String firstFrameImageUrl = params.getStr("firstFrameImageUrl");
            String lastFrameImageUrl = params.getStr("lastFrameImageUrl");
            String ratio = params.getStr("ratio", "16:9");
            Integer duration = params.getInt("duration", 5);
            Boolean cameraFixed = params.getBool("cameraFixed", false);

            // 解析多模态参考图片列表
            List<String> referenceImageUrlList = new ArrayList<>();
            cn.hutool.json.JSONArray refImagesArr = params.getJSONArray("referenceImageUrls");
            if (refImagesArr != null) {
                for (int i = 0; i < refImagesArr.size(); i++) {
                    String url = refImagesArr.getStr(i);
                    if (StrUtil.isNotBlank(url)) {
                        referenceImageUrlList.add(url);
                    }
                }
            }
            String referenceImageUrls = CollUtil.isEmpty(referenceImageUrlList)
                    ? null : JSONUtil.toJsonStr(referenceImageUrlList);

            // 解析参考视频列表
            String referenceVideoUrls = parseUrlArray(params, "referenceVideoUrls");

            // 解析参考音频列表
            String referenceAudioUrls = parseUrlArray(params, "referenceAudioUrls");

            // 确定生成模式
            String generateMode = StrUtil.isNotBlank(firstFrameImageUrl) ? "image2video" : "text2video";

            // 获取默认视频生成模型的 ID
            Long modelId = resolveDefaultModelId();

            // 构建生视频任务
            VideoTask task = VideoTask.builder()
                    .prompt(prompt)
                    .generateMode(generateMode)
                    .firstFrameImageUrl(firstFrameImageUrl)
                    .lastFrameImageUrl(lastFrameImageUrl)
                    .referenceImageUrls(referenceImageUrls)
                    .referenceVideoUrls(referenceVideoUrls)
                    .referenceAudioUrls(referenceAudioUrls)
                    .ratio(ratio)
                    .duration(duration)
                    .cameraFixed(cameraFixed)
                    .modelId(modelId)
                    .count(1)
                    .userId(context.getUserId())
                    .build();

            log.info("[generate_video] 提交生视频任务: prompt={}, mode={}, ratio={}, duration={}s, modelId={}, 首帧: {}, 参考图: {}张",
                    StrUtil.sub(prompt, 0, 80), generateMode, ratio, duration, modelId,
                    firstFrameImageUrl != null ? "有" : "无",
                    referenceImageUrlList.size());

            // 提交到队列并同步等待结果
            VideoTask completed = videoGenerationConsumer.submitAndWait(task, WAIT_TIMEOUT_MS);

            // 从完成的任务中获取生成的视频 URL
            List<VideoItem> items = videoGenerationService.listItems(completed.getId());
            VideoItem videoItem = items.stream()
                    .filter(item -> StrUtil.isNotBlank(item.getVideoUrl()))
                    .findFirst()
                    .orElse(null);

            if (videoItem == null) {
                return errorResult("生成完成但未获取到视频 URL");
            }

            log.info("[generate_video] 生成成功: videoUrl={}, coverUrl={}",
                    videoItem.getVideoUrl(), videoItem.getCoverUrl());

            return JSONUtil.createObj()
                    .set("status", "success")
                    .set("videoUrl", videoItem.getVideoUrl())
                    .set("coverUrl", videoItem.getCoverUrl())
                    .set("duration", videoItem.getDuration())
                    .set("prompt", prompt)
                    .toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResult("生成任务被中断");
        } catch (Exception e) {
            log.error("[generate_video] 生成视频失败", e);
            return errorResult("生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认视频生成模型的 ID
     */
    private Long resolveDefaultModelId() {
        AiModel defaultModel = aiModelService.getDefaultByType(MODEL_TYPE_VIDEO);
        if (defaultModel != null) {
            return defaultModel.getId();
        }
        List<AiModel> videoModels = aiModelService.getListByType(MODEL_TYPE_VIDEO);
        if (!videoModels.isEmpty()) {
            return videoModels.get(0).getId();
        }
        return null;
    }

    private String errorResult(String message) {
        return JSONUtil.createObj().set("status", "error").set("message", message).toString();
    }

    /**
     * 从参数中解析 URL 数组字段，返回 JSON 字符串或 null
     */
    private String parseUrlArray(JSONObject params, String fieldName) {
        cn.hutool.json.JSONArray arr = params.getJSONArray(fieldName);
        if (arr == null || arr.isEmpty()) {
            return null;
        }
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String url = arr.getStr(i);
            if (StrUtil.isNotBlank(url)) {
                urls.add(url);
            }
        }
        return CollUtil.isEmpty(urls) ? null : JSONUtil.toJsonStr(urls);
    }
}
