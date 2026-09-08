package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.security.AccessKeyService;
import java.time.Clock;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

final class ApiSecurityConfigurationApiSecurityFilterChainAction {
  static SecurityFilterChain apiSecurityFilterChain(ApiSecurityConfiguration self, HttpSecurity http, ApiProblemWriter problemWriter, AccessKeyService accessKeys, CanonicalTransferService transfers, StyleAnalysisService analyses, Clock clock, StoryBlockTelemetry telemetry, String ownerToken, boolean trustedLan, boolean hideCrossNovel, int rateLimit) throws Exception {
    AccessKeyAuthenticationFilter authenticationFilter =
        new AccessKeyAuthenticationFilter(
            accessKeys, clock, ownerToken, problemWriter, telemetry,
            trustedLan
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
        .authorizeHttpRequests(routes -> {
            ApiReadAndCreationRoutes.configure(routes);
            ApiMutationRoutes.configure(routes);
            routes.anyRequest().denyAll();
        })
        .exceptionHandling(exceptions -> ApiSecurityFailures.configure(exceptions, telemetry, problemWriter))
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
}
