package com.NGLP.backend.v1.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * حمولة لوحة الطالب — كل الأرقام محسوبة في الخادم. طلب واحد،
 * بلا حسابات على الواجهة.
 */
public record StudentDashboardResponse(
        Summary summary,
        ResumeLearning resumeLearning,        // nullable
        List<CourseProgress> courses,
        List<ActivityItem> recentActivity,
        QuizPerformance quizPerformance,
        List<RecommendedCourse> recommendations
) {
    public record Summary(
            int enrolledCount,
            int inProgressCount,
            int completedCount,
            int overallProgressPercent,
            long lessonsCompleted,
            long totalLessons,
            long totalWatchTimeSeconds,
            long enrolledContentSeconds,
            long quizzesTaken,
            int avgQuizScorePercent,
            long aiQuestionsAsked,
            long aiSessions,
            LocalDateTime memberSince
    ) {}

    public record ResumeLearning(
            Long courseId, String courseTitle,
            Long lessonId, String lessonTitle,
            int coursePercent
    ) {}

    public record CourseProgress(
            Long courseId, String title, String description, String category,
            String imageUrl, String teacherName,
            int progressPercent, long completedLessons, long totalLessons,
            Long lastWatchedLessonId
    ) {}

    /** type: LESSON_COMPLETED | QUIZ_SUBMITTED | AI_SESSION | ENROLLED */
    public record ActivityItem(
            String type, String title, String courseTitle,
            LocalDateTime timestamp, String link
    ) {}

    public record QuizPerformance(
            long totalAttempts, int avgScorePercent, List<AttemptSummary> recent
    ) {
        public record AttemptSummary(
                Long quizId, String quizTitle, String courseTitle,
                int scorePercent, Integer score, Integer maxScore,
                LocalDateTime submittedAt, String link
        ) {}
    }

    public record RecommendedCourse(
            Long courseId, String title, String description, String category,
            String imageUrl, String teacherName, int lessonsCount, boolean sameCategory
    ) {}
}
