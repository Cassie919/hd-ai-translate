package com.hd.ai.translate.service;

import com.hd.ai.translate.app.APP_v1;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.dto.TranslateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final APP_v1 appV1;
    private final ChapterService chapterService;
    private final PromptService promptService;
    private final SessionService sessionService;

    public List<TranslateItem> translateByParagraph(TranslateRequest request) {
        String sessionId = request.getSessionId();
        String content = chapterService.getChapterContent(request.getChapterTitle());
        List<String> paragraphs = chapterService.splitIntoParagraphs(content);

        // 将标题作为第一个段落
        paragraphs.add(0, request.getChapterTitle());

        log.info("开始翻译章节: sessionId={}, 段落数={}", sessionId, paragraphs.size());

        try {
            List<TranslateItem> results = translateWithFallback(paragraphs);

            // 保存到会话
            sessionService.addTranslateItems(sessionId, results);
            sessionService.updateCurrentChapter(sessionId, request.getChapterTitle());

            return results;

        } catch (Exception e) {
            log.error("翻译失败: sessionId={}", sessionId, e);
            return null;
        }
    }

    public List<TranslateItem> translateText(String content) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        List<String> paragraphs = chapterService.splitIntoParagraphs(content);
        log.info("开始翻译文本，段落数={}", paragraphs.size());

        try {
            return translateWithFallback(paragraphs);
        } catch (Exception e) {
            log.error("翻译文本失败", e);
            return null;
        }
    }

    /**
     * 将单词列表还原为原型（词形还原）。
     * 借助大模型结合语境判断词性，输出每个单词对应的原型，顺序与输入一致。
     *
     * @param words 待还原的单词列表
     * @return 与输入顺序对应的原型列表
     */
    public List<String> lemmatizeWords(List<String> words) {
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }

        String prompt = buildLemmatizePrompt(words);
        log.info("词性还原请求，单词数={}", words.size());

        List<String> lemmas = appV1.ask(prompt, new ParameterizedTypeReference<List<String>>() {});
        log.info("词性还原完成，原词={}, 原型={}", words, lemmas);

        // 兜底：若返回数量不一致，以原词补齐，保证输出与输入等长
        if (lemmas == null || lemmas.size() != words.size()) {
            log.warn("词性还原返回数量不一致，原词数={}, 返回数={}，使用原词兜底", words.size(), lemmas == null ? 0 : lemmas.size());
            List<String> fallback = new ArrayList<>();
            for (int i = 0; i < words.size(); i++) {
                String lemma = (lemmas != null && i < lemmas.size()) ? lemmas.get(i) : null;
                fallback.add(lemma != null && !lemma.isBlank() ? lemma : words.get(i));
            }
            return fallback;
        }
        return lemmas;
    }

    /**
     * 构建词性还原的提示词。
     */
    private String buildLemmatizePrompt(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个英语词形还原（lemmatization）助手。请将下面每个英文单词还原为其原型（字典词条形式），");
        sb.append("并结合常见语境判断词性。例如 running -> run, better -> good, mice -> mouse, went -> go。");
        sb.append("只输出一个 JSON 数组，元素顺序与输入单词顺序严格一一对应，不要输出任何解释或其他内容。\n");
        sb.append("输入单词：");
        sb.append(String.join(", ", words));
        return sb.toString();
    }

    private List<TranslateItem> translateWithFallback(List<String> paragraphs) {
        List<TranslateItem> allResults = new ArrayList<>();
        int chunkSize = 100;
        int startIndex = 0;

        while (startIndex < paragraphs.size()) {
            List<String> chunk = new ArrayList<>();
            int currentLength = 0;

            while (startIndex < paragraphs.size()) {
                String para = paragraphs.get(startIndex);
                if (currentLength + para.length() > chunkSize && !chunk.isEmpty()) {
                    break;
                }
                chunk.add(para);
                currentLength += para.length();
                startIndex++;
            }

            try {
                List<TranslateItem> chunkResults = translateChunk(chunk, allResults.size());
                allResults.addAll(chunkResults);
                log.info("Chunk翻译完成，当前进度: {}/{}", allResults.size(), paragraphs.size());
            } catch (Exception e) {
                log.error("Chunk翻译失败", e);
                for (String para : chunk) {
                    allResults.add(new TranslateItem(allResults.size() + 1, "Translation failed", para));
                }
            }
        }

        return allResults;
    }

    private List<TranslateItem> translateChunk(List<String> paragraphs, int globalIndex) throws Exception {
        StringBuilder textToTranslate = new StringBuilder();
        for (int i = 0; i < paragraphs.size(); i++) {
            textToTranslate.append(i + 1).append(". ").append(paragraphs.get(i).trim()).append("\n");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("content",textToTranslate.toString());
        log.info("params: {}", params);
        String renderPrompt = promptService.renderPrompt("novel-translate-v3", params);

        List<TranslateItem> results = appV1.ask(renderPrompt);

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setIndex(globalIndex + i + 1);
        }

        return results;
    }
}
