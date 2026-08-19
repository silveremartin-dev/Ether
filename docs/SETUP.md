# Ether Simulation — Setup & Operational Guide

> **Master Setup & Configuration Guide**: Prerequisites, Standalone & Docker Startup, Distributed Multi-Node Cluster Setup, OpenCL Drivers, TornadoVM GPU Acceleration, and Troubleshooting.

---

## 1. System Requirements & Software Prerequisites

### Hardware Requirements

| Requirement | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **CPU** | 4-Core CPU with x86-64 SIMD support | 8+ Core CPU with AVX2 / AVX-512 extensions |
| **RAM** | 4 GB System RAM | 16+ GB System RAM (for 175k+ H3 cell grids) |
| **GPU** | Integrated Intel UHD / AMD Radeon | Discrete NVIDIA RTX / AMD RX with OpenCL 1.2+ |
| **Storage** | 2 GB Available Hard Disk Space | SSD with 5+ GB Available Space |

### Software Prerequisites

1. **Java Development Kit (JDK 21+)**: Required for Virtual Threads (Project Loom) and foreign memory APIs.
   ```bash
   java -version
   ```
2. **Apache Maven (v3.9+)**: Used to compile, test, and package the application.
   ```bash
   mvn -version
   ```
3. **Docker Desktop (Optional)**: Required for database-backed persistence (PostgreSQL / PostGIS).

---

## 2. Standalone & Database Startup

Ether can be launched in standalone in-memory mode or full database-backed mode:

### Startup Scripts (`scripts/`)

| Script | Purpose | Description |
| :--- | :--- | :--- |
| `start-no-db.bat` / `.ps1` / `.sh` | **Standalone Startup** | Immediate launch in memory without requiring Docker or PostgreSQL. |
| `start-docker.sh` | **Full Production Startup** | Starts PostgreSQL/PostGIS container, applies schema migrations, and launches application. |
| `stop.bat` / `.sh` | **Shutdown** | Gracefully stops active database containers. |
| `database-status.bat` / `.sh` | **Health Inspection** | Checks database health, port bindings, and logs. |

### PostgreSQL Configuration
- **Host**: `localhost` | **Port**: `54320`
- **Database**: `ether_simulation` | **User**: `ether` | **Password**: `dev_password`

---

## 3. Distributed Cluster Setup (Multi-Node Scaling)

Ether supports distributed cluster execution across Master and Worker nodes using gRPC binary streaming.

### Cluster Topology & Environment Variables

| Node Role | Script | Environment Variable | Port |
| :--- | :--- | :--- | :--- |
| **Master Node** | `scripts/start-master.ps1` / `.sh` | `ETHER_ROLE=MASTER` | `50051` (gRPC) |
| **Worker Node** | `scripts/start-worker.ps1` / `.sh` | `ETHER_ROLE=WORKER` | Dynamic |

### Starting a Multi-Node Cluster

1. **Start Master Node**:
   ```bash
   ./scripts/start-master.sh --port=50051
   ```
2. **Start Worker Nodes**:
   ```bash
   ./scripts/start-worker.sh --master=192.168.1.100:50051 --domain=EUROPE
   ./scripts/start-worker.sh --master=192.168.1.100:50051 --domain=ASIA
   ```

---

## 4. OpenCL Drivers & TornadoVM Acceleration Setup

### Vendor-Specific OpenCL Drivers
- **NVIDIA**: Install CUDA Toolkit (v12.x+) or display drivers. Confirm with `clinfo`.
- **AMD**: Install Adrenalin Edition or ROCm runtime.
- **Intel**: Install Intel Graphics DCH drivers with OpenCL support.

### TornadoVM GPU Integration
TornadoVM JIT-compiles Java bytecodes into OpenCL C and NVIDIA PTX kernels at runtime.

#### Installation:
```bash
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM
./bin/tornadovm-installer --jdk jdk-21 --backend opencl,ptx
```

#### Running with GPU Backend:
```bash
tornado --backend opencl mvn javafx:run
```

*Note: If no GPU is available, Ether gracefully falls back to CPU Loom Virtual Threads.*

---

## 5. Troubleshooting & Frequently Asked Questions

### `java.lang.OutOfMemoryError: Java heap space`
Increase heap allocation for large H3 resolution grids (Res 7-8):
```bash
mvn javafx:run -Dexec.mainClass="org.ether.society.EtherApp" -Dexec.args="-Xms4g -Xmx8g"
```

### Headless CLI Execution
Run the engine in headless server mode for benchmark evaluation:
```bash
./scripts/start-headless.sh --scenario=Earth --years=1000
```
