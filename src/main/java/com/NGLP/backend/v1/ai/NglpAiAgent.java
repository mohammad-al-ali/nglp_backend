package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.service.ConversationService;
import com.NGLP.backend.v1.entity.Conversation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * وكيل الذكاء الاصطناعي الأساسي (AI Agent).
 * يدير الاتصال بنموذج الـ LLM، ويهيئ التوجيهات الأساسية (System Prompt)، ويربط أدوات التحليل والذاكرة المستمرة.
 */
@Service
@Slf4j
public class NglpAiAgent {

    private final ConversationService conversationService;
    private final ChatClient chatClient;
    private final AiToolsConfig aiToolsConfig; // كلاس الأدوات المساعد لاستدعاء بيانات قاعدة البيانات برمجياً

    public NglpAiAgent(ConversationService conversationService,
                       ChatClient.Builder builder,
                       ChatMemory chatMemory,
                       AiToolsConfig aiToolsConfig) {
        this.conversationService = conversationService;
        this.aiToolsConfig = aiToolsConfig;

        // 🌟 موجه النظام الأكاديمي المحدث والاحترافي
        // 🌟 الموجه النظامي المثالي والمتكامل لـ NGLP AI Tutor
        String systemPrompt = """
    You are an elite, highly intelligent, and pedagogical AI Tutor for the NGLP educational platform.
    Your ultimate goal is to facilitate student understanding, clear confusion, and provide accurate, context-aware technical explanations.
    
    ### 1. CONTEXT & AWARENESS 🧠
    You process three layers of context to form your answers:
    - HIGHEST PRIORITY: The [TEACHER'S TRANSCRIPT] provided with the user prompt. This is the exact current context of the lesson.
    - SECOND PRIORITY: The [CHAT MEMORY] to understand follow-up questions (e.g., "Give me an example of THAT").
    - THIRD PRIORITY: Your general programming knowledge to fill in gaps, ONLY IF it aligns with the lesson's topic.
    
    ### 2. PEDAGOGICAL RULES (HOW TO TEACH) 🎓
    - Be a guide, not a solution dispenser. Explain the 'Why' and 'How' clearly.
    - If a question is vague ("explain", "example"), instantly anchor your answer to the provided [TEACHER'S TRANSCRIPT].
    - NEVER invent concepts that contradict the teacher's explanation.
    - Keep answers dangerously concise and scannable. Limit text to 2 short paragraphs or a 3-point list.
    
    ### 3. GUARDRAILS & BOUNDARIES 🛡️
    - OFF-TOPIC: If the student asks about topics completely unrelated to programming, technology, or the platform (e.g., politics, movies, cooking), politely decline and steer them back to the lesson.
    - CHEATING: If the student asks you to solve an entire assignment or write a complete project from scratch, provide a structural guide and a small snippet, but encourage them to write the rest.
    
    ### 4. LANGUAGE & FORMATTING 📝
    - PROSE: All explanations and conversational text MUST be in standard, professional, and friendly Arabic.
    - TECHNICAL TERMS: ALL programming languages, frameworks, variables, and technical concepts (e.g., React, Object-Oriented, Loop, Spring Boot) MUST remain in English to preserve technical accuracy.
    - FORMATTING: Use Markdown aggressively. Wrap code snippets in proper `code blocks` with the language specified. Bold **key terms**.
    """;
        // تهيئة الـ ChatClient مع ربط الذاكرة بشكل مستقر لحفظ سجل الدردشة
        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * استقبال السؤال من الطالب، وإرجاع رد كامل دفعة واحدة.
     * يستخلص سياق الدرس استباقياً لحقنه في السؤال لضمان أفضل إجابة.
     */
    public String ask(Long userId, Long lessonId, String timestamp, String message) {
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        String activeConversationId = String.valueOf(conversation.getId());

        // 1. استباق جلب سياق تفريغ الفيديو عند الطابع الزمني الحالي (RAG)
        String transcriptContext = "";
        try {
            AiToolsConfig.TranscriptResponse response = aiToolsConfig.fetchLessonTranscript(
                    String.valueOf(lessonId),
                    timestamp
            );
            if (response.found()) {
                transcriptContext = String.format(
                        "\n[Video Transcript Context from Teacher's explanation at timestamp %s]:\n\"%s\"\n",
                        timestamp, response.context()
                );
                log.info("🎯 Proactively injected transcript context for Lesson: {}, Timestamp: {}", lessonId, timestamp);
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch transcript proactively in ask(): ", e);
        }

        // 2. صياغة الموجه المدمج لإرساله للـ LLM
        String enrichedPrompt = String.format(
                "Student Question: %s\n%s[System Info: lessonId=%s, timestamp=%s]",
                message, transcriptContext, String.valueOf(lessonId), timestamp
        );

        // 3. الحصول على الرد النهائي النظيف بالكامل
        return this.chatClient.prompt()
                .user(enrichedPrompt)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId)
                )
                .call()
                .content();
    }

    /**
     * استقبال السؤال من الطالب، وإرسال الرد المتدفق لحظياً (SSE Streaming).
     * متوافق بالكامل مع البث اللحظي بفضل تفعيل الجلب الاستباقي للسياق.
     */
    public Flux<String> askStream(Long userId, Long lessonId, String timestamp, String message) {
        Conversation conversation = conversationService.getOrCreateConversation(userId, lessonId);
        String activeConversationId = String.valueOf(conversation.getId());

        // 1. جلب السياق وتغليفه بشكل محمي
        String transcriptContext = "";
        try {
            AiToolsConfig.TranscriptResponse response = aiToolsConfig.fetchLessonTranscript(
                    String.valueOf(lessonId),
                    timestamp
            );
            if (response.found() && !response.context().trim().isEmpty()) {
                transcriptContext = String.format(
                        """
                        
                        <TRANSCRIPT_CONTEXT>
                        Timestamp: %s
                        Content: "%s"
                        </TRANSCRIPT_CONTEXT>
                        """,
                        timestamp, response.context()
                );
                log.info("🎯 Proactively injected streaming transcript context for Lesson: {}, Timestamp: {}", lessonId, timestamp);
            } else {
                transcriptContext = "\n<TRANSCRIPT_CONTEXT>No specific transcript available for this exact moment. Rely on active lesson context and chat memory.</TRANSCRIPT_CONTEXT>\n";
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch transcript proactively in askStream(): ", e);
        }

        // 2. تجميع الموجه النهائي بهيكلية صارمة
        String enrichedPrompt = String.format(
                """
                <SYSTEM_METADATA>
                Lesson ID: %s
                Current Video Timestamp: %s
                </SYSTEM_METADATA>
                %s
                <STUDENT_QUESTION>
                %s
                </STUDENT_QUESTION>
                """,
                lessonId, timestamp, transcriptContext, message
        );

        // 3. التنفيذ
        return this.chatClient.prompt()
                .user(enrichedPrompt)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId)
                )
                .stream()
                .content();
    }

}