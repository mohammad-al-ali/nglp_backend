package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.Quiz;
import com.NGLP.backend.v1.repo.LessonRepo;
import com.NGLP.backend.v1.repo.QuizRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepo quizRepo;
    private final LessonRepo lessonRepo;

    // جلب اختبارات درس معين
    public List<Quiz> findByLessonId(Long lessonId) {
        return quizRepo.findByLessonId(lessonId);
    }

    public Quiz findById(Long id) {
        return quizRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Quiz not found with this id " + id));
    }

    @Transactional
    public Quiz create(Long lessonId, Quiz quiz) {

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Lesson not found with this id " + lessonId));

        quiz.setLesson(lesson);

        if (quiz.getGeneratedAt() == null) {
            quiz.setGeneratedAt(LocalDateTime.now());
        }

        return quizRepo.save(quiz);
    }

    public Quiz update(Long id, Quiz quiz) {
        return quizRepo.findById(id).map(existing -> {

            existing.setStatus(quiz.getStatus());
            existing.setVersion(quiz.getVersion());
            existing.setGeneratedAt(quiz.getGeneratedAt());
            existing.setLesson(quiz.getLesson());

            return quizRepo.save(existing);

        }).orElseThrow(() ->
                new EntityNotFoundException("Quiz not found with this id " + id));
    }

    @Transactional
    public void delete(Long id) {
        quizRepo.deleteById(id);
    }
}