package dev.storyblock.contracts;

import java.math.BigDecimal;

final class EditOperationCanonicalMapperExactInt {
    static int exactInt(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(path + " must be an exact 32-bit integer", exception);
        }
    }
}
