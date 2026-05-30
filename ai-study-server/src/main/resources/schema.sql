-- AI 知识闯关平台 数据库建表脚本
-- 数据库: ai_study

CREATE DATABASE IF NOT EXISTS ai_study DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_study;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    password VARCHAR(100) DEFAULT NULL COMMENT '密码(BCrypt加密)',
    auth_type VARCHAR(20) NOT NULL DEFAULT 'email' COMMENT '认证类型: email/github/wechat',
    auth_id VARCHAR(100) DEFAULT NULL COMMENT '第三方平台用户ID',
    vip_level TINYINT DEFAULT 0 COMMENT 'VIP等级',
    vip_expire_time DATETIME DEFAULT NULL COMMENT 'VIP过期时间',
    total_quizzes INT DEFAULT 0 COMMENT '总答题次数',
    total_correct INT DEFAULT 0 COMMENT '总答对题数',
    total_questions INT DEFAULT 0 COMMENT '总答题数',
    streak_days INT DEFAULT 0 COMMENT '连续学习天数',
    total_points BIGINT DEFAULT 0 COMMENT '累计总积分',
    tier_id BIGINT DEFAULT NULL COMMENT '当前段位ID',
    last_study_date DATE DEFAULT NULL COMMENT '最后学习日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email (email),
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
    knowledge_summary TEXT DEFAULT NULL COMMENT 'AI生成的知识总结',
    strength_points TEXT DEFAULT NULL COMMENT '擅长知识点JSON',
    weak_points TEXT DEFAULT NULL COMMENT '薄弱知识点JSON',
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
    score INT DEFAULT 0 COMMENT '题目分值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 知识库表
CREATE TABLE IF NOT EXISTS t_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '知识库名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '知识库描述',
    doc_count INT DEFAULT 0 COMMENT '文档数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS t_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL COMMENT '所属知识库ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    file_type VARCHAR(20) DEFAULT NULL COMMENT '文件类型(pdf/docx/txt)',
    status VARCHAR(20) DEFAULT 'processing' COMMENT '处理状态: processing/completed/failed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_knowledge_base_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

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
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- 段位配置表
CREATE TABLE IF NOT EXISTS t_league_tier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL COMMENT '段位名称',
    icon TEXT NOT NULL COMMENT '段位图标SVG',
    min_points BIGINT NOT NULL COMMENT '最低累计积分',
    sort_order INT NOT NULL COMMENT '排序（从低到高）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='段位配置表';

-- 联赛小组表
CREATE TABLE IF NOT EXISTS t_league_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier_id BIGINT NOT NULL COMMENT '段位ID',
    week_start_date DATE NOT NULL COMMENT '周起始日期（周一）',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-进行中 1-已结算',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_week_tier (week_start_date, tier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联赛小组表';

-- 联赛小组成员表
CREATE TABLE IF NOT EXISTS t_league_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    weekly_points INT DEFAULT 0 COMMENT '本周积分',
    ranking INT DEFAULT 0 COMMENT '排名',
    result VARCHAR(20) DEFAULT NULL COMMENT '结算结果: promote/keep/demote',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_group_user (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联赛小组成员表';

-- 周积分明细表
CREATE TABLE IF NOT EXISTS t_weekly_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id BIGINT NOT NULL COMMENT '答题会话ID',
    points INT DEFAULT 0 COMMENT '获得积分',
    week_start_date DATE NOT NULL COMMENT '周起始日期（周一）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_week (user_id, week_start_date),
    UNIQUE KEY uk_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周积分明细表';

-- 初始化段位配置数据
INSERT IGNORE INTO t_league_tier (name, icon, min_points, sort_order) VALUES
('铜牌', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="bronze-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#D4A574"/><stop offset="50%" style="stop-color:#CD7F32"/><stop offset="100%" style="stop-color:#8B5E3C"/></linearGradient></defs><path d="M28 4 L48 14 L48 30 C48 42 38 50 28 54 C18 50 8 42 8 30 L8 14 Z" fill="url(#bronze-grad)" stroke="#8B5E3C" stroke-width="1.5"/><path d="M28 8 L44 16 L44 30 C44 40 36 47 28 50 C20 47 12 40 12 30 L12 16 Z" fill="none" stroke="#D4A574" stroke-width="1" opacity="0.6"/><polygon points="28,18 31,26 40,26 33,31 35,39 28,34 21,39 23,31 16,26 25,26" fill="#FFE0B2" opacity="0.8"/></svg>', 0, 1),
('银牌', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="silver-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#E8E8E8"/><stop offset="50%" style="stop-color:#C0C0C0"/><stop offset="100%" style="stop-color:#A0A0A0"/></linearGradient></defs><path d="M10 28 C10 28 14 20 20 22 C16 24 14 28 14 28" fill="#D0D0D0" opacity="0.7"/><path d="M8 32 C8 32 12 22 20 24 C14 26 12 32 12 32" fill="#B8B8B8" opacity="0.6"/><path d="M46 28 C46 28 42 20 36 22 C40 24 42 28 42 28" fill="#D0D0D0" opacity="0.7"/><path d="M48 32 C48 32 44 22 36 24 C42 26 44 32 44 32" fill="#B8B8B8" opacity="0.6"/><path d="M28 6 L46 16 L46 30 C46 42 36 48 28 52 C20 48 10 42 10 30 L10 16 Z" fill="url(#silver-grad)" stroke="#909090" stroke-width="1.5"/><path d="M28 10 L42 18 L42 30 C42 40 34 45 28 48 C22 45 14 40 14 30 L14 18 Z" fill="none" stroke="#E0E0E0" stroke-width="1" opacity="0.5"/><polygon points="28,20 31,27 38,27 32,32 34,39 28,35 22,39 24,32 18,27 25,27" fill="white" opacity="0.9"/></svg>', 2000, 2),
('金牌', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="gold-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#FFE082"/><stop offset="40%" style="stop-color:#FFD700"/><stop offset="100%" style="stop-color:#FFA000"/></linearGradient></defs><line x1="28" y1="2" x2="28" y2="10" stroke="#FFD700" stroke-width="1.5" opacity="0.5"/><line x1="10" y1="10" x2="16" y2="16" stroke="#FFD700" stroke-width="1.5" opacity="0.4"/><line x1="46" y1="10" x2="40" y2="16" stroke="#FFD700" stroke-width="1.5" opacity="0.4"/><path d="M28 8 L46 18 L46 32 C46 42 36 48 28 52 C20 48 10 42 10 32 L10 18 Z" fill="url(#gold-grad)" stroke="#E6A800" stroke-width="1.5"/><path d="M20 28 L24 22 L28 26 L32 22 L36 28 L34 34 L22 34 Z" fill="#FFF8E1" opacity="0.9"/><circle cx="24" cy="24" r="1.5" fill="#FFA000"/><circle cx="28" cy="22" r="1.5" fill="#FFA000"/><circle cx="32" cy="24" r="1.5" fill="#FFA000"/></svg>', 5000, 3),
('蓝宝石', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="sapphire-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#81D4FA"/><stop offset="50%" style="stop-color:#0288D1"/><stop offset="100%" style="stop-color:#01579B"/></linearGradient></defs><polygon points="28,4 48,16 52,28 48,40 28,52 8,40 4,28 8,16" fill="#4FC3F7" opacity="0.2"/><polygon points="28,6 46,16 50,28 46,40 28,50 10,40 6,28 10,16" fill="url(#sapphire-grad)"/><polygon points="28,6 28,28 46,16" fill="rgba(255,255,255,0.2)"/><polygon points="28,6 28,28 10,16" fill="rgba(0,0,0,0.1)"/><ellipse cx="22" cy="20" rx="8" ry="5" fill="white" opacity="0.3" transform="rotate(-20 22 20)"/></svg>', 12000, 4),
('红宝石', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="ruby-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#FF8A80"/><stop offset="40%" style="stop-color:#E53935"/><stop offset="100%" style="stop-color:#B71C1C"/></linearGradient><radialGradient id="ruby-glow" cx="50%" cy="50%" r="50%"><stop offset="0%" style="stop-color:#FF5252;stop-opacity:0.4"/><stop offset="100%" style="stop-color:#FF5252;stop-opacity:0"/></radialGradient></defs><circle cx="28" cy="28" r="24" fill="url(#ruby-glow)"/><polygon points="28,6 46,16 50,28 46,40 28,50 10,40 6,28 10,16" fill="url(#ruby-grad)"/><polygon points="28,6 28,28 46,16" fill="rgba(255,255,255,0.2)"/><polygon points="28,6 28,28 10,16" fill="rgba(0,0,0,0.1)"/><polygon points="28,16 30,24 38,24 32,28 34,36 28,32 22,36 24,28 18,24 26,24" fill="white" opacity="0.15"/><ellipse cx="22" cy="20" rx="8" ry="5" fill="white" opacity="0.35" transform="rotate(-20 22 20)"/></svg>', 25000, 5),
('紫水晶', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="amethyst-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#CE93D8"/><stop offset="40%" style="stop-color:#9C27B0"/><stop offset="100%" style="stop-color:#6A1B9A"/></linearGradient><radialGradient id="amethyst-glow" cx="50%" cy="40%" r="50%"><stop offset="0%" style="stop-color:#E1BEE7;stop-opacity:0.6"/><stop offset="100%" style="stop-color:#E1BEE7;stop-opacity:0"/></radialGradient></defs><circle cx="28" cy="24" r="22" fill="url(#amethyst-glow)"/><polygon points="28,2 44,14 44,38 28,50 12,38 12,14" fill="url(#amethyst-grad)"/><polygon points="28,2 28,26 44,14" fill="rgba(255,255,255,0.25)"/><polygon points="28,2 28,26 12,14" fill="rgba(0,0,0,0.1)"/><polygon points="28,12 36,20 36,34 28,42 20,34 20,20" fill="none" stroke="white" stroke-width="0.5" opacity="0.3"/></svg>', 50000, 6),
('珍珠', '<svg viewBox="0 0 56 56" fill="none"><defs><radialGradient id="pearl-grad" cx="40%" cy="35%" r="60%"><stop offset="0%" style="stop-color:#FFFFFF"/><stop offset="50%" style="stop-color:#F5F5F5"/><stop offset="100%" style="stop-color:#E0E0E0"/></radialGradient></defs><path d="M12 34 L16 18 L22 26 L28 14 L34 26 L40 18 L44 34 Z" fill="#D4AF37" stroke="#B8860B" stroke-width="1"/><rect x="12" y="34" width="32" height="6" rx="2" fill="#D4AF37" stroke="#B8860B" stroke-width="1"/><circle cx="16" cy="22" r="2" fill="#E53935"/><circle cx="28" cy="16" r="2.5" fill="#1E88E5"/><circle cx="40" cy="22" r="2" fill="#43A047"/><circle cx="28" cy="46" r="8" fill="url(#pearl-grad)" stroke="#D4C5A9" stroke-width="1"/><ellipse cx="25" cy="43" rx="4" ry="3" fill="white" opacity="0.5"/></svg>', 100000, 7),
('黑曜石', '<svg viewBox="0 0 56 56" fill="none"><defs><linearGradient id="obsidian-grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#455A64"/><stop offset="50%" style="stop-color:#263238"/><stop offset="100%" style="stop-color:#000000"/></linearGradient><radialGradient id="obsidian-glow" cx="50%" cy="50%" r="50%"><stop offset="0%" style="stop-color:#7C4DFF;stop-opacity:0.4"/><stop offset="100%" style="stop-color:#7C4DFF;stop-opacity:0"/></radialGradient><linearGradient id="obsidian-crown" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#B0BEC5"/><stop offset="50%" style="stop-color:#546E7A"/><stop offset="100%" style="stop-color:#263238"/></linearGradient></defs><circle cx="28" cy="28" r="26" fill="url(#obsidian-glow)"/><path d="M10 32 L14 14 L20 22 L24 10 L28 20 L32 10 L36 22 L42 14 L46 32 Z" fill="url(#obsidian-crown)" stroke="#78909C" stroke-width="1"/><rect x="10" y="32" width="36" height="7" rx="2" fill="url(#obsidian-crown)" stroke="#78909C" stroke-width="1"/><circle cx="28" cy="12" r="3" fill="#E040FB"/><polygon points="28,42 34,46 32,52 24,52 22,46" fill="url(#obsidian-grad)" stroke="#546E7A" stroke-width="1"/><ellipse cx="28" cy="48" rx="3" ry="2" fill="#7C4DFF" opacity="0.8"/></svg>', 200000, 8);
