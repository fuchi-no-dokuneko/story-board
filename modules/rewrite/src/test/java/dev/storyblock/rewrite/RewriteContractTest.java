package dev.storyblock.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RewriteContractTest {
    @Test
    void inputAndProposalRoundTripWithExactImmutableBindings() {
        RewriteWorkerInput input = input();
        RewriteWorkerInput restored = RewriteWorkerInput.fromCanonical(
                object(CanonicalJson.bytes(input.canonicalValue()))
        );
        assertEquals(input, restored);
        assertEquals(input.inputHash(), restored.inputHash());

        RewriteTextProposal proposal = new RewriteTextProposal(
                input,
                "fixture-model-v1",
                CanonicalJson.hash("model-response"),
                List.of(RewriteCandidateBlock.create(
                        input.blocks().get(1), "The measured rain eased into silence."
                )),
                Instant.parse("2026-08-22T02:00:00Z")
        );
        RewriteTextProposal restoredProposal = RewriteTextProposal.fromCanonical(
                object(CanonicalJson.bytes(proposal.canonicalValue()))
        );
        assertEquals(proposal, restoredProposal);
        assertEquals(proposal.proposalHash(), restoredProposal.proposalHash());
    }

    @Test
    void editableRangeMustBeContiguousAndHaveAtMostTwoContextBlocksPerSide() {
        RewriteWorkerInput valid = input();
        List<RewriteSourceBlock> interrupted = List.of(
                block("First target sentence.", true),
                block("Read-only sentence.", false),
                block("Second target sentence.", true)
        );
        assertThrows(IllegalArgumentException.class, () -> withBlocks(
                valid, interrupted, new RewriteConstraints(
                        1, 100, List.of("Use a steadier rhythm.")
                )
        ));

        List<RewriteSourceBlock> excessiveContext = new ArrayList<>();
        excessiveContext.add(block("First context sentence.", false));
        excessiveContext.add(block("Second context sentence.", false));
        excessiveContext.add(block("Third context sentence.", false));
        excessiveContext.add(block("Target sentence.", true));
        assertThrows(IllegalArgumentException.class, () -> withBlocks(
                valid, excessiveContext, new RewriteConstraints(
                        1, 100, List.of("Use a steadier rhythm.")
                )
        ));
    }

    @Test
    void strictCanonicalReadersRejectUnknownFieldsAndMutatedBindings() {
        RewriteWorkerInput input = input();
        Map<String, Object> unknown = new LinkedHashMap<>(input.canonicalValue());
        unknown.put("commit_token", "not-allowed");
        assertThrows(
                IllegalArgumentException.class,
                () -> RewriteWorkerInput.fromCanonical(unknown)
        );

        RewriteCandidateBlock wrongSource = new RewriteCandidateBlock(
                input.blocks().get(1).blockId(),
                Ids.BlockVersionId.create(),
                input.blocks().get(1).textHash(),
                "The measured rain eased into silence.",
                CanonicalJson.hash("The measured rain eased into silence.")
        );
        assertThrows(IllegalArgumentException.class, () -> new RewriteTextProposal(
                input,
                "fixture-model-v1",
                CanonicalJson.hash("model-response"),
                List.of(wrongSource),
                Instant.parse("2026-08-22T02:00:00Z")
        ));

        List<RewriteSourceBlock> editable = List.of(
                block("First editable sentence.", true),
                block("Second editable sentence.", true)
        );
        RewriteWorkerInput twoBlockInput = withBlocks(
                input,
                editable,
                new RewriteConstraints(
                        2, 200, List.of("Use a steadier rhythm.")
                )
        );
        List<RewriteCandidateBlock> reversed = List.of(
                RewriteCandidateBlock.create(
                        editable.get(1), "The second sentence became measured."
                ),
                RewriteCandidateBlock.create(
                        editable.get(0), "The first sentence became measured."
                )
        );
        assertThrows(IllegalArgumentException.class, () -> new RewriteTextProposal(
                twoBlockInput,
                "fixture-model-v1",
                CanonicalJson.hash("model-response"),
                reversed,
                Instant.parse("2026-08-22T02:00:00Z")
        ));
    }

    static RewriteWorkerInput input() {
        return new RewriteWorkerInput(
                Ids.ProposalId.create(),
                Ids.StyleAnalysisId.create(),
                Ids.NovelId.create(),
                Ids.RevisionId.create(),
                CanonicalJson.hash("revision"),
                Ids.StyleProfileVersionId.create(),
                CanonicalJson.hash("profile"),
                CanonicalJson.hash("analyzer"),
                CanonicalJson.hash("windows"),
                List.of(CanonicalJson.hash("finding")),
                List.of(
                        block("The clouds gathered over the harbor.", false),
                        block("The rain fell hard against the empty street.", true),
                        block("At dawn, the shutters opened again.", false)
                ),
                new RewriteConstraints(
                        1,
                        100,
                        List.of(
                                "Use a steadier sentence rhythm.",
                                "Reduce repeated surface phrasing."
                        )
                )
        );
    }

    private static RewriteWorkerInput withBlocks(
            RewriteWorkerInput source,
            List<RewriteSourceBlock> blocks,
            RewriteConstraints constraints
    ) {
        return new RewriteWorkerInput(
                source.proposalId(),
                source.analysisId(),
                source.novelId(),
                source.revisionId(),
                source.revisionHash(),
                source.profileVersionId(),
                source.profileVersionHash(),
                source.analyzerContractHash(),
                source.windowConfigurationHash(),
                source.findingIds(),
                blocks,
                constraints
        );
    }

    private static RewriteSourceBlock block(String text, boolean editable) {
        return RewriteSourceBlock.create(
                Ids.BlockId.create(), Ids.BlockVersionId.create(), text, editable
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(byte[] value) {
        return CanonicalJson.mapper().readValue(value, Map.class);
    }
}
