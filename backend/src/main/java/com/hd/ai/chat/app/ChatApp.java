package com.hd.ai.chat.app;

import com.hd.ai.chat.dto.ChatHistoryResponse;
import com.hd.ai.chat.dto.ChatMessageItem;
import com.hd.ai.chat.memory.RedisChatMemoryRepository;
import com.hd.ai.skill.EnglishLearningSkill;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChatApp {
    private final ChatClient chatClient;

    @Resource
    private ToolCallback[] allTools;


    @Autowired
    private EnglishLearningSkill englishLearningSkill;
/*初始化*/
    public ChatApp(ChatModel dashscopeChatModel, RedisChatMemoryRepository chatMemoryRepository){
        // 初始化基于 Redis 的对话记忆（持久化到 Redis，重启后仍可复用历史）
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        // 自定义日志 Advisor，可按需开启
//                        ,new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }


    @Resource
    private RedisChatMemoryRepository chatMemoryRepository;

    /**
     * 生成新的会话 ID（使用 UUID，去掉连字符保证简洁）
     *
     * @return 新会话 ID
     */
    public String createChatId() {
        String chatId = UUID.randomUUID().toString().replace("-", "");
        log.info("创建新会话, chatId={}", chatId);
        return chatId;
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        try {
            ChatResponse chatResponse = chatClient
                    .prompt(englishLearningSkill.getPrompt())
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .toolCallbacks(englishLearningSkill.getTools())
                    .call()
                    .chatResponse();
            String content = chatResponse.getResult().getOutput().getText();
            log.info("doChat 调用成功, chatId={}, content={}", chatId, content);
            if (content == null || content.isBlank()) {
                log.warn("AI 返回内容为空, chatId={}", chatId);
                return "";
            }
            return content;
        } catch (Exception e) {
            log.error("doChat 调用失败, chatId={}", chatId, e);
            return "抱歉，当前服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 查询会话历史记录
     *
     * @param chatId 会话 ID
     * @return 历史记录
     */
    public ChatHistoryResponse getChatHistory(String chatId) {
        List<Message> messages = chatMemoryRepository.findByConversationId(chatId);
        List<ChatMessageItem> items = messages.stream()
                .map(m -> new ChatMessageItem(resolveRole(m), getMessageText(m)))
                .collect(Collectors.toList());
        return new ChatHistoryResponse(chatId, items);
    }

    /**
     * 查询所有会话 ID
     *
     * @return 会话 ID 列表
     */
    public List<String> listChatIds() {
        return chatMemoryRepository.findConversationIds();
    }

    private String resolveRole(Message message) {
        MessageType type = message.getMessageType();
        return type == null ? "unknown" : type.name().toLowerCase();
    }

    private String getMessageText(Message message) {
        String content = message.getText();
        return content == null ? "" : content;
    }



}
