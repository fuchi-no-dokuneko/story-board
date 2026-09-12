package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class StyleAnalysisServiceDecodeCursor {
    static int decodeCursor(
            Ids.StyleAnalysisId analysisId,
            String cursor
    ) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII
            );
            int checksumSeparator = decoded.lastIndexOf(':');
            String payload = decoded.substring(0, checksumSeparator);
            String checksum = decoded.substring(checksumSeparator + 1);
            String prefix = analysisId.value() + ":";
            if (!payload.startsWith(prefix)
                    || !checksum.equals(CanonicalJson.hash(payload).substring(7, 23))) {
                throw new IllegalArgumentException("Cursor does not match style analysis");
            }
            int ordinal = Integer.parseInt(payload.substring(prefix.length()));
            if (ordinal < 0) {
                throw new IllegalArgumentException("Cursor ordinal is invalid");
            }
            return ordinal;
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new IllegalArgumentException("Style analysis cursor is invalid", failure);
        }
    }
}
