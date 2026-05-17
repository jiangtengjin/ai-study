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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final AiService aiService;
    private final QuizSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final QuizAnswerMapper answerMapper;

    /**
     * 创建答题会话：输入知识内容 → AI 出题 → 存储题目
     */
    @Transactional
    public CreateQuizVO createSession(CreateQuizRequest request) {
        // 1. 创建会话
        QuizSession session = new QuizSession();
        session.setKnowledgeContent(request.getContent());
        session.setQuestionCount(request.getQuestionCount());
        session.setDifficulty(request.getDifficulty());
        session.setStatus(0); // 进行中
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        // 2. 调用 AI 生成题目
        try {
            Map<String, Object> result = aiService.generateQuestions(
                    request.getContent(), request.getQuestionCount());

            String title = (String) result.getOrDefault("title", "知识闯关");
            session.setKnowledgeTitle(title);

            // 3. 解析并存储题目
            List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
            if (questions == null || questions.isEmpty()) {
                throw new BizException(1001, "AI 未能生成有效题目，请修改内容后重试");
            }

            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                Question question = new Question();
                question.setSessionId(session.getId());
                question.setQuestionIndex(i + 1);
                question.setQuestionType("single");
                question.setDifficulty(getStringValue(q, "difficulty", "medium"));
                question.setQuestionContent(getStringValue(q, "question", ""));
                question.setOptionA(getStringValue(q, "optionA", ""));
                question.setOptionB(getStringValue(q, "optionB", ""));
                question.setOptionC(getStringValue(q, "optionC", ""));
                question.setOptionD(getStringValue(q, "optionD", ""));
                question.setCorrectAnswer(getStringValue(q, "answer", "A"));
                question.setExplanation(getStringValue(q, "explanation", ""));
                question.setKnowledgePoint(getStringValue(q, "knowledgePoint", ""));
                question.setCreatedAt(LocalDateTime.now());
                questionMapper.insert(question);
            }

            // 更新会话题目数和标题
            session.setQuestionCount(questions.size());
            sessionMapper.updateById(session);

            return CreateQuizVO.builder()
                    .sessionId(session.getId())
                    .title(title)
                    .questionCount(questions.size())
                    .status("answering")
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建答题会话失败", e);
            throw new BizException(1001, "AI 生成题目失败，请稍后重试");
        }
    }

    /**
     * 获取会话下的所有题目（不含答案）
     */
    public List<QuestionVO> getQuestions(Long sessionId) {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId)
                        .orderByAsc(Question::getQuestionIndex));

        return questions.stream().map(this::toQuestionVO).toList();
    }

    /**
     * 获取指定序号的题目（不含答案）
     */
    public QuestionVO getQuestion(Long sessionId, int index) {
        Question question = questionMapper.selectOne(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId)
                        .eq(Question::getQuestionIndex, index));

        if (question == null) {
            throw new BizException(404, "题目不存在");
        }
        return toQuestionVO(question);
    }

    /**
     * 提交单题答案
     */
    @Transactional
    public AnswerResultVO submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        // 校验会话
        QuizSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(1003, "答题会话不存在");
        }
        if (session.getStatus() != 0) {
            throw new BizException(1003, "答题会话已结束");
        }

        // 获取题目
        Question question = questionMapper.selectById(request.getQuestionId());
        if (question == null || !question.getSessionId().equals(sessionId)) {
            throw new BizException(1003, "题目不存在");
        }

        // 判断是否已答过
        Long existCount = answerMapper.selectCount(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId)
                        .eq(QuizAnswer::getQuestionId, request.getQuestionId()));
        if (existCount > 0) {
            throw new BizException(1003, "该题已回答过");
        }

        // 判断对错
        boolean isCorrect = question.getCorrectAnswer()
                .equalsIgnoreCase(request.getUserAnswer().trim());

        // 保存答题记录
        QuizAnswer answer = new QuizAnswer();
        answer.setSessionId(sessionId);
        answer.setQuestionId(request.getQuestionId());
        answer.setUserId(0L); // MVP 无用户体系
        answer.setUserAnswer(request.getUserAnswer().trim().toUpperCase());
        answer.setIsCorrect(isCorrect ? 1 : 0);
        answer.setAnswerTimeSeconds(request.getAnswerTimeSeconds());
        answer.setCreatedAt(LocalDateTime.now());
        answerMapper.insert(answer);

        // 计算当前进度和连对数
        Long answeredCount = answerMapper.selectCount(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        int streak = calculateStreak(sessionId);

        return AnswerResultVO.builder()
                .isCorrect(isCorrect)
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .knowledgePoint(question.getKnowledgePoint())
                .currentProgress(answeredCount.intValue())
                .totalQuestions(session.getQuestionCount())
                .streak(streak)
                .build();
    }

    /**
     * 结束答题，计算得分
     */
    @Transactional
    public void finishSession(Long sessionId) {
        QuizSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(1003, "答题会话不存在");
        }
        if (session.getStatus() != 0) {
            return; // 已结束
        }

        // 统计答对数量
        List<QuizAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        int correctCount = (int) answers.stream()
                .filter(a -> a.getIsCorrect() == 1)
                .count();

        int maxStreak = calculateMaxStreak(sessionId);
        int totalQuestions = session.getQuestionCount();

        // 计算得分
        int score = calculateScore(correctCount, totalQuestions, session.getDifficulty(), maxStreak);

        // 更新会话
        session.setCorrectCount(correctCount);
        session.setScore(score);
        session.setStatus(1); // 已完成
        session.setFinishedAt(LocalDateTime.now());

        if (session.getStartedAt() != null) {
            session.setDurationSeconds(
                    (int) (LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.ofHours(8))
                            - session.getStartedAt().toEpochSecond(java.time.ZoneOffset.ofHours(8))));
        }

        sessionMapper.updateById(session);
    }

    /**
     * 获取答题进度
     */
    public Map<String, Object> getProgress(Long sessionId) {
        QuizSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(1003, "答题会话不存在");
        }

        Long answeredCount = answerMapper.selectCount(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        return Map.of(
                "sessionId", sessionId,
                "totalQuestions", session.getQuestionCount(),
                "answeredQuestions", answeredCount.intValue(),
                "status", session.getStatus()
        );
    }

    /**
     * 计算当前连对数
     */
    private int calculateStreak(Long sessionId) {
        List<QuizAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId)
                        .orderByDesc(QuizAnswer::getId));

        int streak = 0;
        for (QuizAnswer answer : answers) {
            if (answer.getIsCorrect() == 1) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 计算最大连对数
     */
    private int calculateMaxStreak(Long sessionId) {
        List<QuizAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId)
                        .orderByAsc(QuizAnswer::getId));

        int maxStreak = 0;
        int currentStreak = 0;
        for (QuizAnswer answer : answers) {
            if (answer.getIsCorrect() == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }
        return maxStreak;
    }

    /**
     * 计算得分
     * 基础分 = 正确率 × 70
     * 难度加分 = 难度系数 × 5 (easy=0, medium=1, hard=2)
     * 连对加分 = 最大连对数 / 总题数 × 15
     */
    private int calculateScore(int correctCount, int totalCount, String difficulty, int maxStreak) {
        double baseScore = (double) correctCount / totalCount * 70;
        int difficultyBonus = switch (difficulty) {
            case "easy" -> 0;
            case "hard" -> 2;
            default -> 1;
        };
        double difficultyScore = difficultyBonus * 5.0;
        double streakScore = (double) maxStreak / totalCount * 15;
        return Math.min(100, (int) (baseScore + difficultyScore + streakScore));
    }

    /**
     * Entity → VO（去除答案信息）
     */
    private QuestionVO toQuestionVO(Question q) {
        return QuestionVO.builder()
                .id(q.getId())
                .index(q.getQuestionIndex())
                .questionType(q.getQuestionType())
                .difficulty(q.getDifficulty())
                .questionContent(q.getQuestionContent())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
