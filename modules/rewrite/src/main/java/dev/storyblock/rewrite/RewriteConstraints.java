package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.UnicodeText;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RewriteConstraints(
        int maxChangedBlocks,
        int maxOutputGraphemes,
        List<String> styleDirectives
) {
    private static final Set<String> FIELDS = Set.of(
            "max_changed_blocks", "max_output_graphemes", "style_directives"
    );

    public RewriteConstraints {
        if (maxChangedBlocks < 1
                || maxChangedBlocks > RewriteModule.MAX_EDITABLE_BLOCKS) {
            throw new IllegalArgumentException("Rewrite changed-block limit is invalid");
        }
        if (maxOutputGraphemes < 1 || maxOutputGraphemes
                > RewriteModule.MAX_EDITABLE_BLOCKS * UnicodeText.MAX_BLOCK_GRAPHEMES) {
            throw new IllegalArgumentException("Rewrite output grapheme limit is invalid");
        }
        styleDirectives = List.copyOf(styleDirectives);
        if (styleDirectives.isEmpty()
                || styleDirectives.size() > RewriteModule.MAX_STYLE_DIRECTIVES
                || new HashSet<>(styleDirectives).size() != styleDirectives.size()) {
            throw new IllegalArgumentException("Rewrite style directives are invalid");
        }
        for (String directive : styleDirectives) {
            if (directive == null || directive.isBlank() || directive.length() > 300
                    || !directive.equals(directive.strip())
                    || directive.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Rewrite style directive is invalid");
            }
        }
    }

    public static RewriteConstraints fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_constraints");
        return new RewriteConstraints(
                RewriteCanonical.integer(
                        value, "max_changed_blocks", "rewrite_constraints"
                ),
                RewriteCanonical.integer(
                        value, "max_output_graphemes", "rewrite_constraints"
                ),
                RewriteCanonical.strings(
                        value.get("style_directives"),
                        "rewrite_constraints.style_directives"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("max_changed_blocks", maxChangedBlocks);
        value.put("max_output_graphemes", maxOutputGraphemes);
        value.put("style_directives", styleDirectives);
        return CanonicalValues.freezeMap(value, "rewrite_constraints");
    }
}
