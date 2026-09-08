package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record StyleMaskingLexicon(List<String> names, List<String> places) {
    private static final Set<String> FIELDS = Set.of("names", "places");
    public StyleMaskingLexicon {
        names = StyleMaskingLexiconNormalized.normalized(names, "names");
        places = StyleMaskingLexiconNormalized.normalized(places, "places");
    }

    public static StyleMaskingLexicon empty() {
        return new StyleMaskingLexicon(List.of(), List.of());
    }

    public static StyleMaskingLexicon fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_masking_lexicon");
        return new StyleMaskingLexicon(
                StyleMaskingLexiconStrings.strings(value.get("names"), "style_masking_lexicon.names"),
                StyleMaskingLexiconStrings.strings(value.get("places"), "style_masking_lexicon.places")
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

}
