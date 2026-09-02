package com.NGLP.backend.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizQuestionRequest(
    @NotBlank(message = "{nglp.quiz.question.text.required}")
    @Size(max = 1000, message = "{nglp.quiz.question.text.size}")
    String questionText,

    @Min(value = 1, message = "{nglp.quiz.question.difficulty.range}")
    @Max(value = 10, message = "{nglp.quiz.question.difficulty.range}")
    Integer difficultyWeight,

    String explanation,

    @NotNull(message = "{nglp.quiz.question.choices.size}")
    @Size(min = 4, max = 4, message = "{nglp.quiz.question.choices.size}")
    @Valid
    List<ChoiceEntry> choices
) {
    public record ChoiceEntry(
        @NotBlank(message = "{nglp.quiz.choice.text.required}")
        String choiceText,

        @NotNull(message = "{nglp.quiz.choice.isCorrect.required}")
        Boolean isCorrect
    ) {}
}
