package com.NGLP.backend.v1.ai;

import com.NGLP.backend.v1.config.LlmModelsConfig;
import com.NGLP.backend.v1.entity.UserAiPreference;
import com.NGLP.backend.v1.repo.UserAiPreferenceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

    public LlmRouterService(List<LlmProvider> providers,
                            UserAiPreferenceRepo preferenceRepo,
                            LlmModelsConfig modelsConfig) {
        this.providers = providers;
        this.preferenceRepo = preferenceRepo;
        this.modelsConfig = modelsConfig;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(LlmProvider::getProviderKey, Function.identity()));
    }

    public LlmProvider resolveProvider(Long userId) {
        UserAiPreference pref = preferenceRepo.findByUserId(userId).orElse(null);
        if (pref != null) {
            LlmProvider provider = providerMap.get(pref.getProviderKey());
            if (provider != null && provider.isEnabled()) {
                return provider;
            }
        }
        LlmProvider fallback = providers.stream()
                .filter(LlmProvider::isEnabled)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("لا يوجد مزود LLM مفعّل حالياً"));
        log.info("User {} has no AI preference, falling back to {}", userId, fallback.getProviderKey());
        return fallback;
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

    public ChatClient.Builder createBuilder(Long userId) {
        LlmProvider provider = resolveProvider(userId);
        return provider.createChatClientBuilder();
    }

    public record ProviderDto(String key, List<ModelDto> models) {
        public record ModelDto(String key, String name, boolean isFree) {}
    }
}
