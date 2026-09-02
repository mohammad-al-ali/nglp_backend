package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * جسم طلب إنشاء حساب جديد — مع تحقق كامل من صحة المدخلات.
 */
public record RegisterRequest(
    @NotBlank(message = "{nglp.user.fullName.required}")
    @Size(min = 2, max = 80, message = "{nglp.user.fullName.size}")
    String fullName,

    @NotBlank(message = "{nglp.user.email.required}")
    @Email(message = "{nglp.user.email.invalid}")
    @Size(max = 190, message = "{nglp.user.email.size}")
    String email,

    @NotBlank(message = "{nglp.user.password.required}")
    @Size(min = 6, max = 64, message = "{nglp.user.password.size}")
    String password,

    @NotNull(message = "{nglp.user.role.required}")
    Long roleId
) {}
