package com.stonewu.fusion.service.ai.agentscope;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.stonewu.fusion.common.BusinessException;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.entity.ai.ApiConfig;
import com.stonewu.fusion.service.ai.ApiConfigService;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentScope 模型适配工厂
 * <p>
 * 从数据库 AiModel 配置动态创建 AgentScope 的 Model 实例。
 * 与现有 ChatModelFactory 并行存在，两者使用不同的模型体系：
 * - ChatModelFactory → Spring AI ChatModel
 * - AgentScopeModelFactory → AgentScope Model
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentScopeModelFactory {

    private final ApiConfigService apiConfigService;

    /** 缓存已创建的 AgentScope Model 实例（按 AiModel.id 缓存） */
    private final Map<Long, Model> modelCache = new ConcurrentHashMap<>();

    /**
     * 根据 AiModel 配置创建或返回缓存的 AgentScope Model
     */
    public Model getOrCreate(AiModel model) {
        return modelCache.computeIfAbsent(model.getId(), id -> createModel(model));
    }

    public void evict(Long modelId) {
        modelCache.remove(modelId);
    }

    public void evictAll() {
        modelCache.clear();
    }

    // ========== 核心分发 ==========

    private Model createModel(AiModel model) {
        Map<String, Object> config = parseConfig(model);
        ApiConfig apiConfig = resolveApiConfig(model);
        String apiKey = resolveApiKey(apiConfig, config);
        String baseUrl = resolveBaseUrl(apiConfig, config);
        String platform = resolvePlatform(apiConfig, baseUrl);
        String modelName = getStr(config, "modelName", model.getCode());

        log.info("[AgentScopeModelFactory] 创建模型: name={}, platform={}, code={}",
                model.getName(), platform, modelName);

        return switch (platform.toLowerCase()) {
            case "dashscope" -> createDashScope(apiKey, modelName, config);
            case "openai_compatible", "openai", "deepseek", "zhipu", "moonshot", "volcengine", "siliconflow" ->
                createOpenAiCompatible(apiKey, baseUrl, modelName, platform);
            case "ollama" -> createOllama(baseUrl, modelName);
            case "anthropic" -> createAnthropic(apiKey, modelName);
            case "vertex_ai", "vertexai", "gemini" -> createGemini(apiKey, modelName, config);
            default -> throw new BusinessException("不支持的模型平台: " + platform);
        };
    }

    // ========== DashScope ==========

    private Model createDashScope(String apiKey, String modelName, Map<String, Object> config) {
        requireApiKey(apiKey, "DashScope");
        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true);

        if (Boolean.TRUE.equals(config.get("enableThinking"))) {
            builder.enableThinking(true);
        }

        return builder.build();
    }

    // ========== OpenAI Compatible ==========

    private Model createOpenAiCompatible(String apiKey, String baseUrl, String modelName, String platform) {
        requireApiKey(apiKey, "OpenAI Compatible (" + platform + ")");
        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = inferBaseUrl(platform);
        }

        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true);
        if (StrUtil.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    // ========== Ollama ==========

    private Model createOllama(String baseUrl, String modelName) {
        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = "http://localhost:11434";
        }
        return OllamaChatModel.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

    // ========== Anthropic ==========

    private Model createAnthropic(String apiKey, String modelName) {
        requireApiKey(apiKey, "Anthropic");
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true)
                .build();
    }

    // ========== Gemini ==========

    private Model createGemini(String apiKey, String modelName, Map<String, Object> config) {
        GeminiChatModel.Builder builder = GeminiChatModel.builder()
                .modelName(modelName)
                .streamEnabled(true);

        String projectId = getStr(config, "projectId", null);
        if (StrUtil.isNotBlank(projectId)) {
            builder.project(projectId)
                    .location(getStr(config, "location", "us-central1"))
                    .vertexAI(true);
        } else {
            requireApiKey(apiKey, "Gemini");
            builder.apiKey(apiKey);
        }
        return builder.build();
    }

    // ========== 工具方法 ==========

    private String inferBaseUrl(String platform) {
        return switch (platform.toLowerCase()) {
            case "deepseek" -> "https://api.deepseek.com";
            case "zhipu" -> "https://open.bigmodel.cn/api/paas/v4";
            case "volcengine" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "moonshot" -> "https://api.moonshot.cn/v1";
            case "siliconflow" -> "https://api.siliconflow.cn/v1";
            case "openai" -> "https://api.openai.com";
            default -> null;
        };
    }

    private Map<String, Object> parseConfig(AiModel model) {
        String json = model.getConfig();
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("[AgentScopeModelFactory] 配置JSON解析失败: modelId={}", model.getId(), e);
            return Map.of();
        }
    }

    private ApiConfig resolveApiConfig(AiModel model) {
        if (model.getApiConfigId() == null) {
            return null;
        }
        try {
            return apiConfigService.getById(model.getApiConfigId());
        } catch (Exception e) {
            log.warn("[AgentScopeModelFactory] ApiConfig 获取失败: {}", model.getApiConfigId());
            return null;
        }
    }

    private String resolveApiKey(ApiConfig apiConfig, Map<String, Object> config) {
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getApiKey())) {
            return apiConfig.getApiKey();
        }
        return getStr(config, "apiKey", null);
    }

    private String resolveBaseUrl(ApiConfig apiConfig, Map<String, Object> config) {
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getApiUrl())) {
            return apiConfig.getApiUrl();
        }
        return getStr(config, "baseUrl", null);
    }

    private String resolvePlatform(ApiConfig apiConfig, String baseUrl) {
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getPlatform())) {
            return apiConfig.getPlatform();
        }
        if (StrUtil.isNotBlank(baseUrl)) {
            String url = baseUrl.toLowerCase();
            if (url.contains("deepseek"))
                return "deepseek";
            if (url.contains("dashscope") || url.contains("aliyuncs"))
                return "dashscope";
            if (url.contains("bigmodel.cn"))
                return "zhipu";
            if (url.contains("volces.com") || url.contains("volcengine"))
                return "volcengine";
            if (url.contains("moonshot"))
                return "moonshot";
            if (url.contains("siliconflow"))
                return "siliconflow";
            if (url.contains("anthropic"))
                return "anthropic";
            if (url.contains("localhost") || url.contains("127.0.0.1"))
                return "ollama";
            if (url.contains("openai.com"))
                return "openai";
            return "openai_compatible";
        }
        return "openai_compatible";
    }

    private void requireApiKey(String apiKey, String platformName) {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(platformName + " 模型缺少 apiKey 配置");
        }
    }

    private String getStr(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
