package io.quarkiverse.modulith.events.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntitiesTest {

    @Test
    void archivedRowsHaveThePublicationColumns() {
        // arrange
        ArchivedJpaEventPublication archived = new ArchivedJpaEventPublication();

        // act
        archived.listenerId = "l";

        // assert
        assertThat(archived).isInstanceOf(AbstractJpaEventPublication.class);
        assertThat(archived.listenerId).isEqualTo("l");
    }
}
