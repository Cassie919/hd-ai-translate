package com.hd.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * ToolCallback 日志代理，在工具真正执行时统一输出日志。
 */
@Slf4j
public class LoggingToolCallback implements ToolCallback {

    private final ToolCallback delegate;

    public LoggingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        log.info("【Spring AI 执行 {} Tool】", delegate.getToolDefinition().name());
        return delegate.call(toolInput);
    }
}
