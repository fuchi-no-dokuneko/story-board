package dev.storyblock.rewrite.policy;

import dev.storyblock.style.StyleCorpusSourceKind;

final class RewriteNearCopyCheckerDisposition {
    static NearCopyDisposition disposition(StyleCorpusSourceKind kind) {
        return kind == StyleCorpusSourceKind.OWNER
                || kind == StyleCorpusSourceKind.PUBLIC_DOMAIN
                ? NearCopyDisposition.MANUAL_ONLY : NearCopyDisposition.BLOCK;
    }
}
