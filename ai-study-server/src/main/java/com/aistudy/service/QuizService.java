package com.aistudy.service;

import cn.dev33.satoken.stp.StpUtil;
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
import com.aistudy.vo.QuestionDetailVO;
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
import java.util.stream.Collectors;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final AiService aiService;
    private final KnowledgeService knowledgeService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseVectorStoreService vectorStoreService;
    private final UserService userService;
    private final QuizSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final QuizAnswerMapper answerMapper;

    /**
     * 创建答题会话：输入知识内容 → AI 出题 → 存储题目
     */
    @Transactional
    public CreateQuizVO createSession(CreateQuizRequest request) {
        // 1. 获取当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 创建会话
        QuizSession session = new QuizSession();
        session.setUserId(userId);
        session.setKnowledgeContent(request.getContent());
        session.setQuestionCount(request.getQuestionCount());
        session.setDifficulty(request.getDifficulty());
        session.setStatus(0); // 进行中
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        // 2. 调用 AI 生成题目
        try {
            // 知识库 RAG 出题与联网搜索互斥，优先使用知识库
            Map<String, Object> result;
            if (request.getKnowledgeBaseId() != null) {
                // 基于知识库 RAG 出题
                log.info("使用知识库 {} 进行 RAG 出题...", request.getKnowledgeBaseId());
                var kb = knowledgeBaseService.getKnowledgeBase(request.getKnowledgeBaseId());
                if (kb == null) {
                    throw new BizException(400, "知识库不存在");
                }
                if (knowledgeBaseService.getDocCount(request.getKnowledgeBaseId()) == 0) {
                    throw new BizException(400, "该知识库暂无文档，请先上传文档后再出题");
                }
                String ragResults = retrieveFromKnowledgeBase(request.getKnowledgeBaseId(), request.getContent());
                result = aiService.generateQuestionsWithRag(
                        request.getContent(), request.getQuestionCount(), request.getDifficulty(), ragResults);
            } else if (request.isEnableSearch()) {
                // 联网搜索出题
                log.info("联网搜索已开启，正在检索知识...");
                String searchResults = knowledgeService.retrieveKnowledge(request.getContent());
                if (!searchResults.isBlank()) {
                    log.info("知识检索成功，结果长度: {}", searchResults.length());
                } else {
                    log.info("知识检索无结果，使用原始内容出题");
                }
                if (!searchResults.isBlank()) {
                    result = aiService.generateQuestions(
                            request.getContent(), request.getQuestionCount(), request.getDifficulty(), searchResults);
                } else {
                    result = aiService.generateQuestions(
                            request.getContent(), request.getQuestionCount(), request.getDifficulty());
                }
            } else {
                // 普通出题
                result = aiService.generateQuestions(
                        request.getContent(), request.getQuestionCount(), request.getDifficulty());
            }

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
                int questionScore = getIntValue(q, "score", 0);
                question.setScore(questionScore);
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
     * 重新练习：创建新会话，复制原会话的题目
     */
    @Transactional
    public CreateQuizVO retrySession(Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();

        QuizSession originalSession = sessionMapper.selectById(sessionId);
        if (originalSession == null) {
            throw new BizException(1003, "原答题会话不存在");
        }

        // 创建新会话
        QuizSession newSession = new QuizSession();
        newSession.setUserId(userId);
        newSession.setKnowledgeContent(originalSession.getKnowledgeContent());
        newSession.setKnowledgeTitle(originalSession.getKnowledgeTitle());
        newSession.setQuestionCount(originalSession.getQuestionCount());
        newSession.setDifficulty(originalSession.getDifficulty());
        newSession.setStatus(0);
        newSession.setStartedAt(LocalDateTime.now());
        newSession.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(newSession);

        // 复制原会话的题目
        List<Question> originalQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId)
                        .orderByAsc(Question::getQuestionIndex));

        for (Question original : originalQuestions) {
            Question question = new Question();
            question.setSessionId(newSession.getId());
            question.setQuestionIndex(original.getQuestionIndex());
            question.setQuestionType(original.getQuestionType());
            question.setDifficulty(original.getDifficulty());
            question.setQuestionContent(original.getQuestionContent());
            question.setOptionA(original.getOptionA());
            question.setOptionB(original.getOptionB());
            question.setOptionC(original.getOptionC());
            question.setOptionD(original.getOptionD());
            question.setCorrectAnswer(original.getCorrectAnswer());
            question.setExplanation(original.getExplanation());
            question.setKnowledgePoint(original.getKnowledgePoint());
            question.setCreatedAt(LocalDateTime.now());
            questionMapper.insert(question);
        }

        return CreateQuizVO.builder()
                .sessionId(newSession.getId())
                .title(originalSession.getKnowledgeTitle())
                .questionCount(originalQuestions.size())
                .status("answering")
                .build();
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

        // 获取当前用户ID
        Long userId = session.getUserId();

        // 保存答题记录
        QuizAnswer answer = new QuizAnswer();
        answer.setSessionId(sessionId);
        answer.setQuestionId(request.getQuestionId());
        answer.setUserId(userId);
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

        // 获取题目及其分值
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId));
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correctCount = (int) answers.stream()
                .filter(a -> a.getIsCorrect() == 1)
                .count();

        int maxStreak = calculateMaxStreak(sessionId);
        int totalQuestions = session.getQuestionCount();

        // 按每题分值计算得分
        int score = calculateScoreByQuestionScore(answers, questionMap);

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

        // 更新用户学习统计
        if (session.getUserId() != null && session.getUserId() > 0) {
            userService.updateStudyStats(session.getUserId(), correctCount, totalQuestions);
        }
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
     * 按每题分值计算得分：答对的题目分值累加
     */
    private int calculateScoreByQuestionScore(List<QuizAnswer> answers, Map<Long, Question> questionMap) {
        int totalScore = 0;
        boolean hasScoreData = false;

        for (QuizAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;

            if (question.getScore() != null && question.getScore() > 0) {
                hasScoreData = true;
                if (answer.getIsCorrect() == 1) {
                    totalScore += question.getScore();
                }
            }
        }

        // 如果题目没有分值数据，回退到旧公式
        if (!hasScoreData) {
            int correctCount = (int) answers.stream().filter(a -> a.getIsCorrect() == 1).count();
            int totalCount = answers.size();
            int maxStreak = 0;
            int currentStreak = 0;
            for (QuizAnswer a : answers) {
                if (a.getIsCorrect() == 1) {
                    currentStreak++;
                    maxStreak = Math.max(maxStreak, currentStreak);
                } else {
                    currentStreak = 0;
                }
            }
            return calculateScoreFallback(correctCount, totalCount, maxStreak);
        }

        return Math.min(100, totalScore);
    }

    /**
     * 回退得分公式（题目无分值时使用）
     */
    private int calculateScoreFallback(int correctCount, int totalCount, int maxStreak) {
        double correctRate = (double) correctCount / totalCount;
        double baseScore = correctRate * 85;
        double streakScore = (double) maxStreak / totalCount * 5;
        return Math.min(100, (int) Math.round(baseScore + streakScore + 10));
    }

    /**
     * 获取会话的完整答题记录（含用户答案和解析）
     */
    public List<QuestionDetailVO> getQuestionDetails(Long sessionId) {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId)
                        .orderByAsc(Question::getQuestionIndex));

        List<QuizAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        Map<Long, QuizAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, a -> a));

        return questions.stream().map(q -> {
            QuizAnswer answer = answerMap.get(q.getId());
            return QuestionDetailVO.builder()
                    .questionId(q.getId())
                    .questionIndex(q.getQuestionIndex())
                    .questionContent(q.getQuestionContent())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .correctAnswer(q.getCorrectAnswer())
                    .userAnswer(answer != null ? answer.getUserAnswer() : null)
                    .isCorrect(answer != null ? answer.getIsCorrect() : null)
                    .explanation(q.getExplanation())
                    .knowledgePoint(q.getKnowledgePoint())
                    .answerTimeSeconds(answer != null ? answer.getAnswerTimeSeconds() : null)
                    .build();
        }).toList();
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
                .score(q.getScore())
                .build();
    }

    /**
     * 从知识库中检索相关文档片段
     */
    private String retrieveFromKnowledgeBase(Long knowledgeBaseId, String query) {
        try {
            var results = vectorStoreService.search(knowledgeBaseId, query, 5, 0.4);
            if (results.isEmpty()) {
                log.info("知识库 {} 无与查询相关的内容，降级到普通出题", knowledgeBaseId);
                return "";
            }

            StringBuilder sb = new StringBuilder();
            int totalLength = 0;
            for (var doc : results) {
                String content = doc.getText();
                if (totalLength + content.length() > 3000) {
                    content = content.substring(0, 3000 - totalLength) + "...";
                }
                sb.append(content).append("\n\n");
                totalLength += content.length();
                if (totalLength >= 3000) break;
            }

            log.info("从知识库 {} 检索到 {} 个相关片段，总长度: {}", knowledgeBaseId, results.size(), totalLength);
            // 打印第一个片段的前100字，便于确认检索内容来源
            if (!results.isEmpty()) {
                log.info("首个检索片段预览: {}", results.get(0).getText().substring(0, Math.min(100, results.get(0).getText().length())));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("知识库检索失败，降级到普通出题: {}", e.getMessage());
            return "";
        }
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
