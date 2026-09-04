package com.NGLP.backend.v1.entity;

/**
 * مصدر مقطع التفريغ — لأغراض التتبّع وإعادة التوليد.
 * <ul>
 *   <li>{@code WHISPER} — تفريغ Whisper الأصلي بلغة الفيديو المكتشفة.</li>
 *   <li>{@code MACHINE} — ترجمة آلية مجانية أوفلاين (argos-translate) لإكمال اللغة المفقودة.</li>
 *   <li>{@code LLM} — ترجمة عبر نموذج لغوي (غير مستخدَمة حالياً).</li>
 *   <li>{@code MANUAL} — أُدخل يدوياً / عبر البذر.</li>
 *   <li>{@code GOOGLE} — ترجمة عبر Google Translate (بيانات قديمة من نسخة سابقة من خط الأنابيب،
 *       سبقت الانتقال إلى argos-translate). أُبقيت هنا فقط لتفادي فشل تحليل الصفوف الموجودة
 *       أصلاً في قاعدة البيانات؛ لا تُستخدَم في التوليد الجديد.</li>
 * </ul>
 */
public enum TranscriptSource {
    WHISPER,
    MACHINE,
    LLM,
    MANUAL,
    GOOGLE
}
