package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.StudentAnswer;
import com.NGLP.backend.v1.service.StudentAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class StudentAnswerController {

    private final StudentAnswerService studentAnswerService;

    // جلب إجابات طالب معين
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StudentAnswer>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(studentAnswerService.findByUser(userId));
    }

    // جلب إجابة بالمعرف
    @GetMapping("/{id}")
    public ResponseEntity<StudentAnswer> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentAnswerService.findById(id));
    }

    // إرسال إجابة الطالب
    @PostMapping
    public ResponseEntity<StudentAnswer> create(
            @RequestParam Long userId,
            @RequestParam Long questionId,
            @RequestParam Long optionId) {

        StudentAnswer created =
                studentAnswerService.create(userId, questionId, optionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
