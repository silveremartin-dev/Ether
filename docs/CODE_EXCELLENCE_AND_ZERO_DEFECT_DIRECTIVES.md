# CODE EXCELLENCE, PATTERNS & ARCHITECTURAL PURITY DIRECTIVES (ETHER ENGINE)

## 🎯 1. Purpose & Guiding Principles
This document formalizes the **Zero-Defect Architectural Standard** for the Ether planetary simulation platform.
It establishes unified design patterns across the codebase (688 classes) to guarantee:
1. **Instant Readability & Cognitive Fluency**: An engineer or researcher reading any class immediately recognizes its role, lifecycle, and patterns without surprises.
2. **Structural Consistency Across Layers**: Homogeneous use of Factories, Registries, Coordinators, Strategies, and Executors.
3. **NASA JPL Reliability & Zero-Allocation Invariants**: Deterministic performance, clean boundary contracts, and no object churn in hot simulation loops.
4. **Modularity & Strict Size Ceiling**: Every class must strictly remain $\le 500$ lines of code, adhering to the Single Responsibility Principle (SRP).

---

## 🏛️ 2. The Canonical Architecture & Pattern Catalog

Ether organizes its classes into 6 well-defined structural archetypes. Every class MUST strictly belong to one archetype and follow its naming convention and behavioral contract:

```
                  ┌──────────────────────────────────────────────┐
                  │           Scenario / Configuration           │
                  └──────────────────────┬───────────────────────┘
                                         │ builds via
                                         ▼
                               ┌───────────────────┐
                               │     *Factory      │
                               └─────────┬─────────┘
                                         │ instantiates
                                         ▼
                               ┌───────────────────┐
                               │    *Registry      │
                               └─────────┬─────────┘
                                         │ provides lookup to
                                         ▼
                               ┌───────────────────┐
                               │   *Coordinator    │
                               └─────────┬─────────┘
                                         │ orchestrates
                                         ▼
             ┌───────────────────────────┴───────────────────────────┐
             │                                                       │
             ▼                                                       ▼
   ┌───────────────────┐                                   ┌───────────────────┐
   │ *Engine (Strategy)│                                   │     *Executor     │
   └─────────┬─────────┘                                   └─────────┬─────────┘
             │ operates on                                           │ applies
             ▼                                                       ▼
   ┌───────────────────┐                                   ┌───────────────────┐
   │    *Buffer /      │                                   │     *Service      │
   │   Domain Model    │                                   │  (I/O, Geo, H3)   │
   └───────────────────┘                                   └───────────────────┘
```

### Archetype 1: Engines (`*Engine`) — Pluggable Strategy Pattern
* **Pattern**: Strategy (`SimulationEngineModule` / `ProceduralEnginePlugin`).
* **Role**: Computes physical, demographic, thermodynamic, or cliodynamic state transitions per simulation tick.
* **Rules**:
  - Must implement `SimulationEngineModule` (or `ProceduralEnginePlugin`).
  - No public mutable static state. If internal state is required, provide instance state or an explicit `resetState()`.
  - Zero allocation in `tick(WorldBuffer, ...)` or `process(List<H3Cell>, ...)`: pre-allocate memory and iterate with primitive counters.

### Archetype 2: Coordinators (`*Coordinator`) — Facade & Mediator Pattern
* **Pattern**: Mediator / Facade.
* **Role**: Orchestrates lifecycle steps, synchronizes multi-engine execution order, and mediates between UI/headless runners and simulation kernels.
* **Rules**:
  - Controls lifecycle methods (`init()`, `start()`, `pause()`, `tick()`, `shutdown()`).
  - Contains NO low-level physics math. It delegates computation to Engines and persistence to Exporters/Repositories.

### Archetype 3: Factories (`*Factory`) — Abstract & Static Factory Pattern
* **Pattern**: Factory Method / Abstract Factory.
* **Role**: Encapsulates instantiation logic of complex simulation pipelines, scenario tensor grids, or UI host contexts.
* **Rules**:
  - Parameterized with strongly-typed records/configs, never naked magic strings or loose maps.
  - Returns fully initialized, validated, and immutable or ready-to-run components.

### Archetype 4: Registries & Repositories (`*Registry`, `*Repository`) — Registry Pattern
* **Pattern**: Identity / Lookup Registry.
* **Role**: Manages discovery, lifecycle caches, and lookup of plugins, scenarios, historical benchmark data, and spatial tensors.
* **Rules**:
  - Thread-safe or strictly bounded to simulation thread.
  - Explicit cache eviction and invalidation API (`invalidate()`, `clear()`).

### Archetype 5: Executors (`*Executor`) — Command / Strategy Executor Pattern
* **Pattern**: Command Executor.
* **Role**: Applies discrete actions, historical interventions, or catastrophic physical shocks to the simulation grid.
* **Rules**:
  - Stateless execution or keyed deterministically to `(seed, tick, cellIndex)`.
  - Operates via typed enums (e.g. `PhysicalShockType`, `HistoricalInterventionCategory`).

### Archetype 6: Helpers & Formatters (`*Helper`, `*Formatter`) — Pure Math & Presentation Utilities
* **Pattern**: Pure Functional Utility.
* **Role**: Stateless mathematical transformations, geometric interpolations, or UI localization formatting.
* **Rules**:
  - Private constructor.
  - Final class, 100% thread-safe, static idempotent methods.
  - No mutable static variables.

---

## 🚫 3. Code Anti-Patterns & Prohibited Idioms

| Prohibited Anti-Pattern | Severity | Remediation Pattern |
| :--- | :--- | :--- |
| **"God Helper / God Manager"** ($>500$ lines, mixed I/O, UI & math) | 🔴 CRITICAL | Split into `*Coordinator` (flow) + `*Helper` (pure math) + `*Repository` (data). |
| **Silent Exception Swallowing** (`catch (Exception ignored) {}`) | 🔴 CRITICAL | Replace with `logger.debug(...)` or contextual error handler. |
| **Stringly-Typed Identifiers** (`String pattern = "EGYPT_NILE"`) | 🟠 HIGH | Use strongly-typed `Enum` or `sealed interface` with `fromString(...)`. |
| **Per-Tick Stream/Object Churn** in hot simulation loops | 🟠 HIGH | Use primitive loops (`for (int i = 0; i < N; i++)`), DoD `WorldBuffer`, or pre-allocated collections. |
| **Direct Uncoordinated Static State** in engines | 🟠 HIGH | Centralize engine reset via `SimulationEngineStateCoordinator` or instance lifecycle. |
| **Inconsistent Engine API** (some with `run()`, some `tick()`, some `process()`) | 🟡 MEDIUM | Unify all engines under `SimulationEngineModule` / `ProceduralEnginePlugin`. |

---

## 🔍 4. The 500-Line Limit & Class Splitting Plan

The following classes currently exceed the 500-line ceiling and must be refactored into focused components:

1. **`BenchmarkSynthesisChartGenerator.java`** (1,067 lines)
   - *Split into*:
     - `BenchmarkSynthesisChartGenerator.java` ($\le 450$ lines, main chart composer)
     - `BenchmarkChartSeriesBuilder.java` (series data extraction & statistics)
     - `BenchmarkChartStylingHelper.java` (JFreeChart / JavaFX styling and colors)

2. **`H3RasterAggregator.java`** (695 lines)
   - *Split into*:
     - `H3RasterAggregator.java` ($\le 400$ lines, public facade & spatial routing)
     - `H3RasterSamplingHelper.java` (bilinear / nearest-neighbor raster interpolation)
     - `H3RasterGeometryProjector.java` (equirectangular to H3 polygon transformation)

3. **`GroundTruthAuditGenerator.java`** (651 lines) & **`GroundTruthAuditReportBuilder.java`** (647 lines)
   - *Split into*:
     - Refine modular extraction of statistical tables and metric comparisons into `GroundTruthStatisticsFormatter.java`.

4. **`H3SimulationEngine.java`** (522 lines)
   - *Split into*:
     - Already delegates to `H3EngineLifecycleAndTickCoordinator` and `H3BufferAndEventCoordinator`. Minor cleanup of inline diagnostic logging will bring it well below 450 lines.

---

## 🛠️ 5. Automated Architecture Audit Script

A PowerShell audit script (`scripts/audit-architecture-compliance.ps1`) allows validating all rules:
1. Detects files exceeding 500 lines.
2. Flags `catch (Exception ignored) {}`.
3. Verifies that all `*Engine` classes implement `SimulationEngineModule` or `ProceduralEnginePlugin`.
4. Checks that all `*Helper` classes have private constructors.
