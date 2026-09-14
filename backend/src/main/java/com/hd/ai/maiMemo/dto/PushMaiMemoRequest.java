package com.hd.ai.maiMemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushMaiMemoRequest {
    private List<WordItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordItem {
        private String word;
        private String sentence;
        private String translation;
    }
}
