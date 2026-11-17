import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.docs.Documenter;
import io.quarkiverse.modulith.docs.DocumenterOptions;
import io.quarkiverse.modulith.it.springcompat.SpringCompatApplication;

class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(SpringCompatApplication.class);
    private static final Path DOCS = Path.of(DocumenterOptions.DEFAULT_LOCATION);

    @Test
    void verify_always_modulesConformToStructure() {
        // act
        ApplicationModules verified = MODULES.verify();

        // assert
        assertSame(MODULES, verified);
    }

    @Test
    void writeDocumentation_always_createsModuleDocs() {
        // act
        new Documenter(MODULES).writeDocumentation();

        // assert
        assertTrue(Files.isRegularFile(DOCS.resolve("components.puml")));
        assertTrue(Files.isRegularFile(DOCS.resolve("module-billing.puml")));
        assertTrue(Files.isRegularFile(DOCS.resolve("module-billing.adoc")));
        assertTrue(Files.isRegularFile(DOCS.resolve("all-docs.adoc")));
    }

    @Test
    void writeDocumentation_always_writesPlantUmlAndCanvases() {
        // act
        new Documenter(MODULES).writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();

        // assert
        assertTrue(Files.isRegularFile(DOCS.resolve("components.puml")));
        assertTrue(Files.isRegularFile(DOCS.resolve("module-mail.puml")));
        assertTrue(Files.isRegularFile(DOCS.resolve("module-mail.adoc")));
    }

    @Test
    void detectViolations_always_returnsNoViolations() {
        // act
        var violations = MODULES.detectViolations();

        // assert
        assertFalse(violations.hasViolations());
    }
}
