package com.aistudy.controller;

import com.aistudy.service.TrendStatsService;
import com.aistudy.vo.TrendStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrendStatsController.class)
@DisplayName("TrendStatsController 集成测试")
class TrendStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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

    @Test
    @DisplayName("获取学习趋势数据 - 成功")
    void getTrendStats_success() throws Exception {
        when(trendStatsService.getTrendStats(anyLong(), eq("day"), eq(7)))
                .thenReturn(mockStatsData);

        mockMvc.perform(get("/api/v1/trend/stats")
                        .param("period", "day")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].date").value("2025-01-15"))
                .andExpect(jsonPath("$.data[0].totalQuestions").value(10))
                .andExpect(jsonPath("$.data[0].correctAnswers").value(8))
                .andExpect(jsonPath("$.data[0].accuracy").value(80.0))
                .andExpect(jsonPath("$.data[0].studyDuration").value(15));
    }

    @Test
    @DisplayName("获取学习趋势数据 - 使用默认参数")
    void getTrendStats_defaultParams() throws Exception {
        when(trendStatsService.getTrendStats(anyLong(), eq("day"), eq(7)))
                .thenReturn(mockStatsData);

        mockMvc.perform(get("/api/v1/trend/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取学习趋势数据 - 按周统计")
    void getTrendStats_byWeek() throws Exception {
        when(trendStatsService.getTrendStats(anyLong(), eq("week"), eq(30)))
                .thenReturn(mockStatsData);

        mockMvc.perform(get("/api/v1/trend/stats")
                        .param("period", "week")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取学习趋势数据 - 按月统计")
    void getTrendStats_byMonth() throws Exception {
        when(trendStatsService.getTrendStats(anyLong(), eq("month"), eq(90)))
                .thenReturn(mockStatsData);

        mockMvc.perform(get("/api/v1/trend/stats")
                        .param("period", "month")
                        .param("days", "90")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取学习趋势数据 - 空数据")
    void getTrendStats_emptyData() throws Exception {
        when(trendStatsService.getTrendStats(anyLong(), eq("day"), eq(7)))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/trend/stats")
                        .param("period", "day")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
