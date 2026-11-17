package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.test.execution.ExecutionApplication;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

class ModuleTestExecutionTest {

    private static final String FULL_SETUP = "module=order\nbootstrap-mode=DIRECT_DEPENDENCIES\n"
            + "dependencies=catalog\nextra-includes=billing\nshared-modules=shared\n";

    private final ApplicationModules modules = TestApplicationModules.of(ExecutionApplication.class);

    @Test
    void readsTheModuleUnderTestAndTheBootstrapMode() {
        // arrange
        InputStream resource = resource(FULL_SETUP);

        // act
        ModuleTestExecution execution = ModuleTestExecution.of(resource, modules).orElseThrow();

        // assert
        assertThat(execution.getModule().getName()).isEqualTo("order");
        assertThat(execution.getBootstrapMode()).isEqualTo(BootstrapMode.DIRECT_DEPENDENCIES);
    }

    @Test
    void countsTheExtraModulesAsDependencies() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        List<ApplicationModule> dependencies = execution.getDependencies();

        // assert
        assertThat(dependencies).extracting(ApplicationModule::getName).containsExactly("catalog", "billing");
    }

    @Test
    void readsTheExtraModules() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        List<ApplicationModule> extraIncludes = execution.getExtraIncludes();

        // assert
        assertThat(extraIncludes).extracting(ApplicationModule::getName).containsExactly("billing");
    }

    @Test
    void readsTheSharedModules() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        List<ApplicationModule> sharedModules = execution.getSharedModules();

        // assert
        assertThat(sharedModules).extracting(ApplicationModule::getName).containsExactly("shared");
    }

    @Test
    void keepsAllModulesOfTheApplication() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        ApplicationModules all = execution.getModules();

        // assert
        assertThat(all).isSameAs(modules);
    }

    @Test
    void listsTheBasePackagesOfTheStartedModules() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        List<String> basePackages = execution.getBasePackages();

        // assert
        assertThat(basePackages).containsExactly("io.quarkiverse.modulith.test.execution.order",
                "io.quarkiverse.modulith.test.execution.catalog", "io.quarkiverse.modulith.test.execution.billing",
                "io.quarkiverse.modulith.test.execution.shared");
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.execution.catalog.Catalog, true", "com.other.Type, true" })
    void includesTheClassesOfStartedModulesAndOfNoModule(String className, boolean included) {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        boolean result = execution.includes(className);

        // assert
        assertThat(result).isEqualTo(included);
    }

    @Test
    void knowsTheStartedModules() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        boolean included = execution.isIncludedInExecution("billing");

        // assert
        assertThat(included).isTrue();
    }

    @Test
    void describesTheSetup() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act
        String description = execution.toString();

        // assert
        assertThat(description).isEqualTo(
                "ModuleTestExecution[module=order, mode=DIRECT_DEPENDENCIES, started=[order, catalog, billing, shared]]");
    }

    @Test
    void verifiesTheModulesOfTheApplication() {
        // arrange
        ModuleTestExecution execution = fullExecution();

        // act & assert
        assertThatNoException().isThrownBy(execution::verify);
    }

    @Test
    void startsNoFurtherModulesWithoutTheirEntries() {
        // arrange
        InputStream resource = resource("module=order\nbootstrap-mode=STANDALONE\n");

        // act
        ModuleTestExecution execution = ModuleTestExecution.of(resource, modules).orElseThrow();

        // assert
        assertThat(execution).extracting(ApplicationModule::getName).containsExactly("order");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "bootstrap-mode=STANDALONE|module", "module=order|bootstrap-mode",
            "module=|module" })
    void rejectsSetupsWithoutModuleOrBootstrapMode(String content, String missing) {
        // arrange
        InputStream resource = resource(content);

        // act
        Throwable failure = catchThrowable(() -> ModuleTestExecution.of(resource, modules));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("META-INF/quarkus-modulith/"
                + "module-test.properties has no " + missing + " entry. Rebuild the module test with the same version "
                + "of Quarkus Modulith as the test.");
    }

    @Test
    void leavesOutModulesThatWereNotStarted() {
        // arrange
        InputStream resource = resource("module=order\nbootstrap-mode=STANDALONE\ndependencies=\nextra-includes=\n"
                + "shared-modules=\n");

        // act
        ModuleTestExecution execution = ModuleTestExecution.of(resource, modules).orElseThrow();

        // assert
        assertThat(execution).extracting(ApplicationModule::getName).containsExactly("order");
        assertThat(execution.includes("io.quarkiverse.modulith.test.execution.catalog.Catalog")).isFalse();
        assertThat(execution.isIncludedInExecution("catalog")).isFalse();
    }

    @Test
    void isEmptyOutsideModuleTests() {
        // act
        Optional<ModuleTestExecution> execution = ModuleTestExecution.of(null, modules);

        // assert
        assertThat(execution).isEmpty();
    }

    @Test
    void rejectsUnknownModules() {
        // arrange
        InputStream resource = resource("module=unknown\nbootstrap-mode=STANDALONE\n");

        // act
        Throwable failure = catchThrowable(() -> ModuleTestExecution.of(resource, modules));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class)
                .hasMessage("The module test names an unknown module unknown");
    }

    @Test
    void reportsUnreadableResources() {
        // arrange
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }
        };

        // act
        Throwable failure = catchThrowable(() -> ModuleTestExecution.of(broken, modules));

        // assert
        assertThat(failure).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot read META-INF/quarkus-modulith/module-test.properties");
    }

    @Test
    void currentNeedsARunningApplication() {
        // act
        Throwable failure = catchThrowable(ModuleTestExecution::current);

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("No Quarkus application is running");
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentNeedsTheApplicationModules() {
        // arrange
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<ApplicationModules> handle = mock(InstanceHandle.class);
        when(container.instance(ApplicationModules.class)).thenReturn(handle);

        // act
        Throwable failure = catchThrowable(() -> ModuleTestExecution.current(container));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("The running application has no "
                + "ApplicationModules bean. Module tests need an application with the Quarkus Modulith extension.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void currentIsEmptyOutsideModuleTests() {
        // arrange
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<ApplicationModules> handle = mock(InstanceHandle.class);
        when(container.instance(ApplicationModules.class)).thenReturn(handle);
        when(handle.isAvailable()).thenReturn(true);
        when(handle.get()).thenReturn(modules);

        // act
        Optional<ModuleTestExecution> execution = ModuleTestExecution.current(container);

        // assert
        assertThat(execution).isEmpty();
    }

    private ModuleTestExecution fullExecution() {
        return ModuleTestExecution.of(resource(FULL_SETUP), modules).orElseThrow();
    }

    private static InputStream resource(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
    }
}
