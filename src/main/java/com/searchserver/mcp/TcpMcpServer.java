package com.searchserver.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchserver.model.LaptopInfo;
import com.searchserver.service.LaptopSearchService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class TcpMcpServer {
    @Resource
    private LaptopSearchService laptopSearchService;
    @Resource
    private ObjectMapper objectMapper;

    private Map<String, String> requestIdToResponse = new ConcurrentHashMap<>();
    private Map<String, Object> toolHandlers = new HashMap<>();
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(TcpMcpServer.class);
    @PostConstruct
    public void init() {
        // 注册工具处理器
        toolHandlers.put("search_laptops", handleSearchLaptops);
        toolHandlers.put("find_similar_laptops", handleFindSimilarLaptops);
        toolHandlers.put("get_laptop_by_id", handleGetLaptopById);
        toolHandlers.put("refresh_laptop_data", handleRefreshLaptopData);

        // 启动MCP服务器
        startMcpServer();
    }

    private void startMcpServer() {
        Thread serverThread = new Thread(() -> {
            try {
                log.info("Starting JD Laptop MCP Server...");

                // 创建服务器套接字
                ServerSocket serverSocket = new ServerSocket(9876);
                log.info("JD Laptop MCP Server started on port 9876");

                while (true) {
                    try {
                        // 等待客户端连接
                        Socket clientSocket = serverSocket.accept();
                        log.info("Client connected: {}", clientSocket.getInetAddress());

                        // 为每个客户端创建一个新线程
                        Thread clientThread = new Thread(() -> handleClient(clientSocket));
                        clientThread.start();
                    } catch (IOException e) {
                        log.error("Error accepting client connection", e);
                    }
                }
            } catch (IOException e) {
                log.error("Error starting MCP server", e);
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    // 解析请求
                    Map<String, Object> request = objectMapper.readValue(inputLine, Map.class);
                    String method = (String) request.get("method");
                    String id = (String) request.get("id");
                    Map<String, Object> params = (Map<String, Object>) request.get("params");

                    // 处理请求
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", id);

                    if ("list_tools".equals(method)) {
                        response.put("result", handleListTools());
                    } else if ("call_tool".equals(method)) {
                        String toolName = (String) params.get("name");
                        Object arguments = params.get("arguments");

                        if (toolHandlers.containsKey(toolName)) {
                            Object result = ((ToolHandler) toolHandlers.get(toolName)).handle(arguments);
                            response.put("result", result);
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("code", -32601);
                            error.put("message", "Method not found: " + toolName);
                            response.put("error", error);
                        }
                    } else {
                        Map<String, Object> error = new HashMap<>();
                        error.put("code", -32601);
                        error.put("message", "Method not found: " + method);
                        response.put("error", error);
                    }

                    // 发送响应
                    out.println(objectMapper.writeValueAsString(response));
                } catch (Exception e) {
                    log.error("Error processing request", e);

                    // 发送错误响应
                    Map<String, Object> errorResponse = new HashMap<>();
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", -32603);
                    error.put("message", "Internal error: " + e.getMessage());
                    errorResponse.put("error", error);
                    errorResponse.put("id", null);

                    out.println(objectMapper.writeValueAsString(errorResponse));
                }
            }
        } catch (IOException e) {
            log.error("Error handling client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }

    private Map<String, Object> handleListTools() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tools = new ArrayList<>();

        // 搜索笔记本电脑
        Map<String, Object> searchTool = new HashMap<>();
        searchTool.put("name", "search_laptops");
        searchTool.put("description", "搜索京东二手笔记本电脑");
        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        Map<String, Object> searchProps = new HashMap<>();

        Map<String, Object> keywordProp = new HashMap<>();
        keywordProp.put("type", "string");
        keywordProp.put("description", "搜索关键词");
        searchProps.put("keyword", keywordProp);

        Map<String, Object> minPriceProp = new HashMap<>();
        minPriceProp.put("type", "number");
        minPriceProp.put("description", "最低价格");
        searchProps.put("minPrice", minPriceProp);

        Map<String, Object> maxPriceProp = new HashMap<>();
        maxPriceProp.put("type", "number");
        maxPriceProp.put("description", "最高价格");
        searchProps.put("maxPrice", maxPriceProp);

        searchSchema.put("properties", searchProps);
        searchTool.put("inputSchema", searchSchema);
        tools.add(searchTool);

        // 查找相似笔记本电脑
        Map<String, Object> similarTool = new HashMap<>();
        similarTool.put("name", "find_similar_laptops");
        similarTool.put("description", "查找相似的京东二手笔记本电脑");
        Map<String, Object> similarSchema = new HashMap<>();
        similarSchema.put("type", "object");
        Map<String, Object> similarProps = new HashMap<>();

        Map<String, Object> descProp = new HashMap<>();
        descProp.put("type", "string");
        descProp.put("description", "描述或需求");
        similarProps.put("description", descProp);

        Map<String, Object> laptopIdProp = new HashMap<>();
        laptopIdProp.put("type", "number");
        laptopIdProp.put("description", "笔记本电脑ID");
        similarProps.put("laptopId", laptopIdProp);

        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "number");
        limitProp.put("description", "返回结果数量限制");
        similarProps.put("limit", limitProp);

        similarSchema.put("properties", similarProps);
        similarTool.put("inputSchema", similarSchema);
        tools.add(similarTool);

        // 根据ID获取笔记本电脑
        Map<String, Object> getByIdTool = new HashMap<>();
        getByIdTool.put("name", "get_laptop_by_id");
        getByIdTool.put("description", "根据产品ID获取京东二手笔记本电脑详情");
        Map<String, Object> getByIdSchema = new HashMap<>();
        getByIdSchema.put("type", "object");
        Map<String, Object> getByIdProps = new HashMap<>();

        Map<String, Object> productIdProp = new HashMap<>();
        productIdProp.put("type", "string");
        productIdProp.put("description", "产品ID");
        getByIdProps.put("productId", productIdProp);

        getByIdSchema.put("properties", getByIdProps);
        getByIdSchema.put("required", Collections.singletonList("productId"));
        getByIdTool.put("inputSchema", getByIdSchema);
        tools.add(getByIdTool);

        // 刷新笔记本电脑数据
        Map<String, Object> refreshTool = new HashMap<>();
        refreshTool.put("name", "refresh_laptop_data");
        refreshTool.put("description", "刷新京东二手笔记本电脑数据");
        Map<String, Object> refreshSchema = new HashMap<>();
        refreshSchema.put("type", "object");
        refreshTool.put("inputSchema", refreshSchema);
        tools.add(refreshTool);

        result.put("tools", tools);
        return result;
    }

    private Function<Map<String, Object>, Object> handleSearchLaptops = args -> {
        try {
            String keyword = (String) args.get("keyword");
            Number minPriceNum = (Number) args.get("minPrice");
            Number maxPriceNum = (Number) args.get("maxPrice");

            List<LaptopInfo> results;
            if (keyword != null && !keyword.trim().isEmpty()) {
                results = laptopSearchService.searchByKeyword(keyword);
            } else if (minPriceNum != null && maxPriceNum != null) {
                BigDecimal minPrice = BigDecimal.valueOf(minPriceNum.doubleValue());
                BigDecimal maxPrice = BigDecimal.valueOf(maxPriceNum.doubleValue());
                results = laptopSearchService.searchByPriceRange(minPrice, maxPrice);
            } else {
                throw new IllegalArgumentException("必须提供关键词或价格范围");
            }

            return convertLaptopsToMap(results);
        } catch (Exception e) {
            log.error("Error handling search laptops", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    };

    private Function<Map<String, Object>, Object> handleFindSimilarLaptops = arguments -> {
        try {
            Map<String, Object> args = (Map<String, Object>) arguments;
            String description = (String) args.get("description");
            Number laptopIdNum = (Number) args.get("laptopId");
            Number limitNum = (Number) args.get("limit");

            int limit = limitNum != null ? limitNum.intValue() : 5;

            List<LaptopInfo> results;
            if (description != null && !description.trim().isEmpty()) {
                results = laptopSearchService.findSimilarLaptops(description, limit);
            } else if (laptopIdNum != null) {
                Long laptopId = laptopIdNum.longValue();
                results = laptopSearchService.findSimilarLaptops(laptopId, limit);
            } else {
                throw new IllegalArgumentException("必须提供描述或笔记本电脑ID");
            }

            return convertLaptopsToMap(results);
        } catch (Exception e) {
            log.error("Error handling find similar laptops", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    };

    private Function<Map<String, Object>, Object> handleGetLaptopById = arguments -> {
        try {
            Map<String, Object> args = (Map<String, Object>) arguments;
            String productId = (String) args.get("productId");

            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("产品ID不能为空");
            }

            LaptopInfo laptop = laptopSearchService.findByProductId(productId);
            return convertLaptopToMap(laptop);
        } catch (Exception e) {
            log.error("Error handling get laptop by id", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    };

    private Function<Map<String, Object>, Object> handleRefreshLaptopData = arguments -> {
        try {
            laptopSearchService.refreshLaptopData();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "笔记本电脑数据刷新成功");
            return result;
        } catch (Exception e) {
            log.error("Error handling refresh laptop data", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    };

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

    @FunctionalInterface
    private interface ToolHandler {
        Object handle(Object arguments);
    }
}
