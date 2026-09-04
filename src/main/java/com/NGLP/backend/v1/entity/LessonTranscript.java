package com.NGLP.backend.v1.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lesson_transcripts", indexes = {
        @Index(name = "ix_transcript_lesson_lang", columnList = "lesson_id, language, segmentIndex")
})
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

    /**
     * لغة هذا المقطع. الصفوف القديمة (قبل إضافة هذا العمود) تُملأ بـ {@code AR}
     * عبر خطوة backfill في {@code DataInitializer} لأن كل التفريغ السابق عربي.
     *
     * <p>{@code @JdbcTypeCode(VARCHAR)}: نُخزّنها كـ varchar لا كنوع MySQL {@code ENUM}
     * الأصلي — نوع ENUM الأصلي يجعل {@code ddl-auto=update} يفشل عند إضافة أي قيمة
     * جديدة للـ enum لاحقاً (خطأ «Data truncated for column»).
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 8)
    private TranscriptLanguage language;

    /** ترتيب المقطع ضمن لغته (0-based) — أكثر متانة من الترتيب بـ startSecond. */
    private Integer segmentIndex;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private TranscriptSource source;

    /** كود اللغة الخام كما اكتشفه Whisper (مثل "ar" / "fr") — للتتبّع فقط. */
    @Column(length = 8)
    private String detectedLanguage;
}
