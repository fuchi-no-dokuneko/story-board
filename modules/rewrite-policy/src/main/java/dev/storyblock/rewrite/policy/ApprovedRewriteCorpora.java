package dev.storyblock.rewrite.policy;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ApprovedRewriteCorpora {
  static Map<String, RewriteReferenceCorpus> require(RewriteTextProposal proposal, StyleProfileVersionView profile, List<RewriteReferenceCorpus> corpora) {
    java.util.Objects.requireNonNull(proposal, "proposal");
    java.util.Objects.requireNonNull(profile, "profile");
    corpora = List.copyOf(corpora);
    if (!profile.canGateRewrites()
        || !proposal.input().profileVersionId().equals(
            profile.profileVersion().versionId()
        )
        || !proposal.input().profileVersionHash().equals(
            profile.profileVersion().versionHash()
        )) {
      throw new RewriteRiskPolicyException(
          "Near-copy checking requires the proposal's exact READY profile"
      );
    }
    Map<String, RewriteReferenceCorpus> supplied = new HashMap<>();
    for (RewriteReferenceCorpus corpus : corpora) {
      if (supplied.put(corpus.source().sourceId(), corpus) != null) {
        throw new RewriteRiskPolicyException(
            "Rewrite reference corpus IDs must be unique"
        );
      }
    }
    List<StyleCorpusSource> approved = profile.profileVersion().content()
        .corpusSources();
    if (approved.size() != supplied.size()) {
      throw new RewriteRiskPolicyException(
          "Every approved corpus must be supplied for near-copy checking"
      );
    }
    for (StyleCorpusSource source : approved) {
      RewriteReferenceCorpus corpus = supplied.get(source.sourceId());
      if (corpus == null || !corpus.source().equals(source)) {
        throw new RewriteRiskPolicyException(
            "Rewrite corpus provenance does not match the approved profile"
        );
      }
    }

    return supplied;
  }
}
