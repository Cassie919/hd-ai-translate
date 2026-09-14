package com.hd.ai.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String sessionId;
    private String fileName;
    private List<String> chapters;
    /** 源文件在对象存储中的对象名 */
    private String fileObjectName;
    /** 源文件可访问的预签名 URL */
    private String fileUrl;

    public UploadResponse(List<String> chapters) {
        this.chapters = chapters;
    }

}
