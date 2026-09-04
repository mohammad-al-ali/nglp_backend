package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepo extends JpaRepository<Course, Long> {
    // جلب كورسات قسم معين
    List<Course> findByCategoryId(Long categoryId);
    List<Course> findByTeacherId(Long teacherId);

    /**
     * كورسات مقترحة: غير مسجَّل بها الطالب، وتحوي درساً واحداً على الأقل، مع
     * تقديم كورسات تخصّصه (تصنيفاته الحالية). التصنيف/المعلّم مُحمَّلان مسبقاً.
     * ملاحظة: قوائم :excludeIds و :preferredCategoryIds يجب ألا تكون فارغة
     * (JPQL لا يقبل IN ()) — الخدمة تضع List.of(-1L) عند الفراغ.
     */
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.category cat LEFT JOIN FETCH c.teacher " +
            "WHERE c.id NOT IN :excludeIds AND SIZE(c.lessons) > 0 " +
            "ORDER BY CASE WHEN cat.id IN :preferredCategoryIds THEN 0 ELSE 1 END, c.id DESC")
    List<Course> findRecommendations(@Param("excludeIds") List<Long> excludeIds,
                                     @Param("preferredCategoryIds") List<Long> preferredCategoryIds,
                                     Pageable pageable);
}
