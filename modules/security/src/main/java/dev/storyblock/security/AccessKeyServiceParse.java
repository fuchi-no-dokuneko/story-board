package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static dev.storyblock.security.AccessKeyService.DECODER;
import static dev.storyblock.security.AccessKeyService.ENCODER;
import static dev.storyblock.security.AccessKeyService.ParsedCredential;
import static dev.storyblock.security.AccessKeyService.SECRET_BYTES;
import static dev.storyblock.security.AccessKeyService.TOKEN_PREFIX;

final class AccessKeyServiceParse {
    static ParsedCredential parse(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        int separator = token.indexOf('.', TOKEN_PREFIX.length());
        if (separator < 0 || token.indexOf('.', separator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        Ids.AccessKeyId keyId = new Ids.AccessKeyId(
                token.substring(TOKEN_PREFIX.length(), separator)
        );
        String encoded = token.substring(separator + 1);
        byte[] secret = DECODER.decode(encoded.getBytes(StandardCharsets.US_ASCII));
        if (secret.length != SECRET_BYTES || !ENCODER.encodeToString(secret).equals(encoded)) {
            Arrays.fill(secret, (byte) 0);
            throw new IllegalArgumentException("Invalid bearer token");
        }
        return new ParsedCredential(keyId, secret);
    }
}
