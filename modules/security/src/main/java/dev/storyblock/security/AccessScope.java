package dev.storyblock.security;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum AccessScope {
    NOVEL_READ("novel:read"),
    NOVEL_ANALYZE("novel:analyze"),
    NOVEL_PROPOSE("novel:propose"),
    NOVEL_COMMIT("novel:commit"),
    NOVEL_ADMIN("novel:admin"),
    STYLE_ANALYZE("style:analyze"),
    STYLE_ADMIN("style:admin"),
    REWRITE_PROPOSE("rewrite:propose"),
    MONITOR_SUBMIT("monitor:submit"),
    WORKER_EXECUTE("worker:execute");

    private final String canonicalName;

    AccessScope(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static AccessScope fromCanonicalName(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.canonicalName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported access scope " + value
                ));
    }

    public static List<String> canonicalNames(Iterable<AccessScope> scopes) {
        java.util.ArrayList<AccessScope> values = new java.util.ArrayList<>();
        scopes.forEach(values::add);
        return values.stream()
                .sorted(Comparator.comparing(AccessScope::canonicalName))
                .map(AccessScope::canonicalName)
                .toList();
    }
}
