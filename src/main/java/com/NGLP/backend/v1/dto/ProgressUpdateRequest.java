package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressUpdateRequest(
    @NotNull(message = "{nglp.progress.percentage.required}")
    @Min(value = 0, message = "{nglp.progress.percentage.range}")
    @Max(value = 100, message = "{nglp.progress.percentage.range}")
    Integer progressPercentage,

    Long lastWatchedLessonId
) {}
