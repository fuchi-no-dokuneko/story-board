package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalPackageException;

final class CanonicalTransferControllerUnquoteEtag {
    static String unquoteEtag(String value) {
        if (value == null || value.length() < 2
                || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            throw new CanonicalPackageException("If-Match is not a quoted strong ETag");
        }
        return value.substring(1, value.length() - 1);
    }
}
