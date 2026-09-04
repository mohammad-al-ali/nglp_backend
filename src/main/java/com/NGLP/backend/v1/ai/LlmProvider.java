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
     * هل المزوّد قابل للوصول فعلياً الآن؟ ({@code isEnabled()} يعني فقط أن المفتاح والإعداد
     * صحيحان وقت الإقلاع — لا يكشف عطلاً لاحقاً في الشبكة أو حساب المزوّد، كما حدث مع Groq).
     * المزوّدون الذين لا يملكون فحصاً دورياً خاصاً يبقون بصحة افتراضية {@code true}.
     *
     * @see #markUnhealthy() لتحديث الحالة فور فشل استدعاء فعلي، دون انتظار الفحص الدوري التالي.
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * يُستدعى عندما يفشل استدعاء فعلي لهذا المزوّد (مثل 403/5xx من الشبكة)، ليُستبعد فوراً من
     * التوجيه حتى ينجح الفحص الدوري التالي — بدل انتظار دورة الفحص القادمة لاكتشاف العطل.
     * لا تأثير للمزوّدين الذين لا يتتبّعون صحة حية.
     */
    default void markUnhealthy() {
        // لا شيء بشكل افتراضي.
    }

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
