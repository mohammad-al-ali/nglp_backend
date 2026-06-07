package com.NGLP.backend.v1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_transcripts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    private Integer startSecond;
    private Integer endSecond;

    @Column(columnDefinition = "TEXT")
    private String transcriptContent;
}


