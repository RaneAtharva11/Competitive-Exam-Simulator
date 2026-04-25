package com.examplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {         // LINE 1
        return new RestTemplate();
    }

    // AppConfig.java — replace taskExecutor bean

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);     // queue up to 20 jobs
        executor.setThreadNamePrefix("ai-explanation-");
        executor.initialize();
        return executor;
    }
}