package com.NGLP.backend.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizSubmitRequest(
    @NotEmpty(message = "{nglp.quiz.answers.required}")
    @Valid
    List<AnswerEntry> answers
) {
    public record AnswerEntry(
        @NotNull(message = "{nglp.quiz.answer.questionId.required}")
        Long questionId,

        @NotNull(message = "{nglp.quiz.answer.choiceId.required}")
        Long selectedChoiceId
    ) {}
}
