package com.hd.ai.translate.service;

import com.hd.ai.translate.config.PromptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class PromptService {
    private final PromptConfig promptConfig;

    public PromptService(PromptConfig promptConfig) {
        this.promptConfig = promptConfig;
    }

    public String renderPrompt(String key, Map<String, Object> params) {
        String template = promptConfig.getTemplateByKey(key);
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }

    public Map<String, PromptConfig.PromptItem> getAllPromptTemplates() {
        return promptConfig.getAllPromptTemplates();
    }
}