package io.quarkiverse.modulith.observability.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.observability.ModuleInvocation;

/**
 * The observed methods of the application, computed at build time: bean class and method signature to the module
 * invocation they stand for.
 */
public final class ObservedModules {

    /**
     * Separates the fields of an encoded row.
     */
    public static final String SEPARATOR = "\u0000";

    private static final String ANY_METHOD = "*";

    private final Map<String, ModuleInvocation> invocations;
    private final Map<CallSite, Optional<ModuleInvocation>> cache = new ConcurrentHashMap<>();

    ObservedModules(Map<String, ModuleInvocation> invocations) {
        this.invocations = Map.copyOf(invocations);
    }

    /**
     * @param rows {@link #row(String, String, ModuleInvocation) encoded} observed methods
     */
    public static ObservedModules of(String[] rows) {
        Map<String, ModuleInvocation> invocations = new HashMap<>();
        for (String row : rows) {
            String[] fields = row.split(SEPARATOR, -1);
            invocations.put(fields[0], new ModuleInvocation(fields[1], fields[2], fields[3], fields[4], fields[5]));
        }
        return new ObservedModules(invocations);
    }

    /**
     * @param beanClass the binary name of the bean class
     * @param signature the method name and parameter type names, {@code complete(java.lang.String)}
     * @return the row that {@link #of(String[])} reads
     */
    public static String row(String beanClass, String signature, ModuleInvocation invocation) {
        return String.join(SEPARATOR, key(beanClass, signature), invocation.moduleIdentifier(),
                invocation.moduleName(), invocation.method(), invocation.invocationType(),
                invocation.contextualName());
    }

    /**
     * @param resource the binary name of a REST Data with Panache resource interface, whose implementation Quarkus
     *        generates
     * @param invocation the module and the contextual name prefix, {@code <module name> > <abbreviated interface>}
     * @return the row that matches every method of the generated implementation
     */
    public static String resourceRow(String resource, ModuleInvocation invocation) {
        return row(resource, ANY_METHOD, invocation);
    }

    /**
     * @param type the class of the target instance, a subclass of the bean class when the bean is intercepted
     * @return the module invocation a call of the method on the type stands for, if it is observed
     */
    public Optional<ModuleInvocation> lookup(Class<?> type, Method method) {
        return cache.computeIfAbsent(new CallSite(type, method), this::resolve);
    }

    /**
     * @param beanClass the binary name of the bean class
     * @param signature the method name and parameter type names, {@code complete(java.lang.String)}
     * @return the module invocation of the method declared or inherited by exactly that class, if it is observed
     */
    public Optional<ModuleInvocation> find(String beanClass, String signature) {
        return Optional.ofNullable(invocations.get(key(beanClass, signature)));
    }

    private Optional<ModuleInvocation> resolve(CallSite site) {
        String signature = signature(site.method());
        for (Class<?> type = site.type(); Objects.nonNull(type); type = type.getSuperclass()) {
            Optional<ModuleInvocation> invocation = find(type.getName(), signature);
            if (invocation.isPresent()) {
                return invocation;
            }
            for (Class<?> resource : type.getInterfaces()) {
                ModuleInvocation any = invocations.get(key(resource.getName(), ANY_METHOD));
                if (Objects.nonNull(any)) {
                    return Optional.of(new ModuleInvocation(any.moduleIdentifier(), any.moduleName(),
                            site.method().getName(), any.invocationType(), any.contextualName() + "."
                                    + site.method().getName() + parameters(site.method())));
                }
            }
        }
        return Optional.empty();
    }

    static String signature(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes()).map(ObservedModules::typeName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    /**
     * @return the parameter types abbreviated like Spring Modulith renders non-module types: {@code (j.l.String)}
     */
    static String parameters(Method method) {
        return Arrays.stream(method.getParameterTypes()).map(ObservedModules::abbreviate)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String abbreviate(Class<?> type) {
        if (type.isArray()) {
            return abbreviate(type.getComponentType()) + "[]";
        }
        String name = type.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        }
        return Arrays.stream(name.substring(0, dot).split("\\.")).map(it -> it.substring(0, 1))
                .collect(Collectors.joining(".")) + "." + name.substring(dot + 1).replace('$', '.');
    }

    private static String typeName(Class<?> type) {
        return type.isArray() ? typeName(type.getComponentType()) + "[]" : type.getName();
    }

    private static String key(String beanClass, String signature) {
        return beanClass + "#" + signature;
    }
}
