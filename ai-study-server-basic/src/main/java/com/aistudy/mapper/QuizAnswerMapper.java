package com.aistudy.mapper;

import com.aistudy.entity.QuizAnswer;
import com.aistudy.vo.TrendStatsVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuizAnswerMapper extends BaseMapper<QuizAnswer> {

    /**
     * 按天统计用户学习数据
     */
    @Select("SELECT " +
            "DATE_FORMAT(qa.created_at, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS totalQuestions, " +
            "SUM(qa.is_correct) AS correctAnswers, " +
            "ROUND(SUM(qa.is_correct) * 100.0 / COUNT(*), 2) AS accuracy, " +
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration " +
            "FROM t_quiz_answer qa " +
            "WHERE qa.user_id = #{userId} " +
            "AND qa.created_at >= #{startDate} " +
            "GROUP BY DATE_FORMAT(qa.created_at, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<TrendStatsVO> getStatsByDay(@Param("userId") Long userId,
                                     @Param("startDate") String startDate);

    /**
     * 按周统计用户学习数据
     */
    @Select("SELECT " +
            "CONCAT(YEAR(qa.created_at), '-W', LPAD(WEEK(qa.created_at, 1), 2, '0')) AS date, " +
            "COUNT(*) AS totalQuestions, " +
            "SUM(qa.is_correct) AS correctAnswers, " +
            "ROUND(SUM(qa.is_correct) * 100.0 / COUNT(*), 2) AS accuracy, " +
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration " +
            "FROM t_quiz_answer qa " +
            "WHERE qa.user_id = #{userId} " +
            "AND qa.created_at >= #{startDate} " +
            "GROUP BY YEAR(qa.created_at), WEEK(qa.created_at, 1) " +
            "ORDER BY date ASC")
    List<TrendStatsVO> getStatsByWeek(@Param("userId") Long userId,
                                      @Param("startDate") String startDate);

    /**
     * 按月统计用户学习数据
     */
    @Select("SELECT " +
            "DATE_FORMAT(qa.created_at, '%Y-%m') AS date, " +
            "COUNT(*) AS totalQuestions, " +
            "SUM(qa.is_correct) AS correctAnswers, " +
            "ROUND(SUM(qa.is_correct) * 100.0 / COUNT(*), 2) AS accuracy, " +
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration " +
            "FROM t_quiz_answer qa " +
            "WHERE qa.user_id = #{userId} " +
            "AND qa.created_at >= #{startDate} " +
            "GROUP BY DATE_FORMAT(qa.created_at, '%Y-%m') " +
            "ORDER BY date ASC")
    List<TrendStatsVO> getStatsByMonth(@Param("userId") Long userId,
                                       @Param("startDate") String startDate);
}
