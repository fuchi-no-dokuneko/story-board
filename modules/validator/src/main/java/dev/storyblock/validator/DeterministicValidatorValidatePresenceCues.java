package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import static dev.storyblock.validator.DeterministicValidator.EXIT_CUE;
import static dev.storyblock.validator.DeterministicValidator.ENTER_CUE;

final class DeterministicValidatorValidatePresenceCues {
    static void validatePresenceCues(
            Ids.BlockId blockId,
            String text,
            List<Map<String, Object>> events,
            List<ValidationIssue> issues
    ) {
        DeterministicValidatorRequireEventForCue.requireEventForCue(blockId, text, events, "enter", ENTER_CUE, issues);
        DeterministicValidatorRequireEventForCue.requireEventForCue(blockId, text, events, "exit", EXIT_CUE, issues);
    }
}
