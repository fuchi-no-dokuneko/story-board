package dev.storyblock.worker.style;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StyleWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StyleWorkerApplication.class, args);
    }

    @Bean
    CommandLineRunner idleWorker() {
        return args -> {
            // Job leasing is introduced with ADR-306; the process boundary exists now.
        };
    }
}
