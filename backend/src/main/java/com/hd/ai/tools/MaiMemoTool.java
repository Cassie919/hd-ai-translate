package com.hd.ai.tools;

import com.hd.ai.maiMemo.service.MaiMemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 推送墨墨背单词工具（仅推送单词到云词本，不含例句/译文）。
 * <p>适用于：用户要求把生词推送到墨墨背单词云词本。
 * 前置条件：用户必须已在系统中上传过墨墨 API Token（通过 set-token 接口），否则会提示先设置 Token。
 * userId 取自 {@code UserContext}（由请求拦截器在同一 HTTP 线程中设置）。
 */
@Slf4j
@Component
public class MaiMemoTool {

    private final MaiMemoService maiMemoService;

    public MaiMemoTool(MaiMemoService maiMemoService) {
        this.maiMemoService = maiMemoService;
    }

    @Tool(description = "将生词推送到墨墨背单词云词本（仅添加单词，不含例句/译文）。" +
            "适用于：用户要求把单词加入墨墨记忆、生词本。" +
            "需要用户已上传墨墨 Token，若提示未设置 Token 请引导用户先到设置页上传。" +
            "参数 wordsText 为单词列表，可用逗号或换行分隔，例如 \"abandon, ability, abandon\"。", returnDirect = false)
    public String pushMaiMemo(
            @ToolParam(description = "要推送的单词列表，逗号或换行分隔，例如 \"abandon, ability, abandon\"") String wordsText) {
        try {
            List<String> words = parseWords(wordsText);
            if (words.isEmpty()) {
                throw new RuntimeException("未解析到任何有效的单词");
            }
            var result = maiMemoService.addWords(
                    com.hd.ai.common.interceptor.UserContext.getUserId(),
                    words
            );
            return "已推送 " + words.size() + " 个单词到墨墨（新增：" + result.get("uniqueWords")
                    + "，重复跳过：" + result.get("duplicateWords") + "）";
        } catch (Exception e) {
            log.error("推送墨墨失败", e);
            throw new RuntimeException("推送墨墨失败: " + e.getMessage());
        }
    }

    /**
     * 解析逗号/换行分隔的单词文本为去重后的单词列表。
     */
    private List<String> parseWords(String wordsText) {
        List<String> words = new ArrayList<>();
        if (wordsText == null || wordsText.isBlank()) {
            return words;
        }
        for (String raw : wordsText.split("[,\\n\\r]")) {
            String w = raw.trim();
            if (!w.isEmpty() && !words.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }

    @Tool(description = "根据翻译会话中的原文，把指定单词（带原文例句和译文）一键推送到墨墨背单词。" +
            "适用于：用户说\"给 abandon、ability 添加例句推送墨墨\"这类指令，希望用原文例句而非手动提供。" +
            "参数 sessionId 为当前翻译会话ID，wordsText 为要推送的单词列表（逗号或换行分隔）。" +
            "需要用户已上传墨墨 Token。例句与译文会自动从翻译会话的原文句中提取，无需用户另行提供。", returnDirect = false)
    public String pushSelectedWords(
            @ToolParam(description = "当前翻译会话ID，例如从对话上下文中获取的 sessionId") String sessionId,
            @ToolParam(description = "要推送的单词列表，逗号或换行分隔，例如 \"abandon, ability\"") String wordsText) {
        try {
            if (sessionId == null || sessionId.isBlank()) {
                throw new RuntimeException("缺少翻译会话ID（sessionId），无法从原文提取例句");
            }
            var result = maiMemoService.pushSelectedWords(
                    com.hd.ai.common.interceptor.UserContext.getUserId(),
                    sessionId,
                    wordsText
            );
            @SuppressWarnings("unchecked")
            List<String> pushed = (List<String>) result.get("pushedWords");
            @SuppressWarnings("unchecked")
            List<String> notFound = (List<String>) result.get("notFoundWords");
            StringBuilder sb = new StringBuilder();
            sb.append("已推送 ").append(pushed.size()).append(" 个单词（含原文例句）到墨墨");
            if (notFound != null && !notFound.isEmpty()) {
                sb.append("；未找到原文例句的单词：").append(String.join(", ", notFound));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("按单词推送墨墨失败", e);
            throw new RuntimeException("按单词推送墨墨失败: " + e.getMessage());
        }
    }
}
