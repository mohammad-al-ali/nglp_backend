package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Course;
import com.NGLP.backend.v1.repo.CourseRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepo courseRepo;
    private final LessonService lessonService; // حقن للتحقق من الدروس قبل الحذف

    // 1. يمكننا الإبقاء على findAll للوحة تحكم الإدارة (Admin Dashboard)
    public List<Course> findAll() {
        return courseRepo.findAll();
    }

    // 2. الدالة الجوهرية للـ Frontend: جلب كورسات قسم محدد
    public List<Course> findCoursesByCategory(Long categoryId) {
        return courseRepo.findByCategoryId(categoryId);
    }

    public List<Course> findCoursesByTeacher(Long teacherId) {
        return courseRepo.findByTeacherId(teacherId);
    }

    public Course findById(Long id) {
        return courseRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Course not found with this id"+ id));
    }

    public Course create(Course course) {
        return courseRepo.save(course);
    }

    public Course update(Long id, Course course) {
        return courseRepo.findById(id).map(existing -> {
            existing.setTitle(course.getTitle());
            existing.setDescription(course.getDescription());
            existing.setCategory(course.getCategory());
            existing.setTeacher(course.getTeacher());
            return courseRepo.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Course not found id"+ id));
    }

    // 3. الحذف الآمن (Safe Delete)
    public void delete(Long id) {
        // التحقق قبل الحذف: هل الكورس يحتوي على دروس؟
        if (lessonService.existsByCourseId(id)) {
            throw new IllegalStateException("This Course has Lessons");
        }
        courseRepo.deleteById(id);
    }
}