package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.junit.diff.ModifiedFile;
import io.quarkiverse.modulith.junit.fixtures.app.order.OrderFixture;
import io.quarkiverse.modulith.junit.fixtures.plain.PlainFixture;

class ChangesTests {

    @ParameterizedTest
    @ValueSource(strings = { "src/main/resources/some.txt", "src/test/resources/some.txt",
            "src/main/resources/application.properties" })
    void detectsClasspathFileChange(String path) {
        // arrange
        var changes = Changes.of(ModifiedFile.of(path), OnNoChange.EXECUTE_ALL);

        // act
        var result = changes.hasClasspathResourceChange();

        // assert
        assertThat(result).isTrue();
        assertThat(changes.hasBuildResourceChanges()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
            "gradle.properties", "gradle/libs.versions.toml", "gradle/wrapper/gradle-wrapper.properties", ".gradle" })
    void detectsBuildResourceChange(String path) {
        // arrange
        var change = new OtherFileChange(path);

        // act
        var result = change.affectsBuildResource();

        // assert
        assertThat(result).isTrue();
        assertThat(change.isClasspathResource()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "module-a/pom.xml", "module-a/build.gradle.kts", "module-a/settings.gradle.kts",
            "buildSrc/build.gradle.kts", "buildSrc/src/main/kotlin/my.convention.gradle.kts",
            "build-logic/convention/build.gradle.kts", "config/conventions/java.gradle.kts",
            "src/main/kotlin/example.gradle.kts", "src/main/resources/sample/build.gradle",
            "src/test/resources/fixtures/pom.xml", "README.md", "pom.xml.bak", "build.gradle.bak",
            "gradle/other.toml" })
    void ignoresOtherFilesAsBuildResources(String path) {
        // arrange
        var change = new OtherFileChange(path);

        // act
        var result = change.affectsBuildResource();

        // assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "README.md", "src/main/java-resources/x.txt", "docs/src/main/resources/x.txt" })
    void ignoresNonClasspathFiles(String path) {
        // arrange
        var change = new OtherFileChange(path);

        // act
        var result = change.isClasspathResource();

        // assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "src/main/kotlin/com/example/Service.kt, com.example.Service, KotlinSourceChange",
            "src/test/kotlin/com/example/ServiceTest.kt, com.example.ServiceTest, KotlinTestSourceChange",
            "src/main/java/com/example/Service.java, com.example.Service, JavaSourceChange",
            "src/test/java/com/example/ServiceTest.java, com.example.ServiceTest, JavaTestSourceChange",
            "module/src/main/java/com/example/Service.java, com.example.Service, JavaSourceChange",
            "src/test/java/a/src/main/java/B.java, a.src.main.java.B, JavaTestSourceChange",
            "src/main/java/x/src/test/java/C.java, x.src.test.java.C, JavaSourceChange",
            "com/example/Loose.java, com.example.Loose, JavaSourceChange",
            "com/example/Loose.kt, com.example.Loose, KotlinSourceChange",
            "Foo.java, Foo, JavaSourceChange"
    })
    void classifiesSourceChanges(String path, String className, String type) {
        // arrange
        var file = new ModifiedFile(path);
        SourceChange expected = switch (type) {
            case "KotlinSourceChange" -> new KotlinSourceChange(className);
            case "KotlinTestSourceChange" -> new KotlinTestSourceChange(className);
            case "JavaTestSourceChange" -> new JavaTestSourceChange(className);
            default -> new JavaSourceChange(className);
        };

        // act
        var change = Changes.toChange(file);

        // assert
        assertThat(change).isEqualTo(expected);
    }

    @Test
    void shouldInterpretModifiedFilePathsCorrectly() {
        // arrange
        var modifiedFilePaths = ModifiedFile.of(
                "src/main/java/org/acme/junit/Changes.java",
                "src/test/java/org/acme/ChangesTest.java",
                "src/test/kotlin/org/acme/KotlinServiceTest.kt",
                "src/main/kotlin/org/acme/KotlinService.kt",
                "src/main/resources/META-INF/additional-configuration-metadata.json");

        // act
        var result = Changes.of(modifiedFilePaths, OnNoChange.EXECUTE_ALL);

        // assert
        assertThat(result.hasClasspathResourceChange()).isTrue();
        assertThat(result).containsExactlyInAnyOrder(
                new JavaSourceChange("org.acme.junit.Changes"),
                new JavaTestSourceChange("org.acme.ChangesTest"),
                new KotlinTestSourceChange("org.acme.KotlinServiceTest"),
                new KotlinSourceChange("org.acme.KotlinService"),
                new OtherFileChange("src/main/resources/META-INF/additional-configuration-metadata.json"));
        assertThat(result.getChangedClasses()).containsExactlyInAnyOrder("org.acme.junit.Changes",
                "org.acme.ChangesTest", "org.acme.KotlinServiceTest", "org.acme.KotlinService");
    }

    @Test
    void detectsClassChanges() {
        // arrange
        var changes = Changes.of(ModifiedFile.of("README.md", "src/main/java/a/B.java"), OnNoChange.EXECUTE_ALL);

        // act
        var result = changes.hasClassChanges();

        // assert
        assertThat(result).isTrue();
        assertThat(changes.isEmpty()).isFalse();
        assertThat(changes.hasClasspathResourceChange()).isFalse();
        assertThat(changes.hasBuildResourceChanges()).isFalse();
    }

    @Test
    void detectsAbsenceOfClassChanges() {
        // arrange
        var changes = Changes.of(ModifiedFile.of("README.md"), OnNoChange.EXECUTE_ALL);

        // act
        var result = changes.hasClassChanges();

        // assert
        assertThat(result).isFalse();
        assertThat(changes.getChangedClasses()).isEmpty();
    }

    @Test
    void detectsChangedType() {
        // arrange
        var changes = Changes.of(ModifiedFile.of(
                "src/test/java/io/quarkiverse/modulith/junit/fixtures/app/order/OrderFixture.java"),
                OnNoChange.EXECUTE_ALL);

        // act
        var containsOrder = changes.contains(OrderFixture.class);
        var containsPlain = changes.contains(PlainFixture.class);

        // assert
        assertThat(containsOrder).isTrue();
        assertThat(containsPlain).isFalse();
    }

    @Test
    void matchesOriginOfFileChanges() {
        // arrange
        Change change = new OtherFileChange("README.md");

        // act
        var same = change.hasOrigin("README.md");
        var other = change.hasOrigin("README");

        // assert
        assertThat(same).isTrue();
        assertThat(other).isFalse();
    }

    @Test
    void matchesOriginOfSourceChanges() {
        // arrange
        Change change = new KotlinSourceChange("com.acme.Foo");

        // act
        var same = change.hasOrigin("com.acme.Foo");
        var other = change.hasOrigin("com.acme.Fo");

        // assert
        assertThat(same).isTrue();
        assertThat(other).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "EXECUTE_ALL, false", "SKIP_ALL, true" })
    void exposesOnNoChangesConfiguration(OnNoChange onNoChange, boolean skip) {
        // arrange
        var changes = Changes.of(Stream.empty(), onNoChange);

        // act
        var result = changes.skipTestsOnNoChanges();

        // assert
        assertThat(result).isEqualTo(skip);
        assertThat(changes.isEmpty()).isTrue();
    }

    @Test
    void noneHasNoChangesAndExecutesAllTests() {
        // arrange
        var changes = Changes.NONE;

        // act
        var empty = changes.isEmpty();

        // assert
        assertThat(empty).isTrue();
        assertThat(changes.skipTestsOnNoChanges()).isFalse();
        assertThat(changes).isEmpty();
        assertThat(changes).hasToString("");
    }

    @Test
    void rendersChangesSortedOnePerLine() {
        // arrange
        var changes = Changes.of(ModifiedFile.of("b.txt", "a.txt"), OnNoChange.EXECUTE_ALL);

        // act
        var result = changes.toString();

        // assert
        assertThat(result).isEqualTo("OtherFileChange[path=a.txt]\nOtherFileChange[path=b.txt]");
    }
}
