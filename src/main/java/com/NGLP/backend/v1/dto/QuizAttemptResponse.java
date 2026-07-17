package com.NGLP.backend.v1.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizAttemptResponse(
    Long id,
    Long quizId,
    Long studentId,
    Integer attemptNumber,
    LocalDateTime startedAt,
    LocalDateTime submittedAt,
    Integer score,
    List<AnswerResponse> answers
) {
    public record AnswerResponse(
        Long id,
        Long questionId,
        Long selectedChoiceId,
        Boolean isCorrect,
        Integer pointsAwarded,
        String correctChoiceExplanation,
        Long correctChoiceId,
        String correctChoiceText
    ) {}
}
