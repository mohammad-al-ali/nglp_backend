package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.dto.LessonDurationRequest;
import com.NGLP.backend.v1.dto.LessonTranscriptResponse;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.TranscriptLanguage;
import com.NGLP.backend.v1.exception.BusinessRuleException;
import com.NGLP.backend.v1.exception.ErrorCode;
import com.NGLP.backend.v1.service.LessonService;
import com.NGLP.backend.v1.service.LessonTranscriptService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final LessonTranscriptService lessonTranscriptService;

    public LessonController(LessonService lessonService, LessonTranscriptService lessonTranscriptService) {
        this.lessonService = lessonService;
        this.lessonTranscriptService = lessonTranscriptService;
    }

    @GetMapping
    public List<Lesson> getAll(@RequestParam(required = false) Long courseId) { return lessonService.findLessonsByCourse(courseId); }

    @GetMapping("/{id}")
    public Lesson getById(@PathVariable Long id) { return lessonService.findById(id); }

    /**
     * تفريغ الدرس بلغة واحدة (ar افتراضياً) مع قائمة اللغات المتوفرة.
     * يُرجع {@code available:false} إذا لم يتوفر تفريغ لهذه اللغة بعد.
     */
    @GetMapping("/{id}/transcript")
    public LessonTranscriptResponse getTranscript(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ar") String lang) {
        TranscriptLanguage language = TranscriptLanguage.fromCode(lang);
        if (language == null) {
            throw new BusinessRuleException(
                    "لغة التفريغ غير مدعومة. القيم المسموحة: ar أو en.",
                    HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
        }
        return lessonTranscriptService.getTranscript(id, language);
    }

    /**
     * إعادة توليد ترجمة لغة معيّنة متزامناً عبر خدمة الترجمة الأوفلاين (للتشغيل اليدوي).
     * يحذف الترجمة الحالية لتلك اللغة ثم يعيد بناءها من لغة المصدر المتوفرة.
     * يتطلّب وجود تفريغ مصدر بلغة أخرى مسبقاً — للدروس بلا تفريغ إطلاقاً استخدم
     * {@code POST /{id}/transcript/regenerate}.
     */
    @PostMapping("/{id}/transcript/translate")
    public LessonTranscriptResponse regenerateTranscriptTranslation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ar") String target) {
        TranscriptLanguage language = TranscriptLanguage.fromCode(target);
        if (language == null) {
            throw new BusinessRuleException(
                    "لغة التفريغ غير مدعومة. القيم المسموحة: ar أو en.",
                    HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
        }
        return lessonTranscriptService.regenerateTranslation(id, language);
    }

    /**
     * إعادة تشغيل خط التفريغ كاملاً على فيديو الدرس الموجود (Whisper + الترجمة الأوفلاين).
     * للدروس المرفوعة قبل الميزة أو لإصلاح تفريغ ناقص. يعمل بالخلفية.
     */
    @PostMapping("/{id}/transcript/regenerate")
    public ResponseEntity<java.util.Map<String, String>> regenerateTranscript(@PathVariable Long id) {
        lessonService.regenerateTranscript(id);
        return ResponseEntity.accepted()
                .body(java.util.Map.of("message",
                        "بدأت إعادة توليد التفريغ في الخلفية. راجع الدرس بعد دقيقة."));
    }

    @PostMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createLessonWithVideo(
            @PathVariable Long courseId,
            @Valid @RequestPart("lesson") Lesson lesson,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("📩 طلب إنشاء درس جديد مع الفيديو: {}", lesson.getTitle());
        // لا نبتلع الاستثناءات هنا — يتكفّل GlobalExceptionHandler بتحويلها إلى ApiError عربي موحّد.
        Lesson savedLesson = lessonService.create(courseId, lesson, file, image);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(savedLesson);
    }
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Lesson uploadImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        return lessonService.uploadImage(id, image);
    }

    @PutMapping("/{id}")
    public Lesson update(@PathVariable Long id, @Valid @RequestBody Lesson lesson) { return lessonService.update(id, lesson); }

    /**
     * self-heal لمدة الدرس: تُطبَّق فقط إن كانت المدة الحالية مجهولة. POST (لا PATCH)
     * لأن إعداد CORS لا يسمح بـ PATCH، وبنفس نمط {@code POST /{id}/image}.
     */
    @PostMapping("/{id}/duration")
    public Lesson setDuration(@PathVariable Long id, @Valid @RequestBody LessonDurationRequest body) {
        return lessonService.setDurationIfMissing(id, body.durationSeconds());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
