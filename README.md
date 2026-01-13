# Ether - Human Society Simulation (v2.0)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Build](https://img.shields.io/badge/Build-Maven-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Status](https://img.shields.io/badge/Status-Active-brightgreen.svg)

**Ether** is a high-performance, agent-based simulation modeling the evolution of human society from 20,000 BCE to the modern era. It utilizes grid-based density simulation (Artemis Layer) on a hexagonal Earth grid (Uber H3) to simulate population dynamics, climate change, resource management, and the rise of civilizations.

---

## 🚀 Key Features

### 🌍 Simulation Engine (Artemis)
-   **H3 Hexagonal Grid**: High-precision geospatial grid (Resolution 6-8).
-   **Density Dynamics**: Simulates millions of humans via statistical density rather than individual agents.
-   **Climate System**: Realistic seasonal cycles, temperature gradients, and rainfall simulation based on latitude and elevation.
-   **Procedural Generation**: Generate Earth-like worlds with realistic biomes (Tundra, Jungle, Desert, etc.).

### 🖥️ Premium Visualization
-   **Glassmorphism UI**: Modern, translucent interface with neon accents and smooth transitions.
-   **3D Isometric View**: Toggle between 2D map and 3D terrain Visualization.
-   **Data Layers**: Switch views to see Biomes, Population Heatmaps, Temperature Gradients, or Food Density.
-   **Interactive Controls**: Zoom, Pan, Rotate, and inspect cell details.

### 💾 Persistence & Data
-   **Save/Load System**: Persist world states to database and JSON metadata.
-   **Real Earth Data**: Capable of ingesting SRTM (Elevation) and WorldClim data for realistic Earth simulation.

### 🌐 Internationalization
-   Native support for **English**, **French**, **German**, and **Spanish**.

---

## 🛠️ Getting Started

### Prerequisites
-   **Java 21 JDK** or higher.
-   **Maven 3.9+**.
-   (Optional) **PostGreSQL** for advanced persistence (default uses H2/File).

### Installation
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/Start-Z/Ether.git
    cd Ether
    ```

2.  **Build the project**:
    ```bash
    mvn clean install
    ```

3.  **Run the Simulation**:
    ```bash
    mvn javafx:run
    ```

---

## 🗺️ Roadmap & Progress

| Phase | Feature | Status |
| :--- | :--- | :--- |
| **Phases 1-3** | Core Architecture & Infrastructure | ✅ Complete |
| **Phases 4-6** | H3 Grid & Procedural Generation | ✅ Complete |
| **Phase 7** | UI & Visualization (3D/2D) | ✅ Complete |
| **Phase 8** | Internationalization (i18n) | ✅ Complete |
| **Phase 9** | Testing & Benchmarking | ✅ Complete |
| **Phase 10** | Persistence (Save/Load) | ✅ Complete |
| **Phase 11** | Real Earth Data Ingestion | ✅ Complete |
| **Phase 12** | UI Polish (Glassmorphism) | ✅ Complete |
| **Phase 13** | GPU Acceleration (TornadoVM) | ⏳ Planned |
| **Phase 14** | Advanced Diplomacy AI | ⏳ Planned |

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
