# Guide complet des variables d'état & des Systèmes d'Équations Différentielles d'Ether

> **Document de Référence Technique & Mathématique**  
> *Version 4.0.0 — Modélisation à Base Physique Strictement Dérivée*

---

## 1. Vue d'Ensemble & Architecture des Échelles Temporelles

Le moteur **Ether** fait évoluer l'état planétaire et sociétal sur une grille hexagonale H3 ($175\,000+$ cellules à la résolution 8) via des noyaux orientés données (Data-Oriented Design — `WorldBuffer`, `AgentBuffer`) en résolvant un système d'équations différentielles stochastiques et déterministes (EDO/EDP).

### Échelles Temporelles Intégrées
- **Échelle Rapide ($\Delta t_{\text{fast}} = 1 \text{ jour} = 86\,400 \text{ s}$)** :
  - Équations de conservation de flux thermodynamiques de ressources et logistique (`FluxEngine`).
  - Équilibre instantané de l'offre et de la demande (Prix locaux).
- **Échelle Lente ($\Delta t_{\text{slow}} = 1 \text{ mois} \approx 2.592 \times 10^6 \text{ s} = \Delta t / 31\,557\,600 \text{ an}$)** :
  - Cinétique métabolique d'Arrhenius et démographie des cohortes (`DemographicKernel`).
  - Production primaire et régénération écologique (`EnvironmentalKernel`).
  - Transport démographique par gradients thermodynamiques d'Onsager (`ThermodynamicMigrationEngine`).
  - Accumulation entropique de capital et complexité de Tainter (`UrbanKernel`).

---

## 2. Table des Variables d'État Systémiques

| Variable | Symbole | Unité SI | Noyau / Module | Description Physique |
| :--- | :--- | :--- | :--- | :--- |
| **Biomasse Humaine** | $M_{\text{hum}}$ | $\text{kg}$ / cohorte | `DemographicKernel` | Masse totale de la cohorte d'agents humains. |
| **Stock Énergétique Interne** | $E_{\text{cohorte}}$ | $\text{J}$ | `DemographicKernel` | Énergie disponible pour le travail et la mitose. |
| **Âge de la Cohorte** | $t_{\text{age}}$ | $\text{an}$ / $\text{s}$ | `DemographicKernel` | Temps écoulé depuis la genèse de la cohorte. |
| **Stock de Nourriture** | $F$ | $\text{unités}$ | `EnvironmentalKernel` | Biomasse comestible disponible sur la cellule. |
| **Température Ambiante** | $T$ | ${^\circ\text{C}}$ / $\text{K}$ | `AtmosphericEngine` | Température de surface issue du bilan radiatif. |
| **Précipitations Annuelles** | $R$ | $\text{mm/an}$ | `AtmosphericEngine` | Pluviométrie issue de la circulation Hadley. |
| **Forçage Radiatif $\text{CO}_2$** | $\Delta F_{\text{CO2}}$ | $\text{W/m}^2$ | `GreenhouseRadiativeEngine` | Forçage de l'effet de serre anthropique/volcanique. |
| **Niveau de la Mer** | $h_{\text{mer}}$ | $\text{m}$ | `GreenhouseRadiativeEngine` | Dilatation thermique et fonte des calottes. |
| **Potentiel Économique** | $P_i$ | $\text{valeur}$ | `FluxEngine` | Ratio dynamique offre/demande locaux. |
| **Complexité Institutionnelle** | $C_i$ | $\text{adimensionnel}$ | `UrbanKernel` | Indice de complexité organisationnelle de Tainter. |

---

## 3. Systèmes d'Équations Différentielles par Noyau

### A. Noyau Démographique (`DemographicKernel`)

Le métabolisme et l'évolution des cohortes d'agents sont gouvernés par le système d'EDO suivant :

#### 1. Consommation Métabolique & Échelle Allométrique de Kleiber ($3/4$)
Le besoin énergétique métabolique basal d'une cohorte de masse $M_{\text{hum}}$ est calculé par la loi de Kleiber :
$$B_{\text{basal}} = B_0 \cdot M_{\text{hum}}^{3/4} \quad \left(B_0 = 3.39 \text{ W/kg}^{3/4}\right)$$

Le coût de structure et la variation de l'énergie interne s'écrivent :
$$\frac{dE_{\text{cohorte}}}{dt} = \dot{E}_{\text{ingérée}} - \left( \frac{B_{\text{basal}}}{86\,400} + M_{\text{hum}}^{1.1} \times 1000 \right) \cdot \Delta t$$

#### 2. Natalité & Taux de Fertilité
$$\frac{dM_{\text{hum, naissances}}}{dt} = M_{\text{hum}} \cdot f(E_{\text{cohorte}}, M_{\text{hum}})$$
$$f(E, M) = \begin{cases} 0.05 \cdot \left(1 - \frac{M}{2000}\right) & \text{si } E \ge 50.0 \text{ J} \\ 0.01 \cdot \left(1 - \frac{M}{2000}\right) & \text{sinon} \end{cases}$$

#### 3. Mortalité d'Actuariat de Gompertz-Makeham
Le taux de mortalité instantané $\mu(t_{\text{age}})$ intègre la sénescence cellulaire et la dénutrition :
$$\mu(t_{\text{age}}) = \alpha + \beta \cdot e^{\gamma \cdot t_{\text{age}}} + \mu_{\text{famine}}$$
- $\alpha = 0.0002 \text{ an}^{-1}$ (Mortalité environnementale de fond)
- $\beta = 0.00003 \text{ an}^{-1}$, $\gamma = 0.085 \text{ an}^{-1}$ (Vieillissement biologique Gompertz)
- $\mu_{\text{famine}} = 0.2 \text{ an}^{-1}$ si $E_{\text{cohorte}} < 0$.

---

### B. Noyau Écologique & Environnemental (`EnvironmentalKernel`)

La variation temporelle du stock de nourriture $F_i$ sur une cellule $i$ s'exprime par l'EDO :
$$\frac{dF_i}{dt} = \text{Prod}_{\text{biome}} \cdot k_{\text{Arrhenius}}(T_i) \cdot f_{\text{hydrique}}(R_i) - \lambda_{\text{décomposition}} \cdot F_i$$

#### 1. Cinétique Métabolique Thermique d'Arrhenius (Johnson-Eyring)
$$\kappa_{\text{Arrhenius}}(T) = \frac{\exp\left(-\frac{E_a}{R \cdot T_{\text{Kelvin}}}\right)}{\exp\left(-\frac{E_a}{R \cdot T_{\text{opt}}}\right)} \cdot f_{\text{dénaturation}}(T)$$
- $E_a = 54\,000 \text{ J/mol}$ (Énergie d'activation enzymatique)
- $R = 8.31446 \text{ J/(mol}\cdot\text{K)}$ (Constante des gaz parfaits)
- $T_{\text{opt}} = 298.15 \text{ K} \quad (25^\circ\text{C})$

---

### C. Modèle de Transport Démographique d'Onsager (`ThermodynamicMigrationEngine`)

Le transport de population entre deux cellules limitrophes $i$ et $j$ dérive du **gradient du potentiel libre par habitant** $\Phi_i$ :
$$\Phi_i = \frac{F_i + W_i}{N_i + 1}$$
$$\Delta \Phi_{ij} = \Phi_j - \Phi_i$$

Le flux de transport Onsager $J_{ij}$ entre cellules adjacentes s'écrit :
$$J_{ij} = M_0 \cdot \Delta \Phi_{ij} \cdot N_i$$
$$\frac{dN_i}{dt} = - \sum_{j \in \text{Voisins}(i)} J_{ij}$$
$$\frac{dN_j}{dt} = + \sum_{j \in \text{Voisins}(i)} J_{ij}$$
- $M_0 = 0.05$ (Coefficient de mobilité d'Onsager).

---

### D. Radiatif & Effet de Serre (`GreenhouseRadiativeEngine`)

Le forçage radiatif $\Delta F$ (en $\text{W/m}^2$) causé par la concentration en $\text{CO}_2$ (ppm) et $\text{CH}_4$ (ppb) s'écrit :
$$\Delta F = 5.35 \cdot \ln\left(\frac{[\text{CO}_2]}{280.0}\right) + 0.036 \cdot \left(\sqrt{[\text{CH}_4]} - \sqrt{720.0}\right)$$

L'anomalie de température mondiale $\Delta T$ et l'élévation du niveau de la mer $\Delta h_{\text{mer}}$ satisfont :
$$\Delta T = \lambda_{\text{climat}} \cdot \Delta F \quad (\lambda_{\text{climat}} = 0.8 \text{ }^\circ\text{C / (W/m}^2))$$
$$\Delta h_{\text{mer}} = 42.5 \cdot \Delta T \text{ mètres}$$

---

### E. Effondrement de Complexité d'Urbanisation (`UrbanKernel` / Tainter)

La dynamique d'accumulation et de maintenance du capital s'appuie sur la théorie de Joseph Tainter :
$$C_i = \ln(1 + 0.1 \cdot K_i)$$
$$\Sigma_{\text{maint}} = C_i^{\theta} \cdot 1000 \text{ J} \quad (\theta = 1.15)$$
$$\frac{dK_i}{dt} = P_i \cdot N_i \cdot 0.1 - \Sigma_{\text{maint}}$$

Si $\Sigma_{\text{maint}} > \text{Production Brute}$, la cellule enregistre un rendement marginal négatif provoquant l'érosion du capital et la simplification des infrastructures urbaines.

---

## 4. Bilan de Validation par les Tests

L'intégralité du moteur Ether et de ces formulations différentielles est couverte par la suite de tests unitaires et d'intégration :
```text
[INFO] Results:
[INFO] Tests run: 127, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
