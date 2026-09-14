package com.hd.ai.tools;

import com.hd.ai.storage.service.StorageService;
import com.hd.ai.translate.dto.SessionData;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.service.SessionService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Component
public class PDFGenerationTool {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final StorageService storageService;
    private final SessionService sessionService;

    public PDFGenerationTool(StorageService storageService, SessionService sessionService) {
        this.storageService = storageService;
        this.sessionService = sessionService;
    }

    @Tool(description = "Generate a PDF from translation data by sessionId, upload it to storage, and return its downloadable URL. Use this when user wants to export translation content as PDF.", returnDirect = false)
    public String generatePdf(
            @ToolParam(description = "PDF file name (without extension), use the chapter title") String fileName,
            @ToolParam(description = "Translation session ID to look up content from Redis") String sessionId) {

        String safeName = (fileName != null && !fileName.isBlank())
                ? fileName.trim().replaceAll("[\\\\/:*?\"<>|]", "_")
                : "文档";
        String objectName = "export/pdf/" + safeName + "-" + System.currentTimeMillis() + ".pdf";

        // 从 Redis 查询翻译数据
        StringBuilder contentBuilder = new StringBuilder();
        if (sessionId != null && !sessionId.isBlank()) {
            SessionData sessionData = sessionService.getSessionData(sessionId);
            if (sessionData != null && sessionData.getTranslateItems() != null) {
                for (TranslateItem item : sessionData.getTranslateItems()) {
                    if (item.getEn() != null && !item.getEn().isBlank()) {
                        contentBuilder.append("【原文】").append(item.getEn()).append("\n");
                    }
                    if (item.getCn() != null && !item.getCn().isBlank()) {
                        contentBuilder.append("【译文】").append(item.getCn()).append("\n");
                    }
                    contentBuilder.append("\n");
                }
            }
        }
        String content = contentBuilder.toString();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);

        try (Document document = new Document(pdf)) {
            // 加载 NotoSansSC 字体
            PdfFont notoSansFont;
            try (InputStream fontStream = new ClassPathResource("fonts/NotoSansSC-6.ttf").getInputStream()) {
                byte[] fontBytes = fontStream.readAllBytes();
                notoSansFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
            }

            // 添加标题（文件名）
            Paragraph titleParagraph = new Paragraph();
            titleParagraph.setTextAlignment(TextAlignment.CENTER);
            titleParagraph.add(new Text(fileName != null ? fileName : "文档")
                    .setFont(notoSansFont)
                    .setFontSize(20));
            document.add(titleParagraph);

            // 添加空行
            document.add(new Paragraph(""));

            // 添加正文内容，按行拆分写入
            if (content != null && !content.isBlank()) {
                for (String line : content.split("\\r?\\n")) {
                    Paragraph paragraph = new Paragraph();
                    paragraph.add(new Text(line).setFont(notoSansFont).setFontSize(12));
                    document.add(paragraph);
                }
            }
        } catch (Exception e) {
            log.error("导出PDF文档失败", e);
            throw new RuntimeException("导出PDF文档失败: " + e.getMessage());
        }

        byte[] pdfBytes = outputStream.toByteArray();
        try {
            storageService.upload(objectName, new ByteArrayInputStream(pdfBytes), PDF_CONTENT_TYPE, pdfBytes.length);
            String url = storageService.getUrl(objectName);
            log.info("PDF 已生成: {}, 下载地址: {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("PDF 上传存储失败", e);
            throw new RuntimeException("PDF 上传存储失败: " + e.getMessage());
        }
    }
}
