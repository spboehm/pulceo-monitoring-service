package dev.pulceo.pms.config;

import dev.pulceo.pms.model.event.PulceoEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@EnableAsync
public class ThreadPoolTaskExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        return executor;
    }

    @Bean
    public BlockingQueue<PulceoEvent> eventQueue() {
        return new LinkedBlockingQueue<>();
    }

}
