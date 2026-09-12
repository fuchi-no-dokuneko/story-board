package dev.storyblock.api.http;

import dev.storyblock.application.AgentNovelRegistration;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AgentRegistrationInput {
  static AgentNovelRegistration parse(Map<String, Object> request, Ids.NovelId novelId) {
    List<AgentNovelRegistration.Chapter> chapters = StrictJsonRequest.objects(
        request.get("chapters"), "agent novel registration.chapters"
    ).stream().map(chapter -> {
      StrictJsonRequest.requireKeys(
          chapter, Set.of("text", "title"), "agent novel registration chapter"
      );
      return new AgentNovelRegistration.Chapter(
          StrictJsonRequest.string(
              chapter, "title", "agent novel registration chapter"
          ),
          StrictJsonRequest.string(
              chapter, "text", "agent novel registration chapter"
          )
      );
    }).toList();
    return new AgentNovelRegistration(
            novelId,
            StrictJsonRequest.instant(
                request, "created_at", "agent novel registration"
            ),
            StrictJsonRequest.string(
                request, "title", "agent novel registration"
            ),
            StrictJsonRequest.string(
                request, "language", "agent novel registration"
            ),
            StrictJsonRequest.uniqueStrings(
                request, "main_characters", "agent novel registration"
            ),
            StrictJsonRequest.integer(
                request, "expected_han_characters", "agent novel registration"
            ),
            chapters
        );
  }
}
