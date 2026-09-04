package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.config.LlmModelsConfig;
import com.NGLP.backend.v1.entity.UserAiPreference;
import com.NGLP.backend.v1.repo.UserAiPreferenceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LlmRouterService {

    private final List<LlmProvider> providers;
    private final UserAiPreferenceRepo preferenceRepo;
    private final LlmModelsConfig modelsConfig;
    private final Map<String, LlmProvider> providerMap;
    private final String defaultProviderKey;

    public LlmRouterService(List<LlmProvider> providers,
                            UserAiPreferenceRepo preferenceRepo,
                            LlmModelsConfig modelsConfig,
                            @Value("${nglp.llm.default-provider:groq}") String defaultProviderKey) {
        this.providers = providers;
        this.preferenceRepo = preferenceRepo;
        this.modelsConfig = modelsConfig;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(LlmProvider::getProviderKey, Function.identity()));
        this.defaultProviderKey = defaultProviderKey;
    }

    public LlmProvider resolveProvider(Long userId) {
        return resolveProviderChain(userId).get(0);
    }

    /**
     * يبني ترتيب محاولة المزوّدين لمستخدم معيّن: التفضيل المحفوظ (إن وُجد) فالمزوّد
     * الافتراضي فبقية المزوّدين — بحيث يُقدَّم كل مزوّد سليم فعلياً ({@link LlmProvider#isHealthy()})
     * على أي مزوّد معطوب في آخر فحص، دون استبعاد المعطوب كلياً (آخر ملاذ قبل الفشل التام).
     * تستخدمها {@code NglpAiAgent} لتجربة كل مزوّد بالترتيب حتى ينجح أحدها، بدل إرجاع خطأ
     * عام للطالب لمجرد تعطّل المزوّد الأول. تُرمى استثناء فقط إن لم يكن أي مزوّد مفعّلاً أصلاً.
     */
    public List<LlmProvider> resolveProviderChain(Long userId) {
        LinkedHashSet<LlmProvider> chain = new LinkedHashSet<>();

        UserAiPreference pref = preferenceRepo.findByUserId(userId).orElse(null);
        LlmProvider preferred = pref != null ? providerMap.get(pref.getProviderKey()) : null;
        LlmProvider defaultProvider = providerMap.get(defaultProviderKey);

        // الجولة الأولى: كل مزوّد سليم فعلياً الآن، مرتّب حسب الأفضلية.
        if (usable(preferred)) chain.add(preferred);
        if (usable(defaultProvider)) chain.add(defaultProvider);
        providers.stream().filter(this::usable).forEach(chain::add);

        // الجولة الثانية (آخر ملاذ): أي مزوّد مفعّل حتى لو ظهر معطوباً في آخر فحص دوري —
        // فشل الفحص الدوري لا يعني بالضرورة فشلاً فعلياً الآن.
        if (preferred != null && preferred.isEnabled()) chain.add(preferred);
        if (defaultProvider != null && defaultProvider.isEnabled()) chain.add(defaultProvider);
        providers.stream().filter(LlmProvider::isEnabled).forEach(chain::add);

        if (chain.isEmpty()) {
            throw new RuntimeException("لا يوجد مزود LLM مفعّل حالياً");
        }
        if (log.isDebugEnabled()) {
            log.debug("User {} provider chain: {}", userId,
                    chain.stream().map(LlmProvider::getProviderKey).collect(Collectors.joining(" -> ")));
        }
        return List.copyOf(chain);
    }

    private boolean usable(LlmProvider provider) {
        return provider != null && provider.isEnabled() && provider.isHealthy();
    }

    public List<ProviderDto> getAvailableProviders() {
        return providers.stream()
                .filter(LlmProvider::isEnabled)
                .map(p -> {
                    List<ProviderDto.ModelDto> modelDtos = p.getModels().stream()
                            .map(m -> new ProviderDto.ModelDto(m.key(), m.name(), m.isFree()))
                            .collect(Collectors.toList());
                    return new ProviderDto(p.getProviderKey(), modelDtos);
                })
                .collect(Collectors.toList());
    }

    public UserAiPreference getUserPreference(Long userId) {
        return preferenceRepo.findByUserId(userId)
                .orElse(null);
    }

    public UserAiPreference updateUserPreference(Long userId, String providerKey, String modelKey) {
        LlmProvider provider = providerMap.get(providerKey);
        if (provider == null || !provider.isEnabled()) {
            throw new IllegalArgumentException("The provider not found" + providerKey);
        }
        boolean modelValid = provider.getModels().stream()
                .anyMatch(m -> m.key().equals(modelKey));
        if (!modelValid) {
            throw new IllegalArgumentException("The model is not available for this provider" + modelKey);
        }
        UserAiPreference pref = preferenceRepo.findByUserId(userId)
                .orElse(UserAiPreference.builder()
                        .userId(userId)
                        .build());
        pref.setProviderKey(providerKey);
        pref.setModelKey(modelKey);
        pref.setUpdatedAt(java.time.LocalDateTime.now());
        return preferenceRepo.save(pref);
    }

    public record ProviderDto(String key, List<ModelDto> models) {
        public record ModelDto(String key, String name, boolean isFree) {}
    }
}
