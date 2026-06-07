package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.dto.ProgressUpdateRequest;
import com.NGLP.backend.v1.entity.Course;
import com.NGLP.backend.v1.entity.Enrollment;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.CourseRepo;
import com.NGLP.backend.v1.repo.EnrollmentRepo;
import com.NGLP.backend.v1.repo.LessonRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepo enrollmentRepo;
    private final UserRepo userRepo;
    private final CourseRepo courseRepo;
    private final LessonRepo lessonRepo;

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
        }

        return enrollmentRepo.save(enrollment);
    }
}
