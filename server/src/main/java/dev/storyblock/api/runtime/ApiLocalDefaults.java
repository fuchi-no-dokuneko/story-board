package dev.storyblock.api.runtime;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.*;

public final class ApiLocalDefaults {
    private ApiLocalDefaults() {}

    public static Map<String, Object> prepare() {
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("user.home", LocalRuntime.directory("").toString());
            System.setProperty("java.io.tmpdir", LocalRuntime.directory("tmp").toString());
            Path directory = LocalRuntime.directory("secrets");
            Path pepper = LocalRuntime.contained(directory.resolve("server-pepper"));
            try (var channel = FileChannel.open(LocalRuntime.contained(directory.resolve("generate.lock")),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var lock = channel.lock()) {
                if (!Files.exists(pepper)) {
                    byte[] bytes = new byte[48];
                    new SecureRandom().nextBytes(bytes);
                    Files.writeString(pepper, HexFormat.of().formatHex(bytes), StandardOpenOption.CREATE_NEW);
                }
                Files.setPosixFilePermissions(pepper, PosixFilePermissions.fromString("rw-------"));
            }
            return Map.of(
                    "storyblock.trusted-lan.enabled", true,
                    "storyblock.security.pepper", Files.readString(pepper).strip(),
                    "storyblock.database.path", LocalRuntime.directory("").resolve("storyblock.db").toString());
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot initialize repository-local defaults", failure);
        }
    }
}
