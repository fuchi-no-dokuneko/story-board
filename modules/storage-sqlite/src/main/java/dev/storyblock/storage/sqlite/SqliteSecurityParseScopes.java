package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.security.AccessScope;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SqliteSecurityParseScopes {
    static Set<AccessScope> parseScopes(String json) {
        @SuppressWarnings("unchecked")
        List<String> names = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), List.class
        );
        Set<AccessScope> scopes = new LinkedHashSet<>();
        for (Object name : names) {
            if (!(name instanceof String value) || !scopes.add(
                    AccessScope.fromCanonicalName(value)
            )) {
                throw new IllegalArgumentException("Stored access scopes are invalid");
            }
        }
        return Set.copyOf(scopes);
    }
}
