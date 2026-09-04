package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.StudentDashboardResponse;
import com.NGLP.backend.v1.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentDashboardService service;

    @GetMapping("/{userId}/dashboard")
    public ResponseEntity<StudentDashboardResponse> dashboard(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {

        boolean privileged = callerRole != null
                && (callerRole.toUpperCase().contains("ADMIN") || callerRole.toUpperCase().contains("TEACHER"));
        if (callerId != null && !callerId.equals(userId) && !privileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "لا يمكنك عرض لوحة طالب آخر.");
        }
        return ResponseEntity.ok(service.build(userId));
    }
}
