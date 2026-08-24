package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.List;

public record AgentNovelRegistration(
        Ids.NovelId novelId,
        Instant createdAt,
        String title,
        String language,
        List<String> mainCharacters,
        int zombieCount,
        int tntCannonCount,
        int expectedHanCharacters,
        List<Chapter> chapters
) {
    public AgentNovelRegistration {
        mainCharacters = List.copyOf(mainCharacters);
        chapters = List.copyOf(chapters);
    }

    public record Chapter(String title, String text) {
    }
}
