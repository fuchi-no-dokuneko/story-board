package dev.storyblock.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class CanonicalValues {
    private CanonicalValues() {
    }

    public static Map<String, Object> freezeMap(Map<String, ?> input, String path) {
        Objects.requireNonNull(input, path);
        Map<String, Object> frozen = new TreeMap<>();
        for (Map.Entry<String, ?> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException(path + " contains a null key");
            }
            frozen.put(entry.getKey(), freeze(entry.getValue(), path + "." + entry.getKey()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    public static Object freeze(Object value, String path) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof BigInteger || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.signum() == 0 ? BigDecimal.ZERO : decimal.stripTrailingZeros();
        }
        if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException(path + " contains a non-finite number");
            }
            return BigDecimal.valueOf(number).stripTrailingZeros();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(path + " contains a non-string key");
                }
                typed.put(key, freeze(entry.getValue(), path + "." + key));
            }
            return Collections.unmodifiableMap(typed);
        }
        if (value instanceof List<?> list) {
            List<Object> frozen = new ArrayList<>(list.size());
            for (int index = 0; index < list.size(); index++) {
                frozen.add(freeze(list.get(index), path + "[" + index + "]"));
            }
            return List.copyOf(frozen);
        }
        throw new IllegalArgumentException(path + " contains unsupported canonical value "
                + value.getClass().getSimpleName());
    }
}
