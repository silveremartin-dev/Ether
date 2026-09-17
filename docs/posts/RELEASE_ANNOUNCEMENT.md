# Ether 1.0.0-beta.1 — Official Release Announcement & Changelog

> **Release Version**: 1.0.0-beta.1  
> **Release Date**: September 2026  
> **Repository**: [https://github.com/silveremartin-dev/Ether](https://github.com/silveremartin-dev/Ether)  
> **License**: MIT License  

---

## 🌟 Major Highlights of Ether

### 1. 1-Click Autonomous Standalone Deployment
* **Zero-Database Requirement**: Run Ether immediately on any machine with Java 21+ using `install.bat` / `run.bat` (Windows) or `install.sh` / `run.sh` (Linux/macOS).
* **Automated Standalone Packaging**: `scripts/package_release.ps1` and `scripts/package_release.sh` generate ready-to-distribute portable `.zip` archives.
* **Optional Cluster & PostGIS Integration**: Seamlessly switch between zero-config in-memory mode, local PostGIS, and distributed multi-node clusters.

### 2. Comprehensive 80+ Simulation Engine Suite
* **Tier 1 — Core Model Physics (~30 Engines)**:
  - Exact astronomical Milankovitch insolation forcing (eccentricity, obliquity, precession).
  - 3-cell atmospheric circulation (Hadley, Ferrel, Polar) with Coriolis acceleration.
  - Stommel AMOC 2-box thermohaline ocean conveyor.
  - 2D lateral Darcy porous aquifer depletion.
  - Manning-Strickler open channel hydraulics & Stokes siltation.
  - Viscoelastic Glacial Isostatic Adjustment (GIA) and PDD cryospheric melting.
  - Farquhar FvCB photosynthesis, van Genuchten soil water retention, N-P-K nutrient exhaustion.
  - Stull wet-bulb temperature hyperthermia mortality.
* **Tier 2 — Optional Cliodynamic & Institutional Engines (~50 Engines)**:
  - Turchin-Goldstone Structural-Demographic Theory (SDT) with elite overproduction dynamics.
  - Tainter diminishing returns on organizational complexity.
  - Kümmel-Ayres-Warr exergy growth models.
  - West-Bettencourt urban allometric scaling ($Y \propto N^{1.15}$).
  - NASA HANDY and Limits to Growth (World3) ecological carrying capacity collapse.
  - Acemoglu-Robinson institutional drift, Boserupian agricultural intensification, and Price equation multi-level altruistic selection.
  - 30+ Type B historical scenarios (Edo isolation, Roman hyperinflation, Fertile Crescent salinization, Asabiyyah decay, Wittfogel hydraulic despotism).

### 3. Complete Localization (i18n) & High-Contrast UI
* **100% Translation Parity across 5 Languages**: English (`EN`), French (`FR`), German (`DE`), Spanish (`ES`), and Simplified Chinese (`ZH`) with zero missing keys across 1,814 entries.
* **Universal Technical Mouse-Over Tooltips**: Every interactive UI element displays the mathematical equations and physical mechanisms driving the simulation.
* **Dual High-Contrast Theming**: Full Dark and Light theme parity with optimal visual contrast.

### 4. Publication-Grade Documentation & Empirical Calibrations
* Complete Differential Equations & State Variables specification in `docs/SIMULATION_EQUATIONS_AND_VARIABLES.md`.
* Validated against Seshat Global History Databank, HYDE 3.4, and Maddison Historical Statistics.
* Complete bibliography and dataset provenance catalog in `docs/DATA_SOURCES_AND_INGESTION.md`.

---

## 📦 Download & Installation

```bash
# Clone and run
git clone https://github.com/silveremartin-dev/Ether.git
cd Ether
./install.bat   # Windows
./install.sh    # Linux/macOS
```
