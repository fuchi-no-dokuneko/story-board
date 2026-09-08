package dev.storyblock.validator;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.TextAnalysis;
import dev.storyblock.domain.UnicodeText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.validator.DeterministicValidator.BlockValidation;

final class DeterministicValidatorValidateBlockAction {
    static BlockValidation validateBlock(DeterministicValidator self, Ids.BlockId blockId, String text, BlockMetadata metadata, Set<String> presentBefore, BlockMetadata baselineMetadata)  {
        List<ValidationIssue> issues = new ArrayList<>();
        TextAnalysis analysis = UnicodeText.analyze(text);
        DeterministicValidatorValidateText.validateText(blockId, analysis, issues);

        List<Map<String, Object>> events = DeterministicValidatorMaps.maps(metadata.fields().get("presence_events"));
        DeterministicValidatorValidateEvidence.validateEvidence(blockId, text, metadata.fields(), events, issues);
        Set<String> presentAfter = DeterministicValidatorApplyEvents.applyEvents(presentBefore, events);
        DeterministicValidatorValidateSpeakers.validateSpeakers(blockId, metadata.fields(), presentBefore, presentAfter, issues);
        DeterministicValidatorValidatePresenceCues.validatePresenceCues(blockId, text, events, issues);
        DeterministicValidatorValidateUnknownHandling.validateUnknownHandling(blockId, text, metadata, baselineMetadata, issues);
        return new BlockValidation(List.copyOf(issues), Set.copyOf(presentAfter));
    }
}
