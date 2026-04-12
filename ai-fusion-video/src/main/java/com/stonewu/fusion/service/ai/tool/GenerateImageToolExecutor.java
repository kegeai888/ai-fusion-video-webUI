package com.stonewu.fusion.service.ai.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.entity.generation.ImageItem;
import com.stonewu.fusion.entity.generation.ImageTask;
import com.stonewu.fusion.service.ai.AiModelService;
import com.stonewu.fusion.service.ai.ToolExecutionContext;
import com.stonewu.fusion.service.ai.ToolExecutor;
import com.stonewu.fusion.service.generation.ImageGenerationService;
import com.stonewu.fusion.service.generation.consumer.ImageGenerationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 生图工具（generate_image）
 * <p>
 * 职责：解析参数 → 构建 ImageTask → 提交到队列并同步等待结果。
 * <p>
 * 排队、并发控制、策略路由等全部由 {@link ImageGenerationConsumer} 统一处理，
 * 本工具通过 {@link ImageGenerationConsumer#submitAndWait} 复用其完整流程。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateImageToolExecutor implements ToolExecutor {

    /** 模型类型常量：图片生成 */
    private static final int MODEL_TYPE_IMAGE = 2;

    /** 同步等待超时时间（5 分钟） */
    private static final long WAIT_TIMEOUT_MS = 5 * 60 * 1000L;

    private final AiModelService aiModelService;
    private final ImageGenerationService imageGenerationService;
    private final ImageGenerationConsumer imageGenerationConsumer;

    @Override
    public String getToolName() {
        return "generate_image";
    }

    @Override
    public String getDisplayName() {
        return "AI 生成图片";
    }

    @Override
    public String getToolDescription() {
        return """
                生成AI图片。此工具仅负责生图，不会自动保存到资产库。

                适用场景：
                1. 为角色生成立绘：根据角色的外貌、性格描述生成设定图
                2. 为分镜生成画面：根据分镜的场景、内容描述生成画面图
                3. 生成场景图、道具图等创意素材

                重要提示：
                - 生成完成后，如需将图片保存到角色/场景/道具等资产中，请使用 update_asset_image 工具
                - 提示词要详细具体，包含画面的主体、风格、视角等信息
                - 可以使用中文提示词，系统会自动处理
                """;
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "prompt": { "type": "string", "description": "图片生成提示词（英文效果更佳）" },
                        "negativePrompt": { "type": "string", "description": "反向提示词，描述不希望出现的内容" },
                        "width": { "type": "number", "description": "图片宽度（默认使用模型配置中的默认宽度）" },
                        "height": { "type": "number", "description": "图片高度（默认使用模型配置中的默认高度）" },
                        "style": { "type": "string", "description": "风格（如 realistic, anime, watercolor 等）" },
                        "imageUrls": {
                            "type": "array",
                            "items": { "type": "string" },
                            "description": "参考图片 URL 列表（用于图生图，文生图时不传）"
                        }
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

            int width = params.getInt("width", 0);
            int height = params.getInt("height", 0);

            // 参考图片（图生图）
            String refImageUrls = null;
            if (params.containsKey("imageUrls")) {
                List<String> imageUrls = params.getJSONArray("imageUrls").toList(String.class);
                if (!imageUrls.isEmpty()) {
                    refImageUrls = JSONUtil.toJsonStr(imageUrls);
                }
            }

            // 获取默认图片生成模型的 ID（供 Consumer 处理时使用）
            Long modelId = resolveDefaultModelId();

            // 构建生图任务
            ImageTask task = ImageTask.builder()
                    .prompt(prompt)
                    .width(width > 0 ? width : null)
                    .height(height > 0 ? height : null)
                    .refImageUrls(refImageUrls)
                    .modelId(modelId)
                    .count(1)
                    .userId(context.getUserId())
                    .build();

            log.info("[generate_image] 提交生图任务: prompt={}, size={}x{}, modelId={}, 参考图: {}",
                    prompt, width, height, modelId, refImageUrls != null ? "有" : "无");

            // 提交到队列并同步等待结果
            ImageTask completed = imageGenerationConsumer.submitAndWait(task, WAIT_TIMEOUT_MS);

            // 从完成的任务中获取生成的图片 URL
            List<ImageItem> items = imageGenerationService.listItems(completed.getId());
            String imageUrl = items.stream()
                    .filter(item -> StrUtil.isNotBlank(item.getImageUrl()))
                    .map(ImageItem::getImageUrl)
                    .findFirst()
                    .orElse(null);

            if (imageUrl == null) {
                return errorResult("生成完成但未获取到图片 URL");
            }

            log.info("[generate_image] 生成成功: url={}", imageUrl);

            return JSONUtil.createObj()
                    .set("status", "success")
                    .set("imageUrl", imageUrl)
                    .set("prompt", prompt)
                    .toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResult("生成任务被中断");
        } catch (Exception e) {
            log.error("[generate_image] 生成图片失败", e);
            return errorResult("生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认图片生成模型的 ID
     */
    private Long resolveDefaultModelId() {
        AiModel defaultModel = aiModelService.getDefaultByType(MODEL_TYPE_IMAGE);
        if (defaultModel != null) {
            return defaultModel.getId();
        }
        List<AiModel> imageModels = aiModelService.getListByType(MODEL_TYPE_IMAGE);
        if (!imageModels.isEmpty()) {
            return imageModels.get(0).getId();
        }
        return null;
    }

    private String errorResult(String message) {
        return JSONUtil.createObj().set("status", "error").set("message", message).toString();
    }
}
