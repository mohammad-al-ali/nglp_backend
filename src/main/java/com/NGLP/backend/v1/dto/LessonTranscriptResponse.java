package com.NGLP.backend.v1.dto;

import java.util.List;

/**
 * استجابة تفريغ درس بلغة واحدة، مع قائمة اللغات المتوفرة كي تعرف الواجهة
 * أي مبدّلات لغة تُظهر.
 */
public record LessonTranscriptResponse(
        Long lessonId,
        String language,                 // "ar" | "en"
        boolean available,               // false إذا لا مقاطع لهذه اللغة بعد
        List<String> availableLanguages, // مثل ["ar"] أو ["ar","en"]
        List<Segment> segments
) {
    public record Segment(int index, int startSecond, int endSecond, String text) {}
}
