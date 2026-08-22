package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleStratum(StyleStratumKind kind, String speakerId) {
    private static final Pattern SUBJECT = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Set<String> FIELDS = Set.of("kind", "speaker_id");

    public StyleStratum {
        Objects.requireNonNull(kind, "kind");
        if (kind == StyleStratumKind.NARRATION && speakerId != null) {
            throw new IllegalArgumentException("Narration stratum cannot have a speaker");
        }
        if (speakerId != null && !SUBJECT.matcher(speakerId).matches()) {
            throw new IllegalArgumentException("Style speaker stratum ID is invalid");
        }
    }

    public static StyleStratum narration() {
        return new StyleStratum(StyleStratumKind.NARRATION, null);
    }

    public static StyleStratum dialogue() {
        return new StyleStratum(StyleStratumKind.DIALOGUE, null);
    }

    public static StyleStratum dialogue(String speakerId) {
        return new StyleStratum(StyleStratumKind.DIALOGUE, speakerId);
    }

    public static StyleStratum fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_stratum");
        return new StyleStratum(
                StyleStratumKind.fromCanonicalName(StyleCanonical.string(
                        value, "kind", "style_stratum"
                )),
                StyleCanonical.optionalString(value, "speaker_id", "style_stratum")
        );
    }

    public boolean speakerSpecific() {
        return kind == StyleStratumKind.DIALOGUE && speakerId != null;
    }

    public String canonicalKey() {
        return speakerId == null
                ? kind.canonicalName()
                : kind.canonicalName() + ":" + speakerId;
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("kind", kind.canonicalName());
        value.put("speaker_id", speakerId);
        return CanonicalValues.freezeMap(value, "style_stratum");
    }
}
