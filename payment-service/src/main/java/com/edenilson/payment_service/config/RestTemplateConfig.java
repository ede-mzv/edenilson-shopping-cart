package com.edenilson.payment_service.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .requestFactorySettings(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(Duration.ofSeconds(5)))
                .build();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest servletRequest = attributes.getRequest();
                String authHeader = servletRequest.getHeader("Authorization");
                if (authHeader != null) {
                    request.getHeaders().set("Authorization", authHeader);
                }
            }
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
