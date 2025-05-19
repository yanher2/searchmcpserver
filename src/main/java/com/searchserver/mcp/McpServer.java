package com.searchserver.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * MCP服务器的核心实现
 */
public class McpServer {
    private final ObjectMapper objectMapper;
    private final Map<String, McpTool> tools;
    private final Map<String, Function<JsonNode, CompletableFuture<JsonNode>>> methodHandlers;
    private final ServerInfo serverInfo;

    private BufferedReader input;
    private PrintWriter output;
    private boolean isRunning;

    /**
     * 创建一个新的MCP服务器
     * @param name 服务器名称
     * @param version 服务器版本
     */
    public McpServer(String name, String version) {
        this.objectMapper = new ObjectMapper();
        this.tools = new HashMap<>();
        this.methodHandlers = new HashMap<>();
        this.serverInfo = new ServerInfo(name, version);
        this.isRunning = false;

        // 注册标准方法处理程序
        registerMethodHandler("mcp.list_tools", this::handleListTools);
        registerMethodHandler("mcp.call_tool", this::handleCallTool);
    }

    /**
     * 注册一个工具
     * @param tool 要注册的工具
     */
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 注册一个方法处理程序
     * @param method 方法名称
     * @param handler 处理程序
     */
    public void registerMethodHandler(String method, Function<JsonNode, CompletableFuture<JsonNode>> handler) {
        methodHandlers.put(method, handler);
    }

    /**
     * 启动服务器，使用标准输入/输出进行通信
     */
    public void start() {
        this.input = new BufferedReader(new InputStreamReader(System.in));
        this.output = new PrintWriter(System.out, true);
        this.isRunning = true;

        System.err.println("MCP Server started: " + serverInfo.getName() + " v" + serverInfo.getVersion());

        try {
            processMessages();
        } catch (Exception e) {
            System.err.println("Error in MCP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理传入的消息
     */
    private void processMessages() throws IOException {
        String line;
        while (isRunning && (line = input.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                McpMessage request = objectMapper.readValue(line, McpMessage.class);
                if (request.isRequest()) {
                    handleRequest(request);
                } else {
                    System.err.println("Received non-request message: " + line);
                }
            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse message: " + e.getMessage());
                sendErrorResponse(null, McpError.parseError("Failed to parse message: " + e.getMessage()));
            }
        }
    }

    /**
     * 处理请求
     * @param request 请求消息
     */
    private void handleRequest(McpMessage request) {
        String method = request.getMethod();
        String id = request.getId();

        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        final String requestId = id;

        if (method == null) {
            sendErrorResponse(requestId, McpError.invalidRequest("Method is required"));
            return;
        }

        Function<JsonNode, CompletableFuture<JsonNode>> handler = methodHandlers.get(method);
        if (handler == null) {
            sendErrorResponse(requestId, McpError.methodNotFound("Method not found: " + method));
            return;
        }

        try {
            JsonNode params = objectMapper.valueToTree(request.getParams());
            handler.apply(params)
                .thenAccept(result -> sendSuccessResponse(requestId, result))
                .exceptionally(e -> {
                    sendErrorResponse(requestId, McpError.internalError("Error handling request: " + e.getMessage()));
                    return null;
                });
        } catch (Exception e) {
            sendErrorResponse(requestId, McpError.internalError("Error handling request: " + e.getMessage()));
        }
    }

    /**
     * 发送成功响应
     * @param id 请求ID
     * @param result 结果
     */
    private void sendSuccessResponse(String id, Object result) {
        McpMessage response = new McpMessage(id, result);
        try {
            String json = objectMapper.writeValueAsString(response);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize response: " + e.getMessage());
        }
    }

    /**
     * 发送错误响应
     * @param id 请求ID
     * @param error 错误
     */
    private void sendErrorResponse(String id, McpError error) {
        McpMessage response = new McpMessage(id, error);
        try {
            String json = objectMapper.writeValueAsString(response);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize error response: " + e.getMessage());
        }
    }

    /**
     * 处理列出工具的请求
     * @param params 参数
     * @return 工具列表
     */
    private CompletableFuture<JsonNode> handleListTools(JsonNode params) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", objectMapper.valueToTree(tools.values()));
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 处理调用工具的请求
     * @param params 参数
     * @return 工具调用结果
     */
    private CompletableFuture<JsonNode> handleCallTool(JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : null;
        if (toolName == null) {
            return CompletableFuture.failedFuture(
                new McpException(McpError.invalidParams("Tool name is required")));
        }

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return CompletableFuture.failedFuture(
                new McpException(McpError.methodNotFound("Tool not found: " + toolName)));
        }

        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        try {
            return tool.execute(arguments)
                .thenApply(result -> {
                    ObjectNode response = objectMapper.createObjectNode();
                    response.set("content", result);
                    return response;
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new McpException(McpError.internalError("Error executing tool: " + e.getMessage())));
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        this.isRunning = false;
        System.err.println("MCP Server stopped");
    }

    /**
     * 服务器信息类
     */
    private static class ServerInfo {
        private final String name;
        private final String version;

        public ServerInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    /**
     * MCP异常类
     */
    public static class McpException extends Exception {
        private final McpError error;

        public McpException(McpError error) {
            super(error.getMessage());
            this.error = error;
        }

        public McpError getError() {
            return error;
        }
    }
}
