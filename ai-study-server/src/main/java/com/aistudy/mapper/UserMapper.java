package com.aistudy.mapper;

import com.aistudy.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE t_user SET total_points = total_points + #{points} WHERE id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("points") long points);
}
