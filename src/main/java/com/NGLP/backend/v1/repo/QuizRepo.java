package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepo extends JpaRepository<Quiz, Long> {
    List<Quiz> findByLessonId(Long lessonId);
    List<Quiz> findByLessonIdAndStatus(Long lessonId, String status);
    long countByLessonId(Long lessonId);
}
