package com.NGLP.backend.v1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String videoUrl;
    private Integer durationSeconds;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    //  إضافة العلاقة مع النصوص (Transcripts)
    // orphanRemoval = true تعني: إذا تم حذف النص من القائمة، احذفه من قاعدة البيانات أيضاً
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // مهم جداً لتجنب الدوران اللانهائي
    private List<LessonTranscript> transcripts;

    //  إضافة العلاقة مع المحادثات (Conversations)
     @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Conversation> conversations;
}