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
    /** 源文件在对象存储中的对象名 */
    private String sourceObjectName;
    private Long createdAt;
    private Long updatedAt;
}
