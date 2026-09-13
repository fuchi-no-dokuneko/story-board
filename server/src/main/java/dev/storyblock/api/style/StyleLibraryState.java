package dev.storyblock.api.style;

import java.util.*;

record StyleLibraryState(List<StyleDefinition> definitions, Map<String, StyleBenchmark> benchmarks,
        Map<String, String> errors, String registryError) {
    static StyleLibraryState empty() { return new StyleLibraryState(List.of(), Map.of(), Map.of(), null); }
    Map<String, Object> publicValue() {
        var styles = new ArrayList<Map<String, Object>>();
        for (var definition : definitions) {
            var benchmark = benchmarks.get(definition.id());
            if (benchmark != null) styles.add(benchmark.publicValue(errors.get(definition.id())));
            else {
                var value = new LinkedHashMap<>(definition.publicValue());
                value.put("status", "unavailable");
                value.put("error", errors.getOrDefault(definition.id(), "Benchmark is being calculated"));
                styles.add(value);
            }
        }
        var value = new LinkedHashMap<String, Object>();
        value.put("styles", styles);
        if (registryError != null) value.put("error", registryError);
        return value;
    }
}
