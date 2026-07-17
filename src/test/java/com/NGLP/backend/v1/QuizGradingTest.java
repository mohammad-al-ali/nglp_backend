package com.NGLP.backend.v1;

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

@SpringBootTest
@Transactional
class QuizGradingTest {

    @Autowired private QuizService quizService;
    @Autowired private QuizRepo quizRepo;
    @Autowired private QuizQuestionRepo questionRepo;
    @Autowired private QuizChoiceRepo choiceRepo;
    @Autowired private QuizAttemptRepo attemptRepo;
    @Autowired private QuizAnswerRepo answerRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private LessonRepo lessonRepo;
    @Autowired private CourseRepo courseRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private EnrollmentRepo enrollmentRepo;

    private User teacher;
    private User student;
    private Lesson lesson;
    private Quiz quiz;
    private QuizQuestion q1;
    private QuizQuestion q2;
    private QuizChoice q1CorrectChoice;
    private QuizChoice q1WrongChoice;
    private QuizChoice q2CorrectChoice;
    private QuizChoice q2WrongChoice;

    @BeforeEach
    void setUp() {
        Role teacherRole = roleRepo.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));
        Role studentRole = roleRepo.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepo.save(Role.builder().name("ROLE_STUDENT").description("Student").build()));

        teacher = userRepo.findByEmail("teacher@nglp.com").orElseGet(() ->
                userRepo.save(User.builder().fullName("Teacher").email("teacher@nglp.com").password("pass")
                        .blocked(false).role(teacherRole).build()));
        student = userRepo.findByEmail("student@nglp.com").orElseGet(() ->
                userRepo.save(User.builder().fullName("Student").email("student@nglp.com").password("pass")
                        .blocked(false).role(studentRole).build()));

        List<Lesson> allLessons = lessonRepo.findAll();
        if (allLessons.isEmpty()) {
            Course course = courseRepo.save(Course.builder().title("Test Course").description("Test").teacher(teacher).build());
            lesson = lessonRepo.save(Lesson.builder().title("Test Lesson").description("Test")
                    .durationSeconds(100).course(course).build());
        } else {
            lesson = allLessons.get(0);
        }

        if (enrollmentRepo.findByUserIdAndCourseId(student.getId(), lesson.getCourse().getId()).isEmpty()) {
            enrollmentRepo.save(Enrollment.builder()
                    .user(student).course(lesson.getCourse())
                    .progressPercentage(0).build());
        }

        setUpQuiz();
    }

    private void setUpQuiz() {
        quiz = Quiz.builder()
                .lesson(lesson)
                .title("Test Quiz")
                .status("DRAFT")
                .createdByTeacher(teacher)
                .createdAt(LocalDateTime.now())
                .showAnswersAfterSubmit(true)
                .questions(new ArrayList<>())
                .build();

        q1 = QuizQuestion.builder()
                .quiz(quiz)
                .questionText("What is 2+2?")
                .difficultyWeight(5)
                .orderIndex(1)
                .explanation("Basic math")
                .choices(new ArrayList<>())
                .build();

        q1CorrectChoice = QuizChoice.builder().question(q1).choiceText("4").isCorrect(true).build();
        q1WrongChoice = QuizChoice.builder().question(q1).choiceText("5").isCorrect(false).build();
        QuizChoice q1c3 = QuizChoice.builder().question(q1).choiceText("6").isCorrect(false).build();
        QuizChoice q1c4 = QuizChoice.builder().question(q1).choiceText("7").isCorrect(false).build();
        q1.setChoices(List.of(q1CorrectChoice, q1WrongChoice, q1c3, q1c4));

        q2 = QuizQuestion.builder()
                .quiz(quiz)
                .questionText("What is 3*3?")
                .difficultyWeight(10)
                .orderIndex(2)
                .explanation("Multiplication")
                .choices(new ArrayList<>())
                .build();

        q2CorrectChoice = QuizChoice.builder().question(q2).choiceText("9").isCorrect(true).build();
        q2WrongChoice = QuizChoice.builder().question(q2).choiceText("8").isCorrect(false).build();
        QuizChoice q2c3 = QuizChoice.builder().question(q2).choiceText("7").isCorrect(false).build();
        QuizChoice q2c4 = QuizChoice.builder().question(q2).choiceText("6").isCorrect(false).build();
        q2.setChoices(List.of(q2CorrectChoice, q2WrongChoice, q2c3, q2c4));

        quiz.setQuestions(List.of(q1, q2));
        quiz.setStatus("PUBLISHED");
        quiz = quizRepo.save(quiz);

        q1 = questionRepo.findById(q1.getId()).orElseThrow();
        q2 = questionRepo.findById(q2.getId()).orElseThrow();
    }

    @Test
    void testAllCorrectAnswersGivesFullScore() {
        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());

        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1CorrectChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2CorrectChoice.getId())
        ));

        var response = quizService.submitAttempt(attempt.getId(), request);

        assertEquals(15, response.score(), "Full score should be 5 + 10 = 15");
    }

    @Test
    void testAllWrongAnswersGivesZeroScore() {
        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());

        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1WrongChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2WrongChoice.getId())
        ));

        var response = quizService.submitAttempt(attempt.getId(), request);

        assertEquals(0, response.score(), "Score should be 0 when all answers are wrong");
    }

    @Test
    void testPartialCorrectAnswersGivesPartialScore() {
        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());

        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1CorrectChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2WrongChoice.getId())
        ));

        var response = quizService.submitAttempt(attempt.getId(), request);

        assertEquals(5, response.score(), "Score should be 5 (only first correct, weight=5)");
    }

    @Test
    void testCannotSubmitAlreadySubmittedAttempt() {
        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());

        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1CorrectChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2CorrectChoice.getId())
        ));

        quizService.submitAttempt(attempt.getId(), request);

        assertThrows(IllegalStateException.class, () ->
                quizService.submitAttempt(attempt.getId(), request));
    }

    @Test
    void testMultipleAttemptsHaveIncreasingNumbers() {
        QuizAttempt a1 = quizService.startAttempt(quiz.getId(), student.getId());
        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1CorrectChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2CorrectChoice.getId())
        ));
        quizService.submitAttempt(a1.getId(), request);

        QuizAttempt a2 = quizService.startAttempt(quiz.getId(), student.getId());

        assertEquals(1, a1.getAttemptNumber());
        assertEquals(2, a2.getAttemptNumber());
    }

    @Test
    void testShowAnswersAfterSubmitReturnsCorrectAnswer() {
        QuizAttempt attempt = quizService.startAttempt(quiz.getId(), student.getId());

        QuizSubmitRequest request = new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.AnswerEntry(q1.getId(), q1CorrectChoice.getId()),
                new QuizSubmitRequest.AnswerEntry(q2.getId(), q2CorrectChoice.getId())
        ));

        var response = quizService.submitAttempt(attempt.getId(), request);

        assertNotNull(response.answers());
        assertEquals(2, response.answers().size());
        assertTrue(response.answers().get(0).isCorrect());
        assertNotNull(response.answers().get(0).correctChoiceExplanation());
        assertNotNull(response.answers().get(0).correctChoiceId());
    }
}
