package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.ai.NglpAiAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * المتحكم الخاص بعمليات الذكاء الاصطناعي (AI REST Controller).
 * يوفر نقاط اتصال (Endpoints) تتيح للواجهة الأمامية إرسال أسئلة الطلاب واستلام الردود بشكل كامل أو كبث لحظي.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final NglpAiAgent nglpAiAgent;

    /**
     * هيكل البيانات (Record) الخاص بطلب المحادثة.
     */
    public record AiRequest(
            Long userId,
            Long lessonId,
            String timestamp,
            String message
    ) {}

    /**
     * نقطة الاتصال التقليدية لإرسال سؤال واسترجاع الإجابة الكاملة دفعة واحدة.
     * المسار: POST /api/v1/ai/messages
     */
    @PostMapping("/messages")
    public ResponseEntity<?> askTutor(@RequestBody AiRequest request) {
        log.info("سؤال جديد من الذكاء الاصطناعي من المستخدم: {} للدرس: {}", request.userId(), request.lessonId());

        try {
            String aiResponse = nglpAiAgent.ask(
                    request.userId(),
                    request.lessonId(),
                    request.timestamp(),
                    request.message()
            );

            return ResponseEntity.ok(Map.of("response", aiResponse));

        } catch (Exception e) {
            log.error("❌ خطأ في محرك الدردشة بالذكاء الاصطناعي:", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "عذراً، واجه المعلم الذكي مشكلة غير متوقعة. يرجى تكرار المحاولة لاحقاً."));
        }
    }

    /**
     * نقطة الاتصال للبث اللحظي للرد (Streaming Endpoint) بنظام Server-Sent Events (SSE).
     * المسار: POST /api/v1/ai/messages/stream
     * تتيح للطالب رؤية الكلمات وهي تتولد حرفاً بحرف لحظياً لزيادة التفاعلية.
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askTutorStream(@RequestBody AiRequest request) {
        log.info("سؤال جديد من الذكاء الاصطناعي من المستخدم: {} للدرس: {}", request.userId(), request.lessonId());

        try {
            return nglpAiAgent.askStream(
                    request.userId(),
                    request.lessonId(),
                    request.timestamp(),
                    request.message()
            );
        } catch (Exception e) {
            log.error("❌ Error in AI Chat Engine: ", e);
            return Flux.just("عذراً، واجه المعلم الذكي مشكلة أثناء معالجة البث اللحظي.");
        }
    }
}