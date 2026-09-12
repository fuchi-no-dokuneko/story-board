package dev.storyblock.application;

import java.util.LinkedHashSet;
import java.util.Objects;

final class AgentNovelRegistrationServiceValidate {
    static void validate(AgentNovelRegistration request) {
        Objects.requireNonNull(request, "request");
        AgentNovelRegistrationServiceRequireText.requireText(request.title(), "title", 200);
        AgentNovelRegistrationServiceRequireText.requireText(request.language(), "language", 32);
        if (new LinkedHashSet<>(request.mainCharacters()).size() != request.mainCharacters().size()) {
            throw new IllegalArgumentException(
                    "main_characters must contain unique character names"
            );
        }
        request.mainCharacters().forEach(name -> AgentNovelRegistrationServiceRequireText.requireText(name, "main character", 80));
        if (request.expectedHanCharacters() < 0) {
            throw new IllegalArgumentException("expected_han_characters cannot be negative");
        }
        if (request.chapters().isEmpty() || request.chapters().size() > 100) {
            throw new IllegalArgumentException("chapters must contain 1 to 100 entries");
        }
        for (AgentNovelRegistration.Chapter chapter : request.chapters()) {
            Objects.requireNonNull(chapter, "chapter");
            AgentNovelRegistrationServiceRequireText.requireText(chapter.title(), "chapter title", 200);
            if (chapter.text() == null || chapter.text().isBlank()) {
                throw new IllegalArgumentException("chapter text cannot be blank");
            }
        }
    }
}
