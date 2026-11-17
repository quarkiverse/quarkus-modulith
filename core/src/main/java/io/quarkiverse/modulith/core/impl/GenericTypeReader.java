package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Builds a {@link GenericType} from the events of a {@link SignatureVisitor} for one type signature.
 */
final class GenericTypeReader extends SignatureVisitor {

    private final Consumer<GenericType> result;
    private final char wildcard;
    private final List<GenericType> arguments = new ArrayList<>();
    private String name = "";
    private boolean variable;
    private int dimensions;

    GenericTypeReader(char wildcard, Consumer<GenericType> result) {
        super(Opcodes.ASM9);
        this.wildcard = wildcard;
        this.result = result;
    }

    @Override
    public void visitBaseType(char descriptor) {
        name = Type.getType(String.valueOf(descriptor)).getClassName();
        finish();
    }

    @Override
    public void visitTypeVariable(String typeVariable) {
        name = typeVariable;
        variable = true;
        finish();
    }

    @Override
    public SignatureVisitor visitArrayType() {
        dimensions++;
        return this;
    }

    @Override
    public void visitClassType(String internalName) {
        name = internalName.replace('/', '.');
    }

    @Override
    public void visitInnerClassType(String innerName) {
        name = name + "$" + innerName;
        arguments.clear();
    }

    @Override
    public void visitTypeArgument() {
        arguments.add(new GenericType("?", false, '*', 0, List.of()));
    }

    @Override
    public SignatureVisitor visitTypeArgument(char kind) {
        return new GenericTypeReader(kind == '=' ? ' ' : kind, arguments::add);
    }

    @Override
    public void visitEnd() {
        finish();
    }

    private void finish() {
        result.accept(new GenericType(name, variable, wildcard, dimensions, List.copyOf(arguments)));
    }
}
