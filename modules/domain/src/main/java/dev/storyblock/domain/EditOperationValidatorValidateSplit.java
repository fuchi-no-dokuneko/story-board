package dev.storyblock.domain;

import static dev.storyblock.domain.EditOperationValidator.RangeLocation;

final class EditOperationValidatorValidateSplit {
    static void validateSplit(RevisionManifest base, EditOperation.SplitBlock split) {
        RangeLocation range = EditOperationValidator.validateRange(base, split.block());
        NarrativeBlock original = range.blocks().getFirst();
        if (original.image().isPresent()
                || split.newBlocks().stream().anyMatch(draft -> draft.image().isPresent())) {
            throw EditOperationValidatorInvalid.invalid("split_block does not support image blocks");
        }
        if (!UnicodeText.analyze(original.text()).safeSplitAnchors().contains(split.splitAfterGrapheme())) {
            throw EditOperationValidatorInvalid.invalid("split_block anchor is not a sentence, dialogue, or author-approved boundary");
        }
        String joined = split.newBlocks().get(0).text() + split.newBlocks().get(1).text();
        if (!joined.equals(original.text())) {
            throw EditOperationValidatorInvalid.invalid("split_block output must preserve the original text exactly");
        }
        if (UnicodeText.graphemeCount(split.newBlocks().getFirst().text()) != split.splitAfterGrapheme()) {
            throw EditOperationValidatorInvalid.invalid("split_block payload does not match its grapheme anchor");
        }
        EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, split.newBlocks(), EditOperationValidatorRangeIds.rangeIds(split.block()));
    }
}
