package com.rfid.circularlabs_rfid_backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {
    // Async용 설정 파일
    // 스레드를 관리하기 위한 스레드 풀 설정 파일

    /* 스레드 풀 설정.
     * 이 스레드 설정 메소드를 Service의 멀티스레드가 적용될 동작메소드에 매핑시켜주면
     * 해당 Service의 메소드는 비동기 방식으로 멀티 스레드가 동작된다.
     */
    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(20); // 기본 스레드 수
        taskExecutor.setMaxPoolSize(100); // 최대 스레드 수
        taskExecutor.setQueueCapacity(500); // Queue 사이즈
        taskExecutor.setThreadNamePrefix("Executor-");
        return taskExecutor;
    }

}
