package io.quarkiverse.modulith.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.deployment.events.ListenerDeclarationsBuildItem;
import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

/**
 * Calls the listener bean step directly, for the application without {@code quarkus-narayana-jta} the extension tests
 * cannot build: the transactions extension is on their class path.
 */
class ListenerBeansTest {

    static final ListenerDeclarationsBuildItem NO_LISTENERS = new ListenerDeclarationsBuildItem(Set.of(), List.of(),
            List.of());

    @ParameterizedTest
    @CsvSource({ "true, io.quarkiverse.modulith.runtime.events.JtaTransactionSupport",
            "false, io.quarkiverse.modulith.runtime.events.NoTransactionSupport" })
    void picksTheTransactionSupportOfTheApplication(boolean transactions, String expected) {
        // arrange
        Capabilities capabilities = new Capabilities(transactions ? Set.of(Capability.TRANSACTIONS) : Set.of());
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();

        // act
        listenerBeans(capabilities, LaunchMode.NORMAL, beans);

        // assert
        assertThat(beans).singleElement().extracting(AdditionalBeanBuildItem::getBeanClasses)
                .isEqualTo(List.of(expected));
    }

    private static void listenerBeans(Capabilities capabilities, LaunchMode mode, List<AdditionalBeanBuildItem> beans) {
        List<AutoAddScopeBuildItem> scopes = new ArrayList<>();
        List<UnremovableBeanBuildItem> unremovable = new ArrayList<>();
        new ListenerProcessor().listenerBeans(NO_LISTENERS, capabilities,
                new LaunchModeBuildItem(mode, Optional.empty(), false, Optional.empty(), false),
                () -> ListenerExecutorKind.WORKER, scopes::add, beans::add, unremovable::add);
    }
}
