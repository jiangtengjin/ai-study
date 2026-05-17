package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.dto.CreateQuizRequest;
import com.aistudy.dto.SubmitAnswerRequest;
import com.aistudy.entity.Question;
import com.aistudy.entity.QuizAnswer;
import com.aistudy.entity.QuizSession;
import com.aistudy.mapper.QuestionMapper;
import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.mapper.QuizSessionMapper;
import com.aistudy.vo.AnswerResultVO;
import com.aistudy.vo.CreateQuizVO;
import com.aistudy.vo.QuestionVO;
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
@DisplayName("QuizService 单元测试")
class QuizServiceTest {

    @Mock
    private AiService aiService;

    @Mock
    private QuizSessionMapper sessionMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuizAnswerMapper answerMapper;

    @InjectMocks
    private QuizService quizService;

    private QuizSession testSession;
    private Question testQuestion;

    @BeforeEach
    void setUp() {
        testSession = new QuizSession();
        testSession.setId(1L);
        testSession.setKnowledgeContent("机器学习基础知识测试内容");
        testSession.setKnowledgeTitle("机器学习基础");
        testSession.setQuestionCount(5);
        testSession.setDifficulty("medium");
        testSession.setStatus(0);
        testSession.setCorrectCount(0);
        testSession.setScore(0);

        testQuestion = new Question();
        testQuestion.setId(100L);
        testQuestion.setSessionId(1L);
        testQuestion.setQuestionIndex(1);
        testQuestion.setQuestionType("single");
        testQuestion.setDifficulty("medium");
        testQuestion.setQuestionContent("什么是过拟合？");
        testQuestion.setOptionA("模型在训练集上表现好但泛化差");
        testQuestion.setOptionB("模型欠拟合");
        testQuestion.setOptionC("数据量过大");
        testQuestion.setOptionD("学习率过高");
        testQuestion.setCorrectAnswer("A");
        testQuestion.setExplanation("过拟合指模型在训练数据上表现过好");
        testQuestion.setKnowledgePoint("过拟合");
    }

    // ==================== createSession 测试 ====================

    @Nested
    @DisplayName("createSession 测试")
    class CreateSessionTest {

        @Test
        @DisplayName("创建答题会话成功")
        void createSessionSuccess() {
            CreateQuizRequest request = new CreateQuizRequest();
            request.setContent("机器学习基础知识测试内容，需要足够长的内容来通过验证");
            request.setQuestionCount(5);
            request.setDifficulty("medium");

            Map<String, Object> aiResult = Map.of(
                    "title", "机器学习闯关",
                    "questions", List.of(
                            Map.of("question", "Q1", "optionA", "A", "optionB", "B",
                                    "optionC", "C", "optionD", "D", "answer", "A",
                                    "explanation", "解析", "difficulty", "medium", "knowledgePoint", "ML")
                    )
            );

            when(aiService.generateQuestions(anyString(), anyInt())).thenReturn(aiResult);
            when(sessionMapper.insert(any(QuizSession.class))).thenAnswer(invocation -> {
                QuizSession s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            });
            when(questionMapper.insert(any(Question.class))).thenReturn(1);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            CreateQuizVO result = quizService.createSession(request);

            assertNotNull(result);
            assertEquals(1L, result.getSessionId());
            assertEquals("机器学习闯关", result.getTitle());
            assertEquals(1, result.getQuestionCount());
            assertEquals("answering", result.getStatus());

            verify(aiService).generateQuestions(anyString(), eq(5));
            verify(sessionMapper).insert(any(QuizSession.class));
            verify(questionMapper).insert(any(Question.class));
        }

        @Test
        @DisplayName("AI 返回空题目时抛出异常")
        void createSessionAiReturnsEmptyQuestions() {
            CreateQuizRequest request = new CreateQuizRequest();
            request.setContent("机器学习基础知识测试内容，需要足够长的内容来通过验证");
            request.setQuestionCount(5);
            request.setDifficulty("medium");

            Map<String, Object> aiResult = Map.of("title", "测试", "questions", List.of());

            when(aiService.generateQuestions(anyString(), anyInt())).thenReturn(aiResult);
            when(sessionMapper.insert(any(QuizSession.class))).thenAnswer(invocation -> {
                QuizSession s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            });

            assertThrows(BizException.class, () -> quizService.createSession(request));
        }

        @Test
        @DisplayName("AI 返回 null questions 时抛出异常")
        void createSessionAiReturnsNullQuestions() {
            CreateQuizRequest request = new CreateQuizRequest();
            request.setContent("机器学习基础知识测试内容，需要足够长的内容来通过验证");
            request.setQuestionCount(5);
            request.setDifficulty("medium");

            Map<String, Object> aiResult = Map.of("title", "测试");

            when(aiService.generateQuestions(anyString(), anyInt())).thenReturn(aiResult);
            when(sessionMapper.insert(any(QuizSession.class))).thenAnswer(invocation -> {
                QuizSession s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            });

            assertThrows(BizException.class, () -> quizService.createSession(request));
        }

        @Test
        @DisplayName("AI 调用异常时包装为 BizException")
        void createSessionAiCallFails() {
            CreateQuizRequest request = new CreateQuizRequest();
            request.setContent("机器学习基础知识测试内容，需要足够长的内容来通过验证");
            request.setQuestionCount(5);
            request.setDifficulty("medium");

            when(aiService.generateQuestions(anyString(), anyInt()))
                    .thenThrow(new RuntimeException("网络异常"));
            when(sessionMapper.insert(any(QuizSession.class))).thenAnswer(invocation -> {
                QuizSession s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            });

            assertThrows(BizException.class, () -> quizService.createSession(request));
        }
    }

    // ==================== getQuestions 测试 ====================

    @Nested
    @DisplayName("getQuestions 测试")
    class GetQuestionsTest {

        @Test
        @DisplayName("获取题目列表成功，不包含答案")
        void getQuestionsReturnsNoAnswer() {
            List<Question> questions = new ArrayList<>();
            questions.add(testQuestion);

            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(questions);

            List<QuestionVO> result = quizService.getQuestions(1L);

            assertEquals(1, result.size());
            QuestionVO vo = result.get(0);
            assertEquals(100L, vo.getId());
            assertEquals(1, vo.getIndex());
            assertEquals("什么是过拟合？", vo.getQuestionContent());
            assertEquals("模型在训练集上表现好但泛化差", vo.getOptionA());
            // 确保 VO 不包含敏感字段
            boolean hasAnswer = java.util.Arrays.stream(vo.getClass().getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("correctAnswer"));
            assertFalse(hasAnswer, "QuestionVO 不应包含 correctAnswer 字段");
        }

        @Test
        @DisplayName("无题目时返回空列表")
        void getQuestionsReturnsEmptyList() {
            when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<QuestionVO> result = quizService.getQuestions(999L);

            assertTrue(result.isEmpty());
        }
    }

    // ==================== submitAnswer 测试 ====================

    @Nested
    @DisplayName("submitAnswer 测试")
    class SubmitAnswerTest {

        @Test
        @DisplayName("答对题目返回正确结果")
        void submitCorrectAnswer() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectById(100L)).thenReturn(testQuestion);
            when(answerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 1L);
            when(answerMapper.insert(any(QuizAnswer.class))).thenReturn(1);

            // Mock streak 查询：返回刚插入的答题记录
            QuizAnswer streakAnswer = new QuizAnswer();
            streakAnswer.setId(1L);
            streakAnswer.setSessionId(1L);
            streakAnswer.setIsCorrect(1);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(streakAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("A");
            request.setAnswerTimeSeconds(15);

            AnswerResultVO result = quizService.submitAnswer(1L, request);

            assertTrue(result.getIsCorrect());
            assertEquals("A", result.getCorrectAnswer());
            assertEquals("过拟合指模型在训练数据上表现过好", result.getExplanation());
            assertEquals(1, result.getCurrentProgress());
            assertEquals(5, result.getTotalQuestions());
            assertEquals(1, result.getStreak());
        }

        @Test
        @DisplayName("答错题目返回错误结果")
        void submitWrongAnswer() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectById(100L)).thenReturn(testQuestion);
            when(answerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 1L);
            when(answerMapper.insert(any(QuizAnswer.class))).thenReturn(1);

            // Mock streak 查询：返回答错的记录
            QuizAnswer streakAnswer = new QuizAnswer();
            streakAnswer.setId(1L);
            streakAnswer.setSessionId(1L);
            streakAnswer.setIsCorrect(0);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(streakAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("B");
            request.setAnswerTimeSeconds(20);

            AnswerResultVO result = quizService.submitAnswer(1L, request);

            assertFalse(result.getIsCorrect());
            assertEquals("A", result.getCorrectAnswer());
        }

        @Test
        @DisplayName("答案大小写不敏感")
        void submitAnswerCaseInsensitive() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectById(100L)).thenReturn(testQuestion);
            when(answerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L, 1L);
            when(answerMapper.insert(any(QuizAnswer.class))).thenReturn(1);

            QuizAnswer streakAnswer = new QuizAnswer();
            streakAnswer.setId(1L);
            streakAnswer.setSessionId(1L);
            streakAnswer.setIsCorrect(1);
            when(answerMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(streakAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("a");
            request.setAnswerTimeSeconds(10);

            AnswerResultVO result = quizService.submitAnswer(1L, request);

            assertTrue(result.getIsCorrect());
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void submitAnswerSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("A");

            assertThrows(BizException.class, () -> quizService.submitAnswer(999L, request));
        }

        @Test
        @DisplayName("会话已结束时抛出异常")
        void submitAnswerSessionFinished() {
            QuizSession finishedSession = new QuizSession();
            finishedSession.setId(1L);
            finishedSession.setStatus(1);
            when(sessionMapper.selectById(1L)).thenReturn(finishedSession);

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("A");

            assertThrows(BizException.class, () -> quizService.submitAnswer(1L, request));
        }

        @Test
        @DisplayName("题目不属于该会话时抛出异常")
        void submitAnswerQuestionNotInSession() {
            Question otherQuestion = new Question();
            otherQuestion.setId(200L);
            otherQuestion.setSessionId(999L); // 不同的会话

            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectById(200L)).thenReturn(otherQuestion);

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(200L);
            request.setUserAnswer("A");

            assertThrows(BizException.class, () -> quizService.submitAnswer(1L, request));
        }

        @Test
        @DisplayName("重复回答同一题抛出异常")
        void submitAnswerDuplicate() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(questionMapper.selectById(100L)).thenReturn(testQuestion);
            when(answerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // 已答过

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(100L);
            request.setUserAnswer("A");

            assertThrows(BizException.class, () -> quizService.submitAnswer(1L, request));
        }
    }

    // ==================== finishSession 测试 ====================

    @Nested
    @DisplayName("finishSession 测试")
    class FinishSessionTest {

        @Test
        @DisplayName("结束答题成功，计算得分")
        void finishSessionSuccess() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(i < 3 ? 1 : 0); // 3对2错
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            assertEquals(3, testSession.getCorrectCount());
            assertEquals(1, testSession.getStatus());
            assertNotNull(testSession.getFinishedAt());
            assertTrue(testSession.getScore() > 0);
            assertTrue(testSession.getScore() <= 100);
        }

        @Test
        @DisplayName("全部答对得分接近100")
        void finishSessionAllCorrect() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(1);
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            assertEquals(5, testSession.getCorrectCount());
            // 基础分 = 70, 难度分(medium) = 5, 连对分 = 15 → 总分 = 90
            assertEquals(90, testSession.getScore());
        }

        @Test
        @DisplayName("全部答错得分接近0")
        void finishSessionAllWrong() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(0);
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            assertEquals(0, testSession.getCorrectCount());
            // 基础分 = 0, 难度分(medium) = 5, 连对分 = 0 → 总分 = 5
            assertEquals(5, testSession.getScore());
        }

        @Test
        @DisplayName("困难难度加分更多")
        void finishSessionHardDifficulty() {
            testSession.setDifficulty("hard");
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(1);
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            // 基础分 = 70, 难度分(hard) = 10, 连对分 = 15 → 总分 = 95
            assertEquals(95, testSession.getScore());
        }

        @Test
        @DisplayName("简单难度无加分")
        void finishSessionEasyDifficulty() {
            testSession.setDifficulty("easy");
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(1);
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            // 基础分 = 70, 难度分(easy) = 0, 连对分 = 15 → 总分 = 85
            assertEquals(85, testSession.getScore());
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void finishSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> quizService.finishSession(999L));
        }

        @Test
        @DisplayName("已结束的会话不重复处理")
        void finishSessionAlreadyFinished() {
            QuizSession finishedSession = new QuizSession();
            finishedSession.setId(1L);
            finishedSession.setStatus(1);
            when(sessionMapper.selectById(1L)).thenReturn(finishedSession);

            quizService.finishSession(1L);

            verify(answerMapper, never()).selectList(any());
            verify(sessionMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("得分不超过100")
        void finishSessionScoreCapped() {
            testSession.setQuestionCount(3);
            testSession.setDifficulty("hard");
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(sessionMapper.updateById(any(QuizSession.class))).thenReturn(1);

            List<QuizAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                QuizAnswer a = new QuizAnswer();
                a.setId((long) (i + 1));
                a.setSessionId(1L);
                a.setIsCorrect(1);
                answers.add(a);
            }
            when(answerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(answers);

            quizService.finishSession(1L);

            assertTrue(testSession.getScore() <= 100);
        }
    }

    // ==================== getProgress 测试 ====================

    @Nested
    @DisplayName("getProgress 测试")
    class GetProgressTest {

        @Test
        @DisplayName("获取进度成功")
        void getProgressSuccess() {
            when(sessionMapper.selectById(1L)).thenReturn(testSession);
            when(answerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

            Map<String, Object> progress = quizService.getProgress(1L);

            assertEquals(1L, progress.get("sessionId"));
            assertEquals(5, progress.get("totalQuestions"));
            assertEquals(3, progress.get("answeredQuestions"));
            assertEquals(0, progress.get("status"));
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void getProgressSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> quizService.getProgress(999L));
        }
    }

    // ==================== toQuestionVO 安全性测试 ====================

    @Nested
    @DisplayName("QuestionVO 安全性测试")
    class QuestionVOSecurityTest {

        @Test
        @DisplayName("QuestionVO 不包含正确答案、解析、知识点字段")
        void questionVODoesNotExposeAnswer() {
            when(questionMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(testQuestion));

            List<QuestionVO> result = quizService.getQuestions(1L);

            assertEquals(1, result.size());
            QuestionVO vo = result.get(0);
            // 验证 VO 包含题目内容和选项
            assertNotNull(vo.getQuestionContent());
            assertNotNull(vo.getOptionA());
            // 通过反射确认没有敏感字段
            boolean hasCorrectAnswer = java.util.Arrays.stream(vo.getClass().getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("correctAnswer"));
            boolean hasExplanation = java.util.Arrays.stream(vo.getClass().getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("explanation"));
            boolean hasKnowledgePoint = java.util.Arrays.stream(vo.getClass().getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("knowledgePoint"));
            assertFalse(hasCorrectAnswer, "QuestionVO 不应包含 correctAnswer 字段");
            assertFalse(hasExplanation, "QuestionVO 不应包含 explanation 字段");
            assertFalse(hasKnowledgePoint, "QuestionVO 不应包含 knowledgePoint 字段");
        }
    }
}
