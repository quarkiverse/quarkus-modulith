package io.quarkiverse.modulith.it.externalizationjms;

import org.testcontainers.containers.GenericContainer;

/**
 * Pauses and resumes the broker container, which looks like a broker that stopped answering.
 */
public class BrokerControl {

    private final GenericContainer<?> container;

    BrokerControl(GenericContainer<?> container) {
        this.container = container;
    }

    void pause() {
        container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
    }

    void resume() {
        container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
    }
}
