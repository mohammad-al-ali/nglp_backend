package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.config.LlmModelsConfig;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.core.RequestOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GroqProvider implements LlmProvider {

    private final LlmModelsConfig.ProviderConfig config;
    private final String apiKey;
    private ChatModel chatModel;
    private List<ModelInfo> models;
    private OpenAIClient rawClient;

    /**
     * حالة الوصول الحيّة للمزوّد — منفصلة عن {@code isEnabled()} (الذي يعكس فقط نجاح
     * الإقلاع). يُحدَّثها الفحص الدوري {@link #checkHealth()}، وتُطفأ فوراً عبر
     * {@link #markUnhealthy()} عند أي فشل استدعاء فعلي فلا ننتظر دورة الفحص القادمة.
     */
    private volatile boolean healthy = true;

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
            this.rawClient = client;
            // ملاحظة: لا نفرض responseFormat=JSON_OBJECT هنا. فرضه على مستوى المزوّد
            // كان يجعل ردود المساعد في المحادثة العامة تخرج كـ JSON خام (أقواس ورموز
            // تظهر للطالب). صيغة JSON مطلوبة فقط لتوليد الكويز، وتُطبَّق هناك لكل طلب
            // على حدة عبر structuredOutputOptions().
            OpenAiChatOptions chatOptions = baseChatOptionsBuilder().build();
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
    public boolean isHealthy() { return healthy; }

    @Override
    public void markUnhealthy() {
        if (healthy) {
            healthy = false;
            log.warn("⚠️ Groq marked unhealthy after a live call failure — routing away from it until the next health check succeeds.");
        }
    }

    @Override
    public List<ModelInfo> getModels() { return models; }

    @Override
    public ChatClient.Builder createChatClientBuilder() {
        if (chatModel == null) {
            throw new RuntimeException("Groq provider is not available. Check GROQ_API_KEY environment variable.");
        }
        return ChatClient.builder(chatModel);
    }

    /**
     * فحص دوري خفيف لحالة Groq — طلب قائمة النماذج فقط (بلا تكلفة توليد) بمهلة قصيرة.
     * يعيد المزوّد إلى الخدمة تلقائياً بمجرد نجاح الفحص، ويُبقيه خارج التوجيه طالما استمر الفشل.
     * لا يعمل إن لم يُقلَع المزوّد أصلاً (مفتاح مفقود مثلاً) لتفادي تكرار نفس الخطأ كل دورة.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 5 * 60_000)
    public void checkHealth() {
        if (rawClient == null) {
            return;
        }
        try {
            rawClient.models().list(RequestOptions.builder().timeout(Duration.ofSeconds(8)).build());
            if (!healthy) {
                healthy = true;
                log.info("✅ Groq health check succeeded — provider back in rotation.");
            }
        } catch (Exception e) {
            if (healthy) {
                healthy = false;
                log.warn("⚠️ Groq health check failed — excluding it from routing until it recovers: {}", e.getMessage());
            }
        }
    }

    /**
     * خيارات توليد الكويز: نفس إعدادات المزوّد + إجبار الرد على أن يكون JSON صرفاً.
     * تُطبَّق لكل طلب كويز على حدة، ولا تؤثّر على محادثة المساعد العامة.
     */
    @Override
    public ChatOptions.Builder<?> structuredOutputOptions() {
        return baseChatOptionsBuilder()
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
    }

    /**
     * الإعدادات المشتركة لكل طلبات Groq.
     * <p>maxCompletionTokens: نماذج gpt-oss "تفكّر" قبل الإجابة وهذا الاستدلال يستهلك من
     * نفس حصة التوكنز؛ حساب Groq المجاني هنا محدود بـ 8000 توكن/دقيقة (prompt + completion)،
     * لذا 4096 حدّ آمن.
     */
    private OpenAiChatOptions.Builder baseChatOptionsBuilder() {
        return OpenAiChatOptions.builder()
                .model(config.getDefaultModel())
                .temperature(0.3)
                .maxCompletionTokens(4096);
    }
}
