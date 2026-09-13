package dev.storyblock.api.http;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

final class ApiReadAndCreationRoutes {
  static void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
    routes
            .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()
            .requestMatchers(
                "/",
                "/index.html",
                "/auth.js",
                "/app.js",
                "/styles.css",
                "/reader-actions.js", "/reader-tools.css", "/style-comparison.js",
                "/style-score-table.js", "/style-math.html",
                "/reader-images.js", "/reader-images.css",
                "/v1/openapi.yaml",
                "/actuator/health"
            ).permitAll()
            .requestMatchers(
                "/actuator/health/**",
                "/actuator/metrics",
                "/actuator/metrics/**"
            ).hasRole("OPERATOR")
            .requestMatchers(HttpMethod.GET, "/v1/admin/**")
            .hasRole("OPERATOR")
            .requestMatchers(HttpMethod.POST, "/v1/novels", "/v1/imports")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:admin"))
            .requestMatchers(HttpMethod.POST, "/v1/agent/novels")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:admin"))
            .requestMatchers(HttpMethod.POST, "/v1/style-profiles/**")
            .hasAuthority(ApiSecurityConfigurationScope.scope("style:admin"))
            .requestMatchers(HttpMethod.POST, "/v1/rewrite-proposals")
            .hasAuthority(ApiSecurityConfigurationScope.scope("rewrite:propose"))
            .requestMatchers(HttpMethod.POST, "/v1/internal/jobs/claims")
            .hasAuthority(ApiSecurityConfigurationScope.scope("worker:execute"))
            .requestMatchers(HttpMethod.POST, "/v1/internal/jobs/*/results")
            .hasAuthority(ApiSecurityConfigurationScope.scope("worker:execute"));
  }
}
