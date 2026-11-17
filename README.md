# Quarkus Modulith

[![Build](https://github.com/quarkiverse/quarkus-modulith/actions/workflows/build.yml/badge.svg)](https://github.com/quarkiverse/quarkus-modulith/actions/workflows/build.yml)
[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.modulith/quarkus-modulith?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.modulith/quarkus-modulith)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)

Quarkus Modulith brings [Spring Modulith](https://spring.io/projects/spring-modulith) 2.1 to Quarkus. You keep one deployable application, split into modules by package, and the build checks that the modules respect each other's boundaries. Modules talk through events with a transactional outbox, and the build documents the result.

The module model is computed from the Jandex index and the class files during the build. Nothing is scanned at runtime, so native executables behave like the JVM. Annotation names, attributes, rule semantics, violation messages and the `event_publication` table match Spring Modulith, so a migration is mostly an import change. [The migration page](docs/modules/ROOT/pages/spring-modulith.adoc) maps the Spring Modulith 2.1.2 features to their Quarkus counterparts.

## Features

* **Modules and verification.** Each direct sub-package of the base package is a module (or only annotated packages, or a custom strategy). `@ApplicationModule` (`id`, `displayName`, `allowedDependencies`, `type`), `@NamedInterface` (with propagation), nested modules, shared modules, `@Modulith`/`@Modulithic`, `@PackageInfo`, jMolecules `@Module`, and the Spring Modulith annotations themselves during a migration. The build fails on cycles between closed modules, access to internals, and dependencies `allowedDependencies` does not list. `ApplicationModules.of(App.class).verify()` also works in a plain JUnit test, with ArchUnit and jMolecules rules when they are on the class path.
* **Runtime model.** An `ApplicationModules` bean read from a compressed resource on first use, `ApplicationModuleInitializer` beans run in module dependency order at startup, and `/q/modulith` with Spring's actuator JSON (dev and test mode, or on explicit opt-in).
* **Module events.** `@ApplicationModuleListener` runs after the publishing transaction commits, asynchronously, in a new transaction, with `condition`, `propagation`, `readOnlyTransaction` and composed annotations. CDI `AFTER_SUCCESS` observers go through the registry too.
* **Event publication registry.** Every delivery is stored in the publishing transaction (`PUBLISHED`, `PROCESSING`, `COMPLETED`, `FAILED`, `RESUBMITTED`), with republish on restart, scheduled resubmission, staleness monitoring, `UPDATE`/`DELETE`/`ARCHIVE` completion modes, and multi-instance safety through compare-and-set claims. Stores: Hibernate ORM, JDBC (H2, HSQLDB, PostgreSQL, MySQL, MariaDB, SQL Server, Oracle, v1 and v2 schemas), MongoDB and Neo4j.
* **Externalization.** `@Externalized` and `EventExternalizationConfiguration`, in module-listener or outbox mode, marked done only after the broker acknowledged: Kafka, RabbitMQ and AMQP 1.0 through Quarkus Messaging, JMS, and JobRunr. Routing expressions are compiled at build time, including the common Spring SpEL forms.
* **Moments.** `HourHasPassed`, `DayHasPassed`, `WeekHasPassed`, `MonthHasPassed`, `QuarterHasPassed`, `YearHasPassed` (plus seconds and minutes) from the scheduler, and a `TimeMachine` for tests.
* **Observability.** OpenTelemetry spans around calls into a module and Micrometer counters for module events, with Spring Modulith's names, plus gauges per publication status.
* **Documentation.** C4 and UML PlantUML diagrams, Mermaid, module canvases and `all-docs.adoc`, written by the build or by `Documenter` in a plain test, with Javadoc from an annotation processor. The Dev UI shows modules, graph, violations, listeners, initializers and incomplete publications with a resubmit action.
* **Testing.** `@ApplicationModuleTest` with bootstrap modes, `Scenario`, `PublishedEvents` and AssertJ assertions, module component slices on `QuarkusComponentTest`, and change-aware test execution that skips modules whose code did not change.
* **Module-aware Flyway.** One migration folder and history table per module, migrated in dependency order.

## Modules

| Artifact | Purpose |
|---|---|
| `quarkus-modulith` | The core extension: module model, build-time verification, listeners, initializers, docs at build, `/q/modulith`, Dev UI |
| `quarkus-modulith-dev` | Dev UI backend, added by Quarkus in dev mode only (do not declare it) |
| `quarkus-modulith-api` | Annotations and the events API, without any Quarkus dependency |
| `quarkus-modulith-core` | Module model and verification, usable in a plain JUnit test (`ApplicationModules.of`) |
| `quarkus-modulith-events` | Registry core: lifecycle, jobs, repository and serializer SPI, externalization core |
| `quarkus-modulith-events-jpa` | Registry store on Hibernate ORM |
| `quarkus-modulith-events-jdbc` | Registry store on plain JDBC (Agroal), with schemas per database |
| `quarkus-modulith-events-mongodb` | Registry store on MongoDB |
| `quarkus-modulith-events-neo4j` | Registry store on Neo4j |
| `quarkus-modulith-events-messaging` | Externalization through Quarkus Messaging (Kafka, RabbitMQ, AMQP 1.0) |
| `quarkus-modulith-events-jms` | Externalization through a Jakarta Messaging `JMSContext` |
| `quarkus-modulith-events-jobrunr` | Externalization through JobRunr jobs enqueued in the business transaction |
| `quarkus-modulith-events-tck` | Contract tests for registry stores, with an in-memory store (test scope) |
| `quarkus-modulith-moments` | Passage of time events and `TimeMachine` |
| `quarkus-modulith-observability` | Spans and metrics |
| `quarkus-modulith-flyway` | Module-aware Flyway migrations |
| `quarkus-modulith-documenter` | `Documenter` for plain tests |
| `quarkus-modulith-apt` | Annotation processor recording Javadoc for canvases and the Dev UI |
| `quarkus-modulith-junit` | Module tests, scenarios, published events (test scope) |
| `quarkus-modulith-junit-changes` | Change-aware test execution (test scope) |
| `quarkus-modulith-bom` | Version alignment |

## Quick start

The artifacts are published to Maven Central under the group `io.quarkiverse.modulith`. Java 21 or later is required. The extension version follows the Quarkus version it targets, and each Quarkus line has its own branch:

| Quarkus | Quarkus Modulith | Branch |
|---|---|---|
| 4.0 | 4.0.x (pre-releases such as 4.0.0.CR1 until Quarkus 4.0 is final) | `main` |
| 3.40 | 3.40.x | `3.40.x` |

On Quarkus 3, import the BOM next to the Quarkus BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>3.40.1.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>io.quarkiverse.modulith</groupId>
            <artifactId>quarkus-modulith-bom</artifactId>
            <version>3.40.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Add the extensions you need, without versions:

```xml
<dependencies>
    <dependency>
        <groupId>io.quarkiverse.modulith</groupId>
        <artifactId>quarkus-modulith</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkiverse.modulith</groupId>
        <artifactId>quarkus-modulith-events-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkiverse.modulith</groupId>
        <artifactId>quarkus-modulith-junit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Mark the base package (optional) and declare a module:

```java
@Modulith(systemName = "Shop", sharedModules = "common")
public class ShopApplication {
}
```

```java
@ApplicationModule(allowedDependencies = { "order::events", "common" })
package com.acme.shop.inventory;

import io.quarkiverse.modulith.ApplicationModule;
```

Publish with a plain CDI `Event` and listen in another module:

```java
@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> events;

    @Transactional
    public void place(Order order) {
        // ...
        events.fire(new OrderPlaced(order.id()));
    }
}
```

```java
public class InventoryListener {

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        // runs after the order transaction committed, in its own transaction
    }
}
```

If the inventory module used a class from `com.acme.shop.order.internal`, for example with `new PurchaseOrder(event.orderId())` in an `InventoryListener` of `com.acme.shop.inventory.internal`, `./mvnw package` would stop with

```
[ERROR] Failed to execute goal io.quarkus:quarkus-maven-plugin:3.40.1.2:build (default) on project shop: Failed to build quarkus application: io.quarkus.builder.BuildException: Build failure: Build failed due to errors
[ERROR] 	[error]: Build step io.quarkiverse.modulith.deployment.ModelProcessor#moduleModel threw an exception: io.quarkiverse.modulith.core.Violations: - Module 'inventory' depends on non-exposed type com.acme.shop.order.internal.PurchaseOrder within module 'order'!
[ERROR] Method <com.acme.shop.inventory.internal.InventoryListener.on(com.acme.shop.order.events.OrderPlaced)> calls constructor <com.acme.shop.order.internal.PurchaseOrder.<init>(java.lang.String)> in (InventoryListener.java:12)
```

Each violation starts with `- `, followed by one line per reference that causes it.

The full guide is in [docs](docs/modules/ROOT/pages/index.adoc), the public API in [the API page](docs/modules/ROOT/pages/api.adoc), and the migration from Spring Modulith in [the migration page](docs/modules/ROOT/pages/spring-modulith.adoc). [ARCHITECTURE.md](ARCHITECTURE.md) explains how the extension works.

## Building

```
./mvnw verify
```

The build needs Java 21 or later and Docker, because the integration tests start databases and brokers with Dev Services. It fails below 100% line or branch coverage; the merged report is in `coverage/target/site/jacoco`. The extension builds against Quarkus 3.40.1.2. The Quarkus 4 line is built from `main`.

To build and test the native executables on Linux, run

```
./mvnw verify -Dnative -Dquarkus.native.container-build=true -pl integration-tests/shop,integration-tests/violations -am
```

On macOS the Linux executable cannot run on the host, so the tests run it in a container image instead:

```
./mvnw verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=linux/arm64/v8 -pl integration-tests/shop -am
```

## Releasing

Releases follow the [Quarkiverse release process](https://hub.quarkiverse.io/release/). `main` always carries `999-SNAPSHOT`. To release, open a pull request that sets `current-version` (the version to release) and `next-version` (`999-SNAPSHOT`) in `.github/project.yml`. The `Quarkiverse Pre Release` check validates the versions; merging the pull request runs `Quarkiverse Prepare Release`, which tags the release and creates the GitHub release, and the tag triggers `Quarkiverse Perform Release`, which publishes the artifacts to Maven Central. Each line is released from its own branch: Quarkus 4 from `main`, Quarkus 3.40 from `3.40.x`, through a pull request to that branch.

## License

Apache License 2.0. See [LICENSE](LICENSE).
