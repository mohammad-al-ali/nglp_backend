package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepo extends JpaRepository<Conversation, Long> {
        // جلب محادثة الطالب لدرس معين (إن وجدت)
        Optional<Conversation> findByUserIdAndLessonId(Long userId, Long lessonId);

        /**
         * محادثة تُنشأ ضمنياً بمجرد فتح الدرس (GET /conversations/history)، حتى بلا أي رسالة —
         * لذا "جلسة تعلّم بالـ AI" الحقيقية = محادثة فيها رسالة واحدة على الأقل من الطالب.
         */
        @Query("SELECT COUNT(c) FROM Conversation c WHERE c.user.id = :userId " +
                "AND EXISTS (SELECT 1 FROM Msg m WHERE m.conversation = c AND m.senderType = 'USER')")
        long countActiveByUserId(@Param("userId") Long userId);

        /** أحدث جلسات المساعد الذكي الفعلية (فيها رسالة طالب واحدة على الأقل) — لسجلّ النشاط. */
        @Query("SELECT c FROM Conversation c JOIN FETCH c.lesson l JOIN FETCH l.course cc " +
                "WHERE c.user.id = :userId " +
                "AND EXISTS (SELECT 1 FROM Msg m WHERE m.conversation = c AND m.senderType = 'USER') " +
                "ORDER BY c.startedAt DESC")
        List<Conversation> findRecentActiveForUser(@Param("userId") Long userId, Pageable pageable);
}
