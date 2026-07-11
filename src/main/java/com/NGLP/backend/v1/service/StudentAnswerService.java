package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.QuestionOption;
import com.NGLP.backend.v1.entity.QuizQuestion;
import com.NGLP.backend.v1.entity.StudentAnswer;
import com.NGLP.backend.v1.entity.User;
import com.NGLP.backend.v1.repo.QuestionOptionRepo;
import com.NGLP.backend.v1.repo.QuizQuestionRepo;
import com.NGLP.backend.v1.repo.StudentAnswerRepo;
import com.NGLP.backend.v1.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAnswerService {

    private final StudentAnswerRepo studentAnswerRepo;
    private final UserRepo userRepo;
    private final QuizQuestionRepo quizQuestionRepo;
    private final QuestionOptionRepo questionOptionRepo;

    public List<StudentAnswer> findByUser(Long userId) {
        return studentAnswerRepo.findByUserId(userId);
    }

    public StudentAnswer findById(Long id) {
        return studentAnswerRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Answer not found with this id " + id));
    }

    public StudentAnswer create(Long userId, Long questionId, Long optionId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with this id " + userId));

        QuizQuestion question = quizQuestionRepo.findById(questionId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Question not found with this id " + questionId));

        QuestionOption option = questionOptionRepo.findById(optionId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Option not found with this id " + optionId));

        StudentAnswer answer = new StudentAnswer();
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setSelectedOption(option);

        return studentAnswerRepo.save(answer);
    }

    public void delete(Long id) {
        studentAnswerRepo.deleteById(id);
    }
}
