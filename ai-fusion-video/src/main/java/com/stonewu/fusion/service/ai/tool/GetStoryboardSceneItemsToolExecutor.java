package com.stonewu.fusion.service.ai.tool;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stonewu.fusion.entity.storyboard.StoryboardItem;
import com.stonewu.fusion.entity.storyboard.StoryboardScene;
import com.stonewu.fusion.service.ai.ToolExecutionContext;
import com.stonewu.fusion.service.ai.ToolExecutor;
import com.stonewu.fusion.service.storyboard.StoryboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询分镜场次镜头列表工具（get_storyboard_scene_items）
 * <p>
 * 返回指定场次下的所有镜头详情，含完整的镜头信息（画面内容、运镜、对白、图片、视频等）。
 * 也支持通过 storyboardItemId 查询该镜头所在场次的所有镜头（用于获取上下文）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetStoryboardSceneItemsToolExecutor implements ToolExecutor {

    private final StoryboardService storyboardService;

    @Override
    public String getToolName() {
        return "get_storyboard_scene_items";
    }

    @Override
    public String getDisplayName() {
        return "查询场次镜头列表";
    }

    @Override
    public String getToolDescription() {
        return """
                查询分镜场次下的所有镜头详情。支持两种查询方式：
                1. 通过 sceneId 直接查询场次下的所有镜头
                2. 通过 storyboardItemId 查询该镜头所在场次的所有镜头（自动定位场次）

                返回的每个镜头包含完整信息：画面内容、景别、运镜、对白、音效、图片URL、视频URL等。
                可用于获取上下文信息（上一个/下一个镜头），以便生成连贯的视频提示词。
                """;
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "sceneId": {
                            "type": "integer",
                            "description": "分镜场次ID，直接查询该场次的所有镜头"
                        },
                        "storyboardItemId": {
                            "type": "integer",
                            "description": "分镜条目ID，自动找到所在场次并返回该场次所有镜头"
                        }
                    }
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
            Long sceneId = params.getLong("sceneId");
            Long storyboardItemId = params.getLong("storyboardItemId");

            if (sceneId == null && storyboardItemId == null) {
                return errorResult("请提供 sceneId 或 storyboardItemId");
            }

            // 如果提供了 storyboardItemId，先找到所在的场次
            Long targetItemId = storyboardItemId;
            if (sceneId == null) {
                StoryboardItem targetItem = storyboardService.getItemById(storyboardItemId);
                sceneId = targetItem.getStoryboardSceneId();
                if (sceneId == null) {
                    return errorResult("该分镜条目没有关联场次");
                }
            }

            // 查询场次信息
            StoryboardScene scene = storyboardService.getSceneById(sceneId);

            // 查询该场次下的所有镜头
            List<StoryboardItem> items = storyboardService.listItemsByScene(sceneId);

            JSONArray itemList = new JSONArray();
            for (StoryboardItem item : items) {
                JSONObject itemObj = JSONUtil.createObj()
                        .set("id", item.getId())
                        .set("shotNumber", item.getShotNumber())
                        .set("autoShotNumber", item.getAutoShotNumber())
                        .set("sortOrder", item.getSortOrder())
                        .set("shotType", item.getShotType())
                        .set("content", item.getContent())
                        .set("sceneExpectation", item.getSceneExpectation())
                        .set("dialogue", item.getDialogue())
                        .set("sound", item.getSound())
                        .set("soundEffect", item.getSoundEffect())
                        .set("music", item.getMusic())
                        .set("duration", item.getDuration())
                        .set("cameraMovement", item.getCameraMovement())
                        .set("cameraAngle", item.getCameraAngle())
                        .set("cameraEquipment", item.getCameraEquipment())
                        .set("focalLength", item.getFocalLength())
                        .set("transition", item.getTransition())
                        .set("imageUrl", item.getImageUrl())
                        .set("generatedImageUrl", item.getGeneratedImageUrl())
                        .set("videoUrl", item.getVideoUrl())
                        .set("generatedVideoUrl", item.getGeneratedVideoUrl())
                        .set("videoPrompt", item.getVideoPrompt())
                        .set("characterIds", item.getCharacterIds())
                        .set("sceneAssetItemId", item.getSceneAssetItemId())
                        .set("propIds", item.getPropIds())
                        .set("remark", item.getRemark());

                // 标记当前目标镜头
                if (targetItemId != null && targetItemId.equals(item.getId())) {
                    itemObj.set("isCurrentTarget", true);
                }

                itemList.add(itemObj);
            }

            return JSONUtil.createObj()
                    .set("sceneId", scene.getId())
                    .set("sceneName", scene.getSceneHeading())
                    .set("totalItems", items.size())
                    .set("items", itemList)
                    .toString();

        } catch (Exception e) {
            log.error("[get_storyboard_scene_items] 查询失败", e);
            return errorResult("查询失败: " + e.getMessage());
        }
    }

    private String errorResult(String message) {
        return JSONUtil.createObj().set("status", "error").set("message", message).toString();
    }
}
