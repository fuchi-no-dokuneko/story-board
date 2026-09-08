package dev.storyblock.contracts;

import java.util.Set;
import java.util.regex.Pattern;

final class CanonicalRevisionFields {
  static final Set<String> ROOT_REQUIRED = Set.of(
      "schema_version",
      "novel_id",
      "revision_id",
      "parent_revision_id",
      "chapters",
      "created_at"
  );
  static final Set<String> ROOT_OPTIONAL = Set.of("extensions");
  static final Set<String> CHAPTER_REQUIRED = Set.of("id", "order_key", "scenes");
  static final Set<String> CHAPTER_OPTIONAL = Set.of("title", "extensions");
  static final Set<String> SCENE_REQUIRED = Set.of(
      "id",
      "chapter_id",
      "order_key",
      "transition_mode",
      "blocks"
  );
  static final Set<String> SCENE_OPTIONAL = Set.of(
      "title",
      "initial_meta",
      "extensions"
  );
  static final Set<String> BLOCK_REQUIRED = Set.of(
      "id",
      "block_version_id",
      "order_key",
      "text",
      "meta"
  );
  static final Set<String> BLOCK_OPTIONAL = Set.of("extensions");
  static final Set<String> META_FIELDS = Set.of(
      "time",
      "location",
      "weather",
      "speech",
      "actions",
      "presence_events",
      "pov",
      "narrative_mode",
      "provenance"
  );
  static final Set<String> INITIAL_META_FIELDS = Set.of(
      "time",
      "location",
      "weather",
      "present_character_ids"
  );
  static final Set<String> TRANSITION_MODES = Set.of(
      "opening",
      "continuous",
      "cut",
      "time_skip",
      "flashback",
      "parallel"
  );
  static final Pattern EXTENSION_NAME = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
  static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

}
