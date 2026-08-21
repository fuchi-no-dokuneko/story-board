package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleProfileVersion(
        Ids.StyleProfileVersionId versionId,
        Ids.StyleProfileId profileId,
        int version,
        StyleProfileVersionContent content,
        String createdBy,
        Instant createdAt
) {
    private static final Pattern ACTOR = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Set<String> FIELDS = Set.of(
            "version_id", "profile_id", "version", "content", "created_by", "created_at"
    );

    public StyleProfileVersion {
        Objects.requireNonNull(versionId, "versionId");
        Objects.requireNonNull(profileId, "profileId");
        if (version < 1) {
            throw new IllegalArgumentException("Style profile version number must be positive");
        }
        Objects.requireNonNull(content, "content");
        if (createdBy == null || !ACTOR.matcher(createdBy).matches()) {
            throw new IllegalArgumentException("Style profile version creator is invalid");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static StyleProfileVersion fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_profile_version");
        return new StyleProfileVersion(
                new Ids.StyleProfileVersionId(StyleCanonical.string(
                        value, "version_id", "style_profile_version"
                )),
                new Ids.StyleProfileId(StyleCanonical.string(
                        value, "profile_id", "style_profile_version"
                )),
                StyleCanonical.integer(value, "version", "style_profile_version"),
                StyleProfileVersionContent.fromCanonical(StyleCanonical.object(
                        value.get("content"), "style_profile_version.content"
                )),
                StyleCanonical.string(value, "created_by", "style_profile_version"),
                StyleCanonical.instant(value, "created_at", "style_profile_version")
        );
    }

    public String versionHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("content", content.canonicalValue());
        value.put("created_at", createdAt.toString());
        value.put("created_by", createdBy);
        value.put("profile_id", profileId.value());
        value.put("version", version);
        value.put("version_id", versionId.value());
        return CanonicalValues.freezeMap(value, "style_profile_version");
    }
}
