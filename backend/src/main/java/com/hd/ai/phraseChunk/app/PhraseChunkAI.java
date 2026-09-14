package com.hd.ai.phraseChunk.app;

import com.hd.ai.phraseChunk.dto.PhraseChunkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PhraseChunkAI {

    private final ChatClient chatClient;

    public PhraseChunkAI(ChatModel dashscopeChatModel) {
        chatClient = ChatClient.builder(dashscopeChatModel)
                .build();
    }

    public List<PhraseChunkItem> ask(String message) {
        log.info("开始意群划分请求");
        List<PhraseChunkItem> result = chatClient
                .prompt()
                .user(message)
                .call()
                .entity(new ParameterizedTypeReference<List<PhraseChunkItem>>() {});
        log.info("意群划分请求完成，返回 {} 条结果", result != null ? result.size() : 0);
        return result;
    }

}
