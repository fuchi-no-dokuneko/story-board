package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.UnicodeText;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

final class RewriteNearCopyCheckerNormalized {
    static List<String> normalized(String text) {
        return UnicodeText.graphemes(Normalizer.normalize(
                text, Normalizer.Form.NFC
        ).toLowerCase(Locale.ROOT)).stream().filter(unit -> !unit.isBlank()).toList();
    }
}
