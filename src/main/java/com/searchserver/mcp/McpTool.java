package com.searchserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;

/**
 * MCP工具的接口定义
 */
public interface McpTool {
    /**
     * 获取工具名称
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取工具的输入模式（JSON Schema）
     * @return 输入模式的JSON表示
     */
    JsonNode getInputSchema();

    /**
     * 执行工具
     * @param arguments 输入参数
     * @return 执行结果
     */
    CompletableFuture<JsonNode> execute(JsonNode arguments);
}
