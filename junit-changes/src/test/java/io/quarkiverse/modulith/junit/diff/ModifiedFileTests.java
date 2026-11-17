package io.quarkiverse.modulith.junit.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModifiedFileTests {

    @Test
    void returnsRelativeFile() {
        // arrange
        var file = new ModifiedFile("foo/bar.txt");

        // act
        var result = file.asRelativeTo("foo");

        // assert
        assertThat(result).isEqualTo(new ModifiedFile("bar.txt"));
    }

    @Test
    void returnsFileAsIsForEmptyReference() {
        // arrange
        var file = new ModifiedFile("foo/bar.txt");

        // act
        var result = file.asRelativeTo("");

        // assert
        assertThat(result).isSameAs(file);
    }

    @Test
    void rejectsFileOutsideOfReference() {
        // arrange
        var file = new ModifiedFile("foobar/baz.txt");

        // act
        var thrown = catchThrowable(() -> file.asRelativeTo("foo"));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Modified file foobar/baz.txt is not located in foo!");
    }

    @ParameterizedTest
    @CsvSource({
            "foo/bar.txt, '', true",
            "foo/bar.txt, foo, true",
            "foo/bar/baz.txt, foo/bar, true",
            "foobar/baz.txt, foo, false",
            "foo, foo, false",
            "bar/foo/baz.txt, foo, false"
    })
    void detectsLocation(String path, String reference, boolean expected) {
        // arrange
        var file = new ModifiedFile(path);

        // act
        var result = file.isLocatedIn(reference);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "src/main/java/com/acme/Foo.java, true, false",
            "Foo.JAVA, true, false",
            "src/main/kotlin/com/acme/Foo.kt, false, true",
            "Foo.KT, false, true",
            "README, false, false",
            "folder.java/README, false, false",
            "build.gradle.kts, false, false",
            "Foo.javax, false, false"
    })
    void detectsSourceFiles(String path, boolean java, boolean kotlin) {
        // arrange
        var file = new ModifiedFile(path);

        // act
        var isJava = file.isJavaSource();
        var isKotlin = file.isKotlinSource();

        // assert
        assertThat(isJava).isEqualTo(java);
        assertThat(isKotlin).isEqualTo(kotlin);
    }

    @Test
    void createsFilesFromPaths() {
        // arrange
        var paths = new String[] { "a.txt", "b/c.java" };

        // act
        var result = ModifiedFile.of(paths);

        // assert
        assertThat(result).containsExactly(new ModifiedFile("a.txt"), new ModifiedFile("b/c.java"));
    }
}
