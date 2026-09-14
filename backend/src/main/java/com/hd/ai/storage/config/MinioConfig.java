package com.hd.ai.storage.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 * <p>配置项前缀 {@code minio}，对应 application.yml 中的：
 * <pre>
 * minio:
 *   endpoint: http://127.0.0.1:9000
 *   access-key: admin
 *   secret-key: 12345678
 *   bucket-name: english-ai
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
