package dev.storyblock.contracts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CanonicalRevisionFreezeValue {
    static Object freezeValue(Object value, String path) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof BigInteger || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.signum() == 0 ? BigDecimal.ZERO : decimal.stripTrailingZeros();
        }
        if (value instanceof Float || value instanceof Double) {
            if (!Double.isFinite(((Number) value).doubleValue())) {
                throw new IllegalArgumentException(path + " contains a non-finite number");
            }
            return BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros();
        }
        if (value instanceof Map<?, ?> map) {
            return CanonicalRevisionFreezeMap.freezeMap(map, path);
        }
        if (value instanceof List<?> list) {
            List<Object> frozen = new ArrayList<>(list.size());
            for (int index = 0; index < list.size(); index++) {
                frozen.add(freezeValue(list.get(index), path + "[" + index + "]"));
            }
            return List.copyOf(frozen);
        }
        throw new IllegalArgumentException(path + " contains unsupported canonical value "
                + value.getClass().getSimpleName());
    }
}
