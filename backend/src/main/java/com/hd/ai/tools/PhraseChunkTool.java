package com.hd.ai.tools;

import com.hd.ai.phraseChunk.dto.PhraseChunkItem;
import com.hd.ai.phraseChunk.dto.PhraseChunkResponse;
import com.hd.ai.phraseChunk.service.PhraseChunkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 意群划分工具（纯文本）。
 * <p>适用于：用户希望把一段/多篇英文文本按意群切分，便于对照阅读或学习。
 * 输入为纯英文文本（可含多段，按换行拆分），输出为带分隔符的可读纯文本（每条含原文、意群切分、译文）。
 */
@Slf4j
@Component
public class PhraseChunkTool {

    private final PhraseChunkService phraseChunkService;

    public PhraseChunkTool(PhraseChunkService phraseChunkService) {
        this.phraseChunkService = phraseChunkService;
    }

    @Tool(description = "对纯英文文本进行意群划分，返回每条文本的意群切分结果（含原文、意群、译文）。" +
            "适用于用户希望按意群拆分英文、对照阅读或学习的场景。输入是英文文本（可含多段，按换行拆分），输出为带分隔符的可读纯文本。",
            returnDirect = false)
    public String chunkText(
            @ToolParam(description = "英文文本（可含多段，按换行拆分）") String text) {
        PhraseChunkResponse response = phraseChunkService.chunkText(text);
        List<PhraseChunkItem> items = response.getItems();
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(PhraseChunkTool::formatItem)
                .collect(Collectors.joining("\n\n"));
    }

    private static String formatItem(PhraseChunkItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getIndex()).append(". ");
        sb.append("[原文] ").append(item.getEn() == null ? "" : item.getEn()).append("\n");
        List<String> chunks = item.getChunks();
        if (chunks != null && !chunks.isEmpty()) {
            sb.append("  [意群] ").append(String.join(" | ", chunks)).append("\n");
        }
        if (item.getCn() != null && !item.getCn().isBlank()) {
            sb.append("  [译文] ").append(item.getCn());
        }
        return sb.toString();
    }
}
