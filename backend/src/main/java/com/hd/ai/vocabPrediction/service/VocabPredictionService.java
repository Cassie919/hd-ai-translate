package com.hd.ai.vocabPrediction.service;

import com.hd.ai.translate.service.PromptService;
import com.hd.ai.vocabPrediction.app.VocabPredictionAI;
import com.hd.ai.vocabPrediction.dto.VocabPredictionRequest;
import com.hd.ai.vocabPrediction.dto.VocabPredictionResponse;
import com.hd.ai.vocabPrediction.dto.VocabWord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabPredictionService {

    private final VocabPredictionAI vocabPredictionAI;
    private final PromptService promptService;

    /**
     * 按章节生词预测，切分策略与翻译/意群划分时一致（chunkSize=100 字符）
     */
    public VocabPredictionResponse predict(VocabPredictionRequest request) {
        String text = request.getText();
        if (text == null || text.isBlank()) {
            return new VocabPredictionResponse(Collections.emptyList());
        }

        // 1. 拆分为句子
        List<String> sentences = splitSentences(text);
        log.info("生词预测: 原文拆分为 {} 个句子", sentences.size());

        // 2. 按 100 字符分块，逐块调用 AI
        Map<String, VocabWord> wordMap = new LinkedHashMap<>();
        int chunkSize = 100;
        int startIndex = 0;

        while (startIndex < sentences.size()) {
            List<String> chunk = new ArrayList<>();
            int currentLength = 0;

            while (startIndex < sentences.size()) {
                String sentence = sentences.get(startIndex);
                if (currentLength + sentence.length() > chunkSize && !chunk.isEmpty()) {
                    break;
                }
                chunk.add(sentence);
                currentLength += sentence.length();
                startIndex++;
            }

            try {
                List<VocabWord> chunkResults = predictChunk(chunk);
                chunkResults.forEach(w -> wordMap.putIfAbsent(w.getWord().toLowerCase(), w));
                log.info("生词预测 Chunk 完成，当前累计去重: {} 词", wordMap.size());
            } catch (Exception e) {
                log.error("生词预测 Chunk 失败", e);
            }
        }

        List<VocabWord> allWords = new ArrayList<>(wordMap.values());
        allWords.sort(Comparator.comparing(
                VocabWord::getScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        log.info("生词预测完成，共 {} 个词汇", allWords.size());
        return new VocabPredictionResponse(allWords);
    }

    /**
     * 将长文本拆分为句子列表
     * 优先按换行符拆分；若无换行符，按英文标点边界切句
     */
    private List<String> splitSentences(String text) {
        // 按换行拆分
        List<String> lines = Arrays.stream(text.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // 只有一行且文本较长，说明没有换行，按标点切句
        if (lines.size() == 1 && lines.get(0).length() > 200) {
            String single = lines.get(0);
            // 在 .!? 后跟空格处切句
            String[] parts = single.split("(?<=[.!?])\\s+");
            lines = Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return lines;
    }

    /**
     * 对单个 chunk 进行生词预测
     */
    private List<VocabWord> predictChunk(List<String> sentences) throws Exception {
        // 构建带编号的英文文本
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < sentences.size(); i++) {
            textBuilder.append(i + 1)
                    .append(". ").append(sentences.get(i))
                    .append("\n");
        }

        // 渲染生词预测提示词
        Map<String, Object> params = new HashMap<>();
        params.put("content", textBuilder.toString());
        log.info("生词预测参数: {}", params);
        String prompt = promptService.renderPrompt("word-prediction-v3", params);

        // 调用 AI
        return vocabPredictionAI.ask(prompt);
    }

}
