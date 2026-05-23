package com.aistudy.service;

import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.vo.TrendStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendStatsService 单元测试")
class TrendStatsServiceTest {

    @Mock
    private QuizAnswerMapper quizAnswerMapper;

    @InjectMocks
    private TrendStatsService trendStatsService;

    private List<TrendStatsVO> mockStatsData;

    @BeforeEach
    void setUp() {
        mockStatsData = Arrays.asList(
                TrendStatsVO.builder()
                        .date("2025-01-15")
                        .totalQuestions(10)
                        .correctAnswers(8)
                        .accuracy(80.0)
                        .studyDuration(15)
                        .build(),
                TrendStatsVO.builder()
                        .date("2025-01-16")
                        .totalQuestions(15)
                        .correctAnswers(12)
                        .accuracy(80.0)
                        .studyDuration(20)
                        .build()
        );
    }

    @Nested
    @DisplayName("参数验证测试")
    class ValidationTests {

        @Test
        @DisplayName("无效的 period 参数")
        void getTrendStats_invalidPeriod() {
            assertThrows(IllegalArgumentException.class, () -> {
                trendStatsService.getTrendStats(1L, "invalid", 7);
            });
        }

        @Test
        @DisplayName("无效的 days 参数")
        void getTrendStats_invalidDays() {
            assertThrows(IllegalArgumentException.class, () -> {
                trendStatsService.getTrendStats(1L, "day", 100);
            });
        }

        @Test
        @DisplayName("null 的 period 参数")
        void getTrendStats_nullPeriod() {
            assertThrows(IllegalArgumentException.class, () -> {
                trendStatsService.getTrendStats(1L, null, 7);
            });
        }

        @Test
        @DisplayName("null 的 days 参数")
        void getTrendStats_nullDays() {
            assertThrows(IllegalArgumentException.class, () -> {
                trendStatsService.getTrendStats(1L, "day", null);
            });
        }
    }

    @Nested
    @DisplayName("按天统计测试")
    class DayStatsTests {

        @Test
        @DisplayName("获取7天统计数据")
        void getStatsByDay_7days() {
            when(quizAnswerMapper.getStatsByDay(eq(1L), anyString()))
                    .thenReturn(mockStatsData);

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "day", 7);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("2025-01-15", result.get(0).getDate());
            assertEquals(10, result.get(0).getTotalQuestions());
            verify(quizAnswerMapper).getStatsByDay(eq(1L), anyString());
        }

        @Test
        @DisplayName("获取30天统计数据")
        void getStatsByDay_30days() {
            when(quizAnswerMapper.getStatsByDay(eq(1L), anyString()))
                    .thenReturn(mockStatsData);

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "day", 30);

            assertNotNull(result);
            verify(quizAnswerMapper).getStatsByDay(eq(1L), anyString());
        }

        @Test
        @DisplayName("获取90天统计数据")
        void getStatsByDay_90days() {
            when(quizAnswerMapper.getStatsByDay(eq(1L), anyString()))
                    .thenReturn(mockStatsData);

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "day", 90);

            assertNotNull(result);
            verify(quizAnswerMapper).getStatsByDay(eq(1L), anyString());
        }
    }

    @Nested
    @DisplayName("按周统计测试")
    class WeekStatsTests {

        @Test
        @DisplayName("获取按周统计数据")
        void getStatsByWeek() {
            when(quizAnswerMapper.getStatsByWeek(eq(1L), anyString()))
                    .thenReturn(mockStatsData);

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "week", 30);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(quizAnswerMapper).getStatsByWeek(eq(1L), anyString());
        }
    }

    @Nested
    @DisplayName("按月统计测试")
    class MonthStatsTests {

        @Test
        @DisplayName("获取按月统计数据")
        void getStatsByMonth() {
            when(quizAnswerMapper.getStatsByMonth(eq(1L), anyString()))
                    .thenReturn(mockStatsData);

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "month", 90);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(quizAnswerMapper).getStatsByMonth(eq(1L), anyString());
        }
    }

    @Nested
    @DisplayName("空数据测试")
    class EmptyDataTests {

        @Test
        @DisplayName("无数据时返回空列表")
        void getTrendStats_emptyData() {
            when(quizAnswerMapper.getStatsByDay(eq(1L), anyString()))
                    .thenReturn(Arrays.asList());

            List<TrendStatsVO> result = trendStatsService.getTrendStats(1L, "day", 7);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
