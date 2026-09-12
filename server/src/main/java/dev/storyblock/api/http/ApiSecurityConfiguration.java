package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.security.AccessKeyService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfiguration {
    static final String SCOPE_PREFIX = "SCOPE_";

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ApiProblemWriter problemWriter,
            AccessKeyService accessKeys,
            CanonicalTransferService transfers,
            StyleAnalysisService analyses,
            Clock clock,
            StoryBlockTelemetry telemetry,
            @Value("${storyblock.security.owner-token:}") String ownerToken,
            @Value("${storyblock.trusted-lan.enabled:false}") boolean trustedLan,
            @Value("${storyblock.security.hide-cross-novel:true}") boolean hideCrossNovel,
            @Value("${storyblock.security.rate-limit-per-minute:600}") int rateLimit
    ) throws Exception {
        return ApiSecurityConfigurationApiSecurityFilterChainAction.apiSecurityFilterChain(this, http, problemWriter, accessKeys, transfers, analyses, clock, telemetry, ownerToken, trustedLan, hideCrossNovel, rateLimit);
    }

}
