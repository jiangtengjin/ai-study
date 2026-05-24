package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiService {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.model}")
    private String model;

    @Value("${ai.deepseek.max-retries}")
    private int maxRetries;

    @Value("${ai.deepseek.timeout}")
    private int timeout;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成闯关题目
     */
    public Map<String, Object> generateQuestions(String content, int count, String difficulty) {
        String difficultyLabel = switch (difficulty) {
            case "easy" -> "easy（简单）";
            case "hard" -> "hard（困难）";
            case "balanced" -> "balanced（均衡）";
            default -> "medium（中等）";
        };
        String prompt = loadPrompt("prompts/generate-questions.txt")
                .replace("{content}", content)
                .replace("{count}", String.valueOf(count))
                .replace("{difficulty}", difficultyLabel);

        String responseText = callAiApi(prompt);
        return parseJsonResponse(responseText);
    }

    /**
     * 生成学习报告的知识总结
     */
    public Map<String, Object> generateReportSummary(String content, int total, int correct,
                                                      List<String> correctPoints, List<String> wrongPoints) {
        int rate = total > 0 ? (int) ((double) correct / total * 100) : 0;
        String prompt = loadPrompt("prompts/generate-report.txt")
                .replace("{content}", content)
                .replace("{total}", String.valueOf(total))
                .replace("{correct}", String.valueOf(correct))
                .replace("{rate}", String.valueOf(rate))
                .replace("{correct_points}", String.join("、", correctPoints))
                .replace("{wrong_points}", String.join("、", wrongPoints));

        String responseText = callAiApi(prompt);
        return parseJsonResponse(responseText);
    }

    /**
     * 调用 DeepSeek API（带重试）
     */
    private String callAiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是一个专业的教育出题专家，只输出JSON格式的内容，不要包含任何其他文字。"),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.7);
        body.put("max_tokens", 4096);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        baseUrl + "/v1/chat/completions",
                        HttpMethod.POST,
                        request,
                        Map.class
                );

                Map<String, Object> body2 = response.getBody();
                if (body2 == null) {
                    throw new BizException(1001, "AI 返回内容为空");
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) body2.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new BizException(1001, "AI 返回格式异常");
                }

                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");

            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                log.warn("AI API 调用失败，第 {} 次重试: {}", i + 1, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep((long) Math.pow(2, i) * 1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("AI API 调用最终失败", lastException);
        throw new BizException(1001, "AI 生成失败，请稍后重试");
    }

    /**
     * 解析 JSON 响应（带容错）
     */
    private Map<String, Object> parseJsonResponse(String text) {
        // 先尝试直接解析
        try {
            return objectMapper.readValue(text, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.debug("直接 JSON 解析失败，尝试正则提取");
        }

        // 正则提取 JSON 块
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return objectMapper.readValue(matcher.group(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("正则提取的 JSON 解析失败");
            }
        }

        throw new BizException(1001, "AI 返回内容格式异常，请重试");
    }

    /**
     * 加载 Prompt 模板
     */
    private String loadPrompt(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载 Prompt 模板失败: {}", path, e);
            throw new BizException(1001, "系统配置异常");
        }
    }
}
