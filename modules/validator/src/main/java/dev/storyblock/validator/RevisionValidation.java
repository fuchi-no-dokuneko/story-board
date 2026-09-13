package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RevisionValidation {
    static ValidationReport validateRevision(DeterministicValidator self, RevisionManifest candidate, RevisionManifest base, String candidateHash)  {
        RenderPacket packet = self.renderer.render(candidate, candidateHash, RenderRange.all());
        Map<Ids.BlockId, ResolvedBlockMetadata> resolved = new HashMap<>();
        for (ResolvedBlockMetadata metadata : packet.resolvedMetadata()) {
            resolved.put(metadata.blockId(), metadata);
        }
        Map<Ids.BlockId, NarrativeBlock> baseline = new HashMap<>();
        if (base != null) {
            for (NarrativeBlock block : base.liveBlocks()) {
                baseline.put(block.id(), block);
            }
        }

        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        for (NarrativeBlock block : candidate.liveBlocks()) {
            ResolvedBlockMetadata state = resolved.get(block.id());
            Set<String> presentBefore = DeterministicValidatorStrings.strings(state.before().get("present_character_ids"));
            NarrativeBlock old = baseline.get(block.id());
            for (var issue : self.validateBlock(
                    block.id(),
                    block.text(),
                    block.metadata(),
                    presentBefore,
                    old == null ? null : old.metadata()
            ).issues()) {
                if (issue.code() == ValidationCode.PRESENCE_EVENT_REQUIRED && block.equals(old)) {
                    warnings.add(new ValidationIssue(issue.code(), ValidationSeverity.WARNING, issue.blockId(),
                            "Unchanged block has an existing unannotated presence cue", issue.details()));
                } else issues.add(issue);
            }
        }
        return new ValidationReport(issues, warnings);
    }
}
