package dev.storyblock.api.http;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.Ids;

final class PreviewControllerRequireContext {
    static void requireContext(
            EditContext context,
            Ids.NovelId novelId,
            String ifMatch,
            String idempotencyKey
    ) {
        if (!context.novelId().equals(novelId)
                || !context.idempotencyKey().equals(idempotencyKey)
                || !context.expectedHeadHash().equals(
                        StrictJsonRequest.unquoteEtag(ifMatch)
                )) {
            throw new IllegalArgumentException(
                    "Preview headers and operation context do not match"
            );
        }
    }
}
