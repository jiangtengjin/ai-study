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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final QuizSessionMapper sessionMapper;
    private final QuizAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;

    /**
     * 获取用户学习统计
     */
    public ProfileStatsVO getStats(Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ProfileStatsVO.builder()
                    .totalQuizzes(0)
                    .totalQuestions(0)
                    .totalCorrect(0)
                    .correctRate(0.0)
                    .streakDays(0)
                    .averageScore(0)
                    .build();
        }

        // 计算平均分
        Double avgScore = sessionMapper.selectAvgScoreByUserId(userId);

        double correctRate = user.getTotalQuestions() > 0
                ? (double) user.getTotalCorrect() / user.getTotalQuestions() * 100
                : 0;

        return ProfileStatsVO.builder()
                .totalQuizzes(user.getTotalQuizzes())
                .totalQuestions(user.getTotalQuestions())
                .totalCorrect(user.getTotalCorrect())
                .correctRate(Math.round(correctRate * 10.0) / 10.0)
                .streakDays(user.getStreakDays())
                .averageScore(avgScore != null ? avgScore.intValue() : 0)
                .build();
    }

    /**
     * 获取历史记录（分页）
     */
    public Page<HistoryVO> getHistory(Long userId, int page, int size, String filter) {
        Page<QuizSession> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<QuizSession> wrapper = new LambdaQueryWrapper<QuizSession>()
                .eq(QuizSession::getUserId, userId)
                .eq(QuizSession::getStatus, 1) // 已完成
                .orderByDesc(QuizSession::getCreatedAt);

        // 筛选条件
        if ("high".equals(filter)) {
            wrapper.ge(QuizSession::getScore, 80);
        } else if ("low".equals(filter)) {
            wrapper.lt(QuizSession::getScore, 60);
        }

        Page<QuizSession> sessionPage = sessionMapper.selectPage(pageParam, wrapper);

        // 转换为 VO
        Page<HistoryVO> result = new Page<>(page, size, sessionPage.getTotal());
        result.setRecords(sessionPage.getRecords().stream()
                .map(this::convertToHistoryVO)
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * 获取错题本（分页）
     */
    public Page<WrongBookVO> getWrongQuestions(Long userId, int page, int size) {
        // 查询用户答错的记录
        Page<QuizAnswer> pageParam = new Page<>(page, size);
        Page<QuizAnswer> answerPage = answerMapper.selectPage(pageParam,
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getUserId, userId)
                        .eq(QuizAnswer::getIsCorrect, 0)
                        .orderByDesc(QuizAnswer::getCreatedAt));

        Page<WrongBookVO> result = new Page<>(page, size, answerPage.getTotal());
        result.setRecords(answerPage.getRecords().stream()
                .map(answer -> {
                    Question question = questionMapper.selectById(answer.getQuestionId());
                    QuizSession session = sessionMapper.selectById(answer.getSessionId());
                    return convertToWrongBookVO(answer, question, session);
                })
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * 移除错题（标记为已掌握）
     */
    public void removeWrongQuestion(Long userId, Long answerId) {
        QuizAnswer answer = answerMapper.selectById(answerId);
        if (answer != null && answer.getUserId().equals(userId)) {
            answerMapper.deleteById(answerId);
        }
    }

    private HistoryVO convertToHistoryVO(QuizSession session) {
        return HistoryVO.builder()
                .sessionId(session.getId())
                .title(session.getKnowledgeTitle())
                .questionCount(session.getQuestionCount())
                .correctCount(session.getCorrectCount())
                .score(session.getScore())
                .durationSeconds(session.getDurationSeconds())
                .difficulty(session.getDifficulty())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private WrongBookVO convertToWrongBookVO(QuizAnswer answer, Question question, QuizSession session) {
        if (question == null) {
            return WrongBookVO.builder()
                    .answerId(answer.getId())
                    .sessionId(answer.getSessionId())
                    .questionId(answer.getQuestionId())
                    .userAnswer(answer.getUserAnswer())
                    .createdAt(answer.getCreatedAt())
                    .build();
        }

        return WrongBookVO.builder()
                .answerId(answer.getId())
                .sessionId(answer.getSessionId())
                .sessionTitle(session != null ? session.getKnowledgeTitle() : null)
                .questionId(question.getId())
                .questionIndex(question.getQuestionIndex())
                .questionContent(question.getQuestionContent())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctAnswer(question.getCorrectAnswer())
                .userAnswer(answer.getUserAnswer())
                .explanation(question.getExplanation())
                .knowledgePoint(question.getKnowledgePoint())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
