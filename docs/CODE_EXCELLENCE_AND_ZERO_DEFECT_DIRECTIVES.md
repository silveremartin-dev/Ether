# CODE EXCELLENCE, CANONICAL ARCHITECTURAL PATTERNS & ZERO-DEFECT DIRECTIVES (ETHER ENGINE)

> **Software Engineering, Architectural & Reliability Standard — Ether Platform (Java 21 LTS)**  
> Standard inspired by **NASA JPL "Power of 10"**, **Data-Oriented Design (DoD / Hexagonal Mesh Buffer)**, **Effective Java (3rd Ed., Joshua Bloch)**, and **Domain-Driven Design (DDD)**.  
> Every directive in this document is mechanically verifiable via the automated audit script `scripts/audit-architecture-compliance.ps1`.

---

## 🎯 1. Purpose & Guiding Principles

A multi-millennial planetary simulation platform modeling coupled geophysics, thermodynamics, resource ecology, demography, and macrohistory across global spatial meshes (H3 hexagons) cannot tolerate structural ambiguity, hidden state mutations, or undocumented architectural patterns.

The purpose of this framework is to establish an unyielding standard of code excellence, cognitive clarity, and zero defect across the entire codebase (663+ main classes, 280+ tests) by guaranteeing:

1. **Strict Canonical Taxonomy across 8 Architectural Archetypes**: Every Java class belongs to one and only one well-defined archetype with explicit naming conventions, lifecycle semantics, and encapsulation contracts.
2. **Immediate Cognitive Fluency & Extensibility**: Any engineer or researcher opening a file immediately recognizes whether it holds mutable state, how it is instantiated, whether it allocates memory in simulation loops, and how it safely coordinates with the rest of the engine.
3. **High Architectural Standards with Pragmatic Extensibility**: Clear boundaries between pure functional calculation, stateful coordination, high-performance cellular buffers, and rich visual presentation.
4. **Mechanical Verifiability & Zero Silent Swallowing**: Zero guesswork; every standard is verified via `scripts/audit-architecture-compliance.ps1`.

---

## 🏛️ 2. The 8 Canonical Architectural Archetypes

Every Java source class in the Ether repository (excluding tests and `package-info.java`) strictly adheres to one of the **8 Canonical Archetypes**:

```
Ether Platform Source Code Taxonomy:
├── 1. Simulation Computation & Strategy (*Engine, *Kernel, *Calculator, *Plugin)
├── 2. Orchestration, Lifecycle & Mediation (*Coordinator, *Manager, *Runner, *Controller, *Scheduler)
├── 3. Construction, Assembly & Procedural Generation (*Factory, *Builder, *Generator, *Assembler)
├── 4. Spatial Tensors, Persistence, Memory Buffers & Catalogs (*Buffer, *Grid, *Table, *Tensor, *Registry, *Repository)
├── 5. Discrete Execution, Interventions, I/O & Ingestion (*Executor, *Dispatcher, *Ingestor, *Codec, *Exporter)
├── 6. Pure Functions, Mathematical Utilities & Formatters (*Helper, *Util, *Formatter, *MetricsHelper)
├── 7. Presentation, Scientific UI & Visual Rendering (*Pane, *Panel, *View, *Renderer, *Canvas, *Dialog)
└── 8. Domain Models, Cellular States, Events & Snapshots (Entities, Records, *Snapshot, *Event, *Config, *Preset, Enums)
```

---

### Archetype 1: `*Engine`, `*Kernel` & `*Calculator` (Simulation Computation & Strategy)

#### Role & Single Responsibility Principle (SRP)
Computes physical, demographic, thermodynamic, or cliodynamic state transitions per simulation tick (`accumulateCellularDerivatives`, `tick(WorldBuffer, ...)`).
- **Zero allocation in hot loops**: No temporary object allocations inside cellular mesh traversal loops.
- **Zero I/O or direct UI dependencies**: Pure computational logic operating on cellular state tensors or aggregate indicators.
- **Explicit lifecycle & reset contract**: Engines that maintain internal state or caches must provide an explicit `resetState()` or lifecycle binding.

#### Sub-families & Canonical Contracts
| Suffix / Archetype | Architectural Pattern | Structural Contract | Canonical Example |
| :--- | :--- | :--- | :--- |
| `*Engine` (Domain Module) | **Strategy / Module** | Implements `SimulationEngineModule`, `ProceduralEnginePlugin` or extends `AbstractSimulationEngine`. Exposes clear metadata (`id()`, `tier()`, `phase()`). | `LiethNppEngine`, `MalthusianDemographicEngine`, `KooijmanDebEngine` |
| `*Engine` (Runtime Kernel) | **Facade / Runtime Engine** | Manages execution across cellular grids (`H3SimulationEngine`). | `H3SimulationEngine` |
| `*Engine` (Analytical Solver) | **Analytical Solver** | Executes complex mathematical assimilation or calibration sweeps (`BayesianInverseCalibrationEngine`, `LyapunovChaosTrackerEngine`). | `LyapunovChaosTrackerEngine` |
| `*Kernel` | **SIMD / Vector Math Kernel** | Pure vector processing and high-throughput cellular operations. | `VectorSimdKernel` |

---

### Archetype 2: `*Coordinator`, `*Manager`, `*Runner` & `*Controller` (Orchestration & Lifecycle)

#### Role & Single Responsibility Principle (SRP)
Orchestrates execution lifecycles (`init`, `start`, `tick`, `pause`, `shutdown`), manages multi-engine dependencies, mediates between UI/headless runners and simulation kernels, and coordinates high-level state transitions.
- **Delegation of low-level math**: Does not perform raw thermodynamic or differential equations directly; delegates computation to Engines and persistence to Repositories.
- **Dependency Injection**: Dependencies are injected via constructor (`final` fields) rather than relying on hidden global singletons.

#### Sub-families & Canonical Contracts
| Suffix | Role | Instantiation & Concurrency Contract |
| :--- | :--- | :--- |
| `*Coordinator` | **Mediator / Pipeline Orchestrator**: Synchronizes simulation stages (e.g. `H3EngineLifecycleAndTickCoordinator`, `ClusterCoordinator`). | Constructed with explicit dependencies; controls execution order and thread synchronization. |
| `*Manager` (Domain) | **State Facade**: Manages domain state or resources (`SimulationSaveManager`, `H3SpatialPartitionManager`). | Bound to a simulation engine or session instance; no mutable hidden static fields. |
| `*Manager` (Service Singleton) | **Client / System Service**: Cross-cutting system management (`I18nManager`, `ThemeManager`). | If singleton: `public final class`, `private` constructor, `private static final T INSTANCE`, and thread-safe. |
| `*Runner` | **Execution Harness**: Drives headless benchmark campaigns, parameter calibrations, or Monte Carlo batches (`HeadlessSimulationRunner`). | Command-line or test runner with explicit entry and exit lifecycles. |
| `*Controller` | **UI / Viewport Controller**: Handles user interactions, camera/scene navigation, and coordinates view models. | Injected with views/canvas references; delegates domain mutations to commands or coordinators. |

---

### Archetype 3: `*Factory`, `*Builder` & `*Generator` (Construction & Procedural Synthesis)

#### Role & Single Responsibility Principle (SRP)
Encapsulates complex instantiation of simulation grids, procedural planetary topography, UI toolbars, and chart series.
- **Procedural Determinism**: Procedural generators (`*Generator`, `*Synthesizer`) must accept an explicit `DeterministicPRNG` or `long seed`. Unseeded `Math.random()` or `new Random()` is strictly prohibited.
- **Clean Construction**: UI and mesh builders assemble and return strongly-typed nodes or records; they do not mutate package-private parent state by side-effects.

#### Distinction Between Factory, Builder, and Generator
1. **`*Factory`**:
   - Produces polymorphic instances, scenario tensors, or host contexts (`PlanetHostContextFactory`, `ScenarioHostContextFactory`).
   - Pure factory methods or immutable injectable factories.
2. **`*Builder`**:
   - Accumulates parameters fluently for complex scenario or dataset creation (`BenchmarkChartSeriesBuilder`, `GroundTruthAuditReportBuilder`).
   - Produces immutable domain objects, reports, or configured UI assemblies.
3. **`*Generator` & `*Synthesizer`**:
   - Generates synthetic geological, hydrological, or demographic surfaces deterministically (`ProceduralPlanetRasterSynthesizer`, `BenchmarkSynthesisChartGenerator`).

---

### Archetype 4: `*Buffer`, `*Grid`, `*Tensor`, `*Registry` & `*Repository` (Memory & Catalogs)

#### Role & Single Responsibility Principle (SRP)
Provides high-performance spatial data structures, cell state tensors, in-memory catalogs, and persistent scenario/historical datasets.

#### Structural Contracts
1. **Cellular Tensors & DoD Buffers (`*Buffer`, `*Grid`, `*Tensor`, `*Table`)**:
   - Data-Oriented Design structures (`WorldBuffer`, `H3SpatialPartitioner`, `DeterministicSpatialAdjacencyTable`).
   - Cache-friendly array layouts; primitive iterations avoiding object wrapper overhead.
2. **Registries (`*Registry`)**:
   - Centralizes discovery and lookup of engines, scenarios, and interventions (`SimulationEngineRegistry`, `HistoricalInterventionRegistry`, `FutureAndExoplanetScenarioRegistry`).
   - Explicit registration, thread-safe discovery, and non-modifiable collection views (`Collections.unmodifiableCollection(...)` or snapshot copies).
3. **Repositories (`*Repository`)**:
   - Data access for empirical historical data, Maddison/HYDE baselines, and save states (`HistoricalBenchmarkDatasetRepository`, `SimulationRunRepository`).
   - Handles file I/O, resource streams, and JSON parsing with robust validation.

---

### Archetype 5: `*Executor`, `*Dispatcher`, `*Ingestor` & `*Exporter` (Commands & I/O)

#### Role & Single Responsibility Principle (SRP)
Applies discrete shocks, manages external data ingestion, exports scientific white papers, or serializes state to disk.
- **Safe Resource Management**: All stream, reader, and raster operations use `try-with-resources`.
- **Security & Integrity**: Strict validation against path traversal (`normalize()`) and bounded memory ingestion for external files.

---

### Archetype 6: `*Helper`, `*Util`, `*Formatter` & `*MetricsHelper` (Pure Functions & Calculations)

#### Distinction Between Pure Static Utilities and Stateful Sub-Calculators

To prevent artificial workarounds while maintaining high code quality, Archetype 6 defines two complementary contracts:

#### Contract A: Pure Stateless Utility (`*Helper`, `*Util`, `*Formatter`)
Used for stateless mathematical functions, geometric conversions, formatters, and algorithmic helpers.
1. Declared `final class` (or `public final class` when exported across packages).
2. Explicit **`private` constructor** preventing accidental instantiation.
3. **100% `static` methods** and **0 instance fields**.
4. **Zero mutable static variables** (only immutable constants like `static final Color`, `Logger`, or math constants).

#### Contract B: Injected Sub-Calculators (`*MetricsHelper`, etc.)
When a helper acts as an injected sub-component of a larger coordinator or aggregator (e.g. `CliodynamicMetricsHelper`, `ThermodynamicMetricsHelper` decomposing `SimulationMetricsAggregator` to respect the 500-line ceiling):
1. Declared `public final class`.
2. Explicit constructor taking its parent/context as `final` injected dependencies (`this.aggregator = Objects.requireNonNull(aggregator)`).
3. Holds only immutable reference fields to its coordinator/aggregator.
4. Contains no global static state.

---

### Archetype 7: `*Pane`, `*Panel`, `*View`, `*Renderer`, `*Canvas` & `*Dialog` (UI & Rendering)

#### Role & Single Responsibility Principle (SRP)
Displays simulation telemetry, cartographic projections, thematic layers, and parameter control panels according to the scientific UI guidelines of `AGENTS.md`.
- **Decoupled from simulation kernels**: UI components observe state changes via listeners or poll snapshots; they never directly run simulation physics loops on the UI thread.
- **Full Localization (I18n)**: All user-facing labels, buttons, tooltips, and dialogs are bound through `I18n` (EN, FR, DE, ES, ZH). Zero hardcoded UI text in Java source.
- **Consistent High-Contrast Theming**: Follows dark/light theme palettes; no hardcoded contrasting background colors bypassing the design system.

---

### Archetype 8: Domain Models, Cellular States, Events & Snapshots

#### Role & Single Responsibility Principle (SRP)
Encapsulates domain records, cellular properties, physical constants, simulation events, and immutable scenario presets.
- **Modern Java 21 Records**: Extensive use of `record` for DTOs, metric snapshots, cluster network messages, and coordinate pairs (`ClusterNodeRecord`, `HistorySnapshot`, `AuthToken`).
- **Domain Invariants**: Constructors validate arguments eagerly (`Objects.requireNonNull`, value bounds).

---

## 🛡️ 3. The 10 Zero-Defect Rules (NASA JPL "Power of 10" Inspired)

| # | Excellence Rule | Target / Invariant | Automated Check |
| :--- | :--- | :--- | :--- |
| **R1** | **Class Size Ceiling** | **$\le 500$ lines** for all production classes in `src/main/java`. Complex files are decomposed into focused modular components. *(Test validation suites and fixture catalogs may exceed 500 lines when documenting exhaustive empirical datasets).* | `audit-architecture-compliance.ps1` (Section 1) |
| **R2** | **Zero Silent Exception Swallowing** | **0 completely silent empty catch blocks**. Every caught exception must either be logged (`logger.debug`/`logger.trace`/`logger.warn`), rethrown, cause thread interruption restoration, or contain an explicit justification. | `audit-architecture-compliance.ps1` (Section 2) |
| **R3** | **Zero Duplicate Class Names** | **0 duplicate class names** across packages in `src/main/java` to guarantee unambiguous imports and refactoring safety. | `audit-architecture-compliance.ps1` (Section 3) |
| **R4** | **Helper & Utility Purity** | Pure `*Helper`, `*Util` and `*Formatter` classes must be `final` with a `private` constructor and zero instance fields. Injected helpers (e.g. `*MetricsHelper`) must be `final` with immutable injected references. | `audit-architecture-compliance.ps1` (Section 4) |
| **R5** | **Canonical Singleton Contracts** | Any singleton offering `getInstance()` must be `final`, provide a `private` constructor, and use a safe `private static final INSTANCE` or synchronized initialization pattern. | `audit-architecture-compliance.ps1` (Section 5) |
| **R6** | **Engine Contract Consistency** | Simulation engines must implement `SimulationEngineModule` or `ProceduralEnginePlugin`, or extend `AbstractSimulationEngine`, avoiding untyped ad-hoc engine designs. | `audit-architecture-compliance.ps1` (Section 6) |
| **R7** | **Zero Allocation in Hot Loops** | No object churn (e.g. streaming, iterator creation, temporary collections) in high-frequency simulation tick loops across cellular grids. | JMH Benchmarks & Code Review |
| **R8** | **Strict Determinism & Seed Anchoring** | Procedural generation and spatial stochastic functions must be strictly reproducible, anchored to scenario seeds via `DeterministicPRNG`. No unseeded `Math.random()`. | `AGENTS.md` § 3 & Determinism Tests |
| **R9** | **Full Localization & High-Contrast UI** | 100% of user-visible strings localized across 5 languages (EN, FR, DE, ES, ZH); every interactive control features an informative technical tooltip. | `AGENTS.md` § 5 & UI Audits |
| **R10**| **Epistemic Invariance & Literature Provenance** | Tier 1 biophysical models derive from validated cross-taxa laws (DEB, MTE, stoichiometry). Departures are documented exclusively in the Parametric Deviation Registry. | `AGENTS.md` § 9 & Falsification Suite |

---

## 🔍 4. Automated Architecture Audit Script

The compliance of the entire codebase against these architectural standards is verified via:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-architecture-compliance.ps1
```

The script performs static analysis on the source tree, verifying file sizes, exception handling, class uniqueness, archetype contracts, and engine implementations.
