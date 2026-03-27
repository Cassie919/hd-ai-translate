package com.hd.ai.translate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hd.ai.translate.dto.SessionData;
import com.hd.ai.translate.dto.SessionMeta;
import com.hd.ai.translate.dto.SessionResponse;
import com.hd.ai.translate.dto.TranslateItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String SESSION_META_PREFIX = "translate:meta:";
    private static final String SESSION_DATA_PREFIX = "translate:data:";
    private static final long SESSION_TTL_MINUTES = 30;

    /**
     * 创建新会话
     */
    public String createSession(String fileName, List<String> chapters) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        SessionMeta meta = SessionMeta.builder()
                .sessionId(sessionId)
                .fileName(fileName)
                .title(fileName)
                .totalChapters(chapters.size())
                .currentChapterIndex(0)
                .status("PENDING")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        SessionData data = SessionData.builder()
                .sessionId(sessionId)
                .translateItems(new ArrayList<>())
                .build();

        saveSession(sessionId, meta, data);
        log.info("创建新会话: {}, 文件名: {}", sessionId, fileName);

        return sessionId;
    }

    /**
     * 获取会话元数据
     */
    public SessionMeta getSessionMeta(String sessionId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(SESSION_META_PREFIX + sessionId);
            String json = bucket.get();
            if (json != null) {
                return objectMapper.readValue(json, SessionMeta.class);
            }
        } catch (Exception e) {
            log.error("获取会话元数据失败: {}", sessionId, e);
        }
        return null;
    }

    /**
     * 获取会话数据
     */
    public SessionData getSessionData(String sessionId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(SESSION_DATA_PREFIX + sessionId);
            String json = bucket.get();
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<SessionData>() {});
            }
        } catch (Exception e) {
            log.error("获取会话数据失败: {}", sessionId, e);
        }
        return null;
    }

    /**
     * 保存会话
     */
    public void saveSession(String sessionId, SessionMeta meta, SessionData data) {
        try {
            meta.setUpdatedAt(System.currentTimeMillis());

            RBucket<String> metaBucket = redissonClient.getBucket(SESSION_META_PREFIX + sessionId);
            RBucket<String> dataBucket = redissonClient.getBucket(SESSION_DATA_PREFIX + sessionId);

            metaBucket.set(objectMapper.writeValueAsString(meta), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
            dataBucket.set(objectMapper.writeValueAsString(data), SESSION_TTL_MINUTES, TimeUnit.MINUTES);

            log.debug("保存会话成功: {}", sessionId);
        } catch (Exception e) {
            log.error("保存会话失败: {}", sessionId, e);
        }
    }

    /**
     * 更新会话元数据
     */
    public void updateSessionMeta(String sessionId, SessionMeta meta) {
        try {
            SessionData data = getSessionData(sessionId);
            if (data != null) {
                meta.setUpdatedAt(System.currentTimeMillis());
                RBucket<String> bucket = redissonClient.getBucket(SESSION_META_PREFIX + sessionId);
                bucket.set(objectMapper.writeValueAsString(meta), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("更新会话元数据失败: {}", sessionId, e);
        }
    }

    /**
     * 添加翻译结果
     */
    public void addTranslateItems(String sessionId, List<TranslateItem> items) {
        try {
            SessionData data = getSessionData(sessionId);
            if (data != null) {
                data.getTranslateItems().addAll(items);

                SessionMeta meta = getSessionMeta(sessionId);
                if (meta != null) {
                    meta.setStatus("IN_PROGRESS");
                    saveSession(sessionId, meta, data);
                } else {
                    RBucket<String> bucket = redissonClient.getBucket(SESSION_DATA_PREFIX + sessionId);
                    bucket.set(objectMapper.writeValueAsString(data), SESSION_TTL_MINUTES, TimeUnit.MINUTES);
                }

                log.info("添加翻译结果: sessionId={}, 新增条数={}", sessionId, items.size());
            }
        } catch (Exception e) {
            log.error("添加翻译结果失败: {}", sessionId, e);
        }
    }

    /**
     * 设置翻译完成
     */
    public void setTranslationCompleted(String sessionId) {
        SessionMeta meta = getSessionMeta(sessionId);
        if (meta != null) {
            meta.setStatus("COMPLETED");
            updateSessionMeta(sessionId, meta);
            log.info("翻译完成: {}", sessionId);
        }
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        RBucket<String> bucket = redissonClient.getBucket(SESSION_META_PREFIX + sessionId);
        return bucket.isExists();
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        redissonClient.getBucket(SESSION_META_PREFIX + sessionId).delete();
        redissonClient.getBucket(SESSION_DATA_PREFIX + sessionId).delete();
        log.info("删除会话: {}", sessionId);
    }

    /**
     * 获取完整会话
     */
    public SessionResponse getSession(String sessionId) {
        SessionMeta meta = getSessionMeta(sessionId);
        SessionData data = getSessionData(sessionId);

        boolean exists = meta != null && data != null;
        return new SessionResponse(exists, meta, data);
    }

    /**
     * 更新当前章节索引
     */
    public void updateCurrentChapter(String sessionId, String chapterTitle) {
        SessionMeta meta = getSessionMeta(sessionId);
        if (meta != null) {
            meta.setUpdatedAt(System.currentTimeMillis());
            updateSessionMeta(sessionId, meta);
        }
    }
}
