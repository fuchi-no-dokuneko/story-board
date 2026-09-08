package dev.storyblock.contracts;

import java.math.BigDecimal;

final class CanonicalNovelPackageExactLong {
    static long exactLong(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new CanonicalPackageException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new CanonicalPackageException(path + " must be an exact integer", failure);
        }
    }
}
