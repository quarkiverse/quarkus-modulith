package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.Externalized;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.AbstractBase;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Annotated;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.BlankMolecule;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Derived;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.DoubleMeta;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Implementor;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Marker;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Meta;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.MetaMeta;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.MetaMetaUser;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.MetaUser;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Molecule;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.Plain;

class ExternalizedTypeScannerTest {

    private static final String SCAN = "io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan.";

    private static Map<DotName, List<AnnotationInstance>> scan() throws IOException {
        Index index = Index.of(Externalized.class, org.jmolecules.event.annotation.Externalized.class,
                Annotated.class, Marker.class, Implementor.class, AbstractBase.class, Derived.class, Meta.class,
                MetaUser.class, MetaMeta.class, MetaMetaUser.class, Molecule.class, BlankMolecule.class, Plain.class);
        return new ExternalizedTypeScanner(index, index).scan();
    }

    @Test
    void findsConcreteTypesThroughHierarchyAndMetaAnnotations() throws IOException {
        // act
        Map<DotName, List<AnnotationInstance>> types = scan();

        // assert
        assertThat(types.keySet()).extracting(DotName::toString).containsExactly(SCAN + "Annotated",
                SCAN + "BlankMolecule", SCAN + "Derived", SCAN + "Implementor", SCAN + "MetaMetaUser",
                SCAN + "MetaUser", SCAN + "Molecule");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Annotated|a::{event.id}|a::{event.id}",
            "BlankMolecule|quarkus-only|quarkus-only",
            "Derived|base|base,marker",
            "Implementor|marker|marker",
            "MetaMetaUser|meta::#{#this.id()}|meta::#{#this.id()}",
            "MetaUser|meta::#{#this.id()}|meta::#{#this.id()}",
            "Molecule|jmolecules|jmolecules,quarkus"
    })
    void readsTheDeclarations(String type, String declaration, String declarations) throws IOException {
        // arrange
        List<AnnotationInstance> annotations = scan().get(DotName.createSimple(SCAN + type));

        // act
        String winner = ExternalizedTypeScanner.declaration(annotations);
        Set<String> all = ExternalizedTypeScanner.declarations(annotations);

        // assert
        assertThat(winner).isEqualTo(declaration);
        assertThat(all).containsExactlyInAnyOrder(declarations.split(","));
    }

    @Test
    void knowsTheAnnotationTypesAfterScanning() throws IOException {
        // arrange
        Index index = Index.of(Externalized.class, Meta.class, MetaMeta.class, MetaUser.class, DoubleMeta.class);
        ExternalizedTypeScanner scanner = new ExternalizedTypeScanner(index, index);

        // act
        scanner.scan();

        // assert
        assertThat(scanner.annotationTypes()).extracting(DotName::toString).containsExactly(
                Externalized.class.getName(), SCAN + "DoubleMeta", SCAN + "Meta", SCAN + "MetaMeta");
    }

    @Test
    void hasNoDeclarationWithoutTargets() {
        // act
        String declaration = ExternalizedTypeScanner.declaration(List.of());
        Set<String> declarations = ExternalizedTypeScanner.declarations(List.of());

        // assert
        assertThat(declaration).isEmpty();
        assertThat(declarations).isEmpty();
    }

    @Test
    void exposesExternalizedTypes() {
        // arrange
        ExternalizedTypeBuildItem dynamic = new ExternalizedTypeBuildItem("com.acme.A", "{event.topic}",
                Optional.empty());
        ExternalizedTypeBuildItem fixed = new ExternalizedTypeBuildItem("com.acme.B", "", Optional.of("b"));

        // act
        String typeName = dynamic.getTypeName();
        String declaration = dynamic.getDeclaration();
        Optional<String> dynamicTarget = dynamic.getStaticTarget();
        Optional<String> fixedTarget = fixed.getStaticTarget();

        // assert
        assertThat(typeName).isEqualTo("com.acme.A");
        assertThat(declaration).isEqualTo("{event.topic}");
        assertThat(dynamicTarget).isEmpty();
        assertThat(fixedTarget).contains("b");
    }
}
