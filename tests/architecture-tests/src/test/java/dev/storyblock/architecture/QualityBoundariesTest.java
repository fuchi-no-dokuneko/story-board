package dev.storyblock.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class QualityBoundariesTest {
    @Test void qualityMetricsHaveNoTransportPersistenceOrFileDependencies() {
        var classes = new ClassFileImporter().importPackages("dev.storyblock.style.quality");
        noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..","java.sql..",
                "java.net..","java.io..","java.nio.file..","dev.storyblock.storage..","dev.storyblock.application..")
                .check(classes);
    }
    @Test void tokenizerDoesNotDependOnStyleOrApplications() {
        var classes = new ClassFileImporter().importPackages("dev.storyblock.nlp.zh");
        noClasses().should().dependOnClassesThat().resideInAnyPackage("dev.storyblock.style..","dev.storyblock.api..",
                "dev.storyblock.worker..","dev.storyblock.storage..","org.springframework..","tools.jackson..")
                .check(classes);
    }
}
