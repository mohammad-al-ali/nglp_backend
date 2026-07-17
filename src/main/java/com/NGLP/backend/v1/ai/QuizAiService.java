package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.entity.LessonTranscript;
import com.NGLP.backend.v1.service.LessonTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final ChatClient.Builder chatClientBuilder;
    private final LessonTranscriptService transcriptService;

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

    public AiQuizResponse generateQuizQuestions(Long lessonId, Integer numberOfQuestions) {
        List<LessonTranscript> transcripts = transcriptService.findByLesson(lessonId);
        String transcriptText = buildTranscriptText(transcripts);

        String systemPrompt = """
            You are a quiz generation assistant for the NGLP educational platform.
            You generate multiple-choice questions based SOLELY on the provided lesson transcript.
            
            IMPORTANT RULES:
            - Generate EXACTLY %d multiple-choice questions.
            - Each question MUST have exactly 4 choices.
            - Each question MUST have exactly ONE correct choice.
            - Questions must be based ONLY on the transcript content provided below.
            - Do NOT use any external knowledge or invent facts not present in the transcript.
            - Distribute difficulty weights (1-10) appropriately across questions.
            - Write clear, unambiguous questions.
            - Provide a brief explanation for why the correct answer is correct.
            - Order questions logically following the flow of the lesson.
            - All question text and choices MUST be in Arabic.
            - Technical terms (programming languages, frameworks, concepts) may remain in English.
            """.formatted(numberOfQuestions);

        String userPrompt = """
            Here is the lesson transcript content:
            
            %s
            
            Generate %d multiple-choice questions based on this transcript.
            Each question must have exactly 4 choices with exactly one correct choice.
            Return the response as a JSON object with a "questions" array.
            """.formatted(transcriptText, numberOfQuestions);

        ChatClient chatClient = chatClientBuilder.build();

        try {
            AiQuizResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
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
