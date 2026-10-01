# 🛠️ Ether Simulation Engine — Scripts & Deployment Guide

This directory contains cross-platform operational, build, benchmarking, containerization, and cluster deployment scripts for the **Ether Cliodynamic & Planetary Simulation Engine**.

---

## 📋 1. Quick Reference Matrix

| Script Family | Target Environment | GUI (JavaFX) | Database (PostgreSQL) | Containerized | Primary Role & Description |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **`start-no-db`** | Local Host | ✅ Yes | ❌ In-Memory | ❌ No | **Quick Desktop Launch**: Starts JavaFX UI with in-memory state (zero dependencies). |
| **`start-with-db`** | Local Host + Docker | ✅ Yes | ✅ Docker Postgres | 🟡 DB only | **Full Desktop Launch**: Spins up PostgreSQL container, then runs GUI on host machine. |
| **`stop-docker`** | Docker | ❌ N/A | ⏹️ Shuts down | ✅ Yes | **Database Teardown**: Stops and removes active PostgreSQL/Redis Docker containers. |
| **`database-status`** | Docker | ❌ N/A | 🔍 Status check | ✅ Yes | **DB Health Inspection**: Displays status of running PostgreSQL and Redis containers. |
| **`start-headless`** | Local Host / CI | ❌ Headless | ⚙️ Configurable | ❌ No | **Standalone Batch Engine**: Runs headless multi-tick simulation with profiling and saves. |
| **`start-master`** | Local Host / Server | ❌ Headless | ⚙️ Configurable | ❌ No | **Cluster Orchestrator Node**: Listens on gRPC port, partitions H3 grid, dispatches to workers. |
| **`start-worker`** | Local Host / Server | ❌ Headless | ❌ Offloaded | ❌ No | **Cluster Compute Worker**: Connects to master node and executes spatial domain compute. |
| **`docker-deploy`** | Docker / Compose | ❌ Headless | ✅ Automated | ✅ Full Stack | **Full Stack Container Deployer**: Starts DB, Headless batch, or multi-worker Cluster. |
| **`build-native-all`** | Developer Host | ❌ N/A | ❌ N/A | ❌ No | **Multi-Arch Native Compiler**: Builds Rust native core (`.dll`, `.so`, `.dylib`). |
| **`regenerate-all-maps`** | Developer Host | ❌ N/A | ❌ N/A | ❌ No | **Cartographic Tensor Generator**: Regenerates all 25 PNG raster layers (-100k to 2060). |
| **`run_historical_bifurcation_benchmarks`** | Dev / CI / HPC | ❌ N/A | ❌ N/A | ❌ No | **Epistemic Falsification Suite**: Runs canonical bifurcation and rupture verification suites. |
| **`package_release`** | Developer Host | ❌ N/A | ❌ N/A | ❌ No | **Distribution Packager**: Bundles self-contained cross-platform release archives. |
| **`javadoc`** | Developer Host | ❌ N/A | ❌ N/A | ❌ No | **API Documentation**: Generates HTML Javadoc from simulation source code. |

---

## 🚀 2. Local Desktop GUI Execution

### `start-no-db` (Standalone In-Memory Mode)
Runs the JavaFX interactive visualization and planetary dashboard with zero external dependencies (no Docker, no PostgreSQL required).

* **PowerShell**: `.\scripts\start-no-db.ps1`
* **Batch**: `.\scripts\start-no-db.bat`
* **Bash**: `./scripts/start-no-db.sh`

### `start-with-db` (Local GUI + Docker PostgreSQL Persistence)
Spins up PostgreSQL/PostGIS in Docker, waits for database readiness, runs the JavaFX desktop UI on the host system, and gracefully halts the database on exit.

* **PowerShell**: `.\scripts\start-with-db.ps1`
* **Batch**: `.\scripts\start-with-db.bat`
* **Bash**: `./scripts/start-with-db.sh`

### `stop-docker` (Docker Container Teardown)
Stops and tears down the PostgreSQL and Redis containers managed by Docker Compose.

* **PowerShell**: `.\scripts\stop-docker.ps1`
* **Batch**: `.\scripts\stop-docker.bat`
* **Bash**: `./scripts/stop-docker.sh`

---

## ⚡ 3. High-Performance Headless & Distributed Cluster Execution

```
                       ┌──────────────────────────────────────────────┐
                       │          Master Orchestrator Node            │
                       │    (start-master / ether-master container)   │
                       │   - H3 Spatial Mesh Partitioning (Hilbert)   │
                       │   - Global Tick Synchronization Barrier      │
                       └──────────────┬───────────────────────────────┘
                                      │ gRPC / Protobuf (Port 9090)
                ┌─────────────────────┼─────────────────────┐
                ▼                     ▼                     ▼
     ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
     │   Worker Node 1    │ │   Worker Node 2    │ │   Worker Node N    │
     │  (start-worker /   │ │  (start-worker /   │ │  (start-worker /   │
     │   ether-worker)    │ │   ether-worker)    │ │   ether-worker)    │
     │ - Subgrid Compute  │ │ - Subgrid Compute  │ │ - Subgrid Compute  │
     │ - SIMD/Rust/GPU    │ │ - SIMD/Rust/GPU    │ │ - SIMD/Rust/GPU    │
     └────────────────────┘ └────────────────────┘ └────────────────────┘
```

### `start-headless` (Standalone Batch Runner)
Executes a fast headless simulation run directly on the host machine without graphical rendering overhead.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `-Scenario` | string | `"OUT_OF_AFRICA"` | Scenario preset (`OUT_OF_AFRICA`, `CLASSICAL`, `INDUSTRIAL`, etc.) |
| `-Ticks` | int | `300` | Number of simulation ticks to execute |
| `-Cells` | int | `3000` | Number of H3 grid cells in simulation domain |
| `-Profile` | switch | `false` | Enables real-time CPU/SIMD profiling and timing reports |

**Examples**:
```powershell
.\scripts\start-headless.ps1 -Scenario "CLASSICAL" -Ticks 1000 -Cells 5000 -Profile
```

---

### `start-master` (Cluster Master Node)
Launches Ether in Cluster Master Orchestrator mode. Listens for incoming worker nodes, partitions the planetary H3 mesh, and coordinates parallel tick cycles.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `-Scenario` | string | `"OUT_OF_AFRICA"` | Scenario preset to load and simulate |
| `-Port` | int | `9090` | TCP/gRPC cluster synchronization port |
| `-Secret` | string | `"EtherClusterSecret2026"` | AES-256 GCM cluster authentication token |
| `-Ticks` | int | `500` | Total simulation ticks to execute |
| `-Cells` | int | `10000` | Total H3 cells in the global planetary grid |
| `-Profile` | switch | `false` | Enables cluster-wide performance telemetry |

**Examples**:
```powershell
.\scripts\start-master.ps1 -Port 9090 -Scenario "MESOPOTAMIA_BRONZE_AGE" -Ticks 2000 -Cells 20000
```

---

### `start-worker` (Cluster Compute Worker Node)
Connects a worker compute node to an active master node, receives assigned spatial cell partitions, and executes parallel domain calculations.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `-MasterHost` | string | `"127.0.0.1"` | IP address or hostname of the Master Cluster node |
| `-Port` | int | `9090` | Cluster communication port |
| `-Secret` | string | `"EtherClusterSecret2026"` | Cluster authentication secret token |

**Examples**:
```powershell
.\scripts\start-worker.ps1 -MasterHost "192.168.1.50" -Port 9090
```

---

## 🐳 4. Docker Compose Deployment Helper (`docker-deploy`)

Ether features a production-grade multi-stage Docker environment (`Dockerfile` and `docker-compose.yml`) supporting full cluster orchestration, single-command scaling, and isolated batch execution.

### Usage Syntax
```powershell
.\scripts\docker-deploy.ps1 [Mode] [Scenario] [Ticks] [Cells] [Workers]
```

### Modes & Profiles
1. **`dev` (Default)**: Starts only `postgres` + `redis` infrastructure services.
   ```powershell
   .\scripts\docker-deploy.ps1 dev
   ```
2. **`headless`**: Runs a self-terminating headless simulation container with outputs written to `./saves`.
   ```powershell
   .\scripts\docker-deploy.ps1 headless OUT_OF_AFRICA 1000 10000
   ```
3. **`cluster`**: Launches 1 Master node and $N$ auto-connected Worker nodes in isolated containers.
   ```powershell
   .\scripts\docker-deploy.ps1 cluster MESOPOTAMIA_BRONZE_AGE 500 5000 4
   ```
4. **`down`**: Tears down all active containers across all profiles.
   ```powershell
   .\scripts\docker-deploy.ps1 down
   ```

---

## 🔬 5. Development, Verification & Benchmarking Scripts

### `run_historical_bifurcation_benchmarks`
Runs the automated Epistemic Falsification & Validation test suites (couplings between Boserup, Malthus, Turchin SDT, and Scott State Formation).

| Mode | Target Test Suite | Execution Scope |
| :--- | :--- | :--- |
| `master` | `MasterHistoricalBifurcationSuite` | 7 Canonical historical rupture scenarios (~2s) |
| `residual` | `EmpiricalResidualBifurcationTest` | Empirical residual inversion and metastability |
| `leaders` | `HistoricalLeaderBifurcationTest` | Leader archetype bifurcation tests |
| `all` | All validation suites | Comprehensive test verification run |
| `macro` | `HeadlessBatchRunner` | 41,162 H3 cell full planetary multi-decadal run |

**Examples**:
```powershell
.\scripts\run_historical_bifurcation_benchmarks.ps1 -Mode all
```

### `build-native-all`
Compiles the high-performance native Rust core library (`ether-core-native`) for multi-platform targets:
- Windows `x86_64` (`.dll`)
- Linux `x86_64` & `ARM64` (`.so`)
- macOS Intel & Apple Silicon `ARM64` (`.dylib`)

```powershell
.\scripts\build-native-all.ps1
```

### `regenerate-all-maps`
Recomputes and bakes all 25 cartographic tensor rasters ($2048 \times 1024$), cultural registries, provenance JSON files, and layer specifications across historical epochs (-100,000 BP to 2060 CE).

```powershell
.\scripts\regenerate-all-maps.ps1
```

### `package_release`
Automates clean compilation, multi-platform binary gathering, and archive generation (`.zip` / `.tar.gz`) for release distributions.

```powershell
.\scripts\package_release.ps1 -Version "1.0.0-beta.1"
```

---

## ☁️ 6. Cloud & Google Cloud Platform (`scripts/gcp/`)

For large-scale hyperscale simulation campaigns on Google Cloud Platform:

* **`scripts/gcp/run-batch-spot.ps1`**: Orchestrates high-throughput parameter sweep jobs using GCP Cloud Batch with cost-effective Spot VMs.
* **`scripts/gcp/deploy-and-run.ps1`**: Provisions Compute Engine instances, copies jars/data, executes distributed cluster runs, and retrieves output artifacts.
* **`scripts/gcp/run-calibration-campaign.ps1`**: Runs automated parameter calibration sweeps against empirical datasets (HYDE 3.4, Seshat, FAO).
* **`scripts/gcp/fetch-results.ps1`**: Synchronizes simulation state dumps and replay snapshots from Cloud Storage buckets to local disk.
* **`scripts/gcp/start-vms.ps1` / `stop-vms.ps1`**: Controls lifecycle of remote cloud simulation instances.

---

## 💾 7. Unified Persistence & Replay Architecture

All simulation runs (Desktop UI, Headless CLI batch, Cluster Master/Workers, and GCP Cloud Batch) produce a single, unified, high-performance binary save format inside `saves/<saveId>/`:

```
saves/<saveId>/
  ├── topology.bin.gz         # Immutable spatial/geological baseline (lat, lon, elevation, biomes, aquifers, language)
  ├── state.bin.gz            # Current active simulation dynamic state (27 physical & socioeconomic layers)
  ├── metadata.json           # Run identifier, scenario name, tick count, calendar dates
  ├── scenario.json           # Scenario parameters, boundary conditions, and presets
  ├── history.json            # Macro-historical time series telemetry (population, GDP, Gini, climate)
  └── snapshots/              # Periodic tick checkpoints for interactive multi-tick timeline replay
        ├── snapshot_tick_0000000000.bin.gz
        ├── snapshot_tick_0000000050.bin.gz
        └── snapshot_tick_0000000100.bin.gz
```

### Key Advantages:
1. **Zero Redundancy**: Static topology is saved once (`topology.bin.gz`, ~80 KB for 10,000 cells) rather than repeated across checkpoints.
2. **Instant UI Replay**: Cluster runs (Docker or GCP) are immediately discoverable and loadable by the JavaFX Desktop UI with zero conversion.
3. **High-Throughput Worker Resumption**: Crashed or late-joining worker nodes restore state from `snapshot_tick_*.bin.gz` in milliseconds via native SIMD ByteBuffers.

