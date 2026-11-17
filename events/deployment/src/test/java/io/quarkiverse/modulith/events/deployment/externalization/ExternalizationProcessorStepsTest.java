package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationPublications;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRedeliveryDecorator;
import io.quarkiverse.modulith.events.externalization.runtime.NoExternalizationTransactions;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxDrain;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxEventExternalizer;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxResubmissionExclusion;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;
import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;
import io.quarkiverse.modulith.runtime.events.VirtualThreadListenerExecutor;
import io.quarkiverse.modulith.runtime.events.WorkerListenerExecutor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

class ExternalizationProcessorStepsTest {

    private static final Optional<ExternalizationStrategyBuildItem> JOBRUNR = Optional
            .of(new ExternalizationStrategyBuildItem("JobRunr"));

    private static List<String> beans(ExternalizationMode mode, Optional<ExternalizationStrategyBuildItem> outbox) {
        return beans(mode, outbox, ListenerExecutorKind.WORKER);
    }

    private static List<String> beans(ExternalizationMode mode, Optional<ExternalizationStrategyBuildItem> outbox,
            ListenerExecutorKind executor) {
        List<AdditionalBeanBuildItem> produced = new ArrayList<>();
        new ExternalizationProcessor().beans(new ExternalizationBuildItem(true, mode), outbox,
                new Capabilities(Set.of()), new FixedListenerConfig(executor), produced::add);
        return produced.stream().flatMap(item -> item.getBeanClasses().stream()).toList();
    }

    @Test
    void registersTheOutboxOfAnotherExtensionInsteadOfTheRegistryOutbox() {
        // act
        List<String> beans = beans(ExternalizationMode.OUTBOX, JOBRUNR);

        // assert
        assertThat(beans).containsExactly(RoutingResolver.class.getName(), ExternalizationPublications.class.getName(),
                NoExternalizationTransactions.class.getName(), WorkerListenerExecutor.class.getName(),
                EventExternalizer.class.getName());
    }

    @Test
    void registersTheStrategyOfTheMode() {
        // act
        List<String> listener = beans(ExternalizationMode.MODULE_LISTENER, Optional.empty());
        List<String> outbox = beans(ExternalizationMode.OUTBOX, Optional.empty());

        // assert
        assertThat(listener).contains(EventExternalizerModuleListener.class.getName(),
                ExternalizationRedeliveryDecorator.class.getName()).doesNotContain(OutboxDrain.class.getName());
        assertThat(outbox).contains(OutboxEventExternalizer.class.getName(), OutboxDrain.class.getName(),
                ExternalizationRedeliveryDecorator.class.getName(), OutboxResubmissionExclusion.class.getName());
        assertThat(listener).doesNotContain(OutboxResubmissionExclusion.class.getName());
    }

    @Test
    void activatesWithTheTransportOfAnExtension() throws IOException {
        // arrange
        Index empty = Index.of(new Class<?>[0]);
        CombinedIndexBuildItem index = new CombinedIndexBuildItem(empty, empty);
        List<ExternalizationTransportBuildItem> transports = List.of(new ExternalizationTransportBuildItem("test"));
        ExternalizationProcessor processor = new ExternalizationProcessor();

        // act
        ExternalizationBuildItem active = processor.externalization(
                new FixedBuildTimeConfig(true, ExternalizationMode.OUTBOX), index, transports);
        ExternalizationBuildItem disabled = processor.externalization(
                new FixedBuildTimeConfig(false, ExternalizationMode.OUTBOX), index, transports);
        ExternalizationBuildItem without = processor.externalization(
                new FixedBuildTimeConfig(true, ExternalizationMode.MODULE_LISTENER), index, List.of());

        // assert
        assertThat(active.isActive()).isTrue();
        assertThat(active.getMode()).isEqualTo(ExternalizationMode.OUTBOX);
        assertThat(disabled.isActive()).isFalse();
        assertThat(without.isActive()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "MODULE_LISTENER,false,true,false", "MODULE_LISTENER,true,true,false", "OUTBOX,false,true,true",
            "OUTBOX,true,false,false" })
    void decidesWhatTheModeNeeds(ExternalizationMode mode, boolean override, boolean stores, boolean repository) {
        // arrange
        Optional<ExternalizationStrategyBuildItem> outbox = override ? JOBRUNR : Optional.empty();

        // act
        boolean storesPublications = ExternalizationProcessor.storesPublications(mode, outbox);
        boolean needsRepository = ExternalizationProcessor.needsRepository(mode, outbox);

        // assert
        assertThat(storesPublications).isEqualTo(stores);
        assertThat(needsRepository).isEqualTo(repository);
    }

    @Test
    void registersTheVirtualThreadExecutorWhenModuleListenersUseIt() {
        // act
        List<String> beans = beans(ExternalizationMode.MODULE_LISTENER, Optional.empty(),
                ListenerExecutorKind.VIRTUAL_THREADS);

        // assert
        assertThat(beans).contains(VirtualThreadListenerExecutor.class.getName())
                .doesNotContain(WorkerListenerExecutor.class.getName());
    }
}
