package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * جسم طلب تسجيل الدخول.
 */
public record LoginRequest(
    @NotBlank(message = "{nglp.user.email.required}")
    @Email(message = "{nglp.user.email.invalid}")
    String email,

    @NotBlank(message = "{nglp.user.password.required}")
    String password
) {}
