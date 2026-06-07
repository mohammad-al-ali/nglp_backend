package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Category;
import com.NGLP.backend.v1.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * متحكم إدارة تصنيفات الدروس والكورسات (CategoryController)
 * تم تأمين مسارات التعديل والكتابة لضمان تعديل شجرة المنهج الأكاديمي بواسطة المشرفين فقط.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * جلب التصنيفات الرئيسية الفارغة من الأب (متاحة للجميع لعرض الفهرس)
     */
    @GetMapping("/root")
    public ResponseEntity<List<Category>> getRootCategories() {
        return ResponseEntity.ok(categoryService.findRootCategories());
    }

    /**
     * جلب التصنيفات الفرعية لقسم رئيسي محدد (متاحة للجميع)
     */
    @GetMapping("/{parentId}/sub")
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(categoryService.findSubCategories(parentId));
    }

    /**
     * جلب تصنيف محدد بالمعرف
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    /**
     * إنشاء تصنيف جديد (مؤمن للمشرفين فقط)
     */
    @PostMapping
    public ResponseEntity<Category> create(
            @RequestBody Category category,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminAccess(userRole);
        Category created = categoryService.create(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * تحديث تصنيف أو إعادة تسميته (مؤمن للمشرفين فقط)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> update(
            @PathVariable Long id,
            @RequestBody Category category,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        validateAdminAccess(userRole);
        return ResponseEntity.ok(categoryService.update(id, category));
    }

    /**
     * حذف تصنيف بالكامل (مؤمن للمشرفين فقط)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        validateAdminAccess(userRole);
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * دالة التحقق الأكاديمي لمنع وصول غير المشرفين لمسارات الكتابة والتعديل
     */
    private void validateAdminAccess(String userRole) {
        if (userRole == null || !userRole.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "عذراً، لا تمتلك صلاحيات المشرف لتعديل هيكل التصنيفات التعليمي!");
        }
    }
}
