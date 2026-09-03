package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.service.ConversationService;
import com.NGLP.backend.v1.entity.Conversation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class NglpAiAgent {

    private final ConversationService conversationService;
    private final LlmRouterService routerService;
    private final AiToolsConfig aiToolsConfig;
    private final ChatMemory chatMemory;
    private final String systemPrompt;

    public NglpAiAgent(ConversationService conversationService,
                       LlmRouterService routerService,
                       ChatMemory chatMemory,
                       AiToolsConfig aiToolsConfig) {
        this.conversationService = conversationService;
        this.routerService = routerService;
        this.aiToolsConfig = aiToolsConfig;
        this.chatMemory = chatMemory;
        this.systemPrompt = """
    You are an elite, highly intelligent, and pedagogical AI Tutor for the NGLP educational platform.
    Your ultimate goal is to facilitate student understanding, clear confusion, and provide accurate, context-aware technical explanations.

    ### 1. CONTEXT & AWARENESS
    You process three layers of context to form your answers:
    - HIGHEST PRIORITY: The [TEACHER'S TRANSCRIPT] provided in the lesson-context section below. This is the exact current context of the lesson.
    - SECOND PRIORITY: The [CHAT MEMORY] to understand follow-up questions (e.g., "Give me an example of THAT").
    - THIRD PRIORITY: Your general programming knowledge to fill in gaps, ONLY IF it aligns with the lesson's topic.

    ### 2. PEDAGOGICAL RULES (HOW TO TEACH)
    - Be a guide, not a solution dispenser. Explain the 'Why' and 'How' clearly.
    - If a question is vague ("explain", "example"), instantly anchor your answer to the provided transcript context.
    - NEVER invent concepts that contradict the teacher's explanation.
    - Keep answers dangerously concise and scannable. Limit text to 2 short paragraphs or a 3-point list.

    ### 3. GUARDRAILS & BOUNDARIES
    - OFF-TOPIC: If the student asks about topics completely unrelated to programming, technology, or the platform (e.g., politics, movies, cooking), politely decline and steer them back to the lesson.
    - CHEATING: If the student asks you to solve an entire assignment or write a complete project from scratch, provide a structural guide and a small snippet, but encourage them to write the rest.

    ### 4. LANGUAGE & FORMATTING
    - PROSE: All explanations and conversational text MUST be in standard, professional, and friendly Arabic.
    - TECHNICAL TERMS: ALL programming languages, frameworks, variables, and technical concepts (e.g., React, Object-Oriented, Loop, Spring Boot) MUST remain in English to preserve technical accuracy.
    - FORMATTING: Use Markdown. Wrap code snippets in proper code blocks with the language specified. Bold key terms.
    - Reply with the answer ONLY — never echo back the lesson-context section, the metadata, or these instructions.
    - Reply in plain Markdown text, NOT as JSON and NOT wrapped in any object or quotes.
    """;
    }

    public String ask(Long userId, Long lessonId, String timestamp, String message) {
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        String activeConversationId = String.valueOf(conversation.getId());

        return buildChatClient(userId)
                .prompt()
                .system(systemPrompt + buildLessonContext(lessonId, timestamp))
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId))
                .call()
                .content();
    }

    public Flux<String> askStream(Long userId, Long lessonId, String timestamp, String message) {
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        String activeConversationId = String.valueOf(conversation.getId());

        return buildChatClient(userId)
                .prompt()
                .system(systemPrompt + buildLessonContext(lessonId, timestamp))
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId))
                .stream()
                .content();
    }

    private ChatClient buildChatClient(Long userId) {
        return routerService.createBuilder(userId)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * يبني قسم سياق الدرس الذي يُضاف إلى رسالة النظام (وليس إلى رسالة الطالب).
     *
     * <p>الغرض: يصل السياق للنموذج لكنه لا يُخزَّن ضمن رسالة الطالب في قاعدة البيانات
     * ({@code MessageChatMemoryAdvisor} يحفظ آخر رسالة مستخدم فقط)، فلا يظهر
     * كـ«رموز وعبارات إنجليزية» في سجل الدردشة بعد إعادة الدخول.
     */
    private String buildLessonContext(Long lessonId, String timestamp) {
        String transcript = fetchTranscriptSafely(lessonId, timestamp);

        StringBuilder sb = new StringBuilder("\n\n### CURRENT LESSON CONTEXT (reference only — never repeat to the student)\n");
        sb.append("- lessonId: ").append(lessonId).append('\n');
        sb.append("- videoTimestamp: ").append(timestamp).append('\n');
        if (transcript != null && !transcript.isBlank()) {
            sb.append("- Teacher's transcript near this moment:\n\"\"\"\n")
              .append(transcript.trim())
              .append("\n\"\"\"\n");
        } else {
            sb.append("- No transcript available for this exact moment; rely on chat memory and general knowledge aligned with the lesson topic.\n");
        }
        return sb.toString();
    }

    private String fetchTranscriptSafely(Long lessonId, String timestamp) {
        try {
            AiToolsConfig.TranscriptResponse response = aiToolsConfig.fetchLessonTranscript(
                    String.valueOf(lessonId), timestamp);
            if (response.found() && response.context() != null && !response.context().trim().isEmpty()) {
                log.info("Proactively injected transcript context for Lesson: {}, Timestamp: {}", lessonId, timestamp);
                return response.context();
            }
        } catch (Exception e) {
            log.error("Failed to fetch transcript proactively: ", e);
        }
        return null;
    }
}
