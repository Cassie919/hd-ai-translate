package com.hd.ai.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 对象存储服务接口，屏蔽底层存储实现（当前为 MinIO）。
 */
public interface StorageService {

    /**
     * 上传文件流。
     *
     * @return 对象名（存储路径），可用于后续下载 / 删除 / 获取 URL
     */
    String upload(String objectName, InputStream stream, String contentType, long size) throws Exception;

    /** 上传 Spring Web 接收的 MultipartFile */
    String upload(String objectName, MultipartFile file) throws Exception;

    /** 下载文件，返回输入流，调用方需自行关闭 */
    InputStream download(String objectName) throws Exception;

    /** 删除文件 */
    void delete(String objectName) throws Exception;

    /** 获取带时效的预签名访问 URL（默认 60 分钟） */
    String getUrl(String objectName) throws Exception;

    /** 获取带时效的预签名访问 URL，expireMinutes 为有效期（分钟） */
    String getUrl(String objectName, int expireMinutes) throws Exception;

    /** 获取公开访问 URL（需存储桶为 public 可读） */
    String getPublicUrl(String objectName);
}
