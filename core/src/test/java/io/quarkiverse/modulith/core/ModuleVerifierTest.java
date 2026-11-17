package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.fixtures.rules.RulesApplication;

/**
 * The rules on the {@code fixtures.rules} application, which breaks each of them once.
 */
class ModuleVerifierTest {

    static final String R = "io.quarkiverse.modulith.core.fixtures.rules";

    @AfterEach
    void clearProperties() {
        System.clearProperty("quarkus.modulith.verification.allow-field-injection");
    }

    @Test
    void reportsEveryBrokenRuleWithSpringModulithsMessages() {
        // arrange
        System.setProperty("quarkus.modulith.verification.allow-field-injection", "false");
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);

        // act
        List<String> violations = modules.detectViolations().getViolations().stream()
                .map(it -> it.kind() + " | " + it.module() + " | " + it.source() + " | " + it.target() + " | "
                        + it.message())
                .toList();

        // assert
        assertThat(violations).containsExactlyInAnyOrder(
                "INVALID_DECLARATION | nowhere |  | nowhere | @Modulith declares the shared module 'nowhere', which does not exist",
                "INVALID_DECLARATION | bad::id | " + R + ".bad |  | Module identifier 'bad::id' must not contain '::'",
                "INVALID_DECLARATION | bad::id | " + R
                        + ".bad | ghost | Invalid explicit module dependency in bad::id! No module found with name 'ghost'.",
                "INVALID_DECLARATION | bad::id | " + R
                        + ".bad | store::nope | No named interface named 'nope' found! Original dependency declaration: bad::id -> store::nope.",
                "CYCLE | outer | outer -> outer.inner -> outer |  | Cycle detected: outer -> outer.inner -> outer\n  1. Dependencies of outer\n    - Field <"
                        + R + ".outer.Outer.inner> has type <" + R
                        + ".outer.inner.Inner> in (Outer.java:0)\n  2. Dependencies of outer.inner\n    - Field <" + R
                        + ".outer.inner.Inner.internal> has type <" + R + ".outer.internal.OuterInternal> in (Inner.java:0)",
                "CYCLE | outer | outer -> outer.inner -> outer.inner.deep -> outer |  | Cycle detected: outer -> outer.inner -> outer.inner.deep -> outer\n  1. Dependencies of outer\n    - Field <"
                        + R + ".outer.Outer.inner> has type <" + R
                        + ".outer.inner.Inner> in (Outer.java:0)\n  2. Dependencies of outer.inner\n    - Field <" + R
                        + ".outer.inner.Inner.deep> has type <" + R
                        + ".outer.inner.deep.Deep> in (Inner.java:0)\n  3. Dependencies of outer.inner.deep\n    - Field <" + R
                        + ".outer.inner.deep.Deep.internal> has type <" + R + ".outer.internal.OuterInternal> in (Deep.java:0)",
                "CYCLE | outer.inner | outer.inner -> outer.inner.deep -> outer.sibling -> outer.inner |  | Cycle detected: outer.inner -> outer.inner.deep -> outer.sibling -> outer.inner\n  1. Dependencies of outer.inner\n    - Field <"
                        + R + ".outer.inner.Inner.deep> has type <" + R
                        + ".outer.inner.deep.Deep> in (Inner.java:0)\n  2. Dependencies of outer.inner.deep\n    - Field <" + R
                        + ".outer.inner.deep.Deep.sibling> has type <" + R
                        + ".outer.sibling.Sibling> in (Deep.java:0)\n  3. Dependencies of outer.sibling\n    - Field <" + R
                        + ".outer.sibling.Sibling.inner> has type <" + R + ".outer.inner.Inner> in (Sibling.java:0)",
                "NESTED_MODULE_ACCESS | client | " + R + ".client.Client | " + R
                        + ".outer.inner.Inner | Invalid sub-module reference from module 'client' to module 'outer.inner' (via i.q.m.c.f.r.c.Client -> i.q.m.c.f.r.o.i.Inner)!",
                "NON_EXPOSED_TYPE | everything | " + R + ".everything.Everything | " + R
                        + ".store.internal.Secret | Module 'everything' depends on non-exposed type " + R
                        + ".store.internal.Secret within module 'store'!\nField <" + R
                        + ".everything.Everything.secret> has type <" + R + ".store.internal.Secret> in (Everything.java:0)",
                "FIELD_INJECTION | injector | " + R + ".injector.Injector.store | " + R
                        + ".store.Store | Module Injector uses field injection in " + R
                        + ".injector.Injector.store. Prefer constructor injection instead!",
                "DEPENDENCY_NOT_ALLOWED | kernel | " + R + ".kernel.Kernel | " + R
                        + ".store.Store | Module 'kernel' depends on module 'store' via " + R + ".kernel.Kernel -> " + R
                        + ".store.Store. Allowed targets: none.",
                "NON_EXPOSED_TYPE | kerneluser | " + R + ".kerneluser.KernelUser | " + R
                        + ".kernel.internal.Hidden | Module 'kerneluser' depends on non-exposed type " + R
                        + ".kernel.internal.Hidden within module 'kernel'!\nField <" + R
                        + ".kerneluser.KernelUser.hidden> has type <" + R + ".kernel.internal.Hidden> in (KernelUser.java:0)",
                "NESTED_MODULE_ACCESS | outer.inner.deep | " + R + ".outer.inner.deep.Deep | " + R
                        + ".outer.sibling.Sibling | Invalid sub-module reference from module 'outer.inner.deep' to module 'outer.sibling' (via i.q.m.c.f.r.o.i.d.Deep -> i.q.m.c.f.r.o.s.Sibling)!",
                "DEPENDENCY_NOT_ALLOWED | wild | " + R + ".wild.Wild | " + R
                        + ".store.Store | Module 'wild' depends on module 'store' via " + R + ".wild.Wild -> " + R
                        + ".store.Store. Allowed targets: store :: *, kernel.");
    }

    @Test
    void allowsFieldInjectionByDefault() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);

        // act
        Violations violations = modules.detectViolations();

        // assert
        assertThat(violations.ofKind(Violation.Kind.FIELD_INJECTION)).isEmpty();
        assertThat(violations.getViolations()).hasSize(13);
    }

    @Test
    void verifiesSingleModules() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);
        ApplicationModule kernel = modules.getModuleByName("kernel").orElseThrow();

        // act
        Violations violations = kernel.detectDependencies(modules);

        // assert
        assertThat(violations.getMessages()).containsExactly("Module 'kernel' depends on module 'store' via " + R
                + ".kernel.Kernel -> " + R + ".store.Store. Allowed targets: none.");
    }

    @Test
    void failsTheDependencyVerificationOfASingleModule() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);
        ApplicationModule kernel = modules.getModuleByName("kernel").orElseThrow();

        // act & assert
        assertThatThrownBy(() -> kernel.verifyDependencies(modules)).isInstanceOf(Violations.class)
                .hasMessage("- Module 'kernel' depends on module 'store' via " + R + ".kernel.Kernel -> " + R
                        + ".store.Store. Allowed targets: none.");
    }

    @Test
    void failsEveryVerificationUntilTheRulesHold() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);
        Violations first = modules.detectViolations();

        // act & assert
        assertThatThrownBy(modules::verify).isInstanceOf(Violations.class).hasMessage(first.getMessage());
        assertThatThrownBy(modules::verify).isInstanceOf(Violations.class).hasMessage(first.getMessage());
    }

    @Test
    void rejectsAllowedDependenciesOnUnknownModules() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);
        ApplicationModule bad = modules.getModuleByName("bad::id").orElseThrow();

        // act & assert
        assertThatThrownBy(() -> bad.getAllowedDependencies(modules))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid explicit module dependency in bad::id! No module found with name 'ghost'.");
    }
}
