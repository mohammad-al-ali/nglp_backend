package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.QuestionOption;
import com.NGLP.backend.v1.entity.QuizQuestion;
import com.NGLP.backend.v1.repo.QuestionOptionRepo;
import com.NGLP.backend.v1.repo.QuizQuestionRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionOptionService {

    private final QuestionOptionRepo questionOptionRepo;
    private final QuizQuestionRepo quizQuestionRepo;

    // جلب خيارات سؤال معين
    public List<QuestionOption> findOptionsByQuestion(Long questionId) {
        return questionOptionRepo.findByQuestionId(questionId);
    }

    public QuestionOption findById(Long id) {
        return questionOptionRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Option not found with this id " + id));
    }

    public QuestionOption create(Long questionId, QuestionOption option) {

        QuizQuestion question = quizQuestionRepo.findById(questionId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Question not found with this id " + questionId));

        option.setQuestion(question);

        return questionOptionRepo.save(option);
    }

    public QuestionOption update(Long id, QuestionOption option) {
        return questionOptionRepo.findById(id).map(existing -> {

            existing.setOptionLabel(option.getOptionLabel());
            existing.setOptionText(option.getOptionText());
            existing.setIsCorrect(option.getIsCorrect());

            return questionOptionRepo.save(existing);

        }).orElseThrow(() ->
                new EntityNotFoundException("Option not found with this id " + id));
    }

    public void delete(Long id) {
        questionOptionRepo.deleteById(id);
    }
}
