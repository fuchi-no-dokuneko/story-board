package dev.storyblock.architecture;

import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class NarrativeViewBoundariesTest {
    @Test void proseConsumersUseTheSharedProjection() {
        var root = Path.of("../..").toAbsolutePath().normalize();
        var paths = List.of("modules/renderer", "modules/style", "modules/detector", "modules/monitor-adapter",
                "modules/rewrite", "modules/rewrite-policy", "modules/application", "server", "client-cli")
                .stream().map(name -> root.resolve(name).resolve("target/classes")).toList();
        var classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPaths(paths);
        classes.get("dev.storyblock.cli.HtmlBook");
        classes.get("dev.storyblock.api.quality.NovelQualityService");
        noClasses().that().resideInAnyPackage(
                "dev.storyblock.renderer..", "dev.storyblock.style..", "dev.storyblock.detector..",
                "dev.storyblock.monitor..", "dev.storyblock.rewrite..", "dev.storyblock.api.quality..",
                "dev.storyblock.api.style..")
                .or().haveSimpleName("HtmlBook")
                .or().haveNameMatching("dev[.]storyblock[.]application[.](Rewrite.*|StyleAnalysisService.*|NovelCatalogService.*|MonitorService.*)")
                .should().callMethod(NarrativeScene.class,"blocks")
                .orShould().callMethod(RevisionManifest.class,"liveBlocks")
                .check(classes);
    }
}
