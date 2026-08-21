package dev.storyblock.domain;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;

public record OrderKey(String value) implements Comparable<OrderKey> {
    private static final int WIDTH = 32;
    private static final BigInteger MIN = BigInteger.ZERO;
    private static final BigInteger MAX = BigInteger.ONE.shiftLeft(WIDTH * 4).subtract(BigInteger.ONE);

    public OrderKey {
        Objects.requireNonNull(value, "value");
        value = value.toLowerCase(Locale.ROOT);
        if (!value.matches("[0-9a-f]{" + WIDTH + "}")) {
            throw new IllegalArgumentException("Order key must be 32 lowercase hexadecimal digits");
        }
    }

    public static OrderKey initial() {
        return fromNumber(MAX.shiftRight(1));
    }

    public static OrderKey between(OrderKey left, OrderKey right) {
        BigInteger low = left == null ? MIN : left.number();
        BigInteger high = right == null ? MAX : right.number();
        if (low.compareTo(high) >= 0) {
            throw new IllegalArgumentException("Left order key must precede right order key");
        }
        if (high.subtract(low).compareTo(BigInteger.ONE) <= 0) {
            throw new IllegalStateException("No midpoint remains; deterministic rebalance required");
        }
        return fromNumber(low.add(high).shiftRight(1));
    }

    public static OrderKey rebalanced(int index, int total) {
        if (total < 1 || index < 0 || index >= total) {
            throw new IllegalArgumentException("Invalid rebalance position");
        }
        BigInteger step = MAX.divide(BigInteger.valueOf((long) total + 1));
        return fromNumber(step.multiply(BigInteger.valueOf((long) index + 1)));
    }

    @Override
    public int compareTo(OrderKey other) {
        return value.compareTo(other.value);
    }

    private BigInteger number() {
        return new BigInteger(value, 16);
    }

    private static OrderKey fromNumber(BigInteger number) {
        String encoded = number.toString(16);
        return new OrderKey("0".repeat(WIDTH - encoded.length()) + encoded);
    }
}
