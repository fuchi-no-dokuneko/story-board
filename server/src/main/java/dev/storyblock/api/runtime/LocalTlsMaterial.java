package dev.storyblock.api.runtime;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.*;

final class LocalTlsMaterial {
    private LocalTlsMaterial() {}

    static void ensure(Path directory, Path password, Path store) throws Exception {
        var permissions = PosixFilePermissions.fromString("rw-------");
        if (!Files.exists(password)) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            Files.writeString(password, HexFormat.of().formatHex(bytes), StandardOpenOption.CREATE_NEW);
        }
        Files.setPosixFilePermissions(password, permissions);
        if (!Files.exists(store)) {
            Path pending = directory.resolve("storyblock.pending.p12");
            Files.deleteIfExists(pending);
            try {
                keytool("-genkeypair", "-noprompt", "-alias", "storyblock",
                        "-keyalg", "RSA", "-keysize", "3072", "-sigalg", "SHA256withRSA",
                        "-validity", "825", "-dname", "CN=localhost,OU=Local,O=StoryBlock",
                        "-ext", "SAN=dns:localhost,dns:api,ip:127.0.0.1",
                        "-ext", "BC=ca:false", "-ext", "KU=digitalSignature,keyEncipherment",
                        "-ext", "EKU=serverAuth", "-storetype", "PKCS12",
                        "-keystore", pending.toString(), "-storepass:file", password.toString());
                Files.setPosixFilePermissions(pending, permissions);
                Files.move(pending, store, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(pending); }
        }
        Files.setPosixFilePermissions(store, permissions);
        Path certificate = LocalRuntime.directory("tls/public").resolve("storyblock.crt");
        keytool("-exportcert", "-rfc", "-alias", "storyblock", "-keystore", store.toString(),
                "-storepass:file", password.toString(), "-file", certificate.toString());
        Files.setPosixFilePermissions(certificate, permissions);
    }

    static void keytool(String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin/keytool").toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).inheritIO().start();
        if (process.waitFor() != 0) throw new IllegalStateException("Local certificate generation failed");
    }
}
