package com.NGLP.backend.v1.security;

import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.exception.ApiError;
import com.NGLP.backend.v1.exception.ErrorCode;
import com.NGLP.backend.v1.repo.UserRepo;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * نقطة الفرض الوحيدة لحالة الحظر — تُفحص على كل طلب يحمل هوية مستخدم.
 *
 * <p>ما دام النظام بلا رمز (token) ولا جلسة، فإن الهوية تصل عبر الهيدر {@code X-User-Id}
 * (أو المعامل {@code userId} في بعض المسارات). هذا المعترض يُعيد تحميل المستخدم من قاعدة
 * البيانات، فإن كان محظوراً يرفض الطلب بالرمز {@code 403} وبجسم {@link ApiError} موحّد
 * برمز {@link ErrorCode#ACCOUNT_BLOCKED}، ليتعرّف عليه الفرونت إند ويُخرج المستخدم.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockedUserInterceptor implements HandlerInterceptor {

    private static final String BLOCKED_MESSAGE = "هذا الحساب محظور، يرجى التواصل مع الإدارة.";

    private final UserRepo userRepo;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // طلب الاستكشاف (CORS Preflight) لا يحمل هوية ولا يُنفّذ عملية — نمرّره كما هو.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Long userId = resolveUserId(request);
        if (userId == null) {
            return true; // زائر غير مسجّل — لا شيء نفحصه.
        }

        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty() || !Boolean.TRUE.equals(user.get().getBlocked())) {
            // مستخدم غير موجود أو غير محظور — نترك المسار الطبيعي للمتحكّم / معالج الأخطاء.
            return true;
        }

        writeBlocked(request, response);
        return false;
    }

    /** استخراج معرّف المستخدم بنفس طريقة المتحكّمات: هيدر {@code X-User-Id} أولاً، وإلا المعامل {@code userId}. */
    private Long resolveUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        String value = (header != null && !header.isBlank()) ? header : request.getParameter("userId");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeBlocked(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCOUNT_BLOCKED,
                BLOCKED_MESSAGE, request.getRequestURI());

        log.warn("403 - حساب محظور حاول الوصول إلى {}", request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
