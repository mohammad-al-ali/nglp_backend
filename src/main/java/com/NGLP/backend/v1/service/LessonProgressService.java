package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Enrollment;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.LessonProgress;
import com.NGLP.backend.v1.repo.EnrollmentRepo;
import com.NGLP.backend.v1.repo.LessonProgressRepo;
import com.NGLP.backend.v1.repo.LessonRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * تتبّع إكمال الدروس على مستوى الدرس الواحد. كل تغيير يعيد حساب نسبة تقدّم الكورس
 * في {@link EnrollmentService#recomputeProgress}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final LessonRepo lessonRepo;
    private final LessonProgressRepo lessonProgressRepo;
    private final EnrollmentRepo enrollmentRepo;
    private final EnrollmentService enrollmentService;

    @Transactional
    public Enrollment setCompletion(Long userId, Long lessonId, boolean completed) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("الدرس غير موجود: " + lessonId));

        // get-or-create — يضمن وجود تسجيل حتى لو فتح الطالب الدرس دون تسجيل صريح.
        Enrollment enrollment = enrollmentService.enroll(userId, lesson.getCourse().getId());

        LessonProgress progress;
        try {
            progress = lessonProgressRepo
                    .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                    .orElseGet(() -> LessonProgress.builder()
                            .enrollment(enrollment)
                            .lesson(lesson)
                            .build());
            progress.setCompleted(completed);
            progress.setCompletedAt(completed ? LocalDateTime.now() : null);
            lessonProgressRepo.save(progress);
        } catch (DataIntegrityViolationException race) {
            // سباق نادر (onEnded + نقرة يدوية) على قيد الفرادة — أعِد القراءة مرة واحدة.
            progress = lessonProgressRepo
                    .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                    .orElseThrow(() -> race);
            progress.setCompleted(completed);
            progress.setCompletedAt(completed ? LocalDateTime.now() : null);
            lessonProgressRepo.save(progress);
        }

        if (completed) {
            enrollment.setLastWatchedLesson(lesson);
        }
        enrollment.setLastActivityAt(LocalDateTime.now());
        return enrollmentService.recomputeProgress(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Long> completedLessonIds(Long userId, Long courseId) {
        return enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .map(e -> lessonProgressRepo.findByEnrollmentId(e.getId()).stream()
                        .filter(lp -> Boolean.TRUE.equals(lp.getCompleted()))
                        .map(lp -> lp.getLesson().getId())
                        .toList())
                .orElseGet(List::of);
    }
}
