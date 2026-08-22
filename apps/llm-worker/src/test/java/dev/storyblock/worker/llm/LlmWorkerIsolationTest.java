package dev.storyblock.worker.llm;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class LlmWorkerIsolationTest {
    @Test
    void workerHasNoCanonicalWriteDatabaseOrCredentialStoreDependency() {
        JavaClasses workerClasses = new ClassFileImporter().importPackages(
                "dev.storyblock.worker.llm"
        );
        noClasses()
                .that().resideInAPackage("dev.storyblock.worker.llm..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "dev.storyblock.application..",
                        "dev.storyblock.monitor..",
                        "dev.storyblock.security..",
                        "dev.storyblock.storage..",
                        "dev.storyblock.storage.sqlite..",
                        "java.sql..",
                        "javax.sql.."
                )
                .check(workerClasses);
    }
}
