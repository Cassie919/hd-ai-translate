package com.hd.ai.translate.app;

import com.hd.ai.translate.dto.TranslateItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class APP_v1 {

    private final ChatClient chatClient;

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public APP_v1(ChatModel dashscopeChatModel) {

        chatClient = ChatClient.builder(dashscopeChatModel)
                .build();
    }




    public List<TranslateItem> ask(String message) {
        log.info("开始翻译请求");
        List<TranslateItem> result = chatClient
                .prompt()
                .user(message)
                .call()
                .entity(new ParameterizedTypeReference<List<TranslateItem>>() {});
        log.info("翻译请求完成，返回结果: {}", result);
        return result;
    }

    /**
     * 通用 AI 调用方法，按指定类型反序列化返回结果。
     * 适用于返回结构非 {@link TranslateItem} 列表的场景，例如词性还原返回字符串列表。
     *
     * @param message 提示词
     * @param typeRef 目标返回类型的类型引用
     * @param <T>     返回类型
     * @return AI 返回结果
     */
    public <T> T ask(String message, ParameterizedTypeReference<T> typeRef) {
        log.info("开始 AI 请求");
        T result = chatClient
                .prompt()
                .user(message)
                .call()
                .entity(typeRef);
        log.info("AI 请求完成，返回结果: {}", result);
        return result;
    }


}
