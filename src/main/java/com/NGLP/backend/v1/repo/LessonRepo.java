package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepo extends JpaRepository<Lesson, Long> {
    // للتحقق هل يوجد دروس مرتبطة بهذا الكورس؟
    boolean existsByCourseId(Long courseId);
    // جلب دروس كورس محدد
    List<Lesson> findByCourseId(Long courseId);
}
