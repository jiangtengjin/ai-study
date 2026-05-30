package com.aistudy.controller;

import com.aistudy.common.result.BizException;
import com.aistudy.common.result.R;
import com.aistudy.dto.LoginRequest;
import com.aistudy.dto.RegisterRequest;
import com.aistudy.entity.User;
import com.aistudy.service.GithubOAuthService;
import com.aistudy.service.UserService;
import com.aistudy.vo.UserVO;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final GithubOAuthService githubOAuthService;

    @Value("${github.frontend-callback-url:http://localhost:5173/oauth/callback}")
    private String frontendCallbackUrl;

    @Operation(summary = "邮箱注册")
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        verifyCaptcha(request.getCaptcha(), session);

        User user = userService.register(request.getEmail(), request.getPassword(), request.getNickname());

        // 自动登录
        StpUtil.login(user.getId());

        UserVO vo = convertToVO(user);
        vo.setToken(StpUtil.getTokenValue());
        return R.ok(vo);
    }

    @Operation(summary = "邮箱登录")
    @PostMapping("/login")
    public R<UserVO> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        verifyCaptcha(request.getCaptcha(), session);

        User user = userService.login(request.getEmail(), request.getPassword());

        // Sa-Token 登录
        StpUtil.login(user.getId());

        UserVO vo = convertToVO(user);
        vo.setToken(StpUtil.getTokenValue());
        return R.ok(vo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前登录状态")
    @GetMapping("/status")
    public R<Boolean> getLoginStatus() {
        return R.ok(StpUtil.isLogin());
    }

    @Operation(summary = "获取 GitHub 授权 URL")
    @GetMapping("/github")
    public R<Map<String, String>> getGithubAuthUrl(HttpSession session) {
        String url = githubOAuthService.getAuthorizationUrl(session);
        return R.ok(Map.of("url", url));
    }

    @Operation(summary = "GitHub OAuth 回调")
    @GetMapping("/github/callback")
    public void githubCallback(@RequestParam String code,
                               @RequestParam(required = false) String state,
                               HttpSession session,
                               jakarta.servlet.http.HttpServletResponse response) throws Exception {
        User user = githubOAuthService.handleCallback(code, state, session);

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 重定向到前端回调页面（token 通过 fragment 传递，不会发送到服务器）
        response.sendRedirect(frontendCallbackUrl + "?code=success#token=" + StpUtil.getTokenValue());
    }

    private void verifyCaptcha(String captcha, HttpSession session) {
        String expected = (String) session.getAttribute("captcha");
        Long captchaTime = (Long) session.getAttribute("captchaTime");

        // 验证后立即清除，防止重放
        session.removeAttribute("captcha");
        session.removeAttribute("captchaTime");

        if (expected == null || captcha == null || !expected.equalsIgnoreCase(captcha.trim())) {
            throw new BizException(400, "验证码错误");
        }

        // 验证码 5 分钟过期
        if (captchaTime != null && System.currentTimeMillis() - captchaTime > 5 * 60 * 1000) {
            throw new BizException(400, "验证码已过期，请重新获取");
        }
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
