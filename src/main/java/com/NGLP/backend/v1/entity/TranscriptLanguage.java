package com.NGLP.backend.v1.entity;

/**
 * لغة مقطع التفريغ. المنصّة تدعم العربية والإنكليزية فقط لمحتوى التفريغ.
 */
public enum TranscriptLanguage {
    AR,
    EN;

    /** يحوّل كود لغة نصياً ("ar" / "AR" / "en" ...) إلى القيمة، أو {@code null} إن كان غير مدعوم. */
    public static TranscriptLanguage fromCode(String code) {
        if (code == null) return null;
        String normalized = code.trim().toLowerCase();
        return switch (normalized) {
            case "ar", "ara", "arabic" -> AR;
            case "en", "eng", "english" -> EN;
            default -> null;
        };
    }

    /** كود اللغة بأحرف صغيرة كما يتوقعه الفرونت إند ("ar" / "en"). */
    public String code() {
        return name().toLowerCase();
    }
}
