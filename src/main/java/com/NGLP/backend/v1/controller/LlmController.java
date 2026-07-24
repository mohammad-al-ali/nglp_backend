package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.ai.LlmRouterService;
import com.NGLP.backend.v1.entity.UserAiPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmRouterService routerService;

    @GetMapping("/providers")
    public ResponseEntity<List<LlmRouterService.ProviderDto>> getProviders() {
        return ResponseEntity.ok(routerService.getAvailableProviders());
    }

    @GetMapping("/users/{userId}/settings")
    public ResponseEntity<?> getUserSettings(@PathVariable Long userId) {
        UserAiPreference pref = routerService.getUserPreference(userId);
        if (pref == null) {
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "providerKey", "",
                    "modelKey", ""
            ));
        }
        return ResponseEntity.ok(Map.of(
                "userId", pref.getUserId(),
                "providerKey", pref.getProviderKey(),
                "modelKey", pref.getModelKey(),
                "updatedAt", pref.getUpdatedAt()
        ));
    }

    @PutMapping("/users/{userId}/settings")
    public ResponseEntity<?> updateUserSettings(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String providerKey = body.get("providerKey");
        String modelKey = body.get("modelKey");
        if (providerKey == null || modelKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "providerKey ? modelKey ???????"));
        }
        try {
            UserAiPreference pref = routerService.updateUserPreference(userId, providerKey, modelKey);
            return ResponseEntity.ok(Map.of(
                    "userId", pref.getUserId(),
                    "providerKey", pref.getProviderKey(),
                    "modelKey", pref.getModelKey(),
                    "updatedAt", pref.getUpdatedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}