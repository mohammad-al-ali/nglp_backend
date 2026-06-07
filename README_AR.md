#  مشروع NGLP Backend  - الدليل الشامل للمطورين

أهلاً بك في الدليل التعريفي والتشغيلي لمشروع
**NGLP (Next Generation Learning Platform)**.

هذا المشروع يمثل الواجهة الخلفية لمنصة تعليمية ذكية تعتمد على تقنيات الذكاء الاصطناعي لتوفير تجربة تعليمية مخصصة وتفاعلية للطلاب.

---

## 📋 نظرة عامة على المشروع

 **NGLP**

 هي نظام إداري وتعليمي متكامل يدعم أدوار مستخدمين متعددة (طالب، معلم، مدير نظام)، ويتميز بوجود **معلم ذكي** مدمج يساعد الطلاب أثناء مشاهدة الدروس من خلال الإجابة على استفساراتهم.

### أهم الميزات:
- 🔐 **إدارة الحسابات والصلاحيات**: مصادقة المستخدمين وتسجيلهم بصلاحيات محددة عبر Spring Security.
- 🎓 **إدارة الكورسات والدروس**: إنشاء الكورسات وتقسيمها إلى دروس، وربط الفيديوهات بها.
- 💬 **تفريغ نصوص الفيديوهات (Transcripts)**: إتاحة النصوص التفصيلية لكل مقطع فيديو حسب التوقيت الزمني.
- 🤖 **المعلم الذكي (AI Tutor)**: مساعد تفاعلي يستخدم نموذج الذكاء الاصطناعي LLaMA 3.3 عبر Groq API للإجابة على الأسئلة داخل نطاق محتوى الدرس فقط.
- 💾 **قاعدة بيانات سريعة وسهلة (H2 Database)**: استخدام قاعدة بيانات H2 المضمنة التي تعمل في الذاكرة لتسهيل التطوير والاختبار دون تعقيدات تثبيت قواعد بيانات خارجية.

---

## 🏗️ هيكلية المشروع والتقنيات المستخدمة

تم بناء المشروع باستخدام **Java 20** وإطار العمل الشهير **Spring Boot 4.0.6**. يعتمد المشروع على معمارية MVC التقليدية المقسمة كالتالي:

| الطبقة (Layer) | وظيفتها |
| :--- | :--- |
| **Entities** | تمثل جداول قاعدة البيانات وخصائصها (الجداول البرمجية). |
| **Repositories (Repos)** | تتعامل مباشرة مع قاعدة البيانات لجلب وتعديل وحذف البيانات (JPA). |
| **Services** | تحتوي على منطق العمل البرمجي (Business Logic) والعمليات الحسابية والتحقق. |
| **Controllers** | تمثل نقاط الوصول (API Endpoints) التي يستدعيها الواجهة الأمامية (Frontend). |
| **DTOs (Data Transfer Objects)** | كائنات لنقل البيانات المخصصة بين الواجهات والـ API لتقليل حجم البيانات وتأمينها. |

---

## 📂 دليل الأهداف والمقدمات لملفات المشروع (File-by-File Guide)

فيما يلي شرح تفصيلي لـ **الهدف والوظيفة الأساسية** لكل حزمة (Package) ولكل ملف كود برمجي في المشروع لمساعدتك كمبتدئ على معرفة أين يقع كل جزء برمي:

### 1. الحزمة الرئيسية (`com.NGLP.backend.v1`)
- **[Application.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/Application.java)**: 
  * *الهدف*: نقطة انطلاق تشغيل برنامج Spring Boot بأكمله. يحتوي على الدالة الرئيسية `main`.
- **[DataInitializer.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/DataInitializer.java)**:
  * *الهدف*: بذر البيانات الافتراضية (Seeding) تلقائياً في قاعدة البيانات عند إقلاع البرنامج (مثل إنشاء أدوار الطالب والمعلم والمدير، وإضافة كورسات افتراضية ودروس ونصوص تفريغ لتجربة النظام مباشرة).

---

### 2. حزمة الكيانات وجداول البيانات (`entity`)
تضم الكلاسات التي تمثل الجداول في قاعدة بيانات H2 وتحدد العلاقات بينها:
- **[User.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/User.java)**: جدول المستخدمين (الاسم، البريد الإلكتروني، كلمة المرور المشفرة، وهل الحساب محظور أم لا).
- **[Role.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Role.java)**: صلاحيات وأدوار النظام (ADMIN, TEACHER, STUDENT).
- **[Category.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Category.java)**: تصنيفات الكورسات (مثل: برمجة ويب، ذكاء اصطناعي).
- **[Course.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Course.java)**: بيانات الكورسات الأساسية (العنوان، الوصف، المعلم، والتصنيف).
- **[Lesson.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Lesson.java)**: الدروس التابعة لكل كورس (العنوان، رابط الفيديو، والمدة).
- **[LessonTranscript.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/LessonTranscript.java)**: النصوص المفرغة لكل درس مع تحديد التوقيت الزمني (Start/End seconds) لربط الأسئلة بلحظة معينة في الفيديو.
- **[Enrollment.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Enrollment.java)**: تتبع التحاق الطلاب بالكورسات ونسبة تقدمهم والدرس الأخير الذي تمت مشاهدته.
- **[Conversation.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Conversation.java)**: يمثل جلسة حوارية واحدة بين الطالب والمعلم الذكي (AI Tutor) لدرس محدد.
- **[Msg.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/entity/Msg.java)**: الرسائل المتبادلة داخل كل محادثة (سواء كانت من الطالب أو إجابة من الذكاء الاصطناعي).

---

### 3. حزمة مستودعات البيانات (`repo`)
واجهات برمجية (Interfaces) ترث من `JpaRepository` لتمكين الاستعلام التلقائي من قاعدة البيانات بدون كتابة أكواد SQL معقدة:
- **[UserRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/UserRepo.java)**: استعلامات المستخدمين (مثل البحث بالبريد الإلكتروني).
- **[RoleRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/RoleRepo.java)**: استعلامات الأدوار (مثل البحث باسم الدور).
- **[CategoryRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/CategoryRepo.java)**: استعلامات تصنيف الكورسات.
- **[CourseRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/CourseRepo.java)**: استعلامات الكورسات.
- **[LessonRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/LessonRepo.java)**: استعلامات الدروس.
- **[LessonTranscriptRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/LessonTranscriptRepo.java)**: جلب النصوص التفريغية لدرس معين بناءً على ترتيب الثواني.
- **[EnrollmentRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/EnrollmentRepo.java)**: جلب الكورسات التي سجل فيها طالب معين وتحديث تقدمه.
- **[ConversationRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/ConversationRepo.java)**: جلب المحادثات الخاصة بمستخدم معين.
- **[MsgRepo.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/repo/MsgRepo.java)**: جلب سجل الرسائل التابع لمحادثة معينة لتقديمه للـ AI كذاكرة قصيرة المدى.

---

### 4. حزمة منطق العمل والخدمات (`service`)
تحتوي على العمليات البرمجية الفعلية لكل قسم:
- **[UserService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/UserService.java)**: إضافة مستخدمين، حظرهم، وتشفير كلمات المرور.
- **[RoleService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/RoleService.java)**: إدارة الأدوار والصلاحيات.
- **[CategoryService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/CategoryService.java)**: إدارة التصنيفات.
- **[CourseService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/CourseService.java)**: منطق إضافة وتحديث الكورسات.
- **[LessonService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/LessonService.java)**: ربط الدروس بالفيديوهات وإدارتها.
- **[LessonTranscriptService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/LessonTranscriptService.java)**: معالجة النصوص وحساب التوقيت لتقديم النص البرمجي الصحيح للـ AI.
- **[EnrollmentService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/EnrollmentService.java)**: معالجة عمليات التسجيل في الكورسات وحساب التقدم التلقائي.
- **[ConversationService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/ConversationService.java)**: إنشاء محادثات مع الـ AI وحفظها في قاعدة البيانات.
- **[MsgService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/MsgService.java)**: حفظ الرسائل وعرضها.
- **[FileStorageService.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/service/FileStorageService.java)**: التعامل مع رفع ملفات الفيديو وتخزينها محلياً في مجلد `uploads/videos/`.

---

### 5. حزمة التحكم ونقاط الوصول (`controller`)
تقوم باستلام الطلبات (HTTP Requests) وتوجيهها للخدمات المناسبة وإرجاع الإجابات (JSON):
- **[AuthController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/AuthController.java)**: تسجيل الدخول وإنشاء حساب جديد.
- **[UserController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/UserController.java)**: عمليات إدارة المستخدمين (عرض، تعديل، حذف).
- **[CategoryController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/CategoryController.java)**: استدعاء عمليات التصنيفات.
- **[CourseController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/CourseController.java)**: إتاحة مسارات الكورسات.
- **[LessonController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/LessonController.java)**: مسارات الدروس وربطها بالتفريغ النصي.
- **[LessonTranscriptController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/LessonTranscriptController.java)**: جلب النصوص المفرغة للدرس.
- **[EnrollmentController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/EnrollmentController.java)**: مسارات تسجيل الطلاب وتحديث تقدمهم.
- **[ConversationController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/ConversationController.java)**: جلب وإدارة حوارات الـ AI.
- **[RoleController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/RoleController.java)**: استعراض الصلاحيات المتوفرة.
- **[AiController.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/controller/AiController.java)**: واجهة التفاعل الرئيسية مع المعلم الذكي لإرسال السؤال واستلام الإجابة.

---

### 6. حزمة الذكاء الاصطناعي (`ai`)
الحزمة البرمجية المسؤولة عن دمج خوارزميات الـ AI مع الـ API:
- **[NglpAiAgent.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/ai/NglpAiAgent.java)**:
  * *الهدف*: هذا هو العقل المفكر للمعلم الذكي. يقوم بصياغة التوجيهات الأساسية (System Prompt) باللغة العربية وإصدار الأوامر لنموذج LLaMA.
- **[DatabaseChatMemory.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/ai/DatabaseChatMemory.java)**:
  * *الهدف*: ربط ذاكرة الحوار بين المستخدم والذكاء الاصطناعي بجدول قاعدة البيانات `Msg` و `Conversation` بحيث يتذكر المعلم الذكي سياق الأسئلة السابقة ولا ينسى الحوار بمجرد تحديث الصفحة.
- **[AiToolsConfig.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/ai/AiToolsConfig.java)**:
  * *الهدف*: تعريف الأدوات المساعدة للذكاء الاصطناعي (Function Calling). تتيح هذه الأدوات للـ AI أن يقرر بنفسه استدعاء كود برمجى لجلب نصوص الدرس وتفريغ الفيديو عند الحاجة للإجابة بدقة.

---

### 7. حزم الحماية والتكوين (`security` & `utility`)
- **[SecurityConfig.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/security/SecurityConfig.java)**:
  * *الهدف*: تأمين التطبيق وحظر الوصول غير المصرح به للـ APIs. يشمل تشفير كلمات المرور وتحديد من يستطيع الوصول لكل مسار بناءً على الدور (ROLE).
- **[WebConfig.java](file:///d:/ProjectFifthYear/NGLP/backend.v1/src/main/java/com/NGLP/backend/v1/utility/WebConfig.java)**:
  * *الهدف*: إعدادات الويب العامة مثل السماح بمشاركة الموارد عبر الأنظمة المختلفة (CORS) لتمكين الواجهة الأمامية من الاتصال بالسيرفر.

---

## 💾 إعداد وتشغيل قاعدة بيانات H2 المضمنة (H2 Console)

يستخدم هذا المشروع قاعدة بيانات **H2** بشكل مستمر وتخزينها في ملف محلي (تجنباً لفقدان البيانات عند إعادة تشغيل البرنامج).

### خصائص الاتصال الافتراضية في ملف `application.properties`:
```properties
# مسار الاتصال بقاعدة البيانات محلياً وحفظ البيانات في مجلد data داخل المشروع
spring.datasource.url=jdbc:h2:file:./data/nglp_db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# تفعيل واجهة التحكم في قاعدة البيانات عبر الويب H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### طريقة الدخول إلى H2 Console ومراجعة الجداول:
1. قم بتشغيل سيرفر Spring Boot أولاً.
2. افتح متصفح الويب واذهب إلى الرابط: `http://localhost:8080/h2-console`
3. ستظهر لك شاشة تسجيل الدخول التابعة لقاعدة البيانات، قم بإدخال البيانات التالية بدقة:
   - **Saved Settings**: Generic H2 (Embedded)
   - **Driver Class**: `org.h2.Driver`
   - **JDBC URL**: `jdbc:h2:file:./data/nglp_db` *(هام جداً: يجب تطابقه تماماً مع القيمة الموجودة في ملف application.properties)*
   - **User Name**: `sa`
   - **Password**: *(اتركها فارغة)*
4. اضغط على زر **Connect** لتستعرض الجداول والبيانات التي تم بذرها تلقائياً.

---

## 🤖 آلية عمل المعلم الذكي (AI Tutor) والتكامل مع Groq

يتميز هذا النظام بتقديم دعم فوري للطالب يعتمد على سياق الدرس الحالي:
1. **استلام السؤال**: يرسل الطالب استفساره عبر مسار الـ API `/api/ai/messages`.
2. **استدعاء الأدوات (Function Calling)**: يعلم نموذج LLaMA 3.3 أنه بحاجة إلى تفريغ نصي للدرس ليجيب بدقة، فيقوم باستدعاء الأداة المبرمجة `fetchLessonTranscript` بشكل تلقائي.
3. **جلب التفريغ النصي**: يقوم السيرفر بالبحث في قاعدة بيانات H2 عن النصوص التي تقع في الفترة الزمنية للدرس.
4. **تجميع السياق وصياغة الإجابة**: يدمج السيرفر السؤال مع النص التفريغي وذاكرة المحادثة، ويرسلها للنموذج عبر Groq API.
5. **إرسال الإجابة باللغة العربية**: يقوم النموذج بالرد بإجابة مفصلة وودودة باللغة العربية، ويتم حفظ هذه الإجابة في قاعدة البيانات لتحديث ذاكرة المحادثة تلقائياً.

---

## 🚀 طريقة تشغيل المشروع للمبتدئين

يرجى اتباع الخطوات التالية لتشغيل السيرفر على جهازك الشخصي:

### 1. المتطلبات المسبقة:
- تثبيت حزمة التطوير **Java Development Kit (JDK) 20** أو أحدث.
- تثبيت **Maven** (أو استخدام أداة التغليف المرفقة بالمشروع `mvnw`).

### 2. إعداد مفتاح الذكاء الاصطناعي (Groq API Key):
تأكد من وجود مفتاح الـ API الخاص بـ Groq في ملف `src/main/resources/application.properties` على النحو التالي:
```properties
spring.ai.openai.api-key=مفتاح_Groq_الخاص_بك
```

### 3. بناء وتشغيل المشروع:
افتح موجه الأوامر (Terminal/CMD) في المجلد الرئيسي للمشروع ونفذ الأمر التالي:
```bash
# بناء المشروع
./mvnw clean compile

# تشغيل التطبيق
./mvnw spring-boot:run
```

بمجرد تشغيل السيرفر بنجاح، يمكنك الوصول إلى واجهة المطورين واختبار جميع مسارات الـ API عبر **Swagger UI**:
👉 **`http://localhost:8080/swagger-ui/index.html`**
