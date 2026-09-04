package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepo extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserId(Long userId);
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    /** تسجيلات الطالب مع الكورس/التصنيف/آخر درس مُشاهَد مُحمَّلة مسبقاً (لتفادي LazyInitialization في اللوحة). */
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c LEFT JOIN FETCH c.category " +
            "LEFT JOIN FETCH c.teacher LEFT JOIN FETCH e.lastWatchedLesson " +
            "WHERE e.user.id = :userId")
    List<Enrollment> findByUserIdWithCourse(@Param("userId") Long userId);

    @Query("SELECT e.course.id FROM Enrollment e WHERE e.user.id = :userId")
    List<Long> findEnrolledCourseIds(@Param("userId") Long userId);
}
