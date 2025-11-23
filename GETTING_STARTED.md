# H3 Geospatial Earth Simulation - Quick Start

## Overview
This simulation uses **H3 hexagonal grid** (7.2M cells at Level 8, ~1 km² each) with **GPU parallel processing** for massive-scale Earth simulation from 20,000 BC to present.

## Prerequisites

### Required
- **Java 21** ([Download JDK](https://adoptium.net/))
- **Docker** & **Docker Compose** ([Download](https://www.docker.com/products/docker-desktop/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))

### Optional (for GPU acceleration)
- **TornadoVM** (see [TORNADOVM_SETUP.md](TORNADOVM_SETUP.md))
- **GPU** (NVIDIA, AMD, or Intel with OpenCL support)

---

## Quick Start (5 minutes)

### 1. Start Database Services
```bash
# Start PostgreSQL + Redis in Docker
docker-compose up -d

# Verify services are running
docker-compose ps

# Check PostgreSQL logs
docker logs ether_postgres
```

### 2. Build the Application
```bash
# Download dependencies
mvn clean install -DskipTests

# This will download:
# - H3 Java library (~5 MB)
# - Spring Boot (~100 MB)
# - PostgreSQL driver
# - PostGIS extensions
# - GeoTools
```

### 3. Initialize H3 Grid (First Time Only)
```bash
# Generate Earth H3 grid at Level 8 (7.2M hexagons)
# This takes ~10-15 minutes
mvn exec:java -Dexec.mainClass="com.ether.society.data.H3GridInitializer"
```

### 4. Run the Simulation
```bash
# With JavaFX UI (desktop mode)
mvn javafx:run

# With GPU (if TornadoVM installed)
tornado --backend opencl mvn javafx:run

# With Spring Boot backend (for web)
mvn spring-boot:run
```

---

## Architecture Overview

```
7.2 Million H3 Hexagons (Level 8, ~1 km² each)
              ↓
    PostgreSQL + PostGIS
    (Permanent storage)
              ↓
         Redis Cache
         (Hot cells)
              ↓
   Java Backend (Spring Boot)
   - H3Service (geospatial queries)
   - SimulationEngine (Virtual Threads)
   - GPU Kernels (TornadoVM) ← PARALLEL!
              ↓
     Frontend (Choose one)
     ├─ JavaFX (Desktop, 2D/3D)
     └─ React + Three.js (Web, 2D/3D)
```

---

## Database Schema

### Main Table: h3_cells_l8
| Column | Type | Description |
|--------|------|-------------|
| h3_index | BIGINT | H3 hexagon ID (primary) |
| latitude | DOUBLE | Center latitude |
| longitude | DOUBLE | Center longitude |
| elevation | DOUBLE | Mean elevation (m) |
| temperature | DOUBLE | Current temp (°C) |
| rainfall | DOUBLE | Annual rainfall (mm) |
| biome | VARCHAR | Land cover type |
| population | INTEGER | Human count |
| food_resource | DOUBLE | Food availability |
| water_resource | DOUBLE | Water availability |
| wood_resource | DOUBLE | Wood availability |

**Size**: 7.2M rows × ~100 bytes = ~720 MB

---

## GPU Acceleration Model

### CPU vs GPU Performance

**CPU (Virtual Threads)**:
- 7.2M cells updated sequentially
- ~1-5 ticks per second (TPS)
- Good for < 100K cells

**GPU (TornadoVM)**:
- 7.2M cells updated in parallel
- ~10-60 TPS
- **10-100x speedup!**

### How GPU Parallelization Works

```java
// Each H3 cell is updated independently in parallel
@Parallel
for (int i = 0; i < 7_200_000; i++) {
    H3Cell cell = cells[i];
    
    // GPU thread updates this cell
    cell.temperature = calculateTemperature(cell, season);
    cell.population = updatePopulation(cell, neighbors);
    cell.resources = renewResources(cell, biome);
}
```

**Key**: H3 cells are independent → perfect for GPU parallelism!

---

## Data Sources & Ingestion

### Elevation Data
- **Source**: SRTM 90m / ASTER GDEM
- **Resolution**: ~90m per pixel
- **Aggregation**: Mean elevation per H3 hex
- **Command**: `mvn exec:java -Dexec.mainClass="... ElevationIngestion"`

### Biome Data
- **Source**: MODIS Land Cover (MCD12Q1)
- **Resolution**: 500m per pixel
- **Aggregation**: Dominant biome per H3 hex
- **Command**: `mvn exec:java -Dexec.mainClass="...BiomeIngestion"`

### Climate Data
- **Source**: WorldClim 2.1 / Paleoclimate models
- **Years**: -20,000 to 2024 (20K years!)
- **Storage**: ~500 GB compressed
- **Command**: `mvn exec:java -Dexec.mainClass="...ClimateIngestion"`

---

## Development Workflow

### 1. Database Changes
```bash
# Modify: scripts/init-db.sql
docker-compose down
docker-compose up -d
```

### 2. Add New H3 Operation
```java
// In H3Service.java
public List<Long> getCustomQuery(long h3Index) {
    // Your H3 logic
}
```

### 3. GPU Kernel Development
```java
// In gpu/ package
@Parallel
public void customKernel(H3Cell[] cells) {
    // TornadoVM parallel kernel
}
```

### 4. Frontend Changes
- **Desktop**: Edit `App.java` (JavaFX)
- **Web**: Edit React components (future)

---

## Configuration Files

### application.yml
```yaml
ether:
  simulation:
    h3-resolution: 8        # Level 8 (~1 km²)
    gpu-enabled: true       # Use GPU if available
  data:
    elevation-source: "SRTM_90m"
    biome-source: "MODIS"
    climate-years: 20000
```

### docker-compose.yml
- PostgreSQL 16 + PostGIS 3.4
- Redis 7 (cache)

---

## Troubleshooting

### "Port 5432 already in use"
```bash
# Another PostgreSQL running
sudo systemctl stop postgresql
# OR change port in docker-compose.yml
```

### "H3 grid not initialized"
```bash
# Run initialization
mvn exec:java -Dexec.mainClass="...H3GridInitializer"
```

### "Out of memory"
```bash
# Increase JVM heap
export MAVEN_OPTS="-Xmx8g"
mvn javafx:run
```

### "GPU not detected"
- Install OpenCL/CUDA drivers
- See [TORNADOVM_SETUP.md](TORNADOVM_SETUP.md)

---

## Next Steps

1. ✅ Start Docker containers
2. ✅ Build application
3. ✅ Initialize H3 grid
4. ⏭️ Download real Earth data (SRTM, MODIS)
5. ⏭️ Run data ingestion pipeline
6. ⏭️ Start simulation!

---

## Documentation

- [Implementation Plan](docs/implementation_plan.md) - Full architecture
- [TornadoVM Setup](TORNADOVM_SETUP.md) - GPU configuration
- [API Documentation](docs/API.md) - REST endpoints
- [Data Sources](docs/DATA_SOURCES.md) - Where to get Earth data

## Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Email**: silvere.martin@gmail.com

---

**License**: MIT  
**Author**: Silvere Martin-Michiellot  
**Contributors**: AI Assistant (Antigravity/Claude)
