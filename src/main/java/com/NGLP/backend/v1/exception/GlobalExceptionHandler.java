package com.NGLP.backend.v1.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * معالج الاستثناءات المركزي — كل خطأ يخرج من الـ API بشكل {@link ApiError} موحّد،
 * برسالة عربية جاهزة للعرض للمستخدم، ورمز {@code code} برمجي للفرونت إند.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------
    // 1) أخطاء التحقق من صحة المدخلات (Bean Validation)
    // ---------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()));
        }
        e.getBindingResult().getGlobalErrors().forEach(ge ->
                fieldErrors.add(new ApiError.FieldError(ge.getObjectName(), ge.getDefaultMessage())));

        log.warn("400 - تحقق فاشل على {}: {}", req.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "بيانات الطلب غير صالحة، يرجى مراجعة الحقول المميّزة.", req, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException e, HttpServletRequest req) {
        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        e.getParameterValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err ->
                    fieldErrors.add(new ApiError.FieldError(field, err.getDefaultMessage())));
        });
        log.warn("400 - تحقق فاشل على معاملات {}: {}", req.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "بعض قيم الطلب غير صالحة.", req, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.add(new ApiError.FieldError(field, v.getMessage()));
        }
        log.warn("400 - انتهاك قيود على {}: {}", req.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "بعض قيم الطلب غير صالحة.", req, fieldErrors);
    }

    // ---------------------------------------------------------------
    // 2) أخطاء صياغة الطلب
    // ---------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        log.warn("400 - جسم طلب غير قابل للقراءة على {}: {}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "صيغة الطلب غير صالحة أو ناقصة.", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        log.warn("400 - نوع معامل غير متوقع على {}: {}", req.getRequestURI(), e.getName());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "قيمة المعامل «" + e.getName() + "» غير صالحة.", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "المعامل المطلوب «" + e.getParameterName() + "» مفقود.", req,
                List.of(new ApiError.FieldError(e.getParameterName(), "هذا المعامل مطلوب.")));
    }

    // ---------------------------------------------------------------
    // 3) رفع الملفات
    // ---------------------------------------------------------------

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_FILE,
                "الجزء المطلوب «" + e.getRequestPartName() + "» مفقود من الطلب.", req,
                List.of(new ApiError.FieldError(e.getRequestPartName(), "هذا الحقل مطلوب.")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException e, HttpServletRequest req) {
        log.warn("413 - حجم رفع يتجاوز الحد على {}", req.getRequestURI());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.FILE_TOO_LARGE,
                "حجم الملف يتجاوز الحد المسموح (500 ميغابايت).", req, null);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiError> handleBadFileType(UnsupportedFileTypeException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.UNSUPPORTED_FILE_TYPE, e.getMessage(), req, null);
    }

    // ---------------------------------------------------------------
    // 4) قواعد العمل والموارد
    // ---------------------------------------------------------------

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException e, HttpServletRequest req) {
        log.warn("409 - مورد مكرر على {}: {}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.CONFLICT, e.getCode(), e.getMessage(), req, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException e, HttpServletRequest req) {
        log.warn("{} - مخالفة قاعدة عمل على {}: {}", e.getStatus().value(), req.getRequestURI(), e.getMessage());
        return build(e.getStatus(), e.getCode(), e.getMessage(), req, null);
    }

    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException e, HttpServletRequest req) {
        log.warn("404 - المورد غير موجود على {}: {}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                "المورد المطلوب غير موجود.", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("409 - انتهاك سلامة بيانات على {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE,
                "لا يمكن تنفيذ العملية لأنها تخالف قيود البيانات (قد يكون العنصر مرتبطاً بعناصر أخرى).", req, null);
    }

    // ---------------------------------------------------------------
    // 5) توافقية مع الكود القديم + احتياطي
    // ---------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadArgument(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("400 - مدخل غير صالح على {}: {}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                arabicOr(e.getMessage(), "قيمة غير صالحة في الطلب."), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleBadState(IllegalStateException e, HttpServletRequest req) {
        log.warn("409 - حالة غير صالحة على {}: {}", req.getRequestURI(), e.getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE,
                arabicOr(e.getMessage(), "لا يمكن تنفيذ هذه العملية في الوضع الحالي."), req, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException e, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        ErrorCode code = switch (status) {
            case FORBIDDEN, UNAUTHORIZED -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> ErrorCode.BUSINESS_RULE;
            default -> ErrorCode.VALIDATION_ERROR;
        };
        String message = arabicOr(e.getReason(), "تعذّر تنفيذ الطلب.");
        log.warn("{} - {} على {}", status.value(), message, req.getRequestURI());
        return build(status, code, message, req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception e, HttpServletRequest req) {
        log.error("500 - خطأ داخلي غير متوقع على {}: ", req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "حدث خطأ غير متوقع في الخادم، يرجى المحاولة لاحقاً.", req, null);
    }

    // ---------------------------------------------------------------
    // أدوات مساعدة
    // ---------------------------------------------------------------

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                           HttpServletRequest req, List<ApiError.FieldError> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), code, message, req.getRequestURI(), fieldErrors));
    }

    /** يستخدم الرسالة الأصلية إن كانت تحوي حروفاً عربية، وإلا يستبدلها برسالة عربية عامة. */
    private String arabicOr(String original, String fallback) {
        if (original == null || original.isBlank()) return fallback;
        boolean hasArabic = original.chars().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
        return hasArabic ? original : fallback;
    }
}
