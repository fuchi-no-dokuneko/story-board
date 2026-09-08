package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.TextAnalysis;
import dev.storyblock.domain.UnicodeText;
import java.util.List;
import java.util.Map;

final class DeterministicValidatorValidateText {
    static void validateText(
            Ids.BlockId blockId,
            TextAnalysis analysis,
            List<ValidationIssue> issues
    ) {
        if (analysis.graphemeCount() > UnicodeText.MAX_BLOCK_GRAPHEMES) {
            issues.add(ValidationIssue.error(
                    ValidationCode.BLOCK_TOO_LONG,
                    blockId,
                    "Block exceeds the maximum grapheme count",
                    Map.of(
                            "actual_graphemes", analysis.graphemeCount(),
                            "limit", UnicodeText.MAX_BLOCK_GRAPHEMES,
                            "safe_split_anchors", analysis.safeSplitAnchors(),
                            "normalization_version", analysis.normalizationVersion()
                    )
            ));
        }
        if (analysis.sentenceCount() < 1 || analysis.sentenceCount() > 2 || !analysis.complete()) {
            issues.add(ValidationIssue.error(
                    ValidationCode.INVALID_SENTENCE_COUNT,
                    blockId,
                    "Block must contain one or two complete sentences",
                    Map.of(
                            "actual_sentence_count", analysis.sentenceCount(),
                            "complete", analysis.complete(),
                            "required_min", 1,
                            "required_max", 2,
                            "safe_split_anchors", analysis.safeSplitAnchors(),
                            "parser_version", analysis.parserVersion()
                    )
            ));
        }
    }
}
