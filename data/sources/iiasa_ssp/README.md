# IIASA Shared Socioeconomic Pathways (SSP) Scenario Database

## 🌐 Overview & Epistemic Scope
This directory documents and stores the authoritative multi-variable projections from the **International Institute for Applied Systems Analysis (IIASA)** and the **Energy, Climate, and Environment (ECE) Program**.

The SSP database serves as the baseline for the **IPCC Sixth Assessment Report (AR6)**, mapping five distinct global civilizational narratives from 2020 to 2100:
* **SSP1 (Sustainability)**: Low resource intensity, rapid green technology adoption, high human development.
* **SSP2 (Middle of the Road)**: Trend continuity, moderate economic growth, persistent fossil fuel reliance.
* **SSP3 (Regional Rivalry)**: Deglobalization, low technological transfer, high population growth, geopolitical conflict.
* **SSP4 (Inequality)**: High stratification between tech elites and vulnerable majority.
* **SSP5 (Fossil-Fueled Development)**: High technological and economic growth driven by intensive carbon mobilization.

---

## 💻 API Access & Automated Retrieval via Python
As documented on the official portal (`ssp.apps.ece.iiasa.ac.at/api`), the database does not feature direct static file downloads; it is queried dynamically via the official Python packages `pyam-iamc` or `ixmp4`.

An automated query helper is provided at [`scripts/ingest/fetch_iiasa_ssp.py`](file:///c:/Silvere/Encours/Developpement/Ether/scripts/ingest/fetch_iiasa_ssp.py):
```bash
pip install pyam-iamc ixmp4
python scripts/ingest/fetch_iiasa_ssp.py
```

### Direct Python Query Example:
```python
import pyam

# Connect to the official IIASA SSP REST API
df = pyam.read_iiasa(
    "ssp",
    region="World",
    scenario=["SSP2-Baseline", "SSP1-Baseline", "SSP5-Baseline"],
    variable=["Population", "GDP|PPP", "Primary Energy", "Emissions|CO2"]
)

# Export local cache
df.to_csv("data/sources/iiasa_ssp/iiasa_ssp_ssp2_world.csv")
```

---

## 📊 Core Trajectory Benchmarks (World — SSP2 Baseline)

| Year | World Population (Billions) | Gross World Product (Trillion USD2010 PPP) | Primary Energy Consumption (EJ/year) | Atmospheric $\text{CO}_2$ (ppm equivalent) |
|:---:|:---:|:---:|:---:|:---:|
| **2020** | 7.84 | 124.5 | 592.4 | 414.2 |
| **2030** | 8.53 | 175.2 | 674.1 | 436.5 |
| **2040** | 9.15 | 234.8 | 748.9 | 462.1 |
| **2050** | 9.68 | 298.4 | 815.3 | 489.8 |
| **2060** | 10.02 | 362.1 | 871.2 | 518.4 |
| **2070** | 10.21 | 425.6 | 915.0 | 546.9 |
| **2080** | 10.28 (Peak) | 488.7 | 948.7 | 574.8 |
| **2090** | 10.24 | 549.3 | 972.1 | 601.2 |
| **2100** | 10.11 | 607.5 | 985.4 | 625.5 |

---

## 📚 References & Citation
* Riahi, K., et al. (2017). *The Shared Socioeconomic Pathways and their energy, land use, and greenhouse gas emissions implications: An overview*. Global Environmental Change, 42, 153–168.
* O'Neill, B. C., et al. (2014). *A new scenario framework for climate change research: the concept of shared socioeconomic pathways*. Climatic Change, 122(3), 387–400.
