package com.NGLP.backend.v1.exception;

/**
 * يُرمى عندما يرفع المستخدم ملفاً بنوع/امتداد غير مسموح (صورة غير صالحة، فيديو بصيغة غير مدعومة...).
 * يترجمه {@link GlobalExceptionHandler} إلى استجابة 400 بشكل {@link ApiError}.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
