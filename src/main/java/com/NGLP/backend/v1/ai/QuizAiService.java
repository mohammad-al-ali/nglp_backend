package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.entity.LessonTranscript;
import com.NGLP.backend.v1.service.LessonTranscriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QuizAiService {

    private final LlmRouterService routerService;
    private final LessonTranscriptService transcriptService;

    public QuizAiService(LlmRouterService routerService, LessonTranscriptService transcriptService) {
        this.routerService = routerService;
        this.transcriptService = transcriptService;
    }

    public record AiQuizResponse(List<AiQuestion> questions) {
        public record AiQuestion(
            String questionText,
            Integer difficultyWeight,
            Integer orderIndex,
            String explanation,
            List<AiChoice> choices
        ) {}
        public record AiChoice(String choiceText, boolean isCorrect) {}
    }

    public AiQuizResponse generateQuizQuestions(Long userId, Long lessonId, Integer numberOfQuestions) {
        List<LessonTranscript> transcripts = transcriptService.findByLesson(lessonId);
        String transcriptText = buildTranscriptText(transcripts);

        String systemPrompt = """
            You are a quiz-generation assistant for the NGLP educational platform.
            You write multiple-choice questions grounded SOLELY in the provided lesson transcript.

            OUTPUT
            - Exactly %d questions. Each has exactly 4 choices and exactly ONE correct choice.
            - Return ONLY the JSON object: { "questions": [ ... ] }. No prose, no Markdown, no commentary.

            GROUNDING
            - Every question and its correct answer must be verifiable from the transcript below.
            - Do not use outside knowledge or introduce facts, terms, or examples the teacher did not mention.
            - If the transcript is empty or too thin to yield %d sound questions, return { "questions": [] }.

            QUALITY
            - Test understanding of the ideas the teacher explained, not verbatim recall of a sentence.
            - All four choices must be plausible, mutually exclusive, and similar in length and style.
              Distractors should reflect realistic misconceptions — no filler, no "all of the above", no joke options.
            - "explanation": one or two sentences on WHY the correct choice is right, tied to the teacher's point.
            - "difficultyWeight" (1-10): spread across the set — a few easy (1-3), several medium (4-7), one or two hard (8-10).
            - "orderIndex": sequential from 1, following the lesson's own flow.

            LANGUAGE
            - All question text and choices in clear, precise Modern Standard Arabic.
            - Keep technical terms, identifiers, language and framework names in English.
            """.formatted(numberOfQuestions, numberOfQuestions);

        String userPrompt = """
            Here is the lesson transcript content:

            %s

            Generate %d multiple-choice questions based on this transcript, following the system rules exactly.
            """.formatted(transcriptText, numberOfQuestions);

        LlmProvider provider = routerService.resolveProvider(userId);
        ChatClient chatClient = provider.createChatClientBuilder().build();

        try {
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt);

            // بعض المزوّدين (Groq/gpt-oss) يحتاجون فرض صيغة JSON صراحةً وإلا قد يجيبون بنص عادي.
            var structuredOptions = provider.structuredOutputOptions();
            if (structuredOptions != null) {
                requestSpec = requestSpec.options(structuredOptions);
            }

            AiQuizResponse response = requestSpec
                .call()
                .entity(AiQuizResponse.class);

            if (response == null || response.questions() == null || response.questions().isEmpty()) {
                throw new RuntimeException("فشل توليد الأسئلة: الرد فارغ");
            }

            log.info("تم توليد {} سؤالًا بنجاح للدرس {}", response.questions().size(), lessonId);
            return response;

        } catch (Exception e) {
            log.error("فشل توليد الأسئلة بواسطة الذكاء الاصطناعي للدرس {}: {}", lessonId, e.getMessage());
            throw new RuntimeException("فشل توليد الأسئلة: " + e.getMessage(), e);
        }
    }

    private String buildTranscriptText(List<LessonTranscript> transcripts) {
        if (transcripts == null || transcripts.isEmpty()) {
            return "(لا توجد نصوص متاحة لهذا الدرس)";
        }
        StringBuilder sb = new StringBuilder();
        for (LessonTranscript t : transcripts) {
            sb.append("[")
              .append(formatTime(t.getStartSecond()))
              .append(" - ")
              .append(formatTime(t.getEndSecond()))
              .append("] ")
              .append(t.getTranscriptContent())
              .append("\n");
        }
        return sb.toString();
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
