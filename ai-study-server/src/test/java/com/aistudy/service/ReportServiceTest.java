package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.Question;
import com.aistudy.entity.QuizAnswer;
import com.aistudy.entity.QuizSession;
import com.aistudy.mapper.QuestionMapper;
import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.mapper.QuizSessionMapper;
import com.aistudy.vo.ReportVO;
import com.aistudy.vo.WrongQuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 单元测试")
class ReportServiceTest {

    @Mock
    private AiService aiService;

    @Mock
    private QuizSessionMapper sessionMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuizAnswerMapper answerMapper;

    @InjectMocks
    private ReportService reportService;

    private QuizSession testSession;
    private List<Question> testQuestions;
    private List<QuizAnswer> testAnswers;

    @BeforeEach
    void setUp() {
        testSession = new QuizSession();
        testSession.setId(1L);
        testSession.setKnowledgeContent("机器学习基础知识");
        testSession.setKnowledgeTitle("机器学习基础");
        testSession.setQuestionCount(5);
        testSession.setCorrectCount(4);
        testSession.setScore(85);
        testSession.setDurationSeconds(300);
        testSession.setDifficulty("medium");
        testSession.setStatus(1);

        testQuestions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setId((long) (100 + i));
            q.setSessionId(1L);
            q.setQuestionIndex(i);
            q.setQuestionContent("问题" + i);
            q.setCorrectAnswer("A");
            q.setExplanation("解析" + i);
            q.setKnowledgePoint("知识点" + i);
            testQuestions.add(q);
        }

        testAnswers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            QuizAnswer a = new QuizAnswer();
            a.setId((long) (i + 1));
            a.setSessionId(1L);
            a.setQuestionId(101L + i);
            a.setUserAnswer(i < 4 ? "A" : "B"); // 前4题对，第5题错
            a.setIsCorrect(i < 4 ? 1 : 0);
            testAnswers.add(a);
        }
    }

    @Nested
    @DisplayName("generateReport 测试")
    class GenerateReportTest {

        @Test
        @DisplayName("生成报告成功")
        void generateReportSuccess() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);

            Map<String, Object> aiResult = Map.of(
                    "summary", "AI 生成的知识总结",
                    "strengths", List.of("掌握知识点A"),
                    "weaknesses", List.of("需加强知识点B")
            );
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(aiResult);

            ReportVO report = reportService.generateReport(1L);

            assertNotNull(report);
            assertEquals(1L, report.getSessionId());
            assertEquals("机器学习基础", report.getTitle());
            assertEquals(85, report.getScore());
            assertEquals(4, report.getCorrectCount());
            assertEquals(5, report.getQuestionCount());
            assertEquals(300, report.getDurationSeconds());
            assertEquals("AI 生成的知识总结", report.getKnowledgeSummary());
            assertNotNull(report.getWrongQuestions());
            assertEquals(1, report.getWrongQuestions().size());
            assertEquals("掌握知识点A", report.getStrengthPoints().get(0));
            assertEquals("需加强知识点B", report.getWeakPoints().get(0));
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void generateReportSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> reportService.generateReport(999L));
        }

        @Test
        @DisplayName("会话未结束时抛出异常")
        void generateReportSessionNotFinished() {
            QuizSession activeSession = new QuizSession();
            activeSession.setId(1L);
            activeSession.setStatus(0);
            when(sessionMapper.selectById(1L)).thenReturn(activeSession);

            assertThrows(BizException.class, () -> reportService.generateReport(1L));
        }

        @Test
        @DisplayName("AI 总结失败时使用默认总结")
        void generateReportAiFailsUsesDefault() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenThrow(new RuntimeException("AI 调用失败"));

            ReportVO report = reportService.generateReport(1L);

            assertNotNull(report);
            assertTrue(report.getKnowledgeSummary().contains("机器学习基础"));
            assertTrue(report.getKnowledgeSummary().contains("5"));
            assertTrue(report.getKnowledgeSummary().contains("4"));
        }

        @Test
        @DisplayName("错题信息正确")
        void generateReportWrongQuestionsCorrect() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            WrongQuestionVO wrong = report.getWrongQuestions().get(0);
            assertEquals(5, wrong.getQuestionIndex());
            assertEquals("问题5", wrong.getQuestionContent());
            assertEquals("A", wrong.getCorrectAnswer());
            assertEquals("B", wrong.getUserAnswer());
            assertEquals("解析5", wrong.getExplanation());
        }

        @Test
        @DisplayName("全部答对时无错题")
        void generateReportAllCorrect() {
            List<QuizAnswer> allCorrect = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setQuestionId(101L + i);
                a.setUserAnswer("A");
                a.setIsCorrect(1);
                allCorrect.add(a);
            }

            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allCorrect);
            // 不返回 strengths/weaknesses，使用计算值
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertTrue(report.getWrongQuestions().isEmpty());
            // 每道题有不同知识点，全部答对 → 5个strength
            assertEquals(5, report.getStrengthPoints().size());
            assertTrue(report.getWeakPoints().isEmpty());
        }
    }

    @Nested
    @DisplayName("评级测试")
    class RatingTest {

        @Test
        @DisplayName("90分以上评级为学霸")
        void rating90Plus() {
            testSession.setScore(95);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertEquals("学霸", report.getRating());
        }

        @Test
        @DisplayName("80-89分评级为知识达人")
        void rating80to89() {
            testSession.setScore(85);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertEquals("知识达人", report.getRating());
        }

        @Test
        @DisplayName("60-79分评级为学习能手")
        void rating60to79() {
            testSession.setScore(70);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertEquals("学习能手", report.getRating());
        }

        @Test
        @DisplayName("40-59分评级为进步青年")
        void rating40to59() {
            testSession.setScore(50);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertEquals("进步青年", report.getRating());
        }

        @Test
        @DisplayName("40分以下评级为加油")
        void ratingBelow40() {
            testSession.setScore(30);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testQuestions);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testAnswers);
            when(aiService.generateReportSummary(anyString(), anyInt(), anyInt(), anyList(), anyList()))
                    .thenReturn(Map.of("summary", "总结"));

            ReportVO report = reportService.generateReport(1L);

            assertEquals("加油", report.getRating());
        }
    }
}
