package dev.storyblock.rewrite.policy;

import static dev.storyblock.rewrite.policy.RewriteEligibility.HASH;

final class RewriteEligibilityRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Rewrite " + field + " hash is invalid");
        }
    }
}
