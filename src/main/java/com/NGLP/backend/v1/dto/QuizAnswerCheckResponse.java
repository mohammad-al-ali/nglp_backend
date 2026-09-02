package com.NGLP.backend.v1.dto;

public record QuizAnswerCheckResponse(
    Boolean isCorrect,
    String explanation,
    Long correctChoiceId,
    String correctChoiceText
) {}
