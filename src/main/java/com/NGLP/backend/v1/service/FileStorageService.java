package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    // المجلد الذي سنحفظ فيه الفيديوهات (كما يعمل معك بنجاح)
    private final String UPLOAD_DIR = "uploads/videos/";
    private final String IMAGE_UPLOAD_DIR = "uploads/images/";

    // القوائم البيضاء للامتدادات المسموحة
    private static final List<String> IMAGE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> VIDEO_EXTENSIONS = List.of(".mp4", ".webm", ".mov", ".m4v", ".ogv");

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("عذراً، يجب إرفاق ملف صورة صالح.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new UnsupportedFileTypeException("عذراً، الملف المرفق ليس صورة صالحة. الصيغ المدعومة: JPG، PNG، WEBP، GIF.");
        }
        if (!hasAllowedExtension(file.getOriginalFilename(), IMAGE_EXTENSIONS)) {
            throw new UnsupportedFileTypeException("صيغة الصورة غير مدعومة. الصيغ المسموحة: JPG، PNG، WEBP، GIF.");
        }

        try {
            Path uploadPath = Paths.get(IMAGE_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "image.jpg";
            }
            originalFilename = StringUtils.cleanPath(originalFilename);

            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/images/" + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("فشل في حفظ الصورة: " + e.getMessage(), e);
        }
    }

    public String saveVideo(MultipartFile file) {
        // 1. حماية النظام: التحقق من أن الملف ليس فارغاً
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("عذراً، يجب إرفاق ملف فيديو صالح للدرس.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("video/")) {
            throw new UnsupportedFileTypeException("الملف المرفق ليس فيديو. الصيغ المدعومة: MP4، WEBM، MOV.");
        }
        if (!hasAllowedExtension(file.getOriginalFilename(), VIDEO_EXTENSIONS)) {
            throw new UnsupportedFileTypeException("صيغة الفيديو غير مدعومة. الصيغ المسموحة: MP4، WEBM، MOV.");
        }

        try {
            // 2. التأكد من أن المجلد موجود، وإن لم يكن ننشئه
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 3. تنظيف اسم الملف للحماية من ثغرات (Path Traversal) واستخراج الامتداد
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "lesson.mp4"; // اسم افتراضي للطوارئ
            }
            originalFilename = StringUtils.cleanPath(originalFilename);

            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".mp4"; // امتداد افتراضي إذا لم يوجد

            // 4. توليد اسم فريد للملف لتجنب تكرار الأسماء
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 5. مسار الملف النهائي (مع normalize لضمان صحة المسار)
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            // 6. حفظ الملف فعلياً في نظام التشغيل (مع خيار الاستبدال في حال التعارض النادر)
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 7. إرجاع الرابط "النسبي" الذي سيتعرف عليه المتصفح (كما تفضل أنت)
            return "/uploads/videos/" + uniqueFilename;

        } catch (IOException e) {
            // استخدام RuntimeException كما تفضل في نسختك الأصلية
            throw new RuntimeException("فشل في حفظ ملف الفيديو: " + e.getMessage(), e);
        }
    }

    /** يتحقق أن امتداد الملف ضمن القائمة البيضاء (غير حسّاس لحالة الأحرف). */
    private boolean hasAllowedExtension(String filename, List<String> allowed) {
        if (filename == null) return true; // بعض العملاء لا يرسلون اسم الملف؛ نكتفي بفحص نوع المحتوى
        String cleaned = StringUtils.cleanPath(filename).toLowerCase(Locale.ROOT);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0) return false;
        return allowed.contains(cleaned.substring(dot));
    }
}