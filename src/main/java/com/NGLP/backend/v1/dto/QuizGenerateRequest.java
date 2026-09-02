package com.NGLP.backend.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuizGenerateRequest(
    @NotNull(message = "{nglp.quiz.lessonId.required}")
    Long lessonId,

    @NotBlank(message = "{nglp.quiz.title.required}")
    @Size(min = 3, max = 150, message = "{nglp.quiz.title.size}")
    String title,

    @NotNull(message = "{nglp.quiz.numberOfQuestions.required}")
    @Min(value = 1, message = "{nglp.quiz.numberOfQuestions.range}")
    @Max(value = 20, message = "{nglp.quiz.numberOfQuestions.range}")
    Integer numberOfQuestions,

    @NotNull(message = "{nglp.quiz.teacherId.required}")
    Long teacherId
) {}
