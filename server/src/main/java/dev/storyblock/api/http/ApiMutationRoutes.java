package dev.storyblock.api.http;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

final class ApiMutationRoutes {
  static void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
    routes
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/commits")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:commit"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/images")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:commit"))
            .requestMatchers(
                HttpMethod.POST,
                "/v1/novels/*/edit-previews",
                "/v1/novels/*/undo-previews"
            ).hasAuthority(ApiSecurityConfigurationScope.scope("novel:propose"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/detector-runs")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:analyze"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/monitor-packets")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:read"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/monitor-runs")
            .hasAuthority(ApiSecurityConfigurationScope.scope("monitor:submit"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/style-analyses",
                "/v1/novels/*/style-comparisons")
            .hasAuthority(ApiSecurityConfigurationScope.scope("style:analyze"))
            .requestMatchers(HttpMethod.POST, "/v1/novels/*/access-keys")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:admin"))
            .requestMatchers(HttpMethod.DELETE, "/v1/access-keys/*")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:admin"))
            .requestMatchers(HttpMethod.GET, "/v1/**")
            .hasAuthority(ApiSecurityConfigurationScope.scope("novel:read"))
            .requestMatchers(
                HttpMethod.POST,
                "/v1/novels/*/renders",
                "/v1/novels/*/pdf-renders",
                "/v1/novels/*/exports"
            ).hasAuthority(ApiSecurityConfigurationScope.scope("novel:read"));
  }
}
