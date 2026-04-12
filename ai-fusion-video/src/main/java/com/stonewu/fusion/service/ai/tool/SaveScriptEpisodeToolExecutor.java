package com.stonewu.fusion.service.ai.tool;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stonewu.fusion.entity.script.ScriptEpisode;
import com.stonewu.fusion.service.ai.ToolExecutionContext;
import com.stonewu.fusion.service.ai.ToolExecutor;
import com.stonewu.fusion.service.script.ScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 保存集记录工具（save_episode）
 * <p>
 * 创建/更新集记录（标题、概述、原文），自动计算 sort_order。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaveScriptEpisodeToolExecutor implements ToolExecutor {

    private final ScriptService scriptService;

    @Override
    public String getToolName() {
        return "save_script_episode";
    }

    @Override
    public String getDisplayName() {
        return "保存集记录";
    }

    @Override
    public String getToolDescription() {
        return """
                创建或更新剧本的一集记录。如果该集数已存在则更新，不存在则创建。
                工具内部自动计算排序序号，无需显式传入。
                返回值中包含 episode_version，请在后续调用 save_scene_items 时传入此值。
                """;
    }

    @Override
    public String getParametersSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "scriptId": {
                            "type": "number",
                            "description": "剧本ID"
                        },
                        "episodeNumber": {
                            "type": "number",
                            "description": "集数编号（如 1, 2, 3）"
                        },
                        "title": {
                            "type": "string",
                            "description": "集标题（如 '第一集' 或 '第一集：夜叩门'）"
                        },
                        "synopsis": {
                            "type": "string",
                            "description": "本集剧情概述（100-200字）"
                        },
                        "rawContent": {
                            "type": "string",
                            "description": "本集原始剧本文本"
                        },
                        "sourceType": {
                            "type": "number",
                            "description": "内容来源：0-手动 1-AI创作 2-文本解析"
                        }
                    },
                    "required": ["scriptId", "episodeNumber", "title"]
                }
                """;
    }

    @Override
    public String execute(String toolInput, ToolExecutionContext context) {
        try {
            JSONObject params = JSONUtil.parseObj(toolInput);
            Long scriptId = params.getLong("scriptId");
            Integer episodeNumber = params.getInt("episodeNumber");
            String title = params.getStr("title");
            String synopsis = params.getStr("synopsis");
            String rawContent = params.getStr("rawContent");
            Integer sourceType = params.getInt("sourceType");

            if (scriptId == null || episodeNumber == null) {
                return JSONUtil.createObj().set("status", "error")
                        .set("message", "缺少必要参数: scriptId, episodeNumber").toString();
            }

            ScriptEpisode episode = scriptService.saveEpisode(scriptId, episodeNumber, title,
                    synopsis, rawContent, sourceType);

            JSONObject resultObj = JSONUtil.createObj()
                    .set("episodeId", episode.getId())
                    .set("episodeNumber", episodeNumber)
                    .set("title", title)
                    .set("episode_version", episode.getVersion())
                    .set("message", String.format("第%d集 \"%s\" 保存成功", episodeNumber, title));

            return resultObj.toString();
        } catch (Exception e) {
            log.error("保存集记录失败", e);
            return JSONUtil.createObj().set("status", "error").set("message", "保存失败: " + e.getMessage()).toString();
        }
    }
}
