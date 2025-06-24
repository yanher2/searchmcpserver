/*
package com.searchserver.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchserver.model.LaptopInfo;
import com.searchserver.service.LaptopSearchService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

*/
/**
 * MCP服务器的核心实现，通过标准输入输出与客户端通信
 *//*

@Component
public class StdioMcpServer {
    @Resource
    private LaptopSearchService laptopSearchService;
    
    @Resource
    private ObjectMapper objectMapper;
    
    private final Map<String, Map<String, Object>> tools;
    private final Map<String, Function<JsonNode, CompletableFuture<JsonNode>>> methodHandlers;
    private final ServerInfo serverInfo;

    private BufferedReader input;
    private PrintWriter output;
    private boolean isRunning;
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(StdioMcpServer.class);
    */
/**
     * 创建一个新的MCP服务器
     *//*

    public StdioMcpServer() {
        this.tools = new HashMap<>();
        this.methodHandlers = new HashMap<>();
        this.serverInfo = new ServerInfo("laptop-search-mcp-server", "1.0.0");
        this.isRunning = false;

        // 注册标准方法处理程序
        registerMethodHandler("mcp.list_tools", this::handleListTools);
        registerMethodHandler("mcp.call_tool", this::handleCallTool);
    }

    @PostConstruct
    public void init() {
        // 注册笔记本电脑搜索工具
        registerTool(
            "search_laptops",
            "搜索京东二手笔记本电脑",
            createSearchLaptopsSchema(),
            this::handleSearchLaptops
        );

        registerTool(
            "find_similar_laptops",
            "查找相似的京东二手笔记本电脑",
            createFindSimilarLaptopsSchema(),
            this::handleFindSimilarLaptops
        );

        registerTool(
            "get_laptop_by_id",
            "根据产品ID获取京东二手笔记本电脑详情",
            createGetLaptopByIdSchema(),
            this::handleGetLaptopById
        );

        registerTool(
            "refresh_laptop_data",
            "刷新京东二手笔记本电脑数据",
            createRefreshLaptopDataSchema(),
            this::handleRefreshLaptopData
        );

        log.info("Laptop search tools registered successfully");

        start();
    }

    */
/**
     * 注册一个工具
     * @param name 工具名称
     * @param description 工具描述
     * @param inputSchema 输入参数Schema
     * @param handler 处理函数
     *//*

    public void registerTool(String name, String description, JsonNode inputSchema, 
                            Function<JsonNode, CompletableFuture<JsonNode>> handler) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        tool.put("handler", handler);
        tools.put(name, tool);
    }

    */
/**
     * 注册一个方法处理程序
     * @param method 方法名称
     * @param handler 处理程序
     *//*

    public void registerMethodHandler(String method, Function<JsonNode, CompletableFuture<JsonNode>> handler) {
        methodHandlers.put(method, handler);
    }

    */
/**
     * 启动服务器，使用标准输入/输出进行通信
     *//*

    public void start() {
        this.input = new BufferedReader(new InputStreamReader(System.in));
        this.output = new PrintWriter(System.out, true);
        this.isRunning = true;

        System.out.println("MCP Server started: " + serverInfo.getName() + " v" + serverInfo.getVersion());

        try {
            processMessages();
        } catch (Exception e) {
            System.err.println("Error in MCP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    */
/**
     * 处理传入的消息
     *//*

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
                    System.out.println("Received non-request message: " + line);
                }
            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse message: " + e.getMessage());
                sendErrorResponse(null, McpError.parseError("Failed to parse message: " + e.getMessage()));
            }
        }
    }

    */
/**
     * 处理请求
     * @param request 请求消息
     *//*

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

    */
/**
     * 发送成功响应
     * @param id 请求ID
     * @param result 结果
     *//*

    private void sendSuccessResponse(String id, Object result) {
        McpMessage response = new McpMessage(id, result);
        try {
            String json = objectMapper.writeValueAsString(response);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize response: " + e.getMessage());
        }
    }

    */
/**
     * 发送错误响应
     * @param id 请求ID
     * @param error 错误
     *//*

    private void sendErrorResponse(String id, McpError error) {
        McpMessage response = new McpMessage(id, error);
        try {
            String json = objectMapper.writeValueAsString(response);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize error response: " + e.getMessage());
        }
    }

    */
/**
     * 处理列出工具的请求
     * @param params 参数
     * @return 工具列表
     *//*

    private CompletableFuture<JsonNode> handleListTools(JsonNode params) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", objectMapper.valueToTree(tools.values()));
        return CompletableFuture.completedFuture(result);
    }

    */
/**
     * 处理调用工具的请求
     * @param params 参数
     * @return 工具调用结果
     *//*

    private CompletableFuture<JsonNode> handleCallTool(JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : null;
        if (toolName == null) {
            return CompletableFuture.failedFuture(
                new McpException(McpError.invalidParams("Tool name is required")));
        }

        Map<String, Object> tool = tools.get(toolName);
        if (tool == null) {
            return CompletableFuture.failedFuture(
                new McpException(McpError.methodNotFound("Tool not found: " + toolName)));
        }

        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        try {
            @SuppressWarnings("unchecked")
            Function<JsonNode, CompletableFuture<JsonNode>> handler = 
                (Function<JsonNode, CompletableFuture<JsonNode>>) tool.get("handler");
            
            return handler.apply(arguments)
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

    */
/**
     * 创建搜索笔记本电脑的Schema
     *//*

    private JsonNode createSearchLaptopsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode keyword = objectMapper.createObjectNode();
        keyword.put("type", "string");
        keyword.put("description", "搜索关键词");
        properties.set("keyword", keyword);

        ObjectNode minPrice = objectMapper.createObjectNode();
        minPrice.put("type", "number");
        minPrice.put("description", "最低价格");
        properties.set("minPrice", minPrice);

        ObjectNode maxPrice = objectMapper.createObjectNode();
        maxPrice.put("type", "number");
        maxPrice.put("description", "最高价格");
        properties.set("maxPrice", maxPrice);

        schema.set("properties", properties);
        return schema;
    }

    */
/**
     * 创建查找相似笔记本电脑的Schema
     *//*

    private JsonNode createFindSimilarLaptopsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode description = objectMapper.createObjectNode();
        description.put("type", "string");
        description.put("description", "描述或需求");
        properties.set("description", description);

        ObjectNode laptopId = objectMapper.createObjectNode();
        laptopId.put("type", "number");
        laptopId.put("description", "笔记本电脑ID");
        properties.set("laptopId", laptopId);

        ObjectNode limit = objectMapper.createObjectNode();
        limit.put("type", "number");
        limit.put("description", "返回结果数量限制");
        properties.set("limit", limit);

        schema.set("properties", properties);
        return schema;
    }

    */
/**
     * 创建根据ID获取笔记本电脑的Schema
     *//*

    private JsonNode createGetLaptopByIdSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode productId = objectMapper.createObjectNode();
        productId.put("type", "string");
        productId.put("description", "产品ID");
        properties.set("productId", productId);

        schema.set("properties", properties);
        schema.set("required", objectMapper.createArrayNode().add("productId"));
        return schema;
    }

    */
/**
     * 创建刷新笔记本电脑数据的Schema
     *//*

    private JsonNode createRefreshLaptopDataSchema() {
        return objectMapper.createObjectNode().put("type", "object");
    }

    */
/**
     * 处理搜索笔记本电脑的请求
     *//*

    private CompletableFuture<JsonNode> handleSearchLaptops(JsonNode args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String keyword = args.has("keyword") ? args.get("keyword").asText() : null;
                Double minPrice = args.has("minPrice") ? args.get("minPrice").asDouble() : null;
                Double maxPrice = args.has("maxPrice") ? args.get("maxPrice").asDouble() : null;

                List<LaptopInfo> results;
                if (keyword != null && !keyword.trim().isEmpty()) {
                    results = laptopSearchService.searchByKeyword(keyword);
                } else if (minPrice != null && maxPrice != null) {
                    results = laptopSearchService.searchByPriceRange(
                        BigDecimal.valueOf(minPrice),
                        BigDecimal.valueOf(maxPrice)
                    );
                } else {
                    throw new IllegalArgumentException("必须提供关键词或价格范围");
                }

                return objectMapper.valueToTree(convertLaptopsToMap(results));
            } catch (Exception e) {
                log.error("Error handling search laptops", e);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", e.getMessage());
                return error;
            }
        });
    }

    */
/**
     * 处理查找相似笔记本电脑的请求
     *//*

    private CompletableFuture<JsonNode> handleFindSimilarLaptops(JsonNode args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String description = args.has("description") ? args.get("description").asText() : null;
                Long laptopId = args.has("laptopId") ? args.get("laptopId").asLong() : null;
                int limit = args.has("limit") ? args.get("limit").asInt() : 5;

                List<LaptopInfo> results;
                if (description != null && !description.trim().isEmpty()) {
                    results = laptopSearchService.findSimilarLaptops(description, limit);
                } else if (laptopId != null) {
                    results = laptopSearchService.findSimilarLaptops(laptopId, limit);
                } else {
                    throw new IllegalArgumentException("必须提供描述或笔记本电脑ID");
                }

                return objectMapper.valueToTree(convertLaptopsToMap(results));
            } catch (Exception e) {
                log.error("Error handling find similar laptops", e);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", e.getMessage());
                return error;
            }
        });
    }

    */
/**
     * 处理根据ID获取笔记本电脑的请求
     *//*

    private CompletableFuture<JsonNode> handleGetLaptopById(JsonNode args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String productId = args.get("productId").asText();
                if (productId == null || productId.trim().isEmpty()) {
                    throw new IllegalArgumentException("产品ID不能为空");
                }

                LaptopInfo laptop = laptopSearchService.findByProductId(productId);
                return objectMapper.valueToTree(convertLaptopToMap(laptop));
            } catch (Exception e) {
                log.error("Error handling get laptop by id", e);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", e.getMessage());
                return error;
            }
        });
    }

    */
/**
     * 处理刷新笔记本电脑数据的请求
     *//*

    private CompletableFuture<JsonNode> handleRefreshLaptopData(JsonNode args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                laptopSearchService.refreshLaptopData();
                ObjectNode result = objectMapper.createObjectNode();
                result.put("success", true);
                result.put("message", "笔记本电脑数据刷新成功");
                return result;
            } catch (Exception e) {
                log.error("Error handling refresh laptop data", e);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", e.getMessage());
                return error;
            }
        });
    }

    private List<Map<String, Object>> convertLaptopsToMap(List<LaptopInfo> laptops) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LaptopInfo laptop : laptops) {
            result.add(convertLaptopToMap(laptop));
        }
        return result;
    }

    private Map<String, Object> convertLaptopToMap(LaptopInfo laptop) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", laptop.getId());
        result.put("title", laptop.getTitle());
        result.put("description", laptop.getDescription());
        result.put("productId", laptop.getProductId());
        result.put("imageUrl", laptop.getImageUrl());
        result.put("productUrl", laptop.getProductUrl());
        result.put("price", laptop.getPrice());
        result.put("originalPrice", laptop.getOriginalPrice());
        result.put("brand", laptop.getBrand());
        result.put("model", laptop.getModel());
        result.put("processorInfo", laptop.getProcessorInfo());
        result.put("memoryInfo", laptop.getMemoryInfo());
        result.put("storageInfo", laptop.getStorageInfo());
        result.put("displayInfo", laptop.getDisplayInfo());
        result.put("conditionGrade", laptop.getConditionGrade());
        result.put("sellerName", laptop.getSellerName());
        result.put("sellerRating", laptop.getSellerRating());
        result.put("createdAt", laptop.getCreatedAt() != null ? laptop.getCreatedAt().toString() : null);
        result.put("updatedAt", laptop.getUpdatedAt() != null ? laptop.getUpdatedAt().toString() : null);
        return result;
    }

    */
/**
     * 停止服务器
     *//*

    public void stop() {
        this.isRunning = false;
        System.out.println("MCP Server stopped");
    }

    */
/**
     * 服务器信息类
     *//*

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

    */
/**
     * MCP异常类
     *//*

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
*/
