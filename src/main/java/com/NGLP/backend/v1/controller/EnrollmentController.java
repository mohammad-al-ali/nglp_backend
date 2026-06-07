package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.ProgressUpdateRequest;
import com.NGLP.backend.v1.entity.Enrollment;
import com.NGLP.backend.v1.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<List<Enrollment>> getByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(enrollmentService.findByUser(userId));
    }

    @PostMapping
    public ResponseEntity<Enrollment> enroll(@RequestParam Long userId, @RequestParam Long courseId) {
        return ResponseEntity.ok(enrollmentService.enroll(userId, courseId));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<Enrollment> updateProgress(@PathVariable Long id, @RequestBody ProgressUpdateRequest request) {
        return ResponseEntity.ok(enrollmentService.updateProgress(id, request));
    }
}
