# Ether Simulation — Differential Equations & State Variable Specification

> **Master Technical, Physical & Mathematical Specification**  
> *Version 4.3.0 — Strict Separation between Core Model Physics (Tier 1) and Optional Cliodynamic / Phenomenological Modules (Tier 2)*

---

## 1. Architectural Epistemology & Two-Tier Separation

The **Ether Engine** strictly partitions its computational mathematical models into two ontological tiers:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│             TIER 1 : CORE MODEL PHYSICS (Le Socle Dur — Invariant & Consensuel)        │
│    • Lois universelles de conservation (Masse, Énergie, Impulsion, Entropie)           │
│    • Thermodynamique & Rayonnement (Stefan-Boltzmann, Clausius-Clapeyron, Fonte PDD)   │
│    • Hydrogéologie poreuse (Darcy 2D), Chimie des solutions (Loi de Henry)             │
│    • Pédologie hydraulique (van Genuchten AWC) & Sédimentation alluviale (Stokes)     │
│    • Transfert radiatif végétal (Beer-Lambert LAI) & Photosynthèse (Farquhar FvCB)     │
│    • Biophysique métabolique (Kleiber 3/4, Gompertz-Makeham, Stull Wet-Bulb)          │
│    • Génétique des populations mathématique (Kimura / Wright-Fisher SDE)               │
│    • Travail mécanique de frottement (Coulomb/Hydrodynamique) & Enthalpie de réaction  │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Forçages et rétroactions biophysiques
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│       TIER 2 : OPTIONAL PLUGINS & CLIODYNAMICS (Phénoménologique & Débattu)           │
│    • Économie biophysique exergétique (Kümmel / Ayres-Warr)                           │
│    • Lois d'échelle d'allométrie urbaine (West-Bettencourt N^1.15)                    │
│    • Rente d'épuisement des ressources non-renouvelables (Règle de Hotelling)         │
│    • Dynamique institutionnelle & effondrement de complexité (Tainter)                │
│    • Modèles proies-prédateurs socio-écologiques (NASA HANDY)                         │
│    • Sociologie historique & cohésion de frontière (Turchin Asabiyyah)                 │
│    • Épidémiologie compartimentale métapopulationnelle (SEIR-V Spatial)               │
│    • Théorie des jeux spatiaux & ségrégation émergente (Schelling-Axelrod)             │
│    • Nouvelle économie géographique centre-périphérie (Krugman NEG)                  │
│    • Dérive mémétique stochastique (SDE Langevin 4D sur tenseurs culturels)           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Multi-Scale Temporal Decoupling

Ether decouples numerical integration across two discrete time scales on the Uber H3 hexagonal grid ($175,000+$ cells at Resolution 6–8):

- **Fast Tick Scale ($\Delta t_{\text{fast}} = 1 \text{ day} = 86,400 \text{ s}$)**:
  - Resolves symmetric finite-volume mass fluxes under Courant-Friedrichs-Lewy (CFL) limits (`FluxEngine`).
  - Evaluates instantaneous wet-bulb temperature and hyperthermia thresholds (`WetBulbTemperatureEngine`).
  - Resolves active localized epidemiological transmissions via sparse active sets (`EpidemiologyEngine`).

- **Slow Tick Scale ($\Delta t_{\text{slow}} = 30 \text{ days} \approx 2.592 \times 10^6 \text{ s} = \Delta t / 31,557,600 \text{ yr}$)**:
  - Solves 2D lateral piezometric aquifer diffusion (Darcy flux).
  - Integrates cryospheric thermodynamic melt ($L_f = 333.55\text{ kJ/kg}$) and eustatic sea level adjustments.
  - Resolves van Genuchten soil water retention and Stokes siltation kinetics.
  - Integrates Farquhar FvCB photosynthesis, Beer-Lambert light interception, and soil organic carbon decay.
  - Updates cohort aging, Gompertz-Makeham senescence, and Kimura genetic drift.

---

## 3. Tier 1 — Core Model Physics (Socle Fondamental)

### A. Thermodynamique, Cryosphère & Transferts Radiatifs Globaux

#### 1. Bilan Radiatif Non-Linéaire de Stefan-Boltzmann
Stabilisation thermique intrinsèque de la planète par émission infrarouge de corps noir :
$$\epsilon \sigma T_{\text{surface}}^4 = \frac{S_0}{4} (1 - \alpha_{\text{albédo}}) + \Delta F_{\text{effet\_de\_serre}}$$
- $\sigma = 5.670374419 \times 10^{-8}\text{ W/(m}^2\text{K}^4)$ (Constante universelle de Stefan-Boltzmann)
- $S_0 = 1361.0\text{ W/m}^2$ (Constante solaire TOA)
- $\Delta F_{\text{effet\_de\_serre}} = 5.35 \ln\left(\frac{[\text{CO}_2]}{[\text{CO}_2]_0}\right) + 0.036\left(\sqrt{[\text{CH}_4]} - \sqrt{[\text{CH}_4]_0}\right) + \Delta F_{\text{H}_2\text{O}}$

#### 2. Rétroaction Thermodynamique de Clausius-Clapeyron
Saturation en vapeur d'eau régissant l'intensité des précipitations ($+7\%/\text{K}$) :
$$e_s(T) = e_0 \cdot \exp\left( \frac{L_v}{R_v} \left( \frac{1}{T_0} - \frac{1}{T} \right) \right)$$
- $L_v = 2.501 \times 10^6\text{ J/kg}$ (Chaleur latente de vaporisation de l'eau)
- $R_v = 461.5\text{ J/(kg}\cdot\text{K)}$ (Constante spécifique de la vapeur d'eau)

#### 3. Bilan Enthalpique de Fonte Glaciaire PDD (Degrés-Jours Positifs)
Ablation des calottes et glaciers par enthalpie de changement de phase $H_2O$ :
$$\Delta h_{\text{ice}} = \frac{k_{\text{pdd}} \cdot \text{PDD} \cdot \Delta t}{\rho_{\text{ice}} \cdot L_f} \quad [\text{m}]$$
$$\Delta S_L = \frac{\sum \Delta M_{\text{ice}}}{\rho_{\text{water}} \cdot A_{\text{ocean}}} \quad [\text{m}]$$
- $L_f = 333.55 \times 10^3\text{ J/kg}$ (Chaleur latente de fusion de la glace)
- $\rho_{\text{ice}} = 917.0\text{ kg/m}^3$, $\rho_{\text{water}} = 1000.0\text{ kg/m}^3$
- $A_{\text{ocean}} \approx 3.61 \times 10^{14}\text{ m}^2$ (Superficie océanique planétaire)

#### 4. Température Humide de Stull ($T_{\text{wb}}$) & Seuil Létal d'Hyperthermie
$$T_{\text{wb}} = T \arctan\left(0.151977 \sqrt{RH + 8.313659}\right) + \arctan(T + RH) - \arctan(RH - 1.676331) + 0.00391838 \, RH^{1.5} \arctan(0.023101 \, RH) - 4.686035$$
$$\text{Si } T_{\text{wb}} \ge 35.0^\circ\text{C} \implies \left(\frac{dN}{dt}\right)_{\text{hyperthermie}} = -0.15 \cdot N \quad (\text{Refroidissement sudoral impossible})$$

#### 5. Puissance Cinétique Éolienne (Limite de Betz)
$$P_{\text{wind}} = \frac{1}{2} \rho_{\text{air}} \cdot v^3 \cdot C_p \quad \left(\rho_{\text{air}} = 1.225\text{ kg/m}^3, \, C_p \le 0.593\right)$$

---

### B. Hydrogéologie, Pédologie, Géomorphologie & Chimie des Solutions

#### 1. Écoulement en Milieu Poreux de Darcy 2D (Aquifères Latéraux sur Graphe H3)
$$\vec{q}_{ij} = -K_{\text{hydraulique}} \cdot \frac{h_j - h_i}{d_{ij}}, \quad h_i = z_i + \frac{W_{\text{aqua}, i}}{S_y \cdot A}$$
$$\frac{\partial h_i}{\partial t} = \frac{R_i - E_{\text{pompage}, i}}{S_y} + \sum_{j \in \mathcal{N}(i)} \frac{K \cdot A_{ij}}{S_y \cdot d_{ij}} (h_j - h_i)$$

#### 2. Loi de Dissolution de Henry & Pompe de Solubilité Océanique
$$[\text{CO}_{2,\text{aq}}] = K_H(T) \cdot p_{\text{CO}_2} \quad \text{avec} \quad K_H(T) = K_0 \cdot \exp\left( \frac{-\Delta H_{\text{sol}}}{R} \left( \frac{1}{T} - \frac{1}{T_0} \right) \right)$$

#### 3. Rétention d'Eau du Sol de van Genuchten & Capacité Utile Agricole (AWC)
Courbe caractéristique d'humidité du sol en fonction de la succion matricielle $h$ :
$$\theta(h) = \theta_r + \frac{\theta_s - \theta_r}{\left[1 + (\alpha |h|)^n\right]^m} \quad \left(m = 1 - \frac{1}{n}\right)$$
$$\text{AWC} = \max\left(0, \, \theta(h_{\text{FC}}) - \theta(h_{\text{PWP}})\right)$$
- $h_{\text{FC}} = -330\text{ cm}$ (Capacité au champ, $\text{pF} = 2.5$)
- $h_{\text{PWP}} = -15000\text{ cm}$ (Point de flétrissement permanent, $\text{pF} = 4.2$)

#### 4. Vitesse de Sédimentation Alluviale de Stokes (Ensablement Fluvial & Portuaire)
$$v_s = \frac{2}{9} \frac{(\rho_p - \rho_f) \, g}{\mu(T)} r^2 \quad [\text{m/s}]$$
- $\rho_p = 2650\text{ kg/m}^3$ (Masse volumique des particules de quartz/limon)
- $\rho_f = 1000\text{ kg/m}^3$, $\mu(T) \approx 1.002 \times 10^{-3}\text{ Pa}\cdot\text{s}$ à $20^\circ\text{C}$
- $r = 20\,\mu\text{m}$ (Rayon des grains de limon fin)

#### 5. Évapotranspiration Potentielle de Priestley-Taylor
$$\text{PET} = \alpha_{\text{PT}} \cdot \frac{\Delta}{\Delta + \gamma_{\text{psy}}} \cdot \frac{R_n - G}{\lambda_{\text{vap}}} \quad (\alpha_{\text{PT}} = 1.26)$$

#### 6. Stœchiométrie des Sols & Loi du Minimum de Liebig
$$Y_{\text{agri}} = Y_0 \cdot \min\left( \frac{N_{\text{npk}}}{50.0}, \, \frac{P_{\text{npk}}}{25.0}, \, \frac{K_{\text{npk}}}{25.0} \right)$$

---

### C. Biophysique, Métabolisme, Canopée & Génétique des Populations

#### 1. Loi d'Atténuation Lumineuse de Beer-Lambert dans la Canopée
Interception du rayonnement photosynthétiquement actif (PAR) par l'indice foliaire $\text{LAI}$ ($m^2/m^2$) :
$$I(z) = I_0 \cdot \exp(-k_{\text{ext}} \cdot \text{LAI})$$
- $k_{\text{ext}} \approx 0.5 - 0.7$ (Coefficient d'extinction de la végétation)

#### 2. Loi Allométrique Métabolique de Kleiber ($3/4$)
$$B_{\text{basal}} = B_0 \cdot M_{\text{hum}}^{3/4} \quad (B_0 = 3.39\text{ W/kg}^{3/4})$$

#### 3. Loi Actuarielle de Sénescence de Gompertz-Makeham
$$\mu(t_{\text{age}}) = \alpha \cdot e^{\beta \cdot t_{\text{age}}} + \gamma_{\text{environnement}} \quad (\alpha = 0.0001\text{ an}^{-1}, \, \beta = 0.08\text{ an}^{-1})$$

#### 4. Modèle Biochimique de Photosynthèse FvCB (C3/C4)
$$A_{\text{net}} = \min(A_c, A_j, A_p) - R_{\text{sombre}}$$

#### 5. Équation de Diffusion Génétique de Kimura / Wright-Fisher
$$p_{t+\Delta t} = \text{clamp}\left( p_t + s \cdot p_t(1 - p_t) \Delta t + \sqrt{\frac{p_t(1 - p_t)}{2 N_e}} \cdot \mathcal{N}(0, \Delta t), \, 0.0, \, 1.0 \right)$$

---

### D. Mécanique des Fluides, Transports & Enthalpie Réactionnelle

#### 1. Volumes Finis Symétriques & Conservation Stricte de la Masse
$$J_{ij} = \text{clamp}\left( (P_j - P_i) \cdot \frac{\sigma_0}{1 + |\Delta z_{ij}| \cdot 0.1} \cdot \Delta t, \, -0.5 F_j, \, 0.5 F_i \right)$$

#### 2. Travail Mécanique de Frottement Terrestre et Hydrodynamique
$$W = \mu_{\text{biome}} \cdot m \cdot g \cdot d \quad [\text{Joules}]$$

#### 3. Enthalpie de Réduction Métallurgique
$$\Delta H_{\text{smelt}}(\text{Fe}_2\text{O}_3) = +24.7\text{ MJ/kg Fe}$$

---

## 4. Tier 2 — Optional Cliodynamic & Phenomenological Models

### A. Économie Biophysique Exergétique (Kümmel / Ayres-Warr)
Le capital et le travail sont des convertisseurs d'exergie utile :
$$Y_i = A_i \cdot K_i^\alpha \cdot L_i^\beta \cdot \Big( E_{\text{utile}, i} \Big)^\gamma \quad (\alpha + \beta + \gamma = 1)$$
$$E_{\text{utile}} = E_{\text{primaire}} \cdot \eta_{\text{thermodynamique}}(t)$$

### B. Allométrie Métabolique Urbaine de West-Bettencourt
- **Super-linéarité socio-économique** : $Y_{\text{PIB, Innovation, Criminalité}} \propto N_{\text{urbain}}^{1.15}$
- **Sous-linéarité des réseaux & infrastructures** : $\text{Infrastructures}_{\text{voirie, câblage, stations}} \propto N_{\text{urbain}}^{0.85}$

### C. Rente d'Épuisement des Ressources Non-Renouvelables de Hotelling
Évolution du prix de rareté des gisements fossiles et miniers :
$$P(t) - MC = (P_0 - MC) \cdot e^{r \cdot t}$$

### D. Complexité Institutionnelle & Rendements Décroissants de Tainter
$$C_i = \ln(1 + 0.1 \cdot K_i), \quad \Sigma_{\text{maint}} = C_i^{1.15} \times 1000\text{ Joules}, \quad \frac{dK_i}{dt} = P_i \cdot N_i \cdot 0.1 - \Sigma_{\text{maint}}$$

### E. Modèle Prédateur-Proie Social HANDY de la NASA (Motesharrei et al., 2014)
Couplage Élite ($Y$), Plèbe ($X$), Nature ($x$) et Capital ($K$) :
$$\frac{dx}{dt} = \beta x (x_{\max} - x) - \delta (X + Y) x$$
$$\frac{dK}{dt} = \delta X x - (X + \kappa Y) \quad (\kappa = 10.0), \quad \frac{dX}{dt} = (\gamma_X - \alpha_X) X, \quad \frac{dY}{dt} = (\gamma_Y - \alpha_Y) Y$$

### F. Dynamique Séculaire d'Asabiyyah des Frontières de Turchin
$$\frac{dA_i}{dt} = r_a \cdot H_{\text{frontière\_hostile}} \cdot (1 - A_i) - d_a \cdot A_i \cdot \mathbb{I}_{\text{hinterland\_pacifié}}$$

### G. Épidémiologie SEIR-V Métapopulationnelle Spatialisée
$$\frac{dS_i}{dt} = -\beta_i S_i \frac{I_i}{N_i} + \sum_j M_{ji} S_j - \sum_j M_{ij} S_i$$
$$\frac{dE_i}{dt} = \beta_i S_i \frac{I_i}{N_i} - \sigma E_i, \quad \frac{dI_i}{dt} = \sigma E_i - (\gamma + \mu_v) I_i, \quad \frac{dR_i}{dt} = \gamma I_i$$

### H. Nouvelle Économie Géographique de Krugman (Centre-Périphérie)
Agglomération spatiale par interaction entre coûts de transport iceberg $\tau_{ij}$ et rendements d'échelle croissants :
$$\omega_i = \left[ \sum_j Y_j \cdot P_j^{\sigma - 1} \cdot e^{-\tau_{ij} (\sigma - 1)} \right]^{1/\sigma}$$

### I. Dérive Mémétique Stochastique 4D (SDE / Langevin Bridge)
$$d\mathbf{C}_i = \left[ \mu_d \sum_{j \in \mathcal{N}(i)} (\mathbf{C}_j - \mathbf{C}_i) + \lambda_{\text{forcing}} (\mathbf{C}_{\text{tensor}}(x_i) - \mathbf{C}_i) \right] dt + \sigma_{\text{Langevin}} d\mathbf{W}_t$$
- Dimensions : 0: Isoglosse linguistique, 1: Parenté/Clan, 2: Rituels, 3: Souveraineté politique.

---

## 5. Performance & Numerical Optimization Strategy

| Modèle | Tier | Stratégie d'Optimisation | Coût d'Exécution (175k cellules) |
| :--- | :--- | :--- | :--- |
| **Stefan-Boltzmann / Clausius** | Tier 1 | Vectorisation SIMD (`VectorAPI` AVX-512) | $< 0.05\text{ ms}$ |
| **Darcy 2D Aquifères** | Tier 1 | Découplage au pas mensuel ($\Delta t_{\text{slow}} = 30\text{ j}$) | $\approx 0.04\text{ ms/tick}$ |
| **Fonte PDD Cryosphère** | Tier 1 | Filtrage sur cellules gelées actives | $< 0.02\text{ ms}$ |
| **van Genuchten / Stokes** | Tier 1 | Calcul algébrique direct / Forme fermée | $< 0.03\text{ ms}$ |
| **Beer-Lambert & Farquhar** | Tier 1 | Look-Up Table (LUT) 2D / Exponentielle SIMD | $< 0.08\text{ ms}$ |
| **Kimura Genetic SDE** | Tier 1 | Forme fermée vectorisée 1D | $< 0.08\text{ ms}$ |
| **SEIR Métapopulationnel** | Tier 2 | *Sparse Active Set* ($I_i > 0 + \text{1-Ring}$) | $0.00\text{ ms}$ (hors crise), $< 0.3\text{ ms}$ (crise) |
| **West-Bettencourt / Kümmel** | Tier 2 | Opérations arithmétiques locales $O(1)$ | $< 0.03\text{ ms}$ |
| **Krugman NEG** | Tier 2 | Calcul matriciel clairsemé sur routes commerciales | $< 0.20\text{ ms}$ |
