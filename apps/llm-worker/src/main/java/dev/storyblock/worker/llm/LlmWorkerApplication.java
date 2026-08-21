package dev.storyblock.worker.llm;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LlmWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmWorkerApplication.class, args);
    }

    @Bean
    CommandLineRunner idleWorker() {
        return args -> {
            // This worker remains proposal-only and receives no storage implementation.
        };
    }
}
