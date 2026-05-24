package com.aistudy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 通义千问 Embedding 配置
 * 与 DeepSeek Chat 分开，使用不同的 API 地址
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${embedding.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${embedding.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String dashscopeBaseUrl;

    @Value("${embedding.dashscope.model:text-embedding-v3}")
    private String embeddingModel;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("EmbeddingConfig - api-key: [{}], base-url: [{}], model: [{}]",
                dashscopeApiKey.isEmpty() ? "(empty)" : "(set)",
                dashscopeBaseUrl, embeddingModel);
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(dashscopeBaseUrl)
                .apiKey(dashscopeApiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();

        return new OpenAiEmbeddingModel(api, org.springframework.ai.document.MetadataMode.EMBED, options);
    }
}
