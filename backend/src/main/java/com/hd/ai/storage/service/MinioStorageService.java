package com.hd.ai.storage.service;

import com.hd.ai.storage.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * 基于 MinIO 的存储服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /** 应用启动时确保存储桶存在 */
    @PostConstruct
    public void init() {
        try {
            ensureBucket();
            log.info("MinIO bucket 就绪: {}", minioConfig.getBucketName());
        } catch (Exception e) {
            log.warn("MinIO bucket 初始化失败（首次上传时会重试）: {}", e.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        String bucket = minioConfig.getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @Override
    public String upload(String objectName, InputStream stream, String contentType, long size) throws Exception {
        ensureBucket();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .stream(stream, size, -1)
                .contentType(contentType)
                .build());
        return objectName;
    }

    @Override
    public String upload(String objectName, MultipartFile file) throws Exception {
        return upload(objectName, file.getInputStream(), file.getContentType(), file.getSize());
    }

    @Override
    public InputStream download(String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build());
    }

    @Override
    public void delete(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build());
    }

    @Override
    public String getUrl(String objectName) throws Exception {
        return getUrl(objectName, 60);
    }

    @Override
    public String getUrl(String objectName, int expireMinutes) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .method(Method.GET)
                .expiry(expireMinutes, TimeUnit.MINUTES)
                .build());
    }

    @Override
    public String getPublicUrl(String objectName) {
        return String.format("%s/%s/%s",
                minioConfig.getEndpoint(),
                minioConfig.getBucketName(),
                objectName);
    }
}
