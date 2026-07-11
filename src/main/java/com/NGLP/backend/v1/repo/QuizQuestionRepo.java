package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepo extends JpaRepository<QuizQuestion, Long> {

    // لجلب أسئلة اختبار معين
    List<QuizQuestion> findByQuizId(Long quizId);

}
