package com.aistudy.service;

import com.aistudy.entity.Question;
import com.aistudy.entity.QuizAnswer;
import com.aistudy.entity.QuizSession;
import com.aistudy.entity.User;
import com.aistudy.mapper.QuestionMapper;
import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.mapper.QuizSessionMapper;
import com.aistudy.vo.HistoryVO;
import com.aistudy.vo.ProfileStatsVO;
import com.aistudy.vo.WrongBookVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService 单元测试")
class ProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private QuizSessionMapper sessionMapper;

    @Mock
    private QuizAnswerMapper answerMapper;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;
    private QuizSession testSession;
    private Question testQuestion;
    private QuizAnswer testAnswer;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("测试用户");
        testUser.setTotalQuizzes(10);
        testUser.setTotalCorrect(80);
        testUser.setTotalQuestions(100);
        testUser.setStreakDays(5);
        testUser.setLastStudyDate(LocalDate.now());

        testSession = new QuizSession();
        testSession.setId(1L);
        testSession.setUserId(1L);
        testSession.setKnowledgeTitle("机器学习基础");
        testSession.setQuestionCount(10);
        testSession.setCorrectCount(8);
        testSession.setScore(80);
        testSession.setDurationSeconds(300);
        testSession.setDifficulty("medium");
        testSession.setStatus(1);
        testSession.setCreatedAt(LocalDateTime.now().minusDays(1));

        testQuestion = new Question();
        testQuestion.setId(100L);
        testQuestion.setSessionId(1L);
        testQuestion.setQuestionIndex(1);
        testQuestion.setQuestionContent("什么是过拟合？");
        testQuestion.setOptionA("模型在训练集上表现好但泛化差");
        testQuestion.setOptionB("模型欠拟合");
        testQuestion.setOptionC("数据量过大");
        testQuestion.setOptionD("学习率过高");
        testQuestion.setCorrectAnswer("A");
        testQuestion.setExplanation("过拟合指模型在训练数据上表现过好");
        testQuestion.setKnowledgePoint("过拟合");

        testAnswer = new QuizAnswer();
        testAnswer.setId(1L);
        testAnswer.setSessionId(1L);
        testAnswer.setQuestionId(100L);
        testAnswer.setUserId(1L);
        testAnswer.setUserAnswer("B");
        testAnswer.setIsCorrect(0);
        testAnswer.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Nested
    @DisplayName("学习统计测试")
    class StatsTests {

        @Test
        @DisplayName("获取用户学习统计")
        void getStats_success() {
            when(userService.findById(1L)).thenReturn(testUser);
            when(sessionMapper.selectAvgScoreByUserId(1L)).thenReturn(75.5);

            ProfileStatsVO stats = profileService.getStats(1L);

            assertNotNull(stats);
            assertEquals(10, stats.getTotalQuizzes());
            assertEquals(100, stats.getTotalQuestions());
            assertEquals(80, stats.getTotalCorrect());
            assertEquals(80.0, stats.getCorrectRate());
            assertEquals(5, stats.getStreakDays());
            assertEquals(75, stats.getAverageScore());
        }

        @Test
        @DisplayName("用户不存在时返回默认值")
        void getStats_userNotFound() {
            when(userService.findById(999L)).thenReturn(null);

            ProfileStatsVO stats = profileService.getStats(999L);

            assertNotNull(stats);
            assertEquals(0, stats.getTotalQuizzes());
        }
    }

    @Nested
    @DisplayName("历史记录测试")
    class HistoryTests {

        @Test
        @DisplayName("获取历史记录")
        void getHistory_success() {
            Page<QuizSession> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testSession));
            mockPage.setTotal(1);

            when(sessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<HistoryVO> result = profileService.getHistory(1L, 1, 10, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("机器学习基础", result.getRecords().get(0).getTitle());
        }

        @Test
        @DisplayName("筛选高分记录")
        void getHistory_filterHigh() {
            Page<QuizSession> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testSession));
            mockPage.setTotal(1);

            when(sessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<HistoryVO> result = profileService.getHistory(1L, 1, 10, "high");

            assertNotNull(result);
            verify(sessionMapper).selectPage(any(), any());
        }
    }

    @Nested
    @DisplayName("错题本测试")
    class WrongBookTests {

        @Test
        @DisplayName("获取错题本")
        void getWrongQuestions_success() {
            Page<QuizAnswer> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testAnswer));
            mockPage.setTotal(1);

            when(answerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(questionMapper.selectById(100L)).thenReturn(testQuestion);
            when(sessionMapper.selectById(1L)).thenReturn(testSession);

            Page<WrongBookVO> result = profileService.getWrongQuestions(1L, 1, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            WrongBookVO wrongQuestion = result.getRecords().get(0);
            assertEquals("什么是过拟合？", wrongQuestion.getQuestionContent());
            assertEquals("A", wrongQuestion.getCorrectAnswer());
            assertEquals("B", wrongQuestion.getUserAnswer());
        }

        @Test
        @DisplayName("移除错题")
        void removeWrongQuestion_success() {
            when(answerMapper.selectById(1L)).thenReturn(testAnswer);
            when(answerMapper.deleteById(1L)).thenReturn(1);

            profileService.removeWrongQuestion(1L, 1L);

            verify(answerMapper).deleteById(1L);
        }

        @Test
        @DisplayName("移除错题 - 不能移除其他用户的错题")
        void removeWrongQuestion_notOwner() {
            QuizAnswer otherUserAnswer = new QuizAnswer();
            otherUserAnswer.setId(2L);
            otherUserAnswer.setUserId(2L);

            when(answerMapper.selectById(2L)).thenReturn(otherUserAnswer);

            profileService.removeWrongQuestion(1L, 2L);

            verify(answerMapper, never()).deleteById(anyLong());
        }
    }
}
