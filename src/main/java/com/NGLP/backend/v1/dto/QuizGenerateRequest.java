package com.NGLP.backend.v1.dto;

public record QuizGenerateRequest(
    Long lessonId,
    String title,
    Integer numberOfQuestions,
    Long teacherId
) {}
