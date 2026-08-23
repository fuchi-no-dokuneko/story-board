package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.security.AccessKeyService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class ApiSecurityConfiguration {
    private static final String SCOPE_PREFIX = "SCOPE_";

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
            @Value("${storyblock.security.hide-cross-novel:true}") boolean hideCrossNovel,
            @Value("${storyblock.security.rate-limit-per-minute:600}") int rateLimit
    ) throws Exception {
        AccessKeyAuthenticationFilter authenticationFilter =
                new AccessKeyAuthenticationFilter(
                        accessKeys, clock, ownerToken, problemWriter, telemetry
                );
        NovelBoundaryFilter boundaryFilter = new NovelBoundaryFilter(
                transfers, analyses, accessKeys, problemWriter, hideCrossNovel
        );
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/app.js",
                                "/styles.css",
                                "/v1/openapi.yaml",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/metrics",
                                "/actuator/metrics/**"
                        ).hasRole("OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/v1/novels", "/v1/imports")
                        .hasAuthority(scope("novel:admin"))
                        .requestMatchers(HttpMethod.POST, "/v1/style-profiles/**")
                        .hasAuthority(scope("style:admin"))
                        .requestMatchers(HttpMethod.POST, "/v1/rewrite-proposals")
                        .hasAuthority(scope("rewrite:propose"))
                        .requestMatchers(HttpMethod.POST, "/v1/internal/jobs/claims")
                        .hasAuthority(scope("worker:execute"))
                        .requestMatchers(HttpMethod.POST, "/v1/internal/jobs/*/results")
                        .hasAuthority(scope("worker:execute"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/commits")
                        .hasAuthority(scope("novel:commit"))
                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/novels/*/edit-previews",
                                "/v1/novels/*/undo-previews"
                        ).hasAuthority(scope("novel:propose"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/detector-runs")
                        .hasAuthority(scope("novel:analyze"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/monitor-packets")
                        .hasAuthority(scope("novel:read"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/monitor-runs")
                        .hasAuthority(scope("monitor:submit"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/style-analyses")
                        .hasAuthority(scope("style:analyze"))
                        .requestMatchers(HttpMethod.POST, "/v1/novels/*/access-keys")
                        .hasAuthority(scope("novel:admin"))
                        .requestMatchers(HttpMethod.DELETE, "/v1/access-keys/*")
                        .hasAuthority(scope("novel:admin"))
                        .requestMatchers(HttpMethod.GET, "/v1/**")
                        .hasAuthority(scope("novel:read"))
                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/novels/*/renders",
                                "/v1/novels/*/exports"
                        ).hasAuthority(scope("novel:read"))
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ignored) ->
                                {
                                    telemetry.recordAuthDenied("missing");
                                    problemWriter.write(request, response, ApiFailureException.of(
                                        HttpStatus.UNAUTHORIZED,
                                        "AUTHENTICATION_REQUIRED",
                                        "Authentication required",
                                        "authentication-required",
                                        "A valid bearer credential is required."
                                    ));
                                }
                        )
                        .accessDeniedHandler((request, response, ignored) ->
                                {
                                    telemetry.recordAuthDenied("scope");
                                    problemWriter.write(request, response, ApiFailureException.of(
                                        HttpStatus.FORBIDDEN,
                                        "SCOPE_REQUIRED",
                                        "Required scope missing",
                                        "scope-required",
                                        "The authenticated principal lacks the required scope."
                                    ));
                                }
                        )
                )
                .addFilterAfter(
                        new MutationPreconditionFilter(problemWriter),
                        AuthorizationFilter.class
                )
                .addFilterBefore(authenticationFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(
                        new RequestRateLimitFilter(rateLimit, clock, problemWriter),
                        AccessKeyAuthenticationFilter.class
                )
                .addFilterAfter(boundaryFilter, AccessKeyAuthenticationFilter.class);
        return http.build();
    }

    private static String scope(String value) {
        return SCOPE_PREFIX + value;
    }
}
