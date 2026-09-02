package com.NGLP.backend.v1.exception;

/**
 * رموز الأخطاء الموحّدة التي يعتمد عليها الفرونت إند لتمييز نوع الخطأ برمجياً،
 * بينما تبقى الرسالة النصية (بالعربية) للعرض المباشر للمستخدم.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    DUPLICATE_EMAIL,
    DUPLICATE_RESOURCE,
    INVALID_CREDENTIALS,
    RESOURCE_NOT_FOUND,
    BUSINESS_RULE,
    FILE_TOO_LARGE,
    UNSUPPORTED_FILE_TYPE,
    MISSING_FILE,
    FORBIDDEN,
    MALFORMED_REQUEST,
    INTERNAL_ERROR
}
