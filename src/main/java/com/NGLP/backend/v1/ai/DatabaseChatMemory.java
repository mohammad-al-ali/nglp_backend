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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * إدارة ذاكرة المحادثة للذكاء الاصطناعي وتخزينها في قاعدة البيانات.
 * يقوم بحفظ الرسائل الحقيقية فقط بين الطالب والوكيل وتجنب تكرارها أو تلوثها برسائل الأدوات.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChatMemory implements ChatMemory {

    private final ConversationRepo conversationRepository;
    private final MsgRepo msgRepository;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> aiMessages) {
        Long convId = parseConversationId(conversationId);
        if (convId == null || aiMessages == null || aiMessages.isEmpty()) return;

        Conversation conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("لم يتم العثور على المحادثة:" + convId));

        //  كشف الفرق : جلب آخر رسالة مخزنة في قاعدة البيانات للمطابقة
        Msg latestDbMsg = msgRepository.findFirstByConversationIdOrderBySentAtDesc(convId).orElse(null);
        int startIndex = 0;
        if (latestDbMsg != null) {
            // البحث التراجعي من نهاية القائمة لتحديد موقع آخر رسالة مخزنة
            for (int i = aiMessages.size() - 1; i >= 0; i--) {
                if (aiMessages.get(i).getText().equals(latestDbMsg.getContent()) 
                    && aiMessages.get(i).getMessageType().name().equalsIgnoreCase(latestDbMsg.getSenderType())) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        List<Msg> dbMessages = new ArrayList<>();
        for (int i = startIndex; i < aiMessages.size(); i++) {
            Message aiMsg = aiMessages.get(i);
            
            // حفظ رسائل USER و ASSISTANT فقط لتجنب تلوث الذاكرة برسائل الأدوات (TOOL) المؤقتة
            if (aiMsg.getMessageType() == MessageType.USER || aiMsg.getMessageType() == MessageType.ASSISTANT) {
                Msg dbMsg = new Msg();
                dbMsg.setConversation(conversation);
                
                String content = aiMsg.getText();
                if (aiMsg.getMessageType() == MessageType.USER) {
                    content = cleanUserMessage(content);
                }
                dbMsg.setContent(content);
                dbMsg.setSentAt(LocalDateTime.now());
                dbMsg.setSenderType(aiMsg.getMessageType().name()); // USER أو ASSISTANT
                dbMessages.add(dbMsg);
            }
        }

        if (!dbMessages.isEmpty()) {
            msgRepository.saveAll(dbMessages);
            log.info("تم حفظ {} رسالة جديدة في قاعدة البيانات لمعرف المحادثة: {}", dbMessages.size(), convId);
        }
    }

    /**
     * تنظيف رسائل الطالب من السياق المحقون تلقائياً (RAG Context) لحفظ السؤال الصافي فقط في قاعدة البيانات.
     */
    private String cleanUserMessage(String content) {
        if (content == null) return "";
        if (content.startsWith("Student Question: ")) {
            content = content.substring("Student Question: ".length());
        }
        int index = content.indexOf("\n[Video Transcript Context");
        if (index != -1) {
            content = content.substring(0, index);
        }
        int systemIndex = content.indexOf("\n[System Info:");
        if (systemIndex != -1) {
            content = content.substring(0, systemIndex);
        }
        return content.trim();
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, 8); // جلب آخر 8 رسائل فقط (4 حوارات متبادلة) لتوفير الـ Tokens وتجنب حدود TPM لـ Groq
    }

    public List<Message> get(String conversationId, int lastN) {
        Long convId = parseConversationId(conversationId);
        if (convId == null) return List.of();

        List<Msg> dbMessages = msgRepository.findLastMessages(convId, PageRequest.of(0, lastN));

        // عكس ترتيب الرسائل لتكون مرتبة زمنياً من الأقدم للأحدث قبل إرسالها للـ LLM
        Collections.reverse(dbMessages);

        return dbMessages.stream()
                .filter(dbMsg -> "USER".equalsIgnoreCase(dbMsg.getSenderType()) || "ASSISTANT".equalsIgnoreCase(dbMsg.getSenderType()))
                .map(dbMsg -> {
                    if ("USER".equalsIgnoreCase(dbMsg.getSenderType())) {
                        return new UserMessage(dbMsg.getContent());
                    } else {
                        return new AssistantMessage(dbMsg.getContent());
                    }
                }).collect(Collectors.toList());
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