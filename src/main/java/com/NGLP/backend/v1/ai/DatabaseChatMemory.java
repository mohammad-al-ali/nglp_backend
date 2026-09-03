package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.entity.Conversation;
import com.NGLP.backend.v1.entity.Msg;
import com.NGLP.backend.v1.repo.ConversationRepo;
import com.NGLP.backend.v1.repo.MsgRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ذاكرة المحادثة للذكاء الاصطناعي مدعومة بقاعدة البيانات (جدول {@code messages}).
 *
 * <p>يحفظ رسائل الطالب والمساعد فقط (يتجاهل رسائل النظام والأدوات)، وينظّف كل رسالة
 * عبر {@link ChatMessageSanitizer} قبل التخزين حتى لا يتسرّب السياق التقني المحقون
 * (تفريغ الفيديو، وسوم XML الداخلية) إلى سجل الدردشة الظاهر للطالب.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChatMemory implements ChatMemory {

    /** عدد الرسائل الأخيرة المُرسلة للنموذج كسياق (4 حوارات متبادلة) — توفيراً للـ Tokens. */
    private static final int DEFAULT_CONTEXT_WINDOW = 8;

    private final ConversationRepo conversationRepository;
    private final MsgRepo msgRepository;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> aiMessages) {
        Long convId = parseConversationId(conversationId);
        if (convId == null || aiMessages == null || aiMessages.isEmpty()) {
            return;
        }

        Conversation conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new IllegalStateException("لم يتم العثور على المحادثة: " + convId));

        // آخر رسالة مخزّنة — لمنع الحفظ المكرّر لنفس الدور والمحتوى.
        Msg lastStored = msgRepository.findFirstByConversationIdOrderBySentAtDesc(convId).orElse(null);
        String lastType = lastStored != null ? lastStored.getSenderType() : null;
        String lastContent = lastStored != null ? lastStored.getContent() : null;

        List<Msg> toSave = new ArrayList<>();
        for (Message aiMsg : aiMessages) {
            // نحفظ رسائل الطالب والمساعد فقط — لا رسائل النظام ولا استدعاءات الأدوات.
            if (aiMsg.getMessageType() != MessageType.USER && aiMsg.getMessageType() != MessageType.ASSISTANT) {
                continue;
            }

            String senderType = aiMsg.getMessageType().name(); // "USER" أو "ASSISTANT"
            String content = ChatMessageSanitizer.sanitize(aiMsg.getText());
            if (!StringUtils.hasText(content)) {
                continue;
            }

            if (senderType.equalsIgnoreCase(lastType) && content.equals(lastContent)) {
                continue; // تكرار فوري — تجاهله.
            }

            Msg dbMsg = new Msg();
            dbMsg.setConversation(conversation);
            dbMsg.setSenderType(senderType);
            dbMsg.setContent(content);
            dbMsg.setSentAt(LocalDateTime.now());
            toSave.add(dbMsg);

            lastType = senderType;
            lastContent = content;
        }

        if (!toSave.isEmpty()) {
            msgRepository.saveAll(toSave);
            log.info("💾 حُفظت {} رسالة جديدة في قاعدة البيانات للمحادثة: {}", toSave.size(), convId);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        Long convId = parseConversationId(conversationId);
        if (convId == null) {
            return List.of();
        }

        List<Msg> dbMessages = msgRepository.findLastMessages(convId, PageRequest.of(0, DEFAULT_CONTEXT_WINDOW));
        // من الأحدث للأقدم -> نعكسها لتصبح مرتّبة زمنياً قبل إرسالها للنموذج.
        Collections.reverse(dbMessages);

        List<Message> history = new ArrayList<>();
        for (Msg dbMsg : dbMessages) {
            String content = ChatMessageSanitizer.sanitize(dbMsg.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            if ("USER".equalsIgnoreCase(dbMsg.getSenderType())) {
                history.add(new UserMessage(content));
            } else if ("ASSISTANT".equalsIgnoreCase(dbMsg.getSenderType())) {
                history.add(new AssistantMessage(content));
            }
        }
        return history;
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        Long convId = parseConversationId(conversationId);
        if (convId != null) {
            msgRepository.deleteByConversationId(convId);
        }
    }

    private Long parseConversationId(String conversationId) {
        try {
            return Long.parseLong(conversationId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
