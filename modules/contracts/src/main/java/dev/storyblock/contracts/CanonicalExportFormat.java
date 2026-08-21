package dev.storyblock.contracts;

import java.util.Arrays;

public enum CanonicalExportFormat {
    REVISION("canonical-revision"),
    PACKAGE("canonical-package");

    private final String canonicalName;

    CanonicalExportFormat(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static CanonicalExportFormat fromCanonicalName(String value) {
        return Arrays.stream(values())
                .filter(format -> format.canonicalName.equals(value))
                .findFirst()
                .orElseThrow(() -> new CanonicalPackageException(
                        "Unsupported canonical transfer format " + value
                ));
    }
}
