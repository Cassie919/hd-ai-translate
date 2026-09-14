package com.hd.ai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话历史响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {

    /**
     * 会话 ID
     */
    private String chatId;

    /**
     * 历史消息列表
     */
    private List<ChatMessageItem> messages;
}
