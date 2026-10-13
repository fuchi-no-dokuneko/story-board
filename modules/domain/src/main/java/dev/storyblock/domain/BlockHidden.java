package dev.storyblock.domain;

import java.util.Map;

public final class BlockHidden {
    public static final String EXTENSION_KEY = "storyblock.hidden";

    private BlockHidden() { }

    public static boolean isHidden(Map<String, Object> extensions) {
        if (!extensions.containsKey(EXTENSION_KEY)) return false;
        if (!Boolean.TRUE.equals(extensions.get(EXTENSION_KEY))) {
            throw unsupported("storyblock.hidden must be exactly true");
        }
        return true;
    }

    public static void validate(Map<String, Object> extensions, BlockMetadata metadata) {
        if (!isHidden(extensions)) return;
        if (extensions.containsKey(BlockImage.EXTENSION_KEY)) {
            throw unsupported("A hidden block cannot carry an image");
        }
        validateMetadata(metadata);
    }

    public static void validateMetadata(BlockMetadata metadata) {
        if (metadata.fields().keySet().stream().anyMatch(key -> !key.equals("provenance"))) {
            throw new HiddenBlockException(HiddenBlockException.Code.HIDDEN_BLOCK_META_NOT_ALLOWED,
                    "Hidden block metadata allows only provenance");
        }
    }

    public static HiddenBlockException unsupported(String message) {
        return new HiddenBlockException(HiddenBlockException.Code.HIDDEN_BLOCK_UNSUPPORTED, message);
    }
}
