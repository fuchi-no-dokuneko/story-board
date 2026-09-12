package dev.storyblock.rewrite.policy;

import java.math.BigDecimal;
import java.util.Map;

final class RewritePolicyCanonicalIntegerFactory {
    static int integer(Map<String, Object> value, String field, String path)  {
        Object raw = value.get(field);
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException(path + "." + field + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(
                    path + "." + field + " must be an exact integer", failure
            );
        }
    }
}
