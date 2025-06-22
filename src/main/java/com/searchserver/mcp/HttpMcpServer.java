package com.searchserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 独立实现的HTTP MCP服务器
 */
@RestController
@RequestMapping("/api/mcp")
public class HttpMcpServer {

    @Autowired
    private ObjectMapper objectMapper;

    // 模拟工具数据存储
    private final Map<String, Laptop> laptopDatabase = new HashMap<>();
    
    // 可用工具列表
    private final String[] tools = {
        "search_laptops",
        "find_similar_laptops",
        "get_laptop_by_id",
        "refresh_laptop_data"
    };

    public HttpMcpServer() {
        // 初始化模拟数据
        laptopDatabase.put("1", new Laptop("1", "ThinkPad X1 Carbon", 4999.0));
        laptopDatabase.put("2", new Laptop("2", "ThinkPad T480", 2999.0));
        laptopDatabase.put("3", new Laptop("3", "ThinkPad X1 Yoga", 5299.0));
        laptopDatabase.put("4", new Laptop("4", "ThinkPad X1 Extreme", 6999.0));
        laptopDatabase.put("5", new Laptop("5", "ThinkPad T14", 3999.0));
    }

    /**
     * 列出所有可用工具
     */
    @GetMapping("/tools")
    public CompletableFuture<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode toolsArray = response.putArray("tools");
        for (String tool : tools) {
            toolsArray.add(tool);
        }
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 调用工具
     */
    @PostMapping("/tools/{toolName}")
    public CompletableFuture<JsonNode> callTool(
            @PathVariable String toolName,
            @RequestBody JsonNode arguments) {
        switch (toolName) {
            case "search_laptops":
                return searchLaptops(arguments);
            case "find_similar_laptops":
                return findSimilarLaptops(arguments);
            case "get_laptop_by_id":
                return getLaptopById(arguments);
            case "refresh_laptop_data":
                return refreshLaptopData();
            default:
                return CompletableFuture.completedFuture(createErrorResponse("Unknown tool: " + toolName));
        }
    }

    /**
     * 搜索笔记本电脑
     */
    @PostMapping("/tools/search_laptops")
    public CompletableFuture<JsonNode> searchLaptops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        ObjectNode arguments = objectMapper.createObjectNode();
        if (keyword != null) arguments.put("keyword", keyword);
        if (minPrice != null) arguments.put("minPrice", minPrice);
        if (maxPrice != null) arguments.put("maxPrice", maxPrice);
        return searchLaptops(arguments);
    }

    private CompletableFuture<JsonNode> searchLaptops(JsonNode arguments) {
        ArrayNode results = objectMapper.createArrayNode();
        String keyword = arguments.path("keyword").asText(null);
        double minPrice = arguments.path("minPrice").asDouble(0);
        double maxPrice = arguments.path("maxPrice").asDouble(Double.MAX_VALUE);

        laptopDatabase.values().stream()
            .filter(laptop -> keyword == null || laptop.name.contains(keyword))
            .filter(laptop -> laptop.price >= minPrice && laptop.price <= maxPrice)
            .forEach(laptop -> results.add(convertLaptopToJson(laptop)));

        ObjectNode response = objectMapper.createObjectNode();
        response.set("laptops", results);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 查找相似笔记本电脑
     */
    @PostMapping("/tools/find_similar_laptops")
    public CompletableFuture<JsonNode> findSimilarLaptops(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String laptopId,
            @RequestParam(defaultValue = "5") int limit) {
        ObjectNode arguments = objectMapper.createObjectNode();
        if (description != null) arguments.put("description", description);
        if (laptopId != null) arguments.put("laptopId", laptopId);
        arguments.put("limit", limit);
        return findSimilarLaptops(arguments);
    }

    private CompletableFuture<JsonNode> findSimilarLaptops(JsonNode arguments) {
        ArrayNode results = objectMapper.createArrayNode();
        String laptopId = arguments.path("laptopId").asText(null);
        int limit = arguments.path("limit").asInt(5);

        // 简单实现：返回价格相近的笔记本电脑
        if (laptopId != null && laptopDatabase.containsKey(laptopId)) {
            Laptop target = laptopDatabase.get(laptopId);
            laptopDatabase.values().stream()
                .filter(laptop -> !laptop.id.equals(laptopId))
                .sorted((a, b) -> Double.compare(
                    Math.abs(a.price - target.price),
                    Math.abs(b.price - target.price)))
                .limit(limit)
                .forEach(laptop -> results.add(convertLaptopToJson(laptop)));
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.set("similar_laptops", results);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 根据ID获取笔记本电脑详情
     */
    @GetMapping("/tools/get_laptop_by_id")
    public CompletableFuture<JsonNode> getLaptopById(
            @RequestParam String productId) {
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("productId", productId);
        return getLaptopById(arguments);
    }

    private CompletableFuture<JsonNode> getLaptopById(JsonNode arguments) {
        String productId = arguments.path("productId").asText();
        Laptop laptop = laptopDatabase.get(productId);
        if (laptop == null) {
            return CompletableFuture.completedFuture(
                createErrorResponse("Laptop not found: " + productId));
        }
        return CompletableFuture.completedFuture(convertLaptopToJson(laptop));
    }

    /**
     * 刷新笔记本电脑数据
     */
    @PostMapping("/tools/refresh_laptop_data")
    public CompletableFuture<JsonNode> refreshLaptopData() {
        // 简单实现：返回成功响应
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");
        response.put("message", "Data refreshed");
        return CompletableFuture.completedFuture(response);
    }

    private ObjectNode convertLaptopToJson(Laptop laptop) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", laptop.id);
        node.put("name", laptop.name);
        node.put("price", laptop.price);
        node.put("brand", "Lenovo");
        node.put("cpu", "Intel Core i5");
        node.put("ram", "16GB");
        node.put("storage", "512GB SSD");
        return node;
    }

    private ObjectNode createErrorResponse(String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("error", message);
        return response;
    }

    private static class Laptop {
        String id;
        String name;
        double price;

        Laptop(String id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }
}
