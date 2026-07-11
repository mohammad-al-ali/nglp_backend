package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Quiz;
import com.NGLP.backend.v1.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // جلب جميع اختبارات درس معين
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<Quiz>> getByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(quizService.findByLessonId(lessonId));
    }

    // جلب اختبار بالمعرف
    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.findById(id));
    }

    // إنشاء اختبار
    @PostMapping("/lesson/{lessonId}")
    public ResponseEntity<Quiz> create(
            @PathVariable Long lessonId,
            @RequestBody Quiz quiz,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        Quiz created = quizService.create(lessonId, quiz);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // تعديل اختبار
    @PutMapping("/{id}")
    public ResponseEntity<Quiz> update(
            @PathVariable Long id,
            @RequestBody Quiz quiz,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);
        return ResponseEntity.ok(quizService.update(id, quiz));
    }

    // حذف اختبار
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdminAccess(String userRole) {
        if (userRole == null || !userRole.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can manage quizzes."
            );
        }
    }
}