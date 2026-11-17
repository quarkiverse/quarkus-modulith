package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.test.loose.LooseFixture;
import io.quarkiverse.modulith.test.slice.billing.AsyncObserverOnly;
import io.quarkiverse.modulith.test.slice.billing.ListenerOnly;
import io.quarkiverse.modulith.test.slice.billing.ObserverOnly;
import io.quarkiverse.modulith.test.slice.billing.ProducerOnly;
import io.quarkiverse.modulith.test.slice.billing.ScopedBean;
import io.quarkiverse.modulith.test.slice.billing.StereotypedBean;
import io.quarkiverse.modulith.test.slice.billing.internal.BillingSliceFixture;
import io.quarkiverse.modulith.test.slice.billing.internal.InternalBean;
import io.quarkiverse.modulith.test.slice.billing.nested.NestedBean;
import io.quarkiverse.modulith.test.slice.shipping.ShippingBean;
import io.quarkiverse.modulith.test.slice.shipping.internal.ShippingSliceFixture;

class ApplicationModuleComponentsTest {

    private static final String BILLING = "io.quarkiverse.modulith.test.slice.billing.";
    private static final List<Class<?>> BILLING_BEANS = List.of(AsyncObserverOnly.class, ListenerOnly.class,
            ObserverOnly.class, ProducerOnly.class, ScopedBean.class, StereotypedBean.class, InternalBean.class);
    private static final String JARRED = "com.example.jarred";

    private final ClassLoader loader = getClass().getClassLoader();
    private final List<URLClassLoader> isolatedLoaders = new ArrayList<>();

    @TempDir
    Path temp;

    @AfterEach
    void closeIsolatedLoaders() throws IOException {
        for (URLClassLoader isolated : isolatedLoaders) {
            isolated.close();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "ScopedBean, DECLARED",
            "StereotypedBean, DECLARED",
            "ProducerOnly, PRODUCER_OR_OBSERVER",
            "ObserverOnly, PRODUCER_OR_OBSERVER",
            "AsyncObserverOnly, PRODUCER_OR_OBSERVER",
            "ListenerOnly, LISTENER",
            "AbstractScoped, NONE",
            "BillingService, NONE",
            "SliceStereotype, NONE",
            "PlainType, NONE",
            "InheritedScopeBean, NONE",
            "VetoedProducer, NONE",
            "vetoed.VetoedPackageBean, NONE",
            "nested.NestedBean, DECLARED",
            "internal.InternalBean, DECLARED" })
    void classifiesClassesLikeArc(String simpleName, ComponentKind kind) {
        // arrange
        ClassInfo type = ApplicationModuleComponents.index(BILLING + simpleName, loader).orElseThrow();

        // act
        ComponentKind result = ApplicationModuleComponents.classify(type, loader);

        // assert
        assertThat(result).isEqualTo(kind);
    }

    @ParameterizedTest
    @CsvSource({ "ScopedBean, true", "PlainType, false" })
    void tellsBeansApart(String simpleName, boolean bean) {
        // arrange
        ClassInfo type = ApplicationModuleComponents.index(BILLING + simpleName, loader).orElseThrow();

        // act
        boolean result = ApplicationModuleComponents.isBean(type, loader);

        // assert
        assertThat(result).isEqualTo(bean);
    }

    @ParameterizedTest
    @CsvSource({ "NONE, ", "DECLARED, ", "LISTENER, jakarta.inject.Singleton",
            "PRODUCER_OR_OBSERVER, jakarta.enterprise.context.Dependent" })
    void listenerAndProducerClassesGetTheScopeTheApplicationGivesThem(ComponentKind kind, String scope) {
        // act
        String implied = kind.impliedScope().map(Class::getName).orElse(null);

        // assert
        assertThat(implied).isEqualTo(scope);
    }

    @Test
    void takesTheModuleDeclaredByThePackageInfoAboveTheTestClass() {
        // act
        List<Class<?>> components = ApplicationModuleComponents.componentClasses(BillingSliceFixture.class);

        // assert
        assertThat(components).containsExactlyElementsOf(BILLING_BEANS);
    }

    @Test
    void takesTheSubPackageOfTheBasePackageAboveTheTestClass() {
        // act
        List<Class<?>> components = ApplicationModuleComponents.componentClasses(ShippingSliceFixture.class);

        // assert
        assertThat(components).containsExactly(ShippingBean.class);
    }

    @ParameterizedTest
    @CsvSource({ "invoicing, io.quarkiverse.modulith.test.slice.billing",
            "billing, io.quarkiverse.modulith.test.slice.billing",
            "shipping, io.quarkiverse.modulith.test.slice.shipping" })
    void findsModulesByIdentifier(String moduleId, String modulePackage) {
        // act
        String result = ApplicationModuleComponents.modulePackageById("io.quarkiverse.modulith.test.slice", moduleId,
                loader);

        // assert
        assertThat(result).isEqualTo(modulePackage);
    }

    @Test
    void rejectsAnUnknownModuleIdentifier() {
        // act
        Throwable failure = catchThrowable(() -> ApplicationModuleComponents
                .modulePackageById("io.quarkiverse.modulith.test.slice", "unknown", loader));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("The application in "
                + "io.quarkiverse.modulith.test.slice has no module unknown. Known modules: [invoicing, shipping]");
    }

    @Test
    void selectsTheModuleByIdentifier() {
        // act
        List<Class<?>> components = ApplicationModuleComponents.componentClasses(ShippingSliceFixture.class,
                "invoicing");

        // assert
        assertThat(components).containsExactlyElementsOf(BILLING_BEANS);
    }

    @Test
    void needsAnApplicationToSelectByIdentifier() {
        // act
        Throwable failure = catchThrowable(() -> ApplicationModuleComponents.componentClasses(LooseFixture.class,
                "billing"));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("No type annotated with @Modulith "
                + "or @Modulithic encloses io.quarkiverse.modulith.test.loose.LooseFixture, so module billing cannot "
                + "be found");
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.loose, io.quarkiverse.modulith.test.loose",
            "io.quarkiverse.modulith.test.slice, io.quarkiverse.modulith.test.slice",
            "io.quarkiverse.modulith.test.slice.shipping.internal, io.quarkiverse.modulith.test.slice.shipping",
            "io.quarkiverse.modulith.test.slice.billing.internal, io.quarkiverse.modulith.test.slice.billing" })
    void resolvesTheModulePackage(String packageName, String modulePackage) {
        // act
        String result = ApplicationModuleComponents.modulePackage(packageName, loader);

        // assert
        assertThat(result).isEqualTo(modulePackage);
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.test.loose, ",
            "io.quarkiverse.modulith.test.slice, io.quarkiverse.modulith.test.slice",
            "io.quarkiverse.modulith.test.slice.billing.internal, io.quarkiverse.modulith.test.slice",
            "io.quarkiverse.modulith.test.legacy.sub, io.quarkiverse.modulith.test.legacy" })
    void findsTheBasePackageByModulithOrModulithic(String packageName, String basePackage) {
        // act
        String result = ApplicationModuleComponents.basePackage(packageName, loader).orElse(null);

        // assert
        assertThat(result).isEqualTo(basePackage);
    }

    @Test
    void listsTheClassesOfAJarWithoutMetadataAndOtherPackages() throws IOException {
        // arrange
        ClassLoader isolated = isolated(jar());

        // act
        TreeSet<String> names = ApplicationModuleComponents.classNames(JARRED, isolated);

        // assert
        assertThat(names).containsExactly("com.example.jarred.Broken");
    }

    @Test
    void leavesUnreadableClassesOutOfTheSlice() throws IOException {
        // arrange
        ClassLoader isolated = isolated(jar());

        // act
        List<Class<?>> beans = ApplicationModuleComponents.beanClasses(JARRED, isolated);

        // assert
        assertThat(beans).isEmpty();
    }

    @Test
    void fallsBackToThePackageWithoutModuleOrApplication() throws IOException {
        // arrange
        ClassLoader isolated = isolated(jar());

        // act
        String modulePackage = ApplicationModuleComponents.modulePackage(JARRED, isolated);

        // assert
        assertThat(modulePackage).isEqualTo(JARRED);
    }

    @Test
    void ignoresAnnotationsThatCannotBeLoaded() throws IOException {
        // arrange
        ClassLoader isolated = isolated(jar());
        ClassInfo type = ApplicationModuleComponents.index(BILLING + "ScopedBean", isolated).orElseThrow();

        // act
        ComponentKind kind = ApplicationModuleComponents.classify(type, isolated);

        // assert
        assertThat(kind).isEqualTo(ComponentKind.NONE);
    }

    @Test
    void takesTheBeansOfANestedModuleItself() {
        // act
        List<Class<?>> beans = ApplicationModuleComponents.beanClasses(BILLING + "nested", loader);

        // assert
        assertThat(beans).containsExactly(NestedBean.class);
    }

    @ParameterizedTest
    @CsvSource({ "true, com.example.dir.sub.Nested", "false, " })
    void readsDirectoriesAndSkipsModuleInfo(boolean recursive, String expected) throws IOException {
        // arrange
        Path dir = Files.createDirectories(temp.resolve("classes/com/example/dir/sub"));
        Files.write(temp.resolve("classes/com/example/dir/module-info.class"), new byte[] { 1 });
        Files.write(dir.resolve("Nested.class"), new byte[] { 1 });
        ClassLoader isolated = isolated(temp.resolve("classes"));

        // act
        TreeSet<String> names = ApplicationModuleComponents.classNames("com.example.dir", isolated, recursive);

        // assert
        assertThat(names).containsExactlyElementsOf(Stream.ofNullable(expected).toList());
    }

    @Test
    void readsDirectoriesWhosePathNeedsEncoding() throws IOException {
        // arrange
        Path classes = temp.resolve("c++ and spaces/classes");
        Files.createDirectories(classes.resolve("com/example/plus"));
        Files.write(classes.resolve("com/example/plus/Plus.class"), new byte[] { 1 });
        ClassLoader isolated = isolated(classes);

        // act
        TreeSet<String> names = ApplicationModuleComponents.classNames("com.example.plus", isolated);

        // assert
        assertThat(names).containsExactly("com.example.plus.Plus");
    }

    @Test
    void skipsClassPathLocationsThatAreNeitherDirectoriesNorJars() throws IOException {
        // arrange
        ClassLoader remote = new FixedResourcesClassLoader(URI.create("http://localhost/classes/com/example").toURL());

        // act
        TreeSet<String> names = ApplicationModuleComponents.classNames("com.example", remote);

        // assert
        assertThat(names).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void reportsInvalidDirectoryLocations() throws IOException {
        // arrange
        ClassLoader invalid = new FixedResourcesClassLoader(new URL("file:/invalid path"));

        // act
        Throwable failure = catchThrowable(() -> ApplicationModuleComponents.classNames("com.example", invalid));

        // assert
        assertThat(failure).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot list the classes of com.example")
                .hasRootCauseInstanceOf(URISyntaxException.class);
    }

    @Test
    void reportsUnreadableClassPaths() {
        // arrange
        ClassLoader broken = new FixedResourcesClassLoader(new IOException("broken"));

        // act
        Throwable failure = catchThrowable(() -> ApplicationModuleComponents.classNames("com.example", broken));

        // assert
        assertThat(failure).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot list the classes of com.example");
    }

    private Path jar() throws IOException {
        Path jar = temp.resolve("module.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            directory(out, "com/");
            directory(out, "com/example/");
            directory(out, "com/example/jarred/");
            entry(out, "com/example/jarred/Broken.class", new byte[] { 1, 2, 3 });
            entry(out, "com/example/jarred/package-info.class", new byte[] { 1, 2, 3 });
            entry(out, "com/example/jarred/notes.txt", new byte[] { 1, 2, 3 });
            entry(out, "com/example/other/Other.class", new byte[] { 1, 2, 3 });
            entry(out, "io/quarkiverse/modulith/test/slice/billing/ScopedBean.class", classFile(ScopedBean.class));
        }
        return jar;
    }

    private ClassLoader isolated(Path location) throws IOException {
        URLClassLoader isolated = new URLClassLoader(new URL[] { location.toUri().toURL() }, null);
        isolatedLoaders.add(isolated);
        return isolated;
    }

    private byte[] classFile(Class<?> type) throws IOException {
        try (InputStream in = loader.getResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }

    private static void directory(JarOutputStream out, String name) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.closeEntry();
    }

    private static void entry(JarOutputStream out, String name, byte[] content) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(content);
        out.closeEntry();
    }
}
