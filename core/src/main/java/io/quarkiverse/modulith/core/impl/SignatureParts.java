package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * The parts of a class or method signature: type parameters with their bounds, supertypes, parameters and return
 * type.
 */
final class SignatureParts extends SignatureVisitor {

    private final Map<String, List<GenericType>> typeParameters = new LinkedHashMap<>();
    private final List<GenericType> interfaces = new ArrayList<>();
    private final List<GenericType> parameters = new ArrayList<>();
    private String currentTypeParameter = "";
    private GenericType superclass;
    private GenericType returnType;

    private SignatureParts() {
        super(Opcodes.ASM9);
    }

    /**
     * @param signature a class or method signature, may be {@code null}
     */
    static SignatureParts of(String signature) {
        SignatureParts parts = new SignatureParts();
        Optional.ofNullable(signature).ifPresent(it -> new SignatureReader(it).accept(parts));
        return parts;
    }

    /**
     * @param signature a field signature, may be {@code null}
     */
    static Optional<GenericType> ofType(String signature) {
        List<GenericType> result = new ArrayList<>(1);
        Optional.ofNullable(signature)
                .ifPresent(it -> new SignatureReader(it).acceptType(new GenericTypeReader(' ', result::add)));
        return result.stream().findFirst();
    }

    /**
     * @return the type parameters with their bounds, in declaration order
     */
    Map<String, List<GenericType>> typeParameters() {
        return Collections.unmodifiableMap(typeParameters);
    }

    List<GenericType> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    List<GenericType> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    Optional<GenericType> superclass() {
        return Optional.ofNullable(superclass);
    }

    Optional<GenericType> returnType() {
        return Optional.ofNullable(returnType);
    }

    @Override
    public void visitFormalTypeParameter(String name) {
        currentTypeParameter = name;
        typeParameters.put(name, new ArrayList<>());
    }

    @Override
    public SignatureVisitor visitClassBound() {
        return bound();
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        return bound();
    }

    private SignatureVisitor bound() {
        return new GenericTypeReader(' ', typeParameters.get(currentTypeParameter)::add);
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        return new GenericTypeReader(' ', it -> superclass = it);
    }

    @Override
    public SignatureVisitor visitInterface() {
        return new GenericTypeReader(' ', interfaces::add);
    }

    @Override
    public SignatureVisitor visitParameterType() {
        return new GenericTypeReader(' ', parameters::add);
    }

    @Override
    public SignatureVisitor visitReturnType() {
        return new GenericTypeReader(' ', it -> returnType = it);
    }
}
