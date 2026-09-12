package dev.storyblock.api.style;

import jakarta.annotation.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class StyleLibrary {
    private final Path registry;
    private final ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "style-reference-watcher"); thread.setDaemon(true); return thread;
    });
    private volatile StyleLibraryState state = StyleLibraryState.empty();

    public StyleLibrary(@Value("${storyblock.styles.registry:server/config/content-config/styles.yaml}") String path) {
        Path root = Path.of(System.getProperty("storyblock.root", ".")).toAbsolutePath().normalize();
        registry = root.resolve(path).normalize();
        if (!registry.startsWith(root)) throw new IllegalArgumentException("Style YAML must stay inside the repository");
    }

    @PostConstruct void start() { watcher.scheduleWithFixedDelay(this::refresh, 0, 60, TimeUnit.SECONDS); }
    @PreDestroy void stop() { watcher.shutdownNow(); }

    synchronized void refresh() {
        var previous = state;
        try {
            var definitions = StyleRegistryFile.read(registry);
            var benchmarks = new LinkedHashMap<String, StyleBenchmark>();
            var errors = new LinkedHashMap<String, String>();
            for (var definition : definitions) {
                var old = previous.benchmarks().get(definition.id());
                try {
                    var files = StyleSourceFiles.inspect(definition);
                    benchmarks.put(definition.id(), old != null && old.definition().equals(definition)
                            && old.sources().equals(files) ? old : StyleBenchmark.calculate(definition, files));
                } catch (Exception failure) {
                    if (old != null) benchmarks.put(definition.id(), old);
                    errors.put(definition.id(), Objects.toString(failure.getMessage(), "Reference calculation failed"));
                }
            }
            state = new StyleLibraryState(definitions, Map.copyOf(benchmarks), Map.copyOf(errors), null);
        } catch (Exception failure) {
            state = new StyleLibraryState(previous.definitions(), previous.benchmarks(), previous.errors(), failure.getMessage());
        }
    }

    public Map<String, Object> list() { return state.publicValue(); }
    StyleLibraryState snapshot() { return state; }
}
