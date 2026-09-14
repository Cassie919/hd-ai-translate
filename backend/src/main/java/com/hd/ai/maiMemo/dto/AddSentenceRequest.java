package com.hd.ai.maiMemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddSentenceRequest {
    private String word;
    private String sentence;
    private String translation;
}
