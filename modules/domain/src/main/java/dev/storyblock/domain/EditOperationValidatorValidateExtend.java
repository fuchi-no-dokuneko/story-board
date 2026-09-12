package dev.storyblock.domain;

import java.util.List;
import static dev.storyblock.domain.EditOperationValidator.RangeLocation;

final class EditOperationValidatorValidateExtend {
    static void validateExtend(RevisionManifest base, EditOperation.ExtendBlock extend) {
        RangeLocation range = EditOperationValidator.validateRange(base, extend.block());
        NarrativeBlock original = range.blocks().getFirst();
        BlockDraft replacement = extend.replacement();
        if (original.image().isPresent() || replacement.image().isPresent()) {
            throw EditOperationValidatorInvalid.invalid("extend_block does not support image blocks");
        }
        if (!replacement.id().equals(original.id())) {
            throw EditOperationValidatorInvalid.invalid("extend_block must preserve the stable block ID");
        }
        boolean extendsCorrectSide = switch (extend.position()) {
            case BEFORE -> replacement.text().endsWith(original.text());
            case AFTER -> replacement.text().startsWith(original.text());
        };
        if (!extendsCorrectSide || replacement.text().equals(original.text())) {
            throw EditOperationValidatorInvalid.invalid("extend_block replacement must add text on the declared side");
        }
        EditOperationValidatorValidateDraftIdentities.validateDraftIdentities(base, List.of(replacement), EditOperationValidatorRangeIds.rangeIds(extend.block()));
    }
}
