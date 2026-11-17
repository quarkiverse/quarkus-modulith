package io.quarkiverse.modulith.junit;

import java.lang.StackWalker.StackFrame;

/**
 * A stack frame of a given class, for the IDE detection tests.
 */
record NamedStackFrame(String getClassName) implements StackFrame {

    @Override
    public String getMethodName() {
        return "main";
    }

    @Override
    public Class<?> getDeclaringClass() {
        return Object.class;
    }

    @Override
    public int getByteCodeIndex() {
        return 0;
    }

    @Override
    public String getFileName() {
        return "Main.java";
    }

    @Override
    public int getLineNumber() {
        return 1;
    }

    @Override
    public boolean isNativeMethod() {
        return false;
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        return new StackTraceElement(getClassName, getMethodName(), getFileName(), getLineNumber());
    }
}
