package com.NGLP.backend.v1.service;

import com.NGLP.backend.v1.dto.LessonTranscriptResponse;
import com.NGLP.backend.v1.entity.Lesson;
import com.NGLP.backend.v1.entity.LessonTranscript;
import com.NGLP.backend.v1.entity.TranscriptLanguage;
import com.NGLP.backend.v1.entity.TranscriptSource;
import com.NGLP.backend.v1.exception.BusinessRuleException;
import com.NGLP.backend.v1.exception.ErrorCode;
import com.NGLP.backend.v1.repo.LessonRepo;
import com.NGLP.backend.v1.repo.LessonTranscriptRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonTranscriptService {

    private final LessonTranscriptRepo transcriptRepo;
    private final LessonRepo lessonRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_SERVICE_BASE = "http://127.0.0.1:8000";
    private static final String TRANSCRIBE_URL = AI_SERVICE_BASE + "/transcribe";
    private static final String TRANSLATE_URL = AI_SERVICE_BASE + "/translate";

    // =====================================================================
    // 1) الاستخراج من مايكروسيرفيس Whisper (يعمل بالخلفية عند إنشاء الدرس)
    //    - تفريغ أصلي بلغة الفيديو (Whisper)
    //    - ترجمة عربية آلية مجانية (Google عبر deep-translator) إن كان المصدر غير عربي
    // =====================================================================
    @Async
    public void extractAndSaveTranscript(Lesson lesson, String savedVideoAbsolutePath) {
        try {
            log.info("🎬 بدء معالجة الفيديو للدرس رقم: {}", lesson.getId());

            File videoFileToUpload = new File(savedVideoAbsolutePath);
            if (!videoFileToUpload.exists()) {
                throw new RuntimeException("ملف الفيديو غير موجود على السيرفر في المسار: " + savedVideoAbsolutePath);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(videoFileToUpload));
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("🚀 جاري إرسال الفيديو لخدمة التفريغ...");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(TRANSCRIBE_URL, requestEntity, String.class);

            log.info("✅ تم استلام الرد من خدمة التفريغ، جاري التحليل...");
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            if (rootNode.has("error")) {
                throw new RuntimeException("خطأ من خدمة التفريغ: " + rootNode.get("error").asString());
            }

            String detectedRaw = rootNode.path("language").asString();
            if (detectedRaw != null && detectedRaw.isBlank()) detectedRaw = null;
            TranscriptLanguage originalLang =
                    (detectedRaw != null && detectedRaw.toLowerCase().startsWith("ar"))
                            ? TranscriptLanguage.AR : TranscriptLanguage.EN;

            // تشغيل آمن للإعادة: نحذف أي مقاطع سابقة لهذا الدرس قبل إعادة الحفظ.
            List<LessonTranscript> previous = transcriptRepo.findByLessonIdOrderByStartSecondAsc(lesson.getId());
            if (!previous.isEmpty()) {
                transcriptRepo.deleteAll(previous);
            }

            List<LessonTranscript> toSave = new ArrayList<>(parseSegments(
                    rootNode.path("transcription"), lesson, originalLang, TranscriptSource.WHISPER, detectedRaw));

            // "translation" من الخدمة = ترجمة عربية جاهزة (تُنتَج فقط حين يكون المصدر غير عربي).
            JsonNode translationNode = rootNode.path("translation");
            if (translationNode.isArray() && !translationNode.isEmpty()) {
                toSave.addAll(parseSegments(translationNode, lesson, TranscriptLanguage.AR,
                        TranscriptSource.MACHINE, detectedRaw));
            }

            transcriptRepo.saveAll(toSave);
            log.info("🎉 تم حفظ {} مقطعاً نصياً للدرس رقم: {} (لغة المصدر: {})",
                    toSave.size(), lesson.getId(), originalLang.code());

            // مدة الفيديو: تصل الآن من خدمة التفريغ ضمن الحقل "duration". إن غابت، نستعمل
            // أكبر endSecond في المقاطع كتقدير أدنى قريب. نُحدّث الدرس فقط إن كانت المدة مجهولة.
            persistLessonDuration(lesson.getId(), rootNode.path("duration"), toSave);

            // احتياط: إن كان المصدر غير عربي ولم تصل ترجمة من الخدمة، جرّب إنشاءها الآن.
            if (originalLang != TranscriptLanguage.AR
                    && !transcriptRepo.existsByLessonIdAndLanguage(lesson.getId(), TranscriptLanguage.AR)) {
                try {
                    translateViaService(lesson, originalLang, TranscriptLanguage.AR);
                } catch (Exception e) {
                    log.warn("تعذّر توليد الترجمة العربية آلياً للدرس {}: {}", lesson.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ حدث خطأ أثناء محاولة استخراج النص: ", e);
        }
    }

    private List<LessonTranscript> parseSegments(JsonNode arrayNode, Lesson lesson, TranscriptLanguage lang,
                                                TranscriptSource source, String detectedRaw) {
        List<LessonTranscript> list = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) return list;
        int index = 0;
        for (JsonNode node : arrayNode) {
            String text = node.path("text").asString().trim();
            if (text.isEmpty()) continue;
            list.add(LessonTranscript.builder()
                    .lesson(lesson)
                    .startSecond(node.path("start").asInt())
                    .endSecond(node.path("end").asInt())
                    .transcriptContent(text)
                    .language(lang)
                    .segmentIndex(index++)
                    .source(source)
                    .detectedLanguage(detectedRaw)
                    .build());
        }
        return list;
    }

    /**
     * يحفظ مدة الفيديو في {@code lesson.durationSeconds} إن كانت مجهولة (null أو ≤ 0).
     * المصدر الأول: الحقل "duration" من خدمة التفريغ (Whisper يحسبها فعلياً).
     * الاحتياط: أكبر {@code endSecond} في المقاطع المُفرَّغة.
     * نُعيد جلب الدرس داخل خيط @Async لتفادي الكيان المنفصل / ترتيب المعاملات.
     */
    private void persistLessonDuration(Long lessonId, JsonNode durationNode, List<LessonTranscript> segments) {
        int seconds = 0;
        if (durationNode != null && durationNode.isNumber()) {
            seconds = (int) Math.round(durationNode.asDouble());
        }
        if (seconds <= 0) {
            for (LessonTranscript seg : segments) {
                if (seg.getEndSecond() != null && seg.getEndSecond() > seconds) {
                    seconds = seg.getEndSecond();
                }
            }
        }
        if (seconds <= 0) return;

        final int resolved = seconds;
        lessonRepo.findById(lessonId).ifPresent(fresh -> {
            Integer current = fresh.getDurationSeconds();
            if (current == null || current <= 0) {
                fresh.setDurationSeconds(resolved);
                lessonRepo.save(fresh);
                log.info("⏱️ تم ضبط مدة الدرس رقم {} على {} ثانية.", lessonId, resolved);
            }
        });
    }

    // =====================================================================
    // 2) الترجمة الآلية عبر خدمة الترجمة المجانية (بلا LLM / بلا توكِنز)
    // =====================================================================

    /**
     * يترجم مقاطع لغة المصدر إلى {@code target} عبر خدمة الترجمة، ويحفظها.
     * يرمي الاستثناءات للأعلى (خدمة معطّلة / رد غير صالح).
     *
     * @return عدد المقاطع المحفوظة.
     */
    private int translateViaService(Lesson lesson, TranscriptLanguage source, TranscriptLanguage target) {
        List<LessonTranscript> sourceRows =
                transcriptRepo.findByLessonIdAndLanguageOrderBySegmentIndexAsc(lesson.getId(), source);
        if (sourceRows.isEmpty()) {
            return 0;
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("source", source.code());
        requestBody.put("target", target.code());
        ArrayNode segs = requestBody.putArray("segments");
        for (LessonTranscript r : sourceRows) {
            ObjectNode s = segs.addObject();
            s.put("index", r.getSegmentIndex() != null ? r.getSegmentIndex() : 0);
            s.put("start", r.getStartSecond() != null ? r.getStartSecond() : 0);
            s.put("end", r.getEndSecond() != null ? r.getEndSecond() : 0);
            s.put("text", r.getTranscriptContent());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(TRANSLATE_URL, entity, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.has("error")) {
            throw new RuntimeException("خطأ من خدمة الترجمة: " + root.get("error").asString());
        }

        List<LessonTranscript> translated =
                parseSegments(root.path("segments"), lesson, target, TranscriptSource.MACHINE,
                        sourceRows.get(0).getDetectedLanguage());
        if (translated.isEmpty()) {
            return 0;
        }
        transcriptRepo.saveAll(translated);
        log.info("🌐 تمت ترجمة {} مقطعاً للدرس {} إلى {}", translated.size(), lesson.getId(), target.code());
        return translated.size();
    }

    /**
     * إعادة توليد ترجمة لغة معيّنة عبر خدمة الترجمة (يحذف الموجود لتلك اللغة أولاً).
     * للتشغيل اليدوي/التجربة. الأخطاء تُرمى للأعلى فيحوّلها {@code GlobalExceptionHandler}
     * إلى {@code ApiError} عربي.
     */
    public LessonTranscriptResponse regenerateTranslation(Long lessonId, TranscriptLanguage target) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الدرس رقم " + lessonId));

        // لغة المصدر = أي لغة متوفرة غير الهدف (نُفضّل الأصل من Whisper).
        TranscriptLanguage source = transcriptRepo.findDistinctLanguages(lessonId).stream()
                .filter(l -> l != target)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "لا يتوفر تفريغ مصدر لترجمته إلى هذه اللغة.",
                        HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE));

        List<LessonTranscript> existing =
                transcriptRepo.findByLessonIdAndLanguageOrderBySegmentIndexAsc(lessonId, target);
        if (!existing.isEmpty()) {
            transcriptRepo.deleteAll(existing);
        }

        int count;
        try {
            count = translateViaService(lesson, source, target);
        } catch (Exception e) {
            log.error("❌ فشل إعادة توليد ترجمة الدرس {} إلى {}: ", lessonId, target, e);
            throw new BusinessRuleException(
                    "تعذّر توليد الترجمة الآلية: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, ErrorCode.BUSINESS_RULE);
        }
        if (count == 0) {
            throw new BusinessRuleException(
                    "لا يتوفر تفريغ مصدر لترجمته إلى هذه اللغة.",
                    HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE);
        }
        return getTranscript(lessonId, target);
    }

    // =====================================================================
    // 3) القراءة (REST + وكيل الذكاء الاصطناعي)
    // =====================================================================

    /** استجابة تفريغ درس بلغة واحدة — يستخدمها endpoint الواجهة. */
    public LessonTranscriptResponse getTranscript(Long lessonId, TranscriptLanguage lang) {
        lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الدرس رقم " + lessonId));

        List<LessonTranscript> rows = transcriptRepo
                .findByLessonIdAndLanguageOrderBySegmentIndexAsc(lessonId, lang);

        List<String> availableLanguages = transcriptRepo.findDistinctLanguages(lessonId).stream()
                .sorted()                     // AR قبل EN
                .map(TranscriptLanguage::code)
                .toList();

        List<LessonTranscriptResponse.Segment> segments = new ArrayList<>();
        for (LessonTranscript r : rows) {
            segments.add(new LessonTranscriptResponse.Segment(
                    r.getSegmentIndex() != null ? r.getSegmentIndex() : 0,
                    r.getStartSecond() != null ? r.getStartSecond() : 0,
                    r.getEndSecond() != null ? r.getEndSecond() : 0,
                    r.getTranscriptContent()));
        }

        return new LessonTranscriptResponse(lessonId, lang.code(), !segments.isEmpty(),
                availableLanguages, segments);
    }

    /** مقاطع درس بالعربية افتراضياً (توافق خلفي مع مستدعي الذكاء الاصطناعي/الكويز). */
    public List<LessonTranscript> findByLesson(Long lessonId) {
        return findByLesson(lessonId, TranscriptLanguage.AR);
    }

    public List<LessonTranscript> findByLesson(Long lessonId, TranscriptLanguage lang) {
        return transcriptRepo.findByLessonIdAndLanguageOrderBySegmentIndexAsc(lessonId, lang);
    }

    /** سياق التفريغ عند لحظة زمنية — بالعربية افتراضياً (المساعد يعمل بالعربية). */
    public String findContextByTime(Long lessonId, Object timestampObj) {
        return findContextByTime(lessonId, TranscriptLanguage.AR, timestampObj);
    }

    public String findContextByTime(Long lessonId, TranscriptLanguage lang, Object timestampObj) {
        try {
            Integer timestampSeconds = parseTimestampToSeconds(timestampObj);
            if (timestampSeconds == null || timestampSeconds < 0) {
                log.warn("⚠️ تم إلغاء البحث لأن الوقت غير صالح: {}", timestampObj);
                return null;
            }
            log.info("🔍 جاري جلب النص للدرس {} ({}) عند الثانية: {}", lessonId, lang.code(), timestampSeconds);
            return transcriptRepo.findTranscriptAtTimestamp(lessonId, lang, timestampSeconds)
                    .map(LessonTranscript::getTranscriptContent)
                    .orElse(null);
        } catch (Exception e) {
            log.error("❌ خطأ غير متوقع أثناء البحث عن النص للوقت: {}", timestampObj, e);
            return null;
        }
    }

    private Integer parseTimestampToSeconds(Object timestampObj) {
        if (timestampObj == null) return null;
        String timeStr = String.valueOf(timestampObj).trim();
        if (!StringUtils.hasText(timeStr)) return null;
        if (timeStr.contains(":")) {
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                return (Integer.parseInt(parts[0].trim()) * 60) + Integer.parseInt(parts[1].trim());
            }
        }
        return Integer.parseInt(timeStr);
    }

    public void deleteByLesson(Long lessonId) {
        List<LessonTranscript> transcripts = transcriptRepo.findByLessonIdOrderByStartSecondAsc(lessonId);
        transcriptRepo.deleteAll(transcripts);
    }
}
