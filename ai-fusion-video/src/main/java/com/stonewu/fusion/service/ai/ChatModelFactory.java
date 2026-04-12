package com.stonewu.fusion.service.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.google.cloud.vertexai.VertexAI;
import com.stonewu.fusion.common.BusinessException;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.entity.ai.ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ChatModel 动态工厂
 * <p>
 * 从数据库 AiModel 配置动态创建 ChatModel 实例。
 * <p>
 * 支持平台(platform字段值):
 * <ul>
 * <li><b>dashscope</b> — 阿里云通义系列（通义千问等）</li>
 * <li><b>openai_compatible</b> — 兼容 OpenAI API
 * 的平台（DeepSeek/智谱/豆包/Moonshot/硅基流动/OpenAI 等）</li>
 * <li><b>ollama</b> — 本地部署的 Ollama 模型</li>
 * <li><b>anthropic</b> — Anthropic Claude 系列</li>
 * <li><b>vertex_ai</b> — Google Vertex AI Gemini 系列</li>
 * </ul>
 *
 * <h3>AiModel.config JSON 示例：</h3>
 * 
 * <pre>{@code
 * // dashscope:
 * { "apiKey": "sk-xxx", "modelName": "qwen-max" }
 *
 * // openai_compatible (DeepSeek/智谱/豆包/Moonshot/硅基流动/OpenAI):
 * { "apiKey": "sk-xxx", "baseUrl": "https://api.deepseek.com", "modelName": "deepseek-chat" }
 *
 * // ollama:
 * { "baseUrl": "http://localhost:11434", "modelName": "llama3" }
 *
 * // anthropic:
 * { "apiKey": "sk-ant-xxx", "modelName": "claude-sonnet-4-20250514" }
 *
 * // vertex_ai (需要配置 Google Cloud 凭证):
 * { "projectId": "my-gcp-project", "location": "us-central1", "modelName": "gemini-2.5-pro" }
 * }</pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatModelFactory {

    private final ApiConfigService apiConfigService;

    /**
     * 缓存已创建的 ChatModel 实例（按 AiModel.id 缓存）
     * <p>
     * 修改模型配置后需调用 {@link #evict(Long)} 清除缓存
     */
    private final Map<Long, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 根据 AiModel 配置创建或返回缓存的 ChatModel
     */
    public ChatModel getOrCreate(AiModel model) {
        return modelCache.computeIfAbsent(model.getId(), id -> createChatModel(model));
    }

    /**
     * 清除指定模型缓存
     */
    public void evict(Long modelId) {
        modelCache.remove(modelId);
    }

    /**
     * 清除全部缓存
     */
    public void evictAll() {
        modelCache.clear();
    }

    // ========== 核心分发 ==========

    private ChatModel createChatModel(AiModel model) {
        Map<String, Object> config = parseConfig(model);

        // 1. 从 ApiConfig 获取厂商配置（platform、apiUrl、apiKey）
        ApiConfig apiConfig = resolveApiConfig(model);
        String apiKey = resolveApiKey(apiConfig, config);
        String baseUrl = resolveBaseUrl(apiConfig, config);
        String platform = resolvePlatform(apiConfig, baseUrl);

        log.info("[ChatModelFactory] 创建 ChatModel: name={}, platform={}, code={}, baseUrl={}",
                model.getName(), platform, model.getCode(), baseUrl);

        String modelName = getStr(config, "modelName", model.getCode());

        return switch (platform.toLowerCase()) {
            case "dashscope" -> createDashScope(apiKey, modelName, config);
            case "openai_compatible", "openai", "deepseek", "zhipu", "moonshot", "volcengine", "siliconflow" ->
                createOpenAiCompatible(apiKey, baseUrl, modelName, platform, config);
            case "ollama" -> createOllama(baseUrl, modelName, config);
            case "anthropic" -> createAnthropic(apiKey, modelName, config);
            case "vertex_ai", "vertexai", "gemini" -> createVertexAi(config, modelName);
            default -> throw new BusinessException("不支持的模型平台: " + platform);
        };
    }

    // ========== DashScope (通义千问系列) ==========

    private ChatModel createDashScope(String apiKey, String modelName, Map<String, Object> config) {
        requireApiKey(apiKey, "DashScope");

        DashScopeApi api = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(modelName)
                .build();

        applyIfPresent(config, "temperature", v -> options.setTemperature(toDouble(v)));
        applyIfPresent(config, "topP", v -> options.setTopP(toDouble(v)));
        applyIfPresent(config, "maxTokens", v -> options.setMaxTokens(toInt(v)));

        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();
    }

    // ========== OpenAI Compatible (DeepSeek/智谱/豆包/Moonshot/硅基流动/OpenAI) ==========

    private ChatModel createOpenAiCompatible(String apiKey, String baseUrl, String modelName,
            String platform, Map<String, Object> config) {
        requireApiKey(apiKey, "OpenAI Compatible (" + platform + ")");

        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = inferBaseUrl(platform);
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(60 * 1000);
        requestFactory.setReadTimeout(3 * 60 * 1000);

        // 配置 Reactor Netty 的连接池策略
        ConnectionProvider provider = ConnectionProvider
                .builder("openai-compatible-provider")
                .maxConnections(500)
                // 空闲30秒后主动关闭，避免复用已被服务端关闭的连接
                .maxIdleTime(Duration.ofSeconds(45))
                .maxLifeTime(Duration.ofMinutes(10))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .compress(true)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(60));

        OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(apiKey);

        apiBuilder.restClientBuilder(RestClient.builder().requestFactory(requestFactory));
        apiBuilder.webClientBuilder(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient)));
        if (StrUtil.isNotBlank(baseUrl)) {
            apiBuilder.baseUrl(baseUrl);
        }
        OpenAiApi api = apiBuilder.build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(modelName);
        applyDouble(config, "temperature", optionsBuilder::temperature);
        applyDouble(config, "topP", optionsBuilder::topP);
        applyInt(config, "maxTokens", optionsBuilder::maxTokens);

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    // ========== Ollama (本地模型) ==========

    private ChatModel createOllama(String baseUrl, String modelName, Map<String, Object> config) {
        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = "http://localhost:11434";
        }

        OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();

        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder().model(modelName);
        applyDouble(config, "temperature", optionsBuilder::temperature);
        applyDouble(config, "topP", optionsBuilder::topP);

        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    // ========== Anthropic (Claude 系列) ==========

    private ChatModel createAnthropic(String apiKey, String modelName, Map<String, Object> config) {
        requireApiKey(apiKey, "Anthropic");

        String baseUrl = getStr(config, "baseUrl", null);
        AnthropicApi.Builder apiBuilder = AnthropicApi.builder().apiKey(apiKey);
        if (StrUtil.isNotBlank(baseUrl)) {
            apiBuilder.baseUrl(baseUrl);
        }
        AnthropicApi api = apiBuilder.build();

        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder().model(modelName);
        applyDouble(config, "temperature", optionsBuilder::temperature);
        applyDouble(config, "topP", optionsBuilder::topP);
        applyInt(config, "maxTokens", optionsBuilder::maxTokens);

        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    // ========== Google Vertex AI Gemini ==========

    private ChatModel createVertexAi(Map<String, Object> config, String modelName) {
        String projectId = getStr(config, "projectId", null);
        String location = getStr(config, "location", "us-central1");

        if (StrUtil.isBlank(projectId)) {
            throw new BusinessException("Vertex AI 模型缺少 projectId 配置");
        }

        // 使用 VertexAI 原生 SDK（需要 GOOGLE_APPLICATION_CREDENTIALS 环境变量）
        VertexAI vertexAi = new VertexAI.Builder()
                .setProjectId(projectId)
                .setLocation(location)
                .build();

        VertexAiGeminiChatOptions.Builder optionsBuilder = VertexAiGeminiChatOptions.builder()
                .model(modelName);
        applyDouble(config, "temperature", optionsBuilder::temperature);

        return VertexAiGeminiChatModel.builder()
                .vertexAI(vertexAi)
                .defaultOptions(optionsBuilder.build())
                .build();
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
        if (StrUtil.isBlank(json))
            return Map.of();
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("[ChatModelFactory] 配置JSON解析失败: modelId={}", model.getId(), e);
            return Map.of();
        }
    }

    /**
     * 获取关联的 ApiConfig（可能为 null）
     */
    private ApiConfig resolveApiConfig(AiModel model) {
        if (model.getApiConfigId() == null)
            return null;
        try {
            return apiConfigService.getById(model.getApiConfigId());
        } catch (Exception e) {
            log.warn("[ChatModelFactory] ApiConfig 获取失败: {}", model.getApiConfigId());
            return null;
        }
    }

    /**
     * 解析 apiKey：优先 ApiConfig.apiKey → config.apiKey
     */
    private String resolveApiKey(ApiConfig apiConfig, Map<String, Object> config) {
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getApiKey())) {
            return apiConfig.getApiKey();
        }
        return getStr(config, "apiKey", null);
    }

    /**
     * 解析 baseUrl：优先 ApiConfig.apiUrl → config.baseUrl
     */
    private String resolveBaseUrl(ApiConfig apiConfig, Map<String, Object> config) {
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getApiUrl())) {
            return apiConfig.getApiUrl();
        }
        return getStr(config, "baseUrl", null);
    }

    /**
     * 推断平台类型：
     * 1. ApiConfig.platform 不为空 → 直接使用
     * 2. baseUrl 不为空 → 根据 URL 推断
     * 3. 都为空 → fallback 到 openai_compatible
     */
    private String resolvePlatform(ApiConfig apiConfig, String baseUrl) {
        // 1. ApiConfig 明确指定了平台
        if (apiConfig != null && StrUtil.isNotBlank(apiConfig.getPlatform())) {
            return apiConfig.getPlatform();
        }

        // 2. 从 baseUrl 推断平台
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
            // 有 baseUrl 但无法识别 → 走 OpenAI Compatible
            return "openai_compatible";
        }

        // 3. 都没有 → 默认 openai_compatible（而非 dashscope）
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

    private double toDouble(Object val) {
        if (val instanceof Number n)
            return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

    private int toInt(Object val) {
        if (val instanceof Number n)
            return n.intValue();
        return Integer.parseInt(val.toString());
    }

    private void applyIfPresent(Map<String, Object> config, String key, Consumer<Object> setter) {
        Object val = config.get(key);
        if (val != null) {
            try {
                setter.accept(val);
            } catch (Exception e) {
                log.warn("[ChatModelFactory] 参数设置失败: key={}, value={}", key, val);
            }
        }
    }

    private void applyDouble(Map<String, Object> config, String key,
            Consumer<Double> setter) {
        Object val = config.get(key);
        if (val != null) {
            try {
                setter.accept(toDouble(val));
            } catch (Exception e) {
                log.warn("[ChatModelFactory] 参数设置失败: key={}, value={}", key, val);
            }
        }
    }

    private void applyInt(Map<String, Object> config, String key,
            Consumer<Integer> setter) {
        Object val = config.get(key);
        if (val != null) {
            try {
                setter.accept(toInt(val));
            } catch (Exception e) {
                log.warn("[ChatModelFactory] 参数设置失败: key={}, value={}", key, val);
            }
        }
    }
}
