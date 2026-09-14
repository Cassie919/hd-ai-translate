package com.hd.ai.tools;

import com.hd.ai.storage.service.StorageService;
import com.hd.ai.translate.dto.SessionData;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Component
public class WORDGenerationTool {

    private static final String WORD_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final SessionService sessionService;
    private final StorageService storageService;

    public WORDGenerationTool(SessionService sessionService, StorageService storageService) {
        this.sessionService = sessionService;
        this.storageService = storageService;
    }

    @Tool(description = "Generate a Word (.docx) from translation data by sessionId, upload it to storage, and return its downloadable URL. Use this when user wants to export translation content as Word.", returnDirect = false)
    public String generateWord(
            @ToolParam(description = "Document title, use the chapter title") String title,
            @ToolParam(description = "Translation session ID to look up content from Redis") String sessionId) {

        // 从 Redis 查询翻译数据
        List<TranslateItem> content = null;
        String safeTitle = (title != null && !title.isBlank()) ? title : "翻译结果";
        String safeName = safeTitle.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        String objectName = "export/word/" + safeName + "-" + System.currentTimeMillis() + ".docx";

        if (sessionId != null && !sessionId.isBlank()) {
            SessionData sessionData = sessionService.getSessionData(sessionId);
            if (sessionData != null) {
                content = sessionData.getTranslateItems();
            }
        }

        try (XWPFDocument document = new XWPFDocument()) {

            // 添加标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(safeTitle);
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.addBreak();

            // 添加内容
            if (content != null && !content.isEmpty()) {
                for (TranslateItem item : content) {
                    // 原文段落
                    if (item.getEn() != null && !item.getEn().isEmpty()) {
                        XWPFParagraph enParagraph = document.createParagraph();
                        XWPFRun enRun = enParagraph.createRun();
                        enRun.setBold(true);
                        enRun.setFontSize(12);
                        enRun = enParagraph.createRun();
                        enRun.setText(item.getEn());
                        enRun.setFontSize(12);
                        enRun.addBreak();
                    }

                    // 译文段落
                    if (item.getCn() != null && !item.getCn().isEmpty()) {
                        XWPFParagraph cnParagraph = document.createParagraph();
                        XWPFRun cnRun = cnParagraph.createRun();
                        cnRun.setBold(true);
                        cnRun.setFontSize(12);
                        cnRun = cnParagraph.createRun();
                        cnRun.setText(item.getCn());
                        cnRun.setFontSize(12);
                        cnRun.addBreak();
                    }

                    // 添加空行分隔
                    document.createParagraph();
                }
            }

            // 输出到字节数组并上传
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            byte[] wordBytes = outputStream.toByteArray();
            storageService.upload(objectName, new ByteArrayInputStream(wordBytes), WORD_CONTENT_TYPE, wordBytes.length);
            String url = storageService.getUrl(objectName);
            log.info("Word 已生成: {}, 下载地址: {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("导出Word文档失败", e);
            throw new RuntimeException("导出Word文档失败: " + e.getMessage());
        }
    }
}
