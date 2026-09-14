package com.hd.ai.vocabPrediction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabWord {
    private String word;
    private String lemma;
    private String phonetic;
    private String sentence;
    private String translation;
    private Integer score;
}
