# NASA, ESA & USGS Astrogeology: Titan Planetary Datasets

This directory contains authentic empirical datasets, cartographic projections, and thermodynamic parameters for Saturn's giant moon **Titan**, compiled from the **Cassini-Huygens mission** (NASA / JPL-Caltech / ESA / ASI), published in *Nature Astronomy*, *Geophysical Research Letters*, *Icarus*, and *Science*.

---

## 🪐 1. Cartographic & Geophysical Datasets

### A. Global Geologic & Geomorphologic Maps
* **Files**:
  * `titan_geologic_map_mollweide_lopes2019.jpg` (Mollweide projection, scale 1:20,000,000)
  * `titan_cassini_geomorphology_lopes2019.png` (Equirectangular + North/South Polar Orthographic Projections)
* **Citation**: Lopes, R. M. C., Malaska, M. J., Schoenfeld, A. M., Le Gall, A., et al. (2020). *A global geomorphologic map of Titan*. **Nature Astronomy**, 4(3), pp. 228–233. DOI: [10.1038/s41550-019-0917-4](https://doi.org/10.1038/s41550-019-0917-4).
* **Official Portals**:
  * ESA Multimedia: [https://www.esa.int/ESA_Multimedia/Images/2019/11/Global_geologic_map_of_Titan](https://www.esa.int/ESA_Multimedia/Images/2019/11/Global_geologic_map_of_Titan)
  * USGS Astrogeology / NASA PDS Imaging Node.
* **Geologic Terrain Classification**:
  | Terrain Unit | Map Color | Description & Cliodynamic Significance | Areal Coverage |
  |---|---|---|---|
  | **Plains** | Pale Green (`#50fa7b`) | Broad, flat, undifferentiated organic lowlands formed by settled photochemical atmospheric tholins. Prime terrain for surface mobility and habitat construction. | ~65% |
  | **Dunes** | Purple (`#9b59b6`) | Vast longitudinal equatorial hydrocarbon sand seas (Belet, Fensal, Shangri-La) shaped by eastward equinoctial wind downdrafts. | ~17% |
  | **Hummocky** | Pale Orange (`#f39c12`) | Elevated, rugged icy bedrock mountain massifs (Mithrim Montes, Xanadu) composed of silicate/water ice bedrock. Highest mechanical bearing capacity. | ~14% |
  | **Labyrinths** | Pink (`#e91e63`) | Tectonically disrupted elevated karst plateaus dissected by methane fluvial channels and canyons. High terrain friction. | ~1.5% |
  | **Lakes & Seas** | Blue (`#2980b9`) | Liquid hydrocarbon reservoirs filled with liquid methane ($CH_4$), ethane ($C_2H_6$), and dissolved nitrogen (Kraken Mare, Ligeia Mare, Punga Mare, Ontario Lacus). Superabundant petrochemical resource. | ~1.5% |
  | **Impact Craters**| Red (`#e74c3c`) | Rare hypervelocity impact structures (Menrva, Selk, Sinlap) excavating deep crystalline crustal water ice. Key sources for extraction of pure $H_2O$ volatiles. | ~0.4% |

---

### B. Global Topographic Elevation Models (DEM)
* **Files**:
  * `titan_topography_equirectangular_corlies2017.webp` (Cylindrical Equirectangular DEM, $-1800\text{ m}$ to $+600\text{ m}$)
  * `titan_topography_polar_projections_corlies2017.webp` (North & South Polar stereographic insets + equatorial strip, $-1600\text{ m}$ to $+700\text{ m}$)
* **Citations**:
  * Corlies, P., Hayes, A. G., Birch, S. P. D., Lorenz, R. D., et al. (2017). *Titan's Topography and Shape at the End of the Cassini Mission*. **Geophysical Research Letters**, 44(23), 11,754–11,761. DOI: [10.1002/2017GL075518](https://doi.org/10.1002/2017GL075518).
  * Lorenz, R. D., Stiles, B. W., Aharonson, O., Lucas, A., et al. (2013). *A global topographic map of Titan*. **Icarus**, 225(1), 367–377. DOI: [10.1016/j.icarus.2013.04.004](https://doi.org/10.1016/j.icarus.2013.04.004).
* **Official Portals**:
  * NASA Image Archive: [https://www.nasa.gov/image-article/global-topographic-map-of-titan/](https://www.nasa.gov/image-article/global-topographic-map-of-titan/)
  * Planetary Society: [https://www.planetary.org/space-images/global-map-of-titan-topography](https://www.planetary.org/space-images/global-map-of-titan-topography)
  * Astrobiology Magazine: [https://astrobiology.com/2018/01/17/new-titan-topographic-map/](https://astrobiology.com/2018/01/17/new-titan-topographic-map/)
  * Zenodo Data Archive: [https://doi.org/10.5281/zenodo.1009187](https://doi.org/10.5281/zenodo.1009187)
* **Hypsometric Characteristics**:
  * **Mean Radius**: $R_0 = 2574.73 \pm 0.09\text{ km}$.
  * **Topographic Variation**: Titan is globally flat, exhibiting a total peak-to-trough range of only $\approx 2\,600\text{ m}$ across the entire planetary body.
  * **Lowest Depression**: Floor of Kraken Mare ($-1\,600\text{ m}$ to $-1\,800\text{ m}$ below mean geoid).
  * **Highest Peak**: Mithrim Montes ($+2\,014\text{ m}$ above mean radius).

---

## 💨 2. Atmosphere, Thermodynamics & Radiative Balance

* **Source File**: `titan_atmospheric_and_climate_parameters.json`
* **Atmospheric Pressure & Density**:
  * **Surface Pressure**: $P_0 = 1.467\text{ bar} \approx 1.5\text{ bar}$ ($150\text{ kPa}$).
  * **Scale Height ($H$)**: $15\text{ km}$ (lower troposphere) to $50\text{ km}$ (stratosphere).
  * **Surface Air Density**: $\rho_{\text{atm}} \approx 5.4\text{ kg/m}^3$ (over $4\times$ denser than Earth sea-level air), allowing winged human flight with minimal thrust.
* **Chemical Composition (Molar Fractions)**:
  * **Nitrogen ($N_2$)**: $94.2\%$
  * **Methane ($CH_4$)**: $5.65\%$
  * **Hydrogen ($H_2$)**: $0.099\%$
  * **Argon ($^{40}Ar$)**: $0.00432\%$ ($43.2\text{ ppm}$)
  * **Trace Organics**: Ethane ($C_2H_6$), Propane ($C_3H_8$), Acetylene ($C_2H_2$), Hydrogen Cyanide ($HCN$), Carbon Monoxide ($CO$).

* **Radiative Temperature & Greenhouse / Anti-Greenhouse Equilibrium**:
  * **Solar Insolation**: Only $\approx 1.07\%$ of Earth insolation ($S_{\text{Titan}} \approx 14.9\text{ W/m}^2$, at $9.54\text{ AU}$).
  * **Effective Radiative Temperature ($T_{\text{eff}}$)**: $82.0\text{ K}$ ($-191.2^\circ\text{C}$).
  * **Greenhouse Warming ($\Delta T_{\text{GH}}$)**: $+21.0\text{ K}$ driven by tropospheric methane absorption bands.
  * **Anti-Greenhouse Cooling ($\Delta T_{\text{anti-GH}}$)**: $-9.0\text{ K}$ driven by organic stratospheric photochemical haze reflecting high-altitude ultraviolet/visible sunlight.
  * **Net Surface Temperature**:
    $$T_{\text{surface}} = T_{\text{eff}} + \Delta T_{\text{GH}} + \Delta T_{\text{anti-GH}} = 82\text{ K} + 21\text{ K} - 9\text{ K} = 94.0\text{ K} \;(-179.15^\circ\text{C})$$
  * At $94\text{ K}$, water ice vapor pressure is zero ($p_{\text{vap}}(H_2O) < 10^{-20}\text{ Pa}$), completely stripping the atmosphere of water vapor and making water ice an impermeable structural stone.

---

## 🌧️ 3. Methane Hydrologic Cycle, Weather & Orbital Seasons

* **Orbital & Rotational Dynamics**:
  * **Tidal Locking**: Titan is tidally locked to Saturn. Rotation period = orbital period = **15 days 22 hours** ($382.0\text{ hours}$).
  * **Axial Tilt (Obliquity)**: Aligned with Saturn's orbital inclination at **$26.73^\circ$** (seasonal cycle of **29.46 Earth years**).
  * **Eccentricity Asymmetry ($e = 0.054$)**: Titan is $12\%$ closer to the Sun during southern summer. Southern summers are shorter, more intense, and warmer, while northern summers are prolonged and cooler, driving the large accumulation of northern liquid seas (Kraken, Ligeia, Punga Mare).
* **Precipitation & Flash Flooding**:
  * **Evaporation Rate**: Extremely low solar flux limits evaporation to $\approx 1\text{ cm/year}$ (compared to $\approx 100\text{ cm/year}$ of water on Earth).
  * **Atmospheric Liquid Holding Capacity**: Titan's dense, cold troposphere can store a liquid methane column of $\approx 10\text{ meters}$ before saturation (compared to only a few centimeters on Earth).
  * **Climatic Regime**: Prolonged decadal hyper-arid droughts punctuated by catastrophic convective methane torrential downpours and flash floods.
* **Surface Winds & Waves**:
  * **Quiescent Winds**: Baseline surface winds are gentle ($< 1\text{ m/s}$).
  * **Equinoctial Storms**: Semi-annual storms produce strong convective downdrafts reaching up to $10\text{ m/s}$, generating the equatorial linear sand dunes.
  * **Polar Cyclones**: Evaporation from Kraken and Ligeia Mare during northern summer can generate polar cyclones lasting up to $10\text{ days}$ with gale-force winds up to $20\text{ m/s}$ and waves of $1.5$ to $10\text{ cm}$.

---

## 🔗 4. External Verification Links & References

1. [Nature Astronomy: A global geomorphologic map of Titan (Lopes et al. 2020)](https://doi.org/10.1038/s41550-019-0917-4)
2. [Geophysical Research Letters: Titan's Topography and Shape (Corlies et al. 2017)](https://doi.org/10.1002/2017GL075518)
3. [ESA Multimedia: Global Geologic Map of Titan](https://www.esa.int/ESA_Multimedia/Images/2019/11/Global_geologic_map_of_Titan)
4. [NASA Image Article: Global Topographic Map of Titan](https://www.nasa.gov/image-article/global-topographic-map-of-titan/)
5. [The Planetary Society: Global Map of Titan Topography](https://www.planetary.org/space-images/global-map-of-titan-topography)
6. [Astrobiology Magazine: New Titan Topographic Map](https://astrobiology.com/2018/01/17/new-titan-topographic-map/)
7. [Zenodo Open Topographic Data Repository](https://doi.org/10.5281/zenodo.1009187)
8. [Wikipedia: Atmosphere of Titan](https://en.wikipedia.org/wiki/Atmosphere_of_Titan)
9. [Wikipedia: Climate of Titan](https://en.wikipedia.org/wiki/Climate_of_Titan)
