package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.List;

public record NovelCatalogEntry(
        Ids.NovelId novelId,
        String title,
        String language,
        Ids.RevisionId headRevisionId,
        long headSequence,
        String headHash,
        Instant updatedAt,
        int chapterCount,
        int sceneCount,
        int blockCount,
        int hanCharacterCount,
        String hanTextSha256,
        List<String> mainCharacters,
        int zombieCount,
        int tntCannonCount,
        boolean agentWriteRegistered
) {
    public NovelCatalogEntry {
        mainCharacters = List.copyOf(mainCharacters);
    }
}
