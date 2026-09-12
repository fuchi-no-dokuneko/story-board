package dev.storyblock.api.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ListenerConfiguration {
    @Bean
    PolicyTomcatFactory servletWebServerFactory(Environment environment) {
        return new PolicyTomcatFactory(environment);
    }
}
