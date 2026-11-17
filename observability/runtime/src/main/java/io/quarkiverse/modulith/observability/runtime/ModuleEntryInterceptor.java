package io.quarkiverse.modulith.observability.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkus.arc.All;

/**
 * Observes calls into a module from outside it, Spring Modulith's {@code ModuleEntryInterceptor}: calls between beans
 * of the same module are not observed again. A failing observer is logged and skipped, it never fails the call and
 * never leaves the module or an observation scope behind on the thread.
 */
@ModuleObserved
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class ModuleEntryInterceptor {

    private static final FailureLog FAILURES = new FailureLog(ModuleEntryInterceptor.class);

    @Inject
    ObservedModules modules;

    @Inject
    @All
    List<InvocationObserver> observers;

    @AroundInvoke
    Object observe(InvocationContext context) throws Exception {
        Optional<ModuleInvocation> invocation = modules.lookup(context.getTarget().getClass(), context.getMethod());
        if (invocation.isEmpty() || invocation.get().moduleIdentifier().equals(CurrentModule.get())) {
            return context.proceed();
        }
        return observe(context, invocation.get());
    }

    private Object observe(InvocationContext context, ModuleInvocation invocation) throws Exception {
        String previous = CurrentModule.get();
        List<ObservationScope> scopes = new ArrayList<>(observers.size());
        try {
            CurrentModule.enter(invocation.moduleIdentifier());
            observers.forEach(observer -> start(observer, invocation).ifPresent(scopes::add));
            return context.proceed();
        } catch (Throwable e) {
            scopes.forEach(scope -> guarded(scope, "record the error of", () -> scope.error(e)));
            throw e;
        } finally {
            scopes.reversed().forEach(scope -> guarded(scope, "stop", scope::stop));
            CurrentModule.restore(previous);
        }
    }

    private static Optional<ObservationScope> start(InvocationObserver observer, ModuleInvocation invocation) {
        try {
            return Optional.of(observer.start(invocation));
        } catch (RuntimeException e) {
            FAILURES.failed(observer.getClass().getName(),
                    "Cannot observe " + invocation.contextualName() + ", calling it unobserved", e);
            return Optional.empty();
        }
    }

    private static void guarded(ObservationScope scope, String action, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            FAILURES.failed(scope.getClass().getName(), "Cannot " + action + " an observation", e);
        }
    }
}
