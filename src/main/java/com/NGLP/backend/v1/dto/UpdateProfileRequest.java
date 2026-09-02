package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * جسم طلب تحديث الملف الشخصي — كلمة المرور اختيارية (تُترك فارغة إذا لم يُرد المستخدم تغييرها).
 */
public record UpdateProfileRequest(
    @NotBlank(message = "{nglp.user.fullName.required}")
    @Size(min = 2, max = 80, message = "{nglp.user.fullName.size}")
    String fullName,

    @NotBlank(message = "{nglp.user.email.required}")
    @Email(message = "{nglp.user.email.invalid}")
    @Size(max = 190, message = "{nglp.user.email.size}")
    String email,

    @Size(min = 6, max = 64, message = "{nglp.user.password.size}")
    String password
) {}
