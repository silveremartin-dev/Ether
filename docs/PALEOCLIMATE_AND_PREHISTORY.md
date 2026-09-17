# Prehistoric Simulation, Palaeoclimate & Hominin Biogeography (-300,000 BP to Present)
**Ether Simulation Engine — Academic & Technical Reference Specification**
*Authors: Silvere Martin-Michiellot, Gemini AI Assistant (Google DeepMind)*
*Version: 1.0.0-beta.1 (v1.0 b1) | Date: September 2026*

---

## 1. Executive Summary & Academic Foundation

This document defines the mathematical, palaeoanthropological, geophysical, and computational architecture governing deep prehistoric population density, paleoclimate, eustatic sea-level evolution, glacial isostatic adjustment (GIA), megafauna extinction dynamics, and N-dimensional cultural tensor mapping in the **Ether** simulation engine.

The system spans a temporal horizon from the emergence of *Homo sapiens* in the Middle Paleolithic ($\sim -300,000$ BP / MIS 8) through the Last Glacial Maximum (LGM, $\sim -26,500$ to $-19,000$ BP), the Holocene agricultural transition ($\sim -11,700$ BP / $-9,700$ BCE), and into the modern anthropogenic epoch.

```
       DEEP PREHISTORIC SIMULATION ENGINE ARCHITECTURE
+---------------------------------------------------------------+
| Epoch T₀ Thresholds (-300,000 BP ➔ -10,000 BP ➔ Modern)       |
+---------------------------------------------------------------+
       |                                       |
       v (T₀ < -10,000 BP)                     v (T₀ ≥ -10,000 BCE)
+-------------------------------+       +-------------------------------+
| On-Demand 3-Layer Hybrid      |       | Empirical Ingestion Engine    |
| (Fossils + NPP + Containment) |       | (HYDE 3.4 / Goldewijk et al.) |
+-------------------------------+       +-------------------------------+
       |                                       |
       +-------------------+-------------------+
                           |
                           v
+---------------------------------------------------------------+
| Persistent Disk Cache (data/maps/cache/ Base64 PNG + Rasters) |
+---------------------------------------------------------------+
```

---

## 2. Chronological Timeline & Hominin Biogeography

### 2.1 Chronological Milestones (MIS 8 to Holocene)

| Epoch / Era | Time Window (BP) | Marine Isotope Stage (MIS) | Hominin Distribution & Biogeographical Status |
| :--- | :--- | :--- | :--- |
| **Early Middle Paleolithic** | $-300,000$ to $-115,000$ | MIS 8 – MIS 5e | *Homo sapiens* core in Africa; *H. neanderthalensis* in W. Eurasia; *H. denisova* in E. Eurasia. **Sahul = 0, Americas = 0**. |
| **Out of Africa Expansion** | $-115,000$ to $-50,000$ | MIS 5d – MIS 3 | Major Sapiens expansion across Southern Eurasia. Early coastal dispersal along Indian Ocean. **Americas = 0**. |
| **Sahul Migration** | $-50,000$ to $-25,000$ | MIS 3 – MIS 2 | Sahul (Australia + New Guinea + Tasmania) populated ($\sim -50\text{k}$ to $-45\text{k}$ BP). Neanderthal extinction ($\sim -40\text{k}$ to $-35\text{k}$ BP). **Americas = 0**. |
| **LGM & Beringia Crossing** | $-25,000$ to $-14,000$ | MIS 2 (LGM Peak) | Eustatic sea level drop ($-120\text{m}$). Beringian standstill; human expansion into N. & S. America ($\sim -20\text{k}$ to $-14\text{k}$ BP). |
| **Deglaciation & Holocene** | $-14,000$ to $-10,000$ | MIS 1 Transition | Ice sheet retreat; Doggerland inundation; Quaternary Megafauna Extinction (QME); Early agriculture in Fertile Crescent. |
| **Holocene & Historic Era** | $-10,000$ to Present | MIS 1 (Holocene) | Full HYDE 3.4 empirical raster integration ($10000\text{BC}$ to $2023\text{AD}$). |

---

## 3. Geophysical & Palaeoclimate Sub-Systems

### 3.1 Eustatic Sea Level & Palaeobathymetry ($\Delta z_{\text{sea}}$)
During glacial peaks, global eustatic sea level drops up to $-120\text{m}$ to $-130\text{m}$ below modern sea level (MSL), exposing major continental shelf features:

$$\Delta z_{\text{sea}}(t) = -120 \cdot \left( \frac{V_{\text{ice}}(t)}{V_{\text{LGM}}} \right) \quad [\text{meters}]$$

- **Doggerland & English Channel (La Manche)**: At LGM, the English Channel and North Sea are entirely exposed as a dry tundra plain connecting Great Britain directly to continental Europe.
- **Sundaland & Sahul**: Low sea levels join Sumatra, Java, Borneo, and Malaya into the *Sunda Shelf*; Australia, Tasmania, and New Guinea form the *Sahul Continent*, separated by the deep-water trenches of Wallacea.
- **Beringia Land Bridge**: The Bering Strait emerges as a $1,000\,\text{km}$-wide unglaciated steppe corridor between Chukotka (Siberia) and Alaska/Yukon.

### 3.2 Glacial Isostatic Adjustment (GIA) & Crustal Uplift
Ice sheet accumulation exerts enormous vertical pressure, causing lithospheric depression ($\omega_{\text{dep}}$) and asthenospheric mantle displacement. Upon deglaciation ($\sim -19,000$ to $-8,000$ BP):

$$\frac{\partial w(r,t)}{\partial t} = -\frac{1}{\tau} \left( w(r,t) - w_{\text{eq}}(r) \right)$$

where $\tau \approx 1,000 - 5,000$ years is the mantle relaxation time constant.
- **Isostatic Rebound**: Fennoscandia and Northern Britain undergo rapid vertical crustal uplift (up to $+250\,\text{m}$ in Sweden/Baltic), while surrounding forebulge coastal areas (e.g. Doggerland) experience subsidence, hastening marine transgression.

### 3.3 Sea-Ice Traversal & Sub-Glacial Margin Habitability
During glacial winters, sea-ice (*pack ice*) freezes over northern ocean margins:
- Non-zero hunter-gatherer population density ($\rho > 0$) is allowed on frozen sea-ice cells (`isOceanShelf && isFrozenWinterIce`) along coastal hunting routes (e.g., seal and marine mammal harvesting by Paleolithic groups).

### 3.4 Milankovitch Orbital Forcing & Summer Insolation ($\mathcal{I}_{65\text{N}}$)
Orbital forcing controls solar radiation anomalies at high northern latitudes ($65^\circ\text{N}$), modulating ice-age glaciation cycles and tropical monsoon intensity:

$$\mathcal{I}_{65\text{N}}(t) = \mathcal{I}_0 + A_{\text{ecc}} \cdot e(t) + A_{\text{obl}} \cdot \sin(\varepsilon(t)) + A_{\text{prec}} \cdot e(t) \sin(\varpi(t)) \quad [\text{W/m}^2]$$

- **African Humid Periods (Green Sahara)**: Peak summer insolation ($\mathcal{I}_{65\text{N}} > 480\,\text{W/m}^2$) intensifies the West African Monsoon, transforming the Sahara into a lush grassland network of mega-lakes and river corridors (MIS 5e, MIS 1 / Holocene $15,000$ to $5,000$ BP), enabling Out-of-Africa hominin migrations.

### 3.5 Abrupt Palaeoclimate Dynamics: Dansgaard-Oeschger (D-O) Cycles & Heinrich Events
The engine simulates millennial-scale climate abruptness characteristic of the Upper Paleolithic (MIS 3 / MIS 2):
- **Dansgaard-Oeschger (D-O) Oscillations**: Rapid warming spikes of $+8^\circ\text{C}$ to $+15^\circ\text{C}$ over Greenland/Eurasia occurring in less than 50 years, followed by gradual cooling phases.
- **Heinrich Events (H1–H6)**: Massive ice-rafting discharge events in the North Atlantic that shutdown the Atlantic Meridional Overturning Circulation (AMOC). AMOC collapse triggers extreme drop in Eurasian precipitation and temperature, forcing severe population bottlenecks ($\rho \to 0$ in northern Europe) and pushing Neanderthal and Sapiens groups into southern refugia.

### 3.6 Dynamic Ice Sheet Topography & LGM Glacial Refugia
- **Ice Sheet Topography Masking**: Continental ice sheets (Laurentide, Cordilleran, Fennoscandian, British-Irish, Barents-Kara) are modeled as dynamic spatial masks ($H_{\text{ice}} > 0 \implies \rho_{\text{pop}} = 0$).
- **LGM Glacial Refugia ($R_{\text{refugia}}$)**: During the LGM peak ($-26,500$ to $-19,000$ BP), human survival is constrained to sheltered climatic refugia with non-zero net primary productivity:
  - *European Refugia*: Franco-Cantabrian region (Dordogne/Pyrenees), Iberian Peninsula, Italian Peninsula, Balkans.
  - *Asian & American Refugia*: Beringian Standstill corridor (Alaska/Yukon), Southern China, Trans-Urals.

### 3.7 Ice-Core $\text{CO}_2$ Coupling & C3/C4 Vegetation NPP Efficiency
Atmospheric $\text{CO}_2$ concentrations (ingested from EPICA Dome C / Vostok ice cores) govern plant photosynthetic productivity:

$$\text{NPP}(t) = \text{NPP}_0 \cdot \left[ 1 + \beta \ln\left(\frac{[\text{CO}_2](t)}{[\text{CO}_2]_{\text{LGM}}}\right) \right] \cdot f(T, P)$$

- **Glacial Low $\text{CO}_2$ ($180\,\text{ppm}$ at LGM vs $280\,\text{ppm}$ Holocene)**: Suppresses C3 forest growth and expands C4 drought-tolerant steppe-tundra grasslands, fueling vast herds of Quaternary megafauna (mammoths, woolly rhinos, bison).

### 3.8 Palaeohydrology, Mega-Lakes & Trans-Saharan Corridors
- **Endorheic Mega-Lakes**: Simulates historic mega-lakes including **Lake Mega-Chad** ($350,000\,\text{km}^2$), **Lake Bonneville**, **Lac Lahontan**, and **Lake Agassiz**.
- **Trans-Saharan Paleoriver Networks**: Models dry riverbeds (*wadis* / the Irharhar and Tammamaat river systems) becoming active freshwater highways during humid interglacials, connecting Sub-Saharan Africa directly to the Mediterranean coast.


---

## 4. Anthropogenic Fire Regimes & Megafauna Extinction

### 4.1 Pyrotechnology & Fire-Stick Farming
Human hunter-gatherers use fire as a primary landscape management tool:

$$\frac{\partial B_{\text{biomass}}}{\partial t} = P_{\text{npp}} - D_{\text{decay}} - F_{\text{burn}} \cdot \rho_{\text{pop}}$$

- **Australia (Aboriginal Fire-Stick Farming, $-50,000$ BP onwards)**: Systematic mosaic landscape burning altered vegetation regimes from fire-sensitive rain-forests/woodlands to pyrophytic *Eucalyptus* and spinifex grasslands, accelerating continental aridification.
- **Biomass Burning Tensors**: Tracks anthropogenic vs. natural paleofire frequency across biomes.

### 4.2 Quaternary Megafauna Extinction (QME) Model
Megafauna collapse is modeled as a synergistic interaction between human hunting pressure ($K_{\text{hunting}}$) and climate stress ($C_{\text{climate}}$):

$$\frac{d M_{\text{megafauna}}}{dt} = r \cdot M \cdot \left(1 - \frac{M}{K}\right) - \gamma \cdot \rho_{\text{human}} \cdot M - \mu \cdot C_{\text{climate}} \cdot M$$

- **Australia ($\sim -46,000$ to $-40,000$ BP)**: Extinction of *Diprotodon*, *Genyornis newtoni*, and *Thylacoleo carnifex*.
- **Eurasia ($\sim -14,000$ to $-10,000$ BP)**: Extinction of Woolly Mammoth (*Mammuthus primigenius*), Woolly Rhino (*Coelodonta*), and Cave Lion.
- **Americas ($\sim -13,000$ to $-11,000$ BP)**: Extinction of *Smilodon*, *Mammut americanum*, *Megatherium*, and *Glyptodon*.

### 4.3 Super-Volcanic Catastrophes & Volcanic Winter (Toba Eruption, $-74,000$ BP)
- **Stratospheric Aerosol Injection**: Super-volcanic eruptions (e.g. Mount Toba, Sumatra $\sim -74,000$ BP / VEI 8) inject over $10^{15}\,\text{g}$ of $\text{SO}_2$ into the upper atmosphere.
- **Global Volcanic Winter**: Triggers a global temperature drop of $-3.0^\circ\text{C}$ to $-5.0^\circ\text{C}$ for 6–10 years, causing severe planetary vegetation die-off and shrinking the global *Homo sapiens* effective population to a bottleneck of $\sim 10,000$ breeding individuals.

### 4.4 Hominin Interspecies Hybridization & Adaptive Admixture
Modélisation des événements d'admixture paléogénétique et de transfert d'allèles adaptatifs :
- **Introgression Vectors**: Tracks admixture fractions ($\alpha_{\text{Neanderthal}} \sim 1\text{--}2\%$, $\alpha_{\text{Denisova}} \sim 4\text{--}6\%$) across migrating hominin populations.
- **Adaptive Archaic Introgression**: Introgression of beneficial archaic genes:
  - *EPAS1 Gene (Denisovan)*: High-altitude hypoxia adaptation allowing settlement of the Tibetan Plateau ($>4,000\,\text{m}$).
  - *TLR1/6/10 Gene Cluster (Neanderthal)*: Innate immune defense against Eurasian pathogens.

### 4.5 Lithic Technology Transitions & Mode 1–5 Efficiency Multipliers
Energy extraction efficiency ($E_0 / P_0$) is parameterized by lithic industrial modes:
- **Mode 1 (Oldowan)**: Simple pebble choppers ($E_{\text{mult}} = 1.0$).
- **Mode 2 (Acheulean)**: Bifacial handaxes and cleavers ($E_{\text{mult}} = 1.25$).
- **Mode 3 (Mousterian / Levallois)**: Prepared-core flake technology ($E_{\text{mult}} = 1.6$).
- **Mode 4 (Upper Paleolithic)**: Blade technology, bone needles, spearthrowers/atlatls ($E_{\text{mult}} = 2.4$).
- **Mode 5 (Mesolithic Microliths)**: Composite barbed arrows and harpoons ($E_{\text{mult}} = 3.2$).

### 4.6 Biotic Trophic Herds & Migratory Fauna Dynamics
- **Migratory Fauna Tensors**: Models seasonal movements of herbivore herds (Mammoths, Reindeer, Steppe Bison, Wild Horses) tracking pasture NPP.
- **Hominin Transhumance**: Human band movements are coupled to animal migration corridors, determining nomadic seasonal camp positions.

### 4.8 Parietal Art, Symbolic Materiality & Ritual Asabiyyah ($45,000$ BP)
* **Domaine d'Application** : Cognition, Sociologie Rituelle & Cohésion d'Agrégation
* **Fenêtre Temporelle / Déclenchement** : $T \le -45,000$ BP (Chauvet, Lascaux, Sulawesi)
* **Zone Géographique / Biome** : Eurasia Calcaires Karstiques & Grottes Ornées
* **Culture / Mode Lithique** : Mode 4 (Châtelperronien, Aurignacien, Magdalénien)
* **Plugin Optionnel Associé** : `ParietalArtAsabiyyahEngine`

- **Symbolic Culture Acceleration**: Parietal cave art, ochre processing, and personal ornaments (beads/shell pendants) act as ritual technologies.
- **Asabiyyah Boosting**: Parietal art sanctuaries increase intra-group cohesion ($\text{Asabiyyah}$) and facilitate inter-band marriage networks during seasonal aggregation rites.

### 4.9 Paleolanguage Emergence & Isogloss Phonetic Drift
* **Domaine d'Application** : Linguistique Évolutive & Dérive Cultuelle
* **Fenêtre Temporelle / Déclenchement** : $T \le -100,000$ BP (Période Paléolithique Inférieure & Moyenne)
* **Zone Géographique / Biome** : Global (Isolement par reliefs & vallées)
* **Culture / Mode Lithique** : Modes 1 à 5 (Oldowan à Microlithes)
* **Plugin Optionnel Associé** : `PaleoLanguageDriftEngine`

- **Proto-Language Structure**: Simulates language continuum evolution from proto-syntax to complex grammar.
- **Phonetic Drift ($\alpha_{\text{drift}}$)**: Isolated clans in separate valleys undergo dialectal drift:

$$\frac{\partial S_{\text{lang}}(x,y,t)}{\partial t} = D_{\text{lang}} \nabla^2 S_{\text{lang}} + \sigma_{\text{drift}} \cdot \eta(x,y,t)$$

### 4.10 Paleodemographic Life Tables & Band Fission-Fusion
* **Domaine d'Application** : Paléodémographie & Structure Sociale Nomade
* **Fenêtre Temporelle / Déclenchement** : $T \le -10,000$ BP (Sociétés de Chasseurs-Cueilleurs Nomades)
* **Zone Géographique / Biome** : Universel (Toutes zones non-sédentaires)
* **Culture / Mode Lithique** : Modes 1 à 4 (Paléolithique Inférieur à Supérieur)
* **Plugin Optionnel Associé** : `DemographicLifeTableEngine`

- **Life Tables**: High infant mortality ($\sim 40\%$), average life expectancy at birth ($25\text{--}30$ years), and high maternal mortality.
- **Band Fission-Fusion**: Nomadic foraging bands operate at a baseline size of $25\text{--}50$ individuals. Exceeding $50$ individuals triggers band fission into daughter foraging units.

### 4.11 Karst Geomorphology & Cave Micro-Shelters
* **Domaine d'Application** : Géomorphologie & Thermorégulation Hivernale
* **Fenêtre Temporelle / Déclenchement** : Peak Glaciaires ($T \le -20,000$ BP)
* **Zone Géographique / Biome** : Massifs Calcaires Karstiques (Périgord, Jura Souabe, Atapuerca, Levant)
* **Culture / Mode Lithique** : Modes 2 à 4 (Acheuléen, Moustérien, Aurignacien)
* **Plugin Optionnel Associé** : `KarstCaveShelterEngine`

- **Karst Topography Raster**: Limestone karst formations provide natural cave networks.
- **Winter Micro-Shelter**: Cave cells afford thermal insulation, reducing winter mortality coefficients during glacial cycles.

### 4.12 Winter Snowpack Friction & Snowshoe/Ski Invention ($\sim -10,000$ BP)
* **Domaine d'Application** : Mobilité Hivernale & Technologie Sub-Arctique
* **Fenêtre Temporelle / Déclenchement** : $T \le -10,000$ BP (Invention du lac Vis, Russie)
* **Zone Géographique / Biome** : Toundra, Taïga & Zones à Enneigement Persistant
* **Culture / Mode Lithique** : Mode 5 (Microlithes & Épipaléolithique/Mésolithique)
* **Plugin Optionnel Associé** : `SnowpackMobilityEngine`

- **Snowpack Friction**: Deep winter snowpack penalizes movement speed ($\text{Friction} \times 3.0$).
- **Snowshoe & Ski Invention**: Development of snowshoes and early skis offsets snowpack penalties, unlocking sub-arctic winter hunting.

### 4.13 Obsidian & Lithic Provenance Trade Networks (XRF Geochemical Tracing)
* **Domaine d'Application** : Réseaux Économiques & Géochimie de Provenance
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Paléolithique Supérieur Récent & Mésolithique)
* **Zone Géographique / Biome** : Corridors Volcaniques/Sédimentaires (Melos, Anatolie, Caucase)
* **Culture / Mode Lithique** : Modes 4 et 5 (Débitage Laminaire & Microlithes)
* **Plugin Optionnel Associé** : `LithicTradeProvenanceEngine`

- **Lithic Provenance Corridors**: Obsidian and high-grade flint exchange networks traced from volcanic/sedimentary quarry sources.
- **Cost-Distance Energy Surface**: Trade corridors follow least-cost isotropic energy paths:

$$\mathcal{C}(x,y) = \int_{\mathcal{P}} \left( 1.0 + \kappa \cdot \tan^2(\text{slope}) \right) ds$$

### 4.14 Tool Kit Maintenance & Raw Material Distance Decay
* **Domaine d'Application** : Économie Lithique & Rendement Mécanique
* **Fenêtre Temporelle / Déclenchement** : Universel Paléolithique ($T \le -10,000$ BP)
* **Zone Géographique / Biome** : Périmètres d'Éloignement des Carrières Lithiques
* **Culture / Mode Lithique** : Modes 1 à 5 (Toutes industries de taille)
* **Plugin Optionnel Associé** : `ToolKitMaintenanceEngine`

- **Knapping Efficiency Ratio**: Mechanical energy extraction efficiency ratio $\eta_{\text{lithic}} = \frac{E_{\text{net\_calories}}}{E_{\text{investment}}}$ as a function of edge-length per unit mass ($\text{cm/g}$).
- **Quarry Distance Penalty**: Distance from raw material sources induces tool edge degradation, decreasing hunting yield unless compensated by lithic trade corridors.

### 4.15 Fire-Stick Farming & Anthropogenic Ecosystem Mosaics
* **Domaine d'Application** : Écologie Pyrogénique & Gestion de l'Habitat
* **Fenêtre Temporelle / Déclenchement** : $T \le -50,000$ BP (Pratiques Aborigènes Sahul & Afrique)
* **Zone Géographique / Biome** : Savanes, Prairies & Biomes Forestiers Feus-Sensibles
* **Culture / Mode Lithique** : Modes 3 à 5 (Moustérien, Microlithes)
* **Plugin Optionnel Associé** : `FireStickFarmingEngine`

- **Pyrogenic Ecological Succession**: Controlled burning by foraging bands prevents climax forest canopy growth, maintaining open grass-shrubland mosaics:

$$\frac{\partial B_{\text{veg}}}{\partial t} = r B_{\text{veg}} \left(1 - \frac{B_{\text{veg}}}{K}\right) - \lambda_{\text{fire}} \cdot B_{\text{veg}} \cdot H_{\text{anthropogenic}}$$

### 4.16 Canid Domestication & Mutualistic Trophic Symbiosis ($\sim -15,000$ BP)
* **Domaine d'Application** : Domestication, Écologie Trophique & Chasse Assistée
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Bonn-Oberkassel, Altaï)
* **Zone Géographique / Biome** : Eurasia Holarctique & Zones de Grande Chasse
* **Culture / Mode Lithique** : Mode 4 et 5 (Magdalénien, Epipaléolithique)
* **Plugin Optionnel Associé** : `CanidDomesticationEngine`

- **Trophic Energy Coupling**: Mutualistic symbiosis between foraging bands ($N_{\text{human}}$) and proto-dogs ($N_{\text{canid}}$):

$$\frac{dE_{\text{band}}}{dt} = \eta_{\text{hunt}} \cdot M_{\text{prey}} \cdot \left(1.0 + \beta \frac{N_{\text{canid}}}{N_{\text{human}}}\right) - \mathcal{C}_{\text{canid\_maintenance}}$$

### 4.17 Archaic Hominin Competitive Exclusion & Niche Overlap
* **Domaine d'Application** : Paléoanthropologie & Compétition Interspécifique
* **Fenêtre Temporelle / Déclenchement** : $-100,000 \le T \le -30,000$ BP (Coexistence Homininés)
* **Zone Géographique / Biome** : Eurasia (Neanderthal), Asie Orientale (Denisova), Afrique (Sapiens)
* **Culture / Mode Lithique** : Modes 2 et 3 (Acheuléen, Moustérien, Levallois)
* **Plugin Optionnel Associé** : `HomininCompetitiveExclusionEngine`

- **Lotka-Volterra Interspecies Competition**: Coupled differential dynamics between Sapiens ($\rho_1$), Neanderthals ($\rho_2$), and Denisovans ($\rho_3$):

$$\frac{\partial \rho_i}{\partial t} = D_i \nabla^2 \rho_i + r_i \rho_i \left(1 - \frac{\rho_i + \sum_{j \neq i} \alpha_{ij} \rho_j}{K_i(x,y,t)}\right)$$

### 4.19 Fat & Smoked Meat Curing Reserves (Pemmican/Smoking) ($\sim -25,000$ BP)
* **Domaine d'Application** : Conservation Alimentaire & Amortissement des Crises Hivernales
* **Fenêtre Temporelle / Déclenchement** : $T \le -25,000$ BP (Magdalénien & Solutréen)
* **Zone Géographique / Biome** : Zonation Sub-Glaciaire & Permafrost (Eurasie du Nord)
* **Culture / Mode Lithique** : Mode 4 (Industries Laminaires & Bone Needles)
* **Plugin Optionnel Associé** : `MeatCuringReservesEngine`

- **Fat & Meat Preservation**: Smoking, drying, and pemmican-style fat blending extend food shelf life by +6 months.
- **Winter Famine Mitigation**: Stored cured reserves smooth seasonal food shortages, preventing demographic crash ticks during extreme winter months.

### 4.20 Exogamous Kinship Networks & Incest Avoidance ($\sim -40,000$ BP)
* **Domaine d'Application** : Génétique des Populations & Anthropologie de la Parenté
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Sunghir, Paléolithique Supérieur)
* **Zone Géographique / Biome** : Global (Réseaux de Bandes Nomades)
* **Culture / Mode Lithique** : Mode 4 (Châtelperronien, Aurignacien)
* **Plugin Optionnel Associé** : `ExogamousKinshipEngine`

- **Exogamy Rules**: Mandatory inter-band mate exchange prevents inbreeding depression ($F_{\text{is}}$ coefficient control).
- **Inbreeding Penalty**: Small isolated bands ($N < 30$) lacking exogamous connections suffer a genetic fitness degradation factor:

$$W_{\text{genetic}} = W_0 \cdot \left(1.0 - \gamma \cdot F_{\text{is}}\right)$$

### 4.21 Archaic Hominin Introgressions & High-Altitude/Immunity Adaptations ($\sim -50,000$ BP)
* **Domaine d'Application** : Paléogénétique & Adaptations Évolutives
* **Fenêtre Temporelle / Déclenchement** : $-60,000 \le T \le -30,000$ BP (Rencontres Neanderthal/Denisova)
* **Zone Géographique / Biome** : Haute Altitude (Plateau Tibétain), Taïga Sub-Arctique
* **Culture / Mode Lithique** : Mode 3 et 4 (Moustérien / Levallois à Mode 4)
* **Plugin Optionnel Associé** : `ArchaicIntrogressionEngine`

- **Denisovan EPAS1 Introgression**: Hominin gene transfer affords high-altitude hypoxia resistance ($+30\%$ habitability score on Tibetan Plateau).
- **Neanderthal TLR Immunity**: Introgression of toll-like receptor genes boosts resistance to novel zoonotic pathogens in Eurasian cold forests.

### 4.22 Bone Needle Tailored Fur Clothing & Thermal Insulation ($\sim -35,000$ BP)
* **Domaine d'Application** : Technologie Textile/Pellissière & Thermorégulation
* **Fenêtre Temporelle / Déclenchement** : $T \le -35,000$ BP (Aiguilles à chas de Denisova & Sunghir)
* **Zone Géographique / Biome** : Steppe-Toundra & Périboréal
* **Culture / Mode Lithique** : Mode 4 (Aurignacien, Gravettien)
* **Plugin Optionnel Associé** : `TailoredClothingThermalEngine`

- **Tailored Multi-Layer Fur Garments**: Bone needles enable airtight fitted clothing.
- **Hypothermia Mitigation**: Reduces human basal metabolic expenditure penalty in extreme cold ($\le -20^\circ\text{C}$) by up to $65\%$.

### 4.23 Ochre Processing, Red Pigments & Tanning Preservation ($\sim -100,000$ BP)
* **Domaine d'Application** : Matériaux Rituels & Conservation des Peaux (Tannage)
* **Fenêtre Temporelle / Déclenchement** : $T \le -100,000$ BP (Grotte de Blombos, Qafzeh)
* **Zone Géographique / Biome** : Global (Gisements d'Hématite / Ocre)
* **Culture / Mode Lithique** : Modes 2 et 3 (Acheuléen Supérieur & Moustérien)
* **Plugin Optionnel Associé** : `OchreTanningTechnologyEngine`

- **Dual-Use Material**: Red ochre serves both symbolic body painting/rituals and hide preservation (bactericidal tanning agent).
- **Asabiyyah & Storage Boost**: Enhances ritual cohesion ($+10\%$) and slows organic hide/leather container decay.

### 4.24 Spearthrower (Atlatl) & Bow-and-Arrow Mechanical Advantage ($\sim -20,000$ BP)
* **Domaine d'Application** : Ballistique Préhistorique & Ballistique de Chasse
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Atlatl Magdalénien & Archery Solutréen/Epipaléolithique)
* **Zone Géographique / Biome** : Prairies Ouvertes & Écosystèmes Forestiers
* **Culture / Mode Lithique** : Mode 4 et 5 (Solutréen, Magdalénien, Epipaléolithique)
* **Plugin Optionnel Associé** : `AtlatlArcheryBallisticsEngine`

- **Mechanical Advantage**: Propulsive leverage increases projectile kinetic energy ($E_k = \frac{1}{2} m v^2$), doubling safe standoff hunting distance.
- **Large-Game Yield Boost**: Increases hunting success rates on agile/dangerous ungulates by $+40\%$.

### 4.25 Coastal Shellfish Gathering & Marine Foraging Refugia ($\sim -160,000$ BP)
* **Domaine d'Application** : Subsistance Littorale & Refuges Glaciaires
* **Fenêtre Temporelle / Déclenchement** : $T \le -160,000$ BP (Pinnacle Point, Klasies River Mouth)
* **Zone Géographique / Biome** : Rives Causal/Littorales & Intertidales
* **Culture / Mode Lithique** : Mode 3 (Middle Stone Age / Moustérien)
* **Plugin Optionnel Associé** : `CoastalMarineRefugiaEngine`

- **Omega-3 Brain Nutrition & Stable Biomass**: Shellfish beds provide climate-resilient, non-seasonal protein and fatty acids.
- **Glacial Refuge Sustenance**: Buffer foraging bands against inland drought or megafauna collapses during severe MIS glaciations.

### 4.26 Seasonal Aggregation Sanctuaries & Super-Band Assemblies ($\sim -30,000$ BP)
* **Domaine d'Application** : Macro-Sociologie Nomade & Agrégation Saisonnière
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Dolní Věstonice, Pavlov)
* **Zone Géographique / Biome** : Vallée Fluviales & Corridors de Transhumance
* **Culture / Mode Lithique** : Mode 4 (Gravettien, Pavlovien)
* **Plugin Optionnel Associé** : `SeasonalAggregationSanctuaryEngine`

- **Super-Band Assemblies**: Seasonal convergence of hundreds of individuals at designated river confluence sites.
- **Knowledge & Gene Exchange**: Accelerates cultural transmission, ritual trade, and mate selection across distant territories.

### 4.27 Trap & Snare Small-Game Harvesting (Lagomorphs / Waterfowl) ($\sim -15,000$ BP)
* **Domaine d'Application** : Diversification Broad-Spectrum & Capture Passive
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Broad-Spectrum Revolution Flannery)
* **Zone Géographique / Biome** : Zones Humides, Écosystèmes Rives & Taïga
* **Culture / Mode Lithique** : Mode 5 (Microlithes & Piégeage)
* **Plugin Optionnel Associé** : `PassiveSnareSmallGameEngine`

- **Passive Energy Extraction**: Fiber cordage traps and nets capture small game (hares, birds, fish) at minimal energy cost.
- **Carrying Capacity Expansion**: Increases baseline ecological carrying capacity $K$ by $+20\%$ in broad-spectrum ecosystems.

### 4.28 Ostrich Eggshell Water Containers & Arid Transhumance Stashing ($\sim -60,000$ BP)
* **Domaine d'Application** : Logistique Hydrique en Zone Aride & Stockage Clandestin
* **Fenêtre Temporelle / Déclenchement** : $T \le -60,000$ BP (Diepkloof Rock Shelter, Howiesons Poort)
* **Zone Géographique / Biome** : Déserts, Savanes Sèches (Kalahari, Atacama, Out-back Sahul)
* **Culture / Mode Lithique** : Mode 3 et 4 (Howiesons Poort)
* **Plugin Optionnel Associé** : `AridWaterStorageStashEngine`

- **Engraved Ostrich Eggshell Caches**: Sealed water containers buried along desert transit routes.
- **Arid Corridor Traversal**: Unlocks hyper-arid desert crossing pathways previously impassable due to dehydration limits.

### 4.29 Pitch & Resin Adhesive Tool Hafting (Birch Bark Pitch / Spinifex) ($\sim -200,000$ BP)
* **Domaine d'Application** : Pyrotechnologie & Emmanchement Mécanique
* **Fenêtre Temporelle / Déclenchement** : $T \le -200,000$ BP (Königsaue, Havering, Sahul Spinifex)
* **Zone Géographique / Biome** : Taïga, Forêts de Bouleaux & Xérophytes à Résine (Sahul)
* **Culture / Mode Lithique** : Modes 2 et 3 (Moustérien & MSA)
* **Plugin Optionnel Associé** : `ResinHaftingAdhesivesEngine`

- **Adhesive Pyrotechnology**: Anoxic distillation of birch bark pitch or spinifex resin processing creates durable composite hafting.
- **Armament Durability**: Multiplies mechanical shock resistance of speartips and darts by $\times 2.5$.

### 4.30 Bone, Antler & Ivory Tool Carving (Flutes, Harpoons, Points) ($\sim -40,000$ BP)
* **Domaine d'Application** : Économie de l'Os & Industrie Organique
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Jura Souabe, Geissenklösterle)
* **Zone Géographique / Biome** : Steppe-Toundra à Mégafaune (Mammouths/Rennes)
* **Culture / Mode Lithique** : Mode 4 (Aurignacien, Gravettien)
* **Plugin Optionnel Associé** : `OsseousIndustryCarvingEngine`

- **Osseous Tooling**: Carving barbed harpoons, spears, and needles from antler and mammoth ivory.
- **Lithic Quarry Substitution**: Extends tool manufacture capability into quarry-poor glaciated plains.

### 4.31 Salt Spring Gathering & Meat Curing Chemistry ($\sim -15,000$ BP)
* **Domaine d'Application** : Chimie Alimentaire & Conservation Halophile
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Sources Salines & Diapirs)
* **Zone Géographique / Biome** : Bassins Endoréiques & Diapirs Salins
* **Culture / Mode Lithique** : Modes 4 et 5 (Epipaléolithique)
* **Plugin Optionnel Associé** : `HaliteSaltCuringEngine`

- **Halite Curing**: Natural rock salt and brine spring harvesting for osmotic preservation of game meat.
- **Storage Longevity**: Reduces microbial meat spoilage rate by up to $70\%$ in temperate climates.

### 4.32 Fire-Heated Stone Boiling & Water Cooking Logistics ($\sim -25,000$ BP)
* **Domaine d'Application** : Thermodynamique Culinaire & Extraction de Moelle
* **Fenêtre Temporelle / Déclenchement** : $T \le -25,000$ BP (Foyers structurés Magdaléniens)
* **Zone Géographique / Biome** : Global (Sites d'Habitat Structurés)
* **Culture / Mode Lithique** : Mode 4 (Gravettien/Magdalénien)
* **Plugin Optionnel Associé** : `StoneBoilingThermalEngine`

- **Stone Boiling**: Transferring fire-heated cobbles into hide-lined pits or wooden vessels to boil water.
- **Bone Marrow Extraction**: Renders bone grease and collagen broth, increasing caloric extraction efficiency per carcass by $+15\%$.

### 4.33 Pitfall Trap Networks for Megafauna Harvesting ($\sim -15,000$ BP)
* **Domaine d'Application** : Ingénierie de Chasse Collective & Piégeage Lourd
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Tultepec Mammoths, Russie)
* **Zone Géographique / Biome** : Plaines Alluviales & Corridors de Transhumance
* **Culture / Mode Lithique** : Modes 4 et 5 (Epipaléolithique)
* **Plugin Optionnel Associé** : `MegafaunaPitfallTrapEngine`

- **Collective Pit Traps**: Excavation of deep earth pits to capture mammoths and bison without hunter injury.
- **Mass Calorie Spikes**: Yields multi-ton meat stores from a single hunting drive event.

### 4.34 Musical Instruments & Acoustic Resonance Rituals (Bone Flutes) ($\sim -40,000$ BP)
* **Domaine d'Application** : Musicologie Évolutive & Acoustique des Cavernes
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Hohle Fels, Divje Babe)
* **Zone Géographique / Biome** : Grottes à Résonance Karstique
* **Culture / Mode Lithique** : Mode 4 (Aurignacien)
* **Plugin Optionnel Associé** : `AcousticFluteResonanceEngine`

- **Osseous Flutes**: Pentatonic bone flutes crafted from vulture bones and ivory.
- **Acoustic Sanctuary Rituals**: Sound amplification in cavern chambers boosts Asabiyyah ritual cohesion during inter-band assemblies.

### 4.35 Ostrich Eggshell Bead Shell Ornamentation Networks ($\sim -40,000$ BP)
* **Domaine d'Application** : Réseaux d'Échange Symbolique & Parure Personnelle
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Enkapune Ya Muto, Afrique de l'Est)
* **Zone Géographique / Biome** : Savanes & Xérique
* **Culture / Mode Lithique** : Mode 3 et 4 (Later Stone Age)
* **Plugin Optionnel Associé** : `SymbolicBeadNetworkEngine`

- **Symbolic Gift Exchange (Hxaro)**: Standardized ostrich eggshell beads traded over hundreds of kilometers.
- **Inter-Band Reciprocity**: Establishes risk-pooling insurance networks between distant foraging bands during droughts.

### 4.36 Portable Figurines & Mobile Art (Venus Figurines) ($\sim -30,000$ BP)
* **Domaine d'Application** : Matérialité Symbolique & Culte de la Fertilité
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Willendorf, Dolní Věstonice, Mal'ta)
* **Zone Géographique / Biome** : Steppe-Toundra Eurasiatique
* **Culture / Mode Lithique** : Mode 4 (Gravettien, Solutréen)
* **Plugin Optionnel Associé** : `PortableArtFigurineEngine`

- **Mobile Venus Sculptures**: Carved ivory and terracotta female figurines carried across territories.
- **Social Alliances**: Serves as matrimonial alliance tokens, maintaining regional demographic viability.

### 4.37 Subterranean Underground Cave Torch/Lamp Lighting Logistics ($\sim -35,000$ BP)
* **Domaine d'Application** : Éclairage Pyrotechnique & Exploration Profonde
* **Fenêtre Temporelle / Déclenchement** : $T \le -35,000$ BP (Lampes en grès à graisse animale, Lascaux)
* **Zone Géographique / Biome** : Réseaux Endokarstiques Profonds
* **Culture / Mode Lithique** : Mode 4 (Aurignacien, Magdalénien)
* **Plugin Optionnel Associé** : `CaveLightingPyrotechnicsEngine`

- **Animal Fat Lamps**: Sandstone lamps burning animal tallow and resin torches.
- **Deep Sanctuary Access**: Unlocks subterranean karst exploration beyond 500 meters from cave entrances.

### 4.38 Bark Canoe & Raft River Crossing Logistics ($\sim -45,000$ BP)
* **Domaine d'Application** : Transport Fluvial & Franchissement d'Obstacles Hydriques
* **Fenêtre Temporelle / Déclenchement** : $T \le -45,000$ BP (Sahul & Vallées Amont)
* **Zone Géographique / Biome** : Grands Fleuves & Systèmes Lacustres
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `RiverCanoeTransportEngine`

- **Monoxyle & Bark Canoes**: Dugout canoes and bark rafts enabling major river crossings.
- **Freight Reduction**: Reduces water movement cost-distance by up to $80\%$.

### 4.39 Permafrost Storage Pits & Natural Ice Refrigerator Caches ($\sim -30,000$ BP)
* **Domaine d'Application** : Cryo-Conservation Alimentaire & Stockage Glaciaire
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Yana RHS, Sibérie)
* **Zone Géographique / Biome** : Permafrost Continu (Hautes Latitudes)
* **Culture / Mode Lithique** : Mode 4 (Paléolithique Supérieur Arctique)
* **Plugin Optionnel Associé** : `PermafrostColdCacheEngine`

- **Permafrost Caches**: Deep pits dug into frozen ground acting as natural multi-year meat freezers.
- **Arctic Settlement Viability**: Sustains permanent high-latitude Arctic settlements year-round.

### 4.40 Toxic Plant Detoxification & Leaching Pyrotechnics ($\sim -20,000$ BP)
* **Domaine d'Application** : Ethnobotanique & Traitement Chimique des Tubercules
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Niah Cave, Tropiques)
* **Zone Géographique / Biome** : Forêts Tropicales Humides & Savanes
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `PlantDetoxificationLeachingEngine`

- **Leaching & Soaking**: Running water leaching and roasting of toxic tubers (Dioscorea, cycads).
- **Equatorial Habitability**: Unlocks previously lethal tropical plant species, expanding rainforest carrying capacity.

### 4.41 Plant Fiber Cordage, Weaving & Netting Technologies ($\sim -30,000$ BP)
* **Domaine d'Application** : Technologie des Fibres Végétales & Textile Arcaïque
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Grotte de Dzudzuana, Géorgie)
* **Zone Géographique / Biome** : Zones Tempérées & Méditerranéennes (Tilleul, Ortie, Lin Sauvage)
* **Culture / Mode Lithique** : Mode 4 (Gravettien)
* **Plugin Optionnel Associé** : `PlantFiberCordageEngine`

- **Wild Flax & Nettle Cordage**: Twisted plant fiber ropes, carrying bags, and nets.
- **Nomadic Transport Capacity**: Increases mobile band carrying payload capacity by $+40\text{ kg}$.

### 4.42 Game Drive Funnel Walls & V-Shaped Topographic Traps ($\sim -12,000$ BP)
* **Domaine d'Application** : Tactique de Chasse Collective & Aménagement du Relief
* **Fenêtre Temporelle / Déclenchement** : $T \le -12,000$ BP (Desert Kites du Levant, Mésolithique)
* **Zone Géographique / Biome** : Canyons, Rebords d'Écrochement & Collines Escarpées
* **Culture / Mode Lithique** : Modes 4 et 5 (Epipaléolithique)
* **Plugin Optionnel Associé** : `TopographicGameDriveEngine`

- **Desert Kites & V-Funnek Walls**: Stone wall funnels directing migratory herds into slaughter enclosures.
- **Mass Herd Harvest**: Enables large pre-agricultural sedentary population gatherings.

### 4.43 Lunar Calendar Notations & Tally Bone Records ($\sim -30,000$ BP)
* **Domaine d'Application** : Évolutive Astronomie & Notation Temporelle
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Plaquette d'Abri Blanchard, Os d'Ishango)
* **Zone Géographique / Biome** : Global (Paléolithique Supérieur)
* **Culture / Mode Lithique** : Mode 4 (Aurignacien/Gravettien)
* **Plugin Optionnel Associé** : `LunarCalendarTallyEngine`

- **Lunar Phase Tallying**: Serially incised bone plates tracking lunar cycles.
- **Migration Prediction**: Anticipates seasonal fauna migrations with day-level precision.

### 4.44 Tanning Hide Shelters & Portable Mammoth-Bone Tents ($\sim -25,000$ BP)
* **Domaine d'Application** : Architecture Nomade & Structures d'Habitat
* **Fenêtre Temporelle / Déclenchement** : $T \le -25,000$ BP (Mezhyrich, Kostenki, Ukraine)
* **Zone Géographique / Biome** : Steppe-Toundra Sans Arbres (Pénurie de Bois)
* **Culture / Mode Lithique** : Mode 4 (Gravettien, Epigravettien)
* **Plugin Optionnel Associé** : `MammothBoneHabitationEngine`

- **Mammoth-Bone Dwellings**: Circular shelters constructed from mammoth skulls, tusks, and heavy hides.
- **Treeless Steppe Settlement**: Enables permanent shelter in timber-deficient periglacial zones.

### 4.45 Fire-Hardened Wooden Spear Mechanics (Schöningen Spears) ($\sim -300,000$ BP)
* **Domaine d'Application** : Ballistique Pré-Sapiens & Traitement Thermique du Bois
* **Fenêtre Temporelle / Déclenchement** : $T \le -300,000$ BP (Schöningen, Clacton-on-Sea)
* **Zone Géographique / Biome** : Forêts Tempérées (Epicéa, Frêne)
* **Culture / Mode Lithique** : Mode 2 (Acheuléen)
* **Plugin Optionnel Associé** : `FireHardenedSpearEngine`

- **Fire-Hardened Javelins**: Spruce spears with fire-cured tip points.
- **Acheulean Standoff Hunting**: Early hominin javelins effective against horses and deer.

### 4.46 Ochre-Based Insect Repellent & Parasite Control ($\sim -70,000$ BP)
* **Domaine d'Application** : Médecine Médicamenteuse & Hygiène d'Habitat
* **Fenêtre Temporelle / Déclenchement** : $T \le -70,000$ BP (Sibudu Cave, Afrique du Sud)
* **Zone Géographique / Biome** : Biomes Sub-Tropicaux & Humides
* **Culture / Mode Lithique** : Mode 3 (Middle Stone Age)
* **Plugin Optionnel Associé** : `ParasiteControlRepellentEngine`

- **Medicinal Aromatic Bedding**: Compressed sedge bedding mixed with aromatic leaves and ochre.
- **Parasite Mitigation**: Repels vector insects, reducing infant mortality in rock shelters.

### 4.47 Wild Cereal Harvesting & Mortar Grinding Tools ($\sim -23,000$ BP)
* **Domaine d'Application** : Protée-Agronomie & Préparation des Graines
* **Fenêtre Temporelle / Déclenchement** : $T \le -23,000$ BP (Ohalo II, Lac de Tibériade)
* **Zone Géographique / Biome** : Zones Méditerranéennes & Levantines
* **Culture / Mode Lithique** : Mode 4 et 5 (Epipaléolithique)
* **Plugin Optionnel Associé** : `WildCerealGrindingEngine`

- **Wild Grain Processing**: Milling wild barley and wheat seeds on stone querns and mortars.
- **Carbohydrate Extraction**: Renders complex carbohydrates digestible prior to farming.

### 4.48 Volcanic Ash Taphonomy & Tephra Disaster Refugia ($\sim -74,000$ BP)
* **Domaine d'Application** : Téphrochronologie & Résilience aux Catastrophes Volcaniques
* **Fenêtre Temporelle / Déclenchement** : $T \le -74,000$ BP (Super-Éruption du Toba, Sumatra)
* **Zone Géographique / Biome** : Zones d'Impact de Retombées d'Aérosols & Cendres
* **Culture / Mode Lithique** : Modes 2 et 3
* **Plugin Optionnel Associé** : `VolcanicTephraRefugiaEngine`

- **Toba Volcanic Bottleneck**: Global aerosol cooling reduces breeding population to $\sim 10,000$ individuals.
- **Coastal Refugia Survival**: Sustains human survival pockets along coastal South Africa.

### 4.49 Shell Midden Accumulation & Coastal Mesolithic Densities ($\sim -12,000$ BP)
* **Domaine d'Application** : Écologie Littorale & Stratigraphie Malacologique
* **Fenêtre Temporelle / Déclenchement** : $T \le -12,000$ BP (Kjökkenmödding d'Ertebølle, Bretagne, Portugal)
* **Zone Géographique / Biome** : Estuaires & Littoraux Tempérés
* **Culture / Mode Lithique** : Mode 5 (Epipaléolithique / Mésolithique)
* **Plugin Optionnel Associé** : `ShellMiddenAccumulationEngine`

- **Midden Stratigraphy**: Shellfish gathering (mussels, oysters, limpets) builds massive shell mounds ($V_{\text{midden}}$), stabilizing foraging band territories.
- **Coastal Settlement Density**: Unlocks high coastal density baselines:

$$\rho_{\text{coastal}} = \rho_0 \cdot \left(1.0 + \kappa_{\text{shell}} \cdot \ln(1.0 + V_{\text{midden}})\right)$$

### 4.50 Deep-Water Marine Halieutic Netting & Bone Hooks ($\sim -42,000$ BP)
* **Domaine d'Application** : Pêche Hauturière & Technologies de la Mer
* **Fenêtre Temporelle / Déclenchement** : $T \le -42,000$ BP (Jerimalai, Timor Oriental)
* **Zone Géographique / Biome** : Eaux Pélagiques Tropicales & Récifs de Wallacéo
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `PelagicFishingHookEngine`

- **Pelagic Fishing**: Curved shell/bone hooks and fiber lines enable deep-sea capture of fast pelagic fish (tuna, scombrids).
- **Island Population Carrying Capacity**: Increases island carrying capacity ($K$) by $+35\%$.

### 4.51 Fire-Hardened Digging Sticks & Geophyte Underground Storage Harvesting ($\sim -170,000$ BP)
* **Domaine d'Application** : Proto-Botanique & Extraction des Géophytes (USO)
* **Fenêtre Temporelle / Déclenchement** : $T \le -170,000$ BP (Border Cave, Afrique du Sud)
* **Zone Géographique / Biome** : Savanes, Maquis & Steppes à Tubercules
* **Culture / Mode Lithique** : Modes 2 et 3 (MSA)
* **Plugin Optionnel Associé** : `GeophyteDiggingStickEngine`

- **Underground Storage Organs (USO)**: Fire-hardened wooden digging sticks extract starchy rhizomes (*Hypoxis*), providing drought-resistant carbohydrates.
- **Drought Buffer**: Suppresses famine mortality during severe hyper-arid phases.

### 4.52 Birch-Tar Cold Extraction & Pyrolysis Kilns ($\sim -50,000$ BP)
* **Domaine d'Application** : Génie Chimique Préhistorique & Pyrolyse en Fosse
* **Fenêtre Temporelle / Déclenchement** : $T \le -50,000$ BP (Inden-Altdorf, Néandertal)
* **Zone Géographique / Biome** : Taïga & Forêts de Bouleaux
* **Culture / Mode Lithique** : Mode 3 (Moustérien)
* **Plugin Optionnel Associé** : `BirchTarPyrolysisKilnEngine`

- **Anaerobic Tar Extraction**: Subterranean ember pits extract pure birch pitch without oxygen ($T \sim 350^\circ\text{C}$).
- **Composite Weapon Durability**: Eliminates spearhead detachment during high-impact hunting.

### 4.53 Hide Boat Coracle & Kayak Sub-Arctic Navigation Logistics ($\sim -18,000$ BP)
* **Domaine d'Application** : Transport Maritime Périglaciaire & Chasse au Phoque
* **Fenêtre Temporelle / Déclenchement** : $T \le -18,000$ BP (Corridors Côtiers Pacifique / Atlantique)
* **Zone Géographique / Biome** : Marges Glaciaires & Fjords Arctiques
* **Culture / Mode Lithique** : Mode 4 (Paléolithique Supérieur Côtier)
* **Plugin Optionnel Associé** : `SkinKayakSubArcticEngine`

- **Sealskin Craft**: Waterproof hide-covered wood/bone frames navigate ice-strewn fjords.
- **Kelp Highway Bypass**: Enables rapid coastal dispersal bypassing continental ice barriers.

### 4.54 Antler & Ivory Barbed Harpoon Salmon Run Fishing ($\sim -16,000$ BP)
* **Domaine d'Application** : Pêche Fluviale Saisonnière & Exploitation du Saumon
* **Fenêtre Temporelle / Déclenchement** : $T \le -16,000$ BP (Magdalénien Supérieur)
* **Zone Géographique / Biome** : Fleuves Périglaciaires & Vallées d'Eurasie
* **Culture / Mode Lithique** : Mode 4 (Magdalénien)
* **Plugin Optionnel Associé** : `SalmonRunHarpoonEngine`

- **Seasonal Salmon Migrations**: Detachable barbed harpoons harvest spring salmon runs (*Salmo salar*).
- **Caloric Surge**: Generates massive seasonal food surpluses, feeding large Magdalenian aggregations.

### 4.55 Cave Bear (*Ursus spelaeus*) Hibernation Cave Niche Competition ($\sim -50,000$ BP)
* **Domaine d'Application** : Paléo-Écologie Karstique & Compétition Interspécifique
* **Fenêtre Temporelle / Déclenchement** : $T \le -50,000$ BP (Chauvet, Mixnitz, Jura Souabe)
* **Zone Géographique / Biome** : Cavernes Karstiques Alpines & Périglaciaires
* **Culture / Mode Lithique** : Modes 3 et 4
* **Plugin Optionnel Associé** : `CaveBearNicheCompetitionEngine`

- **Den Exclusion Competition**: Human bands hunt hibernating cave bears to claim insulated karst shelters.
- **Fur & Meat Bonus**: Provides thick winter furs and fat-rich meat reserves.

### 4.56 Ochre Mine Quarrying & Pigment Industrial Logistics ($\sim -40,000$ BP)
* **Domaine d'Application** : Minière Préhistorique & Géologie Extractive
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Lion Cave, Eswatini, Lovas)
* **Zone Géographique / Biome** : Formations de Fer Rubané & Diapirs d'Hématite
* **Culture / Mode Lithique** : Modes 3 et 4 (MSA / LSA / UP)
* **Plugin Optionnel Associé** : `OchreMiningQuarryEngine`

- **Industrial Ochre Extraction**: Underground quarrying of specular hematite for regional trade networks.
- **Symbolic Currency**: Red ochre acts as the first standardized symbolic trade medium.

### 4.57 Frozen Steppe Tundra Meat Drying & Wind Curing Caches ($\sim -28,000$ BP)
* **Domaine d'Application** : Cryo-Dessiccation Alimentaire & Survie Glaciaire
* **Fenêtre Temporelle / Déclenchement** : $T \le -28,000$ BP (Kostenki, Sunghir)
* **Zone Géographique / Biome** : Steppe-Toundra à Vent Desséchant
* **Culture / Mode Lithique** : Mode 4 (Gravettien)
* **Plugin Optionnel Associé** : `WindCuringSteppeCacheEngine`

- **Freeze-Drying (Cryo-Desiccation)**: Cold dry glacial winds preserve thin meat strips without salt.
- **Winter Blizzard Survival**: Prevents starvation during prolonged arctic blizzards.

### 4.58 Megafauna Hide Tanning Thongs & Cordage Lashings ($\sim -35,000$ BP)
* **Domaine d'Application** : Technologie du Cuir & Assemblages Mécaniques
* **Fenêtre Temporelle / Déclenchement** : $T \le -35,000$ BP (Sunghir, Dolní Věstonice)
* **Zone Géographique / Biome** : Steppes & Taïga
* **Culture / Mode Lithique** : Mode 4 (Aurignacien/Gravettien)
* **Plugin Optionnel Associé** : `LashingsHideThongEngine`

- **Spiral Leather Thongs**: Cutting mammoth hide in continuous spiral thongs yields high-tensile lashings.
- **Structural Rigidity**: Enables construction of large bone tents and heavy sleds.

### 4.59 Obsidian Hydration Blade Knapping & Microlith Bladelet Production ($\sim -20,000$ BP)
* **Domaine d'Application** : Métrologie Lithique & Économie de Matière Première
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Solutréen, Kebarien, Epipaléolithique)
* **Zone Géographique / Biome** : Périmètres à Silex / Obsidienne de Haute Qualité
* **Culture / Mode Lithique** : Modes 4 et 5 (Lamelles & Microlithes)
* **Plugin Optionnel Associé** : `MicrolithBladeletProductionEngine`

- **Bladelet Yield Efficiency**: Maximizes cutting edge length per kilogram of lithic raw material ($\text{cm/kg}$).
- **Interchangeable Barbs**: Allows instant field repair of damaged composite weapon heads.

### 4.60 High-Altitude Plateau Acclimatization & Hypoxia Genetic Resilience ($\sim -40,000$ BP)
* **Domaine d'Application** : Physiologie Évolutive & Adaptation Altitudinale
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Plateau Tibétain, Baishiya Cave)
* **Zone Géographique / Biome** : Haute Montagne & Altiplano ($> 3,500\,\text{m}$)
* **Culture / Mode Lithique** : Modes 3 et 4
* **Plugin Optionnel Associé** : `HighAltitudeHypoxiaEngine`

- **EPAS1 Denisovan Selection**: Physiological adaptation to low partial $\text{O}_2$ pressure ($p_{\text{O}_2}$).
- **Highland Settlement**: Unlocks permanent human habitation above $3,500\,\text{m}$.

### 4.61 Symbolic Burial Regalia & Mortuary Asabiyyah ($\sim -30,000$ BP)
* **Domaine d'Application** : Sociologie de la Mort & Hiérarchie Symbolique
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Sunghir, Arene Candide)
* **Zone Géographique / Biome** : Grottes & Abris Funéraires
* **Culture / Mode Lithique** : Mode 4 (Gravettien, Magdalénien)
* **Plugin Optionnel Associé** : `MortuaryBurialRegaliaEngine`

- **Mortuary Symbolism**: Complex burials with thousands of ivory beads and red ochre enhance lineage prestige.
- **Intergenerational Cohesion**: Permanently boosts inter-band social cohesion ($\text{Asabiyyah}$).

### 4.62 Clay Firing Pyrotechnics & Ceramic Proto-Figurines ($\sim -29,000$ BP)
* **Domaine d'Application** : Pyrotechnologie Céramique & Terres Cuites Pré-Agricoles
* **Fenêtre Temporelle / Déclenchement** : $T \le -29,000$ BP (Dolní Věstonice)
* **Zone Géographique / Biome** : Loess & Bassins Argileux
* **Culture / Mode Lithique** : Mode 4 (Gravettien)
* **Plugin Optionnel Associé** : `ProtoCeramicFiringEngine`

- **Kiln Pyrotechnics**: Controlled clay firing at $800^\circ\text{C}$ creating ceramic animal and Venus figurines.
- **Ritual Magic**: Thermal thermal-fracture rituals intended to promote hunting success.

### 4.63 Bird Bone Needle Eyed Sewing & Multilayered Arctic Fur Suits ($\sim -35,000$ BP)
* **Domaine d'Application** : Ergonomie Vestimentaire & Protection Polaire
* **Fenêtre Temporelle / Déclenchement** : $T \le -35,000$ BP (Denisova, Mal'ta, Ust'-Ishim)
* **Zone Géographique / Biome** : Arctique, Toundra Périglaciaire
* **Culture / Mode Lithique** : Mode 4 (Paléolithique Supérieur Arctique)
* **Plugin Optionnel Associé** : `EyedNeedleSewingEngine`

- **Airtight Sewing**: Fine eyed bone needles sew windproof multi-layered fur suits (parkas, trousers, boots).
- **Extreme Cold Survival**: Allows human survival under severe $-40^\circ\text{C}$ winter blizzards.

### 4.64 Coastal Kelp Highway Ecosystem Navigation & Resource Harvesting ($\sim -16,000$ BP)
* **Domaine d'Application** : Migration Maritime & Écologie des Forêts de Kelp
* **Fenêtre Temporelle / Déclenchement** : $T \le -16,000$ BP (Route Côtière du Pacifique)
* **Zone Géographique / Biome** : Forêts de Kelp Littorales (Pacifique Nord)
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `KelpHighwayNavigationEngine`

- **Kelp Highway Ecosystem**: Establishes rapid marine dispersal along kelp forests rich in sea otters, fish, and shellfish.
- **Ice Margin Bypass**: Bypasses the North American Laurentide ice sheet via coastal watercraft.

### 4.65 Cave Hyena (*Crocuta crocuta spelaea*) Scavenging & Den Competition ($\sim -45,000$ BP)
* **Domaine d'Application** : Écologie du Taniérage & Nécrophagie Compétitive
* **Fenêtre Temporelle / Déclenchement** : $T \le -45,000$ BP (Châtillon-sur-Indre)
* **Zone Géographique / Biome** : Steppe-Toundra & Karsts
* **Culture / Mode Lithique** : Modes 3 et 4
* **Plugin Optionnel Associé** : `CaveHyenaScavengingEngine`

- **Scavenging Competition**: Fierce contest between human bands and cave hyena packs for megafauna carcasses.
- **Fire Defense**: Requires fire maintenance to repel nocturnal hyena packs.

### 4.66 Trans-Continental Ochre Trade Routes & Red Ochre Inter-Band Pacts ($\sim -50,000$ BP)
* **Domaine d'Application** : Réseaux Économiques & Alliances Inter-Tribales
* **Fenêtre Temporelle / Déclenchement** : $T \le -50,000$ BP (Afrique du Sud, Australie Aborigène)
* **Zone Géographique / Biome** : Corridors d'Échange Trans-Continentaux
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `OchreTradeAllianceEngine`

- **Long-Distance Exchange**: Red ochre blocks exchanged across $>500\,\text{km}$ seal inter-band alliance pacts.
- **Famine Safety Net**: Grants emergency foraging access to neighboring tribal territories.

### 4.67 Flint Heat-Treatment Thermal Alteration (Solutrean Pressure Flaking) ($\sim -21,000$ BP)
* **Domaine d'Application** : Pyrotechnologie Lithique & Retouche par Pression
* **Fenêtre Temporelle / Déclenchement** : $T \le -21,000$ BP (Solutréen, Laugerie-Haute)
* **Zone Géographique / Biome** : Zones d'Habitat à Silex Siliceux
* **Culture / Mode Lithique** : Mode 4 (Solutréen)
* **Plugin Optionnel Associé** : `HeatTreatedFlintPressureEngine`

- **Controlled Thermal Alteration**: Heating flint nodules in sand beds ($250\text{--}300^\circ\text{C}$) improves flaking fracture.
- **Laurel-Leaf Points**: Enables delicate pressure flaking of ultra-thin Solutrean laurel-leaf projectile points.

### 4.68 Sub-Glacial Outwash River Drift & Gravel Bar Lithic Harvesting ($\sim -20,000$ BP)
* **Domaine d'Application** : Géomorphologie Fluvioglaciaire & Approvisionnement Lithique
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Marges des Calottes Fennoscandienne & Laurentide)
* **Zone Géographique / Biome** : Outwash Plains (Sandur) & Terrasses Alluviales
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `FluvioglacialLithicHarvestEngine`

- **Sandur Lithic Sources**: Collecting high-grade flint and quartzite cobbles from sub-glacial meltwater outwash streams.
- **Ice-Margin Tool Supply**: Sustains lithic tool production directly along glacial margins.

### 4.69 Jōmon Ceramic Deep-Boiling & Shellfish Processing ($\sim -16,500$ BP)
* **Domaine d'Application** : Pyrotechnologie Céramique & Bio-Disponibilité Coquillère
* **Fenêtre Temporelle / Déclenchement** : $T \le -16,500$ BP (Culture Jōmon Initiale, Odai Yamamoto)
* **Zone Géographique / Biome** : Archipelago Japonais & Littoraux du Pacifique Ouest
* **Culture / Mode Lithique** : Mode 5 / Céramique Incertée
* **Plugin Optionnel Associé** : `JomonCeramicBoilingEngine`

- **Pointed-Bottom Vessels**: Prolonged boiling of acorns, toxic tubers, and shellfish in ceramic jars.
- **Caloric Bio-Availability Surge**: Increases shellfish nutrient extraction efficiency by $+45\%$ and extends stew storage by $+3$ months.

### 4.70 Levallois Lithic Prepared Core Reduction & Flake Standardizing ($\sim -300,000$ BP)
* **Domaine d'Application** : Métrologie Lithique & Pré-détermination de Forme
* **Fenêtre Temporelle / Déclenchement** : $T \le -300,000$ BP (Paléolithique Moyen, Sapiens & Néandertal)
* **Zone Géographique / Biome** : Afrique, Eurasie
* **Culture / Mode Lithique** : Mode 3 (Levallois / Moustérien)
* **Plugin Optionnel Associé** : `LevalloisPreparedCoreEngine`

- **Prepared Core Flaking**: Shaping flint cores before strike produces predictable, razor-sharp flakes.
- **Knapping Efficiency**: Boosts tool manufacturing speed by $+60\%$ and cuts core raw material waste by $-35\%$.

### 4.71 Acheulean Handaxe Symmetry & Social Display ($\sim -500,000$ BP)
* **Domaine d'Application** : Cognition Évolutive & Signalétique Sociale
* **Fenêtre Temporelle / Déclenchement** : $T \le -500,000$ BP (Acheulean Moyen à Supérieur)
* **Zone Géographique / Biome** : Afrique, Europe de l'Ouest, Asie du Sud
* **Culture / Mode Lithique** : Mode 2 (Acheuléen)
* **Plugin Optionnel Associé** : `AcheuleanBifaceSymmetryEngine`

- **Geometric Symmetry Display**: Highly symmetrical handaxes serve as indicators of motor skill and genetic fitness.
- **Social Cohesion**: Increases band cohesion ($\text{Asabiyyah}$) by $+15\%$ and reduces intra-group mate competition.

### 4.72 Oldowan Chopper Bone Marrow Extraction ($\sim -2,600,000$ BP)
* **Domaine d'Application** : Taphonomie du Scavenging & Lipides Osseux
* **Fenêtre Temporelle / Déclenchement** : $T \le -2,600,000$ BP (Gona, Éthiopie, Olduvai)
* **Zone Géographique / Biome** : Vallée du Grand Rift Africain, Savanes
* **Culture / Mode Lithique** : Mode 1 (Oldowayen)
* **Plugin Optionnel Associé** : `OldowanMarrowPercussionEngine`

- **Long-Bone Percussion**: Shattering scavenged megafauna limb bones accesses fat-rich marrow unavailable to other carnivores.
- **Lipid Energy Pulse**: Delivers $+2.5\,\text{MJ/capita/day}$ during dry season game scarcity.

### 4.73 Trophic Megafauna Extinction Cascades & Predator Collapse ($\sim -13,000$ BP)
* **Domaine d'Application** : Écologie Trophique & Extinction en Cascade
* **Fenêtre Temporelle / Déclenchement** : $-14,000 \le T \le -10,000$ BP (Transition Pléistocène/Holocène)
* **Zone Géographique / Biome** : Holarctique, Amériques
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `TrophicCascadesPredatorEngine`

- **Secondary Extinction**: Disappearance of proboscideans triggers secondary extinction of Cave Hyenas and Sabre-toothed Cats (*Smilodon*).
- **Predation Safety**: Eliminates direct apex predator pressure on human band camps.

### 4.74 Beringian Standstill Isolation & Genetic Cold Adaptation ($\sim -22,000$ BP)
* **Domaine d'Application** : Paléogénétique & Génétique des Pop. Arctiques
* **Fenêtre Temporelle / Déclenchement** : $-25,000 \le T \le -15,000$ BP (Pont Terrestre de Béringie)
* **Zone Géographique / Biome** : Béringie Unglaciée (Steppe de laToundra)
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `BeringianStandstillIsolationEngine`

- **Founder Standstill**: 5,000-year isolation fixes founding Native American mitochondrial/Y-DNA lineages.
- **Cold Metabolic Efficiency**: Enhances brown-adipose metabolic heat generation under polar cold stress.

### 4.75 Sahul Arid Oasis Well Digging & Inland Soaks ($\sim -45,000$ BP)
* **Domaine d'Application** : Paléohydrologie & Dispersion en Zone Aride
* **Fenêtre Temporelle / Déclenchement** : $T \le -45,000$ BP (Australie Centrale, Bassin du Lac Eyre)
* **Zone Géographique / Biome** : Déserts Arides & Semi-Arides du Sahul
* **Culture / Mode Lithique** : Modes 3 à 5
* **Plugin Optionnel Associé** : `AridOasisWellDiggingEngine`

- **Subsurface Well Digging**: Excavating deep holes in dry sandy wadi beds (*soaks*) taps groundwater.
- **Trans-Continental Corridors**: Sustains inland migration routes across the arid interior of Australia.

### 4.76 Hand Stencil Parietal Markings & Territorial Boundaries ($\sim -40,000$ BP)
* **Domaine d'Application** : Sémiotique Spatiale & Droit Territorial
* **Fenêtre Temporelle / Déclenchement** : $T \le -40,000$ BP (Sulawesi, El Castillo, Chauvet)
* **Zone Géographique / Biome** : Karsts & Cavernes Ornées
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `HandStencilTerritoryEngine`

- **Pigment Blow Stenciling**: Negative hand prints on cave walls designate tribal territory boundaries.
- **Conflict Reduction**: Reduces inter-band warfare and territorial trespass mortality by $-25\%$.

### 4.77 Spear-Thrower Atlatl Weight Tuning & Ballistic Range ($\sim -18,000$ BP)
* **Domaine d'Application** : Physico-Ballistique & Ingénierie des Projectiles
* **Fenêtre Temporelle / Déclenchement** : $T \le -18,000$ BP (Paléolithique Supérieur Récent)
* **Zone Géographique / Biome** : Eurasie, Amériques
* **Culture / Mode Lithique** : Mode 4 (Solutréen, Magdalénien)
* **Plugin Optionnel Associé** : `AtlatlBalancingStoneEngine`

- **Resonance Tuning**: Attaching ground-stone weights onto spear-thrower shafts optimizes flex arm momentum.
- **Dart Launch Velocity**: Increases dart launch speed to $40\,\text{m/s}$, extending lethal range on bison to $45\,\text{m}$.

### 4.78 Bone & Antler Pressure Flaking Serration ($\sim -25,000$ BP)
* **Domaine d'Application** : Technologie Lithique & Mécanique de Perforation
* **Fenêtre Temporelle / Déclenchement** : $T \le -25,000$ BP (Gravettien, Solutréen)
* **Zone Géographique / Biome** : Eurasia Holarctique
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `PressureFlakerPointEngine`

- **Serrated Pressure Edges**: Soft-hammer antler retouchers create finely serrated projectile point margins.
- **Penetration Depth**: Increases projectile penetration depth into thick mammoth hide by $+40\%$.

### 4.79 Wild Flax Fiber Netting & High-Tensile Cordage ($\sim -32,000$ BP)
* **Domaine d'Application** : Technologie des Fibres & Piégeage au Filet
* **Fenêtre Temporelle / Déclenchement** : $T \le -32,000$ BP (Grotte de Dzudzuana, Géorgie)
* **Zone Géographique / Biome** : Forêts Tempérées & Caucase
* **Culture / Mode Lithique** : Mode 4 (Upper Paleolithic)
* **Plugin Optionnel Associé** : `WildFlaxSpinningEngine`

- **Wild Flax Netting**: Twisted wild flax threads make high-tensile small-game capture nets and bowstrings.
- **Broad-Spectrum Yield**: Increases small-mammal and bird harvesting rates by $+30\%$.

### 4.80 Chemical Ochre Loading of Plant Pitch Adhesives ($\sim -70,000$ BP)
* **Domaine d'Application** : Pyrotechnologie des Matériaux & Mastics Chimiques
* **Fenêtre Temporelle / Déclenchement** : $T \le -70,000$ BP (Sibudu Cave, Howiesons Poort)
* **Zone Géographique / Biome** : Savanes & Forêts d'Afrique du Sud
* **Culture / Mode Lithique** : Mode 3 (MSA)
* **Plugin Optionnel Associé** : `OchreResinHaftingEngine`

- **Mineral Mineral Fillers**: Adding hematite/ochre powder into resin increases adhesive shear strength by $+150\%$.
- **Cold Temperature Shock**: Prevents glue shatter under sub-zero impact forces.

### 4.81 Sub-Arctic Reindeer River Crossing Interception ($\sim -15,000$ BP)
* **Domaine d'Application** : Stratégie de Chasse & Ethno-Zoologie
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Magdalénien, Bassin Parisien, Dordogne)
* **Zone Géographique / Biome** : Toundra & Fleuves Périglaciaires
* **Culture / Mode Lithique** : Mode 4 (Magdalénien)
* **Plugin Optionnel Associé** : `ReindeerRiverInterceptionEngine`

- **River Crossing Ambush**: Intercepting migrating reindeer herds (*Rangifer tarandus*) at river narrows.
- **Caloric Storage Pulse**: Sustains high winter band densities ($0.15\,\text{hab/km}^2$) through smoked meat storage.

### 4.82 Endokarst Torch Charcoal Marking & Cave Navigation ($\sim -30,000$ BP)
* **Domaine d'Application** : Cartographie Souterraine & Spéléologie
* **Fenêtre Temporelle / Déclenchement** : $T \le -30,000$ BP (Chauvet, Cosquer, Font-de-Gaume)
* **Zone Géographique / Biome** : Réseaux Karstiques Profonds
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `EndokarstTorchMappingEngine`

- **Charcoal Wayfinding**: Torch snuff marks and charcoal lines map subterranean paths $>1\,\text{km}$ inside deep caves.
- **Zero Cave Accidents**: Eliminates mortality from falling into deep karst shafts.

### 4.83 Periglacial Loess Dust Storms & Respiratory Stress ($\sim -24,000$ BP)
* **Domaine d'Application** : Paléo-Écologie Respiratoire & Éolien Périglaciaire
* **Fenêtre Temporelle / Déclenchement** : Peak LGM ($-26,500$ to $-19,000$ BP)
* **Zone Géographique / Biome** : Ceinture de Loess Eurasiatique & Nord-Américaine
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `PeriglacialLoessDustEngine`

- **Glacial Dust Plumes**: Intense LGM loess dust storms cause severe infant respiratory stress ($-10\%$ survival).
- **Shelter Mitigation**: Enforces structural requirement for airtight leather/bone tents.

### 4.84 Ostrich Eggshell Water Canteen Desert Corridors ($\sim -60,000$ BP)
* **Domaine d'Application** : Logistique Hydrique & Corridors Arides
* **Fenêtre Temporelle / Déclenchement** : $T \le -60,000$ BP (Diepkloof, Kalahari)
* **Zone Géographique / Biome** : Déserts d'Afrique Australe & Kalahari
* **Culture / Mode Lithique** : Mode 3 (Howiesons Poort)
* **Plugin Optionnel Associé** : `OstrichEggshellNetworkEngine`

- **Canteen Storage Cache**: Stashing engraved ostrich eggshell water flasks along dry routes.
- **Desert Foraging Radius**: Expands foraging radius into hyper-arid zones by $+200\,\text{km}$.

### 4.85 Bone Marrow Fat Rendering & Stone Boiling Pemmican ($\sim -20,000$ BP)
* **Domaine d'Application** : Chimie Alimentaire & Prévention de la Famine
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Paléolithique Supérieur Récent)
* **Zone Géographique / Biome** : Steppe-Toundra Glaciaire
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `MarrowFatRenderingEngine`

- **Bone Grease Rendering**: Boiling pulverized bone epiphyses with hot stones extracts high-energy tallow.
- **Protein Starvation Prevention**: Prevents rabbit starvation (protein poisoning) during lean winter months.

### 4.86 Sea-Otter Pelt Tanning & Sub-Arctic Kayak Insulation ($\sim -15,000$ BP)
* **Domaine d'Application** : Ergonomie Vestimentaire Aquatique & Tanning
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Kelp Highway, Pacifique Nord)
* **Zone Géographique / Biome** : Marges Côtières de la Mer de Béring & Pacifique
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `SeaOtterFurTanningEngine`

- **Ultra-Dense Fur Tanning**: Processing sea otter skins ($100,000\,\text{hairs/cm}^2$) creates waterproof thermal suits.
- **Aquatic Hypothermia Prevention**: Eliminates freezing mortality during open-water sub-arctic kayaking.

### 4.87 Cave Bat Zoonotic Spillover & Immune TLR Allele Selection ($\sim -50,000$ BP)
* **Domaine d'Application** : Épidémiologie Évolutive & Immunités Archaïques
* **Fenêtre Temporelle / Déclenchement** : $-100,000 \le T \le -30,000$ BP
* **Zone Géographique / Biome** : Cavernes d'Eurasie et d'Afrique
* **Culture / Mode Lithique** : Modes 3 et 4
* **Plugin Optionnel Associé** : `ZoonoticPathogenSpilloverEngine`

- **Zoonotic Spillover**: Heavy cave roosting exposes hominin bands to viral spillover events from bat guano.
- **TLR Allele Selection**: Drives positive natural selection for archaic Neanderthal *TLR1/6/10* immune cluster.

### 4.88 Doggerland Estuary Marsh Fowling Nets ($\sim -11,000$ BP)
* **Domaine d'Application** : Écologie Estuarienne & Capture Aviaire
* **Fenêtre Temporelle / Déclenchement** : $T \le -11,000$ BP (Doggerland, Plaine de la Mer du Nord)
* **Zone Géographique / Biome** : Bassins Marécageux du Doggerland
* **Culture / Mode Lithique** : Mode 5 (Mésolithique)
* **Plugin Optionnel Associé** : `DoggerlandMarshFowlingEngine`

- **Estuary Netting**: Netting migratory waterfowl across dry North Sea marshlands.
- **Pre-Submersion Density**: Maintains high Mesolithic band densities prior to early Holocene sea level rise.

### 4.89 Resinous Pine Torch Night Spearfishing ($\sim -14,000$ BP)
* **Domaine d'Application** : Pêche Fluviale Nocturne & Pyrotechnologie
* **Fenêtre Temporelle / Déclenchement** : $T \le -14,000$ BP (Epipaléolithique Eurasiatique)
* **Zone Géographique / Biome** : Fleuves & Lacs Tempérés
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `NightTorchSpearfishingEngine`

- **Night Torch Luring**: Burning pine resin torches over water pools attracts phototactic fish at night.
- **Nocturnal Harvest**: Doubling nocturnal riverine fishing yields during seasonal game droughts.

### 4.90 Porous Basalt Grinding Slabs & Acorn Tannin Leaching ($\sim -18,000$ BP)
* **Domaine d'Application** : Proto-Agroalimentaire & Lessivage des Tanins
* **Fenêtre Temporelle / Déclenchement** : $T \le -18,000$ BP (Epipaléolithique Levant / Kebarien)
* **Zone Géographique / Biome** : Forêts de Chênes Méditerranéennes & Levant
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `BasaltGrindingSlabEngine`

- **Acorn Processing**: Grinding acorns (*Quercus*) on porous basalt querns followed by water leaching removes bitter toxins.
- **Staple Carbohydrate**: Unlocks acorns as a major storable carbohydrate staple.

### 4.91 Steppe Bison Cliff Jump Drives ($\sim -12,000$ BP)
* **Domaine d'Application** : Chasse Collective en Masse & Stratégie Topographique
* **Fenêtre Temporelle / Déclenchement** : $T \le -12,000$ BP (Grandes Plaines Américaines / Paleoindian)
* **Zone Géographique / Biome** : Steppes & Canyons d'Amérique du Nord
* **Culture / Mode Lithique** : Mode 4 (Clovis / Folsom)
* **Plugin Optionnel Associé** : `BisonCliffJumpDriveEngine`

- **Mass Cliff Drives**: Stampeding bison herds over natural canyon drops captures hundreds of animals simultaneously.
- **Massive Meat Storage**: Yields multi-ton meat stores securing 1 full year of band autonomy.

### 4.92 Polished Obsidian Mirrors & Solar Friction Fire Starting ($\sim -14,000$ BP)
* **Domaine d'Application** : Optique Archéologique & Pyrotechnologie Solaire
* **Fenêtre Temporelle / Déclenchement** : $T \le -14,000$ BP (Anatolie, Grotte d'Öküzini)
* **Zone Géographique / Biome** : Zones Volcaniques à Obsidienne
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `ObsidianSolarIgnitionEngine`

- **Solar Concave Mirrors**: Polishing concave obsidian mirrors concentrates sunlight onto dry tinder.
- **Fire Ignition Efficiency**: Reduces fire ignition effort by $-50\%$ during sunny seasons.

### 4.93 Sub-Glacial Meltwater Sandur Wooden Fish Weirs ($\sim -12,000$ BP)
* **Domaine d'Application** : Génie Hydraulique Préhistorique & Pièges Fluviaux
* **Fenêtre Temporelle / Déclenchement** : $T \le -12,000$ BP (Mésolithique Périglaciaire)
* **Zone Géographique / Biome** : Torrents de Fonte Glacier & Sandur
* **Culture / Mode Lithique** : Mode 5
* **Plugin Optionnel Associé** : `SubGlacialMeltwaterWeirEngine`

- **Stake Fish Weirs**: Driving wooden stakes into outwash river channels traps migrating salmon and eels.
- **Glacial Margin Habitation**: Sustains human settlement directly along melting ice sheet margins.

### 4.94 Hot-Water Steam Bending of Mammoth Tusk Ivory ($\sim -27,000$ BP)
* **Domaine d'Application** : Matériaux Osseux & Ingénierie des Armes
* **Fenêtre Temporelle / Déclenchement** : $T \le -27,000$ BP (Sunghir, Kostenki)
* **Zone Géographique / Biome** : Steppe-Toundra à Mégafaune
* **Culture / Mode Lithique** : Mode 4 (Gravettien)
* **Plugin Optionnel Associé** : `IvoryHotWaterStraighteningEngine`

- **Ivory Steam Straightening**: Soaking curved mammoth tusks in hot water allows straightening into long $2\,\text{m}$ spears.
- **Heavy Shock Thrusting**: Yields rigid shock spears with maximum thrusting leverage against mammoth hide.

### 4.95 Cave Entrance Clay Plastering & Winter Thermal Sealing ($\sim -35,000$ BP)
* **Domaine d'Application** : Architecture Souterraine & Génie Thermique
* **Fenêtre Temporelle / Déclenchement** : $T \le -35,000$ BP (Aurignacien, Périgord)
* **Zone Géographique / Biome** : Karsts & Cavernes Karstiques
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `CaveWallClaySealingEngine`

- **Draft Mud Sealing**: Plastering entrance fissures with mud and clay seals cold winter drafts.
- **Internal Cave Warmth**: Maintains internal cave temperatures above $+10^\circ\text{C}$ during sub-zero winters.

### 4.96 Folded Birch-Bark Vessels & Hot-Stone Boiling ($\sim -20,000$ BP)
* **Domaine d'Application** : Logistique Domestique & Cuisson Sans Céramique
* **Fenêtre Temporelle / Déclenchement** : $T \le -20,000$ BP (Taïga & Sub-Arctique)
* **Zone Géographique / Biome** : Forêts de Bouleaux Eurasiatiques
* **Culture / Mode Lithique** : Modes 4 et 5
* **Plugin Optionnel Associé** : `BirchBarkVesselEngine`

- **Sealed Bark Buckets**: Folding birch-bark sheets sealed with pitch creates lightweight waterproof pots.
- **Lightweight Boiling**: Allows hot-stone water boiling without heavy ceramic pots during nomadic moves.

### 4.97 Snow-Trough Refrigeration Caches & Late Spring Preservation ($\sim -25,000$ BP)
* **Domaine d'Application** : Cryo-Conservation & Logistique de Chasse
* **Fenêtre Temporelle / Déclenchement** : $T \le -25,000$ BP (Yana RHS, Sibérie)
* **Zone Géographique / Biome** : Arctique & Haute Latitude
* **Culture / Mode Lithique** : Mode 4
* **Plugin Optionnel Associé** : `SnowTroughRefrigerationEngine`

- **Snow-Trough Storage**: Packing butchered megafauna meat inside snow trenches insulating with pine boughs.
- **Spring Freeze Preservation**: Keeps meat fresh until May thaw, preventing late-winter starvation.

### 4.98 Plant Aconite Toxin Extraction & Poisoned Microlith Arrows ($\sim -15,000$ BP)
* **Domaine d'Application** : Proto-Pharmacologie & Armes Chimiques
* **Fenêtre Temporelle / Déclenchement** : $T \le -15,000$ BP (Epipaléolithique Eurasiatique / San)
* **Zone Géographique / Biome** : Zones Montagneuses & Forêts
* **Culture / Mode Lithique** : Mode 5 (Microlithes)
* **Plugin Optionnel Associé** : `ArrowPoisonSynthesisEngine`

- **Aconite Toxin Coating**: Coating microlithic arrowheads with *Aconitum* root extracts paralyzes wounded game.
- **Hunting Yield Boost**: Increases hunting success on fast ungulates by $+60\%$.

### 4.99 Trans-Saharan Paleoriver Wadis & Lake Mega-Chad Fishing ($\sim -15,000$ BP)
* **Domaine d'Application** : Paléohydrologie & Périodes Humides Africaines
* **Fenêtre Temporelle / Déclenchement** : $15,000 \le T \le 5,000$ BP (African Humid Period)
* **Zone Géographique / Biome** : Sahara & Bassin du Tchad ($350,000\,\text{km}^2$)
* **Culture / Mode Lithique** : Mode 5 (LSA)
* **Plugin Optionnel Associé** : `LakeChadWadiMigrationEngine`

- **Wadi Highways**: Activation of dry riverbeds (Irharhar system) enables trans-Saharan human dispersal.
- **Mega-Lake Fishery**: Harpoon capture of giant catfish and perch in Lake Mega-Chad.

### 4.100 Epipalaeolithic Sedentary Round Hamlets & Grain Storage Pits ($\sim -12,500$ BP)
* **Domaine d'Application** : Sédentarisation Pré-Agricole & Proto-Urbanisme
* **Fenêtre Temporelle / Déclenchement** : $-12,500 \le T \le -10,200$ BP (Culture Natufienne, Mallaha)
* **Zone Géographique / Biome** : Levant (Proche-Orient)
* **Culture / Mode Lithique** : Mode 5 (Natufien)
* **Plugin Optionnel Associé** : `EpipaleolithicStorageHamletEngine`

- **Stone Round Huts**: Construction of permanent semi-subterranean drystone round huts with plaster floors.
- **Subterranean Grain Pits**: Sealed grain storage pits protect harvested wild cereals, completing the structural transition from nomadic band foraging to permanent village settlement.

---

### 4.18 Universal Planetary Insolation & Plugin Architecture Isolation

#### A. Planetary Insolation Physics Generalization
- **Earth Preset (`isEarthPreset = true`)**: Driven by empirical Milankovitch orbital eccentricity ($e$), obliquity ($\varepsilon$), precession ($\varpi$), and EPICA ice core $\text{CO}_2$ proxy time-series.
- **Procedural / Exoplanet Mode (`isEarthPreset = false`)**: Solves general celestial mechanics radiative equilibrium:

$$F_{\text{solar}}(\phi, t) = \frac{L_{\odot}}{4\pi a(t)^2} \cdot \max\left(0, \sin\phi \sin\delta(t) + \cos\phi \cos\delta(t) \cos h\right)$$

where instantaneous orbital distance $r(t) = \frac{a(1-e^2)}{1+e \cos \theta(t)}$ and solar declination $\delta(t) = \arcsin(\sin\varepsilon \sin(\theta(t) + \varpi))$.

#### B. Architectural Separation: Core Thermodynamic Engine vs Optional Domain Plugins
- **Core Engine Physics (`H3SimulationEngine` / `ProceduralPopulationEngine`)**: Enforces strict physical conservation laws ($\Delta E = 0$, mass conservation, heat transport differential equations, zero hardcoded arbitrary constants).
- **Optional Domain Modules (`ISimulationPlugin`)**: Anthropogenic, socio-cultural, and domain-specific mechanisms (e.g. `PaleoAnthropologyPlugin`, `LithicTradePlugin`, `CanidSymbiosisPlugin`) are isolated into modular plugins. Users can enable/disable these modules per scenario without modifying the core physical solver.

---


## 5. Cartographic Generation & Persistent Disk Caching

### 5.1 Dual-Mode Cartographic Architecture

1. **Empirical Ingestion Mode ($T_0 \ge -10,000$ BCE)**:
   - Streams 5-arc-minute Esri ASCII grids from `data/maps/hyde34/` using `Hyde34GridReader` and `DataDownloaderService`.
2. **On-Demand 3-Layer Hybrid Mode ($T_0 < -10,000$ BP)**:
   - **Layer 1 - Fossil & Archaeological Cluster Ingestion (`HistoricalMapGenerator`)**: Coordinates and spatial sigmas ($\sigma$) for known palaeoanthropological sites (Omo Kibish, Jebel Irhoud, Blombos, Atapuerca, Denisova, Madjedbebe, Yana RHS, Clovis, Monte Verde).
   - **Layer 2 - Paleoclimate NPP & Habitability (`ProceduralPopulationEngine`)**: CHELSA-Trace21k / PaleoCLIM temperature, precipitation, and biome suitability filtering.
   - **Layer 3 - Biogeographical Containment Guards**: Strict zero-population rules (`geoWeight = 0.0`) for uncolonized landmasses.

### 5.2 Disk Cache Plumbing
Once calculated for an epoch or custom year $T_0$, density maps and tensor layers are serialized to disk under `data/maps/cache/` (Base64 PNG + binary rasters). Subsequent requests for the same year reload directly from cache without recomputation.

---

## 6. Repository Integration & Knowledge Item Binding

To ensure project-wide persistence across development sessions:
1. **Repository Document**: Saved directly at `docs/PALEOCLIMATE_AND_PREHISTORY.md` within the `Ether` codebase.
2. **Local Knowledge Item**: Bound in `<appDataDir>/knowledge/deep_prehistoric_population_tensor_infrastructure/metadata.json` to ensure automated AI assistant contextual loading.



