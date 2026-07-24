package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.config.LlmModelsConfig;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeepSeekProvider implements LlmProvider {

    private final LlmModelsConfig.ProviderConfig config;
    private final String apiKey;
    private ChatModel chatModel;
    private List<ModelInfo> models;

    public DeepSeekProvider(LlmModelsConfig llmModelsConfig,
                            @Value("${nglp.llm.deepseek.api-key:}") String apiKeyFromProps) {
        this.config = llmModelsConfig.getProviders().stream()
                .filter(p -> "deepseek".equals(p.getKey()))
                .findFirst()
                .orElse(null);
        String key = apiKeyFromProps;
        if (key == null || key.isBlank()) {
            key = System.getenv("DEEPSEEK_API_KEY");
        }
        this.apiKey = key;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DEEPSEEK_API_KEY is not set, DeepSeek provider disabled");
            return;
        }
        if (config == null || !config.isEnabled()) {
            log.warn("DeepSeek provider is disabled in config");
            return;
        }
        try {
            SpringAiOpenAiHttpClient httpClient = SpringAiOpenAiHttpClient.builder().build();
            ClientOptions options = ClientOptions.builder()
                    .httpClient(httpClient)
                    .apiKey(apiKey)
                    .baseUrl("https://api.deepseek.com")
                    .build();
            OpenAIClient client = new OpenAIClientImpl(options);
            OpenAIClientAsync clientAsync = client.async();
            OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                    .model(config.getDefaultModel())
                    .temperature(0.3)
                    .build();
            this.chatModel = OpenAiChatModel.builder()
                    .openAiClient(client)
                    .openAiClientAsync(clientAsync)
                    .options(chatOptions)
                    .build();
            this.models = config.getModels().stream()
                    .map(m -> new ModelInfo(m.getKey(), m.getName(), m.isFree()))
                    .collect(Collectors.toList());
            log.info("DeepSeek provider initialized with model: {}", config.getDefaultModel());
        } catch (Exception e) {
            log.error("Failed to initialize DeepSeek provider: {}", e.getMessage());
        }
    }

    @Override
    public String getProviderKey() { return "deepseek"; }

    @Override
    public boolean isEnabled() { return chatModel != null; }

    @Override
    public List<ModelInfo> getModels() { return models; }

    @Override
    public ChatClient.Builder createChatClientBuilder() {
        if (chatModel == null) {
            throw new RuntimeException("DeepSeek provider is not available. Check DEEPSEEK_API_KEY environment variable.");
        }
        return ChatClient.builder(chatModel);
    }
}
