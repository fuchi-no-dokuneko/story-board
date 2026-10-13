package dev.storyblock.domain;

import java.text.Normalizer;

public final class HiddenText {
    public static final int MAX_GRAPHEMES = 2000;

    private HiddenText() { }

    public static String validate(String text) {
        if (text == null) throw invalid();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).strip();
        long count = UnicodeText.GRAPHEME.matcher(normalized).results().count();
        if (normalized.isEmpty() || count > MAX_GRAPHEMES) throw invalid();
        return normalized;
    }

    private static HiddenBlockException invalid() {
        return new HiddenBlockException(HiddenBlockException.Code.HIDDEN_BLOCK_TEXT_INVALID,
                "Hidden text must contain 1 to 2000 graphemes after NFC normalization and stripping");
    }
}
