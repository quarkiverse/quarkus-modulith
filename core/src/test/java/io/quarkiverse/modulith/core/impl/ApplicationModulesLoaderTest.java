package io.quarkiverse.modulith.core.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class ApplicationModulesLoaderTest {

    @Test
    void reusesTheModelOfTheSameClassLoaderAndSettings() {
        // arrange
        ApplicationModules first = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

        // act
        ApplicationModules second = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

        // assert
        assertThat(second).isSameAs(first);
    }

    @Test
    void analysesAgainForAnotherContextClassLoader(@TempDir Path directory) throws IOException {
        // arrange
        ApplicationModules first = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

        // act
        ApplicationModules second;
        try (URLClassLoader classLoader = ServiceClassLoaders.with(directory, Map.of())) {
            second = ServiceClassLoaders.callWith(classLoader,
                    () -> ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS));
        }

        // assert
        assertThat(second).isNotSameAs(first);
        assertThat(second.getModelData()).isEqualTo(first.getModelData());
    }

    @Test
    void doesNotCacheModelsFilteredByAPredicate() {
        // arrange
        DescribedPredicate<JavaClass> ignored = DescribedPredicate.alwaysFalse();
        ApplicationModules first = ApplicationModules.of(ShopApplication.class, ignored);

        // act
        ApplicationModules second = ApplicationModules.of(ShopApplication.class, ignored);

        // assert
        assertThat(second).isNotSameAs(first);
    }

    @Test
    void doesNotCacheModelsFilteredByAnImportOption() {
        // arrange
        ImportOption option = ImportOption.Predefined.ONLY_INCLUDE_TESTS;
        ApplicationModules first = ApplicationModules.of(ShopApplication.class, option);

        // act
        ApplicationModules second = ApplicationModules.of(ShopApplication.class, option);

        // assert
        assertThat(second).isNotSameAs(first);
        assertThat(second.getModelData()).isEqualTo(first.getModelData());
    }
}
