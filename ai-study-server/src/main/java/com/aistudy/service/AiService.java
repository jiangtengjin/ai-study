package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.deepseek.max-retries:3}")
    private int maxRetries;

    public AiService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 生成闯关题目
     */
    public Map<String, Object> generateQuestions(String content, int count, String difficulty) {
        return generateQuestions(content, count, difficulty, "");
    }

    /**
     * 生成闯关题目（带搜索结果上下文）
     */
    public Map<String, Object> generateQuestions(String content, int count, String difficulty, String searchResults) {
        String difficultyLabel = switch (difficulty) {
            case "easy" -> "easy（简单）";
            case "hard" -> "hard（困难）";
            case "balanced" -> "balanced（均衡）";
            default -> "medium（中等）";
        };
        String searchSection = searchResults.isBlank() ? "" :
                "## 联网搜索获取的参考知识\n<<<SEARCH_START>>\n" + searchResults + "\n<<<SEARCH_END>>\n\n"
                + "如果上方有参考知识，请优先参考其中的事实信息来出题，确保题目准确。\n"
                + "如果参考知识为空或与主题不相关，请以用户输入的\"知识内容\"为准。\n"
                + "如果搜索结果与用户输入的主题明显不相关（例如属于不同领域），请忽略搜索结果，以用户输入为准。\n";
        String prompt = loadPrompt("prompts/generate-questions.txt")
                .replace("{content}", content)
                .replace("{count}", String.valueOf(count))
                .replace("{difficulty}", difficultyLabel)
                .replace("{searchSection}", searchSection);

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
     * 调用 AI API（带重试，通过 Spring AI ChatClient）
     */
    private String callAiApi(String prompt) {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String response = chatClient.prompt()
                        .system("你是一个专业的教育出题专家，只输出JSON格式的内容，不要包含任何其他文字。")
                        .user(prompt)
                        .call()
                        .content();

                if (response == null || response.isBlank()) {
                    throw new BizException(1001, "AI 返回内容为空");
                }
                return response;

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
