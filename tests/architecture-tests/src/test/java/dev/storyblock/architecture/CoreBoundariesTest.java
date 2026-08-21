package dev.storyblock.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class CoreBoundariesTest {
    private final JavaClasses domainClasses = new ClassFileImporter()
            .importPackages("dev.storyblock.domain");

    @Test
    void domainHasNoFrameworkOrTransportDependencies() {
        noClasses()
                .that().resideInAPackage("dev.storyblock.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "tools.jackson..",
                        "com.fasterxml.jackson..",
                        "jakarta.persistence..",
                        "jakarta.servlet.."
                )
                .check(domainClasses);
    }
}
