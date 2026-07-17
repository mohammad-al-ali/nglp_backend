package com.NGLP.backend.v1.dto;

import java.util.List;

public record QuizQuestionRequest(
    String questionText,
    Integer difficultyWeight,
    String explanation,
    List<ChoiceEntry> choices
) {
    public record ChoiceEntry(String choiceText, Boolean isCorrect) {}
}
