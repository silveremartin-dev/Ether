# Technical Specifications - Human Society Simulation

## Version
**2.0.0** - Complete rewrite with modern architecture

## Overview
Agent-based simulation modeling human society evolution from prehistory to modern times, inspired by SugarScape with extensions for realistic environmental, economic, and social dynamics.

## Design Goals

1. **Modularity**: Feature-based architecture for easy extension
2. **Performance**: Efficient algorithms for large-scale simulations (1000x1000 grids, 10,000+ agents)
3. **Maintainability**: Clean code, comprehensive tests, extensive documentation
4. **Usability**: Intuitive UI, responsive design, internationalization
5. **Portability**: Cross-platform (Windows, macOS, Linux)

## Technology Stack

### Core
- **Language**: Java 21 (LTS)
  - Virtual Threads for concurrency
  - Records for immutable data
  - Pattern Matching for cleaner code
  - Switch Expressions
- **Build Tool**: Maven 3.9+
- **Version Control**: Git

### Libraries
- **UI**: JavaFX 21
- **JSON**: Jackson 2.15+
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5, AssertJ, Mockito
- **Code Quality**: Spotless (formatting), JaCoCo (coverage)

## Architecture

### Layers

```
┌─────────────────────────────────────┐
│         UI Layer (JavaFX)           │
│  Controls, Visualization, Menus     │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Application Layer              │
│  EventBus, Configuration, I18n      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Simulation Layer                │
│  Climate, Resources, Events         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Model Layer                   │
│  World, Cell, Agent, Resources      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Core Layer                    │
│  Engine, TimeManager, EventBus      │
└─────────────────────────────────────┘
```

### Key Components

#### SimulationEngine
- **Responsibility**: Main game loop, time progression
- **Concurrency**: Uses Virtual Threads for parallel agent updates
- **Thread Safety**: Concurrent collections, atomic operations

#### World
- **Structure**: 2D grid of Cells
- **Size**: Configurable (default 100x100, support up to 2000x2000)
- **Storage**: Array-based for cache efficiency

#### Cell
- **Properties**:
  - Position (x, y)
  - Elevation (-1.0 to 1.0, normalized)
  - Temperature (Celsius)
  - Rainfall (0.0 to 1.0)
  - Biome (enum)
  - Resources (Map<ResourceType, Double>)
  - Population count

#### Agent (Abstract)
- **Types**: Human, Animal
- **Properties**:
  - Unique ID (UUID for humans, sequential for animals)
  - Position
  - Age (months)
  - Health (0-100)
  - Energy (0-100)
- **Behaviors**: Pluggable behavior system

#### Human (extends Agent)
- **Genetics**: String ID representing genetic lineage
- **Culture**: String ID for cultural group
- **Inventory**: Resources carried
- **Behaviors**: Gather, Hunt, Farm, Reproduce, Migrate

## Data Structures

### Spatial Indexing
- **Current**: 2D array (O(1) access)
- **Future**: Quadtree for large sparse worlds

### Agent Storage
- **Current**: CopyOnWriteArrayList (thread-safe)
- **Optimization**: Consider spatial partitioning for collision detection

## Algorithms

### Terrain Generation
- **Method**: Perlin Noise (to be implemented with library)
- **Layers**:
  - Elevation: Multiple octaves for realistic mountains
  - Rainfall: Separate noise map
  - Temperature: Latitude + elevation based

### Climate Simulation
- **Seasonal Cycle**: Cosine function for smooth transitions
- **Latitude Effect**: Temperature gradient from equator to poles
- **Elevation Effect**: Lapse rate (approx. 10°C per normalized unit)

### Resource Renewal
- **Growth Model**: Logistic growth curve
- **Harvest Impact**: Reduces available resources
- **Seasonal Variation**: Growth rate varies by season

### Agent Behaviors
- **Decision Making**: Utility-based AI
  - Evaluate needs (food, shelter, reproduction)
  - Select highest utility action
- **Pathfinding**: A* algorithm (for migration)

## Performance Targets

| Metric | Target | Critical Path |
|--------|--------|---------------|
| Tick Rate (1000x1000 world, 1000 agents) | >10 FPS | Agent updates |
| Memory Usage | <2GB heap | Cell array, agent list |
| Startup Time | <5 seconds | Terrain generation |
| Save/Load | <2 seconds | Serialization |

## Concurrency Model

### Virtual Threads (Java 21)
- **Usage**: One virtual thread per agent update
- **Benefits**: Lightweight, millions of threads possible
- **Scheduler**: Automatic by JVM

### Thread Safety
- **Immutable**: Cell properties (after generation)
- **Concurrent Collections**: Agent list (CopyOnWriteArrayList)
- **Synchronization**: Minimal, prefer lockless algorithms

## Configuration Format

### JSON Schema (simplified)
```json
{
  "world": {
    "width": 100,
    "height": 100,
    "seed": 12345,
    "generationParams": {
      "elevationOctaves": 4,
      "rainfallOctaves": 3
    }
  },
  "simulation": {
    "startYear": -20000,
    "tickRateMs": 1000,
    "speedMultipliers": [1, 5, 20]
  },
  "agents": {
    "initialHumans": 100,
    "initialAnimals": {
      "cow": 50,
      "chicken": 100
    }
  }
}
```

## Internationalization

### Supported Languages
- English (en)
- French (fr)
- Spanish (es)
- German (de)

### ResourceBundle Format
```properties
# messages_en.properties
ui.menu.file=File
ui.menu.simulation=Simulation
ui.button.start=Start
ui.button.pause=Pause
simulation.year={0} {1}  # e.g., "20000 BC"
```

## Logging Strategy

### Levels
- **TRACE**: Detailed debugging (agent decisions)
- **DEBUG**: General debugging (tick events)
- **INFO**: Normal operation (simulation start/stop)
- **WARN**: Potential issues (low resources)
- **ERROR**: Errors (exceptions)

### Format
```
[2024-11-23 14:50:00.123] [INFO] [SimulationEngine] Simulation started at year -20000
```

## Testing Strategy

### Unit Tests
- **Coverage**: 70%+ lines, 80%+ branches
- **Focus**: Core logic, algorithms
- **Tools**: JUnit 5, AssertJ

### Integration Tests
- **Scenarios**: Full simulation cycles
- **Verification**: State consistency

### Performance Tests
- **Benchmarks**: JMH for critical methods
- **Profiling**: VisualVM, JFR

## Security Considerations

### Input Validation
- Configuration files: JSON schema validation
- User inputs: Bounds checking

### Resource Limits
- Max world size: 2000x2000
- Max agents: 100,000
- Max memory: Configurable heap

## Future Enhancements

### Possible Architectural Changes
1. **Entity Component System (ECS)**
   - Better performance for large agent counts
   - More flexible behavior composition
2. **Event Sourcing**
   - Full simulation replay
   - Time travel debugging
3. **Distributed Simulation**
   - Multi-machine for massive worlds
   - Networked multiplayer

### Optimization Opportunities
1. **Spatial Partitioning**: Quadtree for agent queries
2. **Dirty Flagging**: Only update changed cells
3. **Level of Detail**: Simplified updates for distant regions
4. **Caching**: Pre-compute seasonal effects

## Code Conventions

### Naming
- **Classes**: PascalCase (e.g., `SimulationEngine`)
- **Methods**: camelCase (e.g., `updateClimate()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_AGENTS`)
- **Packages**: lowercase (e.g., `com.ether.society.core`)

### Documentation
- **Javadoc**: All public APIs
- **Comments**: Complex algorithms only
- **README**: Each module

### Formatting
- **Indentation**: 4 spaces
- **Line Length**: 120 characters
- **Enforced by**: Spotless Maven plugin

## License
MIT License (see LICENSE file)

## Authors
- Silvere Martin-Michiellot (silvere.martin@gmail.com)
- AI Assistant (Antigravity/Claude)

## Document Version
**1.0** - 2024-11-23
