-- AI 知识闯关平台 数据库建表脚本
-- 数据库: ai_study

CREATE DATABASE IF NOT EXISTS ai_study DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_study;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    auth_type VARCHAR(20) NOT NULL COMMENT '认证类型: wechat/github',
    auth_id VARCHAR(100) NOT NULL COMMENT '第三方平台用户ID',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    vip_level TINYINT DEFAULT 0 COMMENT 'VIP等级',
    vip_expire_time DATETIME DEFAULT NULL COMMENT 'VIP过期时间',
    total_quizzes INT DEFAULT 0 COMMENT '总答题次数',
    total_correct INT DEFAULT 0 COMMENT '总答对题数',
    total_questions INT DEFAULT 0 COMMENT '总答题数',
    streak_days INT DEFAULT 0 COMMENT '连续学习天数',
    last_study_date DATE DEFAULT NULL COMMENT '最后学习日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth (auth_type, auth_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 答题会话表
CREATE TABLE IF NOT EXISTS t_quiz_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT 0 COMMENT '用户ID',
    knowledge_content TEXT NOT NULL COMMENT '用户输入的知识内容',
    knowledge_title VARCHAR(200) DEFAULT NULL COMMENT 'AI生成的知识标题',
    question_count INT DEFAULT 0 COMMENT '题目总数',
    correct_count INT DEFAULT 0 COMMENT '答对数量',
    score INT DEFAULT 0 COMMENT '得分(百分制)',
    duration_seconds INT DEFAULT 0 COMMENT '答题用时(秒)',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度等级',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成 2-中途退出',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题会话表';

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    question_index INT NOT NULL COMMENT '题目序号',
    question_type VARCHAR(20) DEFAULT 'single' COMMENT '题目类型',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度',
    question_content TEXT NOT NULL COMMENT '题目内容',
    option_a VARCHAR(500) DEFAULT NULL COMMENT '选项A',
    option_b VARCHAR(500) DEFAULT NULL COMMENT '选项B',
    option_c VARCHAR(500) DEFAULT NULL COMMENT '选项C',
    option_d VARCHAR(500) DEFAULT NULL COMMENT '选项D',
    correct_answer VARCHAR(10) NOT NULL COMMENT '正确答案',
    explanation TEXT COMMENT '知识讲解',
    knowledge_point VARCHAR(200) DEFAULT NULL COMMENT '关联知识点',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 答题记录表
CREATE TABLE IF NOT EXISTS t_quiz_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_id BIGINT DEFAULT 0 COMMENT '用户ID',
    user_answer VARCHAR(10) DEFAULT NULL COMMENT '用户答案',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确',
    answer_time_seconds INT DEFAULT 0 COMMENT '本题用时(秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';
