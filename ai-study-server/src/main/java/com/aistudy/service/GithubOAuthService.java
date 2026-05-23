package com.aistudy.service;

import com.aistudy.common.result.BizException;
import com.aistudy.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GithubOAuthService {

    private final UserService userService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    public GithubOAuthService(UserService userService) {
        this.userService = userService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 获取 GitHub 授权 URL
     */
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "user:email")
                .queryParam("state", "github")
                .build()
                .toUriString();
    }

    /**
     * 处理 GitHub 回调
     */
    public User handleCallback(String code) {
        if (code == null || code.isEmpty()) {
            throw new BizException(400, "GitHub 授权码不能为空");
        }

        // 1. 用 code 换取 access_token
        String accessToken = getAccessToken(code);

        // 2. 用 access_token 获取用户信息
        JsonNode userInfo = getUserInfo(accessToken);

        String githubId = userInfo.get("id").asText();
        String login = userInfo.has("login") ? userInfo.get("login").asText() : "GitHub用户";
        String avatar = userInfo.has("avatar_url") ? userInfo.get("avatar_url").asText() : null;
        String email = userInfo.has("email") && !userInfo.get("email").isNull()
                ? userInfo.get("email").asText()
                : login + "@github.com";

        // 3. 查找或创建用户
        User user = userService.findByAuth("github", githubId);
        if (user == null) {
            user = userService.createOAuthUser("github", githubId, login, avatar, email);
        } else {
            // 更新用户信息
            user.setAvatar(avatar);
            user.setNickname(login);
            userService.update(user);
        }

        return user;
    }

    private String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String responseBody = response.getBody();
            log.info("GitHub access_token 响应状态: {}", response.getStatusCode());

            if (responseBody == null || responseBody.isEmpty()) {
                log.error("GitHub 返回空响应");
                throw new BizException(500, "GitHub 登录失败：服务器未返回有效响应");
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("error")) {
                String errorDesc = jsonNode.has("error_description") ? jsonNode.get("error_description").asText() : "未知错误";
                log.warn("GitHub 授权失败: error={}, description={}", jsonNode.get("error").asText(), errorDesc);
                throw new BizException(400, "GitHub 授权失败: " + errorDesc);
            }
            if (!jsonNode.has("access_token")) {
                log.error("GitHub 响应中缺少 access_token: {}", responseBody);
                throw new BizException(500, "GitHub 登录失败：未获取到访问令牌");
            }
            return jsonNode.get("access_token").asText();
        } catch (BizException e) {
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("GitHub 网络请求超时或连接失败", e);
            throw new BizException(500, "GitHub 登录失败：网络连接超时，请检查网络后重试");
        } catch (Exception e) {
            log.error("获取 GitHub access_token 失败", e);
            throw new BizException(500, "GitHub 登录失败，请重试");
        }
    }

    private JsonNode getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "AI-Study-App");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.error("GitHub 用户信息接口返回空响应");
                throw new BizException(500, "获取 GitHub 用户信息失败：服务器未返回有效数据");
            }

            return objectMapper.readTree(responseBody);
        } catch (BizException e) {
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("获取 GitHub 用户信息网络超时", e);
            throw new BizException(500, "获取 GitHub 用户信息失败：网络连接超时");
        } catch (Exception e) {
            log.error("获取 GitHub 用户信息失败", e);
            throw new BizException(500, "获取 GitHub 用户信息失败");
        }
    }
}
