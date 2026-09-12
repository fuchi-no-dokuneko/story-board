package dev.storyblock.api.http;

import java.math.BigDecimal;

final class MonitorControllerExactInt {
    static int exactInt(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(path + " must be an exact integer", failure);
        }
    }
}
