package com.hd.ai.vocabPrediction.app;

import com.hd.ai.vocabPrediction.dto.VocabWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class VocabPredictionAI {

    private final ChatClient chatClient;

    public VocabPredictionAI(ChatModel dashscopeChatModel) {
        chatClient = ChatClient.builder(dashscopeChatModel)
                .build();
    }

    public List<VocabWord> ask(String message) {
        log.info("开始生词预测请求");
        List<VocabWord> result = chatClient
                .prompt()
                .user(message)
                .call()
                .entity(new ParameterizedTypeReference<List<VocabWord>>() {});
        log.info("生词预测请求完成，返回 {} 条结果", result != null ? result.size() : 0);
        return result;
    }

}
