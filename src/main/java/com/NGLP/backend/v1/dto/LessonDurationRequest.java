package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * جسم طلب ضبط مدة الدرس. يرسله الواجهة الأمامية عندما يكتشف عنصر &lt;video&gt; مدته
 * الحقيقية لدرس رُفع قبل استخراج المدة تلقائياً. الخادم يطبّقها فقط إن كانت المدة
 * الحالية مجهولة (self-heal).
 */
public record LessonDurationRequest(
        @NotNull(message = "{nglp.lesson.duration.positive}")
        @Positive(message = "{nglp.lesson.duration.positive}")
        Integer durationSeconds
) {}
