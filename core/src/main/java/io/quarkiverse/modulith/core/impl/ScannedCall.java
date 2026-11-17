package io.quarkiverse.modulith.core.impl;

/**
 * A method or constructor call made by a scanned class.
 *
 * @param originMethod the name of the calling method, the enclosing one for calls in lambdas
 * @param originHasParameters whether the calling method declares parameters
 * @param owner the binary name of the called method's owner
 * @param name the called method's name, {@code <init>} for constructors
 * @param descriptor the called method's descriptor
 */
record ScannedCall(String originMethod, boolean originHasParameters, String owner, String name, String descriptor) {
}
