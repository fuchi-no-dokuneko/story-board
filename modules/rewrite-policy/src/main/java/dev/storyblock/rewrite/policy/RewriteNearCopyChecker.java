package dev.storyblock.rewrite.policy;

import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.List;

public final class RewriteNearCopyChecker {
    public static final int NGRAM_GRAPHEMES = 16;

    public List<RewriteNearCopyFinding> check(
            RewriteTextProposal proposal,
            StyleProfileVersionView profile,
            List<RewriteReferenceCorpus> corpora
    ) {
        return RewriteNearCopyCheckerCheckAction.check(this, proposal, profile, corpora);
    }

}
