package io.quarkiverse.modulith.apt;

import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;

/**
 * A filer whose every operation fails, as on a full disk.
 */
final class FailingFiler implements Filer {

    @Override
    public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements) throws IOException {
        throw new IOException("disk full");
    }

    @Override
    public JavaFileObject createClassFile(CharSequence name, Element... originatingElements) throws IOException {
        throw new IOException("disk full");
    }

    @Override
    public FileObject createResource(Location location, CharSequence moduleAndPkg, CharSequence relativeName,
            Element... originatingElements) throws IOException {
        throw new IOException("disk full");
    }

    @Override
    public FileObject getResource(Location location, CharSequence moduleAndPkg, CharSequence relativeName)
            throws IOException {
        throw new IOException("disk full");
    }
}
