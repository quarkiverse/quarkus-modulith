package io.quarkiverse.modulith.deployment.events;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkiverse.modulith.runtime.events.ConditionSupport;
import io.quarkiverse.modulith.runtime.events.ConditionalBean;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Generates a {@link Predicate} class for a listener: it evaluates the parsed {@code condition} on the event with
 * direct method calls, and asks the listener bean when it implements
 * {@link io.quarkiverse.modulith.events.ConditionalEventListener}. Property names are resolved against the Jandex
 * index at build time, so a typo fails the build.
 */
public final class ConditionCompiler {

    private static final Map<String, String> PRIMITIVE_DESCRIPTORS = Map.of("boolean", "Z", "byte", "B", "char", "C",
            "short", "S", "int", "I", "long", "J", "float", "F", "double", "D");
    private static final MethodDesc TRUTHY = MethodDesc.of(ConditionSupport.class, "truthy", boolean.class,
            Object.class);
    private static final MethodDesc NUMBER = MethodDesc.of(ConditionSupport.class, "number", Object.class,
            String.class);
    private static final MethodDesc EQUAL = MethodDesc.of(ConditionSupport.class, "equal", boolean.class,
            Object.class, Object.class);
    private static final MethodDesc COMPARE = MethodDesc.of(ConditionSupport.class, "compare", boolean.class,
            Object.class, Object.class, String.class);
    private static final MethodDesc SUPPORTS = MethodDesc.of(ConditionalBean.class, "supports", boolean.class,
            Object.class);
    private static final ConstructorDesc CONDITIONAL_BEAN = ConstructorDesc.of(ConditionalBean.class, Class.class);

    private final IndexView index;

    public ConditionCompiler(IndexView index) {
        this.index = index;
    }

    /**
     * Checks the condition against the event type without generating anything.
     *
     * @throws IllegalArgumentException if a property cannot be resolved or a literal is invalid
     */
    public void validate(ConditionNode condition, Type eventType) {
        switch (condition) {
            case LiteralNode literal -> {
                if (literal.kind() == LiteralKind.NUMBER) {
                    number(literal.text());
                }
            }
            case PathNode path -> resolve(path, eventType);
            case CompareNode compare -> {
                validate(compare.left(), eventType);
                validate(compare.right(), eventType);
            }
            case AndNode and -> {
                validate(and.left(), eventType);
                validate(and.right(), eventType);
            }
            case OrNode or -> {
                validate(or.left(), eventType);
                validate(or.right(), eventType);
            }
            case NotNode not -> validate(not.operand(), eventType);
        }
    }

    /**
     * Writes the predicate class.
     *
     * @param condition the parsed condition, empty when only the bean decides
     * @param conditionalBean the listener bean class when it implements {@code ConditionalEventListener}
     */
    public void generate(ClassOutput output, String className, Optional<ConditionNode> condition, Type eventType,
            Optional<DotName> conditionalBean) {
        ClassDesc eventDesc = desc(eventType);
        Gizmo.create(output).class_(className, cc -> {
            cc.implements_(Predicate.class);
            Optional<FieldDesc> beanField = conditionalBean.map(bean -> cc.field("bean", field -> {
                field.setType(ConditionalBean.class);
                field.setInitializer(init -> init.yield(init.new_(CONDITIONAL_BEAN,
                        Const.of(ClassDesc.of(bean.toString())))));
            }));
            Map<String, StaticFieldVar> numbers = new LinkedHashMap<>();
            condition.ifPresent(node -> numbers(node).forEach(text -> numbers.computeIfAbsent(text,
                    key -> cc.staticField("number" + numbers.size(), field -> {
                        field.setType(Object.class);
                        field.private_();
                        field.final_();
                        field.setInitializer(init -> init.yield(init.invokeStatic(NUMBER, Const.of(key))));
                    }))));
            cc.defaultConstructor();
            cc.method("test", mc -> {
                mc.returning(boolean.class);
                ParamVar parameter = mc.parameter("event", Object.class);
                mc.body(b -> {
                    Expr event = b.localVar("typed", eventDesc, b.cast(parameter, eventDesc));
                    beanField.ifPresent(field -> b.if_(
                            b.logicalNot(b.invokeVirtual(SUPPORTS, b.get(mc.this_().field(field)), parameter)),
                            BlockCreator::returnFalse));
                    if (condition.isPresent()) {
                        b.return_(b.invokeStatic(TRUTHY, value(b, condition.get(), event, eventType, numbers)));
                    } else {
                        b.returnTrue();
                    }
                });
            });
        });
    }

    /**
     * @return the text of every number literal, parsed once when the predicate class is initialized
     */
    private static List<String> numbers(ConditionNode node) {
        return switch (node) {
            case LiteralNode literal -> literal.kind() == LiteralKind.NUMBER ? List.of(literal.text()) : List.of();
            case PathNode path -> List.of();
            case CompareNode compare -> concat(numbers(compare.left()), numbers(compare.right()));
            case AndNode and -> concat(numbers(and.left()), numbers(and.right()));
            case OrNode or -> concat(numbers(or.left()), numbers(or.right()));
            case NotNode not -> numbers(not.operand());
        };
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private Expr value(BlockCreator b, ConditionNode node, Expr event, Type eventType,
            Map<String, StaticFieldVar> numbers) {
        return switch (node) {
            case LiteralNode literal -> literal(b, literal, numbers);
            case PathNode path -> path(b, path, event, eventType);
            case CompareNode compare -> compare(b, compare, event, eventType, numbers);
            case AndNode and -> b.box(b.logicalAnd(
                    b.invokeStatic(TRUTHY, value(b, and.left(), event, eventType, numbers)),
                    right -> right.yield(right.invokeStatic(TRUTHY,
                            value(right, and.right(), event, eventType, numbers)))));
            case OrNode or -> b.box(b.logicalOr(
                    b.invokeStatic(TRUTHY, value(b, or.left(), event, eventType, numbers)),
                    right -> right.yield(right.invokeStatic(TRUTHY,
                            value(right, or.right(), event, eventType, numbers)))));
            case NotNode not -> b.box(b.logicalNot(b.invokeStatic(TRUTHY,
                    value(b, not.operand(), event, eventType, numbers))));
        };
    }

    private static Expr literal(BlockCreator b, LiteralNode literal, Map<String, StaticFieldVar> numbers) {
        return switch (literal.kind()) {
            case NUMBER -> b.get(numbers.get(literal.text()));
            case STRING -> Const.of(literal.text());
            case BOOLEAN -> b.box(Const.of(Boolean.parseBoolean(literal.text())));
            case NULL -> Const.ofNull(Object.class);
        };
    }

    private Expr compare(BlockCreator b, CompareNode compare, Expr event, Type eventType,
            Map<String, StaticFieldVar> numbers) {
        Expr left = value(b, compare.left(), event, eventType, numbers);
        Expr right = value(b, compare.right(), event, eventType, numbers);
        Expr result = switch (compare.operator()) {
            case "==" -> b.invokeStatic(EQUAL, left, right);
            case "!=" -> b.logicalNot(b.invokeStatic(EQUAL, left, right));
            default -> b.invokeStatic(COMPARE, left, right, Const.of(compare.operator()));
        };
        return b.box(result);
    }

    private Expr path(BlockCreator b, PathNode path, Expr event, Type eventType) {
        Expr current = event;
        Type type = eventType;
        for (MethodInfo accessor : resolve(path, eventType)) {
            ClassDesc owner = desc(type);
            MethodTypeDesc signature = MethodTypeDesc.of(desc(accessor.returnType()));
            current = isInterface(type)
                    ? b.invokeInterface(InterfaceMethodDesc.of(owner, accessor.name(), signature), current)
                    : b.invokeVirtual(ClassMethodDesc.of(owner, accessor.name(), signature), current);
            type = accessor.returnType();
        }
        return type.kind() == Type.Kind.PRIMITIVE ? b.box(current) : current;
    }

    /**
     * @return the accessor of each segment, in order
     */
    List<MethodInfo> resolve(PathNode path, Type eventType) {
        Type type = eventType;
        List<MethodInfo> accessors = new ArrayList<>();
        for (String segment : path.segments()) {
            MethodInfo accessor = accessor(type, segment);
            accessors.add(accessor);
            type = accessor.returnType();
        }
        return accessors;
    }

    private MethodInfo accessor(Type type, String segment) {
        ClassInfo clazz = type.kind() == Type.Kind.PRIMITIVE || type.kind() == Type.Kind.ARRAY ? null
                : index.getClassByName(type.name());
        if (Objects.isNull(clazz)) {
            throw new IllegalArgumentException("Cannot read '" + segment + "' of " + type.name()
                    + ": the type is not a class of the application index");
        }
        List<String> candidates = segment.endsWith("()") ? List.of(segment.substring(0, segment.length() - 2))
                : List.of("get" + capitalize(segment), "is" + capitalize(segment), segment);
        for (String candidate : candidates) {
            Optional<MethodInfo> found = findMethod(clazz, candidate);
            if (found.isPresent()) {
                return found.get();
            }
        }
        throw new IllegalArgumentException("No public method " + String.join("(), ", candidates) + "() without"
                + " parameters on " + clazz.name() + " for '" + segment + "'");
    }

    private Optional<MethodInfo> findMethod(ClassInfo start, String name) {
        Deque<ClassInfo> queue = new ArrayDeque<>();
        Set<DotName> seen = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            ClassInfo clazz = queue.poll();
            if (!seen.add(clazz.name())) {
                continue;
            }
            MethodInfo method = clazz.method(name);
            if (Objects.nonNull(method) && Modifier.isPublic(method.flags()) && !Modifier.isStatic(method.flags())
                    && method.returnType().kind() != Type.Kind.VOID) {
                return Optional.of(method);
            }
            Optional.ofNullable(clazz.superName()).map(index::getClassByName).ifPresent(queue::add);
            clazz.interfaceNames().stream().map(index::getClassByName).filter(Objects::nonNull).forEach(queue::add);
        }
        return Optional.empty();
    }

    private boolean isInterface(Type type) {
        return Modifier.isInterface(index.getClassByName(type.name()).flags());
    }

    static void number(String text) {
        try {
            new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number '" + text + "'", e);
        }
    }

    static ClassDesc desc(Type type) {
        return switch (type.kind()) {
            case PRIMITIVE -> ClassDesc.ofDescriptor(PRIMITIVE_DESCRIPTORS.get(type.name().toString()));
            case ARRAY -> ClassDesc.ofDescriptor(type.name().toString().replace('.', '/'));
            default -> ClassDesc.of(type.name().toString());
        };
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
