package com.hd.ai.chat.controller;

import com.hd.ai.chat.app.ChatApp;
import com.hd.ai.chat.dto.ChatHistoryResponse;
import com.hd.ai.chat.dto.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatApp chatApp;

    /**
     * 创建新会话，返回后端生成的 chatId。
     * 初次对话前端无 chatId 时调用。
     */
    @PostMapping("/create")
    public ChatResponse createChat() {
        String chatId = chatApp.createChatId();
        return new ChatResponse(chatId);
    }

    @PostMapping("/do-chat")
    public String doChat(@RequestParam String message,
                         @RequestParam String chatId) {
        if (message == null || message.isBlank() || chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("message 和 chatId 不能为空");
        }
        return chatApp.doChat(message, chatId);
    }

    /**
     * 查询会话历史记录
     */
    @GetMapping("/history")
    public ChatHistoryResponse getHistory(@RequestParam String chatId) {
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        return chatApp.getChatHistory(chatId);
    }

    /**
     * 查询所有会话 ID 列表
     */
    @GetMapping("/list")
    public List<String> listChatIds() {
        return chatApp.listChatIds();
    }

}
