package com.hd.ai.phraseChunk.dto;

import com.hd.ai.translate.dto.TranslateItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhraseChunkRequest {
    private List<TranslateItem> items;
}
