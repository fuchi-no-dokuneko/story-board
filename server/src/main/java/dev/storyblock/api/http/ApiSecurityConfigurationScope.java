package dev.storyblock.api.http;

import static dev.storyblock.api.http.ApiSecurityConfiguration.SCOPE_PREFIX;

final class ApiSecurityConfigurationScope {
    static String scope(String value) {
        return SCOPE_PREFIX + value;
    }
}
