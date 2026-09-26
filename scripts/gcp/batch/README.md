# Ether — Google Cloud Batch Deployment (Option 2)

This directory contains scripts for running Ether simulations on **Google Cloud Batch** — a fully managed, serverless HPC job service that provisions VMs on demand, executes the simulation, and **terminates the VMs immediately on completion**.

This is the recommended approach for heavy batch runs because it has **zero idle cost**.

---

## Why Cloud Batch Instead of Persistent VMs?

| | Old approach (`setup-gcp-infra.sh`) | **Cloud Batch (this directory)** |
|---|---|---|
| **VM lifecycle** | Persistent — must be stopped manually | Ephemeral — auto-terminated on job completion |
| **Idle cost** | Billed even when simulation is not running | **\$0 when no simulation is running** |
| **Spot pricing** | Manual — requires `--preemptible` flag on VM | Built-in — `"provisioningModel": "SPOT"` saves ~80% |
| **Result storage** | Manual SCP download | Auto-uploaded to GCS on completion |
| **Parallelism** | Manual SSH to each VM | Native task arrays (`taskCount: N`) |
| **Monitoring** | SSH + tail log files | Cloud Logging (structured, searchable) |
| **Setup required** | Create VMs, firewall rules, startup scripts | Submit job spec JSON — Cloud Batch handles the rest |

---

## Prerequisites

- [gcloud CLI](https://cloud.google.com/sdk/docs/install) installed and authenticated (`gcloud auth login`)
- Docker installed locally
- Project `ether-509812` accessible with your account

---

## Workflow: 3 Commands to Run a Simulation

### Step 0 — One-time infrastructure setup

```bash
# Linux / macOS
./scripts/gcp/batch/setup-gcp-batch-infra.sh

# Windows PowerShell
.\scripts\gcp\batch\setup-gcp-batch-infra.ps1
```

Creates: Artifact Registry repo, GCS bucket `gs://ether-simulations`, service account `ether-batch-runner`.

---

### Step 1 — Build & push the Docker image

```bash
# Linux / macOS
./scripts/gcp/batch/build-and-push.sh ether-509812 europe-west1 latest

# Windows PowerShell
.\scripts\gcp\batch\build-and-push.ps1 -ProjectId ether-509812 -Region europe-west1 -Tag latest
```

Builds the Ether multi-stage Docker image and pushes it to:
`europe-west1-docker.pkg.dev/ether-509812/ether-registry/ether-engine:latest`

> **Tip**: Only rebuild when the source code changes. Re-use `latest` for the same code version.

---

### Step 2 — Submit a simulation job

```bash
# Linux / macOS
./scripts/gcp/batch/submit-batch-job.sh OUT_OF_AFRICA 1000 5000

# Windows PowerShell
.\scripts\gcp\batch\submit-batch-job.ps1 -Scenario OUT_OF_AFRICA -Ticks 1000 -Cells 5000
```

Full parameter reference:

| Parameter | Default | Description |
|---|---|---|
| `SCENARIO` | `OUT_OF_AFRICA` | Simulation scenario preset |
| `TICKS` | `1000` | Number of simulation ticks |
| `CELLS` | `5000` | Number of H3 hexagonal grid cells |
| `WORKERS` | `1` | Number of parallel task replicas |
| `MACHINE_TYPE` | `e2-standard-4` | GCE machine type (4 vCPU, 16 GB RAM) |
| `USE_SPOT` | `true` | Use Spot instances (~80% cheaper, may be preempted) |
| `PROJECT_ID` | `ether-509812` | GCP project ID |
| `REGION` | `europe-west1` | GCP region |
| `IMAGE_TAG` | `latest` | Docker image tag to use |

---

### Step 3 — Monitor progress

```bash
./scripts/gcp/batch/monitor-batch-job.sh ether-out-of-africa-20260926-183000
```

Or check the [GCP Batch console](https://console.cloud.google.com/batch/jobs?project=ether-509812).

---

### Step 4 — Fetch results

```bash
./scripts/gcp/batch/fetch-batch-results.sh ether-out-of-africa-20260926-183000
```

Downloads simulation snapshots to `./saves/batch/<job-name>/` for local replay in the Ether desktop UI.

---

## Cost Estimation

| Scenario | Machine | Spot? | Estimated Duration | Est. Cost |
|---|---|---|---|---|
| Quick validation (500 ticks, 3000 cells) | `e2-standard-4` | Yes | ~15 min | ~\$0.02 |
| Standard run (1000 ticks, 5000 cells) | `e2-standard-4` | Yes | ~45 min | ~\$0.06 |
| Heavy run (5000 ticks, 50000 cells) | `e2-standard-8` | Yes | ~4 hours | ~\$0.30 |
| Monte Carlo (10 parallel × 1000 ticks) | `e2-standard-4` × 10 | Yes | ~1 hour | ~\$0.50 |

> Costs are approximate. Spot instances save ~80% vs standard pricing but may be preempted (job auto-retries once by default: `maxRetryCount: 1`).

---

## Result Storage Layout in GCS

```
gs://ether-simulations/
└── ether-out-of-africa-20260926-183000/
    ├── task-0/          ← Single-task run (or worker 0 in task array)
    │   ├── save_t0100.ether.gz
    │   ├── save_t0500.ether.gz
    │   └── profile_report.json
    ├── task-1/          ← Second parallel task (if WORKERS > 1)
    └── ...
```

Objects are automatically deleted after **90 days** (lifecycle rule set by `setup-gcp-batch-infra.sh`).

---

## Relationship with Other GCP Scripts

The scripts in `../` (the parent `gcp/` directory) use the **old VM-based approach** (`setup-gcp-infra.sh`, `deploy-and-run.sh`). They are preserved for reference and backward compatibility but the Cloud Batch approach in this directory is recommended for new runs.
