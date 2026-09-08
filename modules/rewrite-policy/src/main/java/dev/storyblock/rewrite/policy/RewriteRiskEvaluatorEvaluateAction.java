package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

final class RewriteRiskEvaluatorEvaluateAction {
  static RewriteRiskAssessment evaluate(RewriteRiskEvaluator self, RewriteTextProposal proposal, List<NarrativeBlock> sourceBlocks, StyleMaskingLexicon lexicon, StyleProfileVersionView currentProfile, List<RewriteReferenceCorpus> corpora)  {
    java.util.Objects.requireNonNull(proposal, "proposal");
    sourceBlocks = List.copyOf(sourceBlocks);
    RewriteRiskEvaluatorRequireExactSources.requireExactSources(proposal, sourceBlocks);
    Map<Ids.BlockId, NarrativeBlock> sourceById = new HashMap<>();
    sourceBlocks.forEach(block -> sourceById.put(block.id(), block));

    List<RewriteProtectedFactSnapshot> sourceFacts = new ArrayList<>();
    List<RewriteProtectedFactSnapshot> candidateFacts = new ArrayList<>();
    List<RewriteFactDifference> differences = new ArrayList<>();
    TreeSet<String> manualReasons = new TreeSet<>();
    for (RewriteCandidateBlock candidate : proposal.candidates()) {
      NarrativeBlock source = sourceById.get(candidate.blockId());
      RewriteProtectedFactSnapshot before = self.facts.snapshot(
          source, source.text(), lexicon
      );
      RewriteProtectedFactSnapshot after = self.facts.snapshot(
          source, candidate.proposedText(), lexicon
      );
      sourceFacts.add(before);
      candidateFacts.add(after);
      differences.addAll(RewriteRiskEvaluatorCompare.compare(before, after));
      manualReasons.addAll(before.manualRiskReasons());
      before.facts().stream()
          .map(RewriteProtectedFact::kind)
          .filter(RewriteRiskEvaluator.SOURCE_MANUAL_RISK::contains)
          .map(kind -> "high_risk:" + kind.canonicalName())
          .forEach(manualReasons::add);
    }
    differences.sort(Comparator
        .comparing((RewriteFactDifference value) -> value.blockId().value())
        .thenComparing(value -> value.kind().ordinal())
        .thenComparing(RewriteFactDifference::valueHash));
    List<RewriteNearCopyFinding> nearCopyFindings = self.nearCopy.check(
        proposal, currentProfile, corpora
    );
    boolean blocked = differences.stream().anyMatch(value ->
        value.disposition() == RewriteFactDisposition.BLOCK)
        || nearCopyFindings.stream().anyMatch(value ->
        value.disposition() == NearCopyDisposition.BLOCK);
    boolean manual = !manualReasons.isEmpty()
        || differences.stream().anyMatch(value ->
        value.disposition() == RewriteFactDisposition.MANUAL_ONLY)
        || nearCopyFindings.stream().anyMatch(value ->
        value.disposition() == NearCopyDisposition.MANUAL_ONLY);
    return new RewriteRiskAssessment(
        proposal.proposalId(),
        proposal.proposalHash(),
        sourceFacts,
        candidateFacts,
        differences,
        nearCopyFindings,
        List.copyOf(manualReasons),
        blocked ? RewriteRiskState.BLOCKED
            : manual ? RewriteRiskState.MANUAL_ONLY
            : RewriteRiskState.SAFE
    );
  }
}
