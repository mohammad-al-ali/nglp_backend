package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.QuizQuestion;
import com.NGLP.backend.v1.service.QuizQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuizQuestionController {

    private final QuizQuestionService quizQuestionService;

    // جلب أسئلة اختبار معين
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizQuestion>> getByQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizQuestionService.findQuestionsByQuiz(quizId));
    }

    // جلب سؤال بالمعرف
    @GetMapping("/{id}")
    public ResponseEntity<QuizQuestion> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quizQuestionService.findById(id));
    }

    // إضافة سؤال لاختبار
    @PostMapping("/quiz/{quizId}")
    public ResponseEntity<QuizQuestion> create(
            @PathVariable Long quizId,
            @RequestBody QuizQuestion question,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        QuizQuestion created = quizQuestionService.create(quizId, question);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // تعديل سؤال
    @PutMapping("/{id}")
    public ResponseEntity<QuizQuestion> update(
            @PathVariable Long id,
            @RequestBody QuizQuestion question,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        return ResponseEntity.ok(quizQuestionService.update(id, question));
    }

    // حذف سؤال
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        quizQuestionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdminAccess(String userRole) {
        if (userRole == null || !userRole.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can manage quiz questions."
            );
        }
    }
}