package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "验证码接口")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取验证码")
    @GetMapping
    public R<Map<String, String>> getCaptcha(HttpSession session) {
        String[] result = captchaService.generate();
        // 将验证码存入 session，忽略大小写
        session.setAttribute("captcha", result[1].toLowerCase());
        session.setAttribute("captchaTime", System.currentTimeMillis());
        return R.ok(Map.of("image", result[0]));
    }
}
