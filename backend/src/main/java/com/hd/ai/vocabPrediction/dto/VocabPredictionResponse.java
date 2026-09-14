package com.hd.ai.vocabPrediction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabPredictionResponse {
    private List<VocabWord> words;
}
