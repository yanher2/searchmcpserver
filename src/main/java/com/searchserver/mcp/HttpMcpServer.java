package com.searchserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchserver.model.LaptopInfo;
import com.searchserver.service.LaptopSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP MCP服务器，复用LaptopSearchService实现功能
 */
@RestController
@RequestMapping("/api/mcp")
public class HttpMcpServer {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LaptopSearchService laptopSearchService;

    // 直接在类中定义工具列表
    private static final List<String> TOOLS = List.of(
        "search_laptops",
        "find_similar_laptops",
        "get_laptop_by_id",
        "refresh_laptop_data"
    );

    /**
     * 列出所有可用工具
     */
    @GetMapping("/tools")
    public CompletableFuture<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode toolsArray = response.putArray("tools");
        TOOLS.forEach(toolsArray::add);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 调用工具
     */
    @PostMapping("/tools/{toolName}")
    public CompletableFuture<JsonNode> callTool(
            @PathVariable String toolName,
            @RequestBody JsonNode arguments) {
        // 验证工具是否存在
        if (!isToolExists(toolName)) {
            return CompletableFuture.completedFuture(createErrorResponse("Unknown tool: " + toolName));
        }

        // 根据工具名称路由到对应服务方法
        switch (toolName) {
            case "search_laptops":
                return handleSearchLaptops(arguments);
            case "find_similar_laptops":
                return handleFindSimilarLaptops(arguments);
            case "get_laptop_by_id":
                return handleGetLaptopById(arguments);
            case "refresh_laptop_data":
                return handleRefreshLaptopData();
            default:
                return CompletableFuture.completedFuture(createErrorResponse("Tool not implemented: " + toolName));
        }
    }

    private boolean isToolExists(String toolName) {
        return TOOLS.contains(toolName);
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
        return handleSearchLaptops(arguments);
    }

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
                return createErrorResponse(e.getMessage());
            }
        });
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
        return handleFindSimilarLaptops(arguments);
    }

    private CompletableFuture<JsonNode> handleFindSimilarLaptops(JsonNode args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String description = args.has("description") ? args.get("description").asText() : null;
                String laptopId = args.has("laptopId") ? args.get("laptopId").asText() : null;
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
                return createErrorResponse(e.getMessage());
            }
        });
    }

    /**
     * 根据ID获取笔记本电脑详情
     */
    @GetMapping("/tools/get_laptop_by_id")
    public CompletableFuture<JsonNode> getLaptopById(
            @RequestParam String productId) {
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("productId", productId);
        return handleGetLaptopById(arguments);
    }

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
                return createErrorResponse(e.getMessage());
            }
        });
    }

    /**
     * 刷新笔记本电脑数据
     */
    @PostMapping("/tools/refresh_laptop_data")
    public CompletableFuture<JsonNode> refreshLaptopData() {
        return handleRefreshLaptopData();
    }

    private CompletableFuture<JsonNode> handleRefreshLaptopData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                laptopSearchService.refreshLaptopData();
                ObjectNode result = objectMapper.createObjectNode();
                result.put("status", "success");
                result.put("message", "笔记本电脑数据刷新成功");
                return result;
            } catch (Exception e) {
                return createErrorResponse(e.getMessage());
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

    private ObjectNode createErrorResponse(String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("error", message);
        return response;
    }
}
