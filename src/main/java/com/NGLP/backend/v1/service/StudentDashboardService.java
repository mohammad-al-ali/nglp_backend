package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.dto.StudentDashboardResponse;
import com.NGLP.backend.v1.dto.StudentDashboardResponse.*;
import com.NGLP.backend.v1.entity.*;
import com.NGLP.backend.v1.repo.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardService {

    private static final List<Long> NONE = List.of(-1L);

    private final UserRepo userRepo;
    private final EnrollmentRepo enrollmentRepo;
    private final LessonRepo lessonRepo;
    private final LessonProgressRepo lessonProgressRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final MsgRepo msgRepo;
    private final ConversationRepo conversationRepo;
    private final CourseRepo courseRepo;

    @Transactional(readOnly = true)
    public StudentDashboardResponse build(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("المستخدم غير موجود: " + userId));

        List<Enrollment> enrollments = enrollmentRepo.findByUserIdWithCourse(userId);
        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();

        long quizzesTaken = 0;
        int avgQuizScore = 0;
        List<Object[]> quizStats = quizAttemptRepo.statsForStudent(userId);
        if (!quizStats.isEmpty() && quizStats.get(0) != null) {
            Object[] row = quizStats.get(0);
            quizzesTaken = ((Number) row[0]).longValue();
            avgQuizScore = (int) Math.round(((Number) row[1]).doubleValue());
        }
        long aiQuestions = msgRepo.countUserMessagesByUser(userId);
        long aiSessions = conversationRepo.countActiveByUserId(userId);

        // ---- الحالة الفارغة: لا تسجيلات ----
        if (enrollments.isEmpty()) {
            Summary empty = new Summary(0, 0, 0, 0, 0, 0, 0, 0,
                    quizzesTaken, avgQuizScore, aiQuestions, aiSessions, user.getCreatedAt());
            return new StudentDashboardResponse(empty, null, List.of(), List.of(),
                    new QuizPerformance(quizzesTaken, avgQuizScore, List.of()),
                    buildRecommendations(NONE, NONE));
        }

        Map<Long, Long> lessonsPerCourse = toMap(lessonRepo.countLessonsByCourseIds(courseIds));
        Map<Long, Long> completedPerEnrollment = toMap(lessonProgressRepo.countCompletedByEnrollmentForUser(userId));

        // ---- courses[] + إعادة حساب النِّسَب دفاعياً ----
        List<CourseProgress> courses = new ArrayList<>();
        int sumPercent = 0, inProgress = 0, completed = 0;
        for (Enrollment e : enrollments) {
            Course c = e.getCourse();
            long total = lessonsPerCourse.getOrDefault(c.getId(), 0L);
            long done = completedPerEnrollment.getOrDefault(e.getId(), 0L);
            int percent = total == 0 ? 0 : (int) Math.min(100, Math.round(done * 100.0 / total));
            sumPercent += percent;
            if (percent >= 100) completed++;
            else if (percent > 0) inProgress++;

            courses.add(new CourseProgress(
                    c.getId(), c.getTitle(), c.getDescription(),
                    c.getCategory() != null ? c.getCategory().getName() : null,
                    c.getImageUrl(),
                    c.getTeacher() != null ? c.getTeacher().getFullName() : null,
                    percent, done, total,
                    e.getLastWatchedLesson() != null ? e.getLastWatchedLesson().getId() : null));
        }
        int overallPercent = Math.round((float) sumPercent / enrollments.size());

        Summary summary = new Summary(
                enrollments.size(), inProgress, completed, overallPercent,
                lessonProgressRepo.countByEnrollment_User_IdAndCompletedTrue(userId),
                lessonsPerCourse.values().stream().mapToLong(Long::longValue).sum(),
                lessonProgressRepo.sumCompletedLessonSecondsForUser(userId),
                lessonRepo.sumDurationSecondsByCourseIds(courseIds),
                quizzesTaken, avgQuizScore, aiQuestions, aiSessions, user.getCreatedAt());

        // ---- resumeLearning ----
        ResumeLearning resume = buildResume(enrollments, courses);

        // ---- recentActivity ----
        List<ActivityItem> activity = buildActivity(userId, enrollments);

        // ---- quizPerformance ----
        List<QuizAttempt> recentAttempts = quizAttemptRepo.findRecentSubmittedForStudent(userId, PageRequest.of(0, 5));
        List<QuizPerformance.AttemptSummary> recentSummaries = recentAttempts.stream().map(qa -> {
            Lesson l = qa.getQuiz().getLesson();
            Course c = l.getCourse();
            int pct = qa.getScorePercentage() != null ? qa.getScorePercentage() : 0;
            return new QuizPerformance.AttemptSummary(
                    qa.getQuiz().getId(), qa.getQuiz().getTitle(), c.getTitle(),
                    pct, qa.getScore(), qa.getMaxScore(), qa.getSubmittedAt(),
                    "/study/" + c.getId() + "/lesson/" + l.getId() + "/quiz/" + qa.getQuiz().getId());
        }).toList();
        QuizPerformance quizPerformance = new QuizPerformance(quizzesTaken, avgQuizScore, recentSummaries);

        // ---- recommendations ----
        List<Long> preferredCategoryIds = enrollments.stream()
                .map(e -> e.getCourse().getCategory())
                .filter(cat -> cat != null)
                .map(Category::getId)
                .distinct().toList();
        List<RecommendedCourse> recommendations = buildRecommendations(
                courseIds.isEmpty() ? NONE : courseIds,
                preferredCategoryIds.isEmpty() ? NONE : preferredCategoryIds);

        return new StudentDashboardResponse(summary, resume, courses, activity, quizPerformance, recommendations);
    }

    // ────────────────────────────────────────────────────────────

    private ResumeLearning buildResume(List<Enrollment> enrollments, List<CourseProgress> courses) {
        Map<Long, CourseProgress> byCourse = courses.stream()
                .collect(Collectors.toMap(CourseProgress::courseId, cp -> cp));

        Enrollment pick = enrollments.stream()
                .filter(e -> byCourse.get(e.getCourse().getId()).progressPercent() < 100)
                .max(Comparator.comparing(Enrollment::getLastActivityAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        if (pick == null) return null;
        CourseProgress cp = byCourse.get(pick.getCourse().getId());

        Long lessonId = pick.getLastWatchedLesson() != null ? pick.getLastWatchedLesson().getId() : null;
        String lessonTitle = pick.getLastWatchedLesson() != null ? pick.getLastWatchedLesson().getTitle() : null;
        if (lessonId == null) {
            List<Lesson> lessons = lessonRepo.findByCourseId(pick.getCourse().getId());
            if (!lessons.isEmpty()) {
                lessonId = lessons.get(0).getId();
                lessonTitle = lessons.get(0).getTitle();
            }
        }
        return new ResumeLearning(pick.getCourse().getId(), pick.getCourse().getTitle(),
                lessonId, lessonTitle, cp.progressPercent());
    }

    private List<ActivityItem> buildActivity(Long userId, List<Enrollment> enrollments) {
        List<ActivityItem> items = new ArrayList<>();

        lessonProgressRepo.findRecentCompletions(userId, PageRequest.of(0, 10)).forEach(lp -> {
            Lesson l = lp.getLesson();
            Course c = lp.getEnrollment().getCourse();
            items.add(new ActivityItem("LESSON_COMPLETED", l.getTitle(), c.getTitle(),
                    lp.getCompletedAt(), "/study-room/" + c.getId() + "/lesson/" + l.getId()));
        });

        quizAttemptRepo.findRecentSubmittedForStudent(userId, PageRequest.of(0, 10)).forEach(qa -> {
            Lesson l = qa.getQuiz().getLesson();
            Course c = l.getCourse();
            int pct = qa.getScorePercentage() != null ? qa.getScorePercentage() : 0;
            items.add(new ActivityItem("QUIZ_SUBMITTED",
                    qa.getQuiz().getTitle() + " — " + pct + "%", c.getTitle(), qa.getSubmittedAt(),
                    "/study/" + c.getId() + "/lesson/" + l.getId() + "/quiz/" + qa.getQuiz().getId()));
        });

        conversationRepo.findRecentActiveForUser(userId, PageRequest.of(0, 10)).forEach(conv -> {
            Lesson l = conv.getLesson();
            Course c = l.getCourse();
            items.add(new ActivityItem("AI_SESSION", "جلسة مع المساعد الذكي — " + l.getTitle(),
                    c.getTitle(), conv.getStartedAt(), "/study-room/" + c.getId() + "/lesson/" + l.getId()));
        });

        enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getEnrolledAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(5)
                .forEach(e -> items.add(new ActivityItem("ENROLLED",
                        "التحقت بكورس " + e.getCourse().getTitle(), e.getCourse().getTitle(),
                        e.getEnrolledAt(), "/catalog/" + e.getCourse().getId())));

        return items.stream()
                .sorted(Comparator.comparing(ActivityItem::timestamp,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(12)
                .toList();
    }

    private List<RecommendedCourse> buildRecommendations(List<Long> excludeIds, List<Long> preferredCategoryIds) {
        return courseRepo.findRecommendations(excludeIds, preferredCategoryIds, PageRequest.of(0, 6)).stream()
                .map(c -> {
                    Long catId = c.getCategory() != null ? c.getCategory().getId() : null;
                    return new RecommendedCourse(
                            c.getId(), c.getTitle(), c.getDescription(),
                            c.getCategory() != null ? c.getCategory().getName() : null,
                            c.getImageUrl(),
                            c.getTeacher() != null ? c.getTeacher().getFullName() : null,
                            c.getLessonsCount(),
                            catId != null && preferredCategoryIds.contains(catId));
                })
                .toList();
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new java.util.HashMap<>();
        for (Object[] r : rows) {
            map.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return map;
    }
}
