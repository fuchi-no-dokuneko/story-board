package dev.storyblock.api.runtime;

import java.net.*;
import java.util.*;

public final class Ipv4Listeners {
    private Ipv4Listeners() {}

    static List<InetAddress> resolve(String policy) throws SocketException {
        return switch (policy) {
            case "local" -> List.of(address("127.0.0.1"));
            case "public" -> List.of(address("0.0.0.0"));
            case "standard" -> tunnelAddresses();
            default -> throw new IllegalArgumentException("Unknown port policy: " + policy);
        };
    }

    public static InetAddress address(String text) {
        String[] parts = text.split("\\.", -1);
        if (parts.length != 4) throw new IllegalArgumentException("Expected IPv4: " + text);
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}")) {
                throw new IllegalArgumentException("Invalid IPv4: " + text);
            }
            int value = Integer.parseInt(parts[i]);
            if (value > 255) throw new IllegalArgumentException("Invalid IPv4: " + text);
            bytes[i] = (byte) value;
        }
        try { return InetAddress.getByAddress(bytes); }
        catch (UnknownHostException impossible) { throw new IllegalStateException(impossible); }
    }

    static List<InetAddress> tunnelAddresses() throws SocketException {
        List<InetAddress> addresses = new ArrayList<>();
        for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            String name = network.getName();
            if (!network.isUp() || !(name.startsWith("wg") || name.startsWith("tails"))) continue;
            Collections.list(network.getInetAddresses()).stream()
                    .filter(Inet4Address.class::isInstance).forEach(addresses::add);
        }
        if (addresses.isEmpty()) throw new IllegalStateException("No active wg* or tails* IPv4 interface");
        return addresses.stream().distinct().sorted(Comparator.comparing(InetAddress::getHostAddress)).toList();
    }
}
