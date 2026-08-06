# Documentation de l'Ordre des Moteurs & Spécification JIT / Optimisation AST (Ether Engine)

## 1. Vue d'Ensemble & Pipeline Causal d'Exécution

Le moteur de simulation **Ether** orchestre des dizaines de noyaux physiques et cliodynamiques en traitant chaque cellule hexagonale de la grille Uber H3. Afin d'éviter les biais d'intégration numérique et les incohérences causales, les moteurs sont exécutés dans un ordre séquentiel strict et phasé à chaque tick de simulation.

### A. Les 7 Phasing Tiers du Moteur (`H3SimulationEngine`)

```
   ┌─────────────────────────────────────────────────────────────┐
   │ Étape 1 : Insolation, Atmosphère & Radiatif                 │
   │ (RenewableEnergy, AtmosphericOxygen, WetBulb, Albedo)      │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 2 : Sol, Hydrographie & Écosystèmes Terrestres         │
   │ (SoilNutrientsNPK, DeforestationErosion, AquiferDepletion) │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 3 : Métabolisme Démographique & Épidémiologie         │
   │ (BiologicalDemographics, BioMolecularEpidemiology)          │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 4 : Énergie (EROEI), Enthalpie & Recyclage              │
   │ (PhysicalEnergyGrid, NetEnergyEROEI, MetallurgyEnthalpy)     │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 5 : Transport Mécanique, Infrastructures & Conflits   │
   │ (PhysicsTransport, ThermodynamicWarfare, Migration)        │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 6 : Information (Shannon), Arbre Techno & Singularité │
   │ (InformationEntropy, TechnologyDiffusion, Singularity)     │
   └──────────────────────────────┬──────────────────────────────┘
                                  │
   ┌──────────────────────────────▼──────────────────────────────┐
   │ Étape 7 : Couplages Cliodynamiques & Extensions (Type B)     │
   │ (World3Coupling, Kurzweil, ProceduralEngineRegistry)        │
   └─────────────────────────────────────────────────────────────┘
```

---

## 2. Résolution du Problème de Non-Commutativité $f_B(f_A(x)) \neq f_A(f_B(x))$

Lorsque plusieurs moteurs modifient la même variable d'état $x$, l'ordre des opérations influe directement sur le résultat (par exemple, $(x \cdot 5) + 2 \neq (x + 2) \cdot 5$). 

Ether résout ce problème selon 3 axes architecturaux :

1. **Schéma Différentiel Cumulatif (Additive Delta Accumulation) :**
   Plutôt que d'appliquer des mutateurs multiplicatifs directs, chaque moteur calcule un dérivé temporel instantané $\frac{dx_i}{dt} = f_i(x)$. La mise à jour globale s'effectue par intégration vectorielle :
   $$x(t + \Delta t) = x(t) + \left( \sum_{i=1}^{M} \Delta x_i \right) \cdot \Delta t$$
   Comme l'addition vectorielle est commutative ($\Delta x_A + \Delta x_B = \Delta x_B + \Delta x_A$), l'ordre n'introduit aucun biais artificiel.

2. **Double Buffering & Isolement des Voisins :**
   Pour les flux spatiaux (transport, épidémies, chaleur, pollution) qui nécessitent la lecture des 6 cellules adjacentes (`neighborIndexes[cellId][0..5]`), le moteur lit l'état au temps $t$ et écrit dans un tampon temporaire au temps $t + \Delta t$. Cela élimine le risque d'une propagation instantanée séquentielle lors du parcours des cellules.

3. **Multi-Échelle Temporelle :**
   - Échelle Rapide ($\Delta t_{\text{fast}} = 1 \text{ jour}$) pour l'équilibre thermodynamique des prix et flux logistiques (`FluxEngine`).
   - Échelle Lente ($\Delta t_{\text{slow}} = 30 \text{ jours}$) pour la géologie, la biomasse et la démographie.

---

## 3. Architecture du Compilateur JIT Symbolique & Fusion de Kernels (CPU)

### A. Principe de la Compilation au Démarrage du Scénario (PreCompute Phase)

Lors du chargement d'un scénario, la liste des moteurs actifs est figée. Le moteur JIT d'Ether (`ScenarioEngineJITCompiler`) effectue une compilation statique à froid :

1. **Construction du Graphe d'Expressions (AST) :**
   Les équations de chaque moteur procédural sont converties en arbres d'expressions symboliques (`SymbolicExpression`).
2. **Réduction Algébrique & Propagation de Constantes (Constant Folding) :**
   Les chaînes d'opérations sur les variables d'état sont simplifiées sous forme affine canonique :
   $$x_{t+1} = A \cdot x_t + B$$
3. **Fusion de Kernels (Kernel Fusion) :**
   Au lieu d'exécuter 40 boucles distinctes par tick sur les tableaux DOD (`WorldBuffer`), le JIT fusionne toutes les transformations en **une seule boucle vectorisée sur le CPU**, réduisant l'empreinte mémoire d'un facteur 10 à 40x.
4. **Analyse d'Incompatibilité & Détection de Conflits :**
   Le compilateur vérifie statiquement si deux moteurs associés au scénario émettent des gradients strictly opposés sans point d'équilibre (ex: $dO_2/dt > 0$ vs $dO_2/dt < 0$ divergents), permettant d'alerter ou d'élaguer automatiquement les modèles contradictoires.

---

## 4. Bénéfices Technologiques & Liens Utiles

- **Optimisation CPU Pure (Sans GPU Requis) :** Parfaitement adapté aux processeurs multi-cœurs modernes (SIMD / Java Vector API).
- **Déterminisme Total & Mode Strict :** Élimination complète des variations numériques dépendantes de l'ordre d'itération lorsque `strictDeterminism = true`.
- **Fusion de Cellules Océaniques & Approximations :** Présence de 3 options de sur-mesure (macro-agrégation abyssale, restriction navale littorale, ticking multi-fréquence) arbitrant entre vitesse (TPS x4-x8) et distorsion physique. Consulter le document spécialisé : [Optimisations Océaniques, Distorsion Physique & Déterminisme](OCEAN_OPTIMIZATIONS_AND_DETERMINISM.md).
- **Transparence & Diagnostic :** Rapport de compilation détaillant la formule condensée globale et les conflits identifiés.
- 🎮 **Support GPU & Fallback :** Consulter le [Guide d'Intégration GPU](GPU_INTEGRATION_GUIDE.md) pour les détails d'installation OpenCL et TornadoVM.


