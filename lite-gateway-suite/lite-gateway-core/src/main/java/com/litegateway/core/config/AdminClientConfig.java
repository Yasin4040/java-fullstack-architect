package com.litegateway.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Admin 客户端配置
 */
@Slf4j
@Configuration
public class AdminClientConfig {

    @Value("${lite.gateway.admin.url:http://localhost:8080}")
    private String adminUrl;

    @Bean
    public WebClient adminWebClient() {
        log.info("Admin client configured with URL: {}", adminUrl);
        return WebClient.builder()
                .baseUrl(adminUrl)
                .build();
    }
}
