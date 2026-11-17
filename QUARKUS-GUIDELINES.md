# Quarkus extension guidelines for Quarkus Modulith

This is the rulebook every change to this repository is reviewed against. Each rule has an ID, the rule, the reason, a check you can run, and the source it comes from. Reviews cite these IDs.

Target platform: Quarkus 4.0.0.CR1 (Java 21 minimum, Java 25 tested), CDI 5 (ArC), Hibernate ORM 8 with Jakarta Persistence 4, Jackson 3. The Quarkus 3.40 line is maintained on the `3.40.x` branch.

Severity used in reviews:

* **BLOCKER**: breaks the build, the native executable or a supported launch mode, loses data, or will not compile on the target Quarkus version. Must be fixed before merge.
* **MAJOR**: breaks a documented Quarkus contract, has a measurable startup, memory or hot path cost, or blocks a maturity matrix criterion. Fix before merge unless the review records an owner and a follow-up.
* **MINOR**: idiom, hygiene, naming or documentation. Fix when touching the code.

Rule groups: [PRJ](#1-project-layout-and-metadata-prj) project layout, [BLD](#2-build-steps-bld) build steps, [SVC](#3-startup-services-and-recorders-svc) startup services, [CFG](#4-configuration-cfg) configuration, [CDI](#5-cdi-and-arc-cdi) CDI, [NAT](#6-native-executables-nat) native, [DEV](#7-dev-mode-dev-ui-and-dev-services-dev) dev mode, [TST](#8-testing-tst) testing, [THR](#9-threading-thr) threading, [TX](#10-transactions-and-persistence-tx) transactions and persistence, [INT](#11-integrations-int) integrations, [OPS](#12-operations-ops) operations, [PERF](#13-performance-memory-and-startup-budget-perf) performance, [CI](#14-ci-and-release-ci) CI and release. The [review checklist](#15-review-checklist) is at the end, followed by the [sources](#16-sources).

---

## 1. Project layout and metadata (PRJ)

### PRJ-01 Every extension is a runtime and deployment pair

* Rule: each extension has a runtime artifact `quarkus-modulith-<name>` and a deployment artifact `quarkus-modulith-<name>-deployment`. The deployment artifact depends on its runtime artifact and on the deployment artifact of every Quarkus extension the runtime depends on. Optional runtime dependencies have an optional deployment counterpart. Plain libraries (`api`, `core`, `documenter`, `junit`) have no deployment module and no Quarkus build steps.
* Why: Quarkus resolves the deployment class path from the runtime artifacts' `quarkus-extension.properties`. A missing deployment dependency means a build step silently does not run.
* Check: for each `io.quarkus:quarkus-X` in a runtime `pom.xml`, the matching deployment `pom.xml` lists `quarkus-X-deployment` with the same `optional` flag. `./mvnw -pl <runtime> dependency:tree | grep -- -deployment` prints nothing.
* Source: [WE], section "Project setup".

### PRJ-02 Runtime artifacts never see build-time libraries

* Rule: runtime modules do not depend on Jandex, ASM, Gizmo, `quarkus-core-deployment` or any `-deployment` artifact. Build-only code (model builder, verifier, documentation writer, ASM collectors) lives in deployment modules or in a plain library that only deployment modules depend on.
* Why: every runtime dependency ends up in the application and in the native image.
* Check: `./mvnw -pl runtime,events/runtime,events-jpa/runtime dependency:tree | grep -E 'jandex|org.ow2.asm|gizmo|-deployment'` prints nothing.
* Source: [WE], section "Three Phases of Bootstrap and Quarkus Philosophy".

### PRJ-03 Complete `quarkus-extension.yaml`

* Rule: every runtime module has `src/main/resources/META-INF/quarkus-extension.yaml` with `name`, `description`, `metadata.keywords`, `metadata.categories`, `metadata.status`, `metadata.guide` and `metadata.config` (the property prefixes the extension owns, for example `quarkus.modulith.`). Internal building blocks set `metadata.unlisted: true`. Never hand-write `quarkus-extension.json` or `.properties`.
* Why: the Dev UI card, code.quarkus.io and the CLI read this file. `metadata.config` filters the configuration shown for the extension in the Dev UI.
* Check: `grep -L 'config:' */runtime/src/main/resources/META-INF/quarkus-extension.yaml runtime/src/main/resources/META-INF/quarkus-extension.yaml` prints nothing for listed extensions.
* Source: [META], [DEVUI] section "quarkus-extension.yaml".

### PRJ-04 Capabilities instead of class probing

* Rule: an extension that provides a contract other modules react to declares it as a capability in the `extension-descriptor` goal (`<capabilities><provides>io.quarkiverse.modulith.events.registry</provides></capabilities>`). Deployment code reacts to other extensions through `Capabilities.isPresent(...)` or an optional build item, never through `Class.forName` or `QuarkusClassLoader.isClassPresentAtRuntime` when a capability exists.
* Why: capabilities are resolved once by the build and documented in the extension metadata. Class probing is fragile in dev mode class loaders.
* Check: `grep -rn 'Class.forName\|isClassPresentAtRuntime' */deployment/src/main deployment/src/main` returns only cases with no capability available, each with a comment.
* Source: [WE] section "Capabilities", [META].

### PRJ-05 Optional integrations stay optional

* Rule: integrations with Narayana, Hibernate ORM, Jackson, Vert.x HTTP, Micrometer, OpenTelemetry, SmallRye Health, the scheduler and Flyway are `<optional>true</optional>` in the runtime module whenever the extension works without them, and the deployment module registers the integration beans only when the capability is present. Classes that import an optional library are only reachable when its capability is present.
* Why: an application must not pay for libraries it does not use. In native mode an unreachable class is never compiled, but a reachable one that imports a missing library breaks the build.
* Check: the extension test suite contains one `QuarkusExtensionTest` with and one without each optional integration (see TST-06).
* Source: [WE] sections "Extension Health Check", "Extension Metrics", "Customizing JSON handling from an extension".

### PRJ-06 Naming and packages

* Rule: groupId `io.quarkiverse.modulith`, artifactIds start with `quarkus-modulith`, Java packages start with `io.quarkiverse.modulith`. Deployment classes live in a `.deployment` package. Runtime internals live in `.runtime` packages, and classes made public only for generated code go to an `.impl` sub-package. `FeatureBuildItem` names are unique, lower case and start with `modulith`.
* Why: Quarkiverse conventions, and a clear line between API and internals.
* Check: `grep -rn 'new FeatureBuildItem' --include='*.java' .` lists one distinct name per extension.
* Source: [HUB-NEW], [WE] section "Features".

### PRJ-07 Users and sibling modules only import API packages

* Rule: application code, `junit`, `documenter` and the persistence or transport extensions only import types from API packages (`io.quarkiverse.modulith`, `io.quarkiverse.modulith.core`, `io.quarkiverse.modulith.events`, and documented SPI packages). Types in `.runtime` and `.deployment` packages are internal. Public constructors of API types do not take internal types.
* Why: internal types change between minor versions. Sibling modules that import them turn every refactoring into a cross-package break, which hurts parallel changes most.
* Check: `grep -rln 'import io.quarkiverse.modulith.runtime\.' --include='*.java' junit integration-tests events*/runtime` lists only documented SPI usages.
* Source: [WE] section "Expose your components via CDI".

### PRJ-08 Quarkiverse parent, release and module profiles

* Rule: the root pom inherits `io.quarkiverse:quarkiverse-parent`. Releases go through a pull request that edits `current-version` and `next-version` in `.github/project.yml`. The `docs` and `it` profiles are active only when `performRelease` is not `true`, so documentation and integration-test modules are versioned but never deployed. New integration-test modules join the `integration-tests` aggregator, never the root module list.
* Why: the Quarkiverse release workflow relies on this layout.
* Check: `grep -n 'performRelease' -A2 pom.xml` shows both profiles. `grep -n '<module>integration' pom.xml` matches only inside the `it` profile.
* Source: [HUB-REL], [HUB-NEW].

### PRJ-09 Java and Quarkus baseline

* Rule: `maven.compiler.release` is `21`. CI builds on JDK 21 and 25. Code must not use APIs newer than Java 21.
* Why: Quarkus 4 requires Java 21 and recommends 25.
* Check: `grep -n 'maven.compiler.release' pom.xml` shows 21. `grep -n 'java:' .github/workflows/build.yml` lists 21 and 25 only.
* Source: [MIG4] section "Java baseline".

### PRJ-10 Integration tests build after the deployment modules

* Rule: each integration-test module declares the deployment artifacts of the extensions it uses as `type pom`, `scope test`, with an `exclusions` wildcard.
* Why: Maven does not otherwise know that the application build needs the deployment module, so `-pl ... -am` and parallel builds can resolve a stale snapshot.
* Check: every `integration-tests/*/pom.xml` lists a `<type>pom</type>` dependency per used `-deployment` artifact.
* Source: Quarkus core integration-test poms, for example [QIT-POM].

### PRJ-11 AI coding skill (optional)

* Rule: once an extension is stable, its deployment module may ship `META-INF/quarkus-skill.md` with patterns, testing guidance and pitfalls.
* Why: listed as a Developer Joy criterion of the maturity matrix.
* Check: file present when `metadata.status` is `stable`.
* Source: [MATRIX] section "AI coding skill".

---

## 2. Build steps (BLD)

### BLD-01 Do the work at build time

* Rule: discovery, validation, model building, listener tables, routing tables and expression parsing happen in build steps. Runtime code receives small, immutable data (records, `List.copyOf`, strings, class names) and does lookups only.
* Why: work done at build time costs nothing at startup, and the closed world is what makes native images small.
* Check: `grep -rn 'getResources\|ClassGraph\|Reflections\|Index.of\|IndexReader' runtime/src/main events*/runtime/src/main` prints nothing.
* Source: [WE] section "Favor build time work over runtime work", [MATRIX] "Static initialization".

### BLD-02 Build steps have no side effects outside build items

* Rule: build steps communicate only through build items. Files meant for the user go through `GeneratedFileSystemResourceBuildItem` (written into the build output directory) or `ArtifactResultBuildItem`. Class path resources go through `GeneratedResourceBuildItem`, service files through `GeneratedServiceProviderBuildItem`, static web files through `GeneratedStaticResourceBuildItem`. Processors hold no static mutable state.
* Why: Quarkus runs steps in parallel and only when their outputs are consumed. In dev and test mode generated resources live in memory. Direct `Files.write` calls run in every launch mode and bypass that handling.
* Check: `grep -rn 'Files.write\|FileOutputStream\|Files.createDirectories' */deployment/src/main deployment/src/main` returns only code reached from a `GeneratedFileSystemResourceBuildItem` producer, or nothing.
* Source: [WE] sections "Build items", "Generating Resources".

### BLD-03 Use the index Quarkus already built

* Rule: read annotations and types from `CombinedIndexBuildItem` (or `BeanArchiveIndexBuildItem` for bean metadata). Add external jars with `IndexDependencyBuildItem` or `AdditionalIndexedClassesBuildItem`, never with an index of your own. When per-archive information is needed (which archive owns a class, test output directories), compute it once in one step and publish the result as a `SimpleBuildItem` that other steps consume.
* Why: Quarkus builds the combined index once. Every extra `CompositeIndex` or archive scan is repeated on every build and on every dev mode reload.
* Check: `grep -rn 'CompositeIndex.create\|getKnownClasses()' */deployment/src/main deployment/src/main` returns at most one producer step, which other steps consume through a build item.
* Source: [WE] sections "Application Archives", "Adding external JARs to the indexer with IndexDependencyBuildItem", "Scanning Deployments Using Jandex".

### BLD-04 Query by annotation or type, then index the result

* Rule: start from `index.getAnnotations(NAME)`, `getAllKnownImplementations` or `getAllKnownSubclasses`. Group the result into maps once (by package, by module) instead of filtering the whole annotation list again per module or per class.
* Why: nested loops over the index grow with modules times annotations and show up in the build metrics of large applications.
* Check: no loop over `index.getAnnotations(...)` nested inside a loop over modules or classes. Build step time stays in the PERF-02 budget.
* Source: [WE] section "Scanning Deployments Using Jandex".

### BLD-05 Read bytecode only when Jandex cannot answer, and read it cheaply

* Rule: never load application classes in a build step (`Class.forName`, `loadClass`) to inspect them. When method bodies matter (type references in instructions), read class files from the `ApplicationArchive` path tree with ASM, pass `ClassReader.SKIP_FRAMES`, and use a visitor instead of building a `ClassNode` tree. Declare `org.ow2.asm:asm` explicitly in the deployment pom.
* Why: loading classes runs static initializers in the build class loader and pins them. A full `ClassNode` allocates the whole tree for each class only to collect names.
* Check: `grep -rn 'new ClassNode\|Class.forName' */deployment/src/main deployment/src/main` prints nothing.
* Source: [WE] sections "Using Thread's Context Class Loader", "Using Gizmo".

### BLD-06 Build items are final and immutable

* Rule: build item classes are `final`, extend `SimpleBuildItem`, `MultiBuildItem` or `EmptyBuildItem`, and expose immutable data (records, `List.copyOf`). A build item that other extensions may consume does not expose a mutable deployment model with `add...` methods.
* Why: build items cross extension boundaries and steps run concurrently.
* Check: `grep -rn 'extends \(Simple\|Multi\|Empty\)BuildItem' --include='*.java' .` lists only `final` classes. Their getters return unmodifiable views.
* Source: [WE] section "Build items".

### BLD-07 Condition steps instead of branching inside them

* Rule: dev-only, test-only and production-only work uses `@BuildStep(onlyIf = ...)` with `IsLocalDevelopment`, `IsDevelopment`, `IsTest`, `IsNormal` or a `BooleanSupplier` that reads BUILD_TIME config. Steps that depend on another extension take an `Optional<SomeBuildItem>` or check `Capabilities`.
* Why: conditional steps are not even scheduled, and the condition is visible in the build graph.
* Check: `grep -rn 'getLaunchMode() ==' */deployment/src/main deployment/src/main` returns only cases that need both branches in the same step.
* Source: [WE] section "Conditional Step Inclusion".

### BLD-08 Report every problem at once, with the fix in the message

* Rule: a step that consumes `ValidationPhaseBuildItem` injects `BuildProducer<ValidationErrorBuildItem>` and reports all problems in one `ValidationErrorBuildItem`. Model-level failures may throw from the step. Every message names the element (`Class#method`, module, property) and the fix.
* Why: ArC only orders validation correctly when the producer is injected. One build that lists all problems saves the user a loop of rebuilds. The message is what the dev mode error page shows.
* Check: each failure path has a `QuarkusExtensionTest` that asserts the message text (TST-01).
* Source: [CDI-INT] section "Use Case - I Need to Validate the Deployment", [WE] section "Validation Error build items".

### BLD-09 Quiet builds

* Rule: build steps log details at DEBUG, at most one INFO line for a summary the user asked for, and WARN only for something the user must act on. The same message is never logged by two steps. Helpers called from several steps compute once and publish a build item instead.
* Why: noisy builds hide real warnings. The maturity matrix asks not to log warnings for conditions that do not affect normal operation.
* Check: build the shop application and `grep -c 'Modulith' build.log` stays at the expected count.
* Source: [MATRIX] "Logging".

### BLD-10 Measure build steps

* Rule: build step time is measured with `-Dquarkus.builder.metrics.enabled=true`, which writes `target/build-metrics.json` (also visible on the Dev UI "Build Metrics" page). Reviews of changes that add steps compare the modulith step times before and after.
* Why: build time is part of the developer loop in dev mode.
* Check: PERF-02 budget.
* Source: `io.quarkus.builder.BuildMetrics` in `quarkus-builder` 4.0.0.CR1 (constant `quarkus.builder.metrics.enabled`).

---

## 3. Startup services and recorders (SVC)

### SVC-01 New startup code uses `ServiceRegistrar`

* Rule: in Quarkus 4 bytecode recorders (`@Record`, `@Recorder`, `ExecutionTime`) are deprecated. New startup work is declared with `io.quarkus.core.deployment.service.ServiceRegistrar` injected into a build step: `reg.forService(Type.class).onStart(ctx -> Factory.create(capturedData))`. Existing recorders are migrated with the official guide.
* Why: recorders will be removed in a future release. Services carry explicit dependencies and run in parallel.
* Check: `grep -rn '@Record\b\|@Recorder' --include='*.java' .` prints nothing.
* Source: [WE] sections "Application Startup and Services", "Legacy Bytecode Recorders (Deprecated)", [R2S].

### SVC-02 Capture only what the transliterator supports

* Rule: service lambdas capture primitives, `String`, `Class`, enums, immutable collections (`List.copyOf`, `Set.copyOf`, `Map.copyOf`), `Constable` values, records whose components are all capturable, and `BUILD_AND_RUN_TIME_FIXED` mappings. BUILD_TIME config values are copied into local variables first. RUN_TIME config is obtained with `.require(ConfigType.class)`. Prefer empty strings or empty lists over `null` in captured records unless a test proves null components work.
* Why: the lambda body is transliterated to bytecode at build time and captured values become constants. Anything else is a build error (`TransliterationException`).
* Check: every service has a `QuarkusExtensionTest` that starts the application (a capture problem fails the build).
* Source: [WE] section "Lambda Capture Rules".

### SVC-03 Lambdas reference runtime classes only

* Rule: a service lambda never references a deployment class, never creates an anonymous class inside a processor method, and never calls a static helper of the processor. Factories live in the runtime module, are `public`, and go to an `.impl` package when they are public only for this reason.
* Why: lambda bytecode runs at runtime where deployment classes do not exist (`NoClassDefFoundError`), and package-private access from the generated class fails (`IllegalAccessError`).
* Check: `grep -n 'onStart' -A3` in each processor shows calls to runtime classes only.
* Source: [WE] section "Critical Pitfalls & Best Practices", [R2S] section "Common Conversion Pitfalls".

### SVC-04 Declare every dependency

* Rule: ordering between services comes only from `require`, `request`, `consumeAll`, `after` and `before`. A synthetic bean backed by a service uses `SyntheticBeanBuildItem.configure(...).serviceValue(Type.class)`. CDI access from a service goes through `.require(BeanContainer.class)` or `.require(ArcContainer.class)`, not `Arc.container()`. `afterBuildItem` is only a bridge to unconverted recorders.
* Why: build step order has no influence on service order. Undeclared dependencies race.
* Check: review each `forService` chain against what the lambda touches.
* Source: [WE] sections "Declaring Dependencies and Ordering", "Synthetic Beans with Services".

### SVC-05 No global mutable state filled at startup

* Rule: runtime classes do not keep build-time data in `static` fields set by a recorder or service. Data flows into a synthetic bean or through the service graph and is injected where needed.
* Why: services run concurrently, and static state survives dev mode restarts and test application restarts in ways that are hard to reason about.
* Check: `grep -rn 'static volatile\|static .*List<\|static .*Map<' runtime/src/main events*/runtime/src/main` returns only constants.
* Source: [R2S] section "Global Mutable State", [WE] section "Handling restarts".

### SVC-06 Pick the earliest safe phase

* Rule: pure data (the module model, the listener table) is produced at `Phase.STATIC_INIT` so it is baked into the native image heap. Anything that reads RUN_TIME config, opens connections, starts threads or reads the clock runs at a later phase (`APPLICATION` by default, `INIT` for one-time tasks such as schema checks, `SERVING` for consumers that start pulling work).
* Why: static init work is free at native startup. Static init code that captures runtime state produces wrong native images.
* Check: no `STATIC_INIT` service requires a RUN_TIME config type or creates threads.
* Source: [WE] section "Service Startup Phases".

### SVC-07 Every started resource has a stop handler

* Rule: services that start threads, executors, schedulers or connections register `ctx.onStop` (or `onStopAsync`). Work that must drain registers `onGracefulPreShutdown` and `onGracefulShutdown`. Threads created at startup must not keep the runtime class loader alive after stop.
* Why: stop runs in reverse dependency order and leaked threads keep the `QuarkusClassLoader` alive across dev mode restarts.
* Check: dev mode test (DEV-01) restarts the application several times without growing thread count.
* Source: [WE] section "Teardown & Shutdown Handlers", [R2S] section "ClassLoader & Thread Context ClassLoader (TCCL)".

---

## 4. Configuration (CFG)

### CFG-01 Config mappings only

* Rule: extension configuration is declared with `@ConfigMapping(prefix = "quarkus.modulith...")` interfaces annotated with `@ConfigRoot(phase = ...)`. No `@ConfigProperty` fields and no `ConfigProvider.getConfig().getValue(...)` for the extension's own keys.
* Why: mappings are validated, documented and exposed per phase by Quarkus.
* Check: `grep -rn 'ConfigProperty\|ConfigProvider.getConfig' */runtime/src/main runtime/src/main */deployment/src/main deployment/src/main` prints nothing.
* Source: [WE] section "Configuration", [CFGMAP].

### CFG-02 Choose the phase by what the value changes

* Rule: BUILD_TIME for anything that changes generated code, beans, verification or documentation (base package, verification mode, module test selection, persistence unit of the outbox entity, enabled transports). RUN_TIME for intervals, timeouts, thresholds and job toggles. BUILD_AND_RUN_TIME_FIXED only when runtime code must read a value fixed at build. BUILD_TIME mappings may live in the deployment module. Other phases live in the runtime module.
* Why: BUILD_TIME values are not available at runtime and RUN_TIME values cannot change what was generated. A wrong phase gives either a silent no-op or a rebuild the user did not expect.
* Check: each property's Javadoc says when it takes effect. A `QuarkusExtensionTest` covers each BUILD_TIME property.
* Source: [WE] section "Config Phases".

### CFG-03 Every property is documented and typed

* Rule: every mapping method has Javadoc (it becomes the generated reference), a `@WithDefault` or an `Optional` type, and the most specific type (`Duration`, `MemorySize`, enums, `List<String>`). Enum constants are written so their kebab-case form reads well (`DIRECT_DEPENDENCIES` gives `direct-dependencies`). The generated reference under `docs/modules/ROOT/pages/includes` is regenerated and committed with the change.
* Why: the config reference is generated from these sources and is the user's main documentation.
* Check: `./mvnw -pl docs -am package` succeeds and `git status docs` shows the regenerated includes.
* Source: [WE] section "Configuration Reference Documentation".

### CFG-04 One owner per prefix

* Rule: each artifact owns a distinct prefix segment (`quarkus.modulith.events.` for the registry core, `quarkus.modulith.events.jpa.` for the JPA store, `quarkus.modulith.events.jdbc.` and so on). Two config roots of the same phase never declare the same property. Shared settings belong to the root of the artifact everybody depends on.
* Why: overlapping roots split the documentation of one prefix across artifacts and make unknown-key warnings ambiguous.
* Check: `grep -rn '@ConfigMapping(prefix' --include='*.java' .` lists no prefix twice for the same phase, and no property name appears in two roots.
* Source: [WE] section "How to expose configuration".

### CFG-05 Annotation placeholders mirror the mapping

* Rule: when a property must be used in an annotation (`@Scheduled(every = "${quarkus.modulith...:off}")`), the property is also declared in a mapping with the same name and the same default, and a test runs with the property set and unset. If a value is used both as an interval and as an age, it gets two properties.
* Why: placeholders are not validated against the mapping and a typo only shows up as a job that never runs.
* Check: grep each `${quarkus.modulith` placeholder and find the matching mapping method.
* Source: [SCHED] section "Intervals", [CFGMAP].

---

## 5. CDI and ArC (CDI)

### CDI-01 Register extension beans explicitly

* Rule: beans that live in extension runtime modules are registered with `AdditionalBeanBuildItem` from the deployment module (conditionally when they belong to an optional integration), not discovered through a Jandex index of the runtime jar.
* Why: registration then follows capabilities and configuration instead of class path contents.
* Check: no `META-INF/beans.xml` and no `jandex` plugin in runtime modules.
* Source: [CDI-INT] section "Reason 1: Class Is Not discovered".

### CDI-02 No private members in beans

* Rule: injection fields, constructors, initializers, observers, producers and disposers are package-private or wider.
* Why: ArC needs reflection for private members, which costs native image size, and transformed classes break JaCoCo class matching.
* Check: `grep -rnE '@Inject\s*$' -A1 --include='*.java' runtime events* | grep private` prints nothing. Same for `@Observes` and `@Produces` parameters on private methods.
* Source: [CDIREF] section "Native Executables and Private Members".

### CDI-03 Unremovable only where lookup is dynamic

* Rule: mark beans unremovable only when they are looked up programmatically and never injected (listener beans looked up by an invoker, beans used from `CDI.current()`). Use the narrow forms (`UnremovableBeanBuildItem.beanClassAnnotation`, `beanTypes`, `AutoAddScopeBuildItem.unremovable()`). Do not add `setUnremovable()` to beans that are injected or have observers.
* Why: unused bean removal is framework-level dead code elimination.
* Check: each `setUnremovable` or `UnremovableBeanBuildItem` has a one-line reason.
* Source: [CDI-INT] section "Reason 3", [CDIREF] section "Removing Unused Beans".

### CDI-04 Default implementations use `@Reserve`

* Rule: in Quarkus 4, a default implementation users may replace (serializer, transport, repository) is annotated `@jakarta.enterprise.inject.Reserve` with `@jakarta.annotation.Priority(0)` when it has no priority. `@io.quarkus.arc.DefaultBean` is deprecated.
* Why: CDI 5 standardizes reserve beans.
* Check: `grep -rn 'DefaultBean' --include='*.java' .` prints no annotation use, only comments.
* Source: [MIG4] section "CDI / ArC".

### CDI-05 Synthetic beans for build-computed data

* Rule: data computed at build time (`ApplicationModules`, listener tables, routing tables) is exposed as a synthetic bean (`SyntheticBeanBuildItem`) backed by a service value (`serviceValue`) or, until migration, a recorded supplier. Use `setRuntimeInit()` when the value needs RUN_TIME config.
* Why: users and other beans inject it like any other bean, tests can replace it, and there is no static holder.
* Check: no runtime class exposes build data through a static getter.
* Source: [CDI-INT] section "Use Case - The Need for Synthetic Beans", [WE] section "Synthetic Beans with Services".

### CDI-06 Invoke user methods through ArC invokers

* Rule: user methods discovered at build time (listeners, externalization hooks) are called through `InvokerFactoryBuildItem` invokers, built with `withInstanceLookup()` where the target is a bean. No `Method.invoke`. Creating the invoker instance by class name with its constructor registered for reflection is acceptable (the Quarkus scheduler does the same), generated bytecode that calls the constructor directly is better.
* Why: invokers are generated bytecode with no reflection and support asynchronous return types.
* Check: `grep -rn 'Method.invoke\|getDeclaredMethod' runtime/src/main events*/runtime/src/main` prints nothing.
* Source: [CDIREF] section "Supported Features and Limitations" (method invokers), Quarkus 4.0.0.CR1 `SchedulerProcessor` ([SCHED-SRC]).

### CDI-07 Observe only the event types you route

* Rule: production beans never declare `@Observes Object` or `@ObservesAsync Object` (or any other unqualified catch-all type). When the event types are known at build time, register one synthetic observer per type with `ObserverRegistrationPhaseBuildItem` and `ObserverConfiguratorBuildItem`, or generate an observer bean. A catch-all observer needed only by tests is registered only in TEST launch mode.
* Why: an observer without qualifiers receives every event, including qualified context lifecycle events such as `@Initialized(RequestScoped.class)`. ArC skips firing those when nobody observes them (`CurrentManagedContext.fireIfNotNull` in ArC 4.0.0.CR1). A catch-all observer turns that skip off for every request and every transaction.
* Check: `grep -rn '@Observes\(Async\)\? Object' --include='*.java' runtime events* | grep -v src/test` prints nothing.
* Source: [CDI-INT] section "Use Case - Synthetic Observers", CDI 4.1/5 observer resolution rules, ArC bytecode (see check).

### CDI-08 Validation and scopes in the CDI phases

* Rule: build-time checks of user beans run in a step that consumes `ValidationPhaseBuildItem` (BLD-08). Classes that only declare annotated methods get a scope through `AutoAddScopeBuildItem` with a `reason`.
* Why: these are the hooks ArC defines for extensions, and they run at the correct bootstrap phase.
* Check: code review.
* Source: [CDI-INT] sections "Use Case - I Need to Validate the Deployment", "Reason 2".

### CDI-09 Decide optional collaborators at build time

* Rule: when the presence of a collaborator is known at build time (registry present, Narayana present, Micrometer present), the deployment module registers the right implementation and runtime code injects it directly. `Instance<T>#isResolvable()` is not called on a hot path. If dynamic lookup is unavoidable, resolve once and keep the result.
* Why: the closed world makes the answer constant, and per-call resolution costs time on every event.
* Check: `grep -rn 'isResolvable()' runtime/src/main events*/runtime/src/main` returns only code outside hot paths.
* Source: [WE] section "Expose your components via CDI", [MATRIX] "Hot path performance optimization".

### CDI-10 Scope choice

* Rule: internal infrastructure beans are `@Singleton` (no client proxy). Beans users are expected to mock with `QuarkusMock` or `@InjectMock` are normal scoped (`@ApplicationScoped`). Request context activation in background work uses `ManagedContext.activate/terminate` in a `finally` block (or `@ActivateRequestContext`).
* Why: mocking needs a client proxy, and a leaked request context leaks request-scoped beans.
* Check: code review.
* Source: [CDIREF] section "Request Context Lifecycle".

---

## 6. Native executables (NAT)

### NAT-01 No runtime discovery

* Rule: no class path scanning, no `ServiceLoader` over application classes and no reflection-based discovery at runtime. Service providers the extension needs at runtime are registered with `GeneratedServiceProviderBuildItem` or `ServiceProviderBuildItem`.
* Why: the native image only contains what the build registers.
* Check: `grep -rn 'ServiceLoader.load\|getResources(' runtime/src/main events*/runtime/src/main` returns only registered providers.
* Source: [WE] section "Native Executable Support", [NATREF].

### NAT-02 Reflection is the exception and is justified

* Rule: every `ReflectiveClassBuildItem` has `.reason(...)` and registers the smallest member set (only `constructors()` when the class is only instantiated). Prefer generated bytecode, invokers or recorded data to reflection.
* Why: each registration grows the image and slows the native build.
* Check: `grep -rn 'ReflectiveClassBuildItem.builder' -A3 --include='*.java' . | grep -c reason` equals the number of builders.
* Source: [MATRIX] "Replace reflection with generated bytecode", [NATTIPS].

### NAT-03 Register the whole serialized type graph

* Rule: types serialized at runtime (events stored by the registry, events externalized to brokers) are registered with `ReflectiveHierarchyBuildItem` so that field types, nested records, collections elements and the type hierarchy are covered. Include every known subtype of a listened type (`index.getAllKnownSubclasses` and `getAllKnownImplementations`), because the runtime stores the concrete class. Types loaded with `Class.forName(name)` at runtime must be registered.
* Why: a listener for a supertype receives subtypes, and Jackson walks the whole graph. A missing registration fails at runtime, only in native, inside the business transaction.
* Check: a native `@QuarkusIntegrationTest` round-trips an event that is a subtype of the listened type and contains a nested record and a collection.
* Source: [NATREF] section "Registering for reflection", [WE] section "Native Executable Support".

### NAT-04 Resources

* Rule: resources read at runtime are registered with `NativeImageResourceBuildItem` (or generated with `GeneratedResourceBuildItem`, which handles it).
* Why: native images contain no resources by default.
* Check: `grep -rn 'getResourceAsStream' runtime/src/main events*/runtime/src/main` has a matching registration.
* Source: [WE] section "Native Executable Support".

### NAT-05 Static initializers are safe at build time

* Rule: classes initialized at image build time do not hold threads, sockets, random seeds, `Instant.now()` or configuration in `static` fields. Classes that must initialize at runtime are registered with `RuntimeInitializedClassBuildItem`.
* Why: build-time initialization freezes whatever the initializer computed into the image.
* Check: native integration tests pass and `-H:+PrintClassInitialization` output shows no extension class with runtime state.
* Source: [NATTIPS] section "Delaying class initialization".

### NAT-06 Every feature runs in native in CI

* Rule: each feature with runtime behavior has a `@QuarkusIntegrationTest` (usually a subclass of the `@QuarkusTest`) that runs against the native executable in the native CI job. Tests talk to the application through HTTP or the database, never through injected beans.
* Why: JVM-mode success says nothing about missing reflection or resources.
* Check: every `*Test.java` in integration-tests with runtime behavior has a `*IT.java` twin.
* Source: [TESTING] section "Native Executable Testing", [MATRIX] "Works as a native application".

---

## 7. Dev mode, Dev UI and Dev Services (DEV)

### DEV-01 Dev mode is tested

* Rule: each extension has `QuarkusDevModeTest` coverage for its live reload story: change a source file, the next request sees the change. For Modulith: introduce a module violation, the dev error page shows it with the fix, remove it, the application recovers. Add a listener, it is picked up.
* Why: "Works in dev mode" is a maturity criterion and class loading bugs only show up there.
* Check: `grep -rln QuarkusDevModeTest --include='*.java' .` lists a test per behavior above.
* Source: [MATRIX] "Works in dev mode", [WE] section "Testing hot reload".

### DEV-02 Failures render on the dev error page

* Rule: build-time failures throw exceptions (or produce validation errors) with actionable messages. Never `System.exit`, never log and continue when the result would be wrong.
* Why: in dev mode Quarkus renders build failures as an error page and retries after the next edit.
* Check: the DEV-01 test asserts the error page content.
* Source: [WE] section "Integrating with Development Mode".

### DEV-03 Watch the files you read

* Rule: if a build step reads a non-class file from the project (a template, a properties or YAML file), it produces `HotDeploymentWatchedFileBuildItem` for it.
* Why: otherwise edits to that file do not trigger a reload.
* Check: every `Files.read*` or resource read in a deployment step has a matching watched file item.
* Source: [WE] section "Integrating with Development Mode".

### DEV-04 State across reloads goes in `LiveReloadBuildItem`

* Rule: anything a build step wants to keep between dev mode reloads is stored with `LiveReloadBuildItem#setContextObject`, never in static fields. `isLiveReload()` and `getChangeInformation()` decide whether expensive work can be skipped.
* Why: static fields in deployment classes survive in surprising ways and leak class loaders.
* Check: `grep -rn 'static [^f].*=' */deployment/src/main deployment/src/main` returns constants only.
* Source: [WE] section "Handling restarts".

### DEV-05 Dev UI conventions

* Rule: Dev UI steps run with `onlyIf = IsLocalDevelopment.class`. Static model data goes through build-time data (small JSON-friendly maps, lists and strings). Live data (incomplete publications, listener activity) goes through a JSON-RPC service registered with `JsonRPCProvidersBuildItem`, whose class lives in a `runtime-dev` module. Deployment-classpath actions use `BuildTimeActionBuildItem`. Web components use the libraries bundled with the Dev UI (`lit`, Vaadin, `echarts`), never a CDN.
* Why: dev-only code then never reaches production artifacts, and the Dev UI works offline.
* Check: `grep -rn 'IsDevelopment.class' */deployment/src/main deployment/src/main` returns no Dev UI step. `grep -rn 'http' deployment/src/main/resources/dev-ui` prints nothing.
* Source: [DEVUI] sections "Adding Pages to the Dev UI", "Communicating to the backend".

### DEV-06 Dev Services only for services the extension owns

* Rule: Modulith extensions do not start containers of their own for databases or brokers that a Quarkus extension already provides Dev Services for (datasources, Kafka, RabbitMQ, AMQP, MongoDB). They rely on those and document it. A Dev Service written here follows the `DevServicesResultBuildItem` contract and has a `quarkus.modulith.<x>.devservices.enabled` switch.
* Why: duplicate containers and conflicting configuration.
* Check: no Testcontainers dependency in runtime or deployment modules.
* Source: [DEVSVC], [MATRIX] "Dev Service".

---

## 8. Testing (TST)

### TST-01 Extension behavior is tested with `QuarkusExtensionTest`

* Rule: build steps, configuration properties, validation errors and recorded state are tested in the deployment module with `io.quarkus.test.QuarkusExtensionTest` (`quarkus-junit-internal`). One class per scenario, application content built with `withApplicationRoot`, failures asserted with `assertException` or `setExpectedException`, log output asserted with `setLogRecordPredicate`.
* Why: each test builds its own small application, so every build-time branch is reachable. `QuarkusUnitTest` no longer exists in Quarkus 4.
* Check: every BUILD_TIME property and every build error message has a test.
* Source: [WE] section "Testing Extensions".

### TST-02 Dev and prod mode tests where behavior differs

* Rule: use `QuarkusDevModeTest` for reload and Dev UI behavior (DEV-01) and `QuarkusProdModeTest` for packaging behavior (generated documentation in the build output, Dev UI absent in production).
* Why: these modes take code paths `QuarkusExtensionTest` does not.
* Check: one test per mode-specific behavior.
* Source: [WE] sections "Testing Extensions", "Testing hot reload".

### TST-03 Application tests in JVM, packaged jar and native

* Rule: integration-test applications use `@QuarkusTest` for in-JVM tests and `@QuarkusIntegrationTest` subclasses for the packaged jar and the native executable (NAT-06). Dev Services provide databases and brokers.
* Why: only these exercise the real packaging.
* Check: see NAT-06.
* Source: [TESTING].

### TST-04 Bean-level tests without booting

* Rule: runtime beans with logic but no build-time dependency (dispatcher routing, retry policies, routing resolution) are tested with `QuarkusComponentTest` or plain JUnit with package-private seams.
* Why: fast feedback and full branch coverage for the JaCoCo gate.
* Check: the 100% line and branch coverage gate of the `coverage` module passes.
* Source: [TESTING] section "Testing components".

### TST-05 One Surefire execution per test style

* Rule: modules that mix `QuarkusExtensionTest` and `@QuarkusTest` run them in separate Surefire executions.
* Why: Quarkus refuses to run both in one JVM (`ExclusivityChecker`).
* Check: the module pom has one execution per style.
* Source: `io.quarkus.test.ExclusivityChecker` in Quarkus' JUnit support.

### TST-06 Test each optional integration present and absent

* Rule: for every optional integration (Narayana, a registry, Micrometer, OpenTelemetry, Jackson), there is at least one test application with it and one without it.
* Why: the "absent" path is where `NoClassDefFoundError` and wrong defaults hide.
* Check: test class names or fixtures make the combination explicit.
* Source: [WE] section "Testing Extensions".

### TST-07 Tests are order independent and continuous-testing safe

* Rule: tests reset shared state in `@BeforeEach`, do not depend on execution order, and do not use `Thread.sleep` to wait for asynchronous work (use Awaitility or the extension's own wait API).
* Why: continuous testing reruns subsets in any order, and sleeps make suites slow and flaky.
* Check: `grep -rn 'Thread.sleep' --include='*.java' */src/test integration-tests` prints nothing.
* Source: [CONTTEST].

### TST-08 Virtual thread paths are pinning-tested

* Rule: code that can run on virtual threads (listener execution with the virtual thread executor, externalization) has tests with `@VirtualThreadUnit` and `@ShouldNotPin` (`io.quarkus.junit:junit-virtual-threads`).
* Why: pinning is invisible in functional tests.
* Check: test classes with those annotations exist for each virtual-thread code path.
* Source: [VT] section "Testing virtual thread applications".

---

## 9. Threading (THR)

### THR-01 Never block an I/O thread

* Rule: any runtime path that can be entered from a Vert.x event loop (a CDI event fired from a reactive route, a Vert.x handler, a Mutiny pipeline) checks `BlockingOperationControl.isBlockingAllowed()` before blocking work (JDBC, JTA, file I/O, `Thread.sleep`, waiting on futures) and offloads it when blocking is not allowed. Starting a JTA transaction is blocking work.
* Why: a blocked event loop stalls every connection it serves.
* Check: a test fires a module event from an event loop (`@Route` or `vertx.runOnContext`) with a registry present and asserts no `BlockingOperationNotAllowedException` and no blocked-thread warning.
* Source: [CDIREF] section "Detecting When Blocking Is Allowed".

### THR-02 Run background work on a configurable, bounded executor

* Rule: asynchronous work (listener delivery, externalization, resubmission) runs on an executor the user can choose by configuration: the Quarkus worker pool by default, the virtual thread executor (`@VirtualThreads ExecutorService`) when enabled. The extension does not create unbounded pools of its own. Queue depth is observable (metric or Dev UI).
* Why: listeners that share the HTTP worker pool can starve request handling. Virtual thread support is a maturity criterion.
* Check: a test per executor option.
* Source: [VT] section on `@VirtualThreads`, [MATRIX] "Virtual thread support".

### THR-03 Be virtual-thread friendly

* Rule: no `synchronized` blocks around I/O on code paths that run on virtual threads under JDK 21 to 23, no `ThreadLocal` caches of heavy objects, no thread-per-task pools that are not virtual.
* Why: pinning on JDK below 24, and memory growth with many virtual threads.
* Check: TST-08.
* Source: [VT] section "Pinning cases".

### THR-04 Carry context across threads on purpose

* Rule: when work moves to another thread, the extension decides what moves with it: request context (activated fresh), OpenTelemetry context, MDC, security identity. The behavior is documented and tested.
* Why: lost trace context breaks the observability story, and leaked contexts are memory leaks.
* Check: an OpenTelemetry test with the in-memory exporter shows the listener span as a child (or a link) of the publishing span.
* Source: [MATRIX] "Tracing context".

### THR-05 No polling loops in runtime code

* Rule: runtime code waits with latches, futures or condition signals, not `Thread.sleep` loops. Test helpers may poll.
* Why: polling burns CPU and adds latency.
* Check: `grep -rn 'Thread.sleep' runtime/src/main events*/runtime/src/main` prints nothing.
* Source: [MATRIX] "Hot path performance optimization".

---

## 10. Transactions and persistence (TX)

### TX-01 Transactions through Quarkus APIs

* Rule: programmatic transactions use `QuarkusTransaction` (`requiringNew`, `joiningExisting`), declarative ones `@Transactional`. After-commit work uses an interposed synchronization on `TransactionSynchronizationRegistry` (or `@Observes(during = AFTER_SUCCESS)` when the event type is static). Narayana integration is enabled only when `Capability.TRANSACTIONS` is present.
* Why: these APIs handle suspension, timeouts and context correctly in Quarkus.
* Check: `grep -rn 'TransactionManager\b' runtime/src/main events*/runtime/src/main` prints nothing.
* Source: [TX].

### TX-02 The persistence unit is configurable

* Rule: entities the extension contributes are added with `AdditionalJpaModelBuildItem` to a persistence unit chosen by a BUILD_TIME property (default: the default unit). The runtime injects the `EntityManager` or `Session` of that unit with its qualifier. Table and column names are configurable where Spring Modulith makes them configurable.
* Why: many applications only have named persistence units, or keep the outbox in a separate one.
* Check: a `QuarkusExtensionTest` with a named persistence unit and no default unit.
* Source: [ORM] section "Multiple persistence units".

### TX-03 Queries run in the database and are bounded

* Rule: selection and deletion filters are expressed in HQL or SQL, not by loading every row and filtering in memory. Unbounded result sets are paged or streamed. Mutations use Hibernate's `createMutationQuery(...).executeUpdate()` or the Jakarta Persistence 4 replacement, because `Query.executeUpdate()` is deprecated for removal in Jakarta Persistence 4.
* Why: the outbox table grows with traffic, and loading it whole at startup or in a job is an outage waiting to happen.
* Check: `grep -rn 'getResultList().stream().*filter' */runtime/src/main` prints nothing. `grep -rn 'createQuery(.*\(update\|delete\)' */runtime/src/main` prints nothing.
* Source: [ORM].

### TX-04 Entity state changes go through tracked access

* Rule: managed entity state is changed through Panache entities (whose field access Quarkus rewrites) or through accessor methods. Plain public fields of non-Panache entities are not written from other classes unless a test proves the change is flushed when the entity is managed.
* Why: Hibernate's enhanced dirty tracking only sees writes that go through enhanced code paths.
* Check: a test modifies a managed extension entity and reads it back from a new session.
* Source: [PANACHE] section on field access, [ORM].

### TX-05 Panache is optional

* Rule: the extension works with plain Hibernate ORM. Panache-specific helpers live in a separate optional module or behind a capability.
* Why: many applications do not use Panache.
* Check: the JPA store's tests run without `quarkus-hibernate-orm-panache`.
* Source: [PANACHE], [WE] section "Expose your components via CDI".

---

## 11. Integrations (INT)

### INT-01 Jackson 3 on Quarkus 4

* Rule: Quarkus 4 code uses `tools.jackson.core` and `tools.jackson.databind` (`JsonMapper`, unchecked `JacksonException`). Jackson annotations stay in `com.fasterxml.jackson.annotation`. Customization from an extension uses `JsonMapperBuilderCustomizer` (replacement of `ObjectMapperCustomizer`). Persisted formats go through the extension's `EventSerializer` SPI, so a change to the application's shared mapper does not silently change what is stored. JSON support is an optional dependency wherever the extension works without it.
* Why: Jackson 2 types do not exist on the Quarkus 4 class path, and the stored format must stay readable across application changes.
* Check: `grep -rn 'com.fasterxml.jackson.\(core\|databind\)' --include='*.java' .` prints nothing.
* Source: [MIG4] section "Jackson 3", [WE] section "Customizing Jackson".

### INT-02 Micrometer through the metrics SPI

* Rule: metrics support is optional. The deployment module reacts to `Optional<MetricsCapabilityBuildItem>` or produces a `MetricsFactoryConsumerBuildItem`. Metric names and tags follow Spring Modulith where parity is required, tags have low cardinality (module, listener id, never event ids), and a `quarkus.modulith.metrics.enabled` switch exists.
* Why: metrics libraries must not be loaded when absent, and high-cardinality tags exhaust memory.
* Check: TST-06 with and without `quarkus-micrometer`.
* Source: [WE] section "Extension Metrics", [MICROMETER].

### INT-03 OpenTelemetry tracing

* Rule: tracing support is optional and detected with `Capability.OPENTELEMETRY_TRACER`. Spans come from the injected `Tracer`. Instrumentation can be turned off by configuration. Tests use the in-memory span exporter.
* Why: maturity criterion "Tracing context", and OpenTelemetry metrics and logs are on by default in Quarkus 4.
* Check: a test asserts span names, parents and attributes.
* Source: [OTEL], [MATRIX] "Tracing context", [MIG4] section "Observability".

### INT-04 Scheduler use

* Rule: maintenance jobs use `@Scheduled` with an `identity`, `concurrentExecution = SKIP`, and an interval that can be `off`. When jobs are optional the scheduler dependency is optional too, or jobs are registered programmatically through the `Scheduler` API only when enabled. Jobs that must run on one node in a cluster coordinate through the database (row claims) rather than assuming a single instance.
* Why: a hard scheduler dependency adds threads to every application, and duplicate jobs on several nodes resubmit the same publications.
* Check: a test with two registry instances shows each incomplete publication resubmitted once.
* Source: [SCHED].

### INT-05 Messaging

* Rule: Quarkus Messaging integration is detected with `Capability.MESSAGING` (`SMALLRYE_REACTIVE_MESSAGING` is removed in Quarkus 4). Channels are known at build time and declared or generated then. Messages go out through `Emitter` or `MutinyEmitter`, broker-specific keys and headers through the connector metadata types. A publication is marked externalized only after the broker acknowledgement completes. Emitting never blocks an I/O thread.
* Why: Quarkus Messaging does not support dynamic channel lookup, and marking before the ack loses messages on broker failure.
* Check: a Dev Services test stops the broker mid-flight and asserts the publication stays incomplete.
* Source: [MESSAGING], [MIG4] section "Core build API removals and replacements".

### INT-06 Health checks

* Rule: health checks are readiness checks, optional (`quarkus-smallrye-health` optional in runtime, `HealthBuildItem` in deployment), and can be disabled with `quarkus.modulith.health.enabled` (default `true`).
* Why: documented extension pattern. Liveness must stay cheap.
* Check: TST-06 with and without SmallRye Health.
* Source: [WE] section "Extension Health Check".

### INT-07 Non-application endpoints

* Rule: endpoints such as `/q/modulith` are declared with `NonApplicationRootPathBuildItem.routeBuilder()` with a relative path and `displayOnNotFoundPage()`, and the HTTP dependency is optional.
* Why: the non-application root is configurable and shared with health, metrics and OpenAPI.
* Check: a test with `quarkus.http.non-application-root-path` changed.
* Source: [WE] section "Extension-defined endpoints".

---

## 12. Operations (OPS)

### OPS-01 JBoss Logging, parameterized

* Rule: extension code logs with `org.jboss.logging.Logger` and the `*f` or `*v` methods. No SLF4J, `System.out` or `java.util.logging`. Logger categories start with `io.quarkiverse.modulith`.
* Why: Quarkus logging configuration and native support are built around JBoss Logging.
* Check: `grep -rn 'org.slf4j\|System.out\|System.err\|java.util.logging' --include='*.java' */src/main runtime/src/main deployment/src/main` prints nothing.
* Source: [LOGGING], [MATRIX] "Logging".

### OPS-02 Do not log payloads

* Rule: log messages at INFO and above identify events by type, listener id and publication id. They do not include the event's `toString()`, which may contain personal data. Payloads may appear at DEBUG or TRACE.
* Why: logs are shipped and retained far more widely than the database.
* Check: `grep -rn 'LOG\.\(info\|warn\|error\)f\?(.*event)' --include='*.java' runtime events*` prints nothing.
* Source: [LOGGING].

### OPS-03 Keep startup observers cheap

* Rule: `StartupEvent` observers and `@Startup` methods do only bounded, fast work. Work proportional to stored data (resubmitting a backlog, scanning tables) runs after startup on a background thread, in pages, and does not delay readiness. Failures are logged once with context.
* Why: synchronous startup observers delay the moment the application serves traffic, and an unbounded backlog read can exhaust memory at the worst moment (after a crash).
* Check: a test with a large backlog measures startup time against PERF-02.
* Source: [LIFECYCLE] sections "Listening for startup and shutdown events", "Using `@Startup` to initialize a CDI bean at application startup".

### OPS-04 Graceful shutdown

* Rule: in-flight asynchronous work stops taking new tasks when shutdown starts and drains within `quarkus.shutdown.timeout`, through a `ShutdownListener` (`ShutdownListenerBuildItem`) or the service stop hooks `onGracefulPreShutdown` and `onGracefulShutdown`. Work that did not drain stays recoverable (the publication stays incomplete) and is logged once as a count.
* Why: rolling deployments stop instances all the time. Without draining, every deploy leaves incomplete publications or loses in-memory deliveries.
* Check: a test stops the application while a slow listener runs and asserts the listener completed or the publication is incomplete.
* Source: [LIFECYCLE] section "Graceful Shutdown", [WE] section "Teardown & Shutdown Handlers".

### OPS-05 Background failures do not die silently

* Rule: background jobs catch exceptions per run, log them once with what was being done, and keep the schedule alive. Repeated identical failures are not logged at ERROR on every run without a back-off or a counter.
* Why: a crashed job is invisible, and a log flood hides everything else.
* Check: code review plus a test with a failing repository.
* Source: [MATRIX] "Logging".

---

## 13. Performance, memory and startup budget (PERF)

### PERF-01 Cheap hot paths

* Rule: the per-event path for an event type with no module listener does one map lookup and returns, without allocation. Per-request and per-transaction code adds no work when the extension has nothing to do (see CDI-07).
* Why: the dispatcher sits on every event of the application.
* Check: a JMH or simple loop benchmark in the review shows no allocation for unlistened events (`-prof gc`).
* Source: [MATRIX] "Hot path performance optimization".

### PERF-02 Budgets for the shop integration application

These are project budgets, calibrated in the first performance review and then enforced. Measure the shop application against the same application with the Modulith dependencies removed (or with every listener and module annotation left in place but `quarkus-modulith*` excluded where that still compiles).

| Metric | JVM budget | Native budget |
|---|---|---|
| Sum of `io.quarkiverse.modulith` build step times (`build-metrics.json`) | 500 ms | same steps, 500 ms |
| Startup time added (`started in`, median of 5) | 50 ms and at most 5% | 10 ms |
| RSS added after startup and one order flow | 8 MB | 4 MB |
| Native executable size added | not applicable | 2 MB |

* Why: Quarkus users pick it for startup and memory. An extension that silently costs more loses that.
* Check: the commands in PERF-04, run on the same machine for both variants.
* Source: [MATRIX] "Supersonic subatomic performance".

### PERF-03 Bounded memory

* Rule: runtime memory is proportional to the model (modules, listeners, event types), never to traffic, except for explicitly bounded buffers (test capture, retry queues). Test-only buffers exist only in TEST launch mode. No Jandex index, class bytes or ASM data is retained at runtime.
* Why: long-running services must have flat memory.
* Check: a heap histogram after 10,000 events shows no extension class growing.
* Source: [WE] section "Favor build time work over runtime work".

### PERF-04 How to measure

* Build steps: `./mvnw -pl integration-tests/shop package -Dquarkus.builder.metrics.enabled=true`, then read `target/build-metrics.json` and sum the durations of steps whose class starts with `io.quarkiverse.modulith`.
* Startup: run the packaged application five times, take the median of the `started in` value. For detail, `-Dquarkus.debug.print-startup-times=true`.
* RSS: `ps -o rss= -p <pid>` after startup and after one order flow through the REST API, on JVM (`java -jar target/quarkus-app/quarkus-run.jar`) and native (`target/*-runner`).
* Native size: `ls -l target/*-runner`.
* Record machine, JDK, GraalVM or Mandrel version and the raw numbers in the review.

---

## 14. CI and release (CI)

### CI-01 Build like CI, before every merge

* Rule: CI runs `./mvnw -B clean verify -Dno-format` (format validation through the Quarkiverse parent) on JDK 21 and 25, plus the native job. Locally, the same command, with the integration tests and the coverage gate, passes before a change is merged.
* Why: format, tests and coverage gates are what keep parallel packages mergeable.
* Check: green CI and a local run recorded in the review.
* Source: [HUB-NEW] "Start coding".

### CI-02 Track Quarkus snapshots

* Rule: the `quarkus-snapshot` workflow stays enabled and the repository joins Quarkus Ecosystem CI once in Quarkiverse.
* Why: breakage against upcoming Quarkus releases shows up early.
* Check: `.github/workflows/quarkus-snapshot.yaml` exists.
* Source: [HUB-NEW] "After the repository is created".

### CI-03 Workflow hygiene

* Rule: workflow files carry no comments, request least privileges (`permissions:`), and use released major versions of actions.
* Why: project convention and supply chain hygiene.
* Check: `grep -n '#' .github/workflows/*.y*ml` prints nothing.
* Source: project convention.

---

## 15. Review checklist

Use this list for every review. Each line names the rules it checks.

Layout and metadata
- [ ] Runtime and deployment pairs complete, optional flags mirrored (PRJ-01, PRJ-05)
- [ ] No build-time library in runtime dependency trees (PRJ-02)
- [ ] `quarkus-extension.yaml` complete, capabilities declared (PRJ-03, PRJ-04)
- [ ] No imports of `.runtime` or `.deployment` internals from other modules (PRJ-07)
- [ ] New IT modules in the `it` profile with deployment `pom` dependencies (PRJ-08, PRJ-10)

Build steps and startup
- [ ] No new index of our own, no repeated full-index scans, no nested annotation loops (BLD-03, BLD-04)
- [ ] No class loading or `ClassNode` in build steps (BLD-05)
- [ ] No file writes outside generated-resource build items (BLD-02)
- [ ] Build items final and immutable (BLD-06)
- [ ] Validation through `ValidationErrorBuildItem`, messages tested (BLD-08)
- [ ] No new `@Record` code, service lambdas capture only allowed values and reference runtime classes only (SVC-01 to SVC-03)
- [ ] No static state filled at startup (SVC-05)
- [ ] Every started thread or executor has a stop handler (SVC-07)

Configuration
- [ ] `@ConfigMapping` only, phase justified, Javadoc on every property, docs regenerated (CFG-01 to CFG-03)
- [ ] Prefix owned by one artifact (CFG-04)

CDI
- [ ] No private injection points or observers (CDI-02)
- [ ] No catch-all observers in production code (CDI-07)
- [ ] Defaults replaceable with `@Reserve` on Quarkus 4 (CDI-04)
- [ ] Optional collaborators decided at build time (CDI-09)

Native
- [ ] Every reflective registration has a reason and minimal members (NAT-02)
- [ ] Serialized types registered with their hierarchy and subtypes (NAT-03)
- [ ] Native `@QuarkusIntegrationTest` for each runtime feature (NAT-06)

Dev mode
- [ ] `QuarkusDevModeTest` for reload behavior (DEV-01)
- [ ] Dev UI steps on `IsLocalDevelopment`, live data through JSON-RPC in `runtime-dev` (DEV-05)

Threading and lifecycle
- [ ] No blocking on I/O threads, tested (THR-01)
- [ ] Executor configurable, virtual threads supported and pinning-tested (THR-02, TST-08)
- [ ] Startup observers bounded (OPS-03)
- [ ] Graceful shutdown drains or leaves work recoverable (OPS-04)
- [ ] No payloads in INFO+ logs (OPS-02)

Persistence and integrations
- [ ] Persistence unit configurable, queries bounded and in the database (TX-02, TX-03)
- [ ] Jackson 3 on Quarkus 4, stored format behind the serializer SPI (INT-01)
- [ ] Metrics, tracing, health optional and switchable (INT-02, INT-03, INT-06)
- [ ] Messaging acks before marking externalized (INT-05)

Tests and performance
- [ ] Each optional integration tested present and absent (TST-06)
- [ ] No `Thread.sleep` in tests (TST-07)
- [ ] Coverage gate passes (TST-04)
- [ ] Build step, startup and RSS numbers recorded against PERF-02 when the package touches build steps, startup or hot paths (PERF-02, PERF-04)

---

## 16. Sources

Quarkus guides are linked at the `4.0.0.CR1` tag of the Quarkus repository so the text matches the target version. The same guides are published at `https://quarkus.io/guides/<name>`.

* [WE]: Writing your own extension, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/writing-extensions.adoc
* [R2S]: Recorders-to-Services Migration Guide, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/recorders-to-services-migration.adoc
* [MATRIX]: Extension maturity matrix, https://quarkus.io/guides/extension-maturity-matrix
* [META]: Quarkus extension metadata, https://quarkus.io/guides/extension-metadata
* [CDI-INT]: CDI integration guide, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/cdi-integration.adoc
* [CDIREF]: CDI reference, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/cdi-reference.adoc
* [CFGMAP]: Mapping configuration to objects, https://quarkus.io/guides/config-mappings
* [DEVUI]: Dev UI, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/dev-ui.adoc
* [DEVSVC]: Writing a Dev Service, https://quarkus.io/guides/extension-writing-dev-service
* [TESTING]: Testing your application, https://quarkus.io/guides/getting-started-testing
* [CONTTEST]: Continuous testing, https://quarkus.io/guides/continuous-testing
* [NATREF]: Native reference, https://quarkus.io/guides/native-reference
* [NATTIPS]: Tips for writing native applications, https://quarkus.io/guides/writing-native-applications-tips
* [VT]: Virtual thread support reference, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/docs/src/main/asciidoc/virtual-threads.adoc
* [LIFECYCLE]: Application initialization and termination, https://quarkus.io/guides/lifecycle
* [LOGGING]: Logging configuration, https://quarkus.io/guides/logging
* [TX]: Using transactions in Quarkus, https://quarkus.io/guides/transaction
* [ORM]: Using Hibernate ORM and Jakarta Persistence, https://quarkus.io/guides/hibernate-orm
* [PANACHE]: Simplified Hibernate ORM with Panache, https://quarkus.io/guides/hibernate-orm-panache
* [SCHED]: Scheduler reference, https://quarkus.io/guides/scheduler-reference
* [SCHED-SRC]: `SchedulerProcessor` at 4.0.0.CR1, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/extensions/scheduler/deployment/src/main/java/io/quarkus/scheduler/deployment/SchedulerProcessor.java
* [MESSAGING]: Quarkus Messaging, https://quarkus.io/guides/messaging
* [MICROMETER]: Micrometer metrics, https://quarkus.io/guides/telemetry-micrometer
* [OTEL]: OpenTelemetry, https://quarkus.io/guides/opentelemetry
* [MIG4]: Migration Guide 4.0, https://github.com/quarkusio/quarkus/wiki/Migration-Guide-4.0
* [HUB-NEW]: Quarkiverse checklist for new projects, https://hub.quarkiverse.io/checklistfornewprojects/
* [HUB-REL]: Quarkiverse release process, https://hub.quarkiverse.io/release/
* [QIT-POM]: example Quarkus integration-test pom, https://github.com/quarkusio/quarkus/blob/4.0.0.CR1/integration-tests/hibernate-orm-panache/pom.xml
* Local evidence: `io.quarkus:quarkus-builder:4.0.0.CR1` (`BuildMetrics`, property `quarkus.builder.metrics.enabled`), `io.quarkus.arc:arc:4.0.0.CR1` (`CurrentManagedContext.fireIfNotNull`), `io.quarkus:quarkus-core-deployment:4.0.0.CR1` (`@Record` is `@Deprecated(since = "4.0")`), inspected with `javap` from the local Maven repository.
