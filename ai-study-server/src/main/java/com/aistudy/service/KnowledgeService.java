package com.aistudy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Slf4j
@Service
public class KnowledgeService {

    @Value("${tavily.api-key:}")
    private String tavilyApiKey;

    @Value("${tavily.base-url:https://api.tavily.com}")
    private String tavilyBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public KnowledgeService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_RESULTS = 5;
    private static final int MAX_CONTENT_LENGTH = 3000;

    /**
     * 根据用户输入获取知识：
     * - 输入包含 URL → 调用 Extract API 提取网页内容
     * - 输入为纯文本 → 调用 Search API 搜索相关内容
     */
    public String retrieveKnowledge(String userInput) {
        if (tavilyApiKey == null || tavilyApiKey.isBlank() || tavilyApiKey.contains("your-tavily")) {
            log.warn("Tavily API Key 未配置，跳过知识检索");
            return "";
        }

        try {
            if (containsUrl(userInput)) {
                return extractFromUrls(userInput);
            } else {
                return searchByKeyword(userInput);
            }
        } catch (Exception e) {
            log.error("知识检索失败，降级到纯模型出题: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 判断输入是否包含 URL
     */
    private boolean containsUrl(String input) {
        return input.matches("(?s).*https?://\\S+.*");
    }

    /**
     * 通过 Tavily Search API 搜索关键词
     */
    private String searchByKeyword(String query) {
        log.info("Tavily 搜索: {}", query);

        Future<String> future = executor.submit(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "api_key", tavilyApiKey,
                    "query", query,
                    "max_results", MAX_RESULTS,
                    "search_depth", "basic"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    tavilyBaseUrl + "/search",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return parseSearchResults(response.getBody());
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Tavily 搜索超时 ({}s)", TIMEOUT_SECONDS);
            return "";
        } catch (Exception e) {
            log.error("Tavily 搜索异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 通过 Tavily Extract API 提取 URL 内容
     */
    private String extractFromUrls(String input) {
        List<String> urls = extractUrls(input);
        if (urls.isEmpty()) {
            return "";
        }

        log.info("Tavily 提取 URL 内容: {}", urls);

        Future<String> future = executor.submit(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "api_key", tavilyApiKey,
                    "urls", urls
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    tavilyBaseUrl + "/extract",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return parseExtractResults(response.getBody());
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Tavily 提取超时 ({}s)", TIMEOUT_SECONDS);
            return "";
        } catch (Exception e) {
            log.error("Tavily 提取异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从输入文本中提取所有 URL
     */
    private List<String> extractUrls(String input) {
        List<String> urls = new ArrayList<>();
        String[] words = input.split("\\s+");
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                // 去除末尾的标点符号
                trimmed = trimmed.replaceAll("[,;，；。）)]+$", "");
                urls.add(trimmed);
            }
        }
        return urls;
    }

    /**
     * 解析 Tavily Search API 响应，格式化为结构化文本
     */
    private String parseSearchResults(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.isEmpty()) {
                log.info("Tavily 搜索无结果");
                return "";
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (JsonNode result : results) {
                if (count >= MAX_RESULTS) break;

                String title = result.has("title") ? result.get("title").asText() : "";
                String content = result.has("content") ? result.get("content").asText() : "";
                String url = result.has("url") ? result.get("url").asText() : "";

                if (content.length() > MAX_CONTENT_LENGTH / MAX_RESULTS) {
                    content = content.substring(0, MAX_CONTENT_LENGTH / MAX_RESULTS) + "...";
                }

                sb.append(String.format("[%d] 标题: %s\n摘要: %s\n来源: %s\n\n",
                        count + 1, title, content, url));
                count++;
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("解析 Tavily 搜索结果失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析 Tavily Extract API 响应，格式化为结构化文本
     */
    private String parseExtractResults(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.isEmpty()) {
                log.info("Tavily 提取无结果");
                return "";
            }

            StringBuilder sb = new StringBuilder();
            int totalLength = 0;
            for (JsonNode result : results) {
                String url = result.has("url") ? result.get("url").asText() : "";
                String rawContent = result.has("raw_content") ? result.get("raw_content").asText() : "";

                if (rawContent.isBlank()) continue;

                // 截断过长内容
                int remaining = MAX_CONTENT_LENGTH - totalLength;
                if (remaining <= 0) break;
                if (rawContent.length() > remaining) {
                    rawContent = rawContent.substring(0, remaining) + "...";
                }

                sb.append(String.format("网页内容（%s）:\n%s\n\n", url, rawContent));
                totalLength += rawContent.length();
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("解析 Tavily 提取结果失败: {}", e.getMessage());
            return "";
        }
    }
}
