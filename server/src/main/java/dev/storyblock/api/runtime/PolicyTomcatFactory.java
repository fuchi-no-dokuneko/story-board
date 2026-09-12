package dev.storyblock.api.runtime;

import java.net.InetAddress;
import java.util.List;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.env.Environment;

final class PolicyTomcatFactory extends TomcatServletWebServerFactory {
    private final Environment environment;

    PolicyTomcatFactory(Environment environment) { this.environment = environment; }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        try {
            int port = environment.getProperty("port", Integer.class, 8443);
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1..65535");
            List<InetAddress> addresses = Ipv4Listeners.resolve(environment.getProperty("policy", "public"));
            setPort(port);
            setSsl(LocalServerTls.prepare());
            setBaseDirectory(LocalRuntime.directory("tmp/tomcat").toFile());
            for (InetAddress address : addresses.subList(1, addresses.size())) {
                setAddress(address);
                Connector connector = new Connector(getProtocol());
                customizeConnector(connector);
                addAdditionalConnectors(connector);
            }
            setAddress(addresses.getFirst());
            return super.getWebServer(initializers);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not prepare local IPv4 HTTPS listeners", failure);
        }
    }
}
