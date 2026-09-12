package dev.storyblock.api.http;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;

final class ApiSecurityFailures {
  static void configure(ExceptionHandlingConfigurer<HttpSecurity> exceptions, StoryBlockTelemetry telemetry, ApiProblemWriter problemWriter) {
    exceptions
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
            );
  }
}
