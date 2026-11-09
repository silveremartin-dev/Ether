/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.List;

/**
 * Exhaustive Catalog of Simulation Engines for Scenario Setup & Physics Inspection.
 *
 * <p>Decouples Tier 1 permanent biophysical conservation engines from Tier 2
 * pluggable historical and cliodynamic hypotheses, providing state equations,
 * literature citations, and historical apparition thresholds (BP / CE).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioEngineCatalog {

    private ScenarioEngineCatalog() {
        // Utility catalog class
    }

    /**
     * Engine metadata descriptor record.
     *
     * @param id unique Java class identifier
     * @param defaultTitle human-readable title
     * @param defaultDesc detailed biophysical / cliodynamic description
     * @param defaultRef academic citations and literature references
     * @param defaultEq mathematical LaTeX / KaTeX state equations
     */
    public record EngineMeta(String id, String defaultTitle, String defaultDesc, String defaultRef, String defaultEq) {
        public EngineMeta(String id, String defaultTitle, String defaultDesc, String defaultRef) {
            this(id, defaultTitle, defaultDesc, defaultRef, null);
        }
    }

    public static List<EngineMeta> getCoreEngines() {
        return List.of(
            new EngineMeta("PhysicalLawEngine", "Moteur Physique & Lois de Conservation",
                "Moteur de conservation thermodynamique de la matière et de l'énergie (Premier et Second Principes). Calcule le bilan calorifique planétaire et la dégradation de l'énergie en chaleur dissipée.",
                "Ref: Carnot, N. L. S. (1824); Clausius, R. (1865); Prigogine, I. (1977). Non-Equilibrium Thermodynamics.",
                "• Premier Principe (Énergie) : dE_total/dt = Q_in - Q_out = 0\n• Second Principe (Entropie) : dS/dt = dS_ext + dS_int >= 0 avec dS_int = Q_dissipee / Temp_surface"),
            new EngineMeta("BiologicalDemographicsEngine", "Démographie Cellulaire & Métabolisme",
                "Régulation métabolique de la population humaine. Calcule la mortalité de Gompertz-Makeham selon l'âge, l'espérance de vie, la natalité malthusienne et la sous-alimentation.",
                "Ref: Kleiber, M. (1932); Gompertz, B. (1825); Makeham, W. M. (1860); Malthus, T. R. (1798).",
                "• Mortalité Gompertz-Makeham : μ(x) = α · exp(β · x) + γ\n• Bilan Métabolique de Kleiber : B_metabolisme = q₀ · M^(0.75)\n• Dynamique de Population : dN/dt = r · N · (1 - N / K_soutenable)"),
            new EngineMeta("PhysicalEnergyGridEngine", "Grille Énergétique & EROEI Brut",
                "Modélisation des flux d'énergie primaire planétaire (solaire, géothermie, biomasse). Détermine l'EROEI brut (Energy Return on Energy Invested) pour les récoltes et l'extraction.",
                "Ref: Hall, C. A. S., et al. (2014). EROEI of Global Energy Resources. Nature Climate Change.",
                "• EROEI Brut = E_produite_brute / E_investie_extraction\n• Énergie Utile Net = E_brute · (1 - 1 / EROEI)\n• Seuil Critique Civilisationnel : EROEI >= 3.0 requis pour soutenir les institutions."),
            new EngineMeta("PhysicalCapitalDiffusionEngine", "Diffusion Spatiale du Capital & Changement Technique Induit",
                "Diffusion spatiale du capital productif le long des routes commerciales et accumulation endogène sous contrainte de rareté des ressources (Allen 2009 ; Clark 2007). Classificateur passif d'ères physiques sans variable d'état technologique.",
                "Ref: Allen, R. C. (2009). The British Industrial Revolution; Mokyr, J. (1990). The Lever of Riches. Oxford Univ. Press.",
                "• Transfert Spatiale de Capital : Flux_K = 0.01 · Δ(K/N) / Friction · dt\n• Changement Technique Induit : ΔK = 5.0 · Rareté_Biomasse · Potentiel_Substituts · dt"),
            new EngineMeta("AquiferDepletionEngine", "Hydrologie & Transfert d'Eau Douce",
                "Hydrologie continentale et réplétion/déplétion des nappes phréatiques. Simule le bilan précipitations-évapotranspiration, le débit des rivières et le stress hydrique.",
                "Ref: Gleick, P. H. (2000). Water Futures; Wada, Y. et al. (2010). Global Groundwater Depletion. GRL.",
                "• Bilan Hydrique Cellulaire : dW_nappe/dt = Precipitations - Evapotranspiration - Extraction_agricole - Ruissellement\n• Stress Hydrique = Extraction_totale / Recharge_annuelle"),
            new EngineMeta("H3ClimateSystem", "Système Climatique H3 & Saisons",
                "Moteur climato-saisonnier basé sur la discrétisation hexagonale H3. Calcule la température moyenne de surface, le gradient équateur-pôle, l'insolation selon l'obliquité orbitale et les saisons.",
                "Ref: Uber H3 Spatial Index (2018); Sellers, W. D. (1969). Energy Balance Climate Models.",
                "• Bilan Radiatif Solaire : S(lat, t) = (S_const / 4) · [1 + e · cos(ω · t)] · cos(lat - declinaison)\n• Équilibre Thermique : C_thermique · dT/dt = S(1 - Albédo) - ε·σ·T⁴ + Div(K_transport · ∇T)"),
            new EngineMeta("PoliticalSimulationEngine", "Moteur Politique & Frontières",
                "Modélisation des structures politiques et géopolitiques. Gère la délimitation des territoires, la souveraineté des cités-états, les confédérations culturelles et la stabilité des frontières.",
                "Ref: Tilly, C. (1990). Coercion, Capital, and European States; Mann, M. (1986). Sources of Social Power.",
                "• Projection de Puissance d'État : P_militaire(d) = (Capacité_Fiscale · Taux_Levée) / (1 + α · Distance_Capitale)\n• Stabilité des Frontières : Seuil d'annexion si P_i(x) > 1.35 · P_j(x)"),
            new EngineMeta("StatisticsKernel", "Noyau Statistique & Cliodynamique",
                "Noyau d'agrégation statistique et d'analyse cliodynamique en temps réel. Calcule l'indice de Gini, le PIB mondial, la complexité de Turchin, le risque d'effondrement et exporte les bilans CSV.",
                "Ref: Gini, C. (1912); Turchin, P. (2016). Ages of Discord; Tainter, J. (1988). Collapse of Complex Societies.",
                "• Indice de Gini : G = (Σ Σ |y_i - y_j|) / (2 · n² · y_moyen)\n• Pression de Crise Systémique (PSI) = (Pop / Pop_soutenable) · (1 / Salaire_reel) · Inegalité_Elite"),
            new EngineMeta("WorldBuffer / AgentBuffer", "Noyau DOD Allocateur Mémoire",
                "Allocateur de mémoire et registres DOD (Data-Oriented Design). Optimise la mémoire cache du processeur en vectorisant les attributs des cohortes d'agents et des mailles H3.",
                "Ref: Acton, M. (2014). Data-Oriented Design; LMAX Disruptor High-Performance Ring Buffer (2011).",
                "• Structure des Tableaux d'Attributs (SoA) : float[] capitalWork, float[] capitalResource, int[] populationCohorts\n• Alignement Cache SIMD Vectorisé : 64-byte aligned blocks for AVX-512 execution."),
            new EngineMeta("OceanPhysicsEngine", "Dynamo Fluidique & Basculement Océanique",
                "Dynamo fluidique et inertie thermique des océans. Modélise la capacité calorifique de la masse d'eau marine, la dérive thermique lente et la régulation du climat végétal.",
                "Ref: Stommel, H. (1961). Thermohaline Convection; Rahmstorf, S. (1995). AMOC Stability. Nature.",
                "• Modèle à Deux Mailles de Stommel : dq/dt = c_T · ΔT - c_S · ΔS\n• Transport de Chaleur Océanique : F_ocean = ρ · C_p · V_derive · (T_equateur - T_pole)"),
            new EngineMeta("MalthusianCapacityEngine", "Pression Malthusienne & Capacité Portante",
                "Capacité portante écologique (K) et pression Malthusienne. Calcule le seuil maximal d'habitants soutenables par cellule avant dégradation irréversible de l'environnement.",
                "Ref: Malthus, T. R. (1798); Catton, W. R. (1980). Overshoot: Ecological Footprint.",
                "• Capacité Portante K(t) = Fertilité_Sol · Eau_Disponible\n• Ratio Malthusien M = Pop / K\n• Dégradation Environnementale en cas de Surconsommation (M > 1) : dK/dt = -γ · (M - 1) · K"),
            new EngineMeta("OreGradeThermodynamicsEngine", "Géo-Métallurgie & Déplétion Crustale",
                "Géo-métallurgie et thermodynamique d'épuisement des filons minéraux crustaux. Simule le déclin du titre des minerais (Loi de Lasky) et la hausse de l'énergie nécessaire à l'extraction.",
                "Ref: Lasky, S. G. (1950). Mineral Resource Depletion Law; Ayres, R. U. (1998). Industrial Ecology.",
                "• Loi de Lasky (Titre du Minerai) : Grade(g) = g₀ · exp(-k · Cumul_Extrait)\n• Énergie Spécifique d'Extraction : E_extraction(g) = E₀ / (Grade(g))^1.35"),
            new EngineMeta("SoilNutrientNPKEngine", "Cycle NPK & Fertilité des Sols",
                "Cycles biogéochimiques des nutriments NPK (Azote, Phosphore, Potassium). Régit l'épuisement des sols agricoles par la culture intensive et la restauration organique.",
                "Ref: Liebig, J. von (1840). Law of the Minimum; Smil, V. (2001). Enriching the Earth (N-P-K Cycles).",
                "• Loi du Minimum de Liebig : Rendement = Y_max · min( N/N_ref, P/P_ref, K/K_ref )\n• Épuisement des Nutriment : dN/dt = Restauration_Naturelle + Apport_Engrais - Export_Recolte"),
            new EngineMeta("FluxEngine", "Flux de Subsistance & Routes Commerciales",
                "Routes commerciales et flux de subsistance inter-cellules H3. Calcule les coûts de transport, l'arbitrage marchand et l'équilibrage des stocks alimentaires par le commerce.",
                "Ref: Tinbergen, J. (1962). Gravity Model of Trade; Onsager, L. (1931). Reciprocal Relations.",
                "• Modèle Gravitationnel de Commerce : Flux(i, j) = G · (PIB_i · PIB_j) / (Distance(i, j)^1.8)\n• Friction de Transport : Coût_fret = Exp(Friction_Relief · Distance)"),
            new EngineMeta("AtmosphericOxygenEngine", "Dynamique de l'Oxygène Atmosphérique",
                "Bilan de la pression partielle d'oxygène (O₂) atmosphérique. Régule les conditions métaboliques pour la survie des organismes complexes et le risque d'incendies forestiers.",
                "Ref: Berner, R. A. (2006). GEOCARBSULF: Atmospheric Oxygen over Phanerozoic Time.",
                "• Bilan d'O₂ Atmosphérique : dO₂/dt = Photosynthese_Net - Respiration_Biomasse - Oxydation_Minérale\n• Risque d'Embrasement Sauvage = Max(0, (pO₂ - 0.15) / 0.06)"),
            new EngineMeta("CrustalGeothermalEngine", "Géothermie Crustale & Tectonique",
                "Flux de chaleur interne terrestre et potentiel géothermique crustal. Simule le gradient géothermique et le potentiel d'énergie géothermique de surface.",
                "Ref: Turcotte, D. L. & Schubert, G. (2002). Geodynamics: Mantle Heat Transport. Cambridge Univ. Press.",
                "• Conductivité Thermique Crustale : q = -k_roche · (dT/dz)\n• Potentiel Géothermique d'Exploitation : P_geoth = q_surface · Surface_H3 · Rendement_Carnot"),
            new EngineMeta("DynamicHydrographicSiltationEngine", "Hydrographie & Ensablement Fluvial",
                "Hydrographie et dynamique d'érosion/ensablement des bassins versants. Modélise la modification du lit des fleuves et le dépôt d'alluvions fertiles.",
                "Ref: Horton, R. E. (1945). Drainage-Basin Development; Schumm, S. A. (1977). The Fluvial System.",
                "• Équation d'Érosion Fluviale : E_sediment = k_erodibilite · (Débit_eau)^1.4 · (Pente_terrain)^1.2\n• Dépôt Alluvionnaire : dSilt/dt = E_amont - Sedimentation_loc_lit"),
            new EngineMeta("GreenhouseRadiativeEngine", "Forçage Radiatif & Effet de Serre",
                "Bilan de forçage radiatif et effet de serre. Calcule l'impact des concentrations de CO₂, CH₄ et H₂O sur l'infrarouge réémis vers la surface.",
                "Ref: Myhre, G. et al. (1998). Radiative Forcing Equations; IPCC AR6 WG1 (2021).",
                "• Forçage Radiatif du CO₂ : ΔF_CO2 = 5.35 · ln(C / C₀)  [W/m²]\n• Sensibilité Climatique : ΔT_eq = λ · (ΔF_CO2 + ΔF_CH4 + ΔF_aerosols)"),
            new EngineMeta("InfrastructureEnergyEngine", "Réseaux d'Infrastructure Énergétique",
                "Réseaux d'infrastructures énergétiques et transport de puissance. Modélise la perte en ligne des réseaux électriques et oléoducs.",
                "Ref: Bak, P. et al. (1987). Self-Organized Criticality in Grid Infrastructure.",
                "• Perte en Ligne Électrique : Perte_Joule = R_cable · I² · Distance\n• Capacité Maximale de Transit : Cap_max = V_reseau · I_max_thermique"),
            new EngineMeta("NetEnergyEROEIEngine", "EROEI Net & Rendement Énergétique",
                "Calcul du rendement énergétique net (EROEI Net civilisationnel). Évalue la fraction d'énergie réinvestie dans l'extraction par rapport à l'énergie utilisable pour la société.",
                "Ref: Hall, C. A. S. & Klitgaard, K. A. (2018). Energy and the Wealth of Nations. Springer.",
                "• Fraction d'Énergie Réinvestie : F_invest = 1 / EROEI_systemique\n• Énergie Nette Utile Société = Énergie_Totale · (1 - F_invest)"),
            new EngineMeta("PermafrostThawEngine", "Dégel du Permafrost & Relargage Méthane",
                "Dynamique de fonte du cryosol (Permafrost). Simule la déstabilisation des sols gelés et le relargage rétroactif de méthane et CO₂ stratosphériques.",
                "Ref: Schuur, E. A. G. et al. (2015). Vulnerability of Permafrost Carbon to Climate Change. Nature.",
                "• Fonte du Cryosol : dV_gel/dt = -α · Max(0, T_surface - 0.0 °C)\n• Émission Rétroactive CH₄/CO₂ : Emiss_gaz = Stock_carbone_degele · K_microbien(T)"),
            new EngineMeta("PhysicsTransportEngine", "Frictions Thermodynamiques de Transport",
                "Coûts énergétiques et frictions thermodynamiques des transports. Calcule l'énergie consommée par tonne-kilomètre selon le relief et le mode de transport.",
                "Ref: Smil, V. (2017). Energy and Civilization: A History. MIT Press.",
                "• Énergie Spécifique par Mode (MJ/t-km) : Maritime=0.15, Ferroviaire=0.35, Route=2.5, Porteur=12.0\n• Friction du Relief : Coût_total = E_specifique · (1 + k_pente · Pente_moyenne) · Distance"),
            new EngineMeta("ThermohalineStommelAMOCEngine", "Circulation Thermohaline Océanique",
                "Circulation thermohaline globale (Boucle AMOC). Modélise la plongée des eaux salées froides en Atlantique Nord et la redistribution de la chaleur planétaire.",
                "Ref: Broecker, W. S. (1991). The Great Ocean Conveyor; Rahmstorf, S. (2002). Ocean Circulation. Nature.",
                "• Débit AMOC Q_amoc = k_thermo · (ρ_nord - ρ_equateur)\n• Point de Basculement Salin : Si Dilution_Eau_Douce > Seuil_Critique -> Effondrement AMOC (Q -> 0)"),
            new EngineMeta("TrophicEcosystemEngine", "Réseau Trophique & Écosystèmes",
                "Réseau trophique et dynamique des écosystèmes fauniques. Simule les équations de Lotka-Volterra entre prédateurs, herbivores et producteurs primaires.",
                "Ref: Lotka, A. J. (1925); Volterra, V. (1926); MacArthur, R. H. & Wilson, E. O. (1967).",
                "• Dynamique Herbivores H : dH/dt = r_h · H · (1 - H/K) - a · H · P\n• Dynamique Prédateurs P : dP/dt = b · a · H · P - m_p · P"),
            new EngineMeta("MegafaunaEcosystemEngine", "Dynamique Trophique de la Mégafaune Sauvage",
                "Dynamique des populations de grands vertébrés sauvages et pression de prélèvement trophique selon les lois allométriques de Kleiber et Lotka-Volterra.",
                "Ref: Martin, P. S. (1984). Quaternary Extinctions; Sandom, C. et al. (2014). Proc. R. Soc. B.",
                "• Biomasse Mégafaune dM/dt = r_m · M · (1 - M/K_m) - k_chasse · Pop · M"),
            new EngineMeta("DeforestationErosionEngine", "Couverture Forestière & Érosion des Bassins Versants",
                "Bilan de biomasse sylvicole, perte de couverture végétale et ravinement des sols arables sous l'effet du ruissellement pluvial.",
                "Ref: Montgomery, D. R. (2007). Dirt: The Erosion of Civilizations. Univ. of California Press.",
                "• Érosion des Sols : Perte_Sol = k_coupe · (1 - Couverture_Forestière) · Précipitations^1.4"),
            new EngineMeta("BioMolecularEpidemiologyEngine", "Épidémiologie Bio-Moléculaire Cellulaire (SIR/SEIR)",
                "Dynamique endémique locale de transmission infectieuse SIR/SEIR en fonction de la densité de population, de la température et de l'état nutritionnel.",
                "Ref: Kermack, W. O. & McKendrick, A. G. (1927). A Contribution to the Mathematical Theory of Epidemics.",
                "• Modèle SEIR : dS/dt = -β·S·I/N, dE/dt = β·S·I/N - σ·E, dI/dt = σ·E - γ·I"),
            new EngineMeta("EntropicMetalDissipationEngine", "Dissipation Entropique & Oxydation des Métaux",
                "Usure mécanique, corrosion chimique et dispersion entropique irréversible des stocks de métaux affinés (Second Principe appliqué à la matière).",
                "Ref: Georgescu-Roegen, N. (1971). The Entropy Law and the Economic Process. Harvard Univ. Press.",
                "• Pertes Entropiques Irrécupérables : dMetal_dissipé/dt = Stock_Métal · δ_corrosion"));
    }

    /**
     * Returns immutable list of optional and pluggable Tier 2 simulation engines.
     *
     * @return list of optional engines
     */
    public static List<EngineMeta> getOptionalEngines() {
        return ScenarioOptionalEnginesCatalog.getOptionalEngines();
    }

    /**
     * Returns the approximate historical emergence year for a given engine key.
     *
     * @param key engine identifier key
     * @return emergence year in BCE/CE
     */
    public static long getEngineApparitionYear(String key) {
        return ScenarioOptionalEnginesCatalog.getEngineApparitionYear(key);
    }
}
