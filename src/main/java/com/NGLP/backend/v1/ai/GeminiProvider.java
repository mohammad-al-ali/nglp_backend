package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.config.LlmModelsConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeminiProvider implements LlmProvider {

    private final ChatModel chatModel;
    private final LlmModelsConfig.ProviderConfig config;
    private List<ModelInfo> models;

    public GeminiProvider(@Qualifier("googleGenAiChatModel") ChatModel chatModel,
                          LlmModelsConfig llmModelsConfig) {
        this.chatModel = chatModel;
        this.config = llmModelsConfig.getProviders().stream()
                .filter(p -> "gemini".equals(p.getKey()))
                .findFirst()
                .orElse(null);
    }

    @PostConstruct
    public void init() {
        if (config != null) {
            this.models = config.getModels().stream()
                    .map(m -> new ModelInfo(m.getKey(), m.getName(), m.isFree()))
                    .collect(Collectors.toList());
        }
        log.info("Gemini provider initialized");
    }

    @Override
    public String getProviderKey() { return "gemini"; }

    @Override
    public boolean isEnabled() { return chatModel != null; }

    @Override
    public List<ModelInfo> getModels() { return models; }

    @Override
    public ChatClient.Builder createChatClientBuilder() {
        return ChatClient.builder(chatModel);
    }
}
