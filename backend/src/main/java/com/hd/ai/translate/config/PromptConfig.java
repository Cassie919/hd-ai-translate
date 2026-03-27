package com.hd.ai.translate.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ai")
public class PromptConfig {
    private static final Logger logger = LoggerFactory.getLogger(PromptConfig.class);


    private Map<String, PromptItem> promptsMap;

    @Data
    public static class PromptItem {
        private String name;
        private String template;
    }

    /**
     * 初始化后打印配置信息，便于调试
     */
    @PostConstruct
    public void init() {
        logger.info("PromptConfig 初始化");

        if (promptsMap != null) {
            //logger.info("成功加载 {} 个提示词模板: {}", promptsMap.size(), promptsMap.keySet());
            logger.info("成功加载 {} 个提示词模板", promptsMap.size());

        } else {
            logger.warn("未找到提示词模板,{}",promptsMap);
        }
    }

    public String getTemplateByKey(String key) {
        if (promptsMap == null) {
            throw new IllegalStateException("提示词模板未初始化，请检查 Nacos 配置文件 ai-prompts.yaml 是否正确");
        }
        if (!promptsMap.containsKey(key)) {
            logger.error("未找到提示词模板: {}, 可用模板: {}", key, promptsMap.keySet());
            throw new IllegalArgumentException("未找到提示词模板: " + key);
        }
        return promptsMap.get(key).getTemplate();
    }

    public Map<String, PromptItem> getAllPromptTemplates() {
        if (promptsMap == null) {
            throw new IllegalStateException("提示词模板未初始化，请检查 Nacos 配置文件 ai-prompts.yaml");
        }
        logger.info("返回提示词模板: {}", Collections.unmodifiableMap(promptsMap));
        return Collections.unmodifiableMap(promptsMap);
    }

}
