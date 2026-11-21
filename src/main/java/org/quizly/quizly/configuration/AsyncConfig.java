package org.quizly.quizly.configuration;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

    @Bean(name = "mockExamTaskExecutor")
    public Executor taskExecutor(
        @Value("${async.mock-exam.concurrency-limit:30}") int concurrencyLimit) {
        return createVirtualThreadExecutor("MockExam-", concurrencyLimit);
    }

    @Bean(name = "quizTaskExecutor")
    public Executor quizTaskExecutor(
        @Value("${async.quiz.concurrency-limit:50}") int concurrencyLimit) {
        return createVirtualThreadExecutor("Quiz-", concurrencyLimit);
    }

    private Executor createVirtualThreadExecutor(String threadNamePrefix, int concurrencyLimit) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(concurrencyLimit);
        return executor;
    }

    @Bean(name = "ocrTaskExecutor")
    public Executor ocrTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OCR-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }

}
