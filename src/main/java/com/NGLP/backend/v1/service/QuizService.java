package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.ai.QuizAiService;
import com.NGLP.backend.v1.dto.*;
import com.NGLP.backend.v1.entity.*;
import com.NGLP.backend.v1.repo.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepo quizRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizChoiceRepo choiceRepo;
    private final QuizAttemptRepo attemptRepo;
    private final QuizAnswerRepo answerRepo;
    private final LessonRepo lessonRepo;
    private final UserRepo userRepo;
    private final QuizAiService quizAiService;

    // ──────────────────────────────────────────────
    //  AI Generation
    // ──────────────────────────────────────────────

    @Transactional
    public QuizResponse generateQuiz(QuizGenerateRequest request) {

        Lesson lesson = lessonRepo.findById(request.lessonId())
                .orElseThrow(() -> new EntityNotFoundException("الدرس غير موجود"));
        log.info("🤖 بدء توليد كويز بالذكاء الاصطناعي للدرس {} بعدد أسئلة {}", request.lessonId(), request.numberOfQuestions());

        User user =userRepo.findById(request.teacherId())
                .orElseThrow(() -> new EntityNotFoundException("المعلم غير موجود"));

        QuizAiService.AiQuizResponse aiResponse = quizAiService.generateQuizQuestions(
                request.lessonId(), request.numberOfQuestions());
        log.info("✅ الذكاء الاصطناعي ولد {} سؤال", aiResponse.questions().size());

        Quiz quiz = Quiz.builder()
                .lesson(lesson)
                .title(request.title())
                .status("DRAFT")
                .createdByTeacher(user)
                .createdAt(LocalDateTime.now())
                .showAnswersAfterSubmit(true)
                .questions(new ArrayList<>())
                .build();

        List<QuizQuestion> questions = new ArrayList<>();
        for (QuizAiService.AiQuizResponse.AiQuestion aiQ : aiResponse.questions()) {
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(aiQ.questionText())
                    .difficultyWeight(aiQ.difficultyWeight())
                    .orderIndex(aiQ.orderIndex())
                    .explanation(aiQ.explanation())
                    .choices(new ArrayList<>())
                    .build();

            List<QuizChoice> choices = new ArrayList<>();
            for (QuizAiService.AiQuizResponse.AiChoice aiC : aiQ.choices()) {
                choices.add(QuizChoice.builder()
                        .question(question)
                        .choiceText(aiC.choiceText())
                        .isCorrect(aiC.isCorrect())
                        .build());
            }
            validateChoices(choices);
            question.setChoices(choices);
            questions.add(question);
        }

        quiz.setQuestions(questions);
        Quiz saved = quizRepo.save(quiz);
        log.info("💾 تم حفظ الكويز ID={} بعنوان '{}' بحالة DRAFT", saved.getId(), saved.getTitle());
        return toQuizResponse(saved);
    }

    // ──────────────────────────────────────────────
    //  Read Operations
    // ──────────────────────────────────────────────

    public QuizResponse findById(Long quizId) {
        return toQuizResponse(findQuizOrThrow(quizId));
    }

    public QuizStudentResponse findStudentView(Long quizId) {
        return toStudentView(findQuizOrThrow(quizId));
    }

    public List<?> findByLesson(Long lessonId) {
        List<Quiz> quizzes = quizRepo.findByLessonId(lessonId);
        log.debug("تم جلب {} كويز للدرس {}", quizzes.size(), lessonId);
        return quizzes.stream()
                .map(this::toQuizResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    //  Teacher Edit Operations
    // ──────────────────────────────────────────────

    @Transactional
    public QuizResponse addQuestion(Long quizId, QuizQuestionRequest request) {
        Quiz quiz = findQuizOrThrow(quizId);
        log.info("➕ إضافة سؤال يدوي للكويز {}", quizId);

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.questionText())
                .difficultyWeight(request.difficultyWeight())
                .explanation(request.explanation())
                .choices(new ArrayList<>())
                .build();

        int maxOrder = quiz.getQuestions().stream()
                .mapToInt(QuizQuestion::getOrderIndex)
                .max()
                .orElse(0);
        question.setOrderIndex(maxOrder + 1);

        List<QuizChoice> choices = request.choices().stream()
                .map(c -> QuizChoice.builder()
                        .question(question)
                        .choiceText(c.choiceText())
                        .isCorrect(c.isCorrect())
                        .build())
                .toList();
        validateChoices(choices);
        question.setChoices(choices);

        if (quiz.getQuestions() == null) {
            quiz.setQuestions(new ArrayList<>());
        }
        quiz.getQuestions().add(question);
        Quiz saved = quizRepo.save(quiz);
        log.info("✅ تمت إضافة السؤال للكويز {}, الآن {} سؤال", quizId, saved.getQuestions().size());
        return toQuizResponse(saved);
    }

    @Transactional
    public QuizResponse updateQuestion(Long quizId, Long questionId, QuizQuestionRequest request) {
        Quiz quiz = findQuizOrThrow(quizId);
        log.info("✏️ تعديل السؤال {} في الكويز {}", questionId, quizId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود"));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new IllegalStateException("السؤال لا ينتمي لهذا الكويز");
        }

        question.setQuestionText(request.questionText());
        question.setDifficultyWeight(request.difficultyWeight());
        question.setExplanation(request.explanation());

        if (request.choices() != null && !request.choices().isEmpty()) {
            List<QuizChoice> newChoices = request.choices().stream()
                    .map(c -> QuizChoice.builder()
                            .question(question)
                            .choiceText(c.choiceText())
                            .isCorrect(c.isCorrect())
                            .build())
                    .toList();
            validateChoices(newChoices);
            question.getChoices().clear();
            question.getChoices().addAll(newChoices);
        }

        questionRepo.save(question);
        log.info("✅ تم تعديل السؤال {} في الكويز {}", questionId, quizId);
        return toQuizResponse(quizRepo.findById(quizId).orElseThrow());
    }

    @Transactional
    public QuizResponse deleteQuestion(Long quizId, Long questionId) {
        Quiz quiz = findQuizOrThrow(quizId);
        log.info("🗑️ حذف السؤال {} من الكويز {}", questionId, quizId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود"));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new IllegalStateException("السؤال لا ينتمي لهذا الكويز");
        }

        quiz.getQuestions().remove(question);
        Quiz saved = quizRepo.save(quiz);
        log.info("✅ تم حذف السؤال {} من الكويز {}", questionId, quizId);
        return toQuizResponse(saved);
    }

    @Transactional
    public QuizResponse publishQuiz(Long quizId) {
        Quiz quiz = findQuizOrThrow(quizId);
        log.info("📢 نشر الكويز {}", quizId);

        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalStateException("لا يمكن نشر كويز بدون أسئلة");
        }

        quiz.setStatus("PUBLISHED");
        Quiz saved = quizRepo.save(quiz);
        log.info("✅ تم نشر الكويز '{}' (ID={})", saved.getTitle(), saved.getId());
        return toQuizResponse(saved);
    }

    // ──────────────────────────────────────────────
    //  Student Attempt Operations
    // ──────────────────────────────────────────────

    @Transactional
    public QuizAttempt startAttempt(Long quizId, Long studentId) {
        Quiz quiz = findQuizOrThrow(quizId);
        log.info("🎯 الطالب {} يبدأ محاولة للكويز {}", studentId, quizId);

        if (!"PUBLISHED".equals(quiz.getStatus())) {
            throw new IllegalStateException("الكويز غير منشور بعد");
        }

        Integer maxAttempt = attemptRepo.findMaxAttemptNumber(quizId, studentId).orElse(0);
        int nextAttempt = maxAttempt + 1;

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(userRepo.findById(studentId)
                        .orElseThrow(() -> new EntityNotFoundException("الطالب غير موجود")))
                .attemptNumber(nextAttempt)
                .startedAt(LocalDateTime.now())
                .build();

        QuizAttempt saved = attemptRepo.save(attempt);
        log.info("✅ تم إنشاء المحاولة رقم {} للطالب {} للكويز {}", nextAttempt, studentId, quizId);
        return saved;
    }

    @Transactional
    public QuizAttemptResponse submitAttempt(Long attemptId, QuizSubmitRequest request) {
        QuizAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("المحاولة غير موجودة"));
        log.info("📥 تسليم المحاولة {} بعدد إجابات {}", attemptId, request.answers().size());

        if (attempt.getSubmittedAt() != null) {
            throw new IllegalStateException("المحاولة تم تسليمها مسبقًا");
        }

        Quiz quiz = attempt.getQuiz();
        List<QuizAnswer> answerEntities = new ArrayList<>();
        int totalScore = 0;

        for (QuizSubmitRequest.AnswerEntry entry : request.answers()) {
            QuizQuestion question = questionRepo.findById(entry.questionId())
                    .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود: " + entry.questionId()));

            QuizChoice selectedChoice = choiceRepo.findById(entry.selectedChoiceId())
                    .orElseThrow(() -> new EntityNotFoundException("الاختيار غير موجود: " + entry.selectedChoiceId()));

            boolean isCorrect = Boolean.TRUE.equals(selectedChoice.getIsCorrect());
            int pointsAwarded = isCorrect ? question.getDifficultyWeight() : 0;

            QuizAnswer answer = QuizAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedChoice(selectedChoice)
                    .isCorrect(isCorrect)
                    .pointsAwarded(pointsAwarded)
                    .build();

            answerEntities.add(answer);
            totalScore += pointsAwarded;
        }

        answerRepo.saveAll(answerEntities);
        attempt.setAnswers(answerEntities);
        attempt.setScore(totalScore);
        attempt.setSubmittedAt(LocalDateTime.now());
        QuizAttempt savedAttempt = attemptRepo.save(attempt);

        int maxScore = calculateMaxScore(quiz);
        log.info("✅ تم تسليم المحاولة {}: النتيجة {}/{}", attemptId, totalScore, maxScore);

        return toAttemptResponse(savedAttempt, quiz.getShowAnswersAfterSubmit());
    }

    public List<QuizAttempt> getAttempts(Long quizId, Long studentId) {
        log.debug("جلب محاولات الطالب {} للكويز {}", studentId, quizId);
        return attemptRepo.findByQuizIdAndStudentIdOrderByAttemptNumberDesc(quizId, studentId);
    }

    // ──────────────────────────────────────────────
    //  Internal Helpers
    // ──────────────────────────────────────────────

    private Quiz findQuizOrThrow(Long quizId) {
        return quizRepo.findById(quizId)
                .orElseThrow(() -> {
                    log.warn("⚠️ كويز غير موجود: ID={}", quizId);
                    return new EntityNotFoundException("الكويز غير موجود: " + quizId);
                });
    }

    private void validateChoices(List<QuizChoice> choices) {
        if (choices.size() != 4) {
            throw new IllegalArgumentException("كل سؤال يجب أن يحتوي على 4 اختيارات بالضبط");
        }
        long correctCount = choices.stream().filter(c -> Boolean.TRUE.equals(c.getIsCorrect())).count();
        if (correctCount != 1) {
            throw new IllegalArgumentException("كل سؤال يجب أن يحتوي على إجابة صحيحة واحدة بالضبط");
        }
    }

    private int calculateMaxScore(Quiz quiz) {
        if (quiz.getQuestions() == null) return 0;
        return quiz.getQuestions().stream()
                .mapToInt(q -> q.getDifficultyWeight() != null ? q.getDifficultyWeight() : 0)
                .sum();
    }

    // ──────────────────────────────────────────────
    //  DTO Mapping
    // ──────────────────────────────────────────────

    private QuizResponse toQuizResponse(Quiz quiz) {
        List<QuizResponse.QuestionResponse> questionResponses = quiz.getQuestions() == null ? List.of()
                : quiz.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .map(q -> {
                    List<QuizResponse.ChoiceResponse> choiceResponses = q.getChoices() == null ? List.of()
                            : q.getChoices().stream()
                            .map(c -> new QuizResponse.ChoiceResponse(c.getId(), c.getChoiceText(), c.getIsCorrect()))
                            .toList();
                    return new QuizResponse.QuestionResponse(
                            q.getId(), q.getQuestionText(), q.getDifficultyWeight(),
                            q.getOrderIndex(), q.getExplanation(), choiceResponses);
                })
                .toList();

        return new QuizResponse(
                quiz.getId(), quiz.getLesson().getId(), quiz.getTitle(), quiz.getStatus(),
                quiz.getCreatedByTeacher().getId(), quiz.getCreatedAt(),
                quiz.getShowAnswersAfterSubmit(), questionResponses);
    }

    private QuizStudentResponse toStudentView(Quiz quiz) {
        List<QuizStudentResponse.StudentQuestionResponse> questionResponses = quiz.getQuestions() == null ? List.of()
                : quiz.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .map(q -> {
                    List<QuizStudentResponse.StudentChoiceResponse> choiceResponses = q.getChoices() == null ? List.of()
                            : q.getChoices().stream()
                            .map(c -> new QuizStudentResponse.StudentChoiceResponse(c.getId(), c.getChoiceText()))
                            .toList();
                    return new QuizStudentResponse.StudentQuestionResponse(
                            q.getId(), q.getQuestionText(), q.getDifficultyWeight(),
                            q.getOrderIndex(), choiceResponses);
                })
                .toList();

        return new QuizStudentResponse(
                quiz.getId(), quiz.getLesson().getId(), quiz.getTitle(), quiz.getStatus(),
                quiz.getCreatedAt(), quiz.getShowAnswersAfterSubmit(), questionResponses);
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, boolean showAnswersAfterSubmit) {
        List<QuizAttemptResponse.AnswerResponse> answerResponses = attempt.getAnswers() == null ? List.of()
                : attempt.getAnswers().stream()
                .map(a -> {
                    QuizQuestion q = a.getQuestion();
                    QuizChoice correctChoice = q.getChoices().stream()
                            .filter(c -> Boolean.TRUE.equals(c.getIsCorrect()))
                            .findFirst()
                            .orElse(null);

                    String explanation = showAnswersAfterSubmit ? q.getExplanation() : null;
                    Long correctChoiceId = showAnswersAfterSubmit && correctChoice != null ? correctChoice.getId() : null;
                    String correctChoiceText = showAnswersAfterSubmit && correctChoice != null ? correctChoice.getChoiceText() : null;

                    return new QuizAttemptResponse.AnswerResponse(
                            a.getId(), q.getId(), a.getSelectedChoice().getId(),
                            a.getIsCorrect(), a.getPointsAwarded(),
                            explanation, correctChoiceId, correctChoiceText);
                })
                .toList();

        return new QuizAttemptResponse(
                attempt.getId(), attempt.getQuiz().getId(), attempt.getStudent().getId(),
                attempt.getAttemptNumber(), attempt.getStartedAt(),
                attempt.getSubmittedAt(), attempt.getScore(), answerResponses);
    }
}
