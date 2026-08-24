package com.NGLP.backend.v1;

import com.NGLP.backend.v1.dto.QuizQuestionRequest;
import com.NGLP.backend.v1.dto.QuizSubmitRequest;
import com.NGLP.backend.v1.entity.*;
import com.NGLP.backend.v1.repo.*;
import com.NGLP.backend.v1.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * اختبارات وحدة إضافية تغطي مسارات التحقق (Validation) والحالات الحدّية
 * في QuizService، والتي لم تكن مشمولة في QuizGradingTest الأصلي
 * (الذي يغطي فقط منطق حساب النتيجة في المسار السعيد Happy Path).
 * أُضيفت هذه الاختبارات أثناء إعداد الفصل السادس من الأطروحة (الاختبار والنتائج)
 * بعد مراجعة الكود الفعلي لـ QuizService وملاحظة أن حالات الرفض
 * (IllegalStateException / IllegalArgumentException) غير مغطاة باختبارات.
 */
@SpringBootTest
@Transactional
class QuizValidationTest {

    @Autowired private QuizService quizService;
    @Autowired private QuizRepo quizRepo;
    @Autowired private QuizQuestionRepo questionRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private LessonRepo lessonRepo;
    @Autowired private CourseRepo courseRepo;
    @Autowired private RoleRepo roleRepo;

    private User teacher;
    private User student;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        Role teacherRole = roleRepo.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));
        Role studentRole = roleRepo.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_STUDENT").description("Student").build()));

        teacher = userRepo.findByEmail("teacher.validation@nglp.com").orElseGet(() ->
                userRepo.save(User.builder().fullName("Teacher V").email("teacher.validation@nglp.com").password("pass")
                        .blocked(false).role(teacherRole).build()));
        student = userRepo.findByEmail("student.validation@nglp.com").orElseGet(() ->
                userRepo.save(User.builder().fullName("Student V").email("student.validation@nglp.com").password("pass")
                        .blocked(false).role(studentRole).build()));

        Course course = courseRepo.save(Course.builder().title("Validation Course").description("Test").teacher(teacher).build());
        lesson = lessonRepo.save(Lesson.builder().title("Validation Lesson").description("Test")
                .durationSeconds(100).course(course).build());
    }

    private Quiz newDraftQuizWithOneQuestion() {
        Quiz quiz = Quiz.builder()
                .lesson(lesson).title("Draft Quiz").status("DRAFT")
                .createdByTeacher(teacher).createdAt(LocalDateTime.now())
                .showAnswersAfterSubmit(true).questions(new ArrayList<>())
                .build();

        QuizQuestion q = QuizQuestion.builder()
                .quiz(quiz).questionText("1+1?").difficultyWeight(5).orderIndex(1)
                .explanation("basic").choices(new ArrayList<>()).build();
        QuizChoice c1 = QuizChoice.builder().question(q).choiceText("2").isCorrect(true).build();
        QuizChoice c2 = QuizChoice.builder().question(q).choiceText("3").isCorrect(false).build();
        QuizChoice c3 = QuizChoice.builder().question(q).choiceText("4").isCorrect(false).build();
        QuizChoice c4 = QuizChoice.builder().question(q).choiceText("5").isCorrect(false).build();
        q.setChoices(List.of(c1, c2, c3, c4));
        quiz.setQuestions(List.of(q));

        return quizRepo.save(quiz);
    }

    @Test
    void testCannotStartAttemptOnDraftQuiz() {
        Quiz draftQuiz = newDraftQuizWithOneQuestion(); // status = DRAFT, لم يُنشر بعد

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> quizService.startAttempt(draftQuiz.getId(), student.getId()));
        assertTrue(ex.getMessage().contains("غير منشور"));
    }

    @Test
    void testCannotPublishQuizWithoutQuestions() {
        Quiz emptyQuiz = Quiz.builder()
                .lesson(lesson).title("Empty Quiz").status("DRAFT")
                .createdByTeacher(teacher).createdAt(LocalDateTime.now())
                .showAnswersAfterSubmit(true).questions(new ArrayList<>())
                .build();
        emptyQuiz = quizRepo.save(emptyQuiz);

        Long emptyQuizId = emptyQuiz.getId();
        assertThrows(IllegalStateException.class, () -> quizService.publishQuiz(emptyQuizId));
    }

    @Test
    void testAddQuestionRejectsWrongChoiceCount() {
        Quiz quiz = newDraftQuizWithOneQuestion();

        QuizQuestionRequest badRequest = new QuizQuestionRequest(
                "سؤال بثلاث خيارات فقط", 5, "شرح",
                List.of(
                        new QuizQuestionRequest.ChoiceEntry("أ", true),
                        new QuizQuestionRequest.ChoiceEntry("ب", false),
                        new QuizQuestionRequest.ChoiceEntry("ج", false)
                )
        );

        Long quizId = quiz.getId();
        assertThrows(IllegalArgumentException.class, () -> quizService.addQuestion(quizId, badRequest));
    }

    @Test
    void testAddQuestionRejectsMultipleCorrectChoices() {
        Quiz quiz = newDraftQuizWithOneQuestion();

        QuizQuestionRequest badRequest = new QuizQuestionRequest(
                "سؤال بإجابتين صحيحتين", 5, "شرح",
                List.of(
                        new QuizQuestionRequest.ChoiceEntry("أ", true),
                        new QuizQuestionRequest.ChoiceEntry("ب", true),
                        new QuizQuestionRequest.ChoiceEntry("ج", false),
                        new QuizQuestionRequest.ChoiceEntry("د", false)
                )
        );

        Long quizId = quiz.getId();
        assertThrows(IllegalArgumentException.class, () -> quizService.addQuestion(quizId, badRequest));
    }

    @Test
    void testAddQuestionRejectsZeroCorrectChoices() {
        Quiz quiz = newDraftQuizWithOneQuestion();

        QuizQuestionRequest badRequest = new QuizQuestionRequest(
                "سؤال بلا إجابة صحيحة", 5, "شرح",
                List.of(
                        new QuizQuestionRequest.ChoiceEntry("أ", false),
                        new QuizQuestionRequest.ChoiceEntry("ب", false),
                        new QuizQuestionRequest.ChoiceEntry("ج", false),
                        new QuizQuestionRequest.ChoiceEntry("د", false)
                )
        );

        Long quizId = quiz.getId();
        assertThrows(IllegalArgumentException.class, () -> quizService.addQuestion(quizId, badRequest));
    }

    @Test
    void testAnswersHiddenWhenShowAnswersAfterSubmitDisabled() {
        Quiz quiz = newDraftQuizWithOneQuestion();
        quiz.setStatus("PUBLISHED");
        quiz.setShowAnswersAfterSubmit(false); // <-- الفرق الجوهري عن اختبار QuizGradingTest الأصلي
        quiz = quizRepo.save(quiz);

        QuizQuestion q = quiz.getQuestions().get(0);
        Long correctChoiceId = q.getChoices().stream().filter(c -> Boolean.TRUE.equals(c.getIsCorrect()))
                .findFirst().orElseThrow().getId();

        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());
        QuizSubmitRequest request = new QuizSubmitRequest(
                List.of(new QuizSubmitRequest.AnswerEntry(q.getId(), correctChoiceId)));

        var response = quizService.submitAttempt(attempt.getId(), request);

        assertEquals(5, response.score(), "النتيجة يجب أن تُحسب بغض النظر عن إعداد إظهار الإجابات");
        assertTrue(response.answers().get(0).isCorrect(), "علم الصحة يبقى ظاهراً دوماً");
        assertNull(response.answers().get(0).correctChoiceExplanation(), "الشرح يجب أن يُخفى عند تعطيل الإعداد");
        assertNull(response.answers().get(0).correctChoiceId(), "معرّف الإجابة الصحيحة يجب أن يُخفى عند تعطيل الإعداد");
    }

    @Test
    void testUpdateQuestionRejectsQuestionFromDifferentQuiz() {
        Quiz quizA = newDraftQuizWithOneQuestion();
        Quiz quizB = newDraftQuizWithOneQuestion();
        Long questionFromQuizA = quizA.getQuestions().get(0).getId();

        QuizQuestionRequest updateRequest = new QuizQuestionRequest(
                "محاولة تعديل سؤال لا ينتمي للكويز", 5, "شرح", null);

        Long quizBId = quizB.getId();
        assertThrows(IllegalStateException.class,
                () -> quizService.updateQuestion(quizBId, questionFromQuizA, updateRequest));
    }
}
