package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.QuizAttempt;
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
}
