package dev.storyblock.domain;

import java.math.BigDecimal;
import java.util.Map;
import static dev.storyblock.domain.BlockImage.EXTENSION_KEY;

final class BlockImageInteger {
    static int integer(Map<?, ?> map, String field) {
        Object value = map.get(field);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(EXTENSION_KEY + "." + field + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(
                    EXTENSION_KEY + "." + field + " must be an exact integer",
                    failure
            );
        }
    }
}
