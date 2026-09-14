package com.hd.ai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条对话消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageItem {

    /**
     * 消息角色，如 user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
