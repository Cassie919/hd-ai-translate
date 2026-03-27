package com.hd.ai.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private String title;
    private List<TranslateItem> content;
    private String sessionId;
}
