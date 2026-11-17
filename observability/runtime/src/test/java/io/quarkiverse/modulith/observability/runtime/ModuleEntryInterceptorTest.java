package io.quarkiverse.modulith.observability.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.observability.ModuleInvocation;

class ModuleEntryInterceptorTest {

    static final ModuleInvocation CALL = new ModuleInvocation("order", "Orders", "call", "bean",
            "Orders > Target.call(j.l.String[])");

    @Test
    void observesCallsFromOutsideTheModuleOnly() throws Exception {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(observer);
        Method call = Target.class.getMethod("call", String[].class);

        // act
        Object outer = interceptor.observe(new FakeInvocationContext(new TargetSubclass(), call,
                () -> interceptor.observe(new FakeInvocationContext(new TargetSubclass(), call, () -> "inner"))));

        // assert
        assertThat(outer).isEqualTo("inner");
        assertThat(observer.events).containsExactly("start call in order", "stop");
        assertThat(CurrentModule.get()).isNull();
    }

    @Test
    void recordsErrorsAndRestoresTheCallingModule() {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(observer);
        String previous = CurrentModule.enter("inventory");

        // act
        Throwable thrown = catchThrowable(() -> interceptor.observe(new FakeInvocationContext(new Target(),
                Target.class.getMethod("call", String[].class), () -> {
                    throw new IllegalStateException("broken");
                })));
        String current = CurrentModule.get();
        CurrentModule.restore(previous);

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("broken");
        assertThat(observer.events).containsExactly("start call in order", "error broken", "stop");
        assertThat(current).isEqualTo("inventory");
    }

    @Test
    void callsUnobservedAndLeavesNoModuleBehindWhenAnObserverFailsToStart() throws Exception {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(observer, new FailingObserver(true));
        Method call = Target.class.getMethod("call", String[].class);

        // act
        Object first = interceptor.observe(new FakeInvocationContext(new Target(), call, CurrentModule::get));
        Object second = interceptor.observe(new FakeInvocationContext(new Target(), call, () -> "second"));

        // assert
        assertThat(first).isEqualTo("order");
        assertThat(second).isEqualTo("second");
        assertThat(observer.events).containsExactly("start call in order", "stop", "start call in order", "stop");
        assertThat(CurrentModule.get()).isNull();
    }

    @Test
    void returnsTheResultWhenAnObservationFailsToStop() throws Exception {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(new FailingObserver(false), observer);

        // act
        Object result = interceptor.observe(new FakeInvocationContext(new Target(),
                Target.class.getMethod("call", String[].class), () -> "result"));

        // assert
        assertThat(result).isEqualTo("result");
        assertThat(observer.events).containsExactly("start call in order", "stop");
        assertThat(CurrentModule.get()).isNull();
    }

    @Test
    void rethrowsTheFailureOfTheCallWhenAnObservationFailsToRecordIt() {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(new FailingObserver(false), observer);

        // act
        Throwable thrown = catchThrowable(() -> interceptor.observe(new FakeInvocationContext(new Target(),
                Target.class.getMethod("call", String[].class), () -> {
                    throw new AssertionError("broken");
                })));

        // assert
        assertThat(thrown).isExactlyInstanceOf(AssertionError.class).hasMessage("broken");
        assertThat(observer.events).containsExactly("start call in order", "error broken", "stop");
        assertThat(CurrentModule.get()).isNull();
    }

    @Test
    void proceedsWithoutObservingUnknownMethods() throws Exception {
        // arrange
        RecordingObserver observer = new RecordingObserver();
        ModuleEntryInterceptor interceptor = interceptor(observer);

        // act
        Object result = interceptor.observe(new FakeInvocationContext(new Target(), Target.class.getMethod("other"),
                () -> "other"));

        // assert
        assertThat(result).isEqualTo("other");
        assertThat(observer.events).isEmpty();
    }

    @Test
    void findsMethodsThroughTheBeanClassAndCachesThem() throws Exception {
        // arrange
        ObservedModules modules = ObservedModules.of(new String[] { ObservedModules.row(Target.class.getName(),
                "call(java.lang.String[])", CALL) });
        Method call = Target.class.getMethod("call", String[].class);

        // act
        Optional<ModuleInvocation> fromSubclass = modules.lookup(TargetSubclass.class, call);
        Optional<ModuleInvocation> again = modules.lookup(TargetSubclass.class, call);
        Optional<ModuleInvocation> missing = modules.lookup(Target.class, Target.class.getMethod("other"));

        // assert
        assertThat(fromSubclass).contains(CALL);
        assertThat(again).containsSame(fromSubclass.orElseThrow());
        assertThat(missing).isEmpty();
        assertThat(ObservedModules.signature(call)).isEqualTo("call(java.lang.String[])");
    }

    @Test
    void matchesEveryMethodOfAResourceImplementation() throws Exception {
        // arrange
        ObservedModules modules = ObservedModules.of(new String[] { ObservedModules.resourceRow(Resource.class.getName(),
                new ModuleInvocation("order", "Orders", "", "bean", "Orders > i.q.m.o.r.Resource")) });

        // act
        Optional<ModuleInvocation> list = modules.lookup(ResourceImplementation.class,
                ResourceImplementation.class.getMethod("list", int.class, String[].class, Nested.class));
        Optional<ModuleInvocation> plain = modules.lookup(ResourceImplementation.class,
                ResourceImplementation.class.getMethod("plain"));

        // assert
        assertThat(list).contains(new ModuleInvocation("order", "Orders", "list", "bean",
                "Orders > i.q.m.o.r.Resource.list(int, j.l.String[], i.q.m.o.r.Nested)"));
        assertThat(plain.orElseThrow().contextualName()).isEqualTo("Orders > i.q.m.o.r.Resource.plain()");
        assertThat(modules.lookup(Target.class, Target.class.getMethod("other"))).isEmpty();
    }

    static ModuleEntryInterceptor interceptor(InvocationObserver... observers) {
        ModuleEntryInterceptor interceptor = new ModuleEntryInterceptor();
        interceptor.modules = ObservedModules.of(new String[] { ObservedModules.row(Target.class.getName(),
                "call(java.lang.String[])", CALL) });
        interceptor.observers = List.of(observers);
        return interceptor;
    }
}
