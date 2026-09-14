package com.hd.ai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话相关响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话 ID，前端需在后续对话中回传
     */
    private String chatId;
}
