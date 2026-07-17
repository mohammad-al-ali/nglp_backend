package com.NGLP.backend.v1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_choices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id")
    @JsonIgnore
    private QuizQuestion question;

    @Column(columnDefinition = "TEXT")
    private String choiceText;

    private Boolean isCorrect;
}
