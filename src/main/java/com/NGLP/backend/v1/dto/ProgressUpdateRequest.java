package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProgressUpdateRequest(
    // اختياري الآن: نسبة التقدّم تُحسب في الخادم من إكمال الدروس. يُبقى الحقل
    // للتوافق مع أي مستدعٍ قديم، ويُطبَّق فقط إن أُرسل.
    @Min(value = 0, message = "{nglp.progress.percentage.range}")
    @Max(value = 100, message = "{nglp.progress.percentage.range}")
    Integer progressPercentage,

    Long lastWatchedLessonId
) {}
