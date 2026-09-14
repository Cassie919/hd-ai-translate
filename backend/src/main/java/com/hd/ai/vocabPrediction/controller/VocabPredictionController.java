package com.hd.ai.vocabPrediction.controller;

import com.hd.ai.vocabPrediction.dto.VocabPredictionRequest;
import com.hd.ai.vocabPrediction.dto.VocabPredictionResponse;
import com.hd.ai.vocabPrediction.service.VocabPredictionService;
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
@Tag(name = "生词预测接口", description = "对英文文本进行生词预测，筛选值得学习的词汇")
public class VocabPredictionController {

    private final VocabPredictionService vocabPredictionService;

    @Operation(summary = "生词预测", description = "对章节英文文本进行生词预测，返回值得学习的词汇列表")
    @PostMapping("/vocab-prediction")
    public ResponseEntity<VocabPredictionResponse> predict(
            @Parameter(description = "生词预测请求参数") @RequestBody VocabPredictionRequest request) {
        try {
            VocabPredictionResponse result = vocabPredictionService.predict(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
