package com.NGLP.backend.v1.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // المجلد الذي سنحفظ فيه الفيديوهات (كما يعمل معك بنجاح)
    private final String UPLOAD_DIR = "uploads/videos/";
    private final String IMAGE_UPLOAD_DIR = "uploads/images/";

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("عذراً، يجب إرفاق ملف صورة صالح.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("عذراً، الملف المرفق ليس صورة صالحة.");
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
        // 1. حماية النظام: التحقق من أن الملف ليس فارغاً (مأخوذة من التعديل الجديد)
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("عذراً، يجب إرفاق ملف فيديو صالح.");
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
}