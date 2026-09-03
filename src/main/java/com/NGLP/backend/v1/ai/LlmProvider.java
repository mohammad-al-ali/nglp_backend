package com.NGLP.backend.v1.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import java.util.List;

public interface LlmProvider {
    String getProviderKey();
    boolean isEnabled();
    List<ModelInfo> getModels();
    ChatClient.Builder createChatClientBuilder();

    /**
     * خيارات المزوّد للطلبات التي يجب أن ترجع JSON صرفاً (توليد الكويز).
     * تُطبَّق لكل طلب على حدة فقط — لا يجوز فرض JSON على المحادثة العامة مع
     * المساعد وإلا ظهرت رموز وأقواس JSON في نص الرد.
     *
     * @return باني الخيارات، أو {@code null} إن لم يكن المزوّد بحاجة لإعداد خاص.
     */
    default ChatOptions.Builder<?> structuredOutputOptions() {
        return null;
    }

    record ModelInfo(String key, String name, boolean isFree) {}
}
