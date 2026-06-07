package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Conversation;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.Msg;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.ConversationRepo;
import com.NGLP.backend.v1.repo.MsgRepo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepo conversationRepo;
    private final EntityManager entityManager; // 🌟 للوصول السريع لـ Proxies بدون استعلامات إضافية
    private final MsgRepo msgRepo;

    @Transactional
    public Conversation getOrCreateConversation(Long userId, Long lessonId) {
        return conversationRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();

                    // 🌟 تكتيك هندسي: ربط الـ IDs مباشرة دون عمل SELECT من جداول المستخدمين والدروس
                    newConv.setUser(entityManager.getReference(User.class, userId));
                    newConv.setLesson(entityManager.getReference(Lesson.class, lessonId));
                    newConv.setStartedAt(LocalDateTime.now());

                    return conversationRepo.save(newConv);
                });
    }

    /**
     * دالة مخصصة للـ Frontend (React) لعرض كامل السجل المرتب زمنياً للطالب
     */
    public List<Msg> getFullChatHistory(Long conversationId) {
        return msgRepo.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Transactional
    public void deleteConversation(Long id) {
        // سيقوم بحذف الرسائل أيضاً لو كنت واضعاً CascadeType.ALL في الـ Entity
        conversationRepo.deleteById(id);
    }
}