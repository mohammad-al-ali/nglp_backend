package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Category;
import com.NGLP.backend.v1.exception.BusinessRuleException;
import com.NGLP.backend.v1.repo.CategoryRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepo categoryRepo;
    private final FileStorageService fileStorageService;

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

    public Category uploadImage(Long id, MultipartFile image) {
        Category category = findById(id);
        String imageUrl = fileStorageService.saveImage(image);
        category.setImageUrl(imageUrl);
        return categoryRepo.save(category);
    }

    public void delete(Long id) {
        // التحقق قبل الحذف: هل يوجد أقسام فرعية تعتمد على هذا القسم؟
        List<Category> subCategories = categoryRepo.findByParentId(id);
        if (!subCategories.isEmpty()) {
            throw new BusinessRuleException("لا يمكن حذف هذا التصنيف لأنه يحتوي على تصنيفات فرعية. احذف التصنيفات الفرعية أولاً.");
        }
        categoryRepo.deleteById(id);
    }
}
