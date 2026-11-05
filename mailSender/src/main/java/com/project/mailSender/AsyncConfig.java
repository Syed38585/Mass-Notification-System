package com.project.mailSender;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean("mailExecutor")
    public Executor mailExecutor() {

            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(30);
            scheduler.setThreadNamePrefix("MailScheduler-");
            scheduler.initialize();
            return scheduler;
    }
}