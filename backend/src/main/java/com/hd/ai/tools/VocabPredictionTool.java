package com.hd.ai.tools;

import com.hd.ai.vocabPrediction.dto.VocabPredictionRequest;
import com.hd.ai.vocabPrediction.dto.VocabPredictionResponse;
import com.hd.ai.vocabPrediction.dto.VocabWord;
import com.hd.ai.vocabPrediction.service.VocabPredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生词预测工具：对英文文本进行生词预测，筛选值得学习的词汇。
 * <p>适用于：用户给出一段英文、章节或文章，希望挑出生词来学习（含音标、释义、例句、重要度评分）。
 * userId 取自 {@code UserContext}（由请求拦截器在同一 HTTP 线程中设置），本工具不依赖 userId。
 */
@Slf4j
@Component
public class VocabPredictionTool {

    private final VocabPredictionService vocabPredictionService;

    public VocabPredictionTool(VocabPredictionService vocabPredictionService) {
        this.vocabPredictionService = vocabPredictionService;
    }

    @Tool(description = "对英文文本进行生词预测，筛选值得学习的词汇（含音标、释义、例句、重要度评分）。" +
            "适用于：用户给出一段英文、章节或文章，希望挑出生词来学习。" +
            "参数 text 为待分析的英文文本。", returnDirect = false)
    public String predictVocab(
            @ToolParam(description = "待分析的英文文本，例如一段文章或章节内容") String text) {
        try {
            if (text == null || text.isBlank()) {
                throw new RuntimeException("文本不能为空");
            }
            VocabPredictionResponse response =
                    vocabPredictionService.predict(new VocabPredictionRequest(text));
            List<VocabWord> words = response.getWords();
            if (words == null || words.isEmpty()) {
                return "未预测到值得学习的生词。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共预测到 ").append(words.size()).append(" 个生词：\n");
            for (int i = 0; i < words.size(); i++) {
                VocabWord w = words.get(i);
                sb.append(i + 1).append(". ").append(w.getWord());
                if (w.getLemma() != null && !w.getLemma().equals(w.getWord())) {
                    sb.append(" (原形: ").append(w.getLemma()).append(")");
                }
                if (w.getPhonetic() != null && !w.getPhonetic().isBlank()) {
                    sb.append("  [").append(w.getPhonetic()).append("]");
                }
                if (w.getScore() != null) {
                    sb.append("  重要度: ").append(w.getScore());
                }
                sb.append("\n");
                if (w.getTranslation() != null && !w.getTranslation().isBlank()) {
                    sb.append("   释义: ").append(w.getTranslation()).append("\n");
                }
                if (w.getSentence() != null && !w.getSentence().isBlank()) {
                    sb.append("   例句: ").append(w.getSentence()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("生词预测失败", e);
            throw new RuntimeException("生词预测失败: " + e.getMessage());
        }
    }
}
