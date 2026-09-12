package dev.storyblock.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "dev.storyblock",
        exclude = UserDetailsServiceAutoConfiguration.class
)
public class StoryBlockApiApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(StoryBlockApiApplication.class);
        application.setDefaultProperties(dev.storyblock.api.runtime.ApiLocalDefaults.prepare());
        application.run(args);
    }
}
