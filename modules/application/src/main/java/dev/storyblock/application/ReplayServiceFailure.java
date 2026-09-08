package dev.storyblock.application;

import dev.storyblock.domain.Ids;

final class ReplayServiceFailure {
    static ReplayException failure(
            Ids.NovelId novelId,
            long sequence,
            String message
    ) {
        return new ReplayException(novelId, sequence, message);
    }
}
