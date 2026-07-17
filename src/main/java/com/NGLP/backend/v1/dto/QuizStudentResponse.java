package com.NGLP.backend.v1.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizStudentResponse(
    Long id,
    Long lessonId,
    String title,
    String status,
    LocalDateTime createdAt,
    Boolean showAnswersAfterSubmit,
    List<StudentQuestionResponse> questions
) {
    public record StudentQuestionResponse(
        Long id,
        String questionText,
        Integer difficultyWeight,
        Integer orderIndex,
        List<StudentChoiceResponse> choices
    ) {}

    public record StudentChoiceResponse(
        Long id,
        String choiceText
    ) {}
}
