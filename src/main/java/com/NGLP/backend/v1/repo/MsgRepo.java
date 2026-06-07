package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Msg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MsgRepo extends JpaRepository<Msg, Long> {
    void deleteByConversationId(Long convId);

    @Query("SELECT m FROM Msg m WHERE m.conversation.id = :convId ORDER BY m.sentAt DESC")
    List<Msg> findLastMessages(@Param("convId") Long convId, Pageable pageable);

    // جلب رسائل محادثة معينة مرتبة من الأقدم للأحدث
    List<Msg> findByConversationIdOrderBySentAtAsc(Long conversationId);

    Optional<Msg> findFirstByConversationIdOrderBySentAtDesc(Long id);
}