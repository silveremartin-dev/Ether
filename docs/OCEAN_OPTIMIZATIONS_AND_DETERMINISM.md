# 🌊 Documentation des Optimisations Océaniques, Distorsion Physique & Déterminisme (`Ether Engine`)

**Version du document :** 4.0.0-SNAPSHOT  
**Dernière mise à jour :** 6 Août 2026  
**Auteurs :** Silvere Martin-Michiellot & Équipe Google DeepMind  

---

## 1. Contexte & Enjeux de Performance

Sur une grille planétaire globale Uber H3 (résolutions 6 à 8, représentant de 175 000 à plus de 1.2 million de cellules hexagonales), la surface océanique couvre plus de **70.8% de la superficie du globe**. Traiter chaque cellule d'océan abyssal (non peuplée par des sociétés humaines) avec la même résolution spatio-temporelle fine que les plaines alluviales ou les nœuds urbains terrestres entraîne un surcoût de calcul considérable.

Pour résoudre cette inefficience tout en offrant un contrôle précis aux chercheurs et utilisateurs, le moteur **Ether** intègre trois options majeures d'optimisation de la physique et de la navigation océanique. Ces options sont stockées au niveau de la structure `Scenario` (pour garantir la persistance et la portabilité des expériences) et sont ajustables dans le panneau `ScenarioSetupPanel` ainsi que via la classe `SimulationPerformanceConfig`.

---

## 2. Analyse Détaillée des 3 Options d'Optimisation

### 1️⃣ Fusion & Macro-Agrégation des Cellules Océaniques Abyssales (`oceanMacroAggregationEnabled`)

- **Principe & Mécanisme :**  
  Les cellules océaniques d'altitude négative stérile ou abyssale ($z < -200\text{ m}$ sans présence d'infrastructure maritime) sont regroupées virtuellement en macro-agrégats. Les équations de la dynamique thermohaline et des bilans de masse d'eau sont résolues à l'échelle du macro-bloc plutôt qu'individuellement sur chaque cellule H3.
- **Gain de Performance :**  
  Accélération globale de **+300% à +600%** (réduction de 4x à 7x de la charge de mise à jour des cellules fluides).
- **Distorsion Physique Introduite :**  
  - Lissage spatial des gradients micro-locaux de température, de salinité et d'acidification dans les fosses abyssales.
  - Atténuation des micro-vortex et des variations fines d'albedo à petite échelle.
- **Impact sur le Déterminisme :**  
  - En mode strict (`strictDeterminism = true`), cette option est désactivée pour garantir un calcul bit-à-bit identique cellule par cellule.
  - Lorsque l'option est active, la simulation reste déterministe par rapport à elle-même (reproductible avec la même graine `seed`), mais sa trajectoire diverge numériquement du calcul intégral 1:1.

---

### 2️⃣ Filtrage de Navigation & Transport Littoral vs Hauturier (`coastalNavigationOnlyEnabled`)

- **Principe & Mécanisme :**  
  Restreint la recherche de chemin (pathfinding $A^*$) et le calcul de mobilité des agents maritimes aux mailles du plateau continental et des zones littorales. La navigation hauturière (en haute mer) est désactivée jusqu'à ce que les technologies de navigation transocéanique (boussole, caravelle, sextant, propulsion à vapeur) soient débloquées dans l'arbre technologique (`TechnologyTree`).
- **Gain de Performance :**  
  Gain de **+200% à +400%** sur la phase d'évaluation des flux d'agents et du commerce maritime par tick (élimine l'exploration inutile du réseau hauturier mondial).
- **Distorsion Physique Introduite :**  
  - Supprime les dérives transocéaniques accidentelles d'agents aux époques pré-industrielles/archaïques.
  - Introduit un léger biais de confinement des flux maritimes le long des côtes avant le franchissement des jalons technologiques.
- **Impact sur le Déterminisme :**  
  - Intégralement déterministe sous les mêmes règles logiques, mais modifie le graphe d'états des agents par rapport à une mobilité hauturière sans restriction.

---

### 3️⃣ Sous-Échantillonnage Fréquentiel des Cycles Océaniques (`oceanMultiRateTickingEnabled`)

- **Principe & Mécanisme :**  
  Exécute les moteurs fluides et écologiques marins à évolution lente (`ThermohalineOceanEngine`, `OceanAcidificationEngine`) à une fréquence réduite ($\Delta t_{\text{océan}} = N \times \Delta t_{\text{atmosphère}}$, par exemple tous les 5 ou 10 ticks au lieu de chaque tick mensuel).
- **Gain de Performance :**  
  Gain de **+150% à +300%** sur le temps d'exécution de la boucle d'environnement planétaire.
- **Distorsion Physique Introduite :**  
  - Décalage par paliers (aliasing temporel) lors des chocs climatiques soudains (par exemple, injection d'aérosols stratosphériques lors d'éruptions volcaniques VEI-7 comme Tambora ou Samalas).
  - Lissage de la réponse thermique de la couche d'eau superficielle aux variations saisonnières extrêmes.
- **Impact sur le Déterminisme :**  
  - Rompt la continuité pas-à-pas d'une intégration à fréquence unitaire. La simulation demeure déterministe pour une fréquence $N$ et une graine `seed` données, mais ne produit pas des résultats bit-à-bit identiques avec un calcul à chaque tick.

---

## 3. Matrice Comparative : Performance vs Distorsion vs Déterminisme

| Configuration / Option | Gain de Vitesse (TPS) | Distorsion Physique | Niveau de Déterminisme |
| :--- | :--- | :--- | :--- |
| **Strict Determinism Mode** | Baseline (1x) | **0%** (Fidélité Physique Minimale & Absolue) | **Bit-Identical (100%)** |
| **Ocean Macro-Aggregation** | **+300% à +600%** | Lissage spatial des gradients abyssaux | Heuristique Reproductible |
| **Coastal Navigation Only** | **+200% à +400%** | Confinement littoral pré-technologique | Déterministe sous règles modifiées |
| **Ocean Multi-Rate Ticking** | **+150% à +300%** | Aliasing temporel des chocs marins | Sensible à la fréquence $N$ |

---

## 4. Recommandations de Configuration

1. **Recherche Scientifique & Benchmarks de Fidélité :**  
   Désactiver toutes les optimisations ou activer `strictDeterminism = true`. Cela garantit des trajectoires d'intégration numérique d'une précision maximale.
2. **Simulations Longue Durée / Mode Interactif :**  
   Activer les trois options d'optimisation pour atteindre un débit élevé (> 50-100 TPS) sur des scénarios couvrant plusieurs millénaires.
3. **Cliodynamique Régionale & Simulation Tronquée (Clipping) :**  
   Combiner la sélection de zone tronquée (`clippingEnabled = true`) avec la macro-agrégation pour isoler des bassins régionaux (ex: Mésopotamie, Méditerranée, Mer Jaune) sans consommer de ressources sur les océans mondiaux extérieurs.
