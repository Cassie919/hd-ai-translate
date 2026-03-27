package com.hd.ai.translate.controller;

import com.hd.ai.translate.dto.*;
import com.hd.ai.translate.service.ChapterService;
import com.hd.ai.translate.service.ExportService;
import com.hd.ai.translate.service.SessionService;
import com.hd.ai.translate.service.TranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "翻译接口", description = "提供文件上传、翻译和导出功能")
public class TranslateController {

    private final ChapterService chapterService;
    private final TranslateService translateService;
    private final SessionService sessionService;
    private final ExportService exportService;

    @Operation(summary = "上传文件", description = "上传PDF或其他格式文件进行翻译处理")
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file) {
        try {
            List<String> chapters = chapterService.parseChapters(file);
            String sessionId = sessionService.createSession(file.getOriginalFilename(), chapters);

            return ResponseEntity.ok(new UploadResponse(sessionId, file.getOriginalFilename(), chapters));
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
            List<TranslateItem> result = translateService.translateChapter(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
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

    @Operation(summary = "导出为Word", description = "将翻译结果导出为Word文档")
    @PostMapping("/export/word")
    public ResponseEntity<byte[]> exportToWord(
            @Parameter(description = "导出请求参数") @RequestBody ExportRequest request) {
        try {
            byte[] wordBytes = exportService.exportToWord(request);

            // 设置文件名
            String fileName = request.getTitle() != null ? request.getTitle() : "翻译结果";
            String encodedFileName = java.net.URLEncoder.encode(fileName + ".docx", StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(wordBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
