package io.quarkiverse.modulith.events.jobrunr.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.Optional;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;

import org.junit.jupiter.api.Test;

class TransactionalStorageProviderProducerTest {

    @Test
    void selectsTheNamedOrTheDefaultDataSource() {
        // act
        Annotation named = TransactionalStorageProviderProducer.qualifier(Optional.of("jobs"));
        Annotation unnamed = TransactionalStorageProviderProducer.qualifier(Optional.empty());

        // assert
        assertThat(named).isEqualTo(NamedLiteral.of("jobs"));
        assertThat(unnamed).isEqualTo(Default.Literal.INSTANCE);
    }
}
