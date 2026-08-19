# Ether 2.0 — Distributed Compute Cluster & Headless Engine Guide

This document outlines the architecture, deployment, network security, fault-tolerance resilience, scenario dispatching, and CLI parameters for running **Ether** in a distributed multi-node compute cluster environment or headless mode.

---

## ⚡ Overview & Compute Modes

Ether supports three compute execution contexts:
1. **CPU Mode (Local JVM)**: Multi-threaded execution on the local host CPU.
2. **GPU Support (Hardware Acceleration)**: OpenCL / JavaFX Prism hardware acceleration via TornadoVM kernels.
3. **Distributed Cluster Mode (gRPC/TCP Network Grid)**: Multi-node distributed planetary computing over TCP/IP with AES-256 GCM encryption.

---

## 🌐 Distributed Cluster Architecture

```
                       ┌─────────────────────────┐
                       │   Master Cluster Node   │
                       │   (IP: 192.168.1.10)    │
                       │   Port: 9090 (TCP/gRPC) │
                       └────────────┬────────────┘
                                    │ AES-256 Encrypted
                                    │ Handshake & Heartbeats
               ┌────────────────────┴────────────────────┐
               │                                         │
    ┌──────────▼───────────┐                  ┌──────────▼───────────┐
    │  Worker Node 01      │                  │  Worker Node 02      │
    │  (Cells 0 .. 4999)   │                  │  (Cells 5000 .. 9999)│
    └──────────────────────┘                  └──────────────────────┘
```

### Key Infrastructure Capabilities:
- **AES-256 GCM Encryption**: All node communications, handshakes, and spatial state sync payloads are encrypted using AES-256 GCM.
- **Dynamic H3 Cell Partitioning**: Planetary H3 cells are partitioned dynamically across connected workers based on spatial chunks and compute capacity.
- **Heartbeat & Resynchronization**: Master tracks worker health every 3 seconds.
- **Fault-Tolerance & Resilience**: If a worker node drops out due to network issues, the Master automatically re-claims orphaned cell chunks and rebalances the grid across remaining active nodes.
- **Late-Joining Workers**: New compute nodes joining mid-simulation receive state synchronization payloads and an assigned cell chunk without interrupting execution.
- **Master Scenario Dispatching (`dispatchScenarioToCluster`)**: When a scenario is selected on Master, the full scenario configuration (start year, end year, initial population, tech level, density type) is broadcasted down to all connected worker nodes automatically.

---

## 💻 Headless CLI Deployment & Parameter Reference

Ether can be executed cleanly in pure headless mode (no JavaFX GUI required) for background servers, HPC compute clusters, and CI/CD benchmarks.

### Exhaustive CLI Parameters:

| Flag / Option | Description | Default Value | Example Usage |
| :--- | :--- | :--- | :--- |
| `--scenario=<NAME>` / `-s <NAME>` | Historical or custom scenario preset to load & dispatch. | `OUT_OF_AFRICA` | `--scenario="Roman Empire & Pax Romana (An 0)"` or `-s BRONZE_AGE` |
| `--mode=cluster` / `--cluster` | Enables multi-node distributed cluster mode. | `Disabled (Local CPU)` | `--mode=cluster` |
| `--role=master` / `--role=worker` | Role of the local node (`master` orchestrator or `worker` compute node). | `master` | `--role=worker` |
| `--master-host=<IP>` | IP address or hostname of the Master node (used by workers). | `127.0.0.1` | `--master-host=192.168.1.10` |
| `--port=<PORT>` | TCP / gRPC cluster communication port. | `9090` | `--port=9090` |
| `--secret=<TOKEN>` | AES-256 GCM shared cluster authentication secret token. | `EtherClusterSecret2026` | `--secret="MySecret2026"` |
| `--ticks=<N>` / `-t <N>` | **(Optional)** Override number of simulation ticks to execute. If omitted, ticks are calculated automatically from scenario duration (`endDateYear - startDateYear`). | Calculated from Scenario | `--ticks=1000` |
| `--cells=<N>` / `-c <N>` | Number of H3 grid cells to generate for benchmark runs. | `3000` | `--cells=10000` |
| `--profile` / `-p` | Output detailed profiling report (TPS, ms/tick, memory) upon completion. | `Enabled` | `--profile` |

---

## 📜 Launch Scripts (`/scripts`)

Pre-configured launch scripts are available for both PowerShell (`.ps1`) and Bash (`.sh`):

### 1. Launch Master Server
```powershell
# PowerShell
.\scripts\start-master.ps1 -Scenario "Roman Empire & Pax Romana (An 0)" -Port 9090

# Bash
./scripts/start-master.sh "OUT_OF_AFRICA" 9090
```

### 2. Launch Worker Compute Node
```powershell
# PowerShell
.\scripts\start-worker.ps1 -MasterHost "192.168.1.10" -Port 9090

# Bash
./scripts/start-worker.sh "192.168.1.10" 9090
```

### 3. Run Headless Batch Simulation
```powershell
# PowerShell
.\scripts\start-headless.ps1 -Scenario "Neolithic Revolution" -Ticks 500

# Bash
./scripts/start-headless.sh "NEOLITHIZATION" 500
```

---

## 🚀 Supported Scenario Presets & Naming Conventions

Ether scenario names can be passed either as **enum keys** or **full descriptive titles**:

| Enum Key | Full Descriptive Display Title | Start Year ($T_0$) | Estimated Pop |
| :--- | :--- | :--- | :--- |
| `OUT_OF_AFRICA` | Out of Africa Migration | -100,000 BC | 10,000 |
| `UPPER_PALEOLITHIC` | Upper Paleolithic | -40,000 BC | 500,000 |
| `MESOLITHIC` | Mesolithic | -15,000 BC | 3,000,000 |
| `NEOLITHIZATION` | Neolithic Revolution | -10,000 BC | 5,000,000 |
| `CHALCOLITHIC` | Copper Age | -5,000 BC | 15,000,000 |
| `BRONZE_AGE` | First Empires & Bronze Age | -3,000 BC | 50,000,000 |
| `IRON_AGE` | Iron Age | -1,200 BC | 100,000,000 |
| `CLASSICAL` | Classical Era | -500 BC | 200,000,000 |
| `ROMAN_EMPIRE` | Roman Empire & Pax Romana | An 0 | 250,000,000 |
| `MEDIEVAL` | Medieval Period | 500 AD | 300,000,000 |
| `EARLY_MODERN` | Age of Exploration | 1500 AD | 500,000,000 |
| `INDUSTRIAL` | Industrial Revolution & Coal Era | 1800 AD | 1,000,000,000 |
| `MODERN` | Modern Information Era | 1945 AD | 2,500,000,000 |

---

## 🛡️ Security & Resilience Best Practices

1. **Authentication Secret**: Ensure all nodes share the identical `--secret=<TOKEN>` to pass the AES-256 handshake.
2. **Firewall Setup**: Open the designated TCP port (default `9090`) on the Master server host.
3. **State Resynchronization**: Master persists state snapshots to `WorldBuffer` to ensure instant recovery upon node failure.
