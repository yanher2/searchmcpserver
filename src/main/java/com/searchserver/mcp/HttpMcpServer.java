package com.searchserver.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchserver.model.LaptopInfo;
import com.searchserver.service.LaptopSearchService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/mcp")
public class HttpMcpServer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LaptopSearchService laptopSearchService;


    @PostMapping(value = "/initialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> initialize(@RequestBody Map<String, Object> body) {
        // 获取 id 字段，默认 0
        Object idObj = body.get("id");
        Object paramsObj = body.get("params");
        int id = 0;
        if (idObj instanceof Integer) {
            id = (Integer) idObj;
        } else if (idObj instanceof String) {
            try {
                id = Integer.parseInt((String) idObj);
            } catch (NumberFormatException e) {
                id = 0;
            }
        }

        // 解析 protocolVersion
        String protocolVersion = "2025-06-18";
        if (paramsObj instanceof Map) {
            Map params = (Map) paramsObj;
            if (params.get("protocolVersion") != null) {
                protocolVersion = params.get("protocolVersion").toString();
            }
        }

        // 构造 tools 列表（根路径的 tools 数组）
        List<Map<String, Object>> toolsList = new ArrayList<>();
        {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", "search_laptops");
            tool.put("description", "Search laptops by keyword or price range");
            List<Map<String, Object>> params = new ArrayList<>();
            params.add(Map.of("name", "keyword", "type", "string", "description", "Search keyword"));
            params.add(Map.of("name", "minPrice", "type", "number", "description", "Minimum price"));
            params.add(Map.of("name", "maxPrice", "type", "number", "description", "Maximum price"));
            tool.put("parameters", params);
            Map<String, Object> searchLaptopsInputSchema = new HashMap<>();
            searchLaptopsInputSchema.put("type", "object");
            Map<String, Object> searchLaptopsProps = new HashMap<>();
            searchLaptopsProps.put("keyword", Map.of("type", "string", "description", "Search keyword"));
            searchLaptopsProps.put("minPrice", Map.of("type", "number", "description", "Minimum price"));
            searchLaptopsProps.put("maxPrice", Map.of("type", "number", "description", "Maximum price"));
            searchLaptopsInputSchema.put("properties", searchLaptopsProps);
            searchLaptopsInputSchema.put("required", new ArrayList<>());
            tool.put("inputSchema", searchLaptopsInputSchema);
            toolsList.add(tool);
        }
        {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", "find_similar_laptops");
            tool.put("description", "Find laptops similar to given description or product");
            List<Map<String, Object>> params = new ArrayList<>();
            params.add(Map.of("name", "description", "type", "string", "description", "Description of desired laptop"));
            params.add(Map.of("name", "laptopId", "type", "string", "description", "ID of reference laptop"));
            params.add(Map.of("name", "limit", "type", "integer", "description", "Maximum number of results"));
            tool.put("parameters", params);
            Map<String, Object> findSimilarInputSchema = new HashMap<>();
            findSimilarInputSchema.put("type", "object");
            Map<String, Object> findSimilarProps = new HashMap<>();
            findSimilarProps.put("description", Map.of("type", "string", "description", "Description of desired laptop"));
            findSimilarProps.put("laptopId", Map.of("type", "string", "description", "ID of reference laptop"));
            findSimilarProps.put("limit", Map.of("type", "integer", "description", "Maximum number of results"));
            findSimilarInputSchema.put("properties", findSimilarProps);
            findSimilarInputSchema.put("required", new ArrayList<>());
            tool.put("inputSchema", findSimilarInputSchema);
            toolsList.add(tool);
        }
        {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", "get_laptop_by_id");
            tool.put("description", "Get laptop details by product ID");
            List<Map<String, Object>> params = new ArrayList<>();
            params.add(Map.of("name", "productId", "type", "string", "description", "Unique product identifier"));
            tool.put("parameters", params);
            Map<String, Object> getByIdInputSchema = new HashMap<>();
            getByIdInputSchema.put("type", "object");
            Map<String, Object> getByIdProps = new HashMap<>();
            getByIdProps.put("productId", Map.of("type", "string", "description", "Unique product identifier"));
            getByIdInputSchema.put("properties", getByIdProps);
            getByIdInputSchema.put("required", new ArrayList<>());
            tool.put("inputSchema", getByIdInputSchema);
            toolsList.add(tool);
        }
        {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", "refresh_laptop_data");
            tool.put("description", "Refresh laptop data from source");
            tool.put("parameters", new ArrayList<>());
            Map<String, Object> refreshInputSchema = new HashMap<>();
            refreshInputSchema.put("type", "object");
            refreshInputSchema.put("properties", new HashMap<>());
            refreshInputSchema.put("required", new ArrayList<>());
            tool.put("inputSchema", refreshInputSchema);
            toolsList.add(tool);
        }
        // 构造 capabilities
        Map<String, Object> capabilities = new HashMap<>();
        // capabilities.tools 是对象，包含 methods 和 tools
        Map<String, Object> toolsObj = new HashMap<>();
        List<String> methods = new ArrayList<>();
        methods.add("search_laptops");
        methods.add("find_similar_laptops");
        methods.add("get_laptop_by_id");
        methods.add("refresh_laptop_data");
        toolsObj.put("methods", methods);
        toolsObj.put("tools", toolsList); // 这里放同样的工具列表
        capabilities.put("tools", toolsObj);
        // 其它功能为空对象
        capabilities.put("prompts", new HashMap<>());
        capabilities.put("resources", new HashMap<>());
        capabilities.put("logging", new HashMap<>());
        Map<String, Object> roots = new HashMap<>();
        roots.put("listChanged", false);
        capabilities.put("roots", roots);

        // 构造 serverInfo
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "java-search-mcp-server");
        serverInfo.put("version", "1.0.0");

        // 构造 result
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        result.put("tools", toolsList); // 根路径的 tools 数组

        // 构造 JSON-RPC 响应
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("result", result);
        return resp;
    }

    /**
     * SSE推送工具定义，首条为工具定义，后续为心跳
     */
    @PostMapping(value = "/tools", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> listTools(@RequestBody Map<String, Object> body) {
        // 获取 id 字段，默认 0
        int id = (int)body.get("id");

        // 构造工具定义
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode searchLaptops = objectMapper.createObjectNode();
        searchLaptops.put("name", "search_laptops");
        searchLaptops.put("description", "Search laptops by keyword or price range");
        ArrayNode searchLaptopsParams = objectMapper.createArrayNode();
        searchLaptopsParams.add(objectMapper.createObjectNode()
            .put("name", "keyword").put("type", "string").put("description", "Search keyword"));
        searchLaptopsParams.add(objectMapper.createObjectNode()
            .put("name", "minPrice").put("type", "number").put("description", "Minimum price"));
        searchLaptopsParams.add(objectMapper.createObjectNode()
            .put("name", "maxPrice").put("type", "number").put("description", "Maximum price"));
        searchLaptops.set("parameters", searchLaptopsParams);
        tools.add(searchLaptops);

        ObjectNode findSimilar = objectMapper.createObjectNode();
        findSimilar.put("name", "find_similar_laptops");
        findSimilar.put("description", "Find laptops similar to given description or product");
        ArrayNode findSimilarParams = objectMapper.createArrayNode();
        findSimilarParams.add(objectMapper.createObjectNode()
            .put("name", "description").put("type", "string").put("description", "Description of desired laptop"));
        findSimilarParams.add(objectMapper.createObjectNode()
            .put("name", "laptopId").put("type", "string").put("description", "ID of reference laptop"));
        findSimilarParams.add(objectMapper.createObjectNode()
            .put("name", "limit").put("type", "integer").put("description", "Maximum number of results"));
        findSimilar.set("parameters", findSimilarParams);
        tools.add(findSimilar);

        ObjectNode getById = objectMapper.createObjectNode();
        getById.put("name", "get_laptop_by_id");
        getById.put("description", "Get laptop details by product ID");
        ArrayNode getByIdParams = objectMapper.createArrayNode();
        getByIdParams.add(objectMapper.createObjectNode()
            .put("name", "productId").put("type", "string").put("description", "Unique product identifier"));
        getById.set("parameters", getByIdParams);
        tools.add(getById);

        ObjectNode refresh = objectMapper.createObjectNode();
        refresh.put("name", "refresh_laptop_data");
        refresh.put("description", "Refresh laptop data from source");
        ArrayNode refreshParams = objectMapper.createArrayNode();
        refresh.set("parameters", refreshParams);
        tools.add(refresh);

        // 构造 JSON-RPC 2.0 格式
        ObjectNode params = objectMapper.createObjectNode();
        params.set("tools", tools);
        ObjectNode rpc = objectMapper.createObjectNode();
        rpc.put("jsonrpc", "2.0");
        rpc.put("id", id);
        rpc.set("params", params);

        // SSE 格式
        String toolDef = " " + rpc.toString() + "\n\n";

        // 心跳包同样用 JSON-RPC 2.0 格式
        ObjectNode heartbeatResult = objectMapper.createObjectNode();
        heartbeatResult.put("type", "heartbeat");
        ObjectNode heartbeatRpc = objectMapper.createObjectNode();
        heartbeatRpc.put("jsonrpc", "2.0");
        heartbeatRpc.put("id", id);
        heartbeatRpc.set("params", heartbeatResult);

        Flux<String> heartbeat = Flux.interval(Duration.ofSeconds(15))
            .map(i -> " " + heartbeatRpc.toString() + "\n\n");

        return Flux.concat(
            Flux.just(toolDef),
            heartbeat
        );
    }

    /**
     * 调用工具
     */
    @PostMapping("/tools/{toolName}")
    public Mono<JsonNode> callTool(
            @PathVariable String toolName,
            @RequestBody JsonNode arguments) {
        return switch (toolName) {
            case "search_laptops" -> handleSearchLaptops(arguments);
            case "find_similar_laptops" -> handleFindSimilarLaptops(arguments);
            case "get_laptop_by_id" -> handleGetLaptopById(arguments);
            case "refresh_laptop_data" -> handleRefreshLaptopData();
            default -> Mono.just(createErrorResponse("Unknown tool: " + toolName));
        };
    }

    private Mono<JsonNode> handleSearchLaptops(JsonNode args) {
        return Mono.<JsonNode>fromCallable(() -> {
            String keyword = args.has("keyword") ? args.get("keyword").asText() : null;
            Double minPrice = args.has("minPrice") ? args.get("minPrice").asDouble() : null;
            Double maxPrice = args.has("maxPrice") ? args.get("maxPrice").asDouble() : null;

            List<LaptopInfo> results;
            if (keyword != null && !keyword.trim().isEmpty()) {
                results = laptopSearchService.searchByKeyword(keyword);
            } else if (minPrice != null && maxPrice != null) {
                results = laptopSearchService.searchByPriceRange(
                        java.math.BigDecimal.valueOf(minPrice),
                        java.math.BigDecimal.valueOf(maxPrice)
                );
            } else {
                throw new IllegalArgumentException("必须提供关键词或价格范围");
            }

            return objectMapper.valueToTree(convertLaptopsToMap(results));
        }).onErrorResume(e -> Mono.just(createErrorResponse(e.getMessage())));
    }

    private Mono<JsonNode> handleFindSimilarLaptops(JsonNode args) {
        return Mono.<JsonNode>fromCallable(() -> {
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
        }).onErrorResume(e -> Mono.just(createErrorResponse(e.getMessage())));
    }

    private Mono<JsonNode> handleGetLaptopById(JsonNode args) {
        return Mono.<JsonNode>fromCallable(() -> {
            String productId = args.get("productId").asText();
            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("产品ID不能为空");
            }
            LaptopInfo laptop = laptopSearchService.findByProductId(productId);
            return objectMapper.valueToTree(convertLaptopToMap(laptop));
        }).onErrorResume(e -> Mono.just(createErrorResponse(e.getMessage())));
    }

    private Mono<JsonNode> handleRefreshLaptopData() {
        return Mono.<JsonNode>fromCallable(() -> {
            laptopSearchService.refreshLaptopData();
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "success");
            result.put("message", "笔记本电脑数据刷新成功");
            return result;
        }).onErrorResume(e -> Mono.just(createErrorResponse(e.getMessage())));
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