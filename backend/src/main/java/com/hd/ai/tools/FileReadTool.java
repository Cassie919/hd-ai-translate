package com.hd.ai.tools;

import com.hd.ai.file.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 文件读取工具：根据 objectKey 从 MinIO 下载并解析文件为纯文本。
 * <p>适用于：用户要求读取某个已上传文件的内容（TXT / PDF / Word）。
 * objectKey 由文件上传接口（{@code /api/file/upload}）返回。
 */
@Slf4j
@Component
public class FileReadTool {

    private final FileService fileService;

    public FileReadTool(FileService fileService) {
        this.fileService = fileService;
    }

    @Tool(description = "读取已上传文件的内容，返回纯文本。支持 TXT / PDF / Word（docx）格式。" +
            "适用于：用户要求查看或读取某个已上传文件的内容。" +
            "参数 objectKey 为文件上传接口返回的对象标识。", returnDirect = false)
    public String readFile(
            @ToolParam(description = "文件的 objectKey，由文件上传接口返回") String objectKey) {
        try {
            if (objectKey == null || objectKey.isBlank()) {
                throw new RuntimeException("缺少文件标识（objectKey）");
            }
            String text = fileService.extractText(objectKey);
            if (text == null || text.isBlank()) {
                return "文件内容为空。";
            }
            return text;
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }
}
