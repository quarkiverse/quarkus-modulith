package io.quarkiverse.modulith.apt;

import java.util.List;

/**
 * The Javadoc of a type or package.
 *
 * @param name the binary type name or the package name
 * @param comment the Javadoc text before the block tags, {@code null} when there is none
 * @param methods the documented methods
 */
record TypeJavadoc(String name, String comment, List<MethodJavadoc> methods) {
}
