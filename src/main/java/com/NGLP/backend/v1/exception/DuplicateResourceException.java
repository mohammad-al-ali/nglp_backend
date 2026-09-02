package com.NGLP.backend.v1.exception;

/**
 * يُرمى عند محاولة إنشاء مورد يخالف قيد تفرّد (مثل بريد إلكتروني مستخدم مسبقاً).
 * يترجمه {@link GlobalExceptionHandler} إلى استجابة 409 بشكل {@link ApiError}.
 */
public class DuplicateResourceException extends RuntimeException {

    private final ErrorCode code;

    public DuplicateResourceException(String message) {
        this(message, ErrorCode.DUPLICATE_RESOURCE);
    }

    public DuplicateResourceException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
