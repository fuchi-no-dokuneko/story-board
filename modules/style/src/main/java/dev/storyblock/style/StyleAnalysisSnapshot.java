package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleAnalysisSnapshot(
    Ids.NovelId novelId,
    Ids.RevisionId revisionId,
    String revisionHash,
    StyleProfileVersion profileVersion,
    StyleMaskingLexicon maskingLexicon,
    List<StyleAnalysisBlock> blocks
) {
  public static final int MAX_BLOCKS = 1_000;
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  static final Set<String> FIELDS = Set.of(
      "novel_id", "revision_id", "revision_hash", "profile_version",
      "masking_lexicon", "blocks"
  );

  public StyleAnalysisSnapshot {
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    StyleAnalysisSnapshotRequireHash.requireHash(revisionHash, "revision");
    Objects.requireNonNull(profileVersion, "profileVersion");
    if (!profileVersion.content().scope().novelId().equals(novelId)) {
      throw new IllegalArgumentException(
          "Style analysis profile scope does not match the novel"
      );
    }
    Objects.requireNonNull(maskingLexicon, "maskingLexicon");
    if (!maskingLexicon.vocabularyHash().equals(
        profileVersion.content().featureSet().contract().vocabularyHash()
    )) {
      throw new IllegalArgumentException(
          "Style analysis masking vocabulary does not match the profile contract"
      );
    }
    blocks = List.copyOf(blocks);
    if (blocks.isEmpty() || blocks.size() > MAX_BLOCKS) {
      throw new IllegalArgumentException(
          "Style analysis snapshot requires 1 to 1000 blocks"
      );
    }
    if (new HashSet<>(blocks.stream().map(block -> block.block().id()).toList())
        .size() != blocks.size()
        || new HashSet<>(blocks.stream().map(
            block -> block.block().versionId()
        ).toList()).size() != blocks.size()) {
      throw new IllegalArgumentException(
          "Style analysis snapshot block identities must be unique"
      );
    }
  }

  public static StyleAnalysisSnapshot fromCanonical(Map<String, Object> value) {
    return StyleAnalysisSnapshotFromCanonicalFactory.fromCanonical(value);
  }

  public String snapshotHash() {
    return CanonicalJson.hash(canonicalValue());
  }

  public String profileVersionHash() {
    return profileVersion.versionHash();
  }

  public String analyzerContractHash() {
    return profileVersion.content().featureSet().contract().contractHash();
  }

  public String windowConfigurationHash() {
    return profileVersion.content().windowConfiguration().configurationHash();
  }

  public Map<String, Object> canonicalValue() {
    return StyleAnalysisSnapshotCanonicalValueAction.canonicalValue(this);
  }

}
