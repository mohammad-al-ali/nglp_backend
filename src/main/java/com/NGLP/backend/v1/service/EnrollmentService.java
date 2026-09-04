package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.dto.ProgressUpdateRequest;
import com.NGLP.backend.v1.entity.Course;
import com.NGLP.backend.v1.entity.Enrollment;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.CourseRepo;
import com.NGLP.backend.v1.repo.EnrollmentRepo;
import com.NGLP.backend.v1.repo.LessonProgressRepo;
import com.NGLP.backend.v1.repo.LessonRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepo enrollmentRepo;
    private final UserRepo userRepo;
    private final CourseRepo courseRepo;
    private final LessonRepo lessonRepo;
    private final LessonProgressRepo lessonProgressRepo;

    public List<Enrollment> findByUser(Long userId) {
        return enrollmentRepo.findByUserId(userId);
    }

    public Enrollment enroll(Long userId, Long courseId) {
        return enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> {
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));
                    Course course = courseRepo.findById(courseId)
                            .orElseThrow(() -> new EntityNotFoundException("Course not found with id " + courseId));

                    Enrollment enrollment = Enrollment.builder()
                            .user(user)
                            .course(course)
                            .progressPercentage(0)
                            .build();
                    return enrollmentRepo.save(enrollment);
                });
    }

    public Enrollment updateProgress(Long id, ProgressUpdateRequest request) {
        Enrollment enrollment = enrollmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found with id " + id));

        if (request.progressPercentage() != null) {
            enrollment.setProgressPercentage(Math.max(0, Math.min(100, request.progressPercentage())));
        }

        if (request.lastWatchedLessonId() != null) {
            Lesson lesson = lessonRepo.findById(request.lastWatchedLessonId())
                    .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id " + request.lastWatchedLessonId()));
            enrollment.setLastWatchedLesson(lesson);
            enrollment.setLastActivityAt(LocalDateTime.now());
        }

        return enrollmentRepo.save(enrollment);
    }

    /**
     * يعيد حساب نسبة تقدّم الكورس = الدروس المكتملة ÷ إجمالي دروس الكورس.
     * يُستدعى من {@code LessonProgressService} بعد كل تغيير على حالة الإكمال.
     */
    @Transactional
    public Enrollment recomputeProgress(Enrollment enrollment) {
        long total = lessonRepo.countByCourseId(enrollment.getCourse().getId());
        long done = lessonProgressRepo.countByEnrollmentIdAndCompletedTrue(enrollment.getId());
        int percent = total == 0 ? 0 : (int) Math.min(100, Math.round(done * 100.0 / total));
        enrollment.setProgressPercentage(percent);
        return enrollmentRepo.save(enrollment);
    }
}
