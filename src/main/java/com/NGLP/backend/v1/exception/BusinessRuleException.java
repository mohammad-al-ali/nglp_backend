package com.NGLP.backend.v1.exception;

import org.springframework.http.HttpStatus;

/**
 * يُرمى عند مخالفة قاعدة عمل (Business Rule) — مثل حذف تصنيف رئيسي له تصنيفات فرعية،
 * أو نشر اختبار بلا أسئلة، أو تسليم محاولة اختبار مرتين.
 * يترجمه {@link GlobalExceptionHandler} إلى {@link ApiError} بالحالة المحددة (افتراضياً 409).
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public BusinessRuleException(String message) {
        this(message, HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE);
    }

    public BusinessRuleException(String message, HttpStatus status) {
        this(message, status, ErrorCode.BUSINESS_RULE);
    }

    public BusinessRuleException(String message, HttpStatus status, ErrorCode code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }
}
