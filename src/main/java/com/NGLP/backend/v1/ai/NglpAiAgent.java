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

        // 🌟 موجه النظام الأكاديمي القياسي باللغة الإنجليزية لضبط تصرفات النموذج
        String systemPrompt = """
            You are a smart, professional, and friendly AI Tutor on the NGLP educational platform.
            Your primary mission is to help students by answering their questions clearly, based on the course materials and lesson content.
            
            You will be provided with the student's question and optionally a [Video Transcript Context] extracted from the teacher's actual spoken words in the lesson video at the student's current timestamp.
            
            ANSWERING RULES:
            1. Always structure your answers beautifully and format key programming concepts in clean code blocks or bullet points.
            2. Keep your answers extremely CONCISE, DIRECT, and SHORT. Do not exceed 2 short paragraphs or a simple bulleted list (maximum 100-120 words).
            3. Do not mix languages or use non-Arabic words (no Vietnamese, Chinese, etc.). All explanations must be in pure, standard Arabic.
            4. Technical terms (like React, React Native) can be written as they are in English.
            5. ALWAYS respond to the student in clear, friendly, and professional ARABIC language, directly helping them understand the lesson.
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
                log.info("🎯 Proactively injected streaming transcript context for Lesson: {}, Timestamp: {}", lessonId, timestamp);
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch transcript proactively in askStream(): ", e);
        }

        // 2. صياغة الموجه المدمج للـ LLM
        String enrichedPrompt = String.format(
                "Student Question: %s\n%s[System Info: lessonId=%s, timestamp=%s]",
                message, transcriptContext, String.valueOf(lessonId), timestamp
        );

        // 3. إرجاع الرد كتدفق حقيقي لحظي متوافق مع الفرونت إند
        return this.chatClient.prompt()
                .user(enrichedPrompt)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, activeConversationId)
                )
                .stream()
                .content();
    }
}