package dev.storyblock.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record VersionedBlock(
        Ids.BlockId blockId,
        Ids.BlockVersionId blockVersionId,
        OrderKey orderKey,
        String text,
        String canonicalMetadataHash
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public VersionedBlock {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockVersionId, "blockVersionId");
        Objects.requireNonNull(orderKey, "orderKey");
        UnicodeText.validateBlock(text);
        if (canonicalMetadataHash == null || !SHA_256.matcher(canonicalMetadataHash).matches()) {
            throw new IllegalArgumentException("Canonical metadata hash must be lowercase SHA-256");
        }
    }

    public static VersionedBlock create(
            OrderKey orderKey,
            String text,
            String canonicalMetadataHash
    ) {
        return new VersionedBlock(
                Ids.BlockId.create(),
                Ids.BlockVersionId.create(),
                orderKey,
                text,
                canonicalMetadataHash
        );
    }

    public VersionedBlock moveTo(OrderKey newOrderKey) {
        return new VersionedBlock(
                blockId,
                blockVersionId,
                newOrderKey,
                text,
                canonicalMetadataHash
        );
    }

    public VersionedBlock revise(String newText, String newCanonicalMetadataHash) {
        if (text.equals(newText) && canonicalMetadataHash.equals(newCanonicalMetadataHash)) {
            return this;
        }
        return new VersionedBlock(
                blockId,
                Ids.BlockVersionId.create(),
                orderKey,
                newText,
                newCanonicalMetadataHash
        );
    }
}
