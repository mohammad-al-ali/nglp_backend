package com.NGLP.backend.v1;

import com.NGLP.backend.v1.ai.ChatMessageSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * يتحقق أن تنظيف نص الرسائل يزيل كل السياق التقني المحقون ويُبقي كلام الطالب الصافي،
 * لكل الصيغ التي مرّ بها الـ prompt (البث، الرد الكامل، والرسائل القديمة الملوّثة).
 */
class ChatMessageSanitizerTest {

    @Test
    void extractsQuestionFromStreamingPrompt() {
        String raw = """
                <SYSTEM_METADATA>
                Lesson ID: 5
                Current Video Timestamp: 120
                </SYSTEM_METADATA>
                <TRANSCRIPT_CONTEXT>
                Timestamp: 120
                Content: "the teacher explains loops"
                </TRANSCRIPT_CONTEXT>
                <STUDENT_QUESTION>
                ما الفرق بين for و while؟
                </STUDENT_QUESTION>""";

        assertEquals("ما الفرق بين for و while؟", ChatMessageSanitizer.sanitize(raw));
    }

    @Test
    void stripsLegacyNonStreamingPrompt() {
        String raw = "Student Question: اشرح لي المتغيرات\n"
                + "\n[Video Transcript Context from Teacher's explanation at timestamp 30]:\n"
                + "\"variables store values\"\n"
                + "[System Info: lessonId=3, timestamp=30]";

        assertEquals("اشرح لي المتغيرات", ChatMessageSanitizer.sanitize(raw));
    }

    @Test
    void stripsLeftoverInternalTags() {
        String raw = "نص عادي <SYSTEM_METADATA>lessonId=1</SYSTEM_METADATA> يتبعه سؤال";
        String result = ChatMessageSanitizer.sanitize(raw);
        assertFalse(result.contains("<"));
        assertFalse(result.contains("SYSTEM_METADATA"));
        assertTrue(result.startsWith("نص عادي"));
        assertTrue(result.endsWith("يتبعه سؤال"));
    }

    @Test
    void unwrapsSingleFieldJsonAssistantMessage() {
        assertEquals("مرحباً بك", ChatMessageSanitizer.sanitize("{\"response\":\"مرحباً بك\"}"));
        assertEquals("الإجابة هنا", ChatMessageSanitizer.sanitize("  {\"answer\": \"الإجابة هنا\"}  "));
    }

    @Test
    void leavesCleanTextUntouched() {
        String clean = "كيف أستخدم **React** hooks؟";
        assertEquals(clean, ChatMessageSanitizer.sanitize(clean));
    }

    @Test
    void leavesRealJsonCodeBlockUntouched() {
        String codeAnswer = "استخدم هذا:\n```json\n{\"key\": \"value\"}\n```";
        assertEquals(codeAnswer, ChatMessageSanitizer.sanitize(codeAnswer));
    }

    @Test
    void handlesNullAndBlank() {
        assertEquals("", ChatMessageSanitizer.sanitize(null));
        assertEquals("", ChatMessageSanitizer.sanitize("   "));
    }
}
