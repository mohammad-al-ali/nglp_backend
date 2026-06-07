package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Conversation;
import com.NGLP.backend.v1.entity.Msg;
import com.NGLP.backend.v1.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * متحكم إدارة المحادثات (Conversation Controller).
 * الغاية منه: تهيئة المحادثات الذكية وجلب سجل الدردشة بين الطالب والمساعد الذكي لكل درس.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * تهيئة المحادثة الحالية للدرس أو جلبها إذا كانت موجودة مسبقاً.
     * GET /api/v1/conversations?userId=1&lessonId=5
     */
    @GetMapping
    public ResponseEntity<Conversation> initConversation(
            @RequestParam Long userId,
            @RequestParam Long lessonId) {
        log.info("🎯 Initializing conversation for User: {}, Lesson: {}", userId, lessonId);
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        return ResponseEntity.ok(conversation);
    }

    /**
     * جلب سجل المحادثة بالكامل (أرشيف الرسائل) للدرس الحالي باستخدام معرف المحادثة.
     * GET /api/v1/conversations/{id}/messages
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Msg>> getConversationMessages(@PathVariable Long id) {
        log.info("🔍 Fetching message history for Conversation ID: {}", id);
        List<Msg> history = conversationService.getFullChatHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * جلب معرف المحادثة وسجل الرسائل معاً دفعة واحدة باستخدام userId و lessonId.
     * الغرض: تقليل عدد الطلبات عبر الشبكة وتبسيط الاستهلاك في الفرونت إند.
     * GET /api/v1/conversations/history?userId=1&lessonId=5
     */
    @GetMapping("/history")
    public ResponseEntity<?> getConversationHistory(
            @RequestParam Long userId,
            @RequestParam Long lessonId) {
        log.info("🔍 Fetching full chat history for User: {}, Lesson: {}", userId, lessonId);
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        List<Msg> messages = conversationService.getFullChatHistory(conversation.getId());
        
        return ResponseEntity.ok(java.util.Map.of(
            "conversationId", conversation.getId(),
            "messages", messages
        ));
    }

    /**
     * مسح محادثة معينة ومقاطعها من قاعدة البيانات.
     * DELETE /api/v1/conversations/10
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        log.info("🗑️ Deleting Conversation ID: {}", id);
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}