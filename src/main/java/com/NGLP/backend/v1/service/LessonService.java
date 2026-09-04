package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.entity.Course;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.exception.BusinessRuleException;
import com.NGLP.backend.v1.exception.ErrorCode;
import com.NGLP.backend.v1.repo.CourseRepo;
import com.NGLP.backend.v1.repo.LessonRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepo lessonRepo;
    private final CourseRepo courseRepo;
    private final LessonTranscriptService transcriptionService;
    private final FileStorageService fileStorageService;

    // 1. تم استبدال findAll لنجلب الدروس بناءً على الكورس
    public List<Lesson> findLessonsByCourse(Long courseId) {
        return lessonRepo.findByCourseId(courseId);
    }

    public Lesson findById(Long id) {
        return lessonRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lessonnot found with this"+ id));
    }

    @Transactional
    public Lesson create(Long courseId , Lesson lesson, MultipartFile file, MultipartFile image) {
        // حماية ضد صفوف دروس فارغة: لوحظت 7 صفوف بعنوان NULL في القاعدة سابقاً،
        // ما أدى إلى تعطل بذر البيانات عند الإقلاع. رفض الطلب هنا بدل حفظه فارغاً.
        if (lesson == null || lesson.getTitle() == null || lesson.getTitle().isBlank()) {
            throw new IllegalArgumentException("عذراً، يجب إدخال عنوان صالح للدرس.");
        }

        // 1. حفظ الفيديو محلياً والحصول على الرابط (مثلاً: /uploads/videos/abc.mp4)
        String videoUrl = fileStorageService.saveVideo(file);
        // 2. إسناد الرابط للدرس
        lesson.setVideoUrl(videoUrl);

        // صورة مصغرة اختيارية للدرس
        if (image != null && !image.isEmpty()) {
            lesson.setImageUrl(fileStorageService.saveImage(image));
        }

        Course course = courseRepo.findById(courseId)
                .orElseThrow(()->new EntityNotFoundException("course not found with this Id :"+courseId));
        // 3. حفظ بيانات الدرس في قاعدة البيانات
        lesson.setCourse(course);
        Lesson savedLesson = lessonRepo.save(lesson);

        // 4. استخراج اسم الملف من الرابط، وبناء "المسار المطلق" (Absolute Path) على السيرفر
        // لكي يستطيع سيرفر البايثون إيجاده وقراءته من الهارد ديسك مباشرة
        String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
        String absolutePath = Paths.get("uploads/videos/", fileName).toAbsolutePath().toString();

        // 5. استدعاء خدمة استخراج النص مع تمرير (الدرس + المسار الفعلي)
        transcriptionService.extractAndSaveTranscript(savedLesson, absolutePath);

        return savedLesson;
    }

    /**
     * يعيد تشغيل خط التفريغ/الترجمة على فيديو الدرس الموجود (بلا إعادة رفع).
     * للدروس التي رُفعت قبل ميزة التفريغ ثنائي اللغة، أو لإصلاح تفريغ ناقص.
     * يعمل بالخلفية عبر {@code extractAndSaveTranscript} (@Async) الذي يحذف القديم أولاً.
     */
    public void regenerateTranscript(Long lessonId) {
        Lesson lesson = findById(lessonId);
        String videoUrl = lesson.getVideoUrl();
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new BusinessRuleException("لا يوجد فيديو مرفوع لهذا الدرس لإعادة توليد التفريغ.",
                    HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE);
        }
        String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
        String absolutePath = Paths.get("uploads/videos/", fileName).toAbsolutePath().toString();
        // نداء عبر bean آخر → وكيل @Async يعمل (لا self-invocation).
        transcriptionService.extractAndSaveTranscript(lesson, absolutePath);
    }

    public Lesson uploadImage(Long id, MultipartFile image) {
        Lesson lesson = findById(id);
        String imageUrl = fileStorageService.saveImage(image);
        lesson.setImageUrl(imageUrl);
        return lessonRepo.save(lesson);
    }

    public Lesson update(Long id, Lesson lesson) {
        return lessonRepo.findById(id).map(existing -> {
            existing.setTitle(lesson.getTitle());
            existing.setDescription(lesson.getDescription());
            existing.setVideoUrl(lesson.getVideoUrl());
            existing.setDurationSeconds(lesson.getDurationSeconds());
            existing.setCourse(lesson.getCourse());
            return lessonRepo.save(existing);
        }).orElseThrow(() -> new EntityNotFoundException("Lesson not found with this id"+ id));
    }

    @Transactional
    public void delete(Long id) {
        lessonRepo.deleteById(id);
    }

    public boolean existsByCourseId(Long id) {
        return lessonRepo.existsByCourseId(id);
    }
}
