package com.hd.ai.phraseChunk.controller;

import com.hd.ai.phraseChunk.dto.PhraseChunkRequest;
import com.hd.ai.phraseChunk.dto.PhraseChunkResponse;
import com.hd.ai.phraseChunk.dto.TextChunkRequest;
import com.hd.ai.phraseChunk.service.PhraseChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "意群划分接口", description = "对英文文本进行意群划分")
public class PhraseChunkController {

    private final PhraseChunkService phraseChunkService;

    @Operation(summary = "意群划分(翻译结果)", description = "对翻译结果中的英文原文进行意群划分")
    @PostMapping("/phrase-chunk")
    public ResponseEntity<PhraseChunkResponse> chunk(
            @Parameter(description = "意群划分请求参数") @RequestBody PhraseChunkRequest request) {
        try {
            PhraseChunkResponse result = phraseChunkService.chunk(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "意群划分(纯文本)", description = "对纯英文文本列表进行意群划分")
    @PostMapping("/phrase-chunk/text")
    public ResponseEntity<PhraseChunkResponse> chunkText(
            @Parameter(description = "意群划分文本请求参数") @RequestBody TextChunkRequest request) {
        try {
            PhraseChunkResponse result = phraseChunkService.chunkText(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
