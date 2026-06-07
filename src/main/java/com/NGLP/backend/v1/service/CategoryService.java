package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Category;
import com.NGLP.backend.v1.repo.CategoryRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepo categoryRepo;

    public List<Category> findRootCategories() {
        return categoryRepo.findByParentIsNull();
    }

    // 2. دالة جديدة لجلب الأقسام الفرعية لقسم معين
    public List<Category> findSubCategories(Long parentId) {
        return categoryRepo.findByParentId(parentId);
    }

    public Category findById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category With this id "+ id));
    }

    public Category create(Category category) {
        return categoryRepo.save(category);
    }

    public Category update(Long id, Category category) {
        return categoryRepo.findById(id).map(existing -> {

            existing.setName(category.getName());
            existing.setParent(category.getParent());
            return categoryRepo.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Category Not found with this id"+ id));
    }

    public void delete(Long id) {
        // التحقق قبل الحذف: هل يوجد أقسام فرعية تعتمد على هذا القسم؟
        List<Category> subCategories = categoryRepo.findByParentId(id);
        if (!subCategories.isEmpty()) {
            throw new IllegalStateException("Can not delete parent category");
        }
        categoryRepo.deleteById(id);
    }
}
