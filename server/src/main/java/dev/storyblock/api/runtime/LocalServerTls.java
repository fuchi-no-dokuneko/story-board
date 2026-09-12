package dev.storyblock.api.runtime;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import org.springframework.boot.web.server.Ssl;

final class LocalServerTls {
    private LocalServerTls() {}

    static Ssl prepare() throws Exception {
        Path directory = LocalRuntime.directory("tls/private");
        Path password = LocalRuntime.contained(directory.resolve("keystore.password"));
        Path store = LocalRuntime.contained(directory.resolve("storyblock.p12"));
        try (var channel = FileChannel.open(LocalRuntime.contained(directory.resolve("generate.lock")),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            LocalTlsMaterial.ensure(directory, password, store);
        }
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        ssl.setKeyStore(store.toUri().toString());
        ssl.setKeyStoreType("PKCS12");
        ssl.setKeyStorePassword(Files.readString(password).strip());
        ssl.setKeyAlias("storyblock");
        return ssl;
    }
}
