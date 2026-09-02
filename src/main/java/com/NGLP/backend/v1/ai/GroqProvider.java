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
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GroqProvider implements LlmProvider {

    private final LlmModelsConfig.ProviderConfig config;
    private final String apiKey;
    private ChatModel chatModel;
    private List<ModelInfo> models;

    public GroqProvider(LlmModelsConfig llmModelsConfig,
                         @Value("${nglp.llm.groq.api-key:}") String apiKeyFromProps) {
        this.config = llmModelsConfig.getProviders().stream()
                .filter(p -> "groq".equals(p.getKey()))
                .findFirst()
                .orElse(null);
        String key = apiKeyFromProps;
        if (key == null || key.isBlank()) {
            key = System.getenv("GROQ_API_KEY");
        }
        this.apiKey = key;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY is not set, Groq provider disabled");
            return;
        }
        if (config == null || !config.isEnabled()) {
            log.warn("Groq provider is disabled in config");
            return;
        }
        try {
            SpringAiOpenAiHttpClient httpClient = SpringAiOpenAiHttpClient.builder().build();
            ClientOptions options = ClientOptions.builder()
                    .httpClient(httpClient)
                    .apiKey(apiKey)
                    .baseUrl("https://api.groq.com/openai/v1")
                    .build();
            OpenAIClient client = new OpenAIClientImpl(options);
            OpenAIClientAsync clientAsync = client.async();
            // بدون هذا، بعض نماذج Groq (مثل gpt-oss) قد تجيب بنص عادي بدل JSON
            // صِرف رغم تعليمات الصيغة في الـ prompt، فيفشل تحويل الرد إلى الكائن
            // المتوقع (AiQuizResponse) عند توليد الكويز.
            // maxCompletionTokens: نماذج gpt-oss "تفكّر" قبل كتابة الإجابة، وهذا
            // الاستدلال الداخلي يستهلك من نفس حصة التوكنز. بدون حد كافٍ يُستنفد
            // الاستدلال الحصة فيفشل توليد الـ JSON النهائي بخطأ 400 من Groq.
            // 4096 (لا أعلى): حساب Groq المجاني هنا محدود بـ 8000 توكن/دقيقة
            // إجمالاً (prompt + completion)؛ رفعها لـ 8192 كان يتجاوز الحد فوراً.
            OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                    .model(config.getDefaultModel())
                    .temperature(0.3)
                    .maxCompletionTokens(4096)
                    .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                    .build();
            this.chatModel = OpenAiChatModel.builder()
                    .openAiClient(client)
                    .openAiClientAsync(clientAsync)
                    .options(chatOptions)
                    .build();
            this.models = config.getModels().stream()
                    .map(m -> new ModelInfo(m.getKey(), m.getName(), m.isFree()))
                    .collect(Collectors.toList());
            log.info("Groq provider initialized with model: {}", config.getDefaultModel());
        } catch (Exception e) {
            log.error("Failed to initialize Groq provider: {}", e.getMessage());
        }
    }

    @Override
    public String getProviderKey() { return "groq"; }

    @Override
    public boolean isEnabled() { return chatModel != null; }

    @Override
    public List<ModelInfo> getModels() { return models; }

    @Override
    public ChatClient.Builder createChatClientBuilder() {
        if (chatModel == null) {
            throw new RuntimeException("Groq provider is not available. Check GROQ_API_KEY environment variable.");
        }
        return ChatClient.builder(chatModel);
    }
}
