package com.NGLP.backend.v1.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * تنظيف نص رسائل المحادثة قبل تخزينها أو عرضها.
 *
 * <p>الوكيل الذكي يحقن في رسالة الطالب سياقاً تقنياً (تفريغ الفيديو، بيانات النظام،
 * وسوم XML داخلية) قبل إرسالها للنموذج اللغوي. يجب ألا يُخزَّن هذا السياق ولا يُعرض
 * للطالب — فهو يظهر كـ«رموز وعبارات إنجليزية» غريبة في سجل الدردشة بعد إعادة الدخول.
 *
 * <p>تُطبَّق هذه الأداة في موضعين:
 * <ul>
 *   <li><b>الكتابة</b> ({@link DatabaseChatMemory}) — حماية استباقية لأي صيغة سياق مستقبلية.</li>
 *   <li><b>القراءة</b> ({@code ConversationService#getFullChatHistory}) — إصلاح فوري
 *       للرسائل القديمة الملوّثة الموجودة أصلاً في قاعدة البيانات دون الحاجة لهجرة (migration).</li>
 * </ul>
 */
public final class ChatMessageSanitizer {

    private ChatMessageSanitizer() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** الصيغة الحديثة (البث اللحظي): السؤال الحقيقي محصور بين وسمَي <STUDENT_QUESTION>. */
    private static final Pattern STUDENT_QUESTION_TAG =
            Pattern.compile("<STUDENT_QUESTION>\\s*(.*?)\\s*</STUDENT_QUESTION>", Pattern.DOTALL);

    /** الصيغة القديمة (الرد الكامل): بادئة "Student Question: " ثم كتل سياق بين أقواس مربّعة. */
    private static final Pattern LEGACY_PREFIX = Pattern.compile("^\\s*Student Question:\\s*");
    private static final Pattern LEGACY_CONTEXT_TAIL =
            Pattern.compile("\\n\\s*\\[(?:Video Transcript Context|System Info)\\b.*$", Pattern.DOTALL);

    /** أي كتل أو وسوم XML داخلية متبقّية من صياغة الـ prompt. */
    private static final Pattern INTERNAL_XML_BLOCK = Pattern.compile(
            "(?s)<(SYSTEM_METADATA|TRANSCRIPT_CONTEXT|SYSTEM_INFO)>.*?</\\1>\\s*");
    private static final Pattern INTERNAL_XML_TAGS = Pattern.compile(
            "</?(SYSTEM_METADATA|TRANSCRIPT_CONTEXT|SYSTEM_INFO|STUDENT_QUESTION)>");

    /**
     * ينظّف نص رسالة (طالب أو مساعد) ويعيد المحتوى الصافي فقط.
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }

        String text = raw;

        // 1) الصيغة الحديثة: خذ ما بين وسمَي سؤال الطالب فقط إن وُجدا.
        Matcher tagged = STUDENT_QUESTION_TAG.matcher(text);
        if (tagged.find()) {
            text = tagged.group(1);
        } else {
            // 2) الصيغة القديمة: أزل البادئة ثم اقتطع كتل السياق اللاحقة.
            text = LEGACY_PREFIX.matcher(text).replaceFirst("");
            text = LEGACY_CONTEXT_TAIL.matcher(text).replaceAll("");
        }

        // 3) نظّف أي كتل/وسوم XML داخلية متبقّية في أي موضع.
        text = INTERNAL_XML_BLOCK.matcher(text).replaceAll("");
        text = INTERNAL_XML_TAGS.matcher(text).replaceAll("");

        // 4) رسائل مساعد قديمة خُزّنت كـ JSON خام (بسبب فرض response_format=json_object
        //    على مزوّد Groq سابقاً) — فُكّ الغلاف إن كان كائناً بسيطاً بحقل نصّي واحد.
        text = unwrapSingleFieldJson(text);

        return text.trim();
    }

    /**
     * إذا كان النص كائن JSON صرفاً يحوي حقلاً نصّياً واحداً، يعيد قيمة ذلك الحقل.
     * غير ذلك يعيد النص كما هو (شرح Markdown حقيقي لا يكون بهذا الشكل عملياً).
     */
    private static String unwrapSingleFieldJson(String text) {
        String trimmed = text.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{' || trimmed.charAt(trimmed.length() - 1) != '}') {
            return text;
        }
        try {
            JsonNode node = JSON.readTree(trimmed);
            if (node.isObject() && node.size() == 1) {
                for (Map.Entry<String, JsonNode> entry : node.properties()) {
                    JsonNode value = entry.getValue();
                    if (value.isString()) {
                        return value.asString();
                    }
                }
            }
        } catch (Exception ignored) {
            // ليس JSON صالحاً — اتركه كما هو.
        }
        return text;
    }
}
