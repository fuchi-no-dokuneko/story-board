package dev.storyblock.style.quality;

import java.util.List;

/** Caller loads this JSON value; quality computations never read files. */
public record QualityInputs(QualityContract contract, List<QualityCorpusSource> sources) {
    public QualityInputs {
        contract = contract == null ? QualityContract.defaults() : contract;
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
    public static QualityInputs empty() { return new QualityInputs(null,null); }
}
