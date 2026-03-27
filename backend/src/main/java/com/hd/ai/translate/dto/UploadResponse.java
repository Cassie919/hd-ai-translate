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

    public UploadResponse(List<String> chapters) {
        this.chapters = chapters;
    }

}
