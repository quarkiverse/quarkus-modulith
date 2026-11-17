package io.quarkiverse.modulith.deployment.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.deployment.docs.fixtures.config.ChildConfig;
import io.quarkiverse.modulith.deployment.docs.fixtures.config.ParentConfig;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.AuditListener;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.ComposedListeners;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.DoubleListener;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.Shipped;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.SyncListener;
import io.quarkiverse.modulith.docs.ListenerMethod;
import io.quarkiverse.modulith.docs.ModuleProperty;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * Reads documentation details from small Jandex indexes of single fixtures.
 */
class DocsIndexTest {

    static final String SHIPPED = Shipped.class.getName();

    @Test
    void findsListenersOfComposedAnnotations() throws IOException {
        // arrange
        Index index = Index.of(ComposedListeners.class, SyncListener.class, AuditListener.class, DoubleListener.class,
                ApplicationModuleListener.class);
        DocumentationModelBuilder builder = new DocumentationModelBuilder(index);

        // act
        List<ListenerMethod> listeners = builder.listenerMethods(index.getClassByName(ComposedListeners.class));

        // assert
        assertThat(listeners).containsExactly(new ListenerMethod("direct", List.of(SHIPPED), List.of(SHIPPED), false),
                new ListenerMethod("nested", List.of(SHIPPED), List.of(SHIPPED), false),
                new ListenerMethod("twice", List.of(SHIPPED), List.of(SHIPPED), false));
    }

    @Test
    void listsThePropertiesOfSuperInterfaces() throws IOException {
        // arrange
        Index index = Index.of(ChildConfig.class, ParentConfig.class);
        ConfigPropertyCollector collector = new ConfigPropertyCollector(new TypeInspector(index),
                DocumentationSource.none());

        // act
        List<ModuleProperty> properties = collector.collect(index.getClassByName(ChildConfig.class));

        // assert
        assertThat(properties).containsExactly(
                new ModuleProperty("shop.child.timeout", "int", ParentConfig.class.getName(), Optional.empty(),
                        Optional.of("10")),
                new ModuleProperty("shop.child.name", "java.lang.String", ChildConfig.class.getName(), Optional.empty(),
                        Optional.empty()));
    }
}
