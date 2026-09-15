# Ether Simulation — Differential Equations & State Variable Specification

> **Master Technical, Physical & Mathematical Specification**  
> *Version 4.4.0 — Strict Separation between Core Model Physics (Tier 1) and Optional Cliodynamic / Phenomenological Modules (Tier 2)*

---

## 1. Architectural Epistemology & Two-Tier Separation

The **Ether Engine** strictly partitions its computational mathematical models into two ontological tiers:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│             TIER 1 : CORE MODEL PHYSICS (Le Socle Dur — Invariant & Consensuel)        │
│    • Lois universelles de conservation (Masse, Énergie, Impulsion, Entropie)           │
│    • Mécanique céleste & Forçages orbitaux (Cycles de Milankovitch 100k/41k/23k)       │
│    • Thermodynamique, Rayonnement & Glaces (Stefan-Boltzmann, Clausius, Fonte PDD)     │
│    • Dynamique des fluides géophysiques (Coriolis, Cellules de Hadley/Ferrel/Polaires) │
│    • Hydrogéologie poreuse (Darcy 2D), Chimie des solutions (Loi de Henry)             │
│    • Géophysique solide & Isostasie (Ajustement Isostatique Glaciaire GIA)             │
│    • Pédologie hydraulique (van Genuchten AWC) & Sédimentation alluviale (Stokes)     │
│    • Transfert radiatif végétal (Beer-Lambert LAI) & Photosynthèse (Farquhar FvCB)     │
│    • Biophysique métabolique (Kleiber 3/4, Gompertz-Makeham, Stull Wet-Bulb)          │
│    • Génétique & Radiochronologie (Diffusion de Kimura SDE, Décroissance C-14, δ13C)   │
│    • Travail mécanique de frottement (Coulomb/Navier) & Enthalpie de réaction (Smelt) │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Forçages et rétroactions biophysiques
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│         TIER 2 : PLUGINS OPTIONNELS & CLIODYNAMIQUE (Phénoménologique & Débattu)       │
│    • Économie biophysique exergétique (Kümmel / Ayres-Warr)                           │
│    • Lois d'échelle d'allométrie urbaine (West-Bettencourt N^1.15)                    │
│    • Évolution culturelle & Équation de Price (Sélection multi-niveaux d'altruisme)   │
│    • Intensification agricole démographique (Modèle de Boserup)                       │
│    • Évolution technologique combinatoire (Modèle de W. Brian Arthur)                 │
│    • Rente d'épuisement des ressources non-renouvelables (Règle de Hotelling)         │
│    • Dynamique institutionnelle & effondrement de complexité (Tainter)                │
│    • Modèles socio-écologiques prédateur-proie (NASA HANDY)                           │
│    • Cohésion sociale & frontière impériale (Turchin Asabiyyah)                       │
│    • Épidémiologie compartimentale métapopulationnelle (SEIR-V Spatial)               │
│    • Nouvelle économie géographique centre-périphérie (Krugman NEG)                  │
│    • Ségrégation culturelle et homophilie spatiale (Schelling-Axelrod)                │
│    • Dérive mémétique stochastique (SDE Langevin 4D sur tenseurs culturels)           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Multi-Scale Temporal Decoupling

Ether decouples numerical integration across two discrete time scales on the Uber H3 hexagonal grid ($175,000+$ cells at Resolution 6–8):

- **Fast Tick Scale ($\Delta t_{\text{fast}} = 1 \text{ day} = 86,400 \text{ s}$)**:
  - Resolves symmetric finite-volume mass fluxes under Courant-Friedrichs-Lewy (CFL) limits (`FluxEngine`).
  - Evaluates instantaneous wet-bulb temperature and hyperthermia thresholds (`WetBulbTemperatureEngine`).
  - Computes zonal wind vectors under Coriolis parameter $f = 2\Omega \sin\phi$ (`AtmosphericCirculationHadleyEngine`).
  - Resolves active localized epidemiological transmissions via sparse active sets (`SpatialMetapopulationSEIREngine`).

- **Slow Tick Scale ($\Delta t_{\text{slow}} = 30 \text{ days} \approx 2.592 \times 10^6 \text{ s} = \Delta t / 31,557,600 \text{ yr}$)**:
  - Updates Milankovitch astronomical orbital parameters ($e$, $\varepsilon$, $\varpi$) and TOA solar insolation.
  - Integrates Viscoelastic Glacial Isostatic Adjustment (GIA, $\tau \approx 4000\text{ yr}$).
  - Solves 2D lateral piezometric aquifer diffusion (Darcy flux).
  - Integrates cryospheric thermodynamic melt ($L_f = 333.55\text{ kJ/kg}$) and eustatic sea level adjustments.
  - Resolves van Genuchten soil water retention, Radiocarbon $^{14}\text{C}$ decay, and Stokes siltation kinetics.
  - Integrates Farquhar FvCB photosynthesis, Beer-Lambert light interception, and soil organic carbon decay.
  - Updates cohort aging, Gompertz-Makeham senescence, and Kimura genetic drift.

---

## 3. Tier 1 — Core Model Physics (Socle Fondamental)

### A. Mécanique Céleste, Rayonnement & Thermodynamique Globale

#### 1. Forçage Orbital Astronomique de Milankovitch
Cycles orbitaux keplériens régissant l'insolation TOA $S(\phi, \delta)$ :
- **Excentricité** $e(t) \in [0.005, 0.058]$ (Période $100\text{ ka}$)
- **Obliquité** $\varepsilon(t) \in [22.1^\circ, 24.5^\circ]$ (Période $41\text{ ka}$)
- **Précession climatique** $\varpi(t)$ (Période $23\text{ ka}$)
$$S(\phi, \delta) = \frac{S_0}{\pi} \left(\frac{1 + e\cos\nu}{1 - e^2}\right)^2 \cdot \left[ H_0 \sin\phi \sin\delta + \cos\phi \cos\delta \sin H_0 \right]$$

#### 2. Bilan Radiatif Non-Linéaire de Stefan-Boltzmann
$$\epsilon \sigma T_{\text{surface}}^4 = \frac{S_0}{4} (1 - \alpha_{\text{albédo}}) + \Delta F_{\text{effet\_de\_serre}}$$
- $\sigma = 5.670374419 \times 10^{-8}\text{ W/(m}^2\text{K}^4)$ (Constante universelle de Stefan-Boltzmann)
- $S_0 = 1361.0\text{ W/m}^2$ (Constante solaire TOA)

#### 3. Rétroaction Thermodynamique de Clausius-Clapeyron
$$e_s(T) = e_0 \cdot \exp\left( \frac{L_v}{R_v} \left( \frac{1}{T_0} - \frac{1}{T} \right) \right)$$
- $L_v = 2.501 \times 10^6\text{ J/kg}$, $R_v = 461.5\text{ J/(kg}\cdot\text{K)}$

#### 4. Bilan Enthalpique de Fonte Glaciaire PDD (Degrés-Jours Positifs)
$$\Delta h_{\text{ice}} = \frac{k_{\text{pdd}} \cdot \text{PDD} \cdot \Delta t}{\rho_{\text{ice}} \cdot L_f} \quad [\text{m}], \quad \Delta S_L = \frac{\sum \Delta M_{\text{ice}}}{\rho_{\text{water}} \cdot A_{\text{ocean}}}$$
- $L_f = 333.55 \times 10^3\text{ J/kg}$, $\rho_{\text{ice}} = 917.0\text{ kg/m}^3$

---

### B. Dynamique des Fluides Géophysiques & Atmosphère

#### 1. Paramètre de Coriolis & Équilibre Géostrophique
$$f = 2\Omega \sin\phi \quad (\Omega = 7.2921159 \times 10^{-5}\text{ rad/s})$$

#### 2. Circulation Tri-Cellulaire Analytique (Hadley, Ferrel, Polaire)
- **Cellule de Hadley ($0^\circ - 30^\circ$)** : Alizés de surface d'Est ($u_z < 0$) et convergence équatoriale (ZCIT).
- **Cellule de Ferrel ($30^\circ - 60^\circ$)** : Vents dominants d'Ouest ($u_z > 0$, Quarantièmes rugissants).
- **Cellule Polaire ($60^\circ - 90^\circ$)** : Vents d'Est polaires froids ($u_z < 0$).
- **Puissance éolienne de Betz** : $P_{\text{wind}} = \frac{1}{2} \rho_{\text{air}} v^3 C_p$ ($C_p \le 0.593$).

---

### C. Géophysique Solide, Hydrogéologie & Isostasie

#### 1. Ajustement Isostatique Glaciaire (GIA / Post-Glacial Rebound)
$$\tau_{\text{gia}} \frac{\partial z}{\partial t} = -(z - z_{\text{eq}}) - \frac{\rho_{\text{ice}}}{\rho_{\text{manteau}}} h_{\text{ice}}$$
- $\tau_{\text{gia}} = 4000\text{ ans}$, $\rho_{\text{manteau}} = 3300\text{ kg/m}^3$, $\rho_{\text{ice}} = 917\text{ kg/m}^3$.

#### 2. Écoulement en Milieu Poreux de Darcy 2D (Aquifères Latéraux sur Graphe H3)
$$\vec{q}_{ij} = -K_{\text{hydraulique}} \cdot \frac{h_j - h_i}{d_{ij}}, \quad h_i = z_i + \frac{W_{\text{aqua}, i}}{S_y \cdot A}$$

#### 3. Rétention d'Eau du Sol de van Genuchten (AWC)
$$\theta(h) = \theta_r + \frac{\theta_s - \theta_r}{\left[1 + (\alpha |h|)^n\right]^m}, \quad \text{AWC} = \theta(h_{\text{FC}}) - \theta(h_{\text{PWP}})$$

#### 4. Vitesse de Sédimentation Alluviale de Stokes
$$v_s = \frac{2}{9} \frac{(\rho_p - \rho_f) \, g}{\mu(T)} r^2 \quad [\text{m/s}]$$

---

### D. Biophysique, Métabolisme & Traçage Isotopique

#### 1. Atténuation Lumineuse de Beer-Lambert dans la Canopée
$$I(z) = I_0 \cdot \exp(-k_{\text{ext}} \cdot \text{LAI}) \quad (k_{\text{ext}} \approx 0.6)$$

#### 2. Décroissance Radioactive du Carbone 14 ($^{14}\text{C}$) & Fractionnement $\delta^{13}\text{C}$
$$N(t) = N_0 \exp(-\lambda_{14} \cdot t), \quad \lambda_{14} = \frac{\ln 2}{5730\text{ ans}} \approx 1.2097 \times 10^{-4}\text{ an}^{-1}$$
$$t_{\text{BP}} = -8033 \ln\left(\frac{A}{A_0}\right)$$
- C3 : $\delta^{13}\text{C} \approx -28\text{ ‰}$ ; C4 : $\delta^{13}\text{C} \approx -12\text{ ‰}$ ; Marin : $\delta^{13}\text{C} \approx -1.5\text{ ‰}$.

#### 3. Loi Métabolique de Kleiber ($3/4$) & Actuariat de Gompertz-Makeham
$$B_{\text{basal}} = B_0 \cdot M_{\text{hum}}^{3/4}, \quad \mu(t_{\text{age}}) = \alpha \cdot e^{\beta \cdot t_{\text{age}}} + \gamma_{\text{env}}$$

#### 4. Diffusion Génétique de Kimura / Wright-Fisher
$$p_{t+\Delta t} = \text{clamp}\left( p_t + s \cdot p_t(1 - p_t) \Delta t + \sqrt{\frac{p_t(1 - p_t)}{2 N_e}} \cdot \mathcal{N}(0, \Delta t), \, 0.0, \, 1.0 \right)$$

---

## 4. Tier 2 — Optional Cliodynamic & Phenomenological Models

### A. Sélection Culturelle Multi-Niveaux & Équation de Price (George R. Price, 1970)
$$\Delta \bar{z} = \frac{\text{Cov}(w_g, z_g)}{\bar{w}} + \frac{\mathbb{E}[w_g \Delta z_g]}{\bar{w}}$$
- Le terme inter-groupes $\text{Cov}(w_g, z_g) > 0$ propage l'altruisme et la discipline militaire via les conflits inter-polities.
- Le terme intra-groupe $\mathbb{E}[w_g \Delta z_g] < 0$ modélise la prédation interne par les passagers clandestins (*free-riders*).

### B. Intensification Agricole Démographique de Boserup (Ester Boserup, 1965)
$$L_{\text{requis}} = L_0 \cdot \left(\frac{\text{Rendement}}{\text{Rendement}_0}\right)^{1.40}$$
- La pression démographique ($D = N / \text{Surface}$) force la transition : Cueillette $\to$ Jachère longue $\to$ Culture annuelle $\to$ Irrigation continue en terrasses.

### C. Évolution Technologique Combinatoire de W. Brian Arthur (2009)
$$\frac{dT}{dt} = \mu_{\text{comb}} \cdot T^{1.25} \cdot \left(\frac{\text{Capital}_{\text{R\&D}}}{N}\right)^{0.5}$$
- L'innovation émerge par recombinaison modulaire des briques technologiques préexistantes ($N_{\text{comb}} \propto T^2$).

### D. Économie Biophysique Exergétique (Kümmel / Ayres-Warr)
$$Y_i = A_i \cdot K_i^\alpha \cdot L_i^\beta \cdot \Big( E_{\text{utile}, i} \Big)^\gamma \quad (\alpha + \beta + \gamma = 1, \, \gamma \approx 0.50)$$

### E. Allométrie Métabolique Urbaine de West-Bettencourt
- Super-linéarité socio-économique : $Y_{\text{PIB, Innovation}} \propto N_{\text{urbain}}^{1.15}$
- Sous-linéarité des infrastructures : $\text{Réseaux} \propto N_{\text{urbain}}^{0.85}$

### F. Rente d'Épuisement des Ressources de Hotelling
$$[P(t) - MC] = [P_0 - MC] \cdot e^{r \cdot t}$$

### G. Épidémiologie Métapopulationnelle Spatialisée SEIR-V
$$\frac{dS_i}{dt} = -\beta_i S_i \frac{I_i}{N_i} + \sum_j (M_{ji} S_j - M_{ij} S_i), \quad \frac{dE_i}{dt} = \beta S \frac{I}{N} - \sigma E, \quad \frac{dI_i}{dt} = \sigma E - (\gamma + \mu_v) I$$

### H. Nouvelle Économie Géographique de Krugman (NEG)
$$\omega_i = \left[ \sum_j Y_j \cdot P_j^{\sigma - 1} \cdot e^{-\tau_{ij} (\sigma - 1)} \right]^{1/\sigma}$$

### I. Ségrégation Spatiale de Schelling-Axelrod
$$U_i = \mathbb{I}\left(\frac{\sum_{j \in \mathcal{N}(i)} \mathbb{I}(C_j = C_i)}{|\mathcal{N}(i)|} \ge \tau_{\text{tol}}\right)$$

### J. Complexité Institutionnelle & Rendements Décroissants de Tainter
$$C_i = \ln(1 + 0.05 K_i), \quad \Sigma_{\text{maint}} = C_i^{1.20} \times 10\text{ Joules}$$

---

## 5. Performance & Numerical Optimization Strategy

| Modèle | Tier | Stratégie d'Optimisation | Coût d'Exécution (175k cellules) |
| :--- | :--- | :--- | :--- |
| **Milankovitch & Insolation** | Tier 1 | Calcul analytique orbital au pas mensuel | $< 0.01\text{ ms}$ |
| **Coriolis & Circulation Hadley** | Tier 1 | Formule zonale vectorisée 1D | $< 0.03\text{ ms}$ |
| **GIA Isostasie & Décroissance C-14**| Tier 1 | Décroissance exponentielle vectorielle | $< 0.02\text{ ms}$ |
| **Stefan-Boltzmann / Clausius** | Tier 1 | Vectorisation SIMD (`VectorAPI` AVX-512) | $< 0.05\text{ ms}$ |
| **Darcy 2D Aquifères** | Tier 1 | Découplage au pas mensuel ($\Delta t_{\text{slow}} = 30\text{ j}$) | $\approx 0.04\text{ ms/tick}$ |
| **Fonte PDD Cryosphère** | Tier 1 | Filtrage sur cellules gelées actives | $< 0.02\text{ ms}$ |
| **van Genuchten / Stokes** | Tier 1 | Calcul algébrique direct / Forme fermée | $< 0.03\text{ ms}$ |
| **Beer-Lambert & Farquhar** | Tier 1 | Look-Up Table (LUT) 2D / Exponentielle SIMD | $< 0.08\text{ ms}$ |
| **Équation de Price & Boserup** | Tier 2 | Opérations arithmétiques locales $O(1)$ | $< 0.03\text{ ms}$ |
| **SEIR Métapopulationnel** | Tier 2 | *Sparse Active Set* ($I_i > 0 + \text{1-Ring}$) | $0.00\text{ ms}$ (hors crise), $< 0.3\text{ ms}$ (crise) |
| **West-Bettencourt / Kümmel** | Tier 2 | Opérations arithmétiques locales $O(1)$ | $< 0.03\text{ ms}$ |
| **Krugman NEG** | Tier 2 | Calcul matriciel clairsemé sur routes commerciales | $< 0.20\text{ ms}$ |
