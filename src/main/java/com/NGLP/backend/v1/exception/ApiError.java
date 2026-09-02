package com.NGLP.backend.v1.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * الشكل الموحّد لكل استجابات الخطأ في الـ API.
 *
 * <pre>
 * {
 *   "timestamp": "2026-09-03T12:00:00Z",
 *   "status": 400,
 *   "code": "VALIDATION_ERROR",
 *   "message": "بيانات الطلب غير صالحة، يرجى مراجعة الحقول.",
 *   "path": "/api/v1/auth/register",
 *   "fieldErrors": [
 *     { "field": "email", "message": "صيغة البريد الإلكتروني غير صحيحة." }
 *   ]
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}

    public static ApiError of(int status, ErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), status, code.name(), message, path, new ArrayList<>());
    }

    public static ApiError of(int status, ErrorCode code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code.name(), message, path,
                fieldErrors != null ? fieldErrors : new ArrayList<>());
    }
}
