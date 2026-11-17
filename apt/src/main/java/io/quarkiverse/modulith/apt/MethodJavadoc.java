package io.quarkiverse.modulith.apt;

/**
 * The Javadoc of a method.
 *
 * @param name the method name
 * @param signature the name and the binary names of the erased parameter types: {@code complete(com.acme.Order, int)}
 * @param comment the Javadoc text before the block tags
 */
record MethodJavadoc(String name, String signature, String comment) {
}
