# Docs

Base package: `io.quarkiverse.modulith.deployment.docs.fixtures.app`

```mermaid
flowchart LR
    catalog["Catalog"]
    inventory["Inventory"]
    order["Orders"]
    shared["Shared"]
    inventory -- "uses" --> catalog
    inventory -- "uses" --> order
    order -- "uses" --> catalog
    classDef open stroke-dasharray: 5 5
    class shared open
```

| Module | Base package | Type | Depends on | Allowed dependencies |
|---|---|---|---|---|
| Catalog | `io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog` | closed |  | any |
| Inventory | `io.quarkiverse.modulith.deployment.docs.fixtures.app.inventory` | closed | catalog, order | any |
| Orders | `io.quarkiverse.modulith.deployment.docs.fixtures.app.order` | closed | catalog | any |
| Shared | `io.quarkiverse.modulith.deployment.docs.fixtures.app.shared` | open |  | any |
