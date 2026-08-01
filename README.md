# Ether - Human Society & Cliodynamic Thermodynamic Simulation (v4.0)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Build](https://img.shields.io/badge/Build-Maven-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Status](https://img.shields.io/badge/Status-Active-brightgreen.svg)

**Ether** is a high-fidelity, physicalist and cliodynamic simulation engine modeling planetary human civilization dynamics from 20,000 BCE into future planetary scenarios. Driven by thermodynamic principles (Joules, Net EROEI, Carnot limits, Shannon entropy, Soil N-P-K & Carbon, and Aquifer depletion), Ether eliminates heuristic short-circuits in favor of deterministic physical forcing equations on a global hexagonal Earth grid (Uber H3).

---

## 🚀 Key Features

### 🌍 Physicalist & Thermodynamic Core Engine
- **H3 Hexagonal Spatial Grid**: Uber H3 multi-resolution hexagonal cell indexing (Resolutions 6 to 8).
- **Thermodynamic Net EROEI & Energy Budgets**: Simulates energy extraction thresholds, EROEI degradation, and industrial work output ($\text{Joules}$).
- **Soil & Aquifer Physical Dynamics**: Tracks N-P-K depletion, Soil Organic Carbon (SOC) sequestration, and freshwater table drawdowns.
- **Biophysical & Cliodynamic Engines**:
  - **Terraforming & Orbital Physics**: Atmospheric pressure ($P_{\text{atm}}$), greenhouse radiative forcing, solar mirrors, and asteroid mining.
  - **Trophic Ecosystems & Rewilding**: 3-tier trophic biomass, Pleistocene Rewilding (permafrost albedo & SOC retention), and species hybridization.
  - **Physical Supply Chains**: Material logistics, transport friction coefficients ($\mu_{\text{sea}}, \mu_{\text{rail}}$), and maritime chokepoint blockades.
  - **Urban Thermodynamics**: Urban Heat Island ($T_{\text{uhi}}$), high-voltage grid transmission losses ($I^2 R$), and city aquifer depletion.

### ⚡ Live "God Mode", Event Injector & Timeline Audit Log
- **Live Simulation Pause & Play**: Instantaneous execution freeze (`engine.pause()`) to inspect or alter planetary parameters.
- **Physical Event Injector**: Trigger volcanic SO₂ aerosol injection ($\tau$), Carrington solar EMP storms, bio-molecular outbreaks, or global heatwaves on demand.
- **Scenario Timeline Audit Log**: Full chronological history tracking $T_0$ initial setup, historical planet events, and manual God Mode interventions.

### 🔀 Scenario Multiverse Branching Tree
- **"What-If" Trajectory Forking**: Snapshot simulation state at year $T$ and create parallel alternate trajectories (e.g. *D-T Fusion Acceleration* vs *Fossil Fuel Lock-in*).
- **Comparative Telemetry Analysis**: Compare population, average temperature, and resource curves across parallel timeline branches.

### 📜 10 Built-in Historical & Future Scenarios
1. **Cro-Magnon & Neolithic Transition (-10,000 BCE)**
2. **Plague of Justinian & Late Antique Climate Anomaly (536 CE)**
3. **Assyrian Empire & Mesopotamian Salinization (-700 BCE)**
4. **Song Dynasty Pre-Industrialization (1080 CE)**
5. **Business As Usual (SSP5-8.5 / Modern Anthropocene, 2026)**
6. **Technological Singularity & D-T Fusion (2045)**
7. **Nuclear Winter & Stratospheric Soot (2030)**
8. **Peak Phosphate & Agricultural Depletion (2040)**
9. **Super-Volcano VEI-8 & Volcanic Winter (2028)**
10. **Custom Planetary Scenario Editor**

---

## 🛠️ Getting Started

### Prerequisites
- **Java 21 JDK** or higher.
- **Maven 3.9+**.
- (Optional) **PostgreSQL / PostGIS** for geospatial persistence.

### Installation & Execution
```bash
# Clone the repository
git clone https://github.com/Start-Z/Ether.git
cd Ether

# Build the project
mvn clean install

# Run the Simulation
mvn javafx:run
```

---

## 🗺️ System Architecture & Progress

| Module | Description | Status |
| :--- | :--- | :--- |
| **Thermodynamic Kernel** | EROEI, Carnot limits, Entropy, Soil N-P-K, Aquifer | ✅ Complete |
| **H3 Geospatial Grid** | Uber H3 resolution 6-8 indexing & visualization | ✅ Complete |
| **Procedural Engines** | Climate, Disease $R_0$, Singularity, Supply Chains, Urban UHI | ✅ Complete |
| **God Mode & Timeline** | Pause/Play, physical event injection, audit trail log | ✅ Complete |
| **Multiverse Branching** | Snapshot & side-by-side trajectory comparison | ✅ Complete |
| **Scenario Documentation**| 10+ rich historical & future physical scenarios | ✅ Complete |

---

## 📄 License & Authors

Distributed under the **MIT License**.

**Authors**:
- **Silvere Martin-Michiellot**
- **Antigravity / Gemini AI (Google DeepMind)**

---

## 🎮 Controls

| Action | Control |
| :--- | :--- |
| **Pan** | Left-Click + Drag |
| **Rotate** | Right-Click + Drag (3D Mode) |
| **Zoom** | Mouse Wheel |
| **Select** | Left-Click on Cell |
| **Toggle View** | Button in Control Panel (2D/3D) |
| **Speed** | 1x, 5x, 20x Buttons |

---

## 🤝 Contributing

Contributions are welcome! Please read `CONTRIBUTING.md` (if available) or submit a Pull Request.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

**Authors**:
-   **Silvere Martin-Michiellot**
-   **Gemini AI (Google DeepMind)**
