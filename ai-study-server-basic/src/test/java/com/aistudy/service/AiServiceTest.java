package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService 单元测试")
class AiServiceTest {

    @Spy
    @InjectMocks
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "baseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(aiService, "model", "deepseek-chat");
        ReflectionTestUtils.setField(aiService, "maxRetries", 3);
        ReflectionTestUtils.setField(aiService, "timeout", 30000);
        ReflectionTestUtils.setField(aiService, "objectMapper", objectMapper);
    }

    @Nested
    @DisplayName("parseJsonResponse 测试")
    class ParseJsonResponseTest {

        @Test
        @DisplayName("直接 JSON 解析成功")
        void parseDirectJson() {
            String json = """
                    {"title":"测试标题","questions":[{"question":"问题1","answer":"A"}]}
                    """;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                    aiService, "parseJsonResponse", json);

            assertNotNull(result);
            assertEquals("测试标题", result.get("title"));
        }

        @Test
        @DisplayName("带前后文字的 JSON 通过正则提取")
        void parseJsonWithPrefixAndSuffix() {
            String text = """
                    这是AI的回复：
                    {"title":"测试标题","questions":[{"question":"问题1","answer":"A"}]}
                    以上是题目。
                    """;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                    aiService, "parseJsonResponse", text);

            assertNotNull(result);
            assertEquals("测试标题", result.get("title"));
        }

        @Test
        @DisplayName("无效内容抛出 BizException")
        void parseInvalidContentThrowsException() {
            String text = "这是一段完全没有JSON的内容";

            assertThrows(BizException.class, () -> {
                ReflectionTestUtils.invokeMethod(aiService, "parseJsonResponse", text);
            });
        }

        @Test
        @DisplayName("空 JSON 对象解析")
        void parseEmptyJson() {
            String json = "{}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                    aiService, "parseJsonResponse", json);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("嵌套 JSON 解析")
        void parseNestedJson() {
            String json = """
                    {"questions":[{"question":"Q1","options":{"A":"a","B":"b"}}]}
                    """;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                    aiService, "parseJsonResponse", json);

            assertNotNull(result);
            assertNotNull(result.get("questions"));
        }
    }

    @Nested
    @DisplayName("loadPrompt 测试")
    class LoadPromptTest {

        @Test
        @DisplayName("加载出题 Prompt 模板成功")
        void loadGenerateQuestionsPrompt() {
            String prompt = (String) ReflectionTestUtils.invokeMethod(
                    aiService, "loadPrompt", "prompts/generate-questions.txt");

            assertNotNull(prompt);
            assertTrue(prompt.contains("{content}"));
            assertTrue(prompt.contains("{count}"));
        }

        @Test
        @DisplayName("加载报告 Prompt 模板成功")
        void loadGenerateReportPrompt() {
            String prompt = (String) ReflectionTestUtils.invokeMethod(
                    aiService, "loadPrompt", "prompts/generate-report.txt");

            assertNotNull(prompt);
            assertTrue(prompt.contains("{content}"));
            assertTrue(prompt.contains("{rate}"));
        }

        @Test
        @DisplayName("加载不存在的 Prompt 抛出 BizException")
        void loadNonExistentPromptThrowsException() {
            assertThrows(BizException.class, () -> {
                ReflectionTestUtils.invokeMethod(aiService, "loadPrompt", "prompts/nonexistent.txt");
            });
        }
    }

    @Nested
    @DisplayName("Prompt 变量替换测试")
    class PromptTemplateTest {

        @Test
        @DisplayName("出题 Prompt 模板变量替换正确")
        void questionsPromptReplacesVariables() {
            String prompt = (String) ReflectionTestUtils.invokeMethod(
                    aiService, "loadPrompt", "prompts/generate-questions.txt");

            String replaced = prompt
                    .replace("{content}", "机器学习基础知识")
                    .replace("{count}", "5");

            assertTrue(replaced.contains("机器学习基础知识"));
            assertTrue(replaced.contains("5"));
            assertFalse(replaced.contains("{content}"));
            assertFalse(replaced.contains("{count}"));
        }

        @Test
        @DisplayName("报告 Prompt 模板变量替换正确")
        void reportPromptReplacesVariables() {
            String prompt = (String) ReflectionTestUtils.invokeMethod(
                    aiService, "loadPrompt", "prompts/generate-report.txt");

            String replaced = prompt
                    .replace("{content}", "深度学习")
                    .replace("{total}", "10")
                    .replace("{correct}", "8")
                    .replace("{rate}", "80")
                    .replace("{correct_points}", "CNN,RNN")
                    .replace("{wrong_points}", "Transformer");

            assertTrue(replaced.contains("深度学习"));
            assertTrue(replaced.contains("80"));
            assertFalse(replaced.contains("{content}"));
            assertFalse(replaced.contains("{rate}"));
        }
    }
}
