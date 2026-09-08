package dev.storyblock.api.http;

import java.util.Map;
import java.math.BigDecimal;

final class StrictJsonRequestIntegerFactory {
    static int integer(Map<String, Object> value, String field, String path)  {
        Object entry = value.get(field);
        if (!(entry instanceof Number number)) {
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
