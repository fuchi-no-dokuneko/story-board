package dev.storyblock.style.quality;

import java.util.List;

/** One independent author/model source; multiple texts still count as one source. */
public record QualityCorpusSource(String id, Kind kind, List<String> texts) {
    public enum Kind { HUMAN, AI }
    public QualityCorpusSource {
        if (id == null || id.isBlank() || kind == null || texts == null || texts.isEmpty())
            throw new IllegalArgumentException("Corpus source needs an id, kind and texts");
        texts = List.copyOf(texts);
    }
}
