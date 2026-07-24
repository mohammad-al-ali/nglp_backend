package com.NGLP.backend.v1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "nglp.llm")
public class LlmModelsConfig {

    private List<ProviderConfig> providers = new ArrayList<>();

    public List<ProviderConfig> getProviders() { return providers; }
    public void setProviders(List<ProviderConfig> providers) { this.providers = providers; }

    public static class ProviderConfig {
        private String key;
        private String apiKey;
        private String baseUrl;
        private String defaultModel;
        private boolean enabled = true;
        private List<ModelOption> models = new ArrayList<>();

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<ModelOption> getModels() { return models; }
        public void setModels(List<ModelOption> models) { this.models = models; }
    }

    public static class ModelOption {
        private String key;
        private String name;
        private boolean isFree;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isFree() { return isFree; }
        public void setFree(boolean isFree) { this.isFree = isFree; }
    }
}