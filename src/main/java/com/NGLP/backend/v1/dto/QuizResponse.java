package com.NGLP.backend.v1.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizResponse(
    Long id,
    Long lessonId,
    String title,
    String status,
    Long createdByTeacherId,
    LocalDateTime createdAt,
    Boolean showAnswersAfterSubmit,
    List<QuestionResponse> questions
) {
    public record QuestionResponse(
        Long id,
        String questionText,
        Integer difficultyWeight,
        Integer orderIndex,
        String explanation,
        List<ChoiceResponse> choices
    ) {}

    public record ChoiceResponse(
        Long id,
        String choiceText,
        Boolean isCorrect
    ) {}
}
