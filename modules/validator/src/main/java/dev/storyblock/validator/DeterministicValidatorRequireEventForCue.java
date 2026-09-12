package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DeterministicValidatorRequireEventForCue {
    static void requireEventForCue(
            Ids.BlockId blockId,
            String text,
            List<Map<String, Object>> events,
            String eventType,
            Pattern cuePattern,
            List<ValidationIssue> issues
    ) {
        Matcher matcher = cuePattern.matcher(text);
        if (!matcher.find()) {
            return;
        }
        boolean present = events.stream().anyMatch(event -> eventType.equals(event.get("type")));
        if (!present) {
            issues.add(ValidationIssue.error(
                    ValidationCode.PRESENCE_EVENT_REQUIRED,
                    blockId,
                    "Text contains an explicit presence transition without matching metadata",
                    Map.of(
                            "required_event_type", eventType,
                            "matched_cue", matcher.group(),
                            "cue_start_utf16", matcher.start(),
                            "rule_version", ValidatorModule.VERSION
                    )
            ));
        }
    }
}
