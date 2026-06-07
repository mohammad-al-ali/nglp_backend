package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Role;
import com.NGLP.backend.v1.repo.RoleRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepo roleRepo;

    // جلب كل الأدوار (مفيدة لواجهة التسجيل لكي يختار: هل أنت طالب أم معلم؟)
    public List<Role> findAll() { return roleRepo.findAll(); }

    public Role findById(Long id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role Not found"));
    }

    // جلب الدور بالاسم (مهمة جداً عند تسجيل مستخدم جديد برمجياً)
    public Role findByName(String name) {
        return roleRepo.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Role With this name"+ name));
    }

}