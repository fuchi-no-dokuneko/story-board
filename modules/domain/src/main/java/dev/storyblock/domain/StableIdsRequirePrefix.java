package dev.storyblock.domain;

import static dev.storyblock.domain.StableIds.PREFIX;

final class StableIdsRequirePrefix {
    static void requirePrefix(String prefix) {
        if (prefix == null || !PREFIX.matcher(prefix).matches()) {
            throw new IllegalArgumentException("Invalid identifier prefix");
        }
    }
}
