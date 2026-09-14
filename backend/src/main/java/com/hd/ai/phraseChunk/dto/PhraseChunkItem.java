package com.hd.ai.phraseChunk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhraseChunkItem {
    private Integer index;
    private String en;
    private String cn;
    private List<String> chunks;
}
