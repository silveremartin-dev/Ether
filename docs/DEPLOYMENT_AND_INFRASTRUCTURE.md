# 🚀 Ether Deployment & Infrastructure Guide

**Document Version:** 2.0.0-unified  
**Classification:** Operational Infrastructure Specification  
**Target Environments:** Local Standalone Desktop, Docker / PostGIS, Google Cloud Platform (GCE Distributed Cluster)  

---

## 📋 Table of Contents
1. [Quick Start: Standalone Portable Desktop (1-Click)](#1-quick-start-standalone-portable-desktop-1-click)
2. [Prerequisites & Build Instructions](#2-prerequisites--build-instructions)
3. [Execution Modes: Standalone vs. PostgreSQL/PostGIS](#3-execution-modes-standalone-vs-postgresqlpostgis)
4. [Docker Containerization](#4-docker-containerization)
5. [Google Cloud Platform (GCP) Distributed Cluster Deployment](#5-google-cloud-platform-gcp-distributed-cluster-deployment)
   * [5.1 Infrastructure Architecture](#51-infrastructure-architecture)
   * [5.2 Automated Provisioning & Firewall Rules](#52-automated-provisioning--firewall-rules)
   * [5.3 Deploying JAR & Running Batch Simulations](#53-deploying-jar--running-batch-simulations)
   * [5.4 Cost Management (Start / Stop / Teardown)](#54-cost-management-start--stop--teardown)
   * [5.5 Fetching Saves & Snapshots for Local Inspection](#55-fetching-saves--snapshots-for-local-inspection)

---

## 1. Quick Start: Standalone Portable Desktop (1-Click)

Ether is engineered to run **immediately out of the box with zero database configuration**.

### Launching on Windows
Double-click `run.bat` or execute in Command Prompt:
```cmd
run.bat
```

### Launching on Linux
```bash
chmod +x run.sh
./run.sh
```

### Launching on macOS
```bash
chmod +x run.sh run.command
./run.command
```

To run explicitly without database dependencies:
* Windows: `scripts\start-no-db.bat`
* Linux / macOS: `./scripts/start-no-db.sh`

---

## 2. Prerequisites & Build Instructions

### System Requirements
* **Java Runtime**: OpenJDK 21 or higher ([Adoptium Eclipse Temurin](https://adoptium.net/))
* **Memory**: Minimum 4 GB RAM (8 GB+ recommended for H3 Resolution 4 and above)
* **Build System**: Apache Maven 3.8+
* **C++ / Rust Toolchain (Optional)**: MSVC 2022 or GCC/Clang with Rust 1.80+ (if compiling `native/ether-core-native`)

### Building from Source
```bash
# 1. Compile and package the complete Fat JAR
mvn clean package -DskipTests

# 2. Build Native Rust Acceleration Core (Optional)
cd native/ether-core-native
cargo build --release
cd ../..
```

---

## 3. Execution Modes: Standalone vs. PostgreSQL/PostGIS

Ether automatically detects database availability and switches gracefully:

| Mode | Database | Description & Use Case |
| :--- | :--- | :--- |
| **Standalone Mode** (Default) | **None** | Embedded in-memory execution with direct binary disk snapshot streaming (`.bin.gz`). Zero setup required. |
| **PostgreSQL / PostGIS Mode** | PostgreSQL 15+ & PostGIS 3.3+ | Spatial indexing, SQL analytics, multi-run comparison and longitudinal archives. |
| **Distributed Cluster Mode** | TCP Barrier Sync (Port 9090) | Multi-node parallel computing across GCP Master and Worker nodes. |

---

## 4. Docker Containerization

To run Ether with PostgreSQL and PostGIS via Docker Compose:
```bash
# Start PostgreSQL / PostGIS container
docker compose up -d postgres

# Start headless simulation container
docker compose up ether-headless
```

---

## 5. Google Cloud Platform (GCP) Distributed Cluster Deployment

### 5.1 Infrastructure Architecture

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

### 5.2 Automated Provisioning & Firewall Rules

All GCP automation scripts are located in `scripts/gcp/`:

```bash
# Provision VPC, firewall rules, and compute instances
chmod +x scripts/gcp/*.sh
./scripts/gcp/setup-gcp-infra.sh ether-509812 europe-west1-b europe-west1 e2-standard-8 e2-standard-4 true
```

### 5.3 Deploying JAR & Running Batch Simulations

```bash
# Deploy updated JAR and run single-node simulation
./scripts/gcp/deploy-and-run.sh ether-509812 europe-west1-b OUT_OF_AFRICA 1000 5000 false

# Deploy updated JAR and run distributed 2-node cluster run
./scripts/gcp/deploy-and-run.sh ether-509812 europe-west1-b OUT_OF_AFRICA 1000 10000 true

# Run standardized hardware benchmark harness
gcloud compute ssh ether-master --zone=europe-west1-b --command="java -jar /opt/ether/society-simulation.jar --benchmark"
```

### 5.4 Cost Management (Start / Stop / Teardown)

Stop VM billing immediately while preserving all disks and PostgreSQL datasets:
```bash
# Stop instances to halt CPU/RAM billing
./scripts/gcp/stop-vms.sh

# Restart instances when ready to simulate
./scripts/gcp/start-vms.sh

# Check cluster state
./scripts/gcp/status-vms.sh

# Full teardown and deletion (destructive)
./scripts/gcp/teardown-gcp.sh ether-509812 delete
```

### 5.5 Fetching Saves & Snapshots for Local Inspection

Download simulation runs from GCP to your local workstation:
```bash
./scripts/gcp/fetch-results.sh ether-509812 europe-west1-b saves
```
Open Ether locally via `scripts\start-no-db.bat` and navigate to Tab 3 (Comparative Analytics & Replay) to inspect timelines, time-series curves, and map layers.
