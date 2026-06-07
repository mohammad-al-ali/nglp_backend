package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * متحكم إدارة الأعضاء والمستخدمين (UserController)
 * تم تأمينه أكاديمياً بالكامل للتحقق من هيدر صلاحية المشرف (X-User-Role) لمنع الوصول غير المصرح.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * جلب كافة الأعضاء في النظام (مخصصة للمشرفين فقط)
     */
    @GetMapping
    public ResponseEntity<List<User>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminAccess(userRole);
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * جلب بيانات مستخدم محدد بالمعرف (تستخدم للملف الشخصي)
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * تحديث الملف الشخصي العادي (الاسم والإيميل فقط)
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateProfile(id, user));
    }

    /**
     * تحديث البيانات الإشرافية للمستخدم (ترقية الدور وحظر الحساب) - مؤمن للمشرفين فقط
     */
    @PutMapping("/{id}/admin")
    public ResponseEntity<User> updateAdminFields(
            @PathVariable Long id,
            @RequestBody User user,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminAccess(userRole);
        return ResponseEntity.ok(userService.updateAdminFields(id, user));
    }

    /**
     * حذف مستخدم بالكامل من النظام - مؤمن للمشرفين فقط
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminAccess(userRole);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * دالة مساعدة للتحقق الأكاديمي من دور المشرف لضمان الأمان البرمجي
     */
    private void validateAdminAccess(String userRole) {
        if (userRole == null || !userRole.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "عذراً، لا تمتلك الصلاحيات الكافية لإتمام هذا الإجراء الإشرافي!");
        }
    }
}
