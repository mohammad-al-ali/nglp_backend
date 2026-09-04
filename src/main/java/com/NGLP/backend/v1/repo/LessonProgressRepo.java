package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.LessonProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepo extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    List<LessonProgress> findByEnrollmentId(Long enrollmentId);

    boolean existsByLessonId(Long lessonId);

    long countByEnrollmentIdAndCompletedTrue(Long enrollmentId);

    long countByEnrollment_User_IdAndCompletedTrue(Long userId);

    long countByEnrollment_User_Id(Long userId);

    /** [enrollmentId, completedCount] لكل تسجيلات الطالب — لحساب تقدّم كل كورس دفعةً واحدة. */
    @Query("SELECT lp.enrollment.id, COUNT(lp) FROM LessonProgress lp " +
            "WHERE lp.enrollment.user.id = :userId AND lp.completed = true " +
            "GROUP BY lp.enrollment.id")
    List<Object[]> countCompletedByEnrollmentForUser(@Param("userId") Long userId);

    /** إجمالي ثواني الدروس المكتملة عبر كل كورسات الطالب (وقت التعلّم الفعلي). */
    @Query("SELECT COALESCE(SUM(lp.lesson.durationSeconds), 0) FROM LessonProgress lp " +
            "WHERE lp.enrollment.user.id = :userId AND lp.completed = true")
    long sumCompletedLessonSecondsForUser(@Param("userId") Long userId);

    /** أحدث الدروس المكتملة (لسجلّ النشاط الأخير). */
    @Query("SELECT lp FROM LessonProgress lp " +
            "JOIN FETCH lp.lesson l JOIN FETCH lp.enrollment e JOIN FETCH e.course c " +
            "WHERE e.user.id = :userId AND lp.completed = true AND lp.completedAt IS NOT NULL " +
            "ORDER BY lp.completedAt DESC")
    List<LessonProgress> findRecentCompletions(@Param("userId") Long userId, Pageable pageable);
}
