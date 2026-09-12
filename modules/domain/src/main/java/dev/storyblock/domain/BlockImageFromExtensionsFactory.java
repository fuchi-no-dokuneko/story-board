package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class BlockImageFromExtensionsFactory {
    static Optional<BlockImage> fromExtensions(Map<String, Object> extensions)  {
        Objects.requireNonNull(extensions, "extensions");
        Object raw = extensions.get(BlockImage.EXTENSION_KEY);
        if (raw == null) {
            return Optional.empty();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(BlockImage.EXTENSION_KEY + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String string) || !BlockImage.FIELDS.contains(string)) {
                throw new IllegalArgumentException(
                        BlockImage.EXTENSION_KEY + " contains an unknown field " + key
                );
            }
        }
        for (String field : BlockImage.FIELDS) {
            if (!map.containsKey(field)) {
                throw new IllegalArgumentException(BlockImage.EXTENSION_KEY + " is missing " + field);
            }
        }
        return Optional.of(new BlockImage(
                new Ids.ArtifactId(BlockImageString.string(map, "artifact_id")),
                BlockImageString.string(map, "content_hash"),
                BlockImageString.string(map, "media_type"),
                BlockImageInteger.integer(map, "width_px"),
                BlockImageInteger.integer(map, "height_px"),
                BlockImageString.string(map, "alt_text")
        ));
    }
}
