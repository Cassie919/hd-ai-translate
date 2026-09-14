package com.hd.ai.chat.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天记忆的纯数据载体（用于 Jackson 序列化）
 *
 * 不直接序列化 Spring AI 的 Message（Record + 多态，Jackson 反序列化困难），
 * 而是用这个简单 POJO 存 role + content，规避多态/Record 问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryItem {

    /**
     * 消息角色：system / user / assistant / tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
