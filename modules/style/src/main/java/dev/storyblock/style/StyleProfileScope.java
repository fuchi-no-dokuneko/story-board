package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleProfileScope(
        Ids.NovelId novelId,
        StyleScopeKind kind,
        String subjectId
) {
    private static final Pattern SUBJECT = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Set<String> FIELDS = Set.of(
            "novel_id", "kind", "subject_id"
    );

    public StyleProfileScope {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(kind, "kind");
        if (kind == StyleScopeKind.NOVEL && subjectId != null) {
            throw new IllegalArgumentException("Novel style scope cannot have a subject ID");
        }
        if (kind != StyleScopeKind.NOVEL
                && (subjectId == null || !SUBJECT.matcher(subjectId).matches())) {
            throw new IllegalArgumentException(
                    "Series and character style scopes require a safe subject ID"
            );
        }
    }

    public static StyleProfileScope fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_profile_scope");
        return new StyleProfileScope(
                new Ids.NovelId(StyleCanonical.string(
                        value, "novel_id", "style_profile_scope"
                )),
                StyleScopeKind.fromCanonicalName(StyleCanonical.string(
                        value, "kind", "style_profile_scope"
                )),
                StyleCanonical.optionalString(value, "subject_id", "style_profile_scope")
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("kind", kind.canonicalName());
        value.put("novel_id", novelId.value());
        value.put("subject_id", subjectId);
        return CanonicalValues.freezeMap(value, "style_profile_scope");
    }
}
