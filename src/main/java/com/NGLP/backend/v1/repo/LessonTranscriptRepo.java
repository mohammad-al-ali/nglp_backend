package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.LessonTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonTranscriptRepo extends JpaRepository<LessonTranscript, Long> {
    @Query("SELECT lt FROM LessonTranscript lt WHERE lt.lesson.id = :lessonId " +
            "AND :timestamp >= lt.startSecond AND :timestamp < lt.endSecond ORDER BY lt.id ASC LIMIT 1")
    Optional<LessonTranscript> findTranscriptAtTimestamp(@Param("lessonId") Long lessonId, @Param("timestamp") Integer timestamp);
    List<LessonTranscript> findByLessonIdOrderByStartSecondAsc(Long lessonId);
}
