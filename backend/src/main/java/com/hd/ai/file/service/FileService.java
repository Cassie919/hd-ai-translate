package com.hd.ai.file.service;

import com.hd.ai.storage.service.StorageService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 文件服务：
 * 1. 上传文件并保存到 MinIO
 * 2. 从 MinIO 下载并解析文件内容（TXT / PDF / Word）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;

    /**
     * 上传文件到 MinIO，返回 objectKey。
     */
    public String upload(MultipartFile file, String objectKey) throws Exception {
        return storageService.upload(objectKey, file);
    }

    /**
     * 根据 objectKey 下载并解析文件为纯文本。
     */
    public String extractText(String objectKey) throws Exception {
        // 1. 查询 Redis/DB 获取 objectKey（当前直接使用入参 objectKey）
        // 2. 从 MinIO 下载
        try (InputStream in = storageService.download(objectKey)) {
            // 3. 解析 TXT/PDF/Word
            String ext = extensionOf(objectKey);
            return switch (ext) {
                case "txt"  -> readText(in);
                case "pdf"  -> readPdf(in);
                case "docx" -> readDocx(in);
                default -> throw new IllegalArgumentException("不支持的文件类型: " + ext);
            };
        }
    }

    private String extensionOf(String objectKey) {
        int i = objectKey.lastIndexOf('.');
        if (i < 0) {
            throw new IllegalArgumentException("无法识别文件类型: " + objectKey);
        }
        return objectKey.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private String readText(InputStream in) throws Exception {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String readPdf(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(in);
             PdfDocument pdf = new PdfDocument(reader)) {
            int pages = pdf.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                sb.append(PdfTextExtractor.getTextFromPage(pdf.getPage(i)));
                if (i < pages) {
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private String readDocx(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append('\n'));
        }
        return sb.toString();
    }
}
