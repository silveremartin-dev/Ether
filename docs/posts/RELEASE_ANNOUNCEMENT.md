# Ether 1.0.0-beta.5 — Official Release Announcement & Changelog

> **Release Version**: 1.0.0-beta.5  
> **Release Date**: November 2026  
> **Repository**: [https://github.com/silveremartin-dev/Ether](https://github.com/silveremartin-dev/Ether)  
> **License**: MIT License  

---

## 🌟 Major Highlights of Ether 1.0.0-beta.5

### 1. Centralized SI Dimensional Consistency & Unified Simulation Clock
* **Authoritative SI Unit Conversion (`PhysicalUnitsHelper`)**: Centralized all physical, thermodynamic, and temporal unit conversions (`J`, `GJ`, `W`, `kg`, `t`, `K`, Julian days/years) across simulation engines, DOD kernels, telemetry aggregators, and data loaders, eliminating inline conversion literals.
* **Canonical Human Population Nomenclature (`humanPopulation`)**: Refactored cellular population buffers from `biomassHuman` to `humanPopulation` across Java, native Rust (`ether-core-native`), wire codecs, and technical documentation, accompanied by grounded Hatton (2015), Ayres-Warr, and IPCC stoichiometric formulations.
* **Deterministic 32D Culture & Mitosis Kernels**: Synchronized 32-dimensional cultural trait vectors in the native Rust engine, enforced strict SplitMix64 PRNG determinism in `DemographicKernel` mitosis, and unified single-clock temporal progression.

### 2. Unified `SimulationTemporalEvent` Architecture & Geophysical Forcings
* **Immutable `SimulationDate` & `SimulationTemporalEvent` Contract**: Introduced linear step-independent temporal progression, enum-dispatched physical shock execution, and continuous intensity envelopes (bell curve, two-phase radioecological decay, sigmoidal plateau).
* **Coupled Stratospheric Aerosol & Radioecological Physics**: Connected volcanic stratospheric aerosol optical depth directly to `AlbedoClimateEngine`, integrated two-phase radioecological decay (`Cs-137` / `Sr-90`) and exclusion zones into `ThermodynamicMigrationEngine`, and added historical shock falsification reports (1347 Black Death, 1914 World Wars, 165 Antonine Plague at H3 Res 6).
* **Regional Charney Albedo Scoping & Category A Formula Fixes**: Scoped Charney (1975) vegetation-albedo fold dynamics to Saharo-Sahelian arid/semi-arid belts, and corrected Category A cellular derivative formulations in `TainterComplexityCollapseEngine` (Tainter 1988 diminishing marginal returns) and `HandyNasaHybridEngine` (Motesharrei et al. 2014 bilinear depletion).

### 3. White Paper Streaming Telemetry, Offline Resilience & Full Javadoc Refresh
* **Streamed Snapshot Telemetry & UTF-8 Integrity**: Recompiled the multi-scenario benchmark white paper with 100% streamed intermediate snapshots, dynamic H3 resolution reporting (`Res >= 4` enforced on modern and prospective scenarios), embedded Mermaid diagrams, off-Earth run classification, and zero UTF-8 mojibake across all 82 scenario preset files.
* **Zero-Latency Offline Mode Latching**: `DatabaseConfig` now latches standalone offline mode on first unreachable PostgreSQL check, eliminating repeated HikariCP connection timeouts during snapshot persistence.
* **Complete Javadoc API Documentation Refresh**: Rebuilt and published `javadoc/` across all 669 main production classes, records, engines, kernels, and UI components under Java 25.

### 4. 1-Click Autonomous Standalone Deployment & Multi-Platform Packages
* **Zero-Database Requirement**: Run Ether out of the box on Windows, Linux, and macOS with Java 25 using portable standalone distribution packages.
* **Full 5-Language Localization (i18n)**: 3,020 keys in 100% parity across English (`EN`), French (`FR`), German (`DE`), Spanish (`ES`), and Simplified Chinese (`ZH`) with 0 missing keys and 0 hardcoded UI strings.

---

## 📦 Download & Installation

```bash
# Clone and run
git clone https://github.com/silveremartin-dev/Ether.git
cd Ether
./install.bat   # Windows
./install.sh    # Linux/macOS
```

Portable standalone release packages:
* `Ether-v1.0.0-beta.5-windows-x64.zip`
* `Ether-v1.0.0-beta.5-linux-x64.tar.gz`
* `Ether-v1.0.0-beta.5-macos-universal.tar.gz`
* `Ether-v1.0.0-beta.5-standalone.zip`
* `Ether-v1.0.0-beta.5-standalone.tar.gz`
* `Ether-v1.0.0-beta.5-executable.jar`
