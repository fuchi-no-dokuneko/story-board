package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;

final class ReplayServiceVerifyHeadAction {
    static ReplayVerification verifyHead(ReplayService self, Ids.NovelId novelId)  {
        RevisionRef head = null;
        try {
            head = self.store.getHead(novelId);
            ReplayResult result = self.materializeFull(novelId, head.revisionId());
            boolean valid = head.contentHash().equals(result.contentHash())
                    && head.sequence() == result.targetSequence();
            return new ReplayVerification(
                    novelId,
                    head.revisionId(),
                    head.contentHash(),
                    result.contentHash(),
                    result.replayedOperations(),
                    valid,
                    valid ? "head hash reproduced" : "head reference did not match replay"
            );
        } catch (RuntimeException exception) {
            return new ReplayVerification(
                    novelId,
                    head == null ? null : head.revisionId(),
                    head == null ? null : head.contentHash(),
                    null,
                    0,
                    false,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }
}
