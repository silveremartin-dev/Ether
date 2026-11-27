# 🛠️ Ether Simulation Engine — Scripts & Deployment Guide

This directory contains cross-platform operational, build, benchmarking, containerization, and cluster deployment scripts for the **Ether Cliodynamic & Planetary Simulation Engine**.

All scripts are categorized into modular subdirectories (`AGENTS.md` §17.2):
- **`build/`**: Compilation, native Rust builds, Javadoc generation, multi-OS release packagers, and workspace hygiene (`clean-workspace`).
- **`run/`**: Desktop GUI launchers, Docker lifecycles, headless engine runners, and cluster orchestrators.
- **`test/`**: Architecture/quality compliance audits, epistemic falsification suites, and bifurcation benchmarks.
- **`benchmark/`**: 5-axis performance benchmarks, 51-preset catalog runners, and falsification report compilers.
- **`i18n/`**: Localization key-parity and zero-hardcoded-string audits across all 6 bundles.
- **`gcp/`**: Google Cloud Platform Batch, Spot VMs, and distributed hyperscale runners.
- **`gis/` & `ingest/`**: External geospatial and empirical dataset ingestion pipelines (e.g. IIASA SSPs).

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
| **`build/`** | `package-release.ps1` / `.sh` | All Platforms | N/A \| Release Packager \| No | **Standalone Packager**: Builds fat JAR, packages multi-OS releases, computes SHA256. |
| **`build/`** | `deploy-release-windows.bat` | Windows Host | N/A \| ZIP + SHA256 \| No | **Windows Release**: Packages `Ether-v<ver>-windows-x64.zip` with `run.bat`. |
| **`build/`** | `deploy-release-linux.sh` | Linux Host | N/A \| Tar.gz + SHA256 \| No | **Linux Release**: Packages `Ether-v<ver>-linux-x64.tar.gz` with `run.sh`. |
| **`build/`** | `deploy-release-macos.sh` | macOS Host | N/A \| Tar.gz + SHA256 \| No | **macOS Release**: Packages `Ether-v<ver>-macos-universal.zip` with `run.command`. |
| **`build/`** | `deploy-release-all.bat` / `.sh` | All Platforms | N/A \| Full Suite \| No | **Universal Release**: Packages all OS distributions in `dist/` with checksums. |
| **`build/`** | `generate-javadoc.bat` / `.sh` | Host Machine | N/A \| HTML Docs \| No | **API Documentation**: Compiles full HTML Javadoc to `target/site/apidocs`. |
| **`build/`** | `build-native-all.bat` / `.sh` | Cargo / Rust | N/A \| Native DLL/SO \| No | **Native Core Compiler**: Builds Rust SIMD core library (`ether-core-native`). |
| **`build/`** | `clean-workspace.bat` / `.ps1` / `.sh` | Local Host / CI | N/A \| Maintenance \| No | **Workspace Cleaner**: Purges saves, logs, scratch, exports, and transient reports. |
| **`test/`** | `test-all-in-one.bat` / `.sh` | CI / Dev Host | N/A \| Test Harness \| No | **All-in-One Test Suite**: Unit tests, bifurcation benchmarks, and leader tests. |
| **`test/`** | `run-scientific-falsification.bat` / `.sh` | CI / Dev Host | N/A \| Falsification \| No | **Epistemic Falsification**: Runs all 24 scientific debate benchmarks (HYDE, EPICA, Seshat). |
| **`test/`** | `run_historical_bifurcation_benchmarks.bat` / `.sh` | CI / Dev Host | N/A \| Falsification \| No | **Bifurcation Suite**: Runs Turchin SDT, Malthus, Boserup, and Scott state tests. |
| **`test/`** | `run_cloud_calibration_campaign.bat` / `.sh` | Cloud / HPC | N/A \| HPC Harness \| No | **Calibration Campaign**: Executes parameter sweeps against empirical datasets. |
| **`test/`** | `audit-architecture-compliance.ps1` / `.sh` | CI / Dev Host | N/A \| Code Audit \| No | **Architecture Compliance**: Enforces zero empty catch, stateless Helper & module contracts. |
| **`test/`** | `audit-full-quality-and-naming.ps1` / `.sh` | CI / Dev Host | N/A \| Quality Audit \| No | **Code Quality & Naming**: Audits naming archetypes, Javadoc coverage, English comments, and PRNG determinism. |
| **`i18n/`** | `audit-i18n-and-hardcoded-strings.ps1` / `.sh` | CI / Dev Host | N/A \| i18n Audit \| No | **Localization & UI Audit**: Verifies 100% key parity across 6 bundles and zero hardcoded UI strings. |
| **`benchmark/`** | `run-all-benchmarks.bat` / `.sh` | Dev Host / Cloud | N/A \| Benchmark Harness \| No | **5-Axis Master Benchmark**: Evaluates spatial, temporal, SIMD, coupling, and epoch scaling. |
| **`benchmark/`** | `run_full_catalog_refresh.ps1` / `.sh` | Dev Host / Cloud | N/A \| Catalog Suite \| No | **51-Preset Catalog Runner**: Executes all 51 presets in native H3 & temporal resolutions. |
| **`benchmark/`** | `run_epochs_and_future_calibration.ps1` / `.sh` | Dev Host / Cloud | N/A \| Calibration \| No | **9-Epoch & Future Runner**: Runs continuous macro-epochs and 2025-2200 projections. |
| **`benchmark/`** | `generate-scenario-falsification-report.ps1` / `.sh` | Dev Host | N/A \| Report Compiler \| No | **Falsification Report**: Compiles transient scenario audit reports to `target/reports/`. |
| **`benchmark/`** | `generate-white-paper.ps1` / `.sh` | Dev Host | N/A \| Manuscript Sync \| No | **Manuscript Compiler**: Compiles synthesis charts and updates academic manuscript. |

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
* **Master Node**: `scripts\run\start-master.bat MESOPOTAMIA_BRONZE_AGE 9090 YOUR_CLUSTER_SECRET 2000 20000`
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
- **`scripts/gcp/batch/`**: Cloud Batch Spot VM provisioning, container build/push, job submission, monitoring, and result fetching.
- **`scripts/gcp/setup-gcp-infra.sh` / `teardown-gcp.sh`**: Provisions and tears down Compute Engine VPC, firewall rules, and Master/Worker instances.
- **`scripts/gcp/deploy-and-run.sh` / `launch-overnight-suite.ps1`**: Deploys shaded JARs and executes detached overnight simulation campaigns.
- **`scripts/gcp/fetch-results.ps1` / `fetch-results.sh`**: Synchronizes simulation snapshots and save files from remote instances.
- **`scripts/gcp/start-vms.bat` / `.sh`, `stop-vms.bat` / `.sh`, `status-vms.bat` / `.sh`**: Cross-platform lifecycle controls for remote cloud instances.
