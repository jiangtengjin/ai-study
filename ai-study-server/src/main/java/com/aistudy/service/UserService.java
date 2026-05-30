package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.LeagueTier;
import com.aistudy.entity.User;
import com.aistudy.mapper.LeagueTierMapper;
import com.aistudy.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final LeagueTierMapper leagueTierMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 邮箱注册
     */
    public User register(String email, String password, String nickname) {
        // 检查邮箱是否已注册
        User existing = findByEmail(email);
        if (existing != null) {
            throw new BizException(400, "该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : "用户" + email.substring(0, email.indexOf("@")));
        user.setAuthType("email");
        user.setTotalQuizzes(0);
        user.setTotalCorrect(0);
        user.setTotalQuestions(0);
        user.setStreakDays(0);
        user.setTotalPoints(0L);
        user.setTierId(getDefaultTierId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 邮箱登录
     */
    public User login(String email, String password) {
        User user = findByEmail(email);
        if (user == null) {
            throw new BizException(400, "邮箱或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(400, "邮箱或密码错误");
        }

        return user;
    }

    /**
     * 根据邮箱查找用户
     */
    public User findByEmail(String email) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
    }

    /**
     * 根据认证类型和ID查找用户（用于第三方登录）
     */
    public User findByAuth(String authType, String authId) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getAuthType, authType)
                .eq(User::getAuthId, authId)
        );
    }

    /**
     * 创建第三方登录用户
     */
    public User createOAuthUser(String authType, String authId, String nickname, String avatar, String email) {
        User user = new User();
        user.setAuthType(authType);
        user.setAuthId(authId);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setEmail(email);
        user.setTotalQuizzes(0);
        user.setTotalCorrect(0);
        user.setTotalQuestions(0);
        user.setStreakDays(0);
        user.setTotalPoints(0L);
        user.setTierId(getDefaultTierId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 根据ID查找用户
     */
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 更新用户信息
     */
    public void update(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (user.getPassword() == null) {
            // GitHub OAuth 用户首次设置密码
            user.setPassword(passwordEncoder.encode(newPassword));
        } else {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BizException(400, "旧密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 更新用户学习统计
     */
    public void updateStudyStats(Long userId, int correctCount, int totalCount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        user.setTotalQuizzes(user.getTotalQuizzes() + 1);
        user.setTotalCorrect(user.getTotalCorrect() + correctCount);
        user.setTotalQuestions(user.getTotalQuestions() + totalCount);

        // 更新连续学习天数
        LocalDate today = LocalDate.now();
        if (user.getLastStudyDate() == null) {
            user.setStreakDays(1);
        } else if (user.getLastStudyDate().equals(today.minusDays(1))) {
            user.setStreakDays(user.getStreakDays() + 1);
        } else if (!user.getLastStudyDate().equals(today)) {
            user.setStreakDays(1);
        }
        user.setLastStudyDate(today);

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private Long getDefaultTierId() {
        LeagueTier tier = leagueTierMapper.selectOne(
                new LambdaQueryWrapper<LeagueTier>()
                        .orderByAsc(LeagueTier::getSortOrder)
                        .last("LIMIT 1"));
        return tier != null ? tier.getId() : null;
    }
}
