package io.quarkiverse.modulith.it.externalizationjms;

import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts a broker of its own from the official image for each test class, so that a test can pause it. The broker
 * never blocks producers on a nearly full disk, which build machines often have: with the default
 * {@code max-disk-usage} of 90% every send would wait for disk space. With the {@value #CALL_TIMEOUT} argument, the URL
 * gives up on calls after that many milliseconds, so that sends to a paused broker fail fast instead of after the
 * default 30 seconds.
 */
public class ArtemisBroker implements QuarkusTestResourceLifecycleManager {

    static final String IMAGE = "apache/activemq-artemis:2.44.0";
    static final String CALL_TIMEOUT = "call-timeout";
    private static final int PORT = 61616;
    private static final String NO_DISK_LIMIT = "--java-options=-Dbrokerconfig.maxDiskUsage=-1";

    private GenericContainer<?> container;
    private Optional<String> callTimeout = Optional.empty();

    @Override
    public void init(Map<String, String> initArgs) {
        callTimeout = Optional.ofNullable(initArgs.get(CALL_TIMEOUT));
    }

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(IMAGE).withExposedPorts(PORT)
                .withEnv("ARTEMIS_USER", "artemis").withEnv("ARTEMIS_PASSWORD", "artemis")
                .withEnv("EXTRA_ARGS", NO_DISK_LIMIT)
                .waitingFor(Wait.forLogMessage(".*AMQ221007.*", 1));
        container.start();
        String url = "tcp://" + container.getHost() + ":" + container.getMappedPort(PORT);
        return Map.of("quarkus.artemis.url", callTimeout
                .map(timeout -> url + "?callTimeout=" + timeout + "&callFailoverTimeout=" + timeout + "&reconnectAttempts=0")
                .orElse(url));
    }

    @Override
    public void inject(TestInjector injector) {
        injector.injectIntoFields(new BrokerControl(container),
                new TestInjector.MatchesType(BrokerControl.class));
    }

    @Override
    public void stop() {
        container.stop();
    }
}
