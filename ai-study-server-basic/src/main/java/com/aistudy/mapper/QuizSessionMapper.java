package com.aistudy.mapper;

import com.aistudy.entity.QuizSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizSessionMapper extends BaseMapper<QuizSession> {

    @Select("SELECT AVG(score) FROM t_quiz_session WHERE user_id = #{userId} AND status = 1")
    Double selectAvgScoreByUserId(Long userId);
}
