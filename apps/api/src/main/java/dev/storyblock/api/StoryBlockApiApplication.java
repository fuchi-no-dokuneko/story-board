package dev.storyblock.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.storyblock")
public class StoryBlockApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoryBlockApiApplication.class, args);
    }
}
