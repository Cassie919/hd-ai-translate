package com.hd.ai.tools;

import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.dto.TranslateRequest;
import com.hd.ai.translate.service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TranslationTool {

    private final TranslateService translateService;

    public TranslationTool(TranslateService translateService) {
        this.translateService = translateService;
    }

    /**
     * 翻译任意文本内容，无需会话（无状态、不落 Redis）。
     */
    @Tool(description = "Translate arbitrary English/Chinese text content into the other language, paragraph by paragraph. Use this for one-off translation without a session. Input is plain text; output is the translated text.", returnDirect = false)
    public String translateText(
            @ToolParam(description = "Text content to translate") String content) {
        List<TranslateItem> items = translateService.translateText(content);
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(TranslateItem::getCn)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 翻译任意文本内容，返回英文/中文逐段对照（中文在英文下方），无需会话。
     */
    @Tool(description = "Translate arbitrary English/Chinese text content into the other language and return a side-by-side paragraph comparison: the original text on top and its translation directly below it. Use this for one-off translation without a session. Input is plain text.", returnDirect = false)
    public String translateWithComparison(
            @ToolParam(description = "Text content to translate") String content) {
        List<TranslateItem> items = translateService.translateText(content);
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            TranslateItem item = items.get(i);
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(item.getEn() == null ? "" : item.getEn());
            sb.append("\n");
            sb.append(item.getCn() == null ? "" : item.getCn());
        }
        return sb.toString();
    }


}
