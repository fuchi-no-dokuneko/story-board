package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleProfile(
        Ids.StyleProfileId profileId,
        String name,
        StyleProfileScope scope,
        String provenance,
        String createdBy,
        Instant createdAt
) {
    private static final Pattern ACTOR = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Set<String> FIELDS = Set.of(
            "profile_id", "name", "scope", "provenance", "created_by", "created_at"
    );

    public StyleProfile {
        Objects.requireNonNull(profileId, "profileId");
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Style profile name is invalid");
        }
        Objects.requireNonNull(scope, "scope");
        if (provenance == null || provenance.isBlank() || provenance.length() > 2_000) {
            throw new IllegalArgumentException("Style profile provenance is invalid");
        }
        if (createdBy == null || !ACTOR.matcher(createdBy).matches()) {
            throw new IllegalArgumentException("Style profile creator is invalid");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static StyleProfile fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_profile");
        return new StyleProfile(
                new Ids.StyleProfileId(StyleCanonical.string(
                        value, "profile_id", "style_profile"
                )),
                StyleCanonical.string(value, "name", "style_profile"),
                StyleProfileScope.fromCanonical(StyleCanonical.object(
                        value.get("scope"), "style_profile.scope"
                )),
                StyleCanonical.string(value, "provenance", "style_profile"),
                StyleCanonical.string(value, "created_by", "style_profile"),
                StyleCanonical.instant(value, "created_at", "style_profile")
        );
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "created_at", createdAt.toString(),
                "created_by", createdBy,
                "name", name,
                "profile_id", profileId.value(),
                "provenance", provenance,
                "scope", scope.canonicalValue()
        ), "style_profile");
    }

    public String resourceHash() {
        return CanonicalJson.hash(canonicalValue());
    }
}
