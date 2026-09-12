package dev.storyblock.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ShortIds {
    public static final Set<String> PREFIXES = Set.of("nov", "ch", "scn", "blk", "blv", "rev");
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Set<String> USED = new HashSet<>();
    private static final Map<String, String> DERIVED = new HashMap<>();

    private ShortIds() {}

    public static synchronized String generate(String prefix) {
        String value;
        do {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 5; i++) suffix.append(ALPHABET.charAt(StableIds.RANDOM.nextInt(62)));
            value = prefix + "_" + suffix;
        } while (!USED.add(value));
        return value;
    }

    public static synchronized String derive(String prefix, String source, String discriminator) {
        String key = prefix + "\0" + source + "\0" + discriminator;
        return DERIVED.computeIfAbsent(key, ignored -> {
            for (int attempt = 0; ; attempt++) {
                byte[] bytes = IdentityDigest.of(key + "\0" + attempt);
                StringBuilder suffix = new StringBuilder();
                for (int i = 0; i < 5; i++) suffix.append(ALPHABET.charAt(Byte.toUnsignedInt(bytes[i]) % 62));
                String value = prefix + "_" + suffix;
                if (USED.add(value)) return value;
            }
        });
    }

    public static String require(String value, String prefix) {
        if (value == null || !value.matches(prefix + "_[A-Za-z0-9]{5}")) {
            throw new IllegalArgumentException("Expected " + prefix + "_ followed by five letters or digits");
        }
        return value;
    }
}
