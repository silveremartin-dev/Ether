# Technical Specifications - Human Society Simulation (Ether)

## Version
**2.0.0** - Premium Edition (Glassmorphism + Density Simulation)

## Overview
Ether is an agent-based simulation modeling the evolution of human society from prehistory to modern times. It leverages a hexagonal geospatial grid (H3), density-based population dynamics (Artemis Layer), and high-performance Java technologies.

## Design Goals
1.  **Immersive UI**: "Glassmorphism" aesthetic, smooth animations, and premium visual feedback.
2.  **Scalability**: Capable of simulating millions of H3 cells using the Artemis density engine.
3.  **Realism**: Data-driven climate (WorldClim), terrain (SRTM), and population models.
4.  **Extensibility**: Modular architecture separating Simulation, Rendering, and Persistence.

## Technology Stack

### Core
-   **Language**: Java 21 (LTS) - Virtual Threads, Records, Switch Expressions.
-   **Build Tool**: Maven 3.9+
-   **Geospatial**: Uber H3 (4.1.1)

### Libraries
-   **UI**: JavaFX 21 (with CSS-driven styling)
-   **JSON**: Jackson 2.15+
-   **Logging**: SLF4J + Logback
-   **Testing**: JUnit 5, Mockito
-   **GPU**: TornadoVM (Planned for Phase 14)

## Architecture

### System Layers
1.  **UI Layer (JavaFX)**: `MainView`, `H3MapCanvas`, `GlassPanel`. Handles visualization and user interaction.
2.  **Application Layer**: `EtherApp`, `Configuration`, `I18n`. Manages lifecycle and wiring.
3.  **Simulation Layer (Artemis)**: `ArtemisSimulationEngine`, `H3ClimateSystem`. The core logic for density updates.
4.  **Persistence Layer**: `GameSaveManager`, `H3CellRepository`. Manages state serialization (SQL/JSON).
5.  **Model Layer**: `H3Cell`, `Nation` (Records/POJOs).

### Artemis Layer (Density Engine)
Instead of tracking millions of individual agents, Artemis tracks **densities** per cell:
-   **Population**: Number of humans.
-   **Resources**: Food, Water, Wood.
-   **Culture**: Vector of cultural traits.
*Note: "Special Agents" (Armies, Diplomats) are tracked individually as overlay entities.*

## Data Structures

### H3 Grid
-   **Resolution**: 6 (~3km edges) to 8 (~0.5km edges).
-   **Storage**: `ArrayList<H3Cell>` ordered by index for cache locality.
-   **Neighbors**: Computed runtime via `H3Service`.

### Persistence Format
-   **World State**: SQL Database (PostGreSQL/H2) for cell data.
-   **Metadata**: JSON for scenario info, time state, and player progress.

## Algorithms

### Simulation Loop (Tick)
1.  **Time Advance**: Increment Year/Month.
2.  **Climate Update**: Calculate Temp/Rain based on Month + Latitude.
3.  **Artemis Density**:
    -   Produces Food (Logistic Growth).
    -   Updates Population (Birth/Death based on Food/Health).
    -   Migrates Population (Flux to higher-desirability neighbors).
4.  **Diplomacy**: Update borders and Nation-states.
5.  **Event Check**: Trigger historical/random events.

### Rendering Painter
-   **2D Mode**: Mercator-like projection of hexagons.
-   **3D Mode**: Isometric projection with elevation extrusions.
-   **Layers**: Biome Colors, Population Heatmap, Temperature Gradient.

## Performance Targets
| Metric | Target | Status |
| :--- | :--- | :--- |
| Tick Rate (175k cells) | >10 TPS | ✅ Achieved |
| FPS (2D) | >30 FPS | ✅ Achieved |
| FPS (3D) | >20 FPS | ✅ Achieved |
| Startup Time | <5s | ✅ Achieved |

## Security & Constraints
-   **Input Validation**: Strict JSON schema loading.
-   **Resource Limits**: Max heap configurable (default 2GB).

## License
**MIT License**
Copyright (c) 2024-2026 Silvere Martin-Michiellot & Gemini AI
