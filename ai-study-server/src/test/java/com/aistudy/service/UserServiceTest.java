package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.User;
import com.aistudy.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"); // "password123" 的 BCrypt 加密
        testUser.setNickname("测试用户");
        testUser.setAuthType("email");
        testUser.setTotalQuizzes(5);
        testUser.setTotalCorrect(40);
        testUser.setTotalQuestions(50);
        testUser.setStreakDays(3);
        testUser.setLastStudyDate(LocalDate.now().minusDays(1));
        testUser.setCreatedAt(LocalDateTime.now().minusDays(10));
    }

    @Nested
    @DisplayName("邮箱注册测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功")
        void register_success() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            User result = userService.register("new@example.com", "password123", "新用户");

            assertNotNull(result);
            assertEquals("new@example.com", result.getEmail());
            assertEquals("新用户", result.getNickname());
            assertEquals("email", result.getAuthType());
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("注册时邮箱已存在")
        void register_emailExists() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            assertThrows(BizException.class, () ->
                    userService.register("test@example.com", "password123", "用户"));
        }

        @Test
        @DisplayName("注册时昵称为空则使用邮箱前缀")
        void register_defaultNickname() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            User result = userService.register("newuser@example.com", "password123", null);

            assertEquals("newuser", result.getNickname());
        }
    }

    @Nested
    @DisplayName("邮箱登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void login_success() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            User result = userService.login("test@example.com", "password123");

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("登录失败 - 邮箱不存在")
        void login_emailNotFound() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BizException.class, () ->
                    userService.login("notexist@example.com", "password123"));
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_wrongPassword() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            assertThrows(BizException.class, () ->
                    userService.login("test@example.com", "wrongpassword"));
        }
    }

    @Nested
    @DisplayName("用户查询测试")
    class QueryTests {

        @Test
        @DisplayName("根据ID查找用户")
        void findById_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);

            User result = userService.findById(1L);

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
        }

        @Test
        @DisplayName("根据邮箱查找用户")
        void findByEmail_success() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            User result = userService.findByEmail("test@example.com");

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }
    }

    @Nested
    @DisplayName("学习统计更新测试")
    class StatsUpdateTests {

        @Test
        @DisplayName("更新学习统计 - 连续学习")
        void updateStudyStats_consecutiveDay() {
            testUser.setLastStudyDate(LocalDate.now().minusDays(1));
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            userService.updateStudyStats(1L, 8, 10);

            assertEquals(6, testUser.getTotalQuizzes());
            assertEquals(48, testUser.getTotalCorrect());
            assertEquals(60, testUser.getTotalQuestions());
            assertEquals(4, testUser.getStreakDays());
            verify(userMapper).updateById(testUser);
        }

        @Test
        @DisplayName("更新学习统计 - 同一天学习")
        void updateStudyStats_sameDay() {
            testUser.setLastStudyDate(LocalDate.now());
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            userService.updateStudyStats(1L, 8, 10);

            assertEquals(3, testUser.getStreakDays()); // 连续天数不变
        }

        @Test
        @DisplayName("更新学习统计 - 中断后重新开始")
        void updateStudyStats_streakReset() {
            testUser.setLastStudyDate(LocalDate.now().minusDays(3));
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            userService.updateStudyStats(1L, 8, 10);

            assertEquals(1, testUser.getStreakDays()); // 连续天数重置为1
        }
    }
}
