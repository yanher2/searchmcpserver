package com.searchserver.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 表示MCP协议消息的基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {
    @JsonProperty("jsonrpc")
    private final String jsonrpc = "2.0";

    @JsonProperty("id")
    private String id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    // 默认构造函数
    public McpMessage() {
    }

    // 请求构造函数
    public McpMessage(String id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // 成功响应构造函数
    public McpMessage(String id, Object result) {
        this.id = id;
        this.result = result;
    }

    // 错误响应构造函数
    public McpMessage(String id, McpError error) {
        this.id = id;
        this.error = error;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }

    public boolean isRequest() {
        return method != null;
    }

    public boolean isResponse() {
        return result != null || error != null;
    }
}
