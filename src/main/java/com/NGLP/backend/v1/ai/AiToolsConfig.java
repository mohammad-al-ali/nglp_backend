package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.service.LessonTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * كلاس الإعدادات الخاص بأدوات الذكاء الاصطناعي (Function Calling).
 * يحتوي على الدوال التي يمكن للنموذج اللغوي (LLM) استدعاؤها برمجياً للحصول على بيانات إضافية.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiToolsConfig {

    private final LessonTranscriptService transcriptService;

    public record TranscriptResponse(String context, boolean found) {}

    @Tool(name = "fetchLessonTranscript", description = "Fetches the exact transcript spoken by the teacher in the video at a specific timestamp.")
    public TranscriptResponse fetchLessonTranscript(
            @ToolParam(description = "The unique ID of the lesson as a string.") String lessonId,
            @ToolParam(description = "The video timestamp formatted as a String (e.g., '100').") String timestamp
    ) {

        log.info(" تم تشغيل أداة الوكيل 'fetchLessonTranscript' بواسطة الذكاء الاصطناعي باستخدام معلمات نصية -> معرف الدرس: {}، الطابع الزمني: {}",
                lessonId, timestamp);

        try {
            // 🌟 تحويل الـ lessonId بأمان من String إلى Long في كود الجافا الداخلي
            Long parsedLessonId = Long.parseLong(lessonId.trim());

            // استدعاء الخدمة (التي تتعامل داخلياً مع الـ timestamp بشكل مرن كما كتبناها سابقاً)
            String context = transcriptService.findContextByTime(parsedLessonId, timestamp);

            if (StringUtils.hasText(context)) {
                log.info("✅ تم العثور على السياق بنجاح من قاعدة البيانات. جارٍ العودة إلى LLM.");
                return new TranscriptResponse(context, true);
            }
            log.info("❌لم يتم العثور على نص مكتوب في هذا الوقت. الاعتماد على معلومات العامة.");
            return new TranscriptResponse("No transcript found at this timestamp. Please rely on your general knowledge.", false);

        } catch (NumberFormatException nfe) {
            log.error("❌ فشل تحليل معرف الدرس من LLM: {}", lessonId);
            return new TranscriptResponse("Invalid lesson ID format provided.", false);
        } catch (Exception e) {
            log.error("❌ حدث خطأ أثناء تنفيذ منطق الأداة", e);
            return new TranscriptResponse("Error retrieving transcript from database.", false);
        }
    }
}