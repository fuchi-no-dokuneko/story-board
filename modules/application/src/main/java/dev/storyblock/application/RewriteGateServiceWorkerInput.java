package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.rewrite.RewriteConstraints;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.rewrite.policy.RewriteEligibility;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleAnalysisJob;
import java.util.ArrayList;
import java.util.List;

final class RewriteGateServiceWorkerInput {
    static RewriteWorkerInput workerInput(
            StyleAnalysisJob analysis,
            RewriteEligibility eligibility
    ) {
        List<StyleAnalysisBlock> snapshot = analysis.snapshot().blocks();
        Ids.BlockId firstId = eligibility.affectedBlockIds().getFirst();
        Ids.BlockId lastId = eligibility.affectedBlockIds().getLast();
        int first = RewriteGateServiceIndexOf.indexOf(snapshot, firstId);
        int last = RewriteGateServiceIndexOf.indexOf(snapshot, lastId);
        int from = Math.max(0, first - RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE);
        int to = Math.min(
                snapshot.size(), last + RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE + 1
        );
        List<RewriteSourceBlock> blocks = new ArrayList<>();
        for (int index = from; index < to; index++) {
            var block = snapshot.get(index).block();
            blocks.add(RewriteSourceBlock.create(
                    block.id(), block.versionId(), block.text(),
                    index >= first && index <= last
            ));
        }
        List<String> directives = eligibility.decisions().stream()
                .flatMap(decision -> decision.independentQ99Channels().stream())
                .distinct()
                .sorted(java.util.Comparator.comparing(Enum::ordinal))
                .map(RewriteGateServiceDirective::directive)
                .toList();
        int editableCount = eligibility.affectedBlockIds().size();
        return new RewriteWorkerInput(
                Ids.ProposalId.create(),
                eligibility.analysisId(),
                eligibility.novelId(),
                eligibility.revisionId(),
                eligibility.revisionHash(),
                eligibility.profileVersionId(),
                eligibility.profileVersionHash(),
                eligibility.analyzerContractHash(),
                eligibility.windowConfigurationHash(),
                eligibility.findingIds(),
                blocks,
                new RewriteConstraints(
                        editableCount,
                        editableCount * UnicodeText.MAX_BLOCK_GRAPHEMES,
                        directives
                )
        );
    }
}
