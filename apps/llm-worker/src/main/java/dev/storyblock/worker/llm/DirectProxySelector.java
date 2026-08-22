package dev.storyblock.worker.llm;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;

final class DirectProxySelector extends ProxySelector {
    static final DirectProxySelector INSTANCE = new DirectProxySelector();

    private DirectProxySelector() {
    }

    @Override
    public List<Proxy> select(URI uri) {
        Objects.requireNonNull(uri, "uri");
        return List.of(Proxy.NO_PROXY);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException failure) {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(failure, "failure");
    }
}
