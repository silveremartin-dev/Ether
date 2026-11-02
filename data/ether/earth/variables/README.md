# Data Architecture: 1D Variables vs 2D Cartographic Rasters

In conformance with the **Ether High Academic Precision & Zero Arbitrary Hardcoding Directive**, observational datasets and baseline trajectories are ontologically structured by dimensionality and encapsulated under their respective planetary domain:

```
data/ether/earth/
├── -100000/                    # 25 thematic PNG/GeoTIFF raster layers for -100k BP (Eemian / Out of Africa)
├── -10000/                     # 25 raster layers for -10k BP (Early Holocene / Neolithic Revolution)
├── 0/                          # 25 raster layers for Year 0 (Classical Antiquity / Pax Romana)
├── 1347/                       # 25 raster layers for 1347 CE (Late Medieval / Black Death)
├── 1816/                       # 25 raster layers for 1816 CE (Tambora Climate Shock / Year Without Summer)
├── 2025/                       # 25 raster layers for 2025 CE (Contemporary Anthropocene / End of Empirical Era)
│
├── events/
│   └── historical_events_catalog.json # 1,670+ empirical chronological events (volcanoes, wars, tech leaps)
│
├── history/
│   ├── treaties_and_transitions.json  # 29 continuous gapless historical territorial epochs (-3000 to 2060)
│   └── paleo/
│       ├── hominin_hearths.json       # Quaternary hominin hearth sites and dispersal points
│       ├── ice_sheets.json            # Quaternary continental ice sheet boundary polygons
│       └── migration_routes.json      # 19 global Paleolithic migration corridors across Africa, Eurasia, Sahul, Americas
│
├── leaders/
│   └── earth_historical_leaders.json  # 55 empirical historical leaders & catalytic interventions
│
└── variables/                  # 1D Scalar Time-Series & Initial State Baselines (time -> scalar)
    ├── historical/
    │   ├── paleo_sources_1d.json        # Paleolithic Deep Time (-100,000 BP to -10,000 BP)
    │   │                               # Sources: EPICA Dome C (ice core CO2/T°), PaleoClim v1.2, Biraben (1979)
    │   └── hyde_maddison_historical_1d.json # Historical Observational Baseline (-10,000 BP to 2025 CE)
    │                                   # Sources: HYDE 3.4 (demography, land), Maddison Project 2020 (GDP),
    │                                   # ECMWF ERA5 (continuous observational climate series through 2024/2025)
    │
    └── projections/                    # Prospective & Future Benchmark Trajectories (2025 CE to 2100 / 2200 CE)
        ├── predictive_socioeconomic_projections_2026_2100.json # Unified IAM projections
        ├── cmip6_knmi_temperatures_1d.json # KNMI Climate Explorer CMIP6 Multi-Model Mean (baseline 1850-1900 at +13.70°C)
        ├── un_wpp_2024_world_projections_1d.json # UN DESA WPP 2024 Probabilistic Projections (Medium, Low, High)
        ├── ssp1_iamc_ingested_1d.json  # IIASA SSP1 / IEA NZE 2050 (Sustainability & Green Growth)
        ├── ssp2_iamc_ingested_1d.json  # IIASA SSP2 / UN WPP 2024 (Middle of the Road)
        ├── ssp3_iamc_ingested_1d.json  # IPCC SSP3 / Club of Rome World3 (Regional Rivalry & Resource Fragmentation)
        ├── ssp4_iamc_ingested_1d.json  # IIASA SSP4 / GCAM4 (Social Inequality & Stratification)
        ├── ssp5_iamc_ingested_1d.json  # IIASA SSP5-8.5 (Fossil-Fueled Development)
        └── ether_calibrated_projection_1d.json # Ether Biophysical Cliodynamic Trajectory Calibrated 2025-2200
```

## Baseline Reference Trajectory Attribution & Mathematical Continuity
1. **Prehistoric Scenarios ($t_0 < -10\,000\,\text{BP}$)**:
   * Mandatory Reference Model: `PALEO_SOURCES` (EPICA Dome C ice cores, PaleoClim v1.2, Biraben 1979 / Kremer 1993).
2. **Historical Observational Scenarios ($-10\,000\,\text{BP} \le t_0 \le 2025\,\text{CE}$)**:
   * Mandatory Reference Model: `HYDE_MADDISON` (HYDE 3.4, Maddison Project 2020, ECMWF ERA5).
3. **Future Prospective Scenarios ($t_0 \ge 2025\,\text{CE}$)**:
   * Selectable via UI ComboBox between `SSP1`, `SSP2` (default), `SSP3`, `SSP4`, `SSP5`, and `ETHER_CALIBRATED`.
   * Temporal Horizon: Standard SSP and UN WPP projections run up to $2100\,\text{CE}$; `ETHER_CALIBRATED` extends to $2200\,\text{CE}$.
4. **Interpolation & Shock Dynamics (PCHIP)**:
   * All 1D continuous trajectories are interpolated using Monotone Piecewise Cubic Hermite Interpolating Polynomials (PCHIP, Fritsch & Carlson 1980).
   * Monotonicity preservation ensures that abrupt cataclysmic anomalies (such as 1816 Tambora, 536 volcanic winter, or 1347 Black Death) remain localized local extrema ($f'=0$) without generating spurious polynomial overshoot (Runge phenomenon) or unphysical negative populations.
