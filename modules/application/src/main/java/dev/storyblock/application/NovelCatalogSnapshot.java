package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;

public record NovelCatalogSnapshot(NovelCatalogEntry novel, CanonicalRevision revision) { }
