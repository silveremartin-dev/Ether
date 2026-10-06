# 🛠️ Ether Simulation Engine — Scripts & Deployment Guide

This directory contains cross-platform operational, build, benchmarking, containerization, and cluster deployment scripts for the **Ether Cliodynamic & Planetary Simulation Engine**.

All scripts are categorized into modular subdirectories:
- **`build/`**: Compilation, Javadoc generation, and multi-OS standalone release packagers.
- **`run/`**: Desktop GUI launchers, Docker lifecycles, headless engine runners, and cluster orchestrators.
- **`test/`**: Epistemic falsification test runners, bifurcation benchmarks, and calibration suites.
- **`gcp/`**: Google Cloud Platform Batch, Spot VMs, and distributed hyperscale runners.

---

## 📋 1. Quick Reference Matrix

| Subdirectory | Script Name | Target Environment | GUI / DB / Container | Description |
| :--- | :--- | :--- | :---: | :--- |
| **`run/`** | `start-no-db.bat` / `.sh` | Local Desktop | GUI \| In-Memory \| No | **Quick Desktop Launch**: Zero-dependency JavaFX launcher with SIMD vectorization. |
| **`run/`** | `start-with-db.bat` / `.sh` | Local Host + Docker | GUI \| Postgres \| DB Only | **Full Desktop Launch**: Spins up PostgreSQL container, executes UI, stops DB on exit. |
| **`run/`** | `stop-docker.bat` / `.sh` | Docker Compose | N/A \| Postgres + Redis \| Full | **Docker Teardown**: Stops and tears down active simulation database containers. |
| **`run/`** | `database-status.bat` / `.sh` | Docker Compose | Status \| Postgres + Redis \| Full | **Health Inspection**: Checks container status and tests ports 5432/54320 & 6379. |
| **`run/`** | `start-headless.bat` / `.sh` | Local Host / CI | Headless \| Flexible \| No | **Standalone Batch Engine**: Runs multi-tick scenario simulations with profiling. |
| **`run/`** | `start-master.bat` / `.sh` | Cluster Server | Headless \| Flexible \| No | **Cluster Master Node**: Dispatches H3 grid cells to worker nodes via gRPC (port 9090). |
| **`run/`** | `start-worker.bat` / `.sh` | Compute Worker | Headless \| Offloaded \| No | **Cluster Worker Node**: Connects to Master node and computes spatial domains. |
| **`run/`** | `docker-deploy.bat` / `.sh` | Docker Stack | Headless / Cluster \| Full | **Container Deployer**: Orchestrates dev DBs, standalone headless, or scaled clusters. |
| **`run/`** | `regenerate-all-maps.bat` / `.sh` | Host Machine | N/A \| File IO \| No | **Cartographic Generator**: Recomputes all 25 raster layers (-100k to 2060 CE). |
| **`build/`** | `package-release.ps1` | Windows / CI | N/A \| Native PS1 \| No | **PowerShell Packager**: Builds fat JAR, packages multi-OS releases, computes SHA256. |
| **`build/`** | `deploy-release-windows.bat` | Windows Host | N/A \| ZIP + SHA256 \| No | **Windows Release**: Packages `Ether-v<ver>-windows-x64.zip` with `run.bat`. |
| **`build/`** | `deploy-release-linux.sh` | Linux Host | N/A \| Tar.gz + SHA256 \| No | **Linux Release**: Packages `Ether-v<ver>-linux-x64.tar.gz` with `run.sh`. |
| **`build/`** | `deploy-release-macos.sh` | macOS Host | N/A \| Tar.gz + SHA256 \| No | **macOS Release**: Packages `Ether-v<ver>-macos-universal.zip` with `run.command`. |
| **`build/`** | `deploy-release-all.bat` / `.sh` | All Platforms | N/A \| Full Suite \| No | **Universal Release**: Packages all OS distributions in `dist/` with checksums. |
| **`build/`** | `generate-javadoc.bat` / `.sh` | Host Machine | N/A \| HTML Docs \| No | **API Documentation**: Compiles full HTML Javadoc to `target/site/apidocs`. |
| **`build/`** | `build-native-all.bat` / `.sh` | Cargo / Rust | N/A \| Native DLL/SO \| No | **Native Core Compiler**: Builds Rust SIMD core library (`ether-core-native`). |
| **`test/`** | `test-all-in-one.bat` / `.sh` | CI / Dev Host | N/A \| Test Harness \| No | **All-in-One Test Suite**: Unit tests, bifurcation benchmarks, and leader tests. |
| **`test/`** | `run_historical_bifurcation_benchmarks.bat` / `.sh` | CI / Dev Host | N/A \| Falsification \| No | **Bifurcation Suite**: Runs Turchin SDT, Malthus, Boserup, and Scott state tests. |
| **`test/`** | `run_cloud_calibration_campaign.sh` | Cloud / HPC | N/A \| HPC Harness \| No | **Calibration Campaign**: Executes parameter sweeps against empirical datasets. |

---

## 🚀 2. Local Desktop GUI Execution (`scripts/run/`)

### `start-no-db` (Standalone In-Memory Mode)
Runs the JavaFX interactive visualization and planetary dashboard with zero external dependencies (no Docker, no PostgreSQL required).

* **Windows**: `scripts\run\start-no-db.bat`
* **Linux / macOS**: `./scripts/run/start-no-db.sh`

### `start-with-db` (Local GUI + Docker PostgreSQL Persistence)
Spins up PostgreSQL/PostGIS in Docker, waits for database readiness, runs the JavaFX desktop UI on the host system, and gracefully halts the database on exit.

* **Windows**: `scripts\run\start-with-db.bat`
* **Linux / macOS**: `./scripts/run/start-with-db.sh`

### `database-status` & `stop-docker` (Docker Lifecycles)
* **Status Check**: `scripts\run\database-status.bat` / `./scripts/run/database-status.sh`
* **Teardown**: `scripts\run\stop-docker.bat` / `./scripts/run/stop-docker.sh`

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
| `SCENARIO` | string | `"OUT_OF_AFRICA"` | Scenario preset (`OUT_OF_AFRICA`, `CLASSICAL`, `INDUSTRIAL`, etc.) |
| `TICKS` | int | `300` | Number of simulation ticks to execute |
| `CELLS` | int | `3000` | Number of H3 grid cells in simulation domain |

**Examples**:
* **Windows**: `scripts\run\start-headless.bat CLASSICAL 1000 5000`
* **Linux / macOS**: `./scripts/run/start-headless.sh CLASSICAL 1000 5000`

### `start-master` & `start-worker` (Distributed Cluster)
* **Master Node**: `scripts\run\start-master.bat MESOPOTAMIA_BRONZE_AGE 9090 EtherClusterSecret2026 2000 20000`
* **Worker Node**: `scripts\run\start-worker.bat 192.168.1.50 9090`

---

## 📦 4. Multi-Platform Standalone Packaging (`scripts/build/`)

Ether provides a complete suite of multi-OS deployment scripts that automatically compile and package self-contained standalone distributions into `dist/`:

### 1-Click OS Packaging:
- **Windows (`.zip` + SHA256)**: `scripts\build\deploy-release-windows.bat [version]`
- **Linux (`.tar.gz` + `.zip` + SHA256)**: `./scripts/build/deploy-release-linux.sh [version]`
- **macOS (`.command` + `.zip` + SHA256)**: `./scripts/build/deploy-release-macos.sh [version]`
- **All-in-One Multi-OS**: `scripts\build\deploy-release-all.bat` / `./scripts/build/deploy-release-all.sh`
- **PowerShell Packager**: `powershell -ExecutionPolicy Bypass -File scripts\build\package-release.ps1 -Platform all`

### Javadoc Documentation Generator:
- **Windows**: `scripts\build\generate-javadoc.bat`
- **Linux / macOS**: `./scripts/build/generate-javadoc.sh`

---

## 🔬 5. Epistemic Falsification & Test Harness (`scripts/test/`)

### `test-all-in-one`
Runs the complete verification pipeline:
1. Java unit & integration tests (`mvn test`)
2. Master Historical Bifurcation Suite (Turchin SDT, Boserup-Malthus)
3. Empirical Residual & Metastability Suite
4. Historical Leader A/B Falsification Suite

* **Windows**: `scripts\test\test-all-in-one.bat`
* **Linux / macOS**: `./scripts/test/test-all-in-one.sh`

### `run_historical_bifurcation_benchmarks`
* **Windows**: `scripts\test\run_historical_bifurcation_benchmarks.bat [master|residual|leaders|all|macro]`
* **Linux / macOS**: `./scripts/test/run_historical_bifurcation_benchmarks.sh [master|residual|leaders|all|macro]`

---

## ☁️ 6. Cloud & Google Cloud Platform (`scripts/gcp/`)

For hyperscale simulation campaigns on Google Cloud Platform:
- **`scripts/gcp/run-batch-spot.sh`**: Orchestrates high-throughput parameter sweep jobs using GCP Cloud Batch with Spot VMs.
- **`scripts/gcp/deploy-and-run.sh`**: Provisions Compute Engine instances, deploys JARs, and executes cluster runs.
- **`scripts/gcp/run-calibration-campaign.sh`**: Runs automated parameter calibration sweeps against empirical datasets.
- **`scripts/gcp/fetch-results.sh`**: Synchronizes simulation snapshots and save files from GCS buckets.
- **`scripts/gcp/start-vms.sh` / `stop-vms.sh`**: Manages remote cloud instance lifecycles.
