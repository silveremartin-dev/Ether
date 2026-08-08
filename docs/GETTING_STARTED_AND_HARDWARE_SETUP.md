# Ether Simulation — Getting Started & Hardware Setup Guide

> **Master Setup & Configuration Specification**: Prerequisites, Installation, Execution Modes, Database Setup, OpenCL Drivers, TornadoVM Integration, and Troubleshooting.

---

## 1. System Requirements & Prerequisites

Before running or building the **Ether Simulation Engine**, ensure your system meets the following software and hardware requirements.

### Hardware Requirements

| Requirement | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **CPU** | 4-Core CPU with x86-64 SIMD support | 8+ Core CPU with AVX2 / AVX-512 extensions |
| **RAM** | 4 GB System RAM | 16+ GB System RAM (for 175k+ H3 cell grids) |
| **GPU** | Integrated Intel UHD / AMD Radeon | Discrete NVIDIA RTX / AMD RX with OpenCL 1.2+ |
| **Storage** | 2 GB Available Hard Disk Space | SSD with 5+ GB Available Space |

### Software Prerequisites

#### 1. Java Development Kit (JDK 21+)
Ether requires **Java 21** or higher for Project Loom (Virtual Threads) and modern foreign function memory access APIs.
- **Verification**:
  ```bash
  java -version
  ```
  *Output should indicate `openjdk version "21"` or higher.*

#### 2. Apache Maven (v3.9+)
Maven is used to compile, run tests, manage dependencies, and launch the JavaFX UI.
- **Verification**:
  ```bash
  mvn -version
  ```

#### 3. Docker Desktop (Optional - For Database Persistence)
Required if running Ether in full database-backed mode with PostgreSQL and PostGIS.
- Download and install **Docker Desktop** for Windows/Linux. Ensure the Docker daemon is active.

---

## 2. Quickstart & Application Startup

Ether can be launched in either **Database-Backed Mode** (persisting state to PostgreSQL/PostGIS) or **Standalone In-Memory Mode** (fast development execution).

### Startup Scripts (Windows Batch Files)

| Script | Purpose | Description & Instructions |
| :--- | :--- | :--- |
| `start-docker.bat` | **Full Production Startup** | Launches PostgreSQL Docker container, waits for health-check, runs migrations, and starts JavaFX application. |
| `start-no-db.bat` | **Standalone Quick Startup** | Launches the application immediately in standalone in-memory mode without requiring Docker or PostgreSQL. |
| `stop.bat` | **Container Shutdown** | Gracefully stops and cleans up active PostgreSQL Docker container instances. |
| `database-status.bat` | **Health Inspection** | Displays container health status, active port bindings, and connection logs. |

### Database Connection Configuration
When launching via `start-docker.bat`, the simulation automatically configures connection parameters:

| Parameter | Value |
| :--- | :--- |
| **Host** | `localhost` |
| **Port** | `54320` |
| **Database Name** | `ether_simulation` |
| **Username** | `ether` |
| **Password** | `dev_password` |

---

## 3. Building & Benchmark Execution

### 1. Compile & Run Unit Tests
To build the codebase and verify system integrity across all 100+ tests:
```bash
mvn clean test
```

### 2. Run High-Fidelity Earth Benchmark (175,000 Cells)
To run the high-resolution planetary Earth benchmark (175,000 H3 cells with 50 Million simulated humans):
```bash
mvn test -Dtest=EarthFullResolution175kBenchmarkTest
```

### 3. Launch Graphical User Interface (JavaFX)
To start the interactive simulation GUI with 2D/3D map canvas and control panel:
```bash
mvn javafx:run
```

---

## 4. OpenCL Drivers & Hardware Acceleration Setup

Ether leverages OpenCL for GPU-accelerated parallel execution over hexagonal cell buffers.

### Vendor-Specific OpenCL Setup

#### NVIDIA GPUs
1. Install the latest **NVIDIA Display Drivers** or **CUDA Toolkit (v12.x+)**.
2. Verify `OpenCL.dll` (Windows) or `libOpenCL.so` (Linux) is installed:
   ```powershell
   clinfo
   ```

#### AMD GPUs
1. Install **AMD Software: Adrenalin Edition** or **ROCm** runtime.
2. Confirm OpenCL platform registration using `clinfo`.

#### Intel GPUs & Integrated Graphics
1. Download Intel Graphics Drivers (DCH drivers include OpenCL runtime).
2. For standalone Intel OpenCL SDK, append the library directory to `PATH`:
   ```powershell
   $env:PATH += ";C:\Program Files (x86)\Intel\OpenCL\SDK\bin"
   ```

---

## 5. TornadoVM GPU Pipeline Integration

TornadoVM JIT-compiles Java bytecode into native OpenCL C and NVIDIA PTX kernels at runtime.

### Installation Instructions

#### Linux / macOS:
```bash
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM
./bin/tornadovm-installer --jdk jdk-21 --backend opencl,ptx
```

#### Windows:
```powershell
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM
.\scripts\tornadoVMInstaller.cmd
```

### Environment Configuration
Export the TornadoVM SDK path in your shell profile (`.bashrc` / `.zshrc` / PowerShell profile):
```bash
export TORNADO_SDK=/path/to/TornadoVM/bin/sdk
source $TORNADO_SDK/etc/sources.env
```

Verify GPU device detection:
```bash
tornado --devices
```

### Running with TornadoVM Acceleration
- Execute with default OpenCL GPU backend:
  ```bash
  tornado --backend opencl mvn javafx:run
  ```
- Execute with NVIDIA PTX backend:
  ```bash
  tornado --backend ptx mvn javafx:run
  ```

### Automatic Fallback Mechanism
If TornadoVM or a compatible GPU hardware device is not present at runtime, Ether gracefully falls back to multithreaded CPU Virtual Threads (**Java 21 Project Loom**) without throwing exceptions or interrupting simulation ticks.

---

## 6. Troubleshooting & FAQ

### Issue: `java.lang.OutOfMemoryError: Java heap space`
- **Cause**: High H3 grid resolutions (Resolutions 7-8) demand additional heap allocation.
- **Solution**: Pass additional JVM memory arguments:
  ```bash
  mvn javafx:run -Dexec.mainClass="org.ether.society.EtherApp" -Dexec.args="-Xms4g -Xmx8g"
  ```

### Issue: Docker Database Connection Timeout
- **Cause**: PostgreSQL container port `54320` is blocked or container is starting up.
- **Solution**: Execute `database-status.bat` to check container logs, or fall back to `start-no-db.bat`.

### Issue: OpenCL Device Not Found
- **Cause**: GPU vendor drivers lack valid OpenCL ICD registration.
- **Solution**: Run `clinfo` to inspect active platforms. If no GPU is found, Ether automatically defaults to CPU Virtual Threads mode.
