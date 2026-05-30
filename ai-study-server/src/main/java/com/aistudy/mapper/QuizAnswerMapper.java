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

    // 积分计算 SQL 片段，与 PointCalculator 使用相同规则
    String POINTS_CASE = "SUM(CASE WHEN q.difficulty = 'easy' THEN (CASE WHEN qa.is_correct = 1 THEN 10 ELSE 2 END) "
            + "WHEN q.difficulty = 'hard' THEN (CASE WHEN qa.is_correct = 1 THEN 35 ELSE 8 END) "
            + "ELSE (CASE WHEN qa.is_correct = 1 THEN 20 ELSE 5 END) END) AS pointsEarned";

    /**
     * 按天统计用户学习数据
     */
    @Select("SELECT " +
            "DATE_FORMAT(qa.created_at, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS totalQuestions, " +
            "SUM(qa.is_correct) AS correctAnswers, " +
            "ROUND(SUM(qa.is_correct) * 100.0 / COUNT(*), 2) AS accuracy, " +
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration, " +
            POINTS_CASE + " " +
            "FROM t_quiz_answer qa " +
            "JOIN t_question q ON qa.question_id = q.id " +
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
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration, " +
            POINTS_CASE + " " +
            "FROM t_quiz_answer qa " +
            "JOIN t_question q ON qa.question_id = q.id " +
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
            "SUM(qa.answer_time_seconds) DIV 60 AS studyDuration, " +
            POINTS_CASE + " " +
            "FROM t_quiz_answer qa " +
            "JOIN t_question q ON qa.question_id = q.id " +
            "WHERE qa.user_id = #{userId} " +
            "AND qa.created_at >= #{startDate} " +
            "GROUP BY DATE_FORMAT(qa.created_at, '%Y-%m') " +
            "ORDER BY date ASC")
    List<TrendStatsVO> getStatsByMonth(@Param("userId") Long userId,
                                       @Param("startDate") String startDate);
}
