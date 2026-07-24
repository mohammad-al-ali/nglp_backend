package com.NGLP.backend.v1.ai;

import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

public interface LlmProvider {
    String getProviderKey();
    boolean isEnabled();
    List<ModelInfo> getModels();
    ChatClient.Builder createChatClientBuilder();

    record ModelInfo(String key, String name, boolean isFree) {}
}
