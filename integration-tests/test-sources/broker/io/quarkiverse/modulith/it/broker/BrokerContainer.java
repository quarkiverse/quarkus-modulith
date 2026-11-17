package io.quarkiverse.modulith.it.broker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

/**
 * The Dev Services broker container of a test, found by the host port the application connects to. Pausing it looks
 * like a broker that stopped answering. Shared by the externalization apps through a test source folder.
 */
public final class BrokerContainer {

    private final DockerClient docker;
    private final String id;

    private BrokerContainer(DockerClient docker, String id) {
        this.docker = docker;
        this.id = id;
    }

    /**
     * @param address the broker address the application uses, such as {@code localhost:32771} or
     *        {@code amqp://localhost:32771}
     * @return the only running container that publishes the port of the address
     */
    public static BrokerContainer publishing(String address) {
        int port = Integer.parseInt(address.substring(address.lastIndexOf(':') + 1).replaceAll("[^0-9].*$", ""));
        DockerClient docker = DockerClientFactory.instance().client();
        List<String> ids = docker.listContainersCmd().exec().stream()
                .filter(container -> publishes(container, port))
                .map(Container::getId)
                .toList();
        if (ids.size() != 1) {
            throw new IllegalStateException("Expected one container publishing port " + port + " but found " + ids);
        }
        return new BrokerContainer(docker, ids.get(0));
    }

    public void pause() {
        docker.pauseContainerCmd(id).exec();
    }

    public void unpause() {
        docker.unpauseContainerCmd(id).exec();
    }

    /**
     * A container on the host network, or one that is still starting, has no port mappings at all.
     */
    private static boolean publishes(Container container, int port) {
        return Stream.ofNullable(container.getPorts()).flatMap(Arrays::stream)
                .anyMatch(mapping -> Objects.equals(mapping.getPublicPort(), port));
    }
}
