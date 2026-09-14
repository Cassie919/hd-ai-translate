package com.hd.ai.phraseChunk.service;

import com.hd.ai.phraseChunk.app.PhraseChunkAI;
import com.hd.ai.phraseChunk.dto.PhraseChunkItem;
import com.hd.ai.phraseChunk.dto.PhraseChunkRequest;
import com.hd.ai.phraseChunk.dto.PhraseChunkResponse;
import com.hd.ai.phraseChunk.dto.TextChunkRequest;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhraseChunkService {

    private final PhraseChunkAI phraseChunkAI;
    private final PromptService promptService;

    /**
     * 按章节意群划分，切分策略与翻译时保持一致（chunkSize=100 字符）
     */
    public PhraseChunkResponse chunk(PhraseChunkRequest request) {
        List<TranslateItem> items = request.getItems();
        List<PhraseChunkItem> allResults = new ArrayList<>();
        int chunkSize = 100;
        int startIndex = 0;

        while (startIndex < items.size()) {
            List<TranslateItem> chunk = new ArrayList<>();
            int currentLength = 0;

            while (startIndex < items.size()) {
                TranslateItem item = items.get(startIndex);
                String enText = item.getEn() != null ? item.getEn().trim() : "";
                if (currentLength + enText.length() > chunkSize && !chunk.isEmpty()) {
                    break;
                }
                chunk.add(item);
                currentLength += enText.length();
                startIndex++;
            }

            try {
                List<PhraseChunkItem> chunkResults = chunkPhrase(chunk);
                allResults.addAll(chunkResults);
                log.info("意群划分 Chunk 完成，当前进度: {}/{}", allResults.size(), items.size());
            } catch (Exception e) {
                log.error("意群划分 Chunk 失败", e);
            }
        }

        log.info("意群划分完成，共 {} 条结果", allResults.size());
        return new PhraseChunkResponse(allResults);
    }

    /**
     * 对单个 chunk 进行意群划分
     */
    private List<PhraseChunkItem> chunkPhrase(List<TranslateItem> chunk) throws Exception {
        // 构建编号的英文文本（chunk 内从 1 开始编号）
        StringBuilder englishText = new StringBuilder();
        for (int i = 0; i < chunk.size(); i++) {
            englishText.append(i + 1).append(". ").append(chunk.get(i).getEn().trim()).append("\n");
        }

        // 渲染意群划分提示词
        Map<String, Object> params = new HashMap<>();
        params.put("content", englishText.toString());
        log.info("意群划分参数: {}", params);
        String prompt = promptService.renderPrompt("phrase-chunk-v2", params);

        // 调用 AI
        List<PhraseChunkItem> results = phraseChunkAI.ask(prompt);

        // 重新编号对齐，同时回填 cn（使用 chunk 中的原始 index 和 cn）
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setIndex(chunk.get(i).getIndex());
            results.get(i).setCn(chunk.get(i).getCn());
        }

        return results;
    }

    public PhraseChunkResponse chunkText(TextChunkRequest request) {
        List<String> texts = request.getTexts();

        // 构建编号的英文文本
        StringBuilder englishText = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            englishText.append(i + 1).append(". ").append(texts.get(i).trim()).append("\n");
        }

        // 渲染意群划分提示词
        Map<String, Object> params = new HashMap<>();
        params.put("content", englishText.toString());
        log.info("意群划分(文本)参数: {}", params);
        String prompt = promptService.renderPrompt("phrase-chunk-v2", params);

        // 调用 AI
        List<PhraseChunkItem> results = phraseChunkAI.ask(prompt);

        // 重新编号
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setIndex(i + 1);
        }

        log.info("意群划分(文本)完成，共 {} 条结果", results.size());
        return new PhraseChunkResponse(results);
    }

    /**
     * 对单段纯文本进行意群划分，入参为 String（按换行拆分为多条文本后处理）。
     */
    public PhraseChunkResponse chunkText(String text) {
        if (text == null || text.isBlank()) {
            return new PhraseChunkResponse(new ArrayList<>());
        }
        List<String> texts = java.util.Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return chunkText(new TextChunkRequest(texts));
    }

}
