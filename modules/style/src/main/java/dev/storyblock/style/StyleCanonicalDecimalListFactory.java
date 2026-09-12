package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class StyleCanonicalDecimalListFactory {
    static List<BigDecimal> decimalList(Object value, String path)  {
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<BigDecimal> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            Object entry = raw.get(index);
            if (!(entry instanceof Number number)) {
                throw new IllegalArgumentException(path + "[" + index + "] must be numeric");
            }
            result.add(new BigDecimal(number.toString()));
        }
        return List.copyOf(result);
    }
}
