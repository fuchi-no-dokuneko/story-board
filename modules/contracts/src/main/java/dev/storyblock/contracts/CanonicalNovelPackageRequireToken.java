package dev.storyblock.contracts;

import static dev.storyblock.contracts.CanonicalPackageFields.TOKEN;

final class CanonicalNovelPackageRequireToken {
    static String requireToken(String value, String field) {
        if (value == null || !TOKEN.matcher(value).matches()) {
            throw new CanonicalPackageException(field + " is not a canonical token");
        }
        return value;
    }
}
