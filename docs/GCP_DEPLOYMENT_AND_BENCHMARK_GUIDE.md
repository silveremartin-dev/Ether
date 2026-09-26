# Google Cloud Platform (GCP) Distributed Deployment & Benchmark Guide

This document specifies the deployment architecture, configuration directives, and automated workflows for running Ether in **Headless & Distributed Multi-Node Cluster Mode** on Google Cloud Compute Engine (GCE) with PostgreSQL/PostGIS telemetry persistence.

---

## 1. Architecture Overview

```
                          [ Google Cloud VPC Network ]
                          
┌─────────────────────────────────────────────────────────────────────────────┐
│ ether-master VM (e2-standard-4: 4 vCPU, 16 GB RAM)                         │
│                                                                             │
│ ┌────────────────────────────────────┐  ┌─────────────────────────────────┐ │
│ │ PostgreSQL 15 + PostGIS            │  │ Ether Headless Master Engine    │ │
│ │ Container (Port 54320)             │  │ - Spatial Hilbert Partitioner   │ │
│ │ - Schema: h3_cells_l8, climate     │  │ - Lock-step Barrier Sync (9090) │ │
│ │ - Snapshots: ether_scenarios       │  │ - DOD WorldBuffer CPU Vector    │ │
│ └────────────────────────────────────┘  └─────────────────────────────────┘ │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Port 9090 (AES-256 TCP Channel)
┌──────────────────────────────────────┴──────────────────────────────────────┐
│ ether-worker VM (e2-standard-4: 4 vCPU, 16 GB RAM)                         │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ Ether Headless Worker Node                                              │ │
│ │ - Computes assigned H3 cell chunk slices                                │ │
│ │ - SIMD FluxEngine vectorized execution                                  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼ gcloud compute scp
┌─────────────────────────────────────────────────────────────────────────────┐
│ Local Client Workstation                                                    │
│ └── Ether GUI (JavaFX) -> Load / Analytics / Replay (saves/<id>/*.json)     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Hardware ROI & GPU vs. Vectorized CPU Analysis

* **Vectorized CPU (Recommended - Optimal ROI)**: Ether's Data-Oriented Design (DOD) structures all dynamic cell properties in contiguous memory layouts (`WorldBuffer`), utilizing Java 21 incubator vector SIMD (`jdk.incubator.vector`) and L1/L2/L3 CPU cache locality. Headless throughput reaches **150 to 500+ TPS** on 4 vCPU instances at minimal cost (~$0.13/hour or ~$0.04/hour on Spot).
* **GPU Cloud Acceleration (Evaluation)**: While Ether includes OpenCL/TornadoVM hooks (`GPUManager`, `ClimateKernel`), running dedicated GPUs (NVIDIA T4/L4) on GCP introduces host-to-device memory transfer latency and requires higher GPU quota and hourly pricing ($0.35–$0.70/h). For world grids between 3,000 and 50,000 cells, high-frequency CPU cores provide superior cost-performance.

---

## 3. Automated PowerShell Workflows

### 3.1 Provisioning Cloud Infrastructure
Provisions the VPC firewall rules, Master VM, and Worker VM:
```powershell
.\scripts\gcp\setup-gcp-infra.ps1 -ProjectId "ether-509812" -Zone "europe-west1-b" -MasterMachineType "e2-standard-4" -CreateWorker
```

### 3.2 Building, Deploying, and Executing Simulations
Compiles the JAR locally, syncs to GCP, starts PostgreSQL, and launches simulation:
* **Single-Node Headless Benchmark**:
  ```powershell
  .\scripts\gcp\deploy-and-run.ps1 -Scenario "OUT_OF_AFRICA" -Ticks 1000 -Cells 5000
  ```
* **Distributed 2-Node Cluster Run**:
  ```powershell
  .\scripts\gcp\deploy-and-run.ps1 -Scenario "OUT_OF_AFRICA" -Ticks 1000 -Cells 10000 -ClusterMode
  ```

### 3.3 Fetching Results for Local Replay
Pulls generated snapshots from the GCP Master into the local `saves/` folder:
```powershell
.\scripts\gcp\fetch-results.ps1
```
Open Ether locally via `Ether_Windows.bat` -> Load Saved Game / Replay Analytics.

### 3.4 Cost Management (Stopping / Restarting Instances)
```powershell
# Stop instances to avoid CPU billing while keeping disks:
.\scripts\gcp\teardown-gcp.ps1 -Action stop

# Restart instances when resuming work:
.\scripts\gcp\teardown-gcp.ps1 -Action start

# Permanently delete instances when testing is complete:
.\scripts\gcp\teardown-gcp.ps1 -Action delete
```
