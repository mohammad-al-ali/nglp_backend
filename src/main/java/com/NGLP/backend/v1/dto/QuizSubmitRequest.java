package com.NGLP.backend.v1.dto;

import java.util.List;

public record QuizSubmitRequest(
    List<AnswerEntry> answers
) {
    public record AnswerEntry(Long questionId, Long selectedChoiceId) {}
}
