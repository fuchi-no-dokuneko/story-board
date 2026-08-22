package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleCorpusSourceKind;
import dev.storyblock.style.StyleProfileVersionView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RewriteNearCopyChecker {
    public static final int NGRAM_GRAPHEMES = 16;

    public List<RewriteNearCopyFinding> check(
            RewriteTextProposal proposal,
            StyleProfileVersionView profile,
            List<RewriteReferenceCorpus> corpora
    ) {
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

        List<RewriteNearCopyFinding> result = new ArrayList<>();
        for (RewriteCandidateBlock candidate : proposal.candidates()) {
            List<String> candidateUnits = normalized(candidate.proposedText());
            List<String> candidateNgrams = ngrams(candidateUnits);
            if (candidateNgrams.isEmpty()) {
                continue;
            }
            for (StyleCorpusSource source : approved) {
                RewriteReferenceCorpus corpus = supplied.get(source.sourceId());
                Set<String> reference = new HashSet<>();
                for (NarrativeBlock block : corpus.blocks()) {
                    if (!block.id().equals(candidate.blockId())) {
                        reference.addAll(ngrams(normalized(block.text())));
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
                            NGRAM_GRAPHEMES,
                            matchedCount,
                            candidateNgrams.size(),
                            disposition(source.kind())
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

    private static NearCopyDisposition disposition(StyleCorpusSourceKind kind) {
        return kind == StyleCorpusSourceKind.OWNER
                || kind == StyleCorpusSourceKind.PUBLIC_DOMAIN
                ? NearCopyDisposition.MANUAL_ONLY : NearCopyDisposition.BLOCK;
    }

    private static List<String> normalized(String text) {
        return UnicodeText.graphemes(Normalizer.normalize(
                text, Normalizer.Form.NFC
        ).toLowerCase(Locale.ROOT)).stream().filter(unit -> !unit.isBlank()).toList();
    }

    private static List<String> ngrams(List<String> units) {
        if (units.size() < NGRAM_GRAPHEMES) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index + NGRAM_GRAPHEMES <= units.size(); index++) {
            result.add(String.join(
                    "", units.subList(index, index + NGRAM_GRAPHEMES)
            ));
        }
        return List.copyOf(result);
    }
}
