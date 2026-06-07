package com.NGLP.backend.v1.dto;

public record ProgressUpdateRequest(Integer progressPercentage, Long lastWatchedLessonId) {}
