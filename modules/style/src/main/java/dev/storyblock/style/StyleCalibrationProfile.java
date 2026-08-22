package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "calibration_schema_version", "target_corpus_hash", "contract_hash",
            "window_configuration_hash", "strata"
    );

    public StyleCalibrationProfile {
        if (!StyleModule.CALIBRATION_SCHEMA_VERSION.equals(calibrationSchemaVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported style calibration schema version"
            );
        }
        validateHash(targetCorpusHash, "target corpus");
        validateHash(contractHash, "contract");
        validateHash(windowConfigurationHash, "window configuration");
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
        StyleCanonical.requireKeys(value, FIELDS, "style_calibration_profile");
        return new StyleCalibrationProfile(
                StyleCanonical.string(
                        value, "calibration_schema_version", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "target_corpus_hash", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "contract_hash", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "window_configuration_hash", "style_calibration_profile"
                ),
                StyleCanonical.objects(
                        value.get("strata"), "style_calibration_profile.strata"
                ).stream().map(StyleStratumCalibration::fromCanonical).toList()
        );
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
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("calibration_schema_version", calibrationSchemaVersion);
        value.put("contract_hash", contractHash);
        value.put("strata", strata.stream()
                .map(StyleStratumCalibration::canonicalValue).toList());
        value.put("target_corpus_hash", targetCorpusHash);
        value.put("window_configuration_hash", windowConfigurationHash);
        return CanonicalValues.freezeMap(value, "style_calibration_profile");
    }

    private static void validateHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Style calibration " + field + " hash is invalid");
        }
    }
}
