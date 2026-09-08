package dev.storyblock.contracts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

final class CanonicalRevisionParseEnvelopeFactory {
    static CanonicalRevision parseEnvelope(byte[] json)  {
        Objects.requireNonNull(json, "json");
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = CanonicalJson.mapper().readValue(json, Map.class);
        envelope = new TreeMap<>(envelope);
        Object declaredHash = envelope.remove("content_hash");
        if (!(declaredHash instanceof String hash) || !CanonicalRevisionFields.SHA_256.matcher(hash).matches()) {
            throw new IllegalArgumentException("Canonical envelope has no valid content_hash");
        }

        CanonicalRevision revision = CanonicalRevision.of(envelope);
        if (!MessageDigest.isEqual(
                hash.getBytes(StandardCharsets.US_ASCII),
                revision.contentHash().getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new IllegalArgumentException("Canonical envelope content_hash does not match content");
        }
        return revision;
    }
}
