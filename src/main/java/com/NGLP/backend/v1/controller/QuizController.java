package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.*;
import com.NGLP.backend.v1.entity.QuizAttempt;
import com.NGLP.backend.v1.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody QuizGenerateRequest request) {
        log.info("📝 توليد كويز: lessonId={}, title='{}', أسئلة={}", request.lessonId(), request.title(), request.numberOfQuestions());
        QuizResponse response = quizService.generateQuiz(request);
        log.info("✅ تم توليد الكويز ID={} بعنوان '{}'", response.id(), response.title());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuiz(@PathVariable Long id) {
        log.info("🔍 عرض كويز كامل: quizId={}", id);
        return ResponseEntity.ok(quizService.findById(id));
    }

    @GetMapping("/{id}/student-view")
    public ResponseEntity<?> getStudentView(@PathVariable Long id) {
        log.info("🔍 عرض كويز للطالب: quizId={}", id);
        return ResponseEntity.ok(quizService.findStudentView(id));
    }

    @GetMapping
    public ResponseEntity<?> getQuizzes(@RequestParam Long lessonId) {
        log.info("🔍 قائمة كويزات الدرس: lessonId={}", lessonId);
        List<?> quizzes = quizService.findByLesson(lessonId);
        log.info("✅ تم جلب {} كويز", quizzes.size());
        return ResponseEntity.ok(quizzes);
    }

    @PutMapping("/{id}/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @PathVariable Long questionId, @RequestBody QuizQuestionRequest request) {
        log.info("✏️ تعديل سؤال: quizId={}, questionId={}", id, questionId);
        return ResponseEntity.ok(quizService.updateQuestion(id, questionId, request));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<?> addQuestion(@PathVariable Long id, @RequestBody QuizQuestionRequest request) {
        log.info("➕ إضافة سؤال يدوي للكويز: quizId={}", id);
        return ResponseEntity.ok(quizService.addQuestion(id, request));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        log.info("🗑️ حذف سؤال: quizId={}, questionId={}", id, questionId);
        return ResponseEntity.ok(quizService.deleteQuestion(id, questionId));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishQuiz(@PathVariable Long id) {
        log.info("📢 نشر كويز: quizId={}", id);
        return ResponseEntity.ok(quizService.publishQuiz(id));
    }

    @PostMapping("/{id}/attempts")
    public ResponseEntity<?> startAttempt(@PathVariable Long id, @RequestParam Long studentId) {
        log.info("🎯 بدء محاولة: quizId={}, studentId={}", id, studentId);
        QuizAttempt attempt = quizService.startAttempt(id, studentId);
        log.info("✅ بدأ الطالب {} المحاولة رقم {} للكويز {}", studentId, attempt.getAttemptNumber(), id);
        return ResponseEntity.ok(Map.of(
                "attemptId", attempt.getId(),
                "attemptNumber", attempt.getAttemptNumber(),
                "startedAt", attempt.getStartedAt()
        ));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<?> submitAttempt(@PathVariable Long attemptId, @RequestBody QuizSubmitRequest request) {
        log.info("📥 تسليم المحاولة: attemptId={}, إجابات={}", attemptId, request.answers() != null ? request.answers().size() : 0);
        QuizAttemptResponse response = quizService.submitAttempt(attemptId, request);
        log.info("✅ تم تسليم المحاولة {}: النتيجة {}", attemptId, response.score());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attempts")
    public ResponseEntity<?> getAttempts(@RequestParam Long studentId, @RequestParam Long quizId) {
        log.info("📋 سجل محاولات: studentId={}, quizId={}", studentId, quizId);
        List<QuizAttempt> attempts = quizService.getAttempts(quizId, studentId);
        log.info("✅ تم جلب {} محاولة", attempts.size());
        return ResponseEntity.ok(attempts);
    }
}
