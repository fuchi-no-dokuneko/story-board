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

    public static String generate(String prefix) {
        String reserved = ShortIdScope.allocate(prefix, prefix + "\0random\0" + java.util.UUID.randomUUID());
        return reserved == null ? generateLocal(prefix) : reserved;
    }

    private static synchronized String generateLocal(String prefix) {
        String value;
        do {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 5; i++) suffix.append(ALPHABET.charAt(StableIds.RANDOM.nextInt(62)));
            value = prefix + "_" + suffix;
        } while (!USED.add(value));
        return value;
    }

    public static String derive(String prefix, String source, String discriminator) {
        String key = prefix + "\0" + source + "\0" + discriminator;
        String reserved = ShortIdScope.allocate(prefix, key);
        return reserved == null ? deriveLocal(prefix, key) : reserved;
    }

    private static synchronized String deriveLocal(String prefix, String key) {
        return DERIVED.computeIfAbsent(key, ignored -> {
            for (int attempt = 0; ; attempt++) {
                String value = candidate(prefix, key, attempt);
                if (USED.add(value)) return value;
            }
        });
    }

    public static String candidate(String prefix, String key, int attempt) {
        if (!PREFIXES.contains(prefix)) throw new IllegalArgumentException("Unsupported short ID prefix");
        byte[] bytes = IdentityDigest.of(key + "\0" + attempt);
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 5; i++) suffix.append(ALPHABET.charAt(Byte.toUnsignedInt(bytes[i]) % 62));
        return prefix + "_" + suffix;
    }

    public static String require(String value, String prefix) {
        if (value == null || !value.matches(prefix + "_[A-Za-z0-9]{5}")) {
            throw new IllegalArgumentException("Expected " + prefix + "_ followed by five letters or digits");
        }
        return value;
    }
}
