package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.deployment.model.Fixtures;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig.BootstrapMode;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;

class ModuleTestsTest {

    static final String NESTED = "io.quarkiverse.modulith.deployment.fixtures.nested";
    static final String DDD = "io.quarkiverse.modulith.deployment.fixtures.model.ddd";

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void registersTheRecorderOnlyWhenJunitIsPresent(boolean present) {
        // act
        Optional<AdditionalBeanBuildItem> bean = ModuleTests.recorderBean(name -> present
                && name.equals("io.quarkiverse.modulith.test.PublishedEventsRecorder"));

        // assert
        assertThat(bean.map(AdditionalBeanBuildItem::getBeanClasses))
                .isEqualTo(present ? Optional.of(List.of("io.quarkiverse.modulith.test.PublishedEventsRecorder"))
                        : Optional.empty());
        assertThat(bean.map(AdditionalBeanBuildItem::isRemovable)).isEqualTo(present ? Optional.of(true)
                : Optional.empty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void registersTheTestAnnotationOnlyWhenJunitIsPresent(boolean present) {
        // act
        Optional<TestAnnotationBuildItem> annotation = ModuleTests.testAnnotation(name -> present
                && name.equals("io.quarkiverse.modulith.test.ApplicationModuleTest"));

        // assert
        assertThat(annotation.map(TestAnnotationBuildItem::getAnnotationClassName))
                .isEqualTo(present ? Optional.of("io.quarkiverse.modulith.test.ApplicationModuleTest")
                        : Optional.empty());
    }

    @Test
    void noModelMeansNoModuleTest() {
        // arrange
        ModuleTestConfig config = new FixedModuleTestConfig(Optional.of("order"), BootstrapMode.STANDALONE,
                Optional.empty(), true);

        // act
        Optional<ModuleTestSetup> setup = ModuleTests.setup(Optional.empty(), config);

        // assert
        assertThat(setup).isEmpty();
    }

    @Test
    void keepsTheNestedModuleUnderTest() {
        // arrange
        ModuleModel model = Fixtures.model(NESTED, Optional.of(NESTED));
        ModuleTestConfig config = new FixedModuleTestConfig(Optional.of("order.sub"), BootstrapMode.STANDALONE,
                Optional.empty(), true);

        // act
        ModuleTestSetup setup = ModuleTests.setup(Optional.of(model), config).orElseThrow();

        // assert
        assertThat(setup.excludedPackages()).containsExactly(NESTED + ".billing", NESTED + ".order",
                NESTED + ".order.other");
        assertThat(setup.keptPackages()).containsExactly(NESTED + ".order.sub");
        assertThat(setup.excludedTypes()).containsExactly(NESTED + ".billing.**", NESTED + ".order.*",
                NESTED + ".order.internal.*", NESTED + ".order.other.**");
    }

    @Test
    void excludesWholeModulesWithoutNestedModuleUnderTest() {
        // arrange
        ModuleModel model = Fixtures.model(NESTED, Optional.of(NESTED));
        ModuleTestConfig config = new FixedModuleTestConfig(Optional.of("billing"), BootstrapMode.STANDALONE,
                Optional.empty(), true);

        // act
        ModuleTestSetup setup = ModuleTests.setup(Optional.of(model), config).orElseThrow();

        // assert
        assertThat(setup.keptPackages()).isEmpty();
        assertThat(setup.excludedTypes()).containsExactly(NESTED + ".order.**", NESTED + ".order.other.**",
                NESTED + ".order.sub.**");
    }

    @Test
    void passesWithoutViolationsOfDisabledRules() {
        // arrange
        ModuleModel model = Fixtures.model(DDD, Optional.of(DDD));
        ModuleTestConfig config = new FixedModuleTestConfig(Optional.of("orders"), BootstrapMode.STANDALONE,
                Optional.empty(), true);

        // act
        Optional<ModuleTestSetup> setup = ModuleTests.setup(Optional.of(model), config);

        // assert
        assertThat(setup).map(ModuleTestSetup::module).hasValue("orders");
    }

    @ParameterizedTest
    @CsvSource({ "order.Service, true", "order.internal.Store, true", "order.sub.Service, false",
            "order.sub.internal.Store, false", "order.sub.deep.Service, true", "billing.Service, false",
            "orders.Service, false" })
    void excludesTheTypesOfTheClosestExcludedModule(String type, boolean expected) {
        // arrange
        ExcludedModulesBuildItem excluded = new ExcludedModulesBuildItem(Set.of("order", "order.sub.deep"),
                Set.of("order.sub"));

        // act
        boolean excludes = excluded.excludes(type);

        // assert
        assertThat(excludes).isEqualTo(expected);
    }

    @Test
    void describesTheSetup() {
        // arrange
        ModuleTestSetup setup = new ModuleTestSetup("order", BootstrapMode.DIRECT_DEPENDENCIES,
                List.of("catalog", "shared"), List.of("billing"), List.of("shared"), Set.of("com.acme.audit"),
                Set.of(), Set.of("com.acme.audit.**"));

        // act
        String description = setup.describe();

        // assert
        assertThat(description).isEqualTo("module=order\nbootstrap-mode=DIRECT_DEPENDENCIES\n"
                + "dependencies=catalog,shared\nextra-includes=billing\nshared-modules=shared\n");
        assertThat(setup.includedModules()).containsExactly("order", "catalog", "shared", "billing");
        assertThat(setup.excludedPackages()).containsExactly("com.acme.audit");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "order|order",
            "a:b=c|a\\:b\\=c",
            "x#y!z w|x\\#y\\!z\\ w",
            "back\\slash|back\\\\slash",
            "caf\u00e9|caf\\u00e9",
            "tab\tline|tab\\u0009line" })
    void escapesValuesForTheProperties(String value, String escaped) {
        // act
        String result = ModuleTestSetup.escape(value);

        // assert
        assertThat(result).isEqualTo(escaped);
    }

    @Test
    void describedSetupReadsBackAsProperties() throws IOException {
        // arrange
        ModuleTestSetup setup = new ModuleTestSetup("caf\u00e9:1", BootstrapMode.STANDALONE, List.of(), List.of(),
                List.of("sh ared"), Set.of(), Set.of(), Set.of());
        Properties properties = new Properties();

        // act
        properties.load(new StringReader(setup.describe()));

        // assert
        assertThat(properties).containsEntry("module", "caf\u00e9:1").containsEntry("shared-modules", "sh ared")
                .containsEntry("dependencies", "");
    }
}
