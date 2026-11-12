# Ether 1.0.0-beta.3 — Official Release Announcement & Changelog

> **Release Version**: 1.0.0-beta.3  
> **Release Date**: November 2026  
> **Repository**: [https://github.com/silveremartin-dev/Ether](https://github.com/silveremartin-dev/Ether)  
> **License**: MIT License  

---

## 🌟 Major Highlights of Ether 1.0.0-beta.3

### 1. Data-Oriented Single-Pass Kernel Fusion on WorldBuffer
* **Unified Cellular Execution**: Complete migration of all Tier 1 (biophysical PDEs) and Tier 2 (cliodynamics) cellular engines to the high-performance Data-Oriented Design (`WorldBuffer`).
* **50x Cliodynamic Overhead Reduction**: Single-pass derivative accumulation (`DerivativeAccumulator`) replaces legacy multi-pass object loops, slashing Tier 2 cliodynamic overhead to just ~1.14 ms/tick (0.74% of tick runtime).
* **High-Throughput Global Ticks**: Reaches 20+ TPS on Res 3 global grids (41,162 hexagons) and sub-second execution on canonical Res 4 planetary grids (288,122 hexagons).

### 2. Comprehensive Architectural Standardization (<= 500 Lines & Zero Defect)
* **100% Architecture Compliance**: Strict enforcement of the 500-line limit across all 663 production Java source files, zero duplicate classes, and zero silent exception swallowing.
* **Canonical Architectural Archetypes**: Systematic organization into formal Coordinators, Services, Repositories, Registries, and Helpers.
* **Unified Simulation Engine Contracts**: 100% of simulation engines unified under `SimulationEngineModule` and `AbstractSimulationEngine`.

### 3. Dynamic Multi-Metric Telemetry & Comparative Analytics
* **Dynamic Metric Ingestion**: `HistorySnapshot` now supports dynamic telemetry mapping (`dynamicMetrics`), seamlessly linking runtime evaluations with historical ground-truth baselines.
* **Headless Batch Falsification**: Fully autonomous CLI batch runner with comprehensive markdown report synthesis, trajectory graphs, and statistical variance tracking.
* **Ground-Truth Calibration**: Automated comparison against authoritative empirical datasets (HYDE 3.4, Maddison Project 2020, NOAA ETOPO 2022, EPICA ice cores, Seshat).

### 4. 1-Click Autonomous Standalone Deployment & Multi-Platform Packages
* **Zero-Database Requirement**: Run Ether immediately on Windows, Linux, and macOS with Java 21+ using portable distribution packages.
* **Full 5-Language Localization (i18n)**: 100% parity across English (`EN`), French (`FR`), German (`DE`), Spanish (`ES`), and Simplified Chinese (`ZH`) with rich mathematical and scientific tooltips.
* **Dual High-Contrast Theme System**: Optimized Dark and Light themes meeting accessibility standards across all panels.

---

## 📦 Download & Installation

```bash
# Clone and run
git clone https://github.com/silveremartin-dev/Ether.git
cd Ether
./install.bat   # Windows
./install.sh    # Linux/macOS
```
