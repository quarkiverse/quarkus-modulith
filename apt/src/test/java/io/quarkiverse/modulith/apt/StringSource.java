package io.quarkiverse.modulith.apt;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * A Java source held in memory.
 */
final class StringSource extends SimpleJavaFileObject {

    private final String content;

    StringSource(String path, String content) {
        super(URI.create("string:///" + path), Kind.SOURCE);
        this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
