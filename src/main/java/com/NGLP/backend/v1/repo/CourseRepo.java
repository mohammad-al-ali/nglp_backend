package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepo extends JpaRepository<Course, Long> {
    // جلب كورسات قسم معين
    List<Course> findByCategoryId(Long categoryId);
    List<Course> findByTeacherId(Long teacherId);
}
