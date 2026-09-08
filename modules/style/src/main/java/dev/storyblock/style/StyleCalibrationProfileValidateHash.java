package dev.storyblock.style;

import static dev.storyblock.style.StyleCalibrationProfile.HASH;

final class StyleCalibrationProfileValidateHash {
    static void validateHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Style calibration " + field + " hash is invalid");
        }
    }
}
