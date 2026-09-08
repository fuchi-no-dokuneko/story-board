package dev.storyblock.contracts;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockImage;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditOperationCanonicalMapperDraft {
    static Map<String, Object> draft(BlockDraft draft) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", draft.id().value());
        result.put("text", draft.text());
        result.put("meta", draft.metadata().fields());
        draft.image().ifPresent(image -> result.put("image", image.canonicalValue()));
        Map<String, Object> publicExtensions = new LinkedHashMap<>(draft.extensions());
        publicExtensions.remove(BlockImage.EXTENSION_KEY);
        if (!publicExtensions.isEmpty()) {
            result.put("extensions", Map.copyOf(publicExtensions));
        }
        return Map.copyOf(result);
    }
}
