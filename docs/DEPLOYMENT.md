# Ether Deployment & Release Guide

> **Zero-Configuration Instant Deployment, Standalone Portable Execution, and High-Performance Clustered Computing**

---

## 1. Quick Start: Instant Standalone Deployment (1-Click)

Ether is engineered to run **immediately out of the box with zero database configuration**.

### Prerequisites
* **Java Runtime**: OpenJDK 21 or higher ([Adoptium Eclipse Temurin](https://adoptium.net/))
* **Memory**: 4 GB RAM recommended (8 GB+ for high-resolution planetary meshes)
* **GPU (Optional)**: Vulkan / OpenGL / OpenCL capable GPU for hardware-accelerated rendering and SIMD acceleration.

### Launching on Windows
Double-click `install.bat` or `run.bat`, or execute via PowerShell:
```powershell
.\run.ps1
```

### Launching on Linux / macOS
Make the script executable and run:
```bash
chmod +x run.sh
./run.sh
```

---

## 2. Standalone vs Database Modes

Ether provides automatic database fallback:

| Mode | Database Requirement | Use Case |
|---|---|---|
| **Standalone Mode** (Default) | **None** (Embedded In-Memory & Direct SoA Buffers) | Instant evaluation, desktop simulation, scenario experimentation |
| **PostgreSQL / PostGIS Mode** | PostgreSQL 15+ with PostGIS 3.3+ | Longitudinal historical archives, persistent geospatial analytics, multi-run benchmarks |
| **Distributed Cluster Mode** | ZeroMQ / WebSocket + Master/Worker cluster | Large-scale multi-node parallel computing across thousands of nodes |

To run explicitly without a database:
```bash
# Windows
scripts\start-no-db.bat

# Linux / macOS
./scripts/start-no-db.sh
```

---

## 3. Building Standalone Release Packages

To generate a fully self-contained distribution archive for end-user distribution:

### On Windows:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\package_release.ps1 -Version 1.0.0-beta.1
```

### On Linux / macOS:
```bash
bash scripts/package_release.sh 1.0.0-beta.1
```

This generates:
* `dist/Ether-v1.0.0-beta.1-standalone/` — Unpacked ready-to-run folder
* `dist/Ether-v1.0.0-beta.1-standalone.zip` (and `.tar.gz`) — Portable compressed archive with executable fat JAR, all documentation, GIS presets, and launcher scripts.

---

## 4. Headless & Automated Batch Simulation

Ether supports fully headless CLI simulation for automated calibration, Monte Carlo sweeps, and Continuous Integration pipelines:

```bash
# Run 500 simulation ticks headlessly with statistical output
java -jar bin/ether.jar --headless --ticks 500 --scenario Earth_Holocene_20kBC --output results.json
```

Or using Maven:
```bash
mvn exec:java -Dexec.args="--headless --ticks 300"
```

---

## 5. Dockerized Deployment

For production server deployments with complete PostgreSQL/PostGIS integration:

```bash
# Start PostgreSQL/PostGIS database container
docker compose up -d

# Check database readiness
scripts/database-status.bat   # Windows
./scripts/database-status.sh  # Linux
```

---

## 6. High-Performance JVM Tuning Parameters

For maximum throughput with massive Uber H3 grid resolutions ($175,000+$ cells):

```bash
java --add-modules=jdk.incubator.vector \
     --enable-native-access=ALL-UNNAMED \
     -XX:+UseG1GC \
     -XX:+ParallelRefProcEnabled \
     -Xms4g -Xmx8g \
     -jar bin/ether.jar
```
