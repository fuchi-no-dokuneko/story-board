package dev.storyblock.contracts;



final class CanonicalNovelPackageExactInt {
    static int exactInt(Object value, String path) {
        try {
            return Math.toIntExact(CanonicalNovelPackageExactLong.exactLong(value, path));
        } catch (ArithmeticException failure) {
            throw new CanonicalPackageException(path + " is outside the integer range", failure);
        }
    }
}
