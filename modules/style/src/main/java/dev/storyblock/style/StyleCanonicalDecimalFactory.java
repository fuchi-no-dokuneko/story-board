package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Map;

final class StyleCanonicalDecimalFactory {
    static BigDecimal decimal(Map<String, Object> value, String field, String path)  {
        Object raw = value.get(field);
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException(path + "." + field + " must be numeric");
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(path + "." + field + " is invalid", failure);
        }
    }
}
