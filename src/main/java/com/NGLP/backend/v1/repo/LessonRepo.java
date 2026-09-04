package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepo extends JpaRepository<Lesson, Long> {
    // للتحقق هل يوجد دروس مرتبطة بهذا الكورس؟
    boolean existsByCourseId(Long courseId);
    // جلب دروس كورس محدد
    List<Lesson> findByCourseId(Long courseId);

    long countByCourseId(Long courseId);

    /** [courseId, lessonCount] لمجموعة كورسات — لحساب تقدّم لوحة الطالب دفعةً واحدة. */
    @Query("SELECT l.course.id, COUNT(l) FROM Lesson l WHERE l.course.id IN :ids GROUP BY l.course.id")
    List<Object[]> countLessonsByCourseIds(@Param("ids") List<Long> ids);

    /** إجمالي مدّة كل دروس مجموعة كورسات (بالثواني). */
    @Query("SELECT COALESCE(SUM(l.durationSeconds), 0) FROM Lesson l WHERE l.course.id IN :ids")
    long sumDurationSecondsByCourseIds(@Param("ids") List<Long> ids);
}
