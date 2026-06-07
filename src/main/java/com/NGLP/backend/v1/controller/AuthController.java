package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.AuthRequest;
import com.NGLP.backend.v1.dto.AuthResponse;
import com.NGLP.backend.v1.entity.Role;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.RoleRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import com.NGLP.backend.v1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    // DTOs
    record LoginRequest(String email, String password) {}
    record RegisterRequest(String fullName, String email, String password, Role role) {}

    // -------------------------------
    // 1) التسجيل (Register)
    // -------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // التحقق من وجود الإيميل مسبقاً
        if (userRepo.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "الإيميل مستخدم بالفعل!")
            );
        }

        // التحقق من وجود الدور
        var role = roleRepo.findById(request.role.getId())
                .orElseThrow(() -> new RuntimeException("الدور غير موجود: " + request.role.getId()));

        // إنشاء المستخدم
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        userRepo.save(user);

        return ResponseEntity.ok(
                Map.of("message", "تم التسجيل بنجاح")
        );
    }

    // -------------------------------
    // 2) تسجيل الدخول (Login)
    // -------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("الإيميل غير صحيح"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "كلمة المرور خاطئة")
            );
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
