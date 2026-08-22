package dev.storyblock.worker.llm;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class LlmWorkerApplication {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication application = new SpringApplication(
                LlmWorkerApplication.class
        );
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of(
                "logging.level.root", "OFF",
                "spring.main.banner-mode", "off"
        ));
        application.run(args);
    }

    @Bean
    @ConditionalOnProperty(
            name = "storyblock.llm-worker.enabled",
            havingValue = "true"
    )
    CommandLineRunner rewriteWorker(Environment environment) {
        return args -> {
            LlmWorkerSettings settings = LlmWorkerSettings.from(environment);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(settings.connectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .proxy(DirectProxySelector.INSTANCE)
                    .build();
            LlmModelTransport transport = new HttpLlmModelTransport(client, settings);
            RewriteProposalGenerator generator = new RewriteProposalGenerator(
                    transport, settings.modelId()
            );
            new StandardIoRewriteRunner(generator, Clock.systemUTC()).run(
                    System.in, System.out
            );
        };
    }
}
