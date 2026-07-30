# 🏗️ Ether Simulation - Technical Architecture

**Version:** 2.0.0-SNAPSHOT  
**Last Updated:** January 29, 2026

---

## 📐 System Overview

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (JavaFX)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ ControlPanel │  │ H3MapCanvas  │  │ Performance  │ │
│  │              │  │  - 2D View   │  │    HUD       │ │
│  │ - Start/Pause│  │  - 3D View   │  │              │ │
│  │ - Speed      │  │  - Biomes    │  │              │ │
│  │ - 2D/3D      │  │  - Contours  │  │              │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Simulation Layer                       │
│  ┌──────────────────────────────────────────────────┐  │
│  │ H3SimulationEngine (orchestrator)                │  │
│  │     ├─ ArtemisSimulationEngine (density flow)    │  │
│  │     ├─ H3ClimateSystem (seasonal cycles)         │  │
│  │     ├─ PoliticalSimulationEngine (nations)       │  │
│  │     ├─ AgentManager (individual entities)        │  │
│  │     └─ TimeManager (20k BCE to Present)          │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│             Data & Persistence Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ DataService  │  │   PostGIS    │  │    Redis     │ │
│  │  (Orchestrer)│  │ (Spatial DB) │  │   (Cache)    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ExternalDataService (SRTM/Map Loading)           │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 Core Components

### 1. H3SimulationEngine

**Purpose:** Manage simulation loop for hexagonal grid  
**Lifecycle:**
```
init() → start() → tick() → pause() → reset()
```

**Tick Loop:**
```java
tick() {
  1. timeManager.advanceMonth()
  2. climateSystem.update(cells) // Future: GPU
  3. eventManager.tick(world)
  4. agents.parallelStream().forEach(agent -> agent.tick())
  5. eventBus.publish(new TickEvent())
}
```

**Thread Model:**
- Main loop: ScheduledExecutorService (single thread)
- Agent updates: Virtual Threads (parallel)
- UI updates: JavaFX Application Thread

---

### 2. H3MapCanvas

**Rendering Pipeline:**
```
setCells(List<H3Cell>) 
  → draw() 
    → findBounds() 
    → calculateScale() 
    → draw2D() | draw3D()
      → getBiomeColor()
      → applyElevationShading() [3D only]
```

**View Modes:**

**2D Mode:**
- Flat Mercator projection
- Simple: `(lng, lat) → (x, y)`
- Performance: 60 FPS (175k cells)

**3D Mode:**
- Isometric projection
- `iso_x = (x - y) * cos(30°)`
- `iso_y = (x + y) * sin(30°) - elevation * 0.05`
- Painter's algorithm (back-to-front sort)
- Performance: ~30 FPS (175k cells)

**Color Mapping:**
```java
OCEAN     → rgb(25, 50, 150)   // Dark blue
FOREST    → rgb(34, 139, 34)   // Forest green
DESERT    → rgb(237, 201, 175) // Sandy tan
MOUNTAINS → rgb(139, 137, 137) // Gray
... (10 biomes total)
```

---

### 3. Data Generation & Climate Simulation

**Procedural Generator Algorithm (`ProceduralGenerator.java`):**
```
For each H3 Cell (lat, lng):
  1. Map (lat, lng) to 3D Cartesian Unit Sphere:
     x = cos(lat) * cos(lng)
     y = cos(lat) * sin(lng)
     z = sin(lat)
  2. Elevation (e): Multi-octave Simplex 3D Noise scaled by user noiseScale
  3. Rainfall (r): 3D Noise combined with Atmospheric Latitude Circulation Belt (latMod):
     - ITCZ Equatorial Rain Belt (|lat| < 10°): latMod = 1.2
     - Subtropical High Deserts (20° < |lat| < 40°): latMod = 0.4
     - Mid-Latitude Westerly Rain (50° < |lat| < 70°): latMod = 0.8
     - Polar Aridity (|lat| > 80°): latMod = 0.2
  4. Temperature (T):
     T = T_avg + T_grad * (cos(lat) - 0.5) - (e * 20.0) [Altitude Lapse Rate]
  5. Biome Classification:
     IF e < waterLevel       -> OCEAN / DEEP_OCEAN
     IF e > 0.8              -> MOUNTAINS
     IF e > 0.5              -> HILLS
     IF T < -5°C             -> SNOW
     IF T < 5°C              -> TUNDRA
     IF r < 0.2              -> DESERT
     IF r < 0.5              -> PLAINS
     IF r < 0.8              -> FOREST
     ELSE                    -> JUNGLE
```

**Satellite Data Pipeline (`OnlineMapService.java` & `ImageMapLoader.java`):**
- Asynchronous fetching of real WMS satellite tiles (USGS, NASA MOLA/Magellan/LRO).
- Local caching & ESRI World File (`.tfw`) geospatial exports.


---

### 4. Module System Integration

**Challenge:** H3 library is non-modular (no `module-info.java`)

**Solution:** JVM Flags
```xml
<!-- Compiler Args -->
<arg>--add-reads</arg>
<arg>com.ether.society=ALL-UNNAMED</arg>

<!-- Runtime Args -->
<option>--add-modules</option>
<option>ALL-MODULE-PATH</option>
<option>--add-reads</option>
<option>com.ether.society=ALL-UNNAMED</option>
```

**module-info.java:**
```java
module com.ether.society {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    // H3 accessed via classpath (no 'requires')
    
    exports com.ether.society;
    exports com.ether.society.h3;
    exports com.ether.society.data;
    exports com.ether.society.database;
}
```

**Why This Works:**
- Named module `com.ether.society` can read ALL-UNNAMED (classpath)
- H3 JAR is on classpath (not module-path)
- JVM bridges the two worlds

---

## 🗄️ Data Model

### H3Cell Entity
```java
@Entity
public class H3Cell {
    @Id Long id;
    String h3Index;         // "881f1a48c7fffff"
    
    // Geometry
    double latitude;
    double longitude;
    Point centroid;         // PostGIS geometry
    
    // Terrain
    double elevation;       // meters (-100 to 8848)
    Biome biome;           // OCEAN, FOREST, etc.
    
    // Climate
    double temperature;     // °C
    double rainfall;        // mm/year
    
    // Resources
    double foodResource;
    double waterResource;
    double woodResource;
}
```

### Simulation Entity
```java
@Entity
public class Simulation {
    @Id Long id;
    @ManyToOne WorldMap worldMap;
    
    int startYear;          // -20000
    int currentYear;
    int currentMonth;
    
    SimulationState state;  // RUNNING, PAUSED
    long totalPopulation;
}
```

---

## ⚙️ Configuration System

**Configuration Structure:**
```java
public record Configuration(
    WorldConfig world,
    SimulationConfig simulation,
    ClimateConfig climate
) {
    public record WorldConfig(
        int width, int height, long seed
    ) {}
    
    public record SimulationConfig(
        int startYear, int tickRateMs
    ) {}
}
```

**Loading:**
```java
// 1. Try user config
Configuration config = ConfigurationLoader.loadConfiguration("config.json");

// 2. Fallback to classpath default
if (config == null) {
    config = ConfigurationLoader.loadDefault();
}
```

---

## 🔀 Event System

**Event Bus Architecture:**
```java
// Publisher
eventBus.publish(new TickEvent(timeManager));

// Subscriber
eventBus.subscribe(TickEvent.class, event -> {
    updateUI(event.timeManager().getFormattedDate());
});
```

**Event Types:**
- `TickEvent` - Every simulation month
- `SimulationResetEvent` - Reset button pressed
- `ViewModeChangedEvent` - 2D ↔ 3D toggle

---

## 🎨 UI Architecture

**JavaFX Scene Graph:**
```
Stage (primaryStage)
└─ Scene (1280x800)
   └─ BorderPane (root)
      ├─ top: Label (info)
      ├─ center: ScrollPane
      │   └─ H3MapCanvas (1200x700)
      └─ bottom: ControlPanel
          ├─ Label (year)
          ├─ Button (Start)
          ├─ Button (Pause)
          ├─ Button (1x/5x/20x)
          └─ Button (2D/3D)
```

**Thread Safety:**
- UI updates: `Platform.runLater()`
- Canvas drawing: JavaFX Application Thread only
- Simulation tick: Background thread

---

## 🚀 Performance Characteristics

### Current (Phase 7)
- **Data Generation:** 2-3 seconds (175k cells)
- **2D Rendering:** ~30 FPS (needs optimization)
- **3D Rendering:** ~15-20 FPS (expected due to sorting)
- **Memory:** ~500 MB heap (175k cells in memory)

### Optimization Opportunities
1. **Viewport Culling:** Only render visible cells
2. **LOD:** Reduce cell count when zoomed out
3. **Canvas Caching:** Pre-render to buffer
4. **GPU Rendering:** TornadoVM for parallel drawing

---

## 📦 Dependency Graph

```
com.ether.society
├── javafx.controls (21)
├── javafx.fxml (21)
├── com.fasterxml.jackson.databind (2.16.0)
├── org.slf4j (2.0.9)
├── ch.qos.logback.classic (1.4.14)
├── com.uber.h3 (4.1.1) ⚠️ non-modular
├── org.springframework.boot:spring-boot-starter-data-jpa (3.2.1)
├── org.postgresql:postgresql (42.7.1)
└── net.postgis:postgis-jdbc (2023.1.0)
```

---

## 🔐 Security Considerations

**Current:**
- All data in-memory (no persistence yet)
- No network exposure
- Local-only JavaFX app

**Future (with Web UI):**
- HTTPS only
- JWT authentication
- CORS configuration
- Rate limiting
- Input validation

---

## 🧪 Testing Strategy

### Unit Tests (Planned)
```
src/test/java/
├── h3/
│   └── H3ServiceTest.java
├── data/
│   └── SampleDataGeneratorTest.java
├── core/
│   └── TimeManagerTest.java
└── util/
    └── ConfigurationLoaderTest.java
```

### Integration Tests (Planned)
```
SimulationEngineTest.java
- testFullSimulationCycle()
- testPauseResume()
- testSpeedChange()
```

---

## 📊 Metrics & Monitoring

**Current Logging:**
```
[INFO ] H3 World generated: 175000 cells
[DEBUG] Drew 175000 cells in VIEW_2D mode
[INFO ] View mode changed to: VIEW_3D
```

**Future Metrics:**
- JMX MBeans for runtime stats
- Prometheus endpoints
- Grafana dashboards
- Performance profiling (JFR)

---

**Last Updated:** 2025-11-24 by Antigravity  
**Maintained by:** Development Team

---

## 🚀 Future Architectural Enhancements

### Entity Component System (ECS)
**Current:** Object-oriented agent model  
**Future:** Data-oriented ECS pattern

**Benefits:**
- 10-100x better performance for 100k+ entities
- Cache-friendly data layout (SoA vs AoS)
- Easy parallelization (process components independently)
- Flexible behavior composition

**Implementation Options:**
1. **Artemis-odb** - Lightweight Java ECS
2. **Ashley** (libGDX) - Battle-tested
3. **Custom** - Tailored to hex-grid needs

**Example:**
```java
// Component-based agent
Position { H3Cell cell; }
Needs { double food, water; }
Behavior { BehaviorTree tree; }

// System processes components
MovementSystem.update(Position, Behavior);
NeedsSystem.update(Needs);
```

### Event Sourcing
**Current:** Mutable state with event bus  
**Future:** Immutable event log

**Benefits:**
- Full simulation replay capability
- Time travel debugging ("go back 500 years")
- Complete audit trail
- State reconstruction from events

**Implementation:**
```java
// Events are facts
AgentMovedEvent { agentId, fromCell, toCell, timestamp }
ResourceConsumedEvent { agentId, resource, amount }

// Rebuild state by replaying
currentState = events.stream()
    .reduce(initialState, State::apply);
```

### Distributed Simulation
**Current:** Single-machine  
**Future:** Multi-machine cluster

**Benefits:**
- Horizontal scaling for massive worlds
- 1M+ cell simulations
- Networked multiplayer potential

**Technologies:**
- **Akka** clustering for agent distribution
- **Apache Kafka** for event streaming
- **Consistent hashing** for world partitioning
- **gRPC** for low-latency communication

### Optimization Opportunities

**1. Spatial Partitioning**
- **Current:** Linear search O(n)
- **Future:** Quadtree/Octree O(log n)
- **Benefit:** Fast neighbor queries, collision detection

**2. Dirty Flagging**
- **Current:** Update all cells every tick
- **Future:** Only update changed data
- **Benefit:** 70-90% reduced computation

**3. Level of Detail (LOD)**
- **Current:** Render all 175k cells
- **Future:** Adaptive cell merging when zoomed out
- **Benefit:** 5-10x rendering performance

**4. Memory Pooling**
- **Current:** New object allocation
- **Future:** Object pools for events, agents
- **Benefit:** 50%+ less GC pressure

**5. GPU Rendering**
- **Current:** CPU canvas drawing
- **Future:** WebGL/Vulkan shaders
- **Benefit:** 100x+ rendering throughput

---

## 📊 Advanced Monitoring (Planned)

### JMX MBeans
```java
@MXBean
public interface SimulationMetricsMXBean {
    double getCurrentFPS();
    long getTotalCells();
    long getTotalAgents();
    long getHeapUsedMB();
    double getFrameTimeMs();
}
```

### Prometheus Endpoints
```
GET /actuator/prometheus

# Metrics
ether_cells_total{biome="ocean"} 35000
ether_fps_current 29.5
ether_heap_bytes 524288000
ether_agents_total 5000
```

### Grafana Dashboards
- **Performance Panel**
  - FPS over time (line chart)
  - Frame time histogram
  - GC pause times

- **Simulation Panel**
  - Agent population (line chart)
  - Biome distribution (pie chart)
  - Resource totals (bar chart)

- **System Panel**
  - CPU usage
  - Memory (heap/non-heap)
  - Thread count

### Java Flight Recorder (JFR)
```bash
# Start recording
jcmd <pid> JFR.start duration=60s filename=ether.jfr

# Analyze with JMC
jmc ether.jfr
```

**Profile:**
- CPU hotspots (rendering loops)
- Memory allocations (agent creation)
- Lock contention (thread sync)
- Garbage collection events

---

**Next Major Milestones:**
1. Cell Hover Tooltip (Phase 7 cont.)
2. Internationalization (Phase 8)
3. Database Persistence (Phase 10)
4. GPU Acceleration (Phase 12)

