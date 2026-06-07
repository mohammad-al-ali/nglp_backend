package com.NGLP.backend.v1;

import com.NGLP.backend.v1.entity.*;
import com.NGLP.backend.v1.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * ====================================================================
 * 🎓 اسم الملف: DataInitializer.java (مهيئ وباذر البيانات المتكامل)
 * 🎯 الغاية منه:
 *   يقوم هذا الكلاس بتشغيل كود تهيئة قاعدة البيانات تلقائياً بمجرد إقلاع سيرفر Spring Boot.
 *   يقوم بتهيئة وبذر:
 *     1. الأدوار والمستخدمين (طالب، معلم، مدير) بكلمات مرور مشفرة.
 *     2. التصنيفات التعليمية (Frontend, Backend, AI, Mobile).
 *     3. نسخ عينات الفيديوهات التفاعلية تلقائياً من مجلد الفيديوهات لمجلد الرفع.
 *     4. الكورسات البرمجية والدروس المرتبطة بالفيديوهات مع نصوص تفريغ Whisper.
 *     5. تسجيل الطالب الافتراضي في الكورسات تلقائياً لعرضها مباشرة باللوحة.
 * ====================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final CategoryRepo categoryRepo;
    private final CourseRepo courseRepo;
    private final LessonRepo lessonRepo;
    private final LessonTranscriptRepo transcriptRepo;
    private final EnrollmentRepo enrollmentRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("🌱 بدء عملية بذر البيانات الافتراضية الشاملة (Full Database Seeding)...");

        // 1. بذر الأدوار القياسية
        Role studentRole = seedRole("ROLE_STUDENT", "Student learner account");
        Role teacherRole = seedRole("ROLE_TEACHER", "Teacher course creator account");
        Role adminRole = seedRole("ROLE_ADMIN", "Administrator account");

        // 2. بذر المستخدمين الافتراضيين
        User student = seedUser("أحمد الطالب", "student@nglp.com", "student123", studentRole);
        User teacher = seedUser("د. محمد المعلم", "teacher@nglp.com", "teacher123", teacherRole);
        seedUser("أبو بكر المشرف", "admin@nglp.com", "admin123", adminRole);

        // 3. بذر التصنيفات الافتراضية
        Category cat1 = seedCategory("تطوير واجهات المستخدم (Frontend)");
        Category cat2 = seedCategory("تطوير برمجيات الخلفية (Backend)");
        Category cat3 = seedCategory("علم البيانات والذكاء الاصطناعي (Data Science & AI)");
        Category cat4 = seedCategory("تطوير تطبيقات الهواتف الذكية (Mobile Apps)");

        // 4. نسخ عينات الفيديو تلقائياً لمجلد الرفع
        copyDemoVideos();

        // 5. بذر الكورسات البرمجية الواقعية
        Course reactCourse = seedCourse("تطوير واجهات الويب باستخدام React", 
            "تعلم بناء واجهات ويب متكاملة وتفاعلية باستخدام مكتبة React الرائدة، مع دعم كامل من المساعد الذكي المدمج لشرح الكود والدرس خطوة بخطوة.",
            cat1, teacher);

        Course laravelCourse = seedCourse("تطوير تطبيقات الويب الحديثة باستخدام Laravel",
            "احترف بناء الأنظمة والواجهات الخلفية المتطورة باستخدام إطار عمل Laravel بلغة PHP، وتعلم معمارية MVC وتأمين الـ APIs بكل سهولة.",
            cat2, teacher);

        Course jsCourse = seedCourse("أساسيات لغة JavaScript للمبتدئين",
            "ابتدئ رحلتك البرمجية بتعلم لغة البرمجة الأكثر شعبية جافاسكربت. تفاعل مع الواجهات، وتعلم المنطق البرمجي السليم من الصفر.",
            cat1, teacher);

        // 6. بذر الدروس المرتبطة بالفيديوهات
        Lesson reactLesson1 = seedLesson("مقدمة إلى React وما هي مميزاتها",
            "في هذا الدرس سنتعرف على بنية React ومفهوم المكونات (Components) وكيفية تنظيم كود واجهات المستخدم وسرعة تحديثها.",
            "uploads/videos/What is React_(360P).mp4", 100, reactCourse);

        Lesson reactLesson2 = seedLesson("تعلم React في 100 ثانية",
            "شرح سريع وبسيط لكيفية عمل React، مفهوم الـ JSX، والربط التلقائي للبيانات والتحكم الفعال في العناصر التفاعلية.",
            "uploads/videos/React in 100 Seconds(360P).mp4", 100, reactCourse);

        Lesson laravelLesson1 = seedLesson("تعلم إطار عمل Laravel في 100 ثانية",
            "شرح سريع للمكونات الأساسية لإطار العمل Laravel، معمارية MVC، نظام إدارة قواعد البيانات وقوة الروابط الخلفية.",
            "uploads/videos/Laravel in 100 Seconds(360P).mp4", 100, laravelCourse);

        Lesson jsLesson1 = seedLesson("مقدمة إلى لغة JavaScript وكيف تعمل",
            "تعلم أساسيات لغة جافاسكربت وتاريخها، وكيف تقوم بإضافة التفاعل التام مع صفحات الويب وتعديل محتوى الصفحة ديناميكياً.",
            "uploads/videos/What is JavaScript and what is it used for_(360P).mp4", 100, jsCourse);

        // 7. بذر نصوص التفريغ (Transcripts) للدروس لتمكين سياق الـ AI Tutor
        // نصوص درس مقدمة ريأكت
        seedTranscript(reactLesson1, 0, 30, "أهلاً بكم في درس مقدمة إلى مكتبة ريأكت. ريأكت هي مكتبة جافاسكربت قوية جداً لبناء واجهات المستخدم.");
        seedTranscript(reactLesson1, 30, 60, "تتميز ريأكت بمفهوم المكونات (Components) التي تمكننا من إعادة استخدام الأكواد وتنظيم الصفحة بشكل ممتاز.");
        seedTranscript(reactLesson1, 60, 100, "كما تعتمد على شجرة المكونات وتحديث الواجهة تلقائياً وبسرعة فائقة بمجرد تغير حالة البيانات (State).");

        // نصوص درس ريأكت في 100 ثانية
        seedTranscript(reactLesson2, 0, 50, "في هذا الفيديو سنتعلم ريأكت في 100 ثانية فقط. ريأكت تمكنك من كتابة كود HTML داخل كود الجافاسكربت باستخدام JSX.");
        seedTranscript(reactLesson2, 50, 100, "وتقوم بعملية الربط التلقائي للبيانات وإدارة الأحداث بكفاءة وسلاسة تامة، مما يجعل بناء المواقع متعة حقيقية.");

        // نصوص درس لارافيل في 100 ثانية
        seedTranscript(laravelLesson1, 0, 50, "مرحباً بكم، سنتحدث عن إطار العمل لارافيل في 100 ثانية. لارافيل هو إطار عمل بلغة PHP مبني على معمارية MVC الشهيرة.");
        seedTranscript(laravelLesson1, 50, 100, "يوفر لارافيل ميزات مذهلة مثل نظام التوجيه (Routing)، ومحرك القوالب Blade، ونظام إدارة قاعدة البيانات Eloquent ORM.");

        // نصوص درس جافاسكربت
        seedTranscript(jsLesson1, 0, 50, "أهلاً بكم في شرح لغة جافاسكربت. جافاسكربت هي لغة البرمجة الأكثر شعبية في العالم، وتستخدم لإضافة الحيوية والتفاعل لمواقع الويب.");
        seedTranscript(jsLesson1, 50, 100, "يمكن تشغيل جافاسكربت مباشرة في متصفح الويب الخاص بك، وهي المحرك الأساسي لكل منصات الويب وتطبيقات الهاتف الحديثة.");

        // 8. بذر تسجيل اشتراكات الطالب الافتراضي بالانتساب التلقائي للكورسات
        seedEnrollment(student, reactCourse, reactLesson1);
        seedEnrollment(student, laravelCourse, laravelLesson1);
        seedEnrollment(student, jsCourse, jsLesson1);

        log.info("🎉 اكتمال عملية بذر الأدوار والمستخدمين والتصنيفات والكورسات والدروس والاشتراكات بنجاح مطلق!");
    }

    private Role seedRole(String name, String description) {
        return roleRepo.findByName(name).orElseGet(() -> roleRepo.save(Role.builder()
                .name(name)
                .description(description)
                .build()));
    }

    private User seedUser(String fullName, String email, String password, Role role) {
        return userRepo.findByEmail(email).orElseGet(() -> userRepo.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .blocked(false)
                .role(role)
                .build()));
    }

    private Category seedCategory(String name) {
        return categoryRepo.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> categoryRepo.save(Category.builder()
                        .name(name)
                        .build()));
    }

    private Course seedCourse(String title, String description, Category category, User teacher) {
        return courseRepo.findAll().stream()
                .filter(c -> c.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> courseRepo.save(Course.builder()
                        .title(title)
                        .description(description)
                        .category(category)
                        .teacher(teacher)
                        .build()));
    }

    private Lesson seedLesson(String title, String description, String videoUrl, Integer durationSeconds, Course course) {
        return lessonRepo.findAll().stream()
                .filter(l -> l.getTitle().equals(title) && l.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseGet(() -> lessonRepo.save(Lesson.builder()
                        .title(title)
                        .description(description)
                        .videoUrl(videoUrl)
                        .durationSeconds(durationSeconds)
                        .course(course)
                        .build()));
    }

    private void seedTranscript(Lesson lesson, Integer startSecond, Integer endSecond, String content) {
        List<LessonTranscript> existing = transcriptRepo.findByLessonIdOrderByStartSecondAsc(lesson.getId());
        boolean exists = existing.stream().anyMatch(t -> t.getStartSecond().equals(startSecond));
        
        if (!exists) {
            transcriptRepo.save(LessonTranscript.builder()
                    .lesson(lesson)
                    .startSecond(startSecond)
                    .endSecond(endSecond)
                    .transcriptContent(content)
                    .build());
        }
    }

    private void seedEnrollment(User student, Course course, Lesson lastWatched) {
        List<Enrollment> existing = enrollmentRepo.findByUserId(student.getId());
        boolean exists = existing.stream().anyMatch(e -> e.getCourse().getId().equals(course.getId()));
        
        if (!exists) {
            enrollmentRepo.save(Enrollment.builder()
                    .user(student)
                    .course(course)
                    .progressPercentage(15) // تقدم افتراضي بسيط لتجربة واجهة المتابعة
                    .lastWatchedLesson(lastWatched)
                    .build());
        }
    }

    /**
     * دالة مساعدة لنسخ الفيديوهات تلقائياً من مجلد الفيديوهات لمجلد الرفع الخاص بـ Spring Boot.
     */
    private void copyDemoVideos() {
        log.info("🎬 التحقق من وجود فيديوهات الدروس ونسخها لمجلد العينات...");
        File sourceDir = new File("../videos");
        File targetDir = new File("uploads/videos");

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        if (sourceDir.exists() && sourceDir.isDirectory()) {
            File[] files = sourceDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".mp4")) {
                        File destFile = new File(targetDir, file.getName());
                        if (!destFile.exists()) {
                            try {
                                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                log.info("✅ تم نسخ الفيديو بنجاح: {} -> {}", file.getName(), destFile.getPath());
                            } catch (IOException e) {
                                log.error("❌ فشل نسخ الفيديو: {}", file.getName(), e);
                            }
                        }
                    }
                }
            }
        } else {
            log.warn("⚠️ مجلد الفيديوهات الرئيسي غير موجود في: {}", sourceDir.getAbsolutePath());
        }
    }
}
