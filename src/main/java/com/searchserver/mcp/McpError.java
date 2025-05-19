package com.searchserver.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 表示MCP协议中的错误
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {
    private int code;
    private String message;
    private Object data;

    // 错误代码常量
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // 默认构造函数
    public McpError() {
    }

    // 带参数构造函数
    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // 带数据的构造函数
    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // 常用错误创建方法
    public static McpError parseError(String message) {
        return new McpError(PARSE_ERROR, message != null ? message : "Parse error");
    }

    public static McpError invalidRequest(String message) {
        return new McpError(INVALID_REQUEST, message != null ? message : "Invalid request");
    }

    public static McpError methodNotFound(String message) {
        return new McpError(METHOD_NOT_FOUND, message != null ? message : "Method not found");
    }

    public static McpError invalidParams(String message) {
        return new McpError(INVALID_PARAMS, message != null ? message : "Invalid params");
    }

    public static McpError internalError(String message) {
        return new McpError(INTERNAL_ERROR, message != null ? message : "Internal error");
    }
}
