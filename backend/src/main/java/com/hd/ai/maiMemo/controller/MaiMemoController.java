package com.hd.ai.maiMemo.controller;

import com.hd.ai.common.interceptor.UserContext;
import com.hd.ai.maiMemo.dto.AddSentenceRequest;
import com.hd.ai.maiMemo.dto.AddWordsRequest;
import com.hd.ai.maiMemo.dto.CreateNotepadRequest;
import com.hd.ai.maiMemo.dto.PushMaiMemoRequest;
import com.hd.ai.maiMemo.dto.SetTokenRequest;
import com.hd.ai.maiMemo.service.MaiMemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "推送墨墨接口", description = "推送单词到墨墨背单词云词本，添加例句")
public class MaiMemoController {

    private final MaiMemoService maiMemoService;

    @Operation(summary = "设置墨墨 Token", description = "设置用户的墨墨 API Token，首次使用必调")
    @PostMapping("/maiMemo/set-token")
    public ResponseEntity<Map<String, Object>> setToken(
            @Parameter(description = "token: 墨墨 API Token")
            @RequestBody SetTokenRequest request) {
        try {
            String userId = UserContext.getUserId();
            Map<String, Object> result = maiMemoService.setToken(userId, request.getToken());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("setToken error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "查询墨墨 Token 配置状态", description = "查询当前用户是否已配置墨墨 API Token")
    @GetMapping("/maiMemo/token-status")
    public ResponseEntity<Map<String, Object>> tokenStatus() {
        try {
            String userId = UserContext.getUserId();
            Map<String, Object> result = maiMemoService.getTokenStatus(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("tokenStatus error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "创建云词本", description = "创建新的墨墨云词本并写入单词，返回 notepadId")
    @PostMapping("/maiMemo/create-notepad")
    public ResponseEntity<Map<String, Object>> createNotepad(
            @Parameter(description = "words: 要写入的单词列表")
            @RequestBody CreateNotepadRequest request) {
        try {
            String userId = UserContext.getUserId();
            Map<String, Object> result = maiMemoService.createNotepad(userId, request.getWords());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("createNotepad error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "追加单词", description = "向已有云词本追加单词，自动去重")
    @PostMapping("/maiMemo/add-words")
    public ResponseEntity<Map<String, Object>> addWords(
            @Parameter(description = "words: 要追加的单词列表")
            @RequestBody AddWordsRequest request) {
        try {
            String userId = UserContext.getUserId();
            Map<String, Object> result = maiMemoService.addWords(userId, request.getWords());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("addWords error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "添加例句", description = "为单词添加例句及中文翻译")
    @PostMapping("/maiMemo/add-sentence")
    public ResponseEntity<Map<String, Object>> addSentence(
            @Parameter(description = "word: 单词, sentence: 例句, translation: 译文")
            @RequestBody AddSentenceRequest request) {
        try {
            String userId = UserContext.getUserId();
            Map<String, Object> result = maiMemoService.addSentence(
                    userId, request.getWord(),
                    request.getSentence(), request.getTranslation());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("addSentence error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "推送单词和例句", description = "将单词列表加到生词本，然后逐个添加例句")
    @PostMapping("/maiMemo/push")
    public ResponseEntity<Map<String, Object>> push(
            @Parameter(description = "items: 单词列表，每个元素包含 word/sentence/translation")
            @RequestBody PushMaiMemoRequest request) {
        try {
            String userId = UserContext.getUserId();
            List<Map<String, String>> items = request.getItems().stream()
                    .map(item -> {
                        Map<String, String> m = new java.util.HashMap<>();
                        m.put("word", item.getWord());
                        m.put("sentence", item.getSentence());
                        m.put("translation", item.getTranslation());
                        return m;
                    })
                    .collect(java.util.stream.Collectors.toList());
            Map<String, Object> result = maiMemoService.pushToMaiMemo(userId, items);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("push error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
