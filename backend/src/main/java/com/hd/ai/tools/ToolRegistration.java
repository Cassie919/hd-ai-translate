package com.hd.ai.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 集中注册所有 AI Tool 工具类
 */
@Configuration
public class ToolRegistration {

    @Bean
    public ToolCallback[] allTools(PDFGenerationTool pdfGenerationTool,
                                   WORDGenerationTool wordGenerationTool,
                                   TranslationTool translationTool,
                                   PhraseChunkTool phraseChunkTool,
                                   MaiMemoTool maiMemoTool,
                                   VocabPredictionTool vocabPredictionTool,
                                   FileReadTool fileReadTool) {
        ToolCallback[] callbacks = ToolCallbacks.from(
                pdfGenerationTool,
                wordGenerationTool,
                translationTool,
                phraseChunkTool,
                maiMemoTool,
                vocabPredictionTool,
                fileReadTool
        );
        return Arrays.stream(callbacks)
                .map(LoggingToolCallback::new)
                .toArray(ToolCallback[]::new);
    }

    @Bean
    public ToolCallback[] enSkillTools(TranslationTool translationTool,
                                   PhraseChunkTool phraseChunkTool,
                                   MaiMemoTool maiMemoTool,
                                   VocabPredictionTool vocabPredictionTool) {
        ToolCallback[] callbacks = ToolCallbacks.from(

                translationTool,
                phraseChunkTool,
                maiMemoTool,
                vocabPredictionTool
        );
        return Arrays.stream(callbacks)
                .map(LoggingToolCallback::new)
                .toArray(ToolCallback[]::new);
    }
}
