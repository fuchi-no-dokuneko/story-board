package dev.storyblock.style;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

final class StyleMaskingLexiconNormalized {
    static List<String> normalized(List<String> values, String field) {
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
}
