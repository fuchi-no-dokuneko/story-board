package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class StyleAnalysisServiceEncodeCursor {
    static String encodeCursor(
            Ids.StyleAnalysisId analysisId,
            int ordinal
    ) {
        String payload = analysisId.value() + ":" + ordinal;
        String signed = payload + ":" + CanonicalJson.hash(payload).substring(7, 23);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                signed.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
