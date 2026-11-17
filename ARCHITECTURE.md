# Architecture

This document explains how Quarkus Modulith is built and why. User documentation lives in `docs/`, and [QUARKUS-GUIDELINES.md](QUARKUS-GUIDELINES.md) lists the rules every change is reviewed against.

## Goals

1. Give Quarkus applications the module model of Spring Modulith: modules are the direct sub-packages of a base package (or the annotated packages, or what a custom strategy says), a module exposes its base package and named interfaces, and the rules are checked automatically.
2. Do the expensive work at build time. The module model comes from the class files of the application archives during augmentation. The runtime reads a compact precomputed model. No classpath scanning happens at runtime, so native executables behave like the JVM.
3. Let modules talk through events with the guarantees of Spring Modulith's `@ApplicationModuleListener` and its event publication registry, on any of its stores, and externalize them to brokers.
4. Keep the migration from Spring Modulith short. Annotation names, attributes, rule semantics, violation messages, configuration names and the outbox table layout match. Where they cannot, `spring-modulith.adoc` lists the difference.
5. Cost nothing for features an application does not use.

## Prior art

I checked GitHub, Maven Central, the Quarkiverse organization, the `quarkusio/quarkus` issues and discussions, and the web, in November 2026.

1. There is no Quarkus extension for application modules. The Quarkiverse organization has none, Maven Central has no artifact other than Spring's, and no extension proposal exists in `quarkusio/quarkus`.
2. Sample projects show how to structure a modular monolith on Quarkus but enforce nothing: `doogle-oss/quarkus-modulith` (an application, not an extension), `ketola/quarkus-modular-monolith` (a project template with one database per module) and `Sandrolaxx/quarkus-modulth-sample` (Maven modules).
3. Discussion [quarkusio/quarkus#51183](https://github.com/quarkusio/quarkus/discussions/51183) ("Modular monolith with Quarkus") asked for a Spring Modulith equivalent. The suggested answer was hand-written ArchUnit tests and CDI events. The same author proposed the topic for Quarkus Insights ([quarkusio/quarkus-insights#307](https://github.com/quarkusio/quarkus-insights/issues/307)).
4. The Quarkus `modularity` guide is about JPMS module graphs for extensions such as jlink. It is experimental and does not deal with package boundaries inside an application.
5. Debezium's Quarkus outbox extension publishes outbox rows to a broker through change data capture. It targets messaging between services and needs Debezium. It does not deliver in-process events to listeners in the same application.
6. ArchUnit can express the same rules, but every team writes and maintains them, and the rules run in a test, not in the build.

Conclusion: the extension fills an empty spot. It complements Debezium's outbox (in-process delivery versus external messaging) and replaces hand-written ArchUnit rules with the conventions Spring Modulith users already know.

## Repository layout

```
api/                      quarkus-modulith-api                annotations and events API, plain jar
core/                     quarkus-modulith-core               module model, detection, verification, plain jar
runtime/, deployment/     quarkus-modulith                    build-time model and verification, listeners, initializers,
                                                              docs at build, /q/modulith, Dev UI
runtime-dev/              quarkus-modulith-dev                Dev UI backend, dev mode only
events/                   quarkus-modulith-events             registry, lifecycle, jobs, repository and serializer SPI,
                                                              externalization core
events-jpa/               quarkus-modulith-events-jpa         Hibernate ORM store
events-jdbc/              quarkus-modulith-events-jdbc        JDBC store on Agroal, schemas for 7 databases
events-mongodb/           quarkus-modulith-events-mongodb     MongoDB store
events-neo4j/             quarkus-modulith-events-neo4j       Neo4j store
events-messaging/         quarkus-modulith-events-messaging   externalization through Quarkus Messaging
events-jms/               quarkus-modulith-events-jms         externalization through JMS
events-jobrunr/           quarkus-modulith-events-jobrunr     externalization through JobRunr jobs
events-tck/               quarkus-modulith-events-tck         repository contract tests, in-memory repository
moments/                  quarkus-modulith-moments            passage of time events, TimeMachine
observability/            quarkus-modulith-observability      spans and metrics
flyway/                   quarkus-modulith-flyway             module-aware Flyway
documenter/               quarkus-modulith-documenter         Documenter for plain tests
apt/                      quarkus-modulith-apt                Javadoc annotation processor
junit/                    quarkus-modulith-junit              module tests, Scenario, PublishedEvents, assertions
junit-changes/            quarkus-modulith-junit-changes      change-aware test execution
bom/                      quarkus-modulith-bom                version alignment
coverage/                 quarkus-modulith-coverage           merged JaCoCo report and the 100% gate, not deployed
integration-tests/        one application per feature, not deployed
docs/                     Antora module, not deployed
```

Every extension is a runtime and deployment pair. The plain jars have no deployment module. Dependencies point one way: `api` is used by `core`, `core` by `quarkus-modulith`, which is used by `quarkus-modulith-events`, which the stores and transports build on. `documenter` and `junit-changes` only need `core`.

Coordinates follow the Quarkiverse naming conventions: groupId `io.quarkiverse.modulith`, root package `io.quarkiverse.modulith`, artifactIds starting with `quarkus-`. The parent is `io.quarkiverse:quarkiverse-parent`. The BOM inherits from that parent directly, so importing it does not pin Quarkus versions.

Optional integrations (Narayana, Hibernate ORM, Jackson, the scheduler, vertx-http, Micrometer, OpenTelemetry, virtual threads) are optional dependencies on the runtime side and detected through capabilities on the deployment side. The extensions share their own capability names through `io.quarkiverse.modulith.runtime.ModulithCapabilities`. Every store provides `io.quarkiverse.modulith.events.repository` (`ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY`) in its extension descriptor, and the registry, the Dev UI publications page and the publication gauges check it.

## Build time

### Module model

The deployment module has one processor per concern: `ModelProcessor` builds and verifies the model, `ListenerProcessor` handles listeners and observers, `InitializerProcessor` orders `ApplicationModuleInitializer` beans, `ModuleTestProcessor` excludes modules in module tests, `DocsProcessor` writes the documentation, and `ModulithDevUiProcessor` feeds the Dev UI. Each config root sits next to its processor.

The analysis lives in `quarkus-modulith-core`, so the build and a plain JUnit test (`ApplicationModules.of(App.class)`) produce the same model. `ModelAnalyzer` reads class files with ASM (an optional dependency of core, so it is not part of applications or native images unless the build needs it). The build passes it the class bytes of every application archive and uses Jandex only to find the `@Modulith` type. The analysis:

1. resolves the base package (`quarkus.modulith.base-package`, then `@Modulith`/`@Modulithic`, then the common package prefix of the root archive) and `additionalPackages`;
2. detects modules with the configured strategy: direct sub-packages, explicitly annotated packages (`@ApplicationModule` or jMolecules `@Module`), a strategy class, or one registered through `ServiceLoader`; `ApplicationModuleSourceFactory` services can contribute modules from libraries. Nested modules, shared modules, fully qualified ids and `@PackageInfo` follow Spring Modulith;
3. builds named interfaces from `@NamedInterface` on packages and types, including propagation through public signatures;
4. records every type reference with its kind (`USES_COMPONENT`, `ENTITY`, `EVENT_LISTENER`, `DEFAULT`), published events with the methods that create them, listened events, beans with stereotypes, aggregates and value types, and entry points;
5. reads the Spring Modulith annotations too (`quarkus.modulith.spring-annotations.enabled`), so a codebase can keep them during a migration.

`SpringModulithParityTest` runs 15 fixtures through Spring Modulith 2.1.2 (test scope only) and through core, and compares modules, interfaces, typed dependencies, violations, events, aggregates and beans.

### Verification

`ModuleVerifier` applies Spring Modulith's rules: no cycles between closed modules (Tarjan, one violation per cycle), no access to non-exposed types, allowed dependencies (`module`, `module::interface`, `module::*`), nested module access, shared modules, and declaration checks. `VerificationOptions` and `quarkus.modulith.verification.additional-rules` add ArchUnit rules or `ModulithRule` classes, and the jMolecules DDD, hexagonal, layered and onion rules run when the libraries are present.

With `quarkus.modulith.verification.mode=fail` (default) the build step throws `Violations`, which stops the build and shows on the dev mode error page. `warn` logs, `disabled` skips.

### The model at runtime

The build writes the model with `ModelCodec` to the compressed resource `META-INF/quarkus-modulith/model.bin`, registers it as a native image resource, and `ApplicationModulesRuntimeProducer` decodes it when the `ApplicationModules` bean is first used. An application that never injects the bean never reads it. For an application of 2,000 classes in 20 modules the earlier recorded model was a 2.1 MB static-init class and broke the 64 KB method limit on Quarkus 4. The resource is 82 KB. The Spring JSON export (`META-INF/quarkus-modulith/application-modules.json`) is written only with `quarkus.modulith.export.enabled=true`.

`InitializerProcessor` sorts `ApplicationModuleInitializer` beans by module dependency order at build time, and `ModuleInitializerInvoker` runs them from a `StartupEvent` observer.

### Native-safe verification

`ApplicationModules#verify` and `#detectViolations` also run in native executables, against the decoded model. The evaluation of ArchUnit and jMolecules rules sits behind `RuleSupport`, whose signatures name no ArchUnit type. When ArchUnit or the jMolecules rules are missing from the native image, GraalVM substitutions in `runtime/.../graal` (`Target_RuleSupport_WithoutArchUnit`, `Target_RuleSupport_WithoutJMoleculesRules`, active through `onlyWith` conditions) replace those methods: no jMolecules rules, and a clear error for ArchUnit rules, which cannot exist without ArchUnit anyway. So the image links without the optional libraries. The shop and violations applications check their violations natively in CI.

### Listeners and observers

`@ApplicationModuleListener` classes get `@Singleton` when they have no scope. The processor validates each listener method, resolves composed (meta-annotated) listener annotations, compiles `condition` expressions into generated predicate classes, and creates an ArC invoker for it. A generated `ListenerInvokers` bean creates every invoker and condition with constructor calls, so there is no reflection and no `Method.invoke`.

There is no `@Observes Object` observer. During ArC's observer registration phase the processor registers one synchronous and one asynchronous synthetic observer per listened event type, whose notify method calls `ModuleEventObservers.notify` with the observed type. Events nobody listens to never reach Modulith code. Context lifecycle events are dropped before the dispatcher is looked up. Only in TEST launch mode does the test support register its own recorder for `PublishedEvents`.

With a store present, CDI observers notified `AFTER_SUCCESS` in the application archive are vetoed through an `ObserverTransformerBuildItem` and delivered by the dispatcher instead, so their deliveries are stored like Spring's `@TransactionalEventListener` ones (`quarkus.modulith.events.registry-trigger-annotation` restricts this).

### Documentation

`DocsProcessor` writes `components.mmd`, `components.puml` (C4 or UML), `module-<id>.puml`, `module-<id>.adoc` canvases, `all-docs.adoc` and `modules.md` to `target/modulith-docs`, through `GeneratedFileSystemResourceBuildItem`, when packaging or on request in dev mode. The writers are those of `quarkus-modulith-documenter`, which also offers Spring's `Documenter` API in plain tests. Canvases include configuration properties found with Jandex and Javadoc recorded by `quarkus-modulith-apt` in `META-INF/quarkus-modulith/javadoc.json`.

### Dev UI

`ModulithDevUiProcessor` (only in local development) adds a card with build-time data and pages for the module grid, a force graph, violations, listeners, initializers and the Mermaid source. With a store it adds an incomplete publications page with a resubmit action. Its JSON-RPC backend, `ModulithJsonRpcService`, lives in `quarkus-modulith-dev` (`runtime-dev/`), which `quarkus-modulith` declares as a conditional dev dependency, so it is never part of a production application.

### Module tests

When an `ApplicationModuleTestProfile` sets `quarkus.modulith.test.module`, `ModuleTestProcessor` computes the modules to keep (the module, shared modules, extra modules, and direct or transitive dependencies depending on the bootstrap mode), optionally verifies them, and produces an `ExcludedTypeBuildItem` for every other module. ArC then leaves their beans out. Hibernate ORM entities of excluded modules stay mapped, because Hibernate ORM's build items cannot remove classes from a persistence unit.

## Runtime

### Dispatcher

`ModuleEventDispatcher` receives events from the synthetic observers. For each event it:

1. resolves the listeners of the observed type from the build-time `ListenerTable` and applies their conditions (and `ConditionalEventListener`);
2. stores one publication per listener through the registry, in the publishing transaction. Without a transaction on a Vert.x I/O thread it moves storing and synchronous listeners to the listener executor first;
3. after the commit (a JTA `Synchronization`, or the store's own `afterCommit` hook, which the MongoDB store uses to run after its session commits) marks the publication `PROCESSING` in a short transaction of its own and submits the delivery;
4. invokes the listener on the listener executor (the worker pool, or virtual threads with `quarkus.modulith.listeners.executor=virtual-threads`), in a new transaction according to `propagation`, read-only when asked, and marks the publication completed inside the listener's transaction (`in-listener`) or right after it. On failure the publication is marked `FAILED` in a new transaction.

At shutdown a `ShutdownEvent` observer stops new deliveries and waits up to `quarkus.modulith.listeners.shutdown-timeout` (2 s) for those in flight. `TransactionSupport` hides JTA: `JtaTransactionSupport` with the Narayana capability, `NoTransactionSupport` otherwise.

### Event publication registry

`DefaultEventPublicationRegistry` in `quarkus-modulith-events` implements the registry and the `IncompleteEventPublications`, `CompletedEventPublications`, `FailedEventPublications` and `AbandonedEventPublications` APIs on top of one SPI, `EventPublicationRepository`. Each store extension provides a repository bean and the capability above. Every store passes the contract suite in `quarkus-modulith-events-tck`, in the `UPDATE`, `DELETE` and `ARCHIVE` completion modes.

Two parts of the SPI matter for several instances and large tables:

* **Compare-and-set claim.** `EventPublicationRepository#claim(identifier, eligible, staleBefore, claimDate)` is one conditional update to `RESUBMITTED`. It succeeds only while the publication is `PUBLISHED` or `FAILED`, or `RESUBMITTED`/`PROCESSING` with a last publication date older than the staleness threshold, which takes over a delivery whose instance died. A publication another instance claimed or is delivering is never claimed twice. Under READ COMMITTED the losing update re-evaluates the condition on the committed row and matches nothing; a serialization failure (SQLState `40001`) counts as a lost claim. The two-instance tests deliver through the real dispatcher on PostgreSQL and MySQL. This is stricter than Spring Modulith 2.1.2, whose claim only excludes `RESUBMITTED`. The legacy JDBC v1 structure cannot claim exclusively.
* **Paged reads.** `findIncompletePublications(PageCriteria)`, the per-listener and per-status variant and `findCompletedPublications(PageCriteria)` read keyset pages ordered by publication date and identifier (`PublicationCursor`). The SPI's default methods filter in memory so that a new store compiles, and every shipped store overrides them with database queries. Events are deserialized lazily.

`EventPublicationJobs` resubmits at startup (in the background, page by page), periodically, and deletes completed publications, through the scheduler when present (`PeriodicJobs`). The staleness monitor marks stale publications `FAILED`. `ResubmissionExclusion` beans keep the registry's resubmissions away from listeners that own their publications, such as the outbox drain.

Events are serialized through the `EventSerializer` SPI, whose default uses the application's Jackson mapper: Jackson 2 on Quarkus 3, the Jackson 3 `JsonMapper` on Quarkus 4. There is no JSON-B fallback. The events deployment module registers listened event types and their subtypes for reflection, which Jackson needs in native executables.

Delivery is at least once. A duplicate needs a crash between the listener's side effect outside the database and its commit.

### Externalization

`@Externalized` types and an `EventExternalizationConfiguration` bean select and route events. Routing expressions are compiled into generated classes at build time from a closed grammar (`{event.property}`, `{inject:bean.method(event)}` and the Spring `#{#this…}`/`#{@bean…}` forms); anything else fails the build. In module-listener mode a synthetic listener sends each event and completes the publication when the broker acknowledges. In outbox mode the publication is the outbox row and `OutboxDrain` sends pending rows oldest first, claiming each with the compare-and-set claim and reading one page at a time. Transports implement `EventExternalizationTransport`: Quarkus Messaging with build-generated channels (Kafka, RabbitMQ with publisher confirms, AMQP 1.0), JMS, and JobRunr jobs enqueued in the business transaction. Pulsar is deferred: Spring Modulith 2.1.2 has no Pulsar externalization, and the generic Quarkus Messaging path (emitter plus connector ack) may cover the Pulsar connector, but nothing tests it yet.

`io.quarkiverse.modulith.runtime.events.ListenerExecutor` (worker pool or virtual threads, chosen at build time) is shared beyond the dispatcher: the externalization core in `quarkus-modulith-events` (the module-listener externalizer and the outbox drain) and the messaging, JMS and JobRunr transports run their broker sends, acknowledgement handling and job enqueues on it, so `quarkus.modulith.listeners.executor` applies to externalization too. It is an internal type of `quarkus-modulith`, so changing it means changing those four modules together.

### Other extensions

* `quarkus-modulith-moments` registers scheduler jobs in the configured zone that fire the moment events through CDI, so listeners and the registry apply. `TimeMachine` replaces `Moments` for tests.
* `quarkus-modulith-observability` adds an interceptor binding to module beans at build time, so a call from another module opens a span with Spring Modulith's names, and counts module events through synthetic observers per module event type. With observability off, or neither OpenTelemetry nor Micrometer present, nothing is registered.
* `quarkus-modulith-flyway` migrates each module's folder with its own history table, in dependency order, through a `quarkus-flyway` customizer.
* `/q/modulith` serves Spring's actuator JSON, computed at build time. It is registered in dev and test mode, and in a packaged application only with `quarkus.modulith.endpoint.enabled=true`.

## Native image

Nothing in the runtime scans the classpath or reflects on application classes, with registered exceptions: event types for Jackson and the stored type names in `EventTypes`. The model is a registered resource, invokers and conditions are created by generated code, and the verification links without ArchUnit (see above). The integration-test applications have `@QuarkusIntegrationTest` twins that run against the packaged jar and, with `-Dnative`, against the native executable. CI builds and tests the shop (orders, outbox and shipment recovery, verification) and the violations application natively.

## Compatibility

The extension is built against Quarkus 4.0.0.CR1 with Java 21 bytecode. This branch releases the Quarkus 4 line (2.x); the Quarkus 3.40 line (1.x) is released from the `1.x` maintenance branch, which differs in three places:

1. Jackson. Quarkus 4 ships Jackson 3 (`tools.jackson`), Quarkus 3 Jackson 2. The JSON code sits behind `EventSerializer`; Moments registers its Jackson module through a customizer.
2. Recording. Quarkus 4 deprecates `@Record` and `ExecutionTime` for `ServiceRegistrar`. This branch uses static-init services, `1.x` keeps recorders.
3. Default beans. Quarkus 4 replaces `@DefaultBean` with CDI's `@Reserve`.

CI builds on JDK 21 and builds every integration-test application as a native executable.

## Coverage

Every test JVM runs with the JaCoCo agent, and `@QuarkusIntegrationTest` passes it to the packaged jar through `quarkus.test.arg-line` (not to native executables, which cannot load it). The `coverage` module merges the data of all modules and fails `./mvnw verify` below 100% line or branch coverage, for the bundle and for every class, on both Quarkus lines. The only exclusion is generated code: the `*$$accessor` classes the Quarkus extension processor writes next to each build step class. A test in the coverage module rejects `@Generated` in hand-written sources.

## Public API and the freeze

Public types and their packages are listed in `docs/modules/ROOT/pages/api.adoc`. Their signatures are frozen: they only change in a compatible way. Types in `runtime`, `deployment` and `impl` packages are internal.

The freeze was amended additively. Each amendment is recorded in `api.adoc`:

1. Event publication: new types `AbandonedEventPublications`, `ConditionalEventListener`, `spi.TransactionRunner`, `spi.ReadOnlyTransactions`, `spi.SerializationFailures`, `core.PageCriteria`, `core.PublicationCursor`, `config.CompletionMode` (with `ARCHIVE`) and `config.EventsFixedConfig`. `IncompleteEventPublications#resubmitIncompletePublications(ResubmissionOptions)` is implemented by the registry instead of a throwing default; `@ApplicationModuleListener`'s `condition`, `propagation` and `readOnlyTransaction` are honored. `EventPublicationRegistry` and `EventPublicationRepository` gained `afterCommit`, `deletePublicationsCounting`, paged reads and staleness marking as default methods, so stores written against the frozen SPI keep compiling. The TCK adds the delete and archive mode contracts.
2. Publication claims and paging: `EventPublicationRepository#claim(UUID, Set<Status>, Instant, Instant)`, `#findIncompletePublications(PublicationTargetIdentifier, Set<Status>, PageCriteria)`, the constant `EventPublicationRepository.STALEABLE`, and `core.ResubmissionExclusion`.
3. Module model: Spring's variants under other names where a frozen return type differs (`getJavaPackage()`, `getNamedInterfaceDetails()`, `getPublishedEventTypes()`, `getObjectComparator()`, `getBeans()` with `getSpringBeans()` deprecated).
4. Externalization: `ParsedRoutingTarget`, the implemented `EventExternalizationConfiguration`, and in `events.externalization` the `EventExternalizer` and `ExternalizationStrategy` SPIs with their build items for transport extensions.
5. Documentation, observability and testing: the documenter, observability and test APIs listed in `api.adoc`, `ModulithCapabilities` in `io.quarkiverse.modulith.runtime`, and `ModuleTestRuntimeConfig` in `io.quarkiverse.modulith.testing`.

## Known gaps

The `detectNamedInterfaces` hook of a custom detection strategy, modules spread across several application archives, and jMolecules `@Module` detection with `explicitlyAnnotated()` are implemented without a dedicated test. The event serializer has no JSON-B fallback, Pulsar externalization is deferred because Spring Modulith 2.1.2 has no Pulsar externalization either, module tests leave the Hibernate ORM entities of excluded modules mapped, and Spring Modulith's Namastack integration has no Quarkus counterpart.
