package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizChoiceRepo extends JpaRepository<QuizChoice, Long> {
    List<QuizChoice> findByQuestionId(Long questionId);
    long countByQuestionId(Long questionId);
}
