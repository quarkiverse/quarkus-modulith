package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.Modulith;
import io.quarkiverse.modulith.junit.fixtures.scan.ScanApplication;
import io.quarkiverse.modulith.junit.fixtures.scan.sub.ScanModuleType;

class BasePackageFinderTests {

    private static final String SCAN = "io/quarkiverse/modulith/junit/fixtures/scan";

    @TempDir
    Path temp;

    @ParameterizedTest
    @CsvSource({
            "io.quarkiverse.modulith.junit.fixtures.app.order, io.quarkiverse.modulith.junit.fixtures.app",
            "io.quarkiverse.modulith.junit.fixtures.app, io.quarkiverse.modulith.junit.fixtures.app",
            "io.quarkiverse.modulith.junit.fixtures.app.order.internal, io.quarkiverse.modulith.junit.fixtures.app",
            "io.quarkiverse.modulith.junit.fixtures.modulithic.billing, io.quarkiverse.modulith.junit.fixtures.modulithic"
    })
    void findsClosestPackageWithApplicationType(String packageName, String expected) {
        // arrange
        var finder = new BasePackageFinder();

        // act
        var result = finder.find(packageName, getClass().getClassLoader());

        // assert
        assertThat(result).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "io.quarkiverse.modulith.junit.fixtures.multiple",
            "io.quarkiverse.modulith.junit.fixtures.plain",
            "''"
    })
    void findsNoUniqueApplicationType(String packageName) {
        // arrange
        var finder = new BasePackageFinder();

        // act
        var result = finder.find(packageName, getClass().getClassLoader());

        // assert
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    void cachesResultPerClassLoaderAndPackage() {
        // arrange
        var finder = new BasePackageFinder();
        var loader = new FixedResourcesClassLoader(List.of(), Optional.empty());
        finder.find("com.acme", loader);

        // act
        var result = finder.find("com.acme", loader);

        // assert
        assertThat(result).isEqualTo(Optional.empty());
        assertThat(loader.lookups()).isEqualTo(2);
    }

    @Test
    void looksUpAgainForAnotherClassLoader() {
        // arrange
        var finder = new BasePackageFinder();
        var packageName = "io.quarkiverse.modulith.junit.fixtures.app.order";
        finder.find(packageName, getClass().getClassLoader());

        // act
        var result = finder.find(packageName,
                new FixedResourcesClassLoader(List.of(), Optional.of(new IOException("not cached"))));

        // assert
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    void scansJarFiles() throws Exception {
        // arrange
        var jar = temp.resolve("app.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (var directory : List.of("io/", "io/quarkiverse/", "io/quarkiverse/modulith/",
                    "io/quarkiverse/modulith/junit/", "io/quarkiverse/modulith/junit/fixtures/", SCAN + "/",
                    SCAN + "/sub/", SCAN + "/sub/deeper/", "other/")) {
                out.putNextEntry(new JarEntry(directory));
            }
            add(out, "io/quarkiverse/modulith/Modulith.class", classBytes(Modulith.class));
            add(out, SCAN + "/ScanApplication.class", classBytes(ScanApplication.class));
            add(out, SCAN + "/sub/ScanModuleType.class", classBytes(ScanModuleType.class));
            add(out, SCAN + "/sub/Broken.class", new byte[] { 1, 2, 3 });
            add(out, SCAN + "/sub/notes.txt", new byte[] { 1 });
            add(out, SCAN + "/sub/deeper/ScanApplication.class", classBytes(ScanApplication.class));
            add(out, "other/ScanApplication.class", classBytes(ScanApplication.class));
        }

        try (var loader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
            // act
            var result = new BasePackageFinder().find("io.quarkiverse.modulith.junit.fixtures.scan.sub", loader);

            // assert
            assertThat(result).contains("io.quarkiverse.modulith.junit.fixtures.scan");
        }
    }

    @Test
    void scansDirectories() throws Exception {
        // arrange
        var classes = temp.resolve("classes");
        write(classes.resolve("io/quarkiverse/modulith/Modulith.class"), classBytes(Modulith.class));
        write(classes.resolve(SCAN + "/ScanApplication.class"), classBytes(ScanApplication.class));
        write(classes.resolve(SCAN + "/sub/ScanModuleType.class"), classBytes(ScanModuleType.class));
        write(classes.resolve(SCAN + "/sub/Broken.class"), new byte[] { 1, 2, 3 });
        write(classes.resolve(SCAN + "/sub/notes.txt"), new byte[] { 1 });
        write(classes.resolve(SCAN + "/sub/deeper/ScanApplication.class"), classBytes(ScanApplication.class));

        try (var loader = new URLClassLoader(new URL[] { classes.toUri().toURL() },
                ClassLoader.getPlatformClassLoader())) {
            // act
            var result = new BasePackageFinder().find("io.quarkiverse.modulith.junit.fixtures.scan.sub", loader);

            // assert
            assertThat(result).contains("io.quarkiverse.modulith.junit.fixtures.scan");
        }
    }

    @Test
    void skipsUnreadableClassPathLocations() throws Exception {
        // arrange
        var ghosts = temp.resolve("ghosts");
        write(ghosts.resolve("Ghost.class"), new byte[] { 1 });
        var loader = new FixedResourcesClassLoader(List.of(
                URI.create("http://localhost/unsupported").toURL(),
                temp.resolve("missing").toUri().toURL(),
                ghosts.toUri().toURL()), Optional.empty());

        // act
        var result = new BasePackageFinder().find("com.acme", loader);

        // assert
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    void skipsClassPathThatCannotBeListed() {
        // arrange
        var loader = new FixedResourcesClassLoader(List.of(), Optional.of(new IOException("broken")));

        // act
        var result = new BasePackageFinder().find("com.acme", loader);

        // assert
        assertThat(result).isEqualTo(Optional.empty());
    }

    private static void add(JarOutputStream out, String name, byte[] content) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(content);
        out.closeEntry();
    }

    private static void write(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        try (InputStream in = type.getClassLoader().getResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }
}
