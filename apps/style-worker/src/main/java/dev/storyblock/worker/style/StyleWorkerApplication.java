package dev.storyblock.worker.style;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class StyleWorkerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            StyleWorkerApplication.class
    );

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication application = new SpringApplication(
                StyleWorkerApplication.class
        );
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    @Bean
    @ConditionalOnProperty(
            name = "storyblock.worker.enabled",
            havingValue = "true"
    )
    CommandLineRunner styleWorker(Environment environment) {
        return args -> run(StyleWorkerSettings.from(environment));
    }

    private static void run(StyleWorkerSettings settings) throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        StyleWorkerClient client = new StyleWorkerClient(
                http, settings, Clock.systemUTC()
        );
        do {
            try {
                StyleWorkerClient.Outcome outcome = client.runOnce();
                LOGGER.info(
                        "Style worker cycle completed with outcome {} for novel {}",
                        outcome,
                        settings.novelId().value()
                );
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException | java.io.IOException failure) {
                LOGGER.error("Style worker cycle failed: {}", failure.getMessage());
                if (settings.runOnce()) {
                    throw failure;
                }
            }
            if (!settings.runOnce()) {
                Thread.sleep(settings.pollInterval().toMillis());
            }
        } while (!settings.runOnce() && !Thread.currentThread().isInterrupted());
    }
}
