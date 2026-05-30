package com.aistudy.service;

import com.aistudy.entity.*;
import com.aistudy.mapper.LeagueMemberMapper;
import com.aistudy.mapper.UserMapper;
import com.aistudy.mapper.WeeklyScoreMapper;
import com.aistudy.mapper.QuestionMapper;
import com.aistudy.mapper.QuizAnswerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final PointCalculator pointCalculator;
    private final UserMapper userMapper;
    private final WeeklyScoreMapper weeklyScoreMapper;
    private final LeagueMemberMapper leagueMemberMapper;
    private final QuestionMapper questionMapper;
    private final QuizAnswerMapper quizAnswerMapper;

    /**
     * 计算会话总积分并双写
     */
    @Transactional
    public int calculateAndSaveSessionPoints(Long userId, Long sessionId) {
        // 幂等检查：如果该会话已计算过积分，跳过
        Long existCount = weeklyScoreMapper.selectCount(
                new LambdaQueryWrapper<WeeklyScore>()
                        .eq(WeeklyScore::getSessionId, sessionId));
        if (existCount > 0) {
            return 0;
        }

        List<QuizAnswer> answers = quizAnswerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId));

        java.util.Map<Long, Question> questionMap = questions.stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));

        int sessionPoints = 0;
        for (QuizAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;
            boolean isCorrect = answer.getIsCorrect() != null && answer.getIsCorrect() == 1;
            sessionPoints += pointCalculator.calculate(question.getDifficulty(), isCorrect);
        }

        // 原子更新累计总积分
        if (sessionPoints > 0) {
            userMapper.addPoints(userId, sessionPoints);
        }

        // 写入周积分明细
        LocalDate weekStart = getWeekStartDate(LocalDate.now());
        WeeklyScore weeklyScore = new WeeklyScore();
        weeklyScore.setUserId(userId);
        weeklyScore.setSessionId(sessionId);
        weeklyScore.setPoints(sessionPoints);
        weeklyScore.setWeekStartDate(weekStart);
        weeklyScoreMapper.insert(weeklyScore);

        // 更新联赛小组成员的周积分
        updateUserWeeklyPoints(userId, sessionPoints);

        return sessionPoints;
    }

    /**
     * 更新用户在当前联赛小组中的周积分
     */
    private void updateUserWeeklyPoints(Long userId, int points) {
        LocalDate weekStart = getWeekStartDate(LocalDate.now());
        LeagueMember member = leagueMemberMapper.selectOne(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getUserId, userId)
                        .apply("group_id IN (SELECT id FROM t_league_group WHERE week_start_date = {0})", weekStart));
        if (member != null) {
            member.setWeeklyPoints(member.getWeeklyPoints() + points);
            leagueMemberMapper.updateById(member);
        }
    }

    /**
     * 获取本周积分
     */
    public int getWeeklyPoints(Long userId) {
        LocalDate weekStart = getWeekStartDate(LocalDate.now());
        List<WeeklyScore> scores = weeklyScoreMapper.selectList(
                new LambdaQueryWrapper<WeeklyScore>()
                        .eq(WeeklyScore::getUserId, userId)
                        .eq(WeeklyScore::getWeekStartDate, weekStart));
        return scores.stream().mapToInt(WeeklyScore::getPoints).sum();
    }

    /**
     * 获取累计总积分
     */
    public long getTotalPoints(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
    }

    /**
     * 获取本周起始日期（周一）
     */
    public LocalDate getWeekStartDate(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
}
