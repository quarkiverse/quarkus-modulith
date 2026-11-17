package io.quarkiverse.modulith.deployment.moduletest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Reads the module test description the build wrote, from inside the test application.
 */
public final class SetupResource {

    private SetupResource() {
    }

    /**
     * @return the resource content, or {@code null} when the build wrote none
     */
    public static String read() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ModuleTests.SETUP_RESOURCE)) {
            return Objects.isNull(in) ? null : new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
