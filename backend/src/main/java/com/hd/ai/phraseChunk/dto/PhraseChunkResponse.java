package com.hd.ai.phraseChunk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhraseChunkResponse {
    private List<PhraseChunkItem> items;
}
