package dev.storyblock.contracts;

import static dev.storyblock.contracts.CanonicalNovelPackage.SHA_256;

final class CanonicalNovelPackageRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new CanonicalPackageException(field + " must be lowercase SHA-256");
        }
    }
}
