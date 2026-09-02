package com.NGLP.backend.v1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "{nglp.lesson.title.required}")
    @Size(min = 3, max = 150, message = "{nglp.lesson.title.size}")
    private String title;
    @Size(max = 5000, message = "{nglp.lesson.description.size}")
    @Column(columnDefinition = "TEXT")
    private String description;
    private String videoUrl;
    private String imageUrl;
    @PositiveOrZero(message = "{nglp.lesson.duration.positive}")
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