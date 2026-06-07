package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepo extends JpaRepository<Category, Long> {
        // لجلب الأقسام الرئيسية فقط
        List<Category> findByParentIsNull();
        // لجلب الأقسام الفرعية لقسم معين
        List<Category> findByParentId(Long parentId);
}
