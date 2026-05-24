package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.Question;
import com.aistudy.entity.QuizAnswer;
import com.aistudy.entity.QuizSession;
import com.aistudy.mapper.QuestionMapper;
import com.aistudy.mapper.QuizAnswerMapper;
import com.aistudy.mapper.QuizSessionMapper;
import com.aistudy.vo.ReportVO;
import com.aistudy.vo.WrongQuestionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AiService aiService;
    private final QuizSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final QuizAnswerMapper answerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成学习报告
     */
    public ReportVO generateReport(Long sessionId) {
        // 获取会话
        QuizSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(1003, "答题会话不存在");
        }

        // 如果会话未结束，先结束
        if (session.getStatus() == 0) {
            throw new BizException(1003, "答题尚未完成");
        }

        // 获取所有题目和答题记录
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSessionId, sessionId)
                        .orderByAsc(Question::getQuestionIndex));

        List<QuizAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getSessionId, sessionId));

        // 建立题目ID → 题目 的映射
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 找出错题
        List<WrongQuestionVO> wrongQuestions = new ArrayList<>();
        Set<String> correctPoints = new HashSet<>();
        Set<String> wrongPoints = new HashSet<>();

        for (QuizAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;

            if (answer.getIsCorrect() == 1) {
                if (question.getKnowledgePoint() != null && !question.getKnowledgePoint().isEmpty()) {
                    correctPoints.add(question.getKnowledgePoint());
                }
            } else {
                if (question.getKnowledgePoint() != null && !question.getKnowledgePoint().isEmpty()) {
                    wrongPoints.add(question.getKnowledgePoint());
                }
                wrongQuestions.add(WrongQuestionVO.builder()
                        .questionId(question.getId())
                        .questionIndex(question.getQuestionIndex())
                        .questionContent(question.getQuestionContent())
                        .correctAnswer(question.getCorrectAnswer())
                        .userAnswer(answer.getUserAnswer())
                        .explanation(question.getExplanation())
                        .build());
            }
        }

        // 计算评级
        String rating = getRating(session.getScore());

        // 生成知识总结（优先读取缓存）
        String knowledgeSummary = session.getKnowledgeSummary();
        List<String> strengthPoints;
        List<String> weakPoints;

        if (knowledgeSummary != null && !knowledgeSummary.isEmpty()) {
            // 已有缓存，直接读取
            strengthPoints = parseJsonList(session.getStrengthPoints());
            weakPoints = parseJsonList(session.getWeakPoints());
        } else {
            // 首次生成，调用 AI 并持久化
            strengthPoints = new ArrayList<>(correctPoints);
            weakPoints = new ArrayList<>(wrongPoints);

            try {
                Map<String, Object> aiResult = aiService.generateReportSummary(
                        session.getKnowledgeContent(),
                        session.getQuestionCount(),
                        session.getCorrectCount(),
                        strengthPoints,
                        weakPoints
                );
                knowledgeSummary = (String) aiResult.getOrDefault("summary", "");
                if (aiResult.containsKey("strengths")) {
                    strengthPoints = (List<String>) aiResult.get("strengths");
                }
                if (aiResult.containsKey("weaknesses")) {
                    weakPoints = (List<String>) aiResult.get("weaknesses");
                }
            } catch (Exception e) {
                log.warn("AI 生成知识总结失败，使用默认总结", e);
                knowledgeSummary = String.format("本次学习了「%s」相关内容，共 %d 题，答对 %d 题。",
                        session.getKnowledgeTitle(), session.getQuestionCount(), session.getCorrectCount());
            }

            // 持久化报告数据
            session.setKnowledgeSummary(knowledgeSummary);
            session.setStrengthPoints(toJson(strengthPoints));
            session.setWeakPoints(toJson(weakPoints));
            sessionMapper.updateById(session);
        }

        return ReportVO.builder()
                .sessionId(sessionId)
                .title(session.getKnowledgeTitle())
                .score(session.getScore())
                .correctCount(session.getCorrectCount())
                .questionCount(session.getQuestionCount())
                .durationSeconds(session.getDurationSeconds())
                .rating(rating)
                .knowledgeSummary(knowledgeSummary)
                .wrongQuestions(wrongQuestions)
                .strengthPoints(strengthPoints)
                .weakPoints(weakPoints)
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取评级
     */
    private String getRating(int score) {
        if (score >= 90) return "学霸";
        if (score >= 80) return "知识达人";
        if (score >= 60) return "学习能手";
        if (score >= 40) return "进步青年";
        return "加油";
    }
}
