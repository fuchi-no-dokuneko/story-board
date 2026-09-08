package dev.storyblock.worker.style;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/** The reachable local service is trusted; TLS still encrypts every connection. */
final class LocalSelfSignedTls {
    static SSLContext context() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {new X509ExtendedTrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] chain, String auth) {}
                public void checkServerTrusted(X509Certificate[] chain, String auth) {}
                public void checkClientTrusted(X509Certificate[] chain, String auth, java.net.Socket socket) {}
                public void checkServerTrusted(X509Certificate[] chain, String auth, java.net.Socket socket) {}
                public void checkClientTrusted(X509Certificate[] chain, String auth, SSLEngine engine) {}
                public void checkServerTrusted(X509Certificate[] chain, String auth, SSLEngine engine) {}
            }}, new SecureRandom());
            return context;
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("Cannot initialize local TLS", failure);
        }
    }
}
