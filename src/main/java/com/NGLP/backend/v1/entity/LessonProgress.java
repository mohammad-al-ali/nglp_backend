package com.NGLP.backend.v1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * تقدّم الطالب على مستوى الدرس الواحد. يرتبط بـ {@link Enrollment} (الذي يشفّر
 * زوج المستخدم/الكورس) + {@link Lesson}. إلغاء الإكمال يُبقي الصف مع
 * {@code completed=false} (يحفظ تاريخ {@code completedAt} ويوحّد مسار الحفظ).
 * كل العدّادات تُرشّح على {@code completed=true}.
 */
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"enrollment_id", "lesson_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "enrollment_id")
    @JsonIgnore
    private Enrollment enrollment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Builder.Default
    private Boolean completed = Boolean.TRUE;

    private LocalDateTime completedAt;

    /** محجوز لتتبّع الثواني المشاهَدة مستقبلاً — لا يُملأ حالياً. */
    private Integer watchedSeconds;
}
