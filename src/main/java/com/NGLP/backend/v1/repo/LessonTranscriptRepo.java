package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.LessonTranscript;
import com.NGLP.backend.v1.entity.TranscriptLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonTranscriptRepo extends JpaRepository<LessonTranscript, Long> {

    /** المقطع الذي يغطي لحظة زمنية معيّنة بلغة محددة (يستخدمه وكيل الذكاء الاصطناعي). */
    @Query("SELECT lt FROM LessonTranscript lt WHERE lt.lesson.id = :lessonId " +
            "AND lt.language = :language " +
            "AND :timestamp >= lt.startSecond AND :timestamp < lt.endSecond " +
            "ORDER BY lt.segmentIndex ASC, lt.id ASC LIMIT 1")
    Optional<LessonTranscript> findTranscriptAtTimestamp(@Param("lessonId") Long lessonId,
                                                         @Param("language") TranscriptLanguage language,
                                                         @Param("timestamp") Integer timestamp);

    /** كل مقاطع درس بلغة محددة، مرتّبة بترتيب المقطع. */
    List<LessonTranscript> findByLessonIdAndLanguageOrderBySegmentIndexAsc(Long lessonId, TranscriptLanguage language);

    /** كل مقاطع درس (كل اللغات) مرتّبة بالزمن — يُستخدم للحذف والـ backfill. */
    List<LessonTranscript> findByLessonIdOrderByStartSecondAsc(Long lessonId);

    /** اللغات المتوفرة فعلاً لهذا الدرس. */
    @Query("SELECT DISTINCT lt.language FROM LessonTranscript lt " +
            "WHERE lt.lesson.id = :lessonId AND lt.language IS NOT NULL")
    List<TranscriptLanguage> findDistinctLanguages(@Param("lessonId") Long lessonId);

    boolean existsByLessonIdAndLanguage(Long lessonId, TranscriptLanguage language);

    /** أكبر ثانية نهاية بين كل مقاطع الدرس ≈ طول الفيديو — يُستخدم كـ backfill لمدّة الدرس. */
    @Query("SELECT MAX(lt.endSecond) FROM LessonTranscript lt WHERE lt.lesson.id = :lessonId")
    Integer findMaxEndSecondByLessonId(@Param("lessonId") Long lessonId);

    /** الصفوف القديمة التي لا لغة لها — تُملأ مرة واحدة عند الإقلاع. */
    List<LessonTranscript> findByLanguageIsNull();
}
