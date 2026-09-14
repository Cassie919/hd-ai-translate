package com.hd.ai.maiMemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mai-memo.api")
public class MaiMemoConfig {
    private String baseUrl;
}
