package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Quiz;
import com.NGLP.backend.v1.entity.QuizQuestion;
import com.NGLP.backend.v1.repo.QuizQuestionRepo;
import com.NGLP.backend.v1.repo.QuizRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizQuestionService {

    private final QuizQuestionRepo quizQuestionRepo;
    private final QuizRepo quizRepo;

    // جلب أسئلة اختبار معين
    public List<QuizQuestion> findQuestionsByQuiz(Long quizId) {
        return quizQuestionRepo.findByQuizId(quizId);
    }

    public QuizQuestion findById(Long id) {
        return quizQuestionRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Question not found with this id " + id));
    }

    public QuizQuestion create(Long quizId, QuizQuestion question) {

        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Quiz not found with this id " + quizId));

        question.setQuiz(quiz);

        return quizQuestionRepo.save(question);
    }

    public QuizQuestion update(Long id, QuizQuestion question) {
        return quizQuestionRepo.findById(id).map(existing -> {

            existing.setQuestionText(question.getQuestionText());
            existing.setDifficulty(question.getDifficulty());
            existing.setGeneratedBy(question.getGeneratedBy());

            return quizQuestionRepo.save(existing);

        }).orElseThrow(() ->
                new EntityNotFoundException("Question not found with this id " + id));
    }

    public void delete(Long id) {
        quizQuestionRepo.deleteById(id);
    }
}
