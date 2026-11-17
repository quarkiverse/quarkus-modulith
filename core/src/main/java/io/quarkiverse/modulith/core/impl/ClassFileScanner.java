package io.quarkiverse.modulith.core.impl;

import java.util.List;

import org.objectweb.asm.ClassReader;

/**
 * Reads class files without building a tree: one visitor pass, stack map frames skipped.
 */
final class ClassFileScanner {

    private ClassFileScanner() {
    }

    static ScannedClass scan(byte[] classFile) {
        ScanningClassVisitor visitor = new ScanningClassVisitor();
        new ClassReader(classFile).accept(visitor, ClassReader.SKIP_FRAMES);
        return visitor.result();
    }

    /**
     * Reads only the class header, without code, debug information and frames.
     *
     * @return the names of the annotation types on the class
     */
    static List<String> annotations(byte[] classFile) {
        ClassAnnotationsVisitor visitor = new ClassAnnotationsVisitor();
        new ClassReader(classFile).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return visitor.annotations();
    }
}
