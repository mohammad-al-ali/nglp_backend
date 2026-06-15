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
        String systemPrompt = """
    You are a highly intelligent, professional, and friendly AI Tutor for the NGLP educational platform.
    Your absolute primary mission is to help students understand their current programming lesson.
    
    You have access to the student's conversation history (Memory) and optionally a [Teacher's Transcript Context] which shows EXACTLY what the teacher is explaining at the exact video timestamp.
    
    CORE RULES & BEHAVIORS:
    1. CONTEXT IS KING: If the student asks a vague question (e.g., "Give me an example", "Explain this", "Why?"), you MUST rely directly on the [Teacher's Transcript Context] and the recent chat history to figure out what they are referring to. Do not provide a generic answer.
    2. BE CONCISE & DIRECT: Your answers must be extremely short, focused, and straight to the point. Maximum 2 short paragraphs or a brief bulleted list (100-150 words). NO fluff.
    3. BEAUTIFUL FORMATTING: Always use Markdown formatting. Highlight key terms in **bold**, and format ALL programming concepts, variable names, or code snippets inside proper `code blocks`.
    4. LANGUAGE POLICY: Your entire conversational response must be in pure, standard, friendly ARABIC. Do NOT use any other languages for the explanation. However, ALL technical programming terms (e.g., React, Java, Loop, Spring Boot) MUST remain in English.
    5. STAY ON TOPIC: If the student asks something completely unrelated to programming or the platform, politely decline to answer and guide them back to the lesson.
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

        // 1. استباق جلب سياق تفريغ الفيديو وتنسيقه بوضوح
        String transcriptContext = "";
        try {
            AiToolsConfig.TranscriptResponse response = aiToolsConfig.fetchLessonTranscript(
                    String.valueOf(lessonId),
                    timestamp
            );
            if (response.found()) {
                // تنسيق السياق بشكل معزول لكي لا يختلط بسؤال الطالب
                transcriptContext = String.format(
                        """
                        ---
                        [TEACHER'S TRANSCRIPT AT TIMESTAMP %s]:
                        "%s"
                        ---
                        """,
                        timestamp, response.context()
                );
                log.info("🎯 Proactively injected streaming transcript context for Lesson: {}, Timestamp: {}", lessonId, timestamp);
            } else {
                // في حال لم نجد سياق، نعطي إشارة للنموذج
                transcriptContext = "[No transcript available for this exact timestamp. Rely on chat history.]\n";
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch transcript proactively in askStream(): ", e);
        }

        // 2. صياغة الموجه المدمج للـ LLM بهيكلية واضحة (Tags-like structure)
        String enrichedPrompt = String.format(
                """
                %s
                [SYSTEM INFO]: lessonId = %s, current_timestamp = %s
                
                [STUDENT'S MESSAGE]:
                %s
                """,
                transcriptContext, lessonId, timestamp, message
        );

        // 3. إرجاع الرد كتدفق لحظي
        return this.chatClient.prompt()
                .user(enrichedPrompt)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId)
                )
                .stream()
                .content();
    }}