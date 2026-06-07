package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepo extends JpaRepository<Conversation, Long> {
        // جلب محادثة الطالب لدرس معين (إن وجدت)
        Optional<Conversation> findByUserIdAndLessonId(Long userId, Long lessonId);
}
