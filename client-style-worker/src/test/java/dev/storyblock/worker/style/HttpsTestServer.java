package dev.storyblock.worker.style;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import javax.net.ssl.*;

final class HttpsTestServer {
    static HttpsServer create() throws IOException {
        try {
            Path directory = Path.of("target/transport-tls").toAbsolutePath();
            Files.createDirectories(directory);
            Path store = directory.resolve("server.p12");
            if (!Files.exists(store)) {
                Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin/keytool").toString(),
                        "-genkeypair", "-noprompt", "-alias", "test", "-keyalg", "RSA",
                        "-keysize", "2048", "-dname", "CN=localhost", "-validity", "30",
                        "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-ext", "BC=ca:false",
                        "-ext", "EKU=serverAuth", "-storetype", "PKCS12",
                        "-keystore", store.toString(), "-storepass", "test-only-password")
                        .redirectErrorStream(true).redirectOutput(directory.resolve("keytool.log").toFile()).start();
                if (process.waitFor() != 0) throw new IOException("Test certificate generation failed");
            }
            KeyStore keys = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(store)) { keys.load(input, "test-only-password".toCharArray()); }
            KeyManagerFactory manager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            manager.init(keys, "test-only-password".toCharArray());
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(manager.getKeyManagers(), null, null);
            HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(context));
            return server;
        } catch (Exception failure) { throw new IOException("Cannot prepare test HTTPS", failure); }
    }
}
