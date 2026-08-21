package dev.storyblock.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class BlockSequenceHash {
    private BlockSequenceHash() {
    }

    public static String ofBlocks(List<NarrativeBlock> blocks) {
        return ofReferences(blocks.stream().map(BlockVersionRef::from).toList());
    }

    public static String ofReferences(List<BlockVersionRef> references) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("storyblock:block-sequence:v1\0".getBytes(StandardCharsets.US_ASCII));
            for (BlockVersionRef reference : references) {
                updateLengthPrefixed(digest, reference.blockId().value());
                updateLengthPrefixed(digest, reference.blockVersionId().value());
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
    }

    private static void updateLengthPrefixed(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
