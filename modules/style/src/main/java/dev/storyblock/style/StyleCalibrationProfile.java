package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleCalibrationProfile(
        String calibrationSchemaVersion,
        String targetCorpusHash,
        String contractHash,
        String windowConfigurationHash,
        List<StyleStratumCalibration> strata
) {
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Set<String> FIELDS = Set.of(
            "calibration_schema_version", "target_corpus_hash", "contract_hash",
            "window_configuration_hash", "strata"
    );

    public StyleCalibrationProfile {
        if (!StyleModule.CALIBRATION_SCHEMA_VERSION.equals(calibrationSchemaVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported style calibration schema version"
            );
        }
        StyleCalibrationProfileValidateHash.validateHash(targetCorpusHash, "target corpus");
        StyleCalibrationProfileValidateHash.validateHash(contractHash, "contract");
        StyleCalibrationProfileValidateHash.validateHash(windowConfigurationHash, "window configuration");
        strata = List.copyOf(strata).stream()
                .sorted(Comparator.comparing(value -> value.stratum().canonicalKey()))
                .toList();
        if (strata.isEmpty()) {
            throw new IllegalArgumentException(
                    "Style calibration profile requires at least one stratum"
            );
        }
        if (strata.stream().map(StyleStratumCalibration::stratum)
                .distinct().count() != strata.size()) {
            throw new IllegalArgumentException(
                    "Style calibration profile strata must be unique"
            );
        }
    }

    public static StyleCalibrationProfile fromCanonical(Map<String, Object> value) {
        return StyleCalibrationProfileFromCanonicalFactory.fromCanonical(value);
    }

    public Optional<StyleStratumCalibration> find(StyleStratum stratum) {
        Objects.requireNonNull(stratum, "stratum");
        return strata.stream().filter(candidate -> candidate.stratum().equals(stratum))
                .findFirst();
    }

    public boolean hasCalibratedStratum() {
        return strata.stream().anyMatch(stratum ->
                stratum.confidence() == StyleCalibrationConfidence.CALIBRATED
        );
    }

    public String calibrationHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return StyleCalibrationProfileCanonicalValueAction.canonicalValue(this);
    }

}
