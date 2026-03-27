package com.hd.ai.translate.service;

import com.hd.ai.translate.app.APP_v1;
import com.hd.ai.translate.dto.SessionData;
import com.hd.ai.translate.dto.SessionMeta;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.dto.TranslateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<TranslateItem> translateChapter(TranslateRequest request) {
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
