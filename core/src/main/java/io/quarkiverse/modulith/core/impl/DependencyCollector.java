package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

/**
 * Collects the dependencies of one class with ArchUnit's descriptions. The source file name is only known once the
 * class has been read, so the locations are added at the end.
 */
final class DependencyCollector {

    private final String self;
    private final List<String> targets = new ArrayList<>();
    private final List<String> descriptions = new ArrayList<>();
    private final List<Integer> lines = new ArrayList<>();

    DependencyCollector(String self) {
        this.self = self;
    }

    /**
     * Records a dependency on the class of the given type, on the element class for arrays.
     *
     * @param origin the description of the origin, {@code Method <com.acme.A.run()>}
     * @param verb the kind of dependency, {@code calls method}
     * @param target the referenced type
     * @param targetDescription how the target is shown, {@code <com.acme.B>} or {@code method <com.acme.B.go()>}
     */
    void add(String origin, String verb, Type target, String targetDescription, int line) {
        if (target.getSort() == Type.ARRAY) {
            TypeNames.classOf(target).forEach(it -> addClass(origin + " depends on component type <" + it + ">", it, line));
        } else {
            TypeNames.classOf(target).forEach(it -> addClass(origin + " " + verb + " " + targetDescription, it, line));
        }
    }

    /**
     * Records a dependency on a type, rendered as {@code <name>}.
     */
    void add(String origin, String verb, Type target, int line) {
        add(origin, verb, target, "<" + TypeNames.name(target) + ">", line);
    }

    /**
     * Records a dependency on a class given by its binary name.
     */
    void addClass(String description, String target, int line) {
        if (!target.equals(self) && !TypeNames.isCoreType(target)) {
            targets.add(target);
            descriptions.add(description);
            lines.add(line);
        }
    }

    List<RawDependency> finish(String sourceFile) {
        List<RawDependency> result = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            result.add(new RawDependency(targets.get(i),
                    descriptions.get(i) + " in (" + sourceFile + ":" + lines.get(i) + ")"));
        }
        return result;
    }
}
