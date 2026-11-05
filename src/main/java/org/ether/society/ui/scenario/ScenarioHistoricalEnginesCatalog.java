/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.List;
import org.ether.society.ui.scenario.ScenarioEngineCatalog.EngineMeta;

/**
 * Sub-catalog of Holocene, Historical, Macroeconomic, Ecological, and Prospective Tier 2
 * Optional Simulation Engines (-6,000 BCE to 2045+ CE).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class ScenarioHistoricalEnginesCatalog {

    private ScenarioHistoricalEnginesCatalog() {
        // Utility catalog class
    }

    /**
     * Returns the immutable list of Holocene, historical, and prospective optional simulation engines.
     *
     * @return list of historical and modern optional {@link EngineMeta} descriptors
     */
    static List<EngineMeta> getHistoricalEngines() {
        return List.of(
            new EngineMeta("AutoRegulationPureEngine", "⚡ Auto-Régulation Pure & Cybernétique (1948)",
                "Moteur de régulation dynamique automatique des équilibres homéostatiques.",
                "Ref: Wiener, N. (1948). Cybernetics: Or Control and Communication in the Animal and the Machine.",
                "• Boucle de Rétroaction : dX/dt = -k · (X - X_cible)"),
            new EngineMeta("FrontierAsabiyyahEngine", "⚔ Asabiyyah de Frontière (Ibn Khaldoun 1377 & Peter Turchin 2003)",
                "Théorie Khaldounienne de la solidarité de groupe et déclin des dynasties (Badiya vs Hadara). Modélise l'érosion de la cohésion sociale lors du passage de la frontière métastable aux métropoles opulentes.",
                "Ref: Ibn Khaldun (1377). Muqaddimah; Turchin, P. (2003). Historical Dynamics: Securing the Peace, Princeton Univ. Press.",
                "• Variation d'Asabiyyah (Cohésion A) : dA/dt = câ‚Â·F(x)Â·(1 - A) - câ‚‚Â·(K(x)/N(x))Â·A\n  où F(x) est la pression militaire de frontière et K(x)/N(x) le capital par habitant (luxe).\n• Métropole opulente (K > 1000 kg/hab) : Déclin d'Asabiyyah dA/dt = -2.0% par pas de temps.\n• Zone de frontière (K â‰¤ 1000 kg/hab) : Forge la cohésion militaire dA/dt = +2.0% par pas de temps.\n• Inégalité & Déclin Dynastique : S_cohesion(t) = A(t) Â· Pop(t) Â· (1 - Gini(t))."),
            new EngineMeta("AiAutonomousRegulationPureEngine", "🤖 Régulation Autonome de l'IA & Gouvernance (2023+)",
                "Modélisation de la régulation et des risques de l'intelligence artificielle. Évalue les probabilités d'émergence d'infrastructures autonomes et de gestion des risques.",
                "Ref: Bostrom, N. (2014). Superintelligence; Russell, S. (2019). Human Compatible.",
                "• Seuil d'Autonomie IA : P_alignement = 1 / (1 + exp(-k · (Niveau_Gouvernance - Complexité_IA)))\n• Taux de Risque Systémique R_ia = (1 - P_alignement) · Puissance_Calcul_Planétaire"),
            new EngineMeta("AmerindianEcosystemEngine", "🌾 Agro-foresterie Amérindienne & Terra Preta (-4 000 BP)",
                "Techniques agricoles précolombiennes et enrichissement des sols en biochar. Augmente la capacité portante et la résilience des sols de forêt tropicale.",
                "Ref: Denevan, W. M. (1992). The Pristine Myth; Glaser, B. et al. (2002). Terra Preta Biochar.",
                "• Formation de Terra Preta : dC_biochar/dt = Apport_Charbon_Organique - Oxidation_Lente(0.001)\n• Gain de Capacité Portante : K_sols = K_base · (1 + α · ln(1 + C_biochar))"),
            new EngineMeta("AsymmetricColonialTradeEngine", "🚢 Commerce Colonial Asymétrique & Extraction (1500)",
                "Flux de ressources et fuite de valeur des colonies vers les métropoles. Simule la capture de rente et le blocage de l'industrialisation périphérique.",
                "Ref: Wallerstein, I. (1974). The Modern World-System; Frank, A. G. (1967). Dependency Theory.",
                "• Capture de Rente : Transfert_Richesse = Termes_Echange_Asym · Export_Matières_Premières\n• Frein d'Industrialisation Périphérique : dT_peripherie/dt = T_base · (1 - Ratio_Extraction)"),
            new EngineMeta("BifurcationChaosEngine", "🌀 Chaos & Analyse des Bifurcations Systémiques (1963)",
                "Sensibilité aux conditions initiales et points de basculement. Génère des micro-oscillations chaotiques pouvant déclencher des cascades d'instabilité.",
                "Ref: Lorenz, E. N. (1963). Deterministic Nonperiodic Flow; May, R. M. (1976).",
                "• Attracteur de Lorenz / Bifurcation Logistique : x_{t+1} = r · x_t · (1 - x_t)\n• Exposant de Liapounov λ > 0 -> Divergence exponentielle des trajectoires de simulation"),
            new EngineMeta("BioMolecularEpidemiologyEngine", "â˜£ï¸ Épidémiologie Bio-Moléculaire & Immunité Pop (-5 000 BP / 1927)",
                "Simulation avancée de la transmission virale et foyers infectieux. Modélise la transmission SIR/SEIR selon la densité urbaine et le réseau de commerce.",
                "Ref: Kermack, W. O. & McKendrick, A. G. (1927). SIR Epidemiological Model.",
                "• Modèle SEIR : dS/dt = -β·S·I/N, dE/dt = β·S·I/N - σ·E, dI/dt = σ·E - γ·I, dR/dt = γ·I\n• Taux de Reproduction de Base R₀ = β / γ · (1 + Variance_Contacts_Densité)"),
            new EngineMeta("CulturalMaterialismPureEngine", "📜 Matérialisme Culturel (Marvin Harris 1979)",
                "Déterminisme de l'infrastructure technologique et démographique sur les croyances. Adapte les valeurs morales aux contraintes d'extraction d'énergie.",
                "Ref: Harris, M. (1979). Cultural Materialism: The Struggle for a Science of Culture.",
                "• Alignement Superstructure : Superstructure(t) = f(Infrastructure_Énergétique, Pression_Démographique)\n• Transition Morale : dM/dt = k_adaptation · (Mode_Production - M)"),
            new EngineMeta("CulturalSociologyEngine", "📜 Sociologie Culturelle & Matrice de Voisinage (-50 000 BP)",
                "Évolution des valeurs culturelles et métissage régional. Gère la diffusion des langues, des normes et la dérive culturelle entre mailles voisines.",
                "Ref: Cavalli-Sforza, L. L. & Feldman, M. W. (1981). Cultural Transmission and Evolution.",
                "• Matrice de Diffusion Culturelle : dC_i/dt = Σ_j w_{ij} · (C_j - C_i) + Drift_Accidentel\n• Distance Culturelle d(i,j) = || Vector_Langue_i - Vector_Langue_j ||"),
            new EngineMeta("DeforestationErosionEngine", "ðŸœï¸ Érosion Forestière & Ensablement Fluvial (-6 000 BP)",
                "Dégradation des sols et perte de couverture végétale. Entraîne le ravinement des terres arables et le comblement des lits de rivières lors de coupes rases.",
                "Ref: Montgomery, D. R. (2007). Dirt: The Erosion of Civilizations. Univ. of California Press.",
                "• Érosion des Sols : Perte_Sol = k_coupe · (1 - Couverture_Forestiere) · Precipitations³\n• Comblement Fluvial = Σ Perte_Sol_Amont"),
            new EngineMeta("EdoJapanIsolationEngine", "â›©ï¸ Isolationnisme du Japon Edo / Sakoku (1635)",
                "Maintien d'un équilibre zéro-croissance et fermeture des frontières. Élimine la dépendance extérieure au détriment du rythme de progrès technologique.",
                "Ref: Totman, C. (1993). Early Modern Japan; Diamond, J. (2005). Collapse (Tokugawa Forestry).",
                "• Équilibre Sylvicole & Zéro-Croissance : Extraction_Bois <= Auto_Régénération_Forêt\n• Isolement Commercial : Flux_Externe = 0, Stabilité_Interne = Maximale"),
            new EngineMeta("EntropicMetalDissipationEngine", "ðŸ­ Dissipation Entropique des Métaux & Jevons Rebound (1800)",
                "Dispersion irrémédiable des métaux rares et effets rebond. Calcule la perte irrécupérable de cuivre et de métaux précieux par usure mécanique et oxydation.",
                "Ref: Georgescu-Roegen, N. (1971). The Entropy Law and the Economic Process.",
                "• Pertes Entropiques Irrécupérables : dMetal_dissipe/dt = Production · (1 - Taux_Recyclage_Max)\n• Limite d'Usure Recyclage : Max_Recyclage = 85% par contrainte thermodynamique"),
            new EngineMeta("EcotoxicologyFertilityEngine", "🧪 Écotoxicologie & Stérilité Chimique (1950)",
                "Impacts des polluants synthétiques sur le taux de fécondité. Simule la baisse de fertilité humaine liée à la concentration cumulée d'entropie chimique.",
                "Ref: Colborn, T. et al. (1996). Our Stolen Future; Swan, S. H. (2021). Count Down.",
                "• Baisse de Fécondité : F_effective = F_naturelle · exp(-k_tox · Charge_Pollution_Cumulee)"),
            new EngineMeta("FertileCrescentSalinizationEngine", "🌾 Salinisation du Croissant Fertile (-2 400 BP)",
                "Dégradation historique des sols irrigués en Mésopotamie antique. Accumulation de sels minéraux toxiques par évaporation de l'eau d'irrigation.",
                "Ref: Jacobsen, T. & Adams, R. M. (1958). Salt and Silt in Ancient Mesopotamian Agriculture.",
                "• Accumulation Saline : dSel/dt = (Volume_Irrigation · Concentration_Sel_Eau) - Leaching_Drainage"),
            new EngineMeta("GeoengineeringAlbedoFeedbackEngine", "🧪 Géo-ingénierie & Rétroaction d'Albédo Artificiel (2030+)",
                "Injections d'aérosols stratosphériques et terraformation. Diminue l'insolation solaire globale pour contrer le réchauffement climatique.",
                "Ref: Crutzen, P. J. (2006). Albedo Modification via Stratospheric Aerosol Injection.",
                "• Delta Albédo Artificiel : ΔAlbédo = k_aerosol · Mass_SO2_Injectee\n• Refroidissement Forcé : ΔT_cooling = -λ · S₀ · ΔAlbédo"),
            new EngineMeta("HandyNasaHybridEngine", "📉 Modèle HANDY NASA Hybride (Démographie & Élites 2014)",
                "Rétroactions entre élites, travailleurs et ressources (NASA / Motesharrei). Simule les scénarios d'effondrement par surconsommation des élites.",
                "Ref: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). HANDY: Human and Nature Dynamics.",
                "• Équations HANDY (4 Variables) : dx_w/dt = α_w·x_w - β_w·x_w,  dx_e/dt = α_e·x_e - β_e·x_e\n• Surconsommation Élites : Consommation_Elite = s · Consommation_Travailleur avec s >> 1"),
            new EngineMeta("HandyNasaPureEngine", "📉 Modèle HANDY NASA Pur (Équations Différentielles 2014)",
                "Système dynamique pur de la dynamique homme-nature (Handy Model). Système à 4 équations couplées (Élites, Travailleurs, Nature, Capital).",
                "Ref: Motesharrei, S. et al. (2014). HANDY Model Equations. Ecological Economics 101.",
                "• Nature N : dN/dt = γ·N·(λ - N) - δ·x_w·N\n• Accumulation de Capital K : dK/dt = δ·x_w·N - C_w - C_e"),
            new EngineMeta("JevonsParadoxEngine", "⚡ Effet Rebond & Paradoxe de Jevons (1865)",
                "L'augmentation de l'efficacité énergétique accroît la consommation globale. Annule les gains d'économie d'énergie par l'expansion de l'échelle industrielle.",
                "Ref: Jevons, W. S. (1865). The Coal Question; Alcott, B. (2005). Jevons' Paradox.",
                "• Énergie Consommée E_total = Pop · Efficacité^ε avec ε > 1.0 (Paradoxe de Jevons)"),
            new EngineMeta("KardashevPureEngine", "🌌 Échelle de Kardashev & Capture Énergétique (1964)",
                "Transition vers le contrôle de l'énergie planétaire intégrale. Évalue le score de Kardashev (Type 0.0 à 1.0) selon la puissance totale captée en watts.",
                "Ref: Kardashev, N. S. (1964). Transmission of Information by Extraterrestrial Civilizations.",
                "• Indice de Kardashev K = (logâ‚â‚€(Puissance_Watts) - 6) / 10\n• Type I = 10Â¹â¶ Watts (Énergie Planétaire Intégrale)"),
            new EngineMeta("KinSelectionHamiltonEngine", "🧬 Sélection de Parentèle (Règle de Hamilton -300 000 BP / 1964)",
                "Évolution de l'altruisme génétique et coopération inter-individus (rB > C). Détermine la cohésion des petites tribus et familles étendues.",
                "Ref: Hamilton, W. D. (1964). The Genetical Evolution of Social Behaviour. J. Theor. Biol.",
                "• Condition d'Altruisme : r · Benefit > Cost avec r = Coefficient d'Apparentement Génétique"),
            new EngineMeta("KurzweilAcceleratingReturnsEngine", "🚀 Loi des Rendements Accélérés (Ray Kurzweil 2005)",
                "Accélération exponentielle du progrès scientifique et des processeurs. Réduit les délais d'invention à mesure que le niveau technologique s'élève.",
                "Ref: Kurzweil, R. (2005). The Singularity Is Near; Moore, G. E. (1965).",
                "• Vitesse d'Invention dTech/dt = V₀ · exp(λ · Tech(t))"),
            new EngineMeta("LenskiPureEngine", "🧠 Évolution Socioculturelle (Gerhard Lenski 1966)",
                "Classification des sociétés selon leur mode d'extraction d'information et d'énergie. Rétrograde ou promeut le type de société (Chasseurs, Agricoles, Industriels).",
                "Ref: Lenski, G. (1966). Power and Privilege: A Theory of Social Stratification.",
                "• Stade Sociétal = f(Énergie_Par_Habitant, Stock_Information_Technologique)"),
            new EngineMeta("LeslieWhitePureEngine", "⚡ Loi de Leslie White (Culture = E × T 1943)",
                "Le développement culturel varie directly avec l'énergie captée par habitant (C = E × T). Détermine la complexité symbolique selon l'énergie.",
                "Ref: White, L. A. (1943). Energy and the Evolution of Culture. American Anthropologist.",
                "• Complexité Culturelle C = Énergie_Par_Capita · Efficacité_Technologique"),
            new EngineMeta("MaritimeHighwayEngine", "⛵ Autoroute Maritime & Thalassocraties (-3 000 BP)",
                "Réseau de navigation côtière et autoroutes maritimes universelles. Réduit la friction de transport sur l'eau libre de glace (thermodynamique) et stimule l'accumulation de capital.",
                "Ref: Braudel, F. (1949). La Méditerranée; Archimedes Buoyancy Transport Models.",
                "• Multiplicateur de Transport Maritime = 0.20 × Friction_Terrestre (Cap : +5%/tick)"),
            new EngineMeta("DynamicMaritimeRoutingGraph", "ðŸŒ Graphe de Routage Trans-Océanique Dynamique (-40 000 BP)",
                "Calcul dynamique et adaptatif des routes maritimes globales selon l'évolution technologique (cabotage -> navigation hauturière -> brise-glace) et thermodynamique.",
                "Ref: Dynamic Graph Routing & Fluid Drag; Bowditch, N. (1802). American Practical Navigator.",
                "• Invalidation auto selon Niveau Tech & Glace de Mer\n• Portée de Cabotage (Tech < 3.0: 300km, Tech >= 6.0: Trans-Océanique)"),
            new EngineMeta("LandReclamationEngine", "ðŸ—ï¸ Poldérisation & Habitats Flottants (1200)",
                "Transformation de zones côtières en polders agricoles et création de cités flottantes en haute mer selon le niveau technologique et le capital.",
                "Ref: Dutch Water Boards History; Seasteading Institute (2008).",
                "• Tech >= 4.0 & K >= 100 -> Poldérisation ; Tech >= 8.5 & K >= 500 -> Habitat Flottant"),
            new EngineMeta("MarineSubmersionEngine", "🚨 Submersion Marine & Évacuation Physique (1950+)",
                "Modélisation physique de l'élévation du niveau de la mer et de la maintenance des digues. Évacuation préventive (jusqu'à 98% en société moderne) et flux de réfugiés sans mortalité brutale irréaliste.",
                "Ref: IPCC Coastal Inundation & Flood Early Warning Physics; Adger, W. N. (2006). Vulnerability.",
                "• Évacuation Physique: Ratio_Evac = 1 - exp(-0.4 · Tech · (1 + K/500))\n• Déplacement de Population vers mailles sèches adjacentes"),
            new EngineMeta("HydrologicalEngineeringEngine", "💧 Ingénierie Hydrologique & Transgressions Paysagères (-3 000 BP)",
                "Assèchement/remplissage de lacs urbains (Texcoco / Tenochtitlan), assèchement anthropique de mers intérieures endoréiques (Mer d'Aral) et retenues d'eau / barrages hydroélectriques de montagne.",
                "Ref: IPCC Water Resource Engineering; World Commission on Dams (2000); Tenochtitlan Chinampa Hydro-Engineering.",
                "• Drenage Texcoco: Lac -> Plaine Urbaine (Tech >= 3.5, K >= 150)\n• Assèchement Aral: Lac -> Désert Salé (Drainage > Recharge)\n• Barrage Montagne: Retenue d'eau (1000L) & Énergie Hydroélectrique (Tech >= 4.5, Alt >= 300m)"),
            new EngineMeta("MegafaunaEcosystemEngine", "🦕 Conservation de la Biodiversité Sauvage & Mégafaune (-50 000 BP)",
                "Pressions de chasse et extinction/préservation des espèces sauvages. Simule la disparition de la grande faune lors de l'expansion humaine néolithique.",
                "Ref: Martin, P. S. (1984). Quaternary Extinctions: A Prehistoric Revolution.",
                "• Extinction Mégafaune dM/dt = r_m·M - k_chasse·Pop_Humaine·M"),
            new EngineMeta("MilitaryTechShockEngine", "💣 Chocs de Technologie Militaire & Poudre à Canon (1200)",
                "Révolution militaire et transformation de l'architecture des fortifs. Augmente la capacité de conquête des empires centralisés.",
                "Ref: Parker, G. (1988). The Military Revolution; McNeill, W. H. (1982).",
                "• Puissance Offensive = Puissance_Base · (1 + Multiplicateur_Poudre_Canon)"),
            new EngineMeta("MonasticDemographicBufferEngine", "ðŸ›ï¸ Buffers Démographiques Monastiques & Savoir (500)",
                "Préservation du capital intellectuel et régulation démographique par les monastères. Empêche la perte totale de savoir lors de la chute d'un empire.",
                "Ref: Weber, M. (1905); Kautsky, K. (1889). Thomas More and his Utopia.",
                "• Plancher de Rétention du Savoir : T_min = Max(T_courant, T_monastique_sauvegardé)"),
            new EngineMeta("NordhausDiceHybridEngine", "ðŸŒ¡ï¸ Modèle DICE Hybride (Nordhaus 1992 - Climat & Économie)",
                "Couplage économie-climat intégré avec boucle de dommage du carbone. Évalue la perte de PIB causée par l'élévation des températures extrêmes.",
                "Ref: Nordhaus, W. D. (1992, 2017). Integrated Assessment Models (DICE-2016R).",
                "• Fonction de Dommage Nordhaus Î©(T) = 1 / (1 + Ï€â‚Â·T + Ï€â‚‚Â·T²)\n• PIB Ajusté Climat Y_net = Î©(T) Â· Y_brut"),
            new EngineMeta("NordhausDicePureEngine", "ðŸŒ¡ï¸ Modèle DICE Pur (Taxe Carbone & PIB 1992)",
                "Modélisation analytique du coût du carbone et investissements verts. Calcule le prix social du carbone pour inciter la décarbonation.",
                "Ref: Nordhaus, W. D. (1992). An Optimal Transition Path for Controlling Greenhouse Gases. Science.",
                "• Prix Social du Carbone SCC = d(Dommages_Futurs_Actualisés) / d(Émission_CO2)"),
            new EngineMeta("NuclearSafetyRadiotoxicityEngine", "âš›ï¸ Radiotoxicité & Fusion Nucléaire (1950)",
                "Gestion des risques d'accidents atomiques et retombées toxiques. Simule la contamination des terres et les surcoûts de sécurité industrielle.",
                "Ref: Perrow, C. (1984). Normal Accidents: Living with High-Risk Technologies.",
                "• Probabilité d'Accident Majeur = 1 - exp(-Taux_Défaillance_Système · Nombre_Reacteurs)"),
            new EngineMeta("NuclearWarfareClimateEngine", "💥 Guerres Thermodynamiques & Hiver Nucléaire (1945)",
                "Modélisation des incendies massifs et refroidissement climatique. Injection de suie stratosphérique bloquant les rayons solaires pendant des années.",
                "Ref: Turco, R. P., Toon, O. B., Ackerman, T. P., Pollack, J. B., & Sagan, C. (1983). TTAPS.",
                "• Baisse Température Mondiale ΔT_nucléaire = -15.0 °C · (Masse_Suie / 150 Tg)"),
            new EngineMeta("OstromCommonsPureEngine", "ðŸžï¸ Auto-Gouvernance des Communs (Elinor Ostrom 1990)",
                "Règles institutionnelles locales pour éviter la tragédie des communs. Maintient la durabilité des pâturages et de la pêche sans privatisation.",
                "Ref: Ostrom, E. (1990). Governing the Commons: Evolution of Institutions.",
                "• Maintien des Communs : Taux_Survie_Communs = f(Institution_Locale, Sanctions_Graduées)"),
            new EngineMeta("PinkerViolenceDeclinePureEngine", "ðŸ•Šï¸ Déclin Historique de la Violence (Steven Pinker 2011)",
                "Baisse de la mortalité violente par l'État, le commerce et l'alphabétisation. Réduit les homicides et les guerres à mesure que l'État de droit progresse.",
                "Ref: Pinker, S. (2011). The Better Angels of Our Nature: Why Violence Has Declined.",
                "• Taux de Mortalité Violente = V₀ · exp(-k_etat · Monopole_Violence - k_commerce · Fret)"),
            new EngineMeta("ProtestantWorkEthicEngine", "âœï¸ Éthique du Travail & Accumulation de Capital (Max Weber 1905)",
                "Impact des valeurs morales sur la formation du capital industriel. Stimule le réinvestissement des bénéfices dans les machines au lieu du luxe.",
                "Ref: Weber, M. (1905). Die protestantische Ethik und der Geist des Kapitalismus.",
                "• Taux d'Épargne Réinvestie : S_épargne = S_base · (1 + α_ethique_travail)"),
            new EngineMeta("PsychohistoryPureEngine", "📊 Psychohistoire Cliodynamique (Modèle d'Asimov 1951)",
                "Prédiction statistique des grandes masses humaines à long terme. Anticipe les cycles de stabilité et prévient les crises d'effondrement.",
                "Ref: Asimov, I. (1951). Foundation; Turchin, P. (2008). Arise Cliodynamics. Nature.",
                "• Trajectoire Macro-Historique : X(t+Δt) = Matrix_P · X(t) avec Invariance Statistique"),
            new EngineMeta("RomanImperialCliodynamicEngine", "🪙 Cycle Cliodynamique Romain & Altération de la Monnaie (-27 / 193)",
                "Dégradation de la pureté du Denier et crises fiscales impériales. Entraîne l'hyper-inflation et le déclin du pouvoir d'achat des légions.",
                "Ref: Turchin, P. & Scheidel, W. (2009). Coin Debasement and Structural Cycles in Rome.",
                "• Pureté de la Monnaie : Teneur_Argent = T₀ · exp(-k_solde_fiscal · Déficit_Armée)\n• Inflation & Solde des Légions : Solde_Reelle = Solde_Nominale / Inflation"),
            new EngineMeta("ScottAgainstTheGrainPureEngine", "🌾 Modèle de l'État Céréalier (James C. Scott -3 500 BP / 2017)",
                "Attractivité des céréales taxables et émergence de l'État archaïque. Privilégie le blé/riz stockables pour l'impôt au détriment des tubercules.",
                "Ref: Scott, J. C. (2017). Against the Grain: A Deep History of the Earliest States.",
                "• Capacité Taxable : Assiette_Fiscale = Production_Céréales_Stockables - Subsistance_Minimale"),
            new EngineMeta("SelfDomesticationEngine", "ðŸ• Auto-Domestication Humaine & Réduction de l'Agressivité (-300 000 BP)",
                "Sélection contre l'agressivité réactive dans les fortes densités. Sélectionne les comportements coopératifs indispensables à la vie urbaine.",
                "Ref: Hare, B. (2017). Survival of the Friendliest; Lahire, B. (2018). Structures Fondamentales.",
                "• Réduction Agressivité Réactive : dAgressivite/dt = -k_urbain · Densité_Cellule"),
            new EngineMeta("SexualSelectionMatingEngine", "ðŸ’ Sélection Sexuelle & Marché Matrimonial (-300 000 BP)",
                "Structures de parenté et polygamie/monogamie selon les ressources. Régule l'accès aux partenaires selon l'inégalité de capital.",
                "Ref: Buss, D. M. (1989). Sex differences in human mate preferences. BBS.",
                "• Indice Polygynie = f(Gini_Richesse, Monopole_Ressources_Elites)"),
            new EngineMeta("SmilMaterialTransitionsPureEngine", "ðŸ—ï¸ Transitions Matérielles & Énergétiques (Vaclav Smil 2019)",
                "Inertie physique des transitions vers l'acier, le béton, l'ammoniac et le plastique. Impose des délais de plusieurs décennies pour remplacer un matériau de base.",
                "Ref: Smil, V. (2019). Material World & Energy Transitions: History, Requirements.",
                "• Temps de Transition Matérielle T_transition = 40 à 60 ans par inertie du capital fixe"),
            new EngineMeta("SpatialCityFractalEngine", "ðŸ™ï¸ Distribution Fractale des Villes (-3 000 BP / 1949)",
                "Hiérarchie des métropoles et loi rang-taille urbaine. Organise le réseau urbain en sous-centres régionaux et métropoles primatiales.",
                "Ref: Batty, M. (2008). The Size, Scale, and Shape of Cities. Science; Zipf, G. K. (1949).",
                "• Loi de Zipf Rang-Taille : Pop(Rang r) = Pop_Max / r^α avec α ≈ 1.0"),
            new EngineMeta("TasmanianCulturalRegressionEngine", "ðŸï¸ Régression Culturelle de Tasmanie (Henrich -10 000 BP)",
                "Perte de technologies complexes par goulet d'étranglement démographique. Fait régresser l'outillage si la population tombe sous le seuil critique d'apprentissage.",
                "Ref: Henrich, J. (2004). Demography and Cultural Loss in Tasmania. American Antiquity.",
                "• Seuil Critique d'Apprentissage : dTech/dt < 0 si Pop_Tribu < N_critique"),
            new EngineMeta("TechnologicalSingularityEngine", "🌌 Singularité Technologique & Kurzweil Rebound (2045+)",
                "Accélération exponentielle du progrès scientifique et IA. Franchit le point d'inflexion où les machines auto-améliorent leur propre conception.",
                "Ref: Good, I. J. (1965); Vinge, V. (1993); Kurzweil, R. (2005). The Singularity Is Near.",
                "• Boucle de Rétroaction Singularité : d(Capacité_IA)/dt = (Capacité_IA)^1.5"),
            new EngineMeta("UrbanThermodynamicsEngine", "ðŸ™ï¸ Thermodynamique Urbaine & Métropoles (1850)",
                "Îlots de chaleur urbains et densité bâtie hyper-concentrée. Élève la température locale des métropoles et consomme de la puissance de climatisation.",
                "Ref: Oke, T. R. (1982). The Energetic Basis of the Urban Heat Island. Q. J. R. Meteorol. Soc.",
                "• Îlot de Chaleur Urbain Î”T_urbain = a Â· logâ‚â‚€(Immobilier_Densité) + b"),
            new EngineMeta("World3CouplingEngine", "📉 Modèle Couplé World3 & Limites à la Croissance (1972)",
                "Rétroactions entre population, pollution et capital (Club de Rome). Connecte le modèle World3 Meadows aux cellules hexagonales H3.",
                "Ref: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). Limits to Growth.",
                "• System Dynamic 5 Subsystems (Population, Capital, Agriculture, Pollution, Non-Renewables)"),
            new EngineMeta("World3HybridEngine", "📉 Modèle World3 Hybride (Physique & Capital 1972)",
                "Couplage de la dynamique de World3 aux variables spatiales H3. Intègre la dispersion spatiale de la pollution et des ressources finies.",
                "Ref: Meadows, D. H. et al. (1972, 2004). Limits to Growth: The 30-Year Update.",
                "• Dispersion Spatiale H3 de la Pollution Persistante"),
            new EngineMeta("World3PureEngine", "📉 Modèle World3 Pur (Équations du Club de Rome 1972)",
                "Reproduction fidèle des 5 sous-systèmes du rapport Meadows 1972 (Population, Capital, Agriculture, Pollution, Ressources).",
                "Ref: Meadows, D. H. et al. (1972). World3 Model Equations (Club of Rome Report).",
                "• Reproduction Intégrale des Équations World3 DYNAMO / Stella"));
    }

    /**
     * Resolves the historical apparition year (BCE/CE) for a Holocene/modern engine, or -100,000L fallback.
     *
     * @param key unique engine identifier
     * @return apparition year in astronomical calendar convention
     */
    static long getHistoricalApparitionYear(String key) {
        return switch (key) {
            case "DeforestationErosionEngine" -> -6000L;
            case "BioMolecularEpidemiologyEngine" -> -5000L;
            case "AmerindianEcosystemEngine" -> -4000L;
            case "ScottAgainstTheGrainPureEngine" -> -3500L;
            case "MaritimeHighwayEngine", "HydrologicalEngineeringEngine", "SpatialCityFractalEngine" -> -3000L;
            case "FertileCrescentSalinizationEngine" -> -2400L;
            case "RomanImperialCliodynamicEngine" -> -27L;
            case "MonasticDemographicBufferEngine" -> 500L;
            case "LandReclamationEngine", "MilitaryTechShockEngine" -> 1200L;
            case "FrontierAsabiyyahEngine" -> 1377L;
            case "AsymmetricColonialTradeEngine" -> 1500L;
            case "EdoJapanIsolationEngine" -> 1635L;
            case "EntropicMetalDissipationEngine" -> 1800L;
            case "UrbanThermodynamicsEngine" -> 1850L;
            case "JevonsParadoxEngine" -> 1865L;
            case "ProtestantWorkEthicEngine" -> 1905L;
            case "LeslieWhitePureEngine" -> 1943L;
            case "NuclearWarfareClimateEngine" -> 1945L;
            case "AutoRegulationPureEngine" -> 1948L;
            case "NuclearSafetyRadiotoxicityEngine", "MarineSubmersionEngine", "EcotoxicologyFertilityEngine" -> 1950L;
            case "PsychohistoryPureEngine" -> 1951L;
            case "BifurcationChaosEngine" -> 1963L;
            case "KardashevPureEngine" -> 1964L;
            case "LenskiPureEngine" -> 1966L;
            case "World3CouplingEngine", "World3HybridEngine", "World3PureEngine" -> 1972L;
            case "CulturalMaterialismPureEngine" -> 1979L;
            case "OstromCommonsPureEngine" -> 1990L;
            case "NordhausDiceHybridEngine", "NordhausDicePureEngine" -> 1992L;
            case "KurzweilAcceleratingReturnsEngine" -> 2005L;
            case "PinkerViolenceDeclinePureEngine" -> 2011L;
            case "HandyNasaHybridEngine", "HandyNasaPureEngine" -> 2014L;
            case "SmilMaterialTransitionsPureEngine" -> 2019L;
            case "AiAutonomousRegulationPureEngine" -> 2023L;
            case "GeoengineeringAlbedoFeedbackEngine" -> 2030L;
            case "TechnologicalSingularityEngine" -> 2045L;
            default -> -100000L;
        };
    }
}
