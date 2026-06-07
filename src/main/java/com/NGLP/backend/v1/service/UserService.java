package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.RoleRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with this"+ id));
    }

    // دالة إنشاء الحساب (التسجيل)
    public User create(User user) {
        // 1. التحقق من أن الإيميل غير مستخدم مسبقاً
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("هذا البريد الإلكتروني مستخدم بالفعل.");
        }

        if (user.getRole() != null && user.getRole().getId() != null) {
            user.setRole(roleRepo.findById(user.getRole().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found with id " + user.getRole().getId())));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBlocked(false);

        return userRepo.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Invalid email or password"));

        if (user.getBlocked()) {
            throw new IllegalStateException("This account is blocked.");
        }

        if (rawPassword != null && rawPassword.equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            return userRepo.save(user);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return user;
    }

    // دالة تحديث الملف الشخصي (آمنة: لا تسمح بتغيير الدور أو الباسورد هنا)
    public User updateProfile(Long id, User updatedUser) {
        return userRepo.findById(id).map(existing -> {

            // إذا أراد تغيير الإيميل، يجب التأكد أن الإيميل الجديد غير مأخوذ
            if (!existing.getEmail().equals(updatedUser.getEmail()) && userRepo.existsByEmail(updatedUser.getEmail())) {
                throw new IllegalArgumentException("البريد الإلكتروني الجديد مستخدم بالفعل.");
            }

            existing.setFullName(updatedUser.getFullName());
            existing.setEmail(updatedUser.getEmail());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            return userRepo.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Usernot found with this"+ id));
    }

    public User updateAdminFields(Long id, User updatedUser) {
        return userRepo.findById(id).map(existing -> {
            if (updatedUser.getRole() != null && updatedUser.getRole().getId() != null) {
                existing.setRole(roleRepo.findById(updatedUser.getRole().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Role not found with id " + updatedUser.getRole().getId())));
            }
            existing.setBlocked(updatedUser.getBlocked());
            return userRepo.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
    }

    public void delete(Long id) {
        // ملاحظة: يُفضل في الأنظمة الحقيقية عمل Soft Delete (إخفاء المستخدم)
        // بدلاً من مسحه كلياً للحفاظ على تاريخ المحادثات والدروس.
        userRepo.deleteById(id);
    }
}

