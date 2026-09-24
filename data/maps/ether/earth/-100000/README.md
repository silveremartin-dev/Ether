# Cartographic & Cultural Tensor Provenance: Earth (-100,000 BP)

## 🌍 1. Overview & Historical Scope
* **Epoch**: $-100\,000\text{ BP}$ (Late Pleistocene, MIS 5c-d Transition, Early Out-of-Africa Inception)
* **Planet**: Earth (*Terre*)
* **Grid Resolution**: $2048 \times 1024$ Equirectangular ($0.175^\circ \times 0.175^\circ$ per pixel, $\approx 19.5\text{ km}$ at equator)
* **Global Population**: $\approx 85\,000 \text{ to } 150\,000$ individuals across Africa, Europe, and Asia.
* **Global Ecosystem State**: Pristine, untouched natural reserves; zero agriculture; hunter-gatherer and scavenging modes of production.

---

## 🔬 2. Deep Scientific Justification: Demographics & Hominin Clades

### 2.1 Bioenergetics, Metabolic Rates & Carrying Capacity

The demographic density $D(\mathbf{x})$ at coordinate $\mathbf{x} = (\lambda, \phi)$ is determined by the local Net Primary Productivity $\text{NPP}(\mathbf{x})$, trophic level efficiency $\eta_{\text{trophic}}$, and the daily per-capita metabolic requirement $\mathcal{E}_{\text{req}}$ of each hominin species:

$$K(\mathbf{x}) = \frac{\text{NPP}(\mathbf{x}) \cdot \eta_{\text{trophic}} \cdot \alpha_{\text{biome}}}{\mathcal{E}_{\text{req}} \cdot 365.25}$$

Where:
* **$\text{NPP}(\mathbf{x})$**: Net Primary Productivity (in $\text{g C}\cdot\text{m}^{-2}\cdot\text{yr}^{-1}$ or equivalent $\text{MJ}\cdot\text{m}^{-2}\cdot\text{yr}^{-1}$) derived from CHELSA-Trace21k and PaleoView v1.2.
* **$\eta_{\text{trophic}}$**: Human trophic level efficiency ($\approx 0.01$ for hyper-carnivorous tundra hunters; $\approx 0.05 - 0.10$ for tropical gatherer-hunters eating roots, fruits, and small game).
* **$\alpha_{\text{biome}}$**: Edible fraction multiplier for human digestive physiology.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                    PHYSIOLOGICAL & CALORIC DIFFERENTIALS BY HOMININ SPECIES                     │
├────────────────────────────────┬──────────────────────┬──────────────────────┬──────────────────┤
│ Parameter / Trait              │ Homo sapiens         │ H. neanderthalensis  │ Denisovans / Arc │
├────────────────────────────────┼──────────────────────┼──────────────────────┼──────────────────┤
│ Mean Daily Caloric Need        │ 2 200 - 2 600 kcal   │ 4 000 - 5 500 kcal   │ 3 000 - 3 800 kca│
│ Dominant Trophic Diet          │ Omnivorous (30-60% V)│ Hyper-carnivorous    │ Omnivore/Cold St.│
│ Biome Thermal Regime           │ Savanna/Tropical/Lev.│ Periglacial / Tundra │ Cold Steppe / Pl.│
│ Mean Carrying Capacity (K)     │ 0.025 - 0.045 hab/km²│ 0.005 - 0.015 hab/km²│ 0.008 - 0.020 hab│
│ Social Band Organization       │ Bilateral Exogamous  │ Patrilocal Inbred    │ Nomadic Bands    │
│ Estimated Global Headcount     │ 55 000 - 95 000      │ 15 000 - 30 000      │ 15 000 - 25 000  │
└────────────────────────────────┴──────────────────────┴──────────────────────┴──────────────────┘
```

#### Why Neanderthal Density is 3× Lower than Sapiens:
1. **Bioenergetic Hyper-Carnivory**:
   * Studies on Neanderthal cold adaptation and heavy muscle mass (Froehle & Churchill 2009; Sorensen 2011) demonstrate that Neanderthals in periglacial Europe required $\approx 4\,500 \text{ to } 5\,000\text{ kcal/day}$ to sustain basal metabolic homeostasis in winter.
   * Isotopic $\delta^{15}\text{N}$ and $\delta^{13}\text{C}$ bone collagen analyses (Richards & Trinkaus 2009) confirm an apex predator trophic position (higher than hyenas and wolves), hunting large megafauna (mammoths, woolly rhinos, steppe bison).
   * Because energy transfer between trophic levels is lossy ($\approx 10\%$), an apex carnivore requires a foraging territory $5\times \text{ to } 10\times$ larger than a generalist tropical gatherer.
2. **Genetic Evidence of Low Population Size & High Inbreeding**:
   * Complete high-coverage genomes from Neanderthal fossils (El Sidrón, Chagyrskaya, Vindija; Prüfer et al. 2014, Skov et al. 2022) exhibit extreme runs of homozygosity (ROH), proving that Neanderthal groups rarely exceeded $15 - 30$ individuals with low inter-band gene flow.

---

### 2.2 Mathematical Soft-Voronoi & Barrier Formulations

The geographical distribution of hominin clades is computed via an anisotropic Gaussian Soft-Voronoi field across 42 verified archaeological hearths:

$$w_i(\lambda, \phi) = \frac{\exp\left(-\frac{d_i(\lambda, \phi) - d_{\min}}{\sigma}\right)}{\sum_{k=1}^N \exp\left(-\frac{d_k(\lambda, \phi) - d_{\min}}{\sigma}\right)}$$

Where:
* $\sigma = 3.5^\circ$ ($\approx 380\text{ km}$) defines the smooth cultural transition gradient between neighboring bands.
* $d_i(\lambda, \phi) = \min_{h \in \mathcal{H}_i} \|\mathbf{x} - \mathbf{h}\| + \mathcal{P}_{\text{barrier}}(\mathbf{x})$ includes physical cost penalties:
  * **Mediterranean Marine Barrier**: $\mathcal{P}_{\text{med}} = +50.0$ (Strict isolation: Sapiens cannot cross into Iberia/Italy; Neanderthals cannot cross into North Africa at 100 ka BP).
  * **Himalayan Mountain Crest**: $\mathcal{P}_{\text{himalayas}} = 16.0 \cdot \exp\left(-\frac{(\phi - 32.0)^2 + ((\lambda - 85.0)\cdot 0.55)^2}{70.0}\right)$ (Isolates Tibetan Denisovans from Indian archaic hominins).

---

### 2.3 Strict Geographic Exclusion Masks ($D = 0.0\text{ hab/km}^2$)

The simulation enforces strict uninhabited status on:
1. **The Americas (North, Central, South)**:
   * Archaeological and genomic consensus (Raghavan et al. 2015, Waters 2019) places the earliest human presence after the Last Glacial Maximum ($\sim 25\,000 - 16\,000\text{ BP}$).
2. **Sahul (Australia, New Guinea, Tasmania)**:
   * First maritime crossing via Wallacea occurred around $\approx -50\,000\text{ BP}$ (Clarkson et al. 2017 *Madjedbebe*).
3. **Madagascar & Remote Ocean Islands**:
   * Madagascar was colonized $\approx 500\text{ CE}$ by Austronesian and Bantu navigators.
   * Iceland, Azores, Canaries, Hawaii, New Zealand were uninhabited throughout the Pleistocene.
4. **Glaciated Polar Domes**:
   * Antarctica and high latitudes north of $64^\circ\text{N}$.

---

## 🧩 3. Decoupling: Initial Maps vs. Dynamical Cliodynamic Engines

To maintain scientific integrity and prevent circular simulation bugs, Ether strictly separates:

$$\text{Simulation State at } t = \underbrace{\mathcal{T}_0(\mathbf{x})}_{\text{Static Initial Cartographic Tensor}} + \int_{t_0}^t \underbrace{\mathcal{F}_{\text{cliodynamic}}(\mathbf{S}(t), \nabla \mathbf{S}(t)) \, dt}_{\text{Dynamical Simulation Kernel}}$$

1. **The Cartographic Tensor Layer ($t = t_0 = -100\,000\text{ BP}$)**:
   * Provides the frozen initial boundary conditions (topography, virgin water tables, pristine ore deposits, initial band locations, temperature baseline).
2. **The Dynamical Simulation Engine ($t > t_0$)**:
   * `ThermodynamicMigrationEngine`: Simulates nomadic band displacement driven by local calorie exhaustion.
   * `AlbedoClimateEngine`: Computes dynamical Milankovitch climate drift, ice sheet advance, and albedo feedback.
   * `CulturalSociologyEngine`: Calculates linguistic drift, value transmission, and sociopolitical evolution.
   * `MalthusianDemographicEngine`: Simulates births, mortality spikes, and carrying capacity adaptations.

---

## 📊 4. Exhaustive 25-Layer Cartographic Tensor Inventory

| Layer Name | File Name | Category | Primary Empirical Sources | Detailed Reconstitution Method & Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **Elevation & Bathymetry** | `earth_-100000_elevation.png` | Géophysique | NOAA ETOPO 2022, GEBCO 2023 | Adjusted for MIS 5c eustatic sea level offset ($\approx -18.5\text{ m}$), exposing Sundaland & Beringia shelves. |
| **Biomes** | `earth_-100000_biomes.png` | Écologie | PaleoView v1.2, CHELSA-Trace21k | Expansion of African humid savannas (Green Sahara) and mammoth steppe north of $48^\circ\text{N}$. |
| **Surface Temperature** | `earth_-100000_temperature.png` | Climat | EPICA Dome C $\delta\text{D}$, NGRIP $\delta^{18}\text{O}$ | Mean annual temperature grid calibrated with orbital insolation for 100 ka BP ($T_{\text{mean}} = 14.1^\circ\text{C}$). |
| **Precipitation** | `earth_-100000_precipitation.png` | Climat | Sanbao & Hulu speleothems, Soreq Cave | Enhanced summer African/Asian monsoons creating wet dispersal corridors through Arabia. |
| **Seasonality** | `earth_-100000_seasonality.png` | Climat | Milankovitch orbital calculations (Laskar 2004) | Obliquity at $24.04^\circ$ driving pronounced continental temperature range in temperate latitudes. |
| **Population Density** | `earth_-100000_density.png` | Démographie | Binford (2001), Tallavaara et al. (2015), HYDE 3.4 | NPP-constrained forager carrying capacity ($\approx 0.01 - 0.045\text{ hab/km}^2$) with strict geographic exclusion masks. |
| **Isogloss / Linguistics** | `earth_-100000_isogloss.png` | Linguistique | Atkinson (2011), Nichols (1992), Glottolog | Soft-Voronoi Gaussian interpolation across 42 verified archaeological hearths with mountain/marine barriers. |
| **Sovereignty / Polity** | `earth_-100000_sovereignty.png` | Politique | Boehm (1999), Carneiro (1970) | Pre-state egalitarian band territories ($30-80$ individuals) bounded by watersheds and crest lines. |
| **Kinship Structure** | `earth_-100000_kinship.png` | Sociologie | Murdock EA (EA022, EA024), Foley & Gamble (2009) | Bilateral exogamous networks (*Sapiens*) vs patrilocal inbred clades (*Neanderthals*). |
| **Ritual Traditions** | `earth_-100000_rituals.png` | Culture | Blombos ochre, Qafzeh burials, Shanidar pollen | Middle Stone Age proto-animism, body pigments, and deliberate burial practices. |
| **Technology / Lithics** | `earth_-100000_technology.png` | Technologie | Bordes (1961), McBrearty & Brooks (2000) | Mode 3 Levallois prepared cores, hafted points, and Schöningen wooden hunting spears. |
| **Institutions** | `earth_-100000_institutional.png` | Institutions | Carneiro Scale (Level 0), Flannery & Marcus (2012) | Uniform egalitarian baseline score ($0.02$) with absence of social classes or hereditary chiefs. |
| **Ecological Footprint** | `earth_-100000_ecological.png` | Environnement | Ellis et al. (2021) Anthrome 6 | Minimal foraging footprint with localized fire-stick management and megafauna hunting pressure. |
| **Pathogen Pressure** | `earth_-100000_pathogen.png` | Épidémiologie | Wolfe et al. (2007), ancient pathogen genomics | High tropical vector-borne load (proto-malaria) vs cold-suppressed boreal Eurasian pathogen load. |
| **Trade Networks** | `earth_-100000_tradenetwork.png` | Économie | Marean et al. (2007), Renfrew (1975) | Down-the-line lithic exchange corridors (obsidian, silcrete, marine shells, pigment) up to $150\text{ km}$. |
| **Aquifers / Water Tables** | `earth_-100000_aquifers.png` | Ressources | UNESCO WHYMAP | $100\%$ pristine, untouched virgin fossil water tables (Nubian, Ogallala, Congo, Great Artesian). |
| **Coal Reserves** | `earth_-100000_coal.png` | Ressources | USGS World Coal Inventory, GEM | $100\%$ untouched reserves with abundant surface outcroppings. |
| **Oil Reserves** | `earth_-100000_oil.png` | Ressources | USGS World Petroleum Assessment | $100\%$ virgin reserves with active natural surface tar/bitumen seeps (Baku, Hit, Gulf). |
| **Natural Gas** | `earth_-100000_gas.png` | Ressources | USGS Global Gas Survey | $100\%$ virgin subterranean reserves. |
| **Iron & Copper Ore** | `earth_-100000_iron_copper.png` | Ressources | USGS MRDS, BGR Germany | Unmined banded iron formations (BIF) and rich surface malachite/native copper gossans. |
| **Precious Metals** | `earth_-100000_precious_metals.png` | Ressources | USGS MRDS Alluvial Placers | Intact alluvial placer gold and silver beds in major river basins. |
| **Uranium Ore** | `earth_-100000_uranium.png` | Ressources | IAEA NFCIS, OECD-NEA Red Book | Pristine high-grade unconformity uranium reserves (Athabasca, Olympic Dam, Shinkolobwe). |
| **Rare Earth Elements** | `earth_-100000_rare_earths.png` | Ressources | USGS REE Global Database | Untouched carbonatite and alkaline complexes (Bayan Obo, Mountain Pass, Lovozero). |
| **Geothermal Heat Flux** | `earth_-100000_geothermal.png` | Géophysique | IHFC Davies (2013) Global Heat Flow | Crustal heat dissipation along tectonic margins and volcanic arcs ($40 - 120\text{ mW/m}^2$). |
| **Helium-3** | `earth_-100000_helium3.png` | Ressources | Solar Wind Volatiles Model | Terrestrial background ($\approx 0.0\text{ ppb}$) due to geomagnetic shielding. |
