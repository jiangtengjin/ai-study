package com.aistudy.service;

import org.springframework.stereotype.Component;

@Component
public class PointCalculator {

    public static final int EASY_CORRECT = 10;
    public static final int EASY_WRONG = 2;
    public static final int MEDIUM_CORRECT = 20;
    public static final int MEDIUM_WRONG = 5;
    public static final int HARD_CORRECT = 35;
    public static final int HARD_WRONG = 8;

    /**
     * 计算单题积分
     *
     * @param difficulty 题目难度: easy/medium/hard
     * @param isCorrect  是否答对: true/false
     * @return 积分
     */
    public int calculate(String difficulty, boolean isCorrect) {
        if (difficulty == null) {
            difficulty = "medium";
        }
        return switch (difficulty.toLowerCase()) {
            case "easy" -> isCorrect ? EASY_CORRECT : EASY_WRONG;
            case "hard" -> isCorrect ? HARD_CORRECT : HARD_WRONG;
            default -> isCorrect ? MEDIUM_CORRECT : MEDIUM_WRONG;
        };
    }

    /**
     * 生成 SQL CASE 表达式（用于趋势统计查询）
     * 与 calculate() 使用相同的积分规则
     */
    public static String pointsCaseExpression(String pointsColumn) {
        return "SUM(CASE WHEN q.difficulty = 'easy' THEN (CASE WHEN qa.is_correct = 1 THEN "
                + EASY_CORRECT + " ELSE " + EASY_WRONG + " END) "
                + "WHEN q.difficulty = 'hard' THEN (CASE WHEN qa.is_correct = 1 THEN "
                + HARD_CORRECT + " ELSE " + HARD_WRONG + " END) "
                + "ELSE (CASE WHEN qa.is_correct = 1 THEN "
                + MEDIUM_CORRECT + " ELSE " + MEDIUM_WRONG + " END) END) AS " + pointsColumn;
    }
}
