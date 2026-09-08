package dev.storyblock.rewrite;

import static dev.storyblock.rewrite.RewriteWorkerInput.HASH;

final class RewriteWorkerInputRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Rewrite " + field + " hash is invalid");
        }
    }
}
