package dev.storyblock.rewrite.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteConstraints;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleCalibrationProfile;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleChannelCalibration;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleCorpusSourceKind;
import dev.storyblock.style.StyleFeatureAnalyzer;
import dev.storyblock.style.StyleFeatureChannel;
import dev.storyblock.style.StyleFeatureContract;
import dev.storyblock.style.StyleLifecycleEvent;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleModule;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersion;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleProfileVersionView;
import dev.storyblock.style.StyleScopeKind;
import dev.storyblock.style.StyleStratum;
import dev.storyblock.style.StyleStratumCalibration;
import dev.storyblock.style.StyleWindowConfiguration;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RewritePolicyEvaluationTest {
    private static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");

    @Test
    void reportsFactPreservationNearCopyAndExecutionLimits() throws Exception {
        StyleMaskingLexicon lexicon = new StyleMaskingLexicon(
                List.of("Alice"), List.of("Hong Kong")
        );
        NarrativeBlock source = block(
                "Alice did not carry 12 keys from Hong Kong.",
                new BlockMetadata(Map.of("speech", Map.of(
                        "direct_speaker_id", "alice"
                )))
        );
        RewriteProtectedFactExtractor extractor = new RewriteProtectedFactExtractor();
        RewriteProtectedFactSnapshot before = extractor.snapshot(
                source, source.text(), lexicon
        );
        RewriteProtectedFactSnapshot preserved = extractor.snapshot(
                source,
                "At dawn, Alice still did not carry 12 keys from Hong Kong.",
                lexicon
        );
        RewriteProtectedFactSnapshot changed = extractor.snapshot(
                source, "At dawn, the traveler carried keys.", lexicon
        );
        Map<ProtectedFactKind, Integer> beforeCounts = counts(before);
        Map<ProtectedFactKind, Integer> preservedCounts = counts(preserved);
        Map<ProtectedFactKind, Integer> changedCounts = counts(changed);

        assertEquals(beforeCounts, preservedCounts);
        assertTrue(beforeCounts.keySet().containsAll(List.of(
                ProtectedFactKind.NAME,
                ProtectedFactKind.NUMBER,
                ProtectedFactKind.NEGATION,
                ProtectedFactKind.SPEAKER
        )));
        assertFalse(changedCounts.containsKey(ProtectedFactKind.NAME));
        assertFalse(changedCounts.containsKey(ProtectedFactKind.NUMBER));
        assertFalse(changedCounts.containsKey(ProtectedFactKind.NEGATION));
        assertTrue(changedCounts.containsKey(ProtectedFactKind.SPEAKER));

        String referenceText =
                "Moonlight crossed the quiet harbor while every shutter stayed closed.";
        NarrativeBlock reference = block(referenceText, BlockMetadata.empty());
        ReadyProfile ready = readyProfile(reference);
        RewriteTextProposal proposal = proposal(source, referenceText, ready.view());
        List<RewriteNearCopyFinding> findings = new RewriteNearCopyChecker().check(
                proposal,
                ready.view(),
                List.of(new RewriteReferenceCorpus(ready.source(), List.of(reference)))
        );
        assertEquals(1, findings.size());
        assertEquals(NearCopyDisposition.MANUAL_ONLY,
                findings.getFirst().disposition());
        assertTrue(findings.getFirst().matchedNgramCount() > 0);

        Map<String, Object> report = Map.of(
                "attempt_limits", Map.of(
                        "analysis_maximum", StyleAnalysisJob.MAX_ATTEMPTS,
                        "analysis_minimum", StyleAnalysisJob.MIN_ATTEMPTS,
                        "enforced_by", "StyleAnalysisJob"
                ),
                "cooldown", Map.of(
                        "default_seconds", RewritePolicyModule.DEFAULT_COOLDOWN.toSeconds(),
                        "maximum_seconds", RewritePolicyModule.MAX_COOLDOWN.toSeconds(),
                        "minimum_seconds", RewritePolicyModule.MIN_COOLDOWN.toSeconds()
                ),
                "near_copy", Map.of(
                        "disposition", findings.getFirst().disposition().canonicalName(),
                        "long_ngram_graphemes", RewriteNearCopyChecker.NGRAM_GRAPHEMES,
                        "matched", true
                ),
                "preservation", Map.of(
                        "changed_detected", List.of("name", "number", "negation"),
                        "preserved", List.of("name", "number", "negation", "speaker")
                ),
                "schema_version", "adr-317-rewrite-evaluation-1"
        );
        Path output = Path.of("target/evaluations/rewrite-policy.json");
        Files.createDirectories(output.getParent());
        Files.write(output, CanonicalJson.bytes(report));
    }

    private static Map<ProtectedFactKind, Integer> counts(
            RewriteProtectedFactSnapshot snapshot
    ) {
        Map<ProtectedFactKind, Integer> result = new EnumMap<>(ProtectedFactKind.class);
        snapshot.facts().forEach(fact -> result.merge(
                fact.kind(), fact.count(), Integer::sum
        ));
        return result;
    }

    private static RewriteTextProposal proposal(
            NarrativeBlock source,
            String proposedText,
            StyleProfileVersionView profile
    ) {
        RewriteSourceBlock inputBlock = RewriteSourceBlock.create(
                source.id(), source.versionId(), source.text(), true
        );
        RewriteWorkerInput input = new RewriteWorkerInput(
                Ids.ProposalId.create(),
                Ids.StyleAnalysisId.create(),
                Ids.NovelId.create(),
                Ids.RevisionId.create(),
                CanonicalJson.hash("evaluation-revision"),
                profile.profileVersion().versionId(),
                profile.profileVersion().versionHash(),
                CanonicalJson.hash("evaluation-analyzer"),
                CanonicalJson.hash("evaluation-windows"),
                List.of(CanonicalJson.hash("evaluation-finding")),
                List.of(inputBlock),
                new RewriteConstraints(1, 100, List.of("Reduce style drift."))
        );
        return new RewriteTextProposal(
                input,
                "evaluation-model",
                CanonicalJson.hash("evaluation-response"),
                List.of(RewriteCandidateBlock.create(inputBlock, proposedText)),
                NOW
        );
    }

    private static ReadyProfile readyProfile(NarrativeBlock reference) {
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        var features = new StyleFeatureAnalyzer().extract(
                List.of(reference),
                lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash())
        );
        StyleWindowConfiguration windows = StyleWindowConfiguration.defaults();
        List<BigDecimal> distances = Collections.nCopies(30, new BigDecimal("0.1"));
        StyleCalibrationProfile calibration = new StyleCalibrationProfile(
                StyleModule.CALIBRATION_SCHEMA_VERSION,
                features.sourceHash(),
                features.contract().contractHash(),
                windows.configurationHash(),
                List.of(new StyleStratumCalibration(
                        StyleStratum.narration(),
                        30,
                        StyleFeatureChannel.requiredChannels().stream()
                                .sorted(java.util.Comparator.comparing(Enum::ordinal))
                                .map(channel -> StyleChannelCalibration.fromDistances(
                                        channel, distances
                                )).toList()
                ))
        );
        StyleCorpusSource corpusSource = new StyleCorpusSource(
                "owner-evaluation-corpus",
                features.sourceHash(),
                StyleCorpusSourceKind.OWNER,
                "ADR-317 evaluation fixture",
                "test-only",
                "test-owner"
        );
        Ids.StyleProfileId profileId = Ids.StyleProfileId.create();
        Ids.StyleProfileVersionId versionId = Ids.StyleProfileVersionId.create();
        StyleProfileVersion version = new StyleProfileVersion(
                versionId,
                profileId,
                1,
                new StyleProfileVersionContent(
                        new StyleProfileScope(
                                Ids.NovelId.create(), StyleScopeKind.NOVEL, null
                        ),
                        List.of(corpusSource),
                        features,
                        windows,
                        calibration.canonicalValue()
                ),
                "evaluation-owner",
                NOW
        );
        List<StyleLifecycleEvent> events = new ArrayList<>();
        events.add(StyleLifecycleEvent.initial(
                Ids.StyleLifecycleEventId.create(), profileId, versionId,
                AuditContext.system("evaluation-draft", NOW)
        ));
        events.add(event(
                profileId, versionId, 2,
                StyleProfileState.DRAFT, StyleProfileState.CALIBRATING,
                "evaluation-calibrating", NOW.plusSeconds(1)
        ));
        events.add(event(
                profileId, versionId, 3,
                StyleProfileState.CALIBRATING, StyleProfileState.READY,
                "evaluation-ready", NOW.plusSeconds(2)
        ));
        return new ReadyProfile(StyleProfileVersionView.of(version, events), corpusSource);
    }

    private static StyleLifecycleEvent event(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            int sequence,
            StyleProfileState from,
            StyleProfileState to,
            String requestId,
            Instant at
    ) {
        AuditContext audit = AuditContext.system(requestId, at);
        return new StyleLifecycleEvent(
                Ids.StyleLifecycleEventId.create(),
                profileId,
                versionId,
                sequence,
                from,
                to,
                "ADR-317 evaluation transition",
                false,
                audit,
                at
        );
    }

    private static NarrativeBlock block(String text, BlockMetadata metadata) {
        return NarrativeBlock.create(
                Ids.BlockId.create(), OrderKey.initial(), text, metadata, Map.of()
        );
    }

    private record ReadyProfile(
            StyleProfileVersionView view,
            StyleCorpusSource source
    ) {
    }
}
