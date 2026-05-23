package com.aistudy.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SaTokenJwtInitializer {

    private static final String DEFAULT_KEY = "ZTdlZjY0MWEtOGFiOC00ZDg5LWEzYTktNzE5ZGYxM2I4NzRjLXNhLXRva2VuLWp3dA==";

    @Value("${sa-token.jwt-secret-key:}")
    private String jwtSecretKey;

    @PostConstruct
    public void init() {
        if (jwtSecretKey == null || jwtSecretKey.isBlank() || DEFAULT_KEY.equals(jwtSecretKey)) {
            log.warn("未配置 JWT_SECRET 环境变量，当前使用默认密钥（重启后 token 失效，生产环境请设置 JWT_SECRET）");
        }
    }
}
