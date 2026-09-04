package com.NGLP.backend.v1;

import com.NGLP.backend.v1.entity.Enrollment;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.LessonProgress;
import com.NGLP.backend.v1.entity.QuizAttempt;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.EnrollmentRepo;
import com.NGLP.backend.v1.repo.LessonProgressRepo;
import com.NGLP.backend.v1.repo.LessonRepo;
import com.NGLP.backend.v1.repo.QuizAttemptRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import com.NGLP.backend.v1.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * هجرة بيانات لمرة واحدة (idempotent) لدعم لوحة الطالب الجديدة. تعمل بعد
 * {@link DataInitializer} (Order 1). كل خطوة محميّة بشرط "إن كان ناقصاً":
 *   1. ملء الطوابع الزمنية الناقصة (users.createdAt، enrollments.enrolledAt/lastActivityAt).
 *   2. اشتقاق صفوف {@link LessonProgress} للتسجيلات التي لا تملك أي صف — من ترتيب
 *      آخر درس مُشاهَد، أو من نسبة التقدّم المبذورة.
 *   3. إعادة حساب scorePercentage/maxScore للمحاولات القديمة.
 *
 * قابل للعكس: احذف جدول lesson_progress وأعد التشغيل.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DashboardBackfillRunner implements CommandLineRunner {

    private final UserRepo userRepo;
    private final EnrollmentRepo enrollmentRepo;
    private final LessonRepo lessonRepo;
    private final LessonProgressRepo lessonProgressRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final EnrollmentService enrollmentService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🩹 DashboardBackfillRunner: بدء هجرة بيانات لوحة الطالب...");
        backfillUserTimestamps();
        backfillEnrollmentTimestamps();
        backfillLessonProgress();
        backfillQuizScorePercentages();
        log.info("✅ DashboardBackfillRunner: اكتملت الهجرة.");
    }

    private void backfillUserTimestamps() {
        List<User> users = userRepo.findAll();
        int fixed = 0;
        for (User u : users) {
            if (u.getCreatedAt() == null) {
                u.setCreatedAt(LocalDateTime.now());
                userRepo.save(u);
                fixed++;
            }
        }
        if (fixed > 0) log.info("   • users.createdAt: عُيّن لـ {} مستخدم", fixed);
    }

    private void backfillEnrollmentTimestamps() {
        List<Enrollment> all = enrollmentRepo.findAll();
        int fixed = 0;
        for (Enrollment e : all) {
            boolean touched = false;
            if (e.getEnrolledAt() == null) { e.setEnrolledAt(LocalDateTime.now()); touched = true; }
            if (e.getLastActivityAt() == null && e.getLastWatchedLesson() != null) {
                e.setLastActivityAt(LocalDateTime.now());
                touched = true;
            }
            if (touched) { enrollmentRepo.save(e); fixed++; }
        }
        if (fixed > 0) log.info("   • enrollments timestamps: عُيّنت لـ {} تسجيل", fixed);
    }

    private void backfillLessonProgress() {
        List<Enrollment> all = enrollmentRepo.findAll();
        int seededEnrollments = 0;
        for (Enrollment e : all) {
            if (!lessonProgressRepo.findByEnrollmentId(e.getId()).isEmpty()) continue;

            List<Lesson> lessons = lessonRepo.findByCourseId(e.getCourse().getId());
            if (lessons.isEmpty()) continue;

            int completeUpTo; // عدد الدروس القيادية التي تُعلَّم كمكتملة
            if (e.getLastWatchedLesson() != null) {
                int idx = indexOfLesson(lessons, e.getLastWatchedLesson().getId());
                completeUpTo = idx >= 0 ? idx + 1 : 0;
            } else if (e.getProgressPercentage() != null && e.getProgressPercentage() > 0) {
                completeUpTo = (int) Math.round(e.getProgressPercentage() / 100.0 * lessons.size());
            } else {
                completeUpTo = 0;
            }
            completeUpTo = Math.min(completeUpTo, lessons.size());

            LocalDateTime when = e.getEnrolledAt() != null ? e.getEnrolledAt() : LocalDateTime.now();
            for (int i = 0; i < completeUpTo; i++) {
                lessonProgressRepo.save(LessonProgress.builder()
                        .enrollment(e)
                        .lesson(lessons.get(i))
                        .completed(true)
                        .completedAt(when)
                        .build());
            }
            enrollmentService.recomputeProgress(e);
            seededEnrollments++;
        }
        if (seededEnrollments > 0) log.info("   • lesson_progress: اشتُقّ لـ {} تسجيل", seededEnrollments);
    }

    private int indexOfLesson(List<Lesson> lessons, Long lessonId) {
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lessonId)) return i;
        }
        return -1;
    }

    private void backfillQuizScorePercentages() {
        List<QuizAttempt> stale = quizAttemptRepo.findByScorePercentageIsNullAndSubmittedAtIsNotNull();
        for (QuizAttempt qa : stale) {
            int max = (qa.getQuiz() == null || qa.getQuiz().getQuestions() == null) ? 0
                    : qa.getQuiz().getQuestions().stream()
                        .mapToInt(q -> q.getDifficultyWeight() != null ? q.getDifficultyWeight() : 0)
                        .sum();
            int score = qa.getScore() == null ? 0 : qa.getScore();
            qa.setMaxScore(max);
            qa.setScorePercentage(max == 0 ? 0 : (int) Math.round(score * 100.0 / max));
            quizAttemptRepo.save(qa);
        }
        if (!stale.isEmpty()) log.info("   • quiz_attempts.scorePercentage: أُعيد حساب {} محاولة", stale.size());
    }
}
