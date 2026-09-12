package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RewriteNearCopyCheckerCheckAction {
  static List<RewriteNearCopyFinding> check(RewriteNearCopyChecker self, RewriteTextProposal proposal, StyleProfileVersionView profile, List<RewriteReferenceCorpus> corpora)  {
    Map<String, RewriteReferenceCorpus> supplied = ApprovedRewriteCorpora.require(proposal, profile, corpora);
    List<StyleCorpusSource> approved = profile.profileVersion().content().corpusSources();

    List<RewriteNearCopyFinding> result = new ArrayList<>();
    for (RewriteCandidateBlock candidate : proposal.candidates()) {
      List<String> candidateUnits = RewriteNearCopyCheckerNormalized.normalized(candidate.proposedText());
      List<String> candidateNgrams = RewriteNearCopyCheckerNgrams.ngrams(candidateUnits);
      if (candidateNgrams.isEmpty()) {
        continue;
      }
      for (StyleCorpusSource source : approved) {
        RewriteReferenceCorpus corpus = supplied.get(source.sourceId());
        Set<String> reference = new HashSet<>();
        for (NarrativeBlock block : corpus.blocks()) {
          if (!block.id().equals(candidate.blockId())) {
            reference.addAll(RewriteNearCopyCheckerNgrams.ngrams(RewriteNearCopyCheckerNormalized.normalized(block.text())));
          }
        }
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        int matchedCount = 0;
        for (String ngram : candidateNgrams) {
          if (reference.contains(ngram)) {
            matched.add(CanonicalJson.hash(ngram));
            matchedCount++;
          }
        }
        if (!matched.isEmpty()) {
          List<String> evidenceHashes = matched.stream().sorted().toList();
          result.add(new RewriteNearCopyFinding(
              candidate.blockId(),
              source.sourceId(),
              source.kind(),
              CanonicalJson.hash(evidenceHashes),
              RewriteNearCopyChecker.NGRAM_GRAPHEMES,
              matchedCount,
              candidateNgrams.size(),
              RewriteNearCopyCheckerDisposition.disposition(source.kind())
          ));
        }
      }
    }
    return result.stream().sorted(Comparator
        .comparing((RewriteNearCopyFinding value) ->
            value.candidateBlockId().value())
        .thenComparing(RewriteNearCopyFinding::sourceId))
        .toList();
  }
}
