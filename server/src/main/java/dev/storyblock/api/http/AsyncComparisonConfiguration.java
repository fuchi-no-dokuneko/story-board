package dev.storyblock.api.http;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AsyncComparisonConfiguration implements WebMvcConfigurer {
    @Bean
    ThreadPoolTaskExecutor comparisonExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("style-comparison-");
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(comparisonExecutor());
        configurer.setDefaultTimeout(300_000);
    }
}
