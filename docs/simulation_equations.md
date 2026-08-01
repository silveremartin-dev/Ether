# Spécifications Mathématiques & Équations de la Simulation Ether

## 1. Vue d'Ensemble & Architecture Temporelle

La simulation du moteur **Ether** fait évoluer l'état de chaque cellule hexagonale de la grille H3 (passant de l'état $N$ à $N+1$) à l'aide d'un découpage en deux échelles temporelles dynamiques :

- **Échelle Rapide ($\Delta t = 1 \text{ jour} = 86\,400\text{ s}$)** : Calcul des flux thermodynamiques de ressources, transport/logistique et équilibre des prix locaux (`FluxEngine`), ainsi que des réactions politiques à chaud.
- **Échelle Lente (Mise à jour mensuelle / tous les 30 jours)** : Métabolisme démographique des cohortes (`DemographicKernel`), production/régénération écologique (`EnvironmentalKernel`), accumulation du capital, urbanisation et complexité institutionnelle (`UrbanKernel`).

---

## 2. Équations par Domaine Fonctionnel

### A. Flux Thermodynamiques & Prix (`FluxEngine`)

Le moteur de flux modélise l'économie et le transport de ressources comme un système thermodynamique conservatif discrétisé par la méthode des volumes finis.

1. **Calcul du Prix Local (Loi de l'Offre et de la Demande)** :
   $$P_i = \frac{\text{Biomasse Humaine}_i + 1}{\text{Ressource Nourriture}_i + 1}$$

2. **Friction Géographique (Relief et Dénivelé)** :
   $$\text{Friction} = 1.0 + 0.1 \times |h_B - h_A|$$
   $$\sigma = \frac{\sigma_0}{\text{Friction}}$$
   *où $h_A, h_B$ représentent l'altitude des cellules adjacentes $A$ et $B$, et $\sigma_0 = 0.05 \text{ s/m}^2$ la conductivité de base.*

3. **Gradient de Potentiel & Flux Conservatif** :
   $$J_{A \to B} = (P_B - P_A) \times \sigma \times \Delta t$$
   Les ressources se déplacent naturellement vers les zones à plus haut potentiel économique (prix élevés / pénurie), sous réserve de la limitation physique $\min(J, 0.05 \times \text{Nourriture}_A)$.

---

### B. Métabolisme Démographique & Dynamique des Cohortes (`DemographicKernel`)

La population humaine est modélisée sous forme de cohortes d'agents caractérisées par leur masse ($\text{kg}$), énergie ($\text{J}$), âge ($\text{s}$) et marqueurs génético-culturels.

1. **Coût de Structure Allométrique ($\Sigma_{\text{struct}}$)** :
   $$\Sigma_{\text{struct}} = \text{masse}^{1.1} \times 1000\text{ J}$$

2. **Besoin Quotidien & Énergie Interne** :
   $$\text{Besoin Quotidien} = \text{masse} \times 150\,000\text{ J/kg/jour}$$
   $$E_{N+1} = E_N + \text{Nourriture Prise} - \left(\frac{\text{Besoin Quotidien}}{86\,400} + \Sigma_{\text{struct}}\right) \times \Delta t$$

3. **Taux de Natalité / Faim / Fécondité** :
   $$f = (\text{si } E > 100 \text{ alors } 0.05 \text{ sinon } 0.01) \times \left(1 - \frac{\text{masse}}{2000}\right)$$
   $$\Delta \text{Naissances} = \text{masse} \times f \times \Delta t$$

4. **Taux de Mortalité Multi-factoriel** :
   $$m = \left(m_{\text{base}} + \frac{\text{Âge}}{100} + m_{\text{famine}}\right) \times \Delta t$$
   *avec $m_{\text{base}} = 0.02$, et $m_{\text{famine}} = 0.2$ si $E < 0$.*

5. **Mitose / Scission de Cohorte** :
   Lorsque $\text{masse} > 1000\text{ kg}$ et $E > 500\text{ J}$, la cohorte se divise en deux sous-cohortes égales, transmettant sa génétique et sa culture avec un bruit de mutation aléatoire ($\pm 0.05$).

---

### C. Écologie & Production de Ressources (`EnvironmentalKernel`)

1. **Production & Régénération Alimentaire** :
   $$\text{Croissance} = \text{Prod}_{\text{biome}} \times f(T) \times f(R) \times 0.1 \times \Delta t$$
   - **Rendement Biome ($\text{Prod}_{\text{biome}}$)** : Jungle ($100$), Forêt ($80$), Plaines ($60$), Collines ($40$), Plages ($30$), Montagnes ($20$), Océan ($50$), Désert/Toundra ($10$).
   - **Facteur Température $f(T)$** : Optimal ($1.0$) entre $15^\circ\text{C}$ et $25^\circ\text{C}$, décroissant hors de cet intervalle.
   - **Facteur Pluviométrie $f(R)$** : Optimal ($1.0$) entre $200\text{ mm}$ et $1500\text{ mm}$.

2. **Entropie & Décomposition Naturelle** :
   $$\text{Décroissance} = \text{Nourriture}_i \times 0.05 \times \Delta t$$

---

### D. Urbanisation & Théorie des Effondrements de Tainter (`UrbanKernel`)

1. **Complexité Institutionnelle ($C$)** :
   $$C_i = \ln(1 + 0.1 \times \text{Capital}_i)$$

2. **Coût de Maintenance Entropique (Modèle de Joseph Tainter)** :
   $$\Sigma_{\text{maint}} = C_i^{1.15} \times 1000\text{ J}$$

3. **Accumulation Nette de Capital Économique** :
   $$\text{Production Brute} = P_i \times \text{Pop}_i \times 0.1$$
   $$\Delta \text{Capital} = (\text{Production Brute} - \Sigma_{\text{maint}}) \times \Delta t$$

4. **Effondrement & Simplification Forcée** :
   Si $\Sigma_{\text{maint}} > \text{Production Brute}$, la société subit un rendement décroissant des investissements en complexité :
   $$C_i \leftarrow C_i \times 0.99$$
   Entraînant la récession du capital et la fragmentation des structures urbaines.

---

## 3. Implémentation Technologique

Toutes ces équations sont exécutées au sein de noyaux orientés données (**Data-Oriented Design - DOD**), exploitant la **Java Vector API (SIMD)** pour traiter les conteneurs contigus (`WorldBuffer`, `AgentBuffer`) avec des performances maximales.
