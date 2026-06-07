package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.service.LessonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
    private final LessonService lessonService;

    public LessonController(LessonService lessonService) { this.lessonService = lessonService; }

    @GetMapping
    public List<Lesson> getAll(@RequestParam(required = false) Long courseId) { return lessonService.findLessonsByCourse(courseId); }

    @GetMapping("/{id}")
    public Lesson getById(@PathVariable Long id) { return lessonService.findById(id); }

    @PostMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createLessonWithVideo(
            // 🌟 عدنا للكود النظيف: نستلم كائن Lesson مباشرة
            @PathVariable Long courseId,
            @RequestPart("lesson") Lesson lesson,
            @RequestPart("file") MultipartFile file) {

        log.info("📩 طلب إنشاء درس جديد مع الفيديو: {}", lesson.getTitle());

        try {
            Lesson savedLesson = lessonService.create(courseId ,lesson, file);
            return ResponseEntity.ok(savedLesson);
        } catch (Exception e) {
            log.error("❌ حدث خطأ أثناء الرفع: ", e);
            return ResponseEntity.badRequest().body("حدث خطأ: " + e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public Lesson update(@PathVariable Long id, @RequestBody Lesson lesson) { return lessonService.update(id, lesson); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
