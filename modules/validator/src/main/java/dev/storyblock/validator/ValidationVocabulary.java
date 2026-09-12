package dev.storyblock.validator;

import dev.storyblock.domain.*;
import java.util.Set;
import java.util.regex.Pattern;

final class ValidationVocabulary {
  static final Pattern ENTER_CUE = Pattern.compile(
      "走進|進入|闖入|踏入|來到|出現|\\b(?:enter(?:s|ed)?|arriv(?:e|es|ed)|walk(?:s|ed)? in)\\b",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );
  static final Pattern EXIT_CUE = Pattern.compile(
      "離開|走出|退出|離場|消失|\\b(?:exit(?:s|ed)?|lea(?:ve|ves|ft)|walk(?:s|ed)? out)\\b",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );
  static final Set<String> OBSERVATION_FIELDS = Set.of(
      "time", "location", "weather", "pov"
  );

}
