package com.hd.ai.translate.service;

import com.hd.ai.translate.dto.ExportRequest;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ChapterService chapterService;

    public byte[] exportToWord(ExportRequest request) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {

            // 添加标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(request.getTitle() != null ? request.getTitle() : "翻译结果");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.addBreak();

            // 添加内容
            List<TranslateItem> content = request.getContent();
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

            // 输出到字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("导出Word文档失败", e);
            throw new IOException("导出Word文档失败: " + e.getMessage());
        }
    }
}
