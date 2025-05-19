package com.searchserver.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchserver.config.WebDriverConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JdLaptopMcpClient implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final ObjectMapper objectMapper;
    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(JdLaptopMcpClient.class);
    public JdLaptopMcpClient(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> searchLaptops(String keyword, Double minPrice, Double maxPrice) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("keyword", keyword);
        arguments.put("minPrice", minPrice);
        arguments.put("maxPrice", maxPrice);

        Map<String, Object> response = callTool("search_laptops", arguments);
        return objectMapper.convertValue(response, new TypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> findSimilarLaptops(String description, Long laptopId, Integer limit) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("description", description);
        arguments.put("laptopId", laptopId);
        arguments.put("limit", limit);

        Map<String, Object> response = callTool("find_similar_laptops", arguments);
        return objectMapper.convertValue(response, new TypeReference<List<Map<String, Object>>>() {});
    }

    public Map<String, Object> getLaptopById(String productId) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("productId", productId);

        return callTool("get_laptop_by_id", arguments);
    }

    public Map<String, Object> refreshLaptopData() throws Exception {
        return callTool("refresh_laptop_data", new HashMap<>());
    }

    public List<Map<String, Object>> listTools() throws Exception {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("method", "list_tools");
        request.put("id", requestId);
        request.put("params", new HashMap<>());

        out.println(objectMapper.writeValueAsString(request));
        String responseStr = in.readLine();
        Map<String, Object> response = objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            throw new RuntimeException("Error: " + error.get("message"));
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return (List<Map<String, Object>>) result.get("tools");
    }

    private Map<String, Object> callTool(String toolName, Map<String, Object> arguments) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("method", "call_tool");
        request.put("id", requestId);

        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        request.put("params", params);

        out.println(objectMapper.writeValueAsString(request));
        String responseStr = in.readLine();
        Map<String, Object> response = objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            throw new RuntimeException("Error: " + error.get("message"));
        }

        return (Map<String, Object>) response.get("result");
    }

    @Override
    public void close() throws Exception {
        in.close();
        out.close();
        socket.close();
    }

    // 使用示例
    public static void main(String[] args) {
        try (JdLaptopMcpClient client = new JdLaptopMcpClient("localhost", 9876)) {
            // 列出所有可用工具
            System.out.println("Available tools:");
            List<Map<String, Object>> tools = client.listTools();
            tools.forEach(tool -> System.out.println(tool.get("name") + ": " + tool.get("description")));

            // 搜索笔记本电脑
            System.out.println("\nSearching laptops with keyword 'ThinkPad':");
            List<Map<String, Object>> searchResults = client.searchLaptops("ThinkPad", null, null);
            searchResults.forEach(laptop -> System.out.println(laptop.get("title") + " - ¥" + laptop.get("price")));

            // 查找相似笔记本电脑
            System.out.println("\nFinding similar laptops:");
            List<Map<String, Object>> similarResults = client.findSimilarLaptops(
                "需要一台性能强劲的游戏本，预算8000左右", null, 5);
            similarResults.forEach(laptop -> System.out.println(laptop.get("title") + " - ¥" + laptop.get("price")));

            // 刷新数据
            System.out.println("\nRefreshing laptop data:");
            Map<String, Object> refreshResult = client.refreshLaptopData();
            System.out.println("Refresh result: " + refreshResult);

        } catch (Exception e) {
            log.error("Error in MCP client demo", e);
        }
    }
}
