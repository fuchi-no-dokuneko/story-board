package dev.storyblock.api.http;

import dev.storyblock.storage.StoredArtifact;

final class CanonicalTransferControllerArtifactExtension {
    static String artifactExtension(StoredArtifact artifact) {
        if ("gzip".equals(artifact.codec())) {
            return ".json.gz";
        }
        return switch (artifact.mediaType()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".json";
        };
    }
}
