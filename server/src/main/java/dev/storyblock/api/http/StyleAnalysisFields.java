package dev.storyblock.api.http;

import java.util.Set;
import org.springframework.web.bind.annotation.*;

final class StyleAnalysisFields {
  static final Set<String> CREATE_FIELDS = Set.of(
      "revision_id", "profile_id", "profile_version_id", "from_block_id",
      "to_block_id", "masking_lexicon", "max_attempts", "retention_days"
  );
  static final Set<String> CLAIM_FIELDS = Set.of(
      "novel_id", "lease_owner", "lease_seconds"
  );
  static final Set<String> RESULT_FIELDS = Set.of(
      "lease_owner", "attempt", "snapshot_hash", "profile_version_hash",
      "analyzer_contract_hash", "window_configuration_hash", "summary",
      "windows", "trace", "completed_at"
  );
  static final Set<String> COMPRESSED_TRACE_FIELDS = Set.of(
      "codec", "content_base64", "content_hash", "uncompressed_bytes"
  );

}
