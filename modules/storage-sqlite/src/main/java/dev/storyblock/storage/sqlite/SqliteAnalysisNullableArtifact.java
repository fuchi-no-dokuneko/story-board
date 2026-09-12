package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;

final class SqliteAnalysisNullableArtifact {
    static Ids.ArtifactId nullableArtifact(String value) {
        return value == null ? null : new Ids.ArtifactId(value);
    }
}
