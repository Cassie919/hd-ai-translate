package com.hd.ai.translate.controller;

import com.hd.ai.common.interceptor.UserContext;
import com.hd.ai.storage.service.StorageService;
import com.hd.ai.tools.PDFGenerationTool;
import com.hd.ai.translate.dto.*;
import com.hd.ai.translate.service.ChapterService;
import com.hd.ai.translate.service.SessionService;
import com.hd.ai.translate.service.TranslateService;
import com.hd.ai.maiMemo.service.MaiMemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "翻译接口", description = "提供文件上传、翻译和导出功能")
public class TranslateController {

    private final ChapterService chapterService;
    private final TranslateService translateService;
    private final SessionService sessionService;
    private final MaiMemoService maiMemoService;
    private final StorageService storageService;
    private final PDFGenerationTool pdfGenerationTool;

    @Operation(summary = "上传文件", description = "上传PDF或其他格式文件进行翻译处理，并持久化存储源文件")
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file) {
        try {
            List<String> chapters = chapterService.parseChapters(file);
            String sessionId = sessionService.createSession(file.getOriginalFilename(), chapters);

            // 持久化存储上传的源文件（分类前缀 upload/）
            String objectName = "upload/" + sessionId + "/" + file.getOriginalFilename();
            storageService.upload(objectName, file);
            String url = storageService.getUrl(objectName);

            // 记录源文件存储位置到会话
            SessionMeta meta = sessionService.getSessionMeta(sessionId);
            if (meta != null) {
                meta.setSourceObjectName(objectName);
                sessionService.updateSessionMeta(sessionId, meta);
            }

            UploadResponse response = new UploadResponse();
            response.setSessionId(sessionId);
            response.setFileName(file.getOriginalFilename());
            response.setChapters(chapters);
            response.setFileObjectName(objectName);
            response.setFileUrl(url);
            log.info("response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "翻译章节", description = "根据请求参数翻译指定章节")
    @PostMapping("/translate")
    public ResponseEntity<List<TranslateItem>> translateChapter(
            @Parameter(description = "翻译请求参数") @RequestBody TranslateRequest request) {
        if (request.getSessionId() == null || !sessionService.sessionExists(request.getSessionId())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<TranslateItem> result = translateService.translateByParagraph(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "翻译文本内容", description = "根据用户传入的文本内容按段落翻译，无需会话")
    @PostMapping("/translate/text")
    public ResponseEntity<List<TranslateItem>> translateText(
            @Parameter(description = "待翻译的文本内容") @RequestBody TranslateTextRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<TranslateItem> result = translateService.translateText(request.getContent());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取章节内容", description = "从 MinIO 源文件重新解析并获取指定章节原始内容")
    @GetMapping("/chapter/content")
    public ResponseEntity<Map<String, String>> getChapterContent(
            @Parameter(description = "会话ID") @RequestParam String sessionId,
            @Parameter(description = "章节标题") @RequestParam String chapterTitle) {
        try {
            SessionMeta meta = sessionService.getSessionMeta(sessionId);
            if (meta == null || meta.getSourceObjectName() == null) {
                return ResponseEntity.badRequest().build();
            }

            String fullContent;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(storageService.download(meta.getSourceObjectName()), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                fullContent = sb.toString();
            }

            String content = chapterService.extractChapterContent(fullContent, chapterTitle);
            return ResponseEntity.ok(Map.of("chapterTitle", chapterTitle, "content", content));
        } catch (Exception e) {
            log.error("获取章节内容失败: sessionId={}, chapterTitle={}", sessionId, chapterTitle, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取单词例句", description = "从翻译会话的逐句译文里，为指定单词匹配英文原句作为例句、中文作为翻译")
    @PostMapping("/word/examples")
    public ResponseEntity<Map<String, Object>> getWordExamples(
            @Parameter(description = "请求参数：sessionId 与单词列表") @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        @SuppressWarnings("unchecked")
        List<String> words = (List<String>) request.get("words");
        if (words == null || words.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Map<String, Object> response = maiMemoService.buildWordItemsFromSession(sessionId, words);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取单词例句失败: sessionId={}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "单词词性还原", description = "将单词列表还原为原型，输出顺序与输入一致")
    @PostMapping("/word/lemmatize")
    public ResponseEntity<Map<String, Object>> lemmatizeWords(
            @Parameter(description = "words: 待还原的单词列表") @RequestBody LemmatizeRequest request) {
        if (request.getWords() == null || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            List<String> lemmas = translateService.lemmatizeWords(request.getWords());
            return ResponseEntity.ok(Map.of("lemmas", lemmas));
        } catch (Exception e) {
            log.error("词性还原失败: words={}", request.getWords(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取会话信息", description = "根据会话ID获取会话详细信息")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        SessionResponse session = sessionService.getSession(sessionId);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "重启会话", description = "重新启动会话并返回新的会话信息")
    @PostMapping("/session/restart")
    public ResponseEntity<SessionMeta> restartSession(
            @Parameter(description = "重启会话请求参数") @RequestBody Map<String, String> request) {
        try {
            String oldSessionId = request.get("sessionId");
            String fileName = request.get("fileName");

            if (oldSessionId != null) {
                sessionService.deleteSession(oldSessionId);
            }

            List<String> chapters = chapterService.getCachedChapters();
            String newSessionId = sessionService.createSession(fileName, chapters);

            SessionMeta meta = sessionService.getSessionMeta(newSessionId);
            return ResponseEntity.ok(meta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "清空会话数据", description = "清空指定会话的所有数据")
    @PostMapping("/session/clear")
    public ResponseEntity<Map<String, String>> clearSession(
            @Parameter(description = "清空会话请求参数") @RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null) {
                return ResponseEntity.badRequest().build();
            }

            sessionService.deleteSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "会话已清空", "sessionId", sessionId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取当前用户信息", description = "返回后端自动分配的用户标识")
    @GetMapping("/user/info")
    public ResponseEntity<Map<String, String>> getUserInfo() {
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(Map.of("userId", userId != null ? userId : ""));
    }

    @Operation(summary = "导出PDF", description = "将会话中已翻译的内容（原文+译文）生成PDF并上传，返回下载URL（测试用途）")
    @PostMapping("/export/pdf")
    public ResponseEntity<Map<String, String>> exportPdf(

            @Parameter(description = "导出文件名（可选，不传则用会话标题）") @RequestParam(required = false) String fileName) {


        try {

           String safeName = "文档";
           String content = "测试\ntest";
            String url = pdfGenerationTool.generatePdf(safeName, content);
            log.info("url: {}",url);
            return ResponseEntity.ok(Map.of("url", url, "fileName", safeName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
