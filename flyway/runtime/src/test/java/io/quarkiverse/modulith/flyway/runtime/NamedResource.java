package io.quarkiverse.modulith.flyway.runtime;

import java.io.Reader;
import java.io.StringReader;

import org.flywaydb.core.api.resource.LoadableResource;

public final class NamedResource extends LoadableResource {

    private final String path;

    public NamedResource(String path) {
        this.path = path;
    }

    @Override
    public Reader read() {
        return new StringReader("");
    }

    @Override
    public String getAbsolutePath() {
        return path;
    }

    @Override
    public String getAbsolutePathOnDisk() {
        return path;
    }

    @Override
    public String getFilename() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public String getRelativePath() {
        return path;
    }
}
