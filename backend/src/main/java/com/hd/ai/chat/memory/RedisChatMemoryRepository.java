package com.hd.ai.chat.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "ai:chat:memory:";

    /**
     * 会话记录过期时间：7 天
     */
    private static final long TTL_DAYS = 7;

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据 conversationId 获取聊天消息
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = buildKey(conversationId);

        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = bucket.get();

        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        List<ChatMemoryItem> items = readItems(json);
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    /**
     * 保存聊天消息
     *
     * 注意：
     * saveAll 是“整体替换”，不是追加。
     * 存储时转成纯 POJO（ChatMemoryItem）并用 Jackson 序列化，
     * 规避直接序列化 Message（Record + 多态）带来的反序列化问题。
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = buildKey(conversationId);

        List<ChatMemoryItem> items = messages.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        RBucket<String> bucket = redissonClient.getBucket(key);

        // 写入时同时设置 7 天过期时间（每次对话都会刷新 TTL）
        bucket.set(writeItems(items), TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 删除整个会话
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        redissonClient.getBucket(key).delete();
    }

    /**
     * 查询所有会话 ID
     */
    @Override
    public List<String> findConversationIds() {
        List<String> conversationIds = new ArrayList<>();

        Iterable<String> keys =
                redissonClient.getKeys()
                        .getKeysByPattern(KEY_PREFIX + "*");

        for (String key : keys) {
            conversationIds.add(
                    key.substring(KEY_PREFIX.length())
            );
        }

        return conversationIds;
    }

    // ---------- Message <-> ChatMemoryItem 转换 ----------

    private ChatMemoryItem toItem(Message message) {
        MessageType type = message.getMessageType();
        String role = type == null ? "unknown" : type.name().toLowerCase();
        String content = message.getText();
        return new ChatMemoryItem(role, content == null ? "" : content);
    }

    private Message toMessage(ChatMemoryItem item) {
        String role = item.getRole();
        String content = item.getContent() == null ? "" : item.getContent();

        if ("system".equalsIgnoreCase(role)) {
            return new SystemMessage(content);
        }
        if ("assistant".equalsIgnoreCase(role)) {
            return new AssistantMessage(content);
        }
        // user、tool 及其他未知角色，统一兜底为 UserMessage
        return new UserMessage(content);
    }

    // ---------- JSON 序列化 ----------

    private String writeItems(List<ChatMemoryItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("序列化聊天记录失败", e);
        }
    }

    private List<ChatMemoryItem> readItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatMemoryItem>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("反序列化聊天记录失败", e);
        }
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
