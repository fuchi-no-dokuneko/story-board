package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record StyleMaskingLexicon(List<String> names, List<String> places) {
    private static final Set<String> FIELDS = Set.of("names", "places");
    public StyleMaskingLexicon {
        names = normalized(names, "names");
        places = normalized(places, "places");
    }

    public static StyleMaskingLexicon empty() {
        return new StyleMaskingLexicon(List.of(), List.of());
    }

    public static StyleMaskingLexicon fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_masking_lexicon");
        return new StyleMaskingLexicon(
                strings(value.get("names"), "style_masking_lexicon.names"),
                strings(value.get("places"), "style_masking_lexicon.places")
        );
    }

    public String mask(String text) {
        String result = Normalizer.normalize(text, Normalizer.Form.NFC);
        for (String name : names) {
            result = result.replace(name, "<NAME>");
        }
        for (String place : places) {
            result = result.replace(place, "<PLACE>");
        }
        return result.replaceAll("\\p{N}+", "<NUM>")
                .toLowerCase(java.util.Locale.ROOT);
    }

    public String vocabularyHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "names", names,
                "places", places
        ), "style_masking_lexicon");
    }

    private static List<String> normalized(List<String> values, String field) {
        List<String> result = new ArrayList<>();
        for (String value : List.copyOf(values)) {
            if (value == null || value.isBlank() || value.length() > 128) {
                throw new IllegalArgumentException("Style masking " + field + " is invalid");
            }
            result.add(Normalizer.normalize(value, Normalizer.Form.NFC));
        }
        if (new HashSet<>(result).size() != result.size()) {
            throw new IllegalArgumentException("Style masking " + field + " has duplicates");
        }
        result.sort(Comparator.comparingInt(String::length).reversed().thenComparing(value -> value));
        return List.copyOf(result);
    }

    private static List<String> strings(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof String text)) {
                throw new IllegalArgumentException(path + " must contain strings");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }
}
