package com.hd.ai.translate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChapterService {

    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^第[0-9一二三四五六七八九十百千]+章.*");

    private String fileContent;
    private List<String> chapters;

    public List<String> parseChapters(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            chapters = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
                if (CHAPTER_PATTERN.matcher(line.trim()).matches()) {
                    chapters.add(line.trim());
                }
            }

            fileContent = content.toString();
            //log.info("fileContent: {}", fileContent);
            if (chapters.isEmpty()) {
                chapters.add("默认章节");
            }

            return chapters;
        }
    }

    public List<String> getCachedChapters() {
        return chapters != null ? new ArrayList<>(chapters) : new ArrayList<>();
    }

    public String getChapterContent(String chapterTitle) {
        if (fileContent == null) {
            return "";
        }
        return extractChapterContent(fileContent, chapterTitle);
    }

    /**
     * 从给定的全文内容中提取指定章节内容，不依赖成员变量缓存。
     */
    public String extractChapterContent(String fullContent, String chapterTitle) {
        if (fullContent == null) {
            return "";
        }

        if ("默认章节".equals(chapterTitle)) {
            return fullContent.trim();
        }

        String[] lines = fullContent.split("\n");
        StringBuilder content = new StringBuilder();
        boolean inChapter = false;

        for (String line : lines) {
            if (line.trim().equals(chapterTitle)) {
                inChapter = true;
                continue;
            }
            if (inChapter) {
                if (CHAPTER_PATTERN.matcher(line.trim()).matches()) {
                    break;
                }
                content.append(line).append("\n");
            }
        }
        log.info("content: {}", content);
        return content.toString().trim();
    }

    public List<String> splitIntoParagraphs(String content) {
        String[] lines = content.split("\n");
        List<String> paragraphs = new ArrayList<>();

        for (String line : lines) {
            // 只去掉行首的全角空格，保留普通空格（缩进）
            String trimmedLine = line.replaceAll("^[\\u3000]+", "").trim();
            if (!trimmedLine.isEmpty()) {
                paragraphs.add(trimmedLine);
            }
        }
        log.info("paragraphs: {}", paragraphs);
        return paragraphs;
    }
}
