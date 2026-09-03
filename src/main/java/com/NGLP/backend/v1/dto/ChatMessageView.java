package com.NGLP.backend.v1.dto;

import com.NGLP.backend.v1.ai.ChatMessageSanitizer;
import com.NGLP.backend.v1.entity.Msg;

import java.time.LocalDateTime;

/**
 * تمثيل رسالة محادثة كما تُعرض للواجهة الأمامية: المحتوى منظّف عبر
 * {@link ChatMessageSanitizer} (يزيل السياق التقني المحقون والوسوم الداخلية
 * من الرسائل القديمة الملوّثة) ودون كشف تفاصيل الكيان الداخلية.
 */
public record ChatMessageView(
        Long id,
        String senderType,
        String content,
        Integer videoTimestamp,
        LocalDateTime sentAt
) {
    public static ChatMessageView from(Msg msg) {
        return new ChatMessageView(
                msg.getId(),
                msg.getSenderType(),
                ChatMessageSanitizer.sanitize(msg.getContent()),
                msg.getVideoTimestamp(),
                msg.getSentAt()
        );
    }
}
