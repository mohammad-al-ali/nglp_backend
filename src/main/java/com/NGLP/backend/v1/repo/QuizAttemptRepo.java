package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.QuizAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNumberDesc(Long quizId, Long studentId);

    @Query("SELECT MAX(qa.attemptNumber) FROM QuizAttempt qa WHERE qa.quiz.id = :quizId AND qa.student.id = :studentId")
    Optional<Integer> findMaxAttemptNumber(@Param("quizId") Long quizId, @Param("studentId") Long studentId);

    long countByQuizIdAndStudentId(Long quizId, Long studentId);

    /** [عدد المحاولات المسلّمة, متوسط النسبة المئوية] لطالب — لإحصاء لوحة الطالب. */
    @Query("SELECT COUNT(qa), COALESCE(AVG(qa.scorePercentage), 0) FROM QuizAttempt qa " +
            "WHERE qa.student.id = :userId AND qa.submittedAt IS NOT NULL AND qa.scorePercentage IS NOT NULL")
    List<Object[]> statsForStudent(@Param("userId") Long userId);

    /** أحدث المحاولات المسلّمة لطالب مع الكويز/الدرس/الكورس (لسجلّ النشاط + بطاقة الأداء). */
    @Query("SELECT qa FROM QuizAttempt qa JOIN FETCH qa.quiz q JOIN FETCH q.lesson l JOIN FETCH l.course c " +
            "WHERE qa.student.id = :userId AND qa.submittedAt IS NOT NULL ORDER BY qa.submittedAt DESC")
    List<QuizAttempt> findRecentSubmittedForStudent(@Param("userId") Long userId, Pageable pageable);

    /** المحاولات القديمة بلا نسبة مئوية — لإعادة الحساب في الـ backfill. */
    List<QuizAttempt> findByScorePercentageIsNullAndSubmittedAtIsNotNull();
}
