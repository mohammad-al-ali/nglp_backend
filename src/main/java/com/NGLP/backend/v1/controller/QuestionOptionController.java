package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.QuestionOption;
import com.NGLP.backend.v1.service.QuestionOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/options")
@RequiredArgsConstructor
public class QuestionOptionController {

    private final QuestionOptionService questionOptionService;

    // جلب خيارات سؤال معين
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<QuestionOption>> getByQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionOptionService.findOptionsByQuestion(questionId));
    }

    // جلب خيار بالمعرف
    @GetMapping("/{id}")
    public ResponseEntity<QuestionOption> getById(@PathVariable Long id) {
        return ResponseEntity.ok(questionOptionService.findById(id));
    }

    // إضافة خيار لسؤال
    @PostMapping("/question/{questionId}")
    public ResponseEntity<QuestionOption> create(
            @PathVariable Long questionId,
            @RequestBody QuestionOption option,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        QuestionOption created = questionOptionService.create(questionId, option);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // تعديل خيار
    @PutMapping("/{id}")
    public ResponseEntity<QuestionOption> update(
            @PathVariable Long id,
            @RequestBody QuestionOption option,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        return ResponseEntity.ok(questionOptionService.update(id, option));
    }

    // حذف خيار
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        validateAdminAccess(userRole);

        questionOptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdminAccess(String userRole) {
        if (userRole == null || !userRole.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can manage question options."
            );
        }
    }
}
