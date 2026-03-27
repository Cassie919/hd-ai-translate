package com.hd.ai.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMeta {
    private String sessionId;
    private String fileName;
    private String title;
    private Integer totalChapters;
    private Integer currentChapterIndex;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
    private Long createdAt;
    private Long updatedAt;
}
