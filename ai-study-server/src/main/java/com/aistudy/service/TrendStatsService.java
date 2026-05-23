package com.aistudy.service;

import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.vo.TrendStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendStatsService {

    private final QuizAnswerMapper quizAnswerMapper;

    private static final List<Integer> VALID_DAYS = Arrays.asList(7, 30, 90);
    private static final List<String> VALID_PERIODS = Arrays.asList("day", "week", "month");

    /**
     * 获取学习趋势数据
     *
     * @param userId 用户ID
     * @param period 统计维度：day/week/month
     * @param days   统计天数：7/30/90
     * @return 趋势统计数据
     */
    public List<TrendStatsVO> getTrendStats(Long userId, String period, Integer days) {
        validateParams(period, days);

        String startDate = calculateStartDate(days);

        switch (period) {
            case "day":
                return quizAnswerMapper.getStatsByDay(userId, startDate);
            case "week":
                return quizAnswerMapper.getStatsByWeek(userId, startDate);
            case "month":
                return quizAnswerMapper.getStatsByMonth(userId, startDate);
            default:
                throw new IllegalArgumentException("无效的统计维度: " + period);
        }
    }

    /**
     * 验证参数
     */
    private void validateParams(String period, Integer days) {
        if (period == null || !VALID_PERIODS.contains(period)) {
            throw new IllegalArgumentException("period 参数无效，可选值: day, week, month");
        }
        if (days == null || !VALID_DAYS.contains(days)) {
            throw new IllegalArgumentException("days 参数无效，可选值: 7, 30, 90");
        }
    }

    /**
     * 计算起始日期
     */
    private String calculateStartDate(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
