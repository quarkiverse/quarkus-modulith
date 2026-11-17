package io.quarkiverse.modulith.events.neo4j.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;

class EventHashTest {

    private static final String EVENT = "{\"orderId\":\"o-1\"}";

    @Test
    void hashesLikeSpringModulith() {
        // act
        String sha256 = EventHash.sha256(EVENT);

        // assert
        assertThat(sha256).isEqualTo("80ea8a979b50c50b35a32bbb085f8524a79f780c52d4d5cbe560831a413769cb");
    }

    @Test
    void offersTheMd5HashOfOlderVersionsAsSecondCandidate() {
        // act
        List<String> candidates = EventHash.candidates(EVENT);

        // assert
        assertThat(candidates).containsExactly("80ea8a979b50c50b35a32bbb085f8524a79f780c52d4d5cbe560831a413769cb",
                "d43159842a6911108856bdc9a0a71621");
    }

    @Test
    void reportsAMissingAlgorithm() {
        // act
        Throwable thrown = catchThrowable(() -> EventHash.digest("SHA-999", EVENT));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("The JVM has no SHA-999 message digest");
    }
}
