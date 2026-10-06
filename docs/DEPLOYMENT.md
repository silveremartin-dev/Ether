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
Double-click `run.bat` or execute via Command Prompt:
```cmd
run.bat
```

### Launching on Linux
Make the script executable and run:
```bash
chmod +x run.sh
./run.sh
```

### Launching on macOS
Double-click `run.command` in Finder, or run via Terminal:
```bash
chmod +x run.sh run.command
./run.command
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
```cmd
scripts\package_release.bat 1.0.0-beta.2
```

### On Linux / macOS:
```bash
bash scripts/package_release.sh 1.0.0-beta.2
```

This generates:
* `dist/Ether-v1.0.0-beta.2-standalone/` — Unpacked ready-to-run folder
* `dist/Ether-v1.0.0-beta.2-standalone.zip` (and `.tar.gz`) — Portable compressed archive with executable fat JAR, all documentation, GIS presets, and launcher scripts.

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

---

## 7. Distributed Multi-Node Cluster CLI Orchestration & Optimization

Ether features a fully decentralized, lock-step distributed computing architecture powered by AES-256 encrypted frames, 2D Hilbert space-filling curve domain partitioning, and barrier synchronization.

### 7.1 Starting the Master Server Node

Launch the central master server orchestrator with custom spatial partitioning, barrier timeout, and snapshot intervals:

```bash
# Start Master Cluster Server
java -jar bin/ether.jar --role=master \
     --port=9090 \
     --secret=EtherClusterSecret2026 \
     --partition-strategy=LOAD_AWARE \
     --barrier-timeout=5000 \
     --snapshots --snapshot-interval=50 \
     --scenario=OUT_OF_AFRICA \
     --res=4 --ticks=1000 \
     --threads=16
```

### 7.2 Launching Multiple Worker Nodes with Custom Acceleration & Hardware Profiles

You can scale horizontally by launching as many heterogeneous worker nodes as desired across local or remote machines:

```bash
# Node 1: Dedicated GPU Compute Worker (OpenCL Shaders)
java -jar bin/ether.jar --role=worker \
     --master-host=192.168.1.100 --port=9090 \
     --secret=EtherClusterSecret2026 \
     --node-id=worker-gpu-01 \
     --worker-capacity="NVIDIA RTX 4090 (24GB VRAM)" \
     --engine=gpu --worker-gpu

# Node 2: Native Rust Multi-Core Worker (Rayon + AVX-512)
java -jar bin/ether.jar --role=worker \
     --master-host=192.168.1.100 --port=9090 \
     --secret=EtherClusterSecret2026 \
     --node-id=worker-rust-02 \
     --worker-capacity="AMD EPYC 64-Core Native Rust" \
     --engine=rust --threads=64

# Node 3: Java 21 SIMD Vector Worker
java -jar bin/ether.jar --role=worker \
     --master-host=192.168.1.100 --port=9090 \
     --secret=EtherClusterSecret2026 \
     --node-id=worker-simd-03 \
     --worker-capacity="Intel Xeon 32-Core SIMD" \
     --engine=simd --threads=32

# Node 4: Ultra-Deterministic Batch Node (Single-Thread SW Fallback)
java -jar bin/ether.jar --role=worker \
     --master-host=192.168.1.100 --port=9090 \
     --secret=EtherClusterSecret2026 \
     --node-id=worker-safe-04 \
     --engine=safe --single-core
```

### 7.3 Complete CLI Flag Reference

| Category | Parameter | Description |
|---|---|---|
| **Topology** | `--role=<master\|worker>` | Set node role (`--server` / `--node` / `--master` / `--worker`) |
| | `--master-host=<IP>` | Master hostname or IP for worker nodes |
| | `--port=<PORT>`, `-P <PORT>` | Port for cluster communication (Default: `9090`) |
| | `--secret=<TOKEN>` | AES-256 handshake token |
| | `--node-id=<ID>` | Unique identity tag for the worker node |
| | `--worker-capacity=<DESC>` | Human-readable node capacity description |
| | `--partition-strategy=<TYPE>` | `HILBERT`, `LOAD_AWARE`, `EQUAL_SLICES` |
| | `--barrier-timeout=<MS>` | Lock-step barrier wait timeout in ms (Default: `3000`) |
| | `--heartbeat-interval=<SEC>` | Worker ping frequency (Default: `2s`) |
| | `--heartbeat-timeout=<SEC>` | Node disconnection timeout (Default: `8s`) |
| | `--snapshots` / `--snapshot-interval=<N>` | Automated snapshot checkpointing |
| **Engine** | `--engine=<rust\|gpu\|simd\|cpu\|safe>` | Compute backend engine |
| | `--threads=<N>`, `--cores=<N>` | CPU worker threads allocation |
| | `--single-core` / `--multi-core` | Single-threaded vs multi-threaded execution |
| **Performance** | `--strict-determinism[=true\|false]` | Strict bit-identical Tier-1 physical conservation |
| | `--sparse-skipping[=true\|false]` | Skip updates on uninhabited desert/ocean cells |
| | `--multi-freq-climate` / `--climate-freq=<N>` | Sub-sampled climate calculation frequency |
| | `--spatial-truncation` | Truncate long-range spatial dispersion tails |
| **Scenario** | `--scenario=<NAME>`, `-s <NAME>` | Preset epoch scenario |
| | `--res=<2..6>`, `-r <N>` | Planetary H3 grid resolution |
| | `--cells=<N>`, `-c <N>` | Number of simulated H3 cells |
| | `--ticks=<N>`, `-t <N>` | Simulation tick duration |
| | `--start-year=<Y>`, `--end-year=<Y>` | Custom historical epoch span |
| **Preferences** | `--lang=<EN\|FR\|DE\|ES\|ZH>`, `-l <LANG>` | Global UI / CLI language preference |
| | `--theme=<dark\|light\|presentation>` | Visual display theme |

