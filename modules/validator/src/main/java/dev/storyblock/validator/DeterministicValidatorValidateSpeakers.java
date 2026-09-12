package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class DeterministicValidatorValidateSpeakers {
    static void validateSpeakers(
            Ids.BlockId blockId,
            Map<String, Object> metadata,
            Set<String> presentBefore,
            Set<String> presentAfter,
            List<ValidationIssue> issues
    ) {
        Set<String> speakers = DeterministicValidatorDirectSpeakers.directSpeakers(metadata.get("speech"));
        if (speakers.size() > 1) {
            issues.add(ValidationIssue.error(
                    ValidationCode.MULTIPLE_DIRECT_SPEAKERS,
                    blockId,
                    "A block can have at most one direct speaker",
                    Map.of("direct_speaker_ids", List.copyOf(new TreeSet<>(speakers)))
            ));
        }
        Set<String> available = new HashSet<>(presentBefore);
        available.addAll(presentAfter);
        for (String speaker : speakers) {
            if (!available.contains(speaker)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.SPEAKER_NOT_PRESENT,
                        blockId,
                        "Direct speaker is not present in the resolved scene state",
                        Map.of(
                                "speaker_id", speaker,
                                "present_character_ids", List.copyOf(new TreeSet<>(available))
                        )
                ));
            }
        }
    }
}
