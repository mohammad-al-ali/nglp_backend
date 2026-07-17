# Prompt for Frontend Agent — AI Quiz Feature

Give this to the opencode agent working on `frontend.v1`:

---

قم ببناء شاشات الكويزات (AI-Generated Quizzes) للواجهة الأمامية. هذا ملف يصف جميع الـ APIs الجديدة التي بنيت في الـ backend، وكيفية بناء الشاشات بنفس تصميم باقي المشروع.

## المصادقة (Authentication)

كل الطلبات ترسل تلقائياً عن طريق `api.js` الموجود:
- Header: `X-User-Id` — معرف المستخدم
- Header: `X-User-Role` — الدور: `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN`

---

## الـ APIs الخاصة بالكويز

### 1. توليد كويز بالذكاء الاصطناعي (Teacher only)

```
POST /quizzes/generate
Headers: X-User-Role: ROLE_TEACHER, X-User-Id: <teacherId>
Body: {
  "lessonId": number,
  "title": "string",
  "numberOfQuestions": number,
  "teacherId": number
}
Response: QuizResponse (full object)
```

**الوصف:** المعلم يختار درس، يكتب عنوان الكويز وعدد الأسئلة → الـ backend يقرأ transcript الدرس ويولّد أسئلة اختيار من متعدد بالذكاء الاصطناعي. الكويز يُحفظ بحالة `DRAFT` — غير مرئي للطلاب.

---

### 2. عرض الكويز كامل (Teacher/Admin)

```
GET /quizzes/{id}
Headers: X-User-Role: ROLE_TEACHER
Response: QuizResponse
```

يرجع الكويز كامل مع `isCorrect` لكل اختيار و `explanation` لكل سؤال.

---

### 3. عرض الكويز للطالب (Student only — بدون إجابات)

```
GET /quizzes/{id}/student-view
Headers: X-User-Role: ROLE_STUDENT
Response: QuizStudentResponse
```

نفس الكويز لكن بدون `isCorrect` وبدون `explanation` — الطالب يشوف فقط نص الأسئلة والاختيارات.

---

### 4. قائمة كويزات درس معين

```
GET /quizzes?lessonId={lessonId}
Headers: X-User-Role, X-User-Id
```

- للمعلم: يرجع كل الكويزات (DRAFT و PUBLISHED) مع الإجابات
- للطالب: يرجع فقط PUBLISHED بدون إجابات

---

### 5. إضافة سؤال يدويًا (Teacher)

```
POST /quizzes/{id}/questions
Body: {
  "questionText": "string",
  "difficultyWeight": number,
  "explanation": "string",
  "choices": [
    {"choiceText": "string", "isCorrect": boolean},
    {"choiceText": "string", "isCorrect": boolean},
    {"choiceText": "string", "isCorrect": boolean},
    {"choiceText": "string", "isCorrect": boolean}
  ]
}
```

يضيف سؤال جديد مع 4 اختيارات بالضبط، اختيار صحيح واحد فقط.

---

### 6. تعديل سؤال (Teacher)

```
PUT /quizzes/{id}/questions/{questionId}
Body: same shape as POST questions
```

---

### 7. حذف سؤال (Teacher)

```
DELETE /quizzes/{id}/questions/{questionId}
```

---

### 8. نشر الكويز (Teacher)

```
POST /quizzes/{id}/publish
```

يغير الحالة من `DRAFT` إلى `PUBLISHED`. لازم يكون في سؤال واحد على الأقل.

---

### 9. بدء محاولة (Student — مسجل في الكورس)

```
POST /quizzes/{id}/attempts
Headers: X-User-Role: ROLE_STUDENT, X-User-Id: <studentId>
Response: { "attemptId": number, "attemptNumber": number, "startedAt": "datetime" }
```

رقم المحاولة يتزايد تلقائياً (أول محاولة = 1، ثاني = 2، إلخ).

---

### 10. تسليم الإجابات والتصحيح (Student)

```
POST /quizzes/attempts/{attemptId}/submit
Headers: X-User-Role: ROLE_STUDENT, X-User-Id: <studentId>
Body: {
  "answers": [
    {"questionId": number, "selectedChoiceId": number},
    {"questionId": number, "selectedChoiceId": number}
  ]
}
Response: QuizAttemptResponse
```

**التصحيح:** التصحيح يحصل فقط عند التسليم الكامل (مو لكل سؤال على حدة). لكل إجابة:
- إذا صح: `pointsAwarded = difficultyWeight`
- إذا خطأ: `pointsAwarded = 0`
- `score` النهائي = مجموع النقاط

في الـ response:
- إذا `showAnswersAfterSubmit == true`: يرجع `correctChoiceId` و `correctChoiceText` و `explanation`
- إذا `false`: يرجع فقط `isCorrect` لكل إجابة بدون تفاصيل إضافية

---

### 11. سجل المحاولات

```
GET /quizzes/attempts?studentId={studentId}&quizId={quizId}
```

الطالب يشوف محاولاته، المعلم يشوف محاولات طلابه، الأدمن الكل.

---

## الـ Response Types

### QuizResponse (للمعلم)
```json
{
  "id": 1,
  "lessonId": 5,
  "title": "اختبار JavaScript",
  "status": "DRAFT",
  "createdByTeacherId": 2,
  "createdAt": "2026-07-17T12:00:00",
  "showAnswersAfterSubmit": true,
  "questions": [
    {
      "id": 10,
      "questionText": "ما هو output الكود التالي؟",
      "difficultyWeight": 5,
      "orderIndex": 1,
      "explanation": "لأن ...",
      "choices": [
        {"id": 100, "choiceText": "A", "isCorrect": true},
        {"id": 101, "choiceText": "B", "isCorrect": false},
        {"id": 102, "choiceText": "C", "isCorrect": false},
        {"id": 103, "choiceText": "D", "isCorrect": false}
      ]
    }
  ]
}
```

### QuizStudentResponse (للطالب قبل التسليم)
نفس الشكل لكن بدون `isCorrect` و `explanation`.

### QuizAttemptResponse (بعد التسليم)
```json
{
  "id": 1,
  "quizId": 1,
  "studentId": 3,
  "attemptNumber": 1,
  "startedAt": "2026-07-17T13:00:00",
  "submittedAt": "2026-07-17T13:05:00",
  "score": 15,
  "answers": [
    {
      "id": 50,
      "questionId": 10,
      "selectedChoiceId": 100,
      "isCorrect": true,
      "pointsAwarded": 5,
      "correctChoiceExplanation": "لأن ...",          // فقط إذا showAnswersAfterSubmit=true
      "correctChoiceId": 100,                          // فقط إذا showAnswersAfterSubmit=true
      "correctChoiceText": "A"                         // فقط إذا showAnswersAfterSubmit=true
    }
  ]
}
```

---

## الشاشات المطلوبة (Screens)

### Teacher Side (صلاحية ROLE_TEACHER)

#### A. صفحة إدارة الكويزات لدرس معين
- المسار: `/teacher/courses/{courseId}/lessons/{lessonId}/quizzes`
- تستخدم `PageFrame` مثل باقي صفحات المعلم
- تعرض قائمة الكويزات الخاصة بهذا الدرس (DRAFT و PUBLISHED)
- كل كويز يظهر: العنوان، الحالة (DRAFT/PUBLISHED)، عدد الأسئلة، تاريخ الإنشاء
- أزرار: "توليد كويز بالذكاء الاصطناعي"، "معاينة"، "نشر"، "حذف"
- زر "توليد كويز" يفتح modal فيه: عنوان الكويز، عدد الأسئلة

#### B. صفحة معاينة/تعديل الكويز
- المسار: `/teacher/quizzes/{quizId}`
- تعرض الأسئلة كاملة مع الإجابات الصحيحة
- كل سؤال يظهر كـ "بطاقة" فيها: نص السؤال، الـ 4 اختيارات (الاختيار الصحيح مميز)، الوزن، التفسير
- أزرار: تعديل سؤال (يفتح modal تعديل)، حذف سؤال، إضافة سؤال يدوي
- زر نشر (publish) إذا الكويز DRAFT
- زر رجوع للقائمة

#### C. Modal إضافة/تعديل سؤال
- نفس تصميم الـ forms الموجودة (TextField)
- حقول: questionText (textarea), difficultyWeight (number), explanation (textarea)
- 4 حقول choice text مع toggle/radio لكل اختيار لتحديد الصحيح
- Validation: لازم 4 اختيارات بالضبط، واحد صحيح فقط

### Student Side (صلاحية ROLE_STUDENT)

#### D. صفحة الكويزات المتاحة لدرس معين
- تظهر ضمن صفحة StudyRoom أو صفحة منفصلة
- تعرض فقط الكويزات `PUBLISHED`
- لكل كويز: عنوان، زر "بدء المحاولة"
- بجانب كل كويز: سجل المحاولات السابقة (رقم المحاولة، النتيجة، هل تم التسليم)

#### E. صفحة أداء الكويز (Take Quiz)
- تعرض الأسئلة واحد تلو الآخر أو كلها مرة واحدة (اختيارك)
- كل سؤال: نص + 4 اختيارات (راديوهات أو أزرار)
- الطالب يحدد إجاباته ثم يضغط "تسليم" مرة واحدة (مو تصحيح فوري لكل سؤال)
- Timer اختياري للتحسين المستقبلي

#### F. صفحة النتيجة بعد التسليم
- تعرض النتيجة النهائية (مثلاً: "15/20")
- تعرض كل سؤال مع:
  - إجابة الطالب (صح/خطأ)
  - إذا `showAnswersAfterSubmit == true`: الإجابة الصحيحة + التفسير
  - إذا `false`: فقط درجة السؤال بدون تفاصيل
- زر "عودة" أو "محاولة مرة أخرى"

---

## تعليمات التطبيق (Implementation Notes)

1. **اتبع نفس نمط باقي المشروع:**
   - استخدم `PageFrame` للصفحات
   - استخدم `api.js` الموجود (axios instance) لطلبات HTTP
   - استخدم Lucide React للأيقونات
   - استخدم inline styles مع متغيرات CSS من `index.css`
   - استخدم RTL (Arabic-first)
   - استخدم `RoleRoute` لحماية المسارات

2. **إدارة الحالة:**
   - استخدم custom hooks مثل `useFetchQuiz`, `useSubmitQuiz` في `src/hooks/`
   - أضف `normalizeQuiz()` في `src/utils/constants.js`
   - تعامل مع حالات: loading, error, empty, success

3. **المسارات المقترحة:**
   ```
   /teacher/courses/:courseId/lessons/:lessonId/quizzes    → قائمة الكويزات
   /teacher/quizzes/:quizId                                 → معاينة/تعديل
   /study/:courseId/lesson/:lessonId/quiz/:quizId           → أداء الكويز
   /quiz-result/:attemptId                                  → النتيجة
   ```

4. **نظام الألوان والتصميم:**
   - DRAFT: لون كهرماني/أصفر فاتح `var(--warning)`
   - PUBLISHED: لون أخضر `var(--success)`
   - الإجابة الصحيحة: أخضر
   - الإجابة الخطأ: أحمر `var(--error)`
   - نفس الـ border-radius, shadows, fonts من `index.css`

5. **الاعتبارات الأمنية:**
   - الطالب ما يشوف `isCorrect` ولا `explanation` إلا بعد التسليم وفقط إذا `showAnswersAfterSubmit == true`
   - المعلم بس اللي يقدر يعدل/ينشر الكويز (يُفحص في الـ backend, لكن خلي الـ UI يخفي الأزرار حسب الدور)

6. **البيانات التجريبية (Seed Data):**
   - في `DataInitializer.java` بيانات تجريبية: أدوار، مستخدمين (student@nglp.com, teacher@nglp.com), كورسات، دروس
   - بعد تشغيل الـ backend، تقدر تختبر الكويزات على الدروس الموجودة

7. **ملاحظات على واجهة المعلم:**
   - صفحة إدارة الكويزات الأفضل تكون ضمن تدفق `CourseBuilder` / `ManageLessons`
   - مثلاً: في صفحة `ManageLessons.jsx` إضافة تبويب (Tab) لإدارة الكويزات لكل درس
   - أو صفحة مستقلة برابط من `ManageLessons`

8. **ملاحظات على واجهة الطالب:**
   - صفحة `StudyRoom.jsx` حالياً فيها video + AI chat + details
   - الأفضل إضافة زر "الكويزات" في الـ StatusBar أو DetailsPanel
   - أو تحويلها إلى تبويب في نفس الـ StudyRoom
