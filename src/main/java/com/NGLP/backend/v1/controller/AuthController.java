package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.LoginRequest;
import com.NGLP.backend.v1.dto.RegisterRequest;
import com.NGLP.backend.v1.entity.Role;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.exception.BusinessRuleException;
import com.NGLP.backend.v1.exception.DuplicateResourceException;
import com.NGLP.backend.v1.exception.ErrorCode;
import com.NGLP.backend.v1.repo.RoleRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import com.NGLP.backend.v1.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------
    // 1) التسجيل (Register)
    // -------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        if (userRepo.existsByEmail(request.email())) {
            throw new DuplicateResourceException("هذا البريد الإلكتروني مستخدم بالفعل.", ErrorCode.DUPLICATE_EMAIL);
        }

        Role role = roleRepo.findById(request.roleId())
                .orElseThrow(() -> new EntityNotFoundException("الدور المطلوب غير موجود: " + request.roleId()));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setBlocked(false);

        userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "email", user.getEmail(),
                        "role", user.getRole().getName()
                )
        );
    }

    // -------------------------------
    // 2) تسجيل الدخول (Login)
    // -------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User user = userRepo.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BusinessRuleException(
                        "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
                        HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS));

        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new BusinessRuleException("هذا الحساب محظور، يرجى التواصل مع الإدارة.",
                    HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_BLOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessRuleException(
                    "البريد الإلكتروني أو كلمة المرور غير صحيحة.",
                    HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);
        }

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "email", user.getEmail(),
                        "role", user.getRole().getName()
                )
        );
    }

    // -------------------------------
    // 3) جلب بيانات المستخدم (Me)
    // -------------------------------
    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestParam Long userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }
}
