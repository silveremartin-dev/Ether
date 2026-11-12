/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.List;
import org.ether.society.ui.scenario.ScenarioEngineRegistry.EngineMeta;

/**
 * Canonical catalog of all 86 verified Tier 2 Type B optional simulation engines.
 *
 * <p>Every entry in this catalog corresponds 1-to-1 to an executable Java class in
 * {@code org.ether.society.engines} and is governed by explicit Domain Validity Conditions
 * in {@link org.ether.society.model.AutoEpochScenarioHelper}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class ScenarioHistoricalEnginesRegistry {

    private ScenarioHistoricalEnginesRegistry() {
        // Utility catalog class
    }

    /**
     * Returns the immutable list of all 86 verified optional Tier 2 Type B simulation engines.
     *
     * @return list of verified optional {@link EngineMeta} descriptors
     */
    static List<EngineMeta> getHistoricalEngines() {
        return List.of(
            // =========================================================================
            // 1. DEEP-TIME BIOCULTURAL, EVOLUTIONARY & FORAGER-MOBILITY ENGINES (8)
            // =========================================================================
            new EngineMeta("SelfDomesticationEngine", "🐕 Auto-Domestication Humaine & Réduction de l'Agressivité (-3 000 000 BP)",
                "Sélection contre l'agressivité réactive dans les fortes densités. Sélectionne les comportements coopératifs indispensables à la vie sociale et urbaine.",
                "Ref: Hare, B. (2017). Survival of the Friendliest; Wrangham, R. (2019). The Goodness Paradox.",
                "• Réduction Agressivité Réactive : dAgressivité/dt = -k_urbain · Densité_Cellule"),
            new EngineMeta("SexualSelectionMatingEngine", "💑 Sélection Sexuelle & Marché Matrimonial (-3 000 000 BP)",
                "Structures de parenté et polygamie/monogamie selon les ressources. Régule l'accès aux partenaires selon l'inégalité de capital.",
                "Ref: Buss, D. M. (1989). Sex differences in human mate preferences. BBS; Henrich, J. (2012).",
                "• Indice Polygynie = f(Gini_Richesse, Monopole_Ressources_Élites)"),
            new EngineMeta("KinSelectionHamiltonEngine", "🧬 Sélection de Parentèle (Règle de Hamilton -3 000 000 BP / 1964)",
                "Évolution de l'altruisme génétique et coopération inter-individus (rB > C). Détermine la cohésion des petites tribus et familles étendues.",
                "Ref: Hamilton, W. D. (1964). The Genetical Evolution of Social Behaviour. J. Theor. Biol.",
                "• Condition d'Altruisme : r · Benefit > Cost avec r = Coefficient d'Apparentement Génétique"),
            new EngineMeta("PriceMultilevelSelectionEngine", "🧬 Sélection Multi-Niveau & Altruisme de Groupe (-3 000 000 BP / Price 1970)",
                "Équation de Price décomposant l'évolution des comportements prosociaux : la sélection intra-groupe favorise les égoïstes, mais la sélection inter-groupes élimine les sociétés divisées.",
                "Ref: Price, G. R. (1970). Selection and Covariance. Nature; Bowles & Gintis (2011).",
                "• Équation de Price : Δz_moyen = (1/w) · Cov(w_g, z_g) + (1/w) · E[w_g · Δz_g]"),
            new EngineMeta("LanguageLinguisticEngine", "🗣️ Évolution Linguistique & Divergence Dialectale (-300 000 BP)",
                "Matrice de glottochronologie et dérive des langues selon la distance géographique et les barrières orographiques. Régit l'intercompréhension et les frictions d'intégration.",
                "Ref: Swadesh, M. (1952); Cavalli-Sforza, L. L. (1994). History and Geography of Human Genes.",
                "• Taux de Rétention du Vocabulaire R = exp(-c_swadesh · t)\n• Friction Linguistique = 1.0 - Similarité_Vocabulaire"),
            new EngineMeta("CulturalSociologyEngine", "📜 Sociologie Culturelle & Matrice de Voisinage (-300 000 BP)",
                "Évolution des valeurs culturelles et métissage régional. Gère la diffusion des langues, des normes et la dérive culturelle entre mailles voisines.",
                "Ref: Cavalli-Sforza, L. L. & Feldman, M. W. (1981). Cultural Transmission and Evolution.",
                "• Matrice de Diffusion Culturelle : dC_i/dt = Σ_j w_{ij} · (C_j - C_i) + Drift_Accidentel\n• Distance Culturelle d(i,j) = || Vector_Langue_i - Vector_Langue_j ||"),
            new EngineMeta("DynamicMaritimeRoutingGraph", "🌊 Graphe de Routage Trans-Océanique Dynamique (-60 000 BP)",
                "Calcul dynamique et adaptatif des routes maritimes globales selon l'évolution du capital naval et des conditions thermodynamiques (glace de mer).",
                "Ref: Dynamic Graph Routing & Fluid Drag; Bowditch, N. (1802). American Practical Navigator.",
                "• Invalidation auto selon Capital Naval & Glace de Mer\n• Portée de Cabotage (k_pc < 150: 300km, k_pc >= 1200: Trans-Océanique)"),
            new EngineMeta("TasmanianCulturalRegressionEngine", "🏝️ Culture Cumulative & Régression Démographique de Tasmanie (-3 000 000 BP / Henrich 2004)",
                "Modèle du Tapis Roulant de Henrich : le maintien des savoir-faire dépend de la taille du réseau social interconnecté (N_eff). Fait régresser l'outillage sous un goulet d'étranglement démographique.",
                "Ref: Henrich, J. (2004). Demography and Cultural Loss in Tasmania. American Antiquity 69(2):197-214.",
                "• Seuil Critique d'Apprentissage : Δz = -2 · α · z + β · ε · ln(N_eff)"),

            // =========================================================================
            // 2. NEOLITHIC, BRONZE AGE & CLASSICAL AGRARIAN-STATE ENGINES (24)
            // =========================================================================
            new EngineMeta("BoserupAgriculturalIntensificationEngine", "🌾 Intensification Agricole Endogène (Ester Boserup 1965)",
                "Réfutation de la fatalité malthusienne : la pression démographique force l'adoption de techniques agricoles plus intensives (jachère longue -> jachère courte -> cultures annuelles -> multi-croissance irriguée).",
                "Ref: Boserup, E. (1965). The Conditions of Agricultural Growth. Allen & Unwin.",
                "• Fréquence des Récoltes : F = F₀ · (Pop / Arable)^(-γ)\n• Rendement Involution : Y = Y₀ · (L_travail / Arable)^0.65"),
            new EngineMeta("ScottAgainstTheGrainPureEngine", "🌾 Modèle de l'État Céréalier (James C. Scott -3 500 BP / 2017)",
                "Attractivité des céréales taxables et émergence de l'État archaïque. Privilégie le blé/riz stockables pour l'impôt au détriment des tubercules.",
                "Ref: Scott, J. C. (2017). Against the Grain: A Deep History of the Earliest States.",
                "• Capacité Taxable : Assiette_Fiscale = Production_Céréales_Stockables - Subsistance_Minimale"),
            new EngineMeta("DraftAnimalFodderAllocationEngine", "🐂 Traction Animale & Allocation du Fourrage (-3 000 BP)",
                "Arbitrage biophysique entre biomasse alimentaire humaine et fourrage réservé aux bêtes de trait (bœufs, chevaux). Démultiplie la force de labour au prix d'une surface cultivable préemptée.",
                "Ref: Sherratt, A. (1981). The Secondary Products Revolution; Smil, V. (2008). Energy in Nature and Society.",
                "• Surface Fourrage = N_bétail · Besoin_Fourrage_Journalier / Rendement_Herbe\n• Multiplicateur de Traction : Force_Agricole = 1.0 + β_trait · (N_bétail / Pop_Humaine)"),
            new EngineMeta("TradeNetworkEngine", "🌐 Réseau Commercial Spatial & Arbitrage de Prix (-3 000 BP)",
                "Réseau de commerce dynamique gravitationnel inter-cellules H3. Évalue les surplus/déficits locaux de grains et métaux et déploie des caravanes marchandes selon le gradient de prix.",
                "Ref: Tinbergen, J. (1962). Shaping the World Economy; Krugman, P. (1991).",
                "• Flux Marchand Flux(i,j) = G · (PIB_i · PIB_j) / (Distance_{ij}^1.8)\n• Arbitrage Bénéfice = (Prix_j - Prix_i) - Coût_Transport"),
            new EngineMeta("WarDiplomacyEngine", "⚔️ Guerre Inter-Polities & Friction Clausewitzienne (-3 000 BP)",
                "Projection de force militaire et résolution des conflits inter-étatiques. Détermine les attritions de siège, les coûts logistiques de campagne et les annexions territoriales.",
                "Ref: Clausewitz, C. von (1832). Vom Kriege; Lanchester, F. W. (1916). Aircraft in Warfare.",
                "• Lois de Lanchester (Attrition) : dN_A/dt = -β · N_B\n• Déperdition Logistique : Puissance_Projetée = Puissance_Base · exp(-k_dist · Distance)"),
            new EngineMeta("ThermodynamicWarfareEngine", "🛡️ Guerre Cinétique & Dissipation Thermodynamique des Conflits (-3 000 BP)",
                "Dissipation exergétique des conflits armés, destruction du capital physique bâti et mortalité cinétique localisée sur les théâtres d'opérations.",
                "Ref: Lanchester, F. W. (1916); Richardson, L. F. (1960). Statistics of Deadly Quarrels.",
                "• Destruction de Capital : dK/dt = -γ_war · E_cinétique\n• Attrition Démographique : dN/dt = -μ_combat · Intensité_Conflit"),
            new EngineMeta("MaritimeHighwayEngine", "⛵ Autoroute Maritime & Thalassocraties (-3 000 BP)",
                "Réseau de navigation côtière et autoroutes maritimes universelles. Réduit la friction de transport sur l'eau libre de glace et stimule l'accumulation de capital.",
                "Ref: Braudel, F. (1949). La Méditerranée; Archimedes Buoyancy Transport Models.",
                "• Multiplicateur de Transport Maritime = 0.20 × Friction_Terrestre (Cap : +5%/tick)"),
            new EngineMeta("HydrologicalEngineeringEngine", "💧 Ingénierie Hydrologique & Transgressions Paysagères (-3 000 BP)",
                "Assèchement/remplissage de lacs urbains (Texcoco / Tenochtitlan), assèchement anthropique de mers intérieures endoréiques (Mer d'Aral) et retenues d'eau / barrages de montagne.",
                "Ref: Wittfogel, K. A. (1957). Oriental Despotism; World Commission on Dams (2000).",
                "• Drainage Lacustre & Canaux : Irrigation fluviale et retenues hydrauliques selon le capital par habitant"),
            new EngineMeta("SoilSalinizationHydrologyEngine", "🧂 Salinisation & Hydrogéologie d'Irrigation Fluviale (-2 400 BP)",
                "Bilan hydro-minéral des périmètres irrigués sous climat aride (Mésopotamie antique, Indus). La remontée de la nappe phréatique dépose des sels toxiques par capillarité et évaporation.",
                "Ref: Jacobsen & Adams (1958). Salt and Silt in Ancient Mesopotamian Agriculture. Science.",
                "• Accumulation Saline : dSel/dt = Q_irrigation · [Sel]_eau - Drainage · [Sel]_lessivé\n• Perte Rendement Céréalier : Y = Y₀ · exp(-k_sel · Concentration_Sel_Sol)"),
            new EngineMeta("SpatialCityFractalEngine", "🏙️ Distribution Fractale des Villes (-3 000 BP / 1949)",
                "Hiérarchie des métropoles et loi rang-taille urbaine. Organise le réseau urbain en sous-centres régionaux et métropoles primatiales.",
                "Ref: Batty, M. (2008). The Size, Scale, and Shape of Cities. Science; Zipf, G. K. (1949).",
                "• Loi de Zipf Rang-Taille : Pop(Rang r) = Pop_Max / r^α avec α ≈ 1.0"),
            new EngineMeta("VonThunenAgriculturalRingsEngine", "🌻 Anneaux Agricoles & Rente Foncière Spatiale (Johann von Thünen 1826)",
                "Organisation concentrique des ceintures d'usage des sols autour des marchés urbains : maraîchage périmétropolitain, puis sylviculture/bois de feu, céréales intensives, élevage extensif.",
                "Ref: von Thünen, J. H. (1826). Der isolierte Staat in Beziehung auf Landwirtschaft und Nationalökonomie.",
                "• Rente Foncière R = Y · (p - c) - Y · f · d\n• Allocation du Sol = Argmax_usage(Rente(usage, distance))"),
            new EngineMeta("WestBettencourtAllometryEngine", "📐 Lois d'Échelle Allométrique Urbaine (West & Bettencourt 2007)",
                "Lois d'échelle universelles des villes : les métriques socio-économiques croissent de façon super-linéaire en N^1.15, tandis que les infrastructures croissent de façon sous-linéaire en N^0.85.",
                "Ref: Bettencourt, L. M. A. et al. (2007). Growth, Innovation, Scaling. PNAS; West, G. (2017). Scale.",
                "• Production Socio-Économique : Y_socio = Y₀ · Pop^{1.15}\n• Réseau Infrastructure : L_infra = L₀ · Pop^{0.85}"),
            new EngineMeta("TurchinGoldstoneSDTEngine", "⚖️ Modèle SDT Séculaire & Surproduction des Élites (Peter Turchin 2016)",
                "Modélisation complète des trois composantes de la Théorie Démo-Structurelle : Mass Mobilization Potential (MMP), Elite Mobilization Potential (EMP) et State Fiscal Distress (SFD).",
                "Ref: Turchin, P. (2016). Ages of Discord; Goldstone, J. A. (1991).",
                "• PSI = MMP · EMP · SFD\n• EMP = (Nombre_Élites / Pop) · (Richesse_Élite / Salaire_Ouvrier)\n• SFD = Dette_Publique / Recettes_Fiscales"),
            new EngineMeta("StructuralDemographicBifurcationEngine", "💥 Bifurcation Démo-Structurelle & Crises Majeures (Jack Goldstone 1991)",
                "Calcul du point de basculement vers l'effondrement ou la révolution civile par accumulation conjointe de la pression populaire, de l'encombrement des élites et de la faillite de l'État.",
                "Ref: Goldstone, J. A. (1991). Revolution and Rebellion in the Early Modern World.",
                "• Potentiel de Crise Démo-Structurelle PSI = MMP · EMP · SFD\n• Déclencheur Révolte si PSI > Seuil_Critique"),
            new EngineMeta("TainterComplexityCollapseEngine", "🏛️ Rendements Décroissants de la Complexité Organisationnelle (Joseph Tainter 1988)",
                "Modèle d'effondrement des sociétés complexes : chaque nouvelle crise est résolue en ajoutant une couche bureaucratique coûteuse, jusqu'à ce que le rendement marginal de la complexité devienne négatif.",
                "Ref: Tainter, J. A. (1988). The Collapse of Complex Societies. Cambridge Univ. Press.",
                "• Rendement Marginal MR = dB/dC = B₀ / (1 + exp(α·C)) - κ · C^1.15\n• Effondrement Brutal lors d'un Choc Externe si MR <= 0"),
            new EngineMeta("CliodynamicsEngine", "⚔️ Cliodynamique Séculaire & Asabiyyah Globale (Peter Turchin 2003)",
                "Dynamique macro-historique des cycles séculaires intégrant la solidarité collective d'Ibn Khaldoun et la mortalité de guerre civile proportionnelle à l'indice de crise politique.",
                "Ref: Turchin, P. (2003). Historical Dynamics: Why States Rise and Fall. Princeton Univ. Press.",
                "• Cohésion dS/dt = r_front · (1 - S) · Frontier - δ_core · S · Core\n• Mortalité de Guerre Civile : μ_strife = μ₀ · PSI² · Δt"),
            new EngineMeta("ArthurCombinatorialTechnologyEngine", "⚙️ Évolution Combinatoire des Technologies (W. Brian Arthur 2009)",
                "Accumulation de capital par recombinaison récursive de technologies antérieures. Les innovations majeures émergent de la rencontre et du réassemblage de composants élémentaires existants.",
                "Ref: Arthur, W. B. (2009). The Nature of Technology: What It Is and How It Evolves.",
                "• Synthèse Combinatoire : dK/dt = μ_comb · Pop · (1 + k_pc/120)^1.35 · ln(1 + Pop/100) - δ_usure · K"),
            new EngineMeta("GranovetterThresholdCascadeEngine", "👥 Cascades de Seuil d'Action Collective (Mark Granovetter 1978)",
                "Émergence non-linéaire des révoltes et mouvements sociaux selon une distribution hétérogène de tolérance au risque individuel.",
                "Ref: Granovetter, M. (1978). Threshold Models of Collective Behavior. Am. J. Sociol.",
                "• Fraction Active : f_active(t+1) = ∫_0^{f_active(t)} N(μ_grief, σ²) dθ"),
            new EngineMeta("SchellingAxelrodSegregationEngine", "🏘️ Ségrégation Spatiale Spontanée & Homophilie (Thomas Schelling 1971)",
                "Modèle de micromotives and macrobehavior : même avec une faible préférence pour des voisins de même trait culturel, l'espace urbain se segmente spontanément.",
                "Ref: Schelling, T. C. (1971). Dynamic Models of Segregation; Axelrod, R. (1997).",
                "• Déménagement si Voisins_Similaires / Voisins_Totaux < Seuil_Tolérance"),
            new EngineMeta("SpatialMetapopulationSEIREngine", "☣️ Métapopulation Épidémique Spatialisée SEIR (2006)",
                "Propagation multiscalaire des agents pathogènes le long des corridors commerciaux et réseaux de transport à maillage hexagonal.",
                "Ref: Balcan, D. et al. (2009). Multiscale Mobility Networks and Spreading. PNAS.",
                "• Transmission Corridors : ΔI_i = β_i · S_i · I_i/N_i + Σ_{j} Flux_{ji} · (I_j / N_j) · S_i"),
            new EngineMeta("HarrisCulturalMaterialismEngine", "🥩 Matérialisme Culturel & Primauté de l'Infrastructure (Marvin Harris 1979)",
                "Primauté causale de l'infrastructure techno-environnementale sur la structure politique et la superstructure idéologique. Les déficits caloriques forcent l'effondrement des structures politiques.",
                "Ref: Harris, M. (1979). Cultural Materialism: The Struggle for a Science of Culture. Random House.",
                "• Primauté Causale : Infrastructure -> Structure -> Superstructure\n• Effondrement Structurel si Nourriture/Pop < 0.05 : Décroissance Capital -10%/an"),
            new EngineMeta("LenskiEcologicalEvolutionaryEngine", "📊 Stratification Écologique-Évolutive & Courbe de Gini (Gerhard Lenski 1966)",
                "Loi de Lenski : les inégalités économiques (Gini) sont fonction du mode technologique de production, culminant dans les empires agraires avant de se stabiliser sous le capitalisme industriel.",
                "Ref: Lenski, G. (1966). Power and Privilege: A Theory of Social Stratification. McGraw-Hill; Milanovic (2011).",
                "• Courbe de Gini G*(k_pc) = G_forager + ΔG_agr · exp(-[ln(k_pc/k_agr)]^2/(2σ^2)) + ΔG_ind · k_pc^2/(k_pc^2 + k_ind^2)"),
            new EngineMeta("WhiteEnergyEvolutionEngine", "⚡ Loi d'Évolution Énergétique Culturelle (Leslie White 1949)",
                "Loi de White : la complexité culturelle et le développement sociétal progressent proportionnellement à la quantité d'exergie captée par habitant multipliée par l'efficacité des transducteurs d'énergie (C = E · T).",
                "Ref: White, L. A. (1949). The Science of Culture. Farrar, Straus and Giroux.",
                "• Complexité Culturelle C = E · T\n• Approfondissement du Capital Actif = f(Exergie_Par_Habitant) · Efficacité"),
            new EngineMeta("FrontierAsabiyyahEngine", "⚔️ Asabiyyah de Frontière (Ibn Khaldoun 1377 & Peter Turchin 2003)",
                "Théorie Khaldounienne de la solidarité de groupe et déclin des dynasties (Badiya vs Hadara). Modélise l'érosion de la cohésion sociale lors du passage de la frontière métastable aux métropoles opulentes.",
                "Ref: Ibn Khaldun (1377). Muqaddimah; Turchin, P. (2003). Historical Dynamics, Princeton Univ. Press.",
                "• Variation d'Asabiyyah : dS/dt = r_front · F(x) · (1 - S) - δ_luxury · (K/N) · S"),
            new EngineMeta("AxialAgeMoralizingGodsEngine", "🕊️ Dieux Moralisateurs & Prosocialité de l'Âge Axial (Whitehouse / Seshat 2019, Jaspers)",
                "Émergence des divinités moralisatrices et omniscience divine lors du franchissement du seuil de 100 000 habitants. Réduit drastiquement la friction contractuelle et les défections opportunistes entre étrangers.",
                "Ref: Whitehouse, H. et al. (2019). Complex societies precede moralizing gods throughout world history. Nature 568:226-229; Jaspers, K. (1949).",
                "• Indice Prosocial Θ_moral = σ((N - 100000)/25000) · σ((k_pc - 80)/20)\n• Friction Effective = Friction_base · (1 - 0.35 · Θ_moral)"),

            // =========================================================================
            // 3. IRON AGE, MEDIEVAL & EARLY MODERN MERCANTILE ENGINES (13)
            // =========================================================================
            new EngineMeta("BarfieldNomadicSteppeEmpireEngine", "🐎 Empire Pastoral de l'Ombre & Extorsion Symbiotique (Thomas Barfield 1989)",
                "Modèle de l'État nomade tourné vers l'extérieur : les confédérations de la steppe (Xiongnu, Mongols) extorquent le tribut d'un empire sédentaire voisin unifié et se fragmentent dès que le tribut cesse.",
                "Ref: Barfield, T. J. (1989). The Perilous Frontier: Nomadic Empires and China. Blackwell; Di Cosmo, N. (2002).",
                "• Capacité d'Extorsion E_strike = Pop_nomade · (Bétail / Pop)^0.5\n• Choc de Rupture : Si Tribut == 0 -> Fracture Clanique Centrifuge"),
            new EngineMeta("AcemogluRobinsonInstitutionsEngine", "🏛️ Institutions Inclusives vs Extractives (Acemoglu & Robinson 2001/2012)",
                "Divergence économique mondiale gouvernée par la qualité institutionnelle et les droits de propriété. Différencie les institutions extractives des institutions inclusives.",
                "Ref: Acemoglu, D. & Robinson, J. A. (2012). Why Nations Fail; Acemoglu, Johnson & Robinson (2001). AER.",
                "• Indice d'Inclusivité dI_inc/dt = θ₁ · Société_Civile - θ₂ · Rente_Élites\n• Accumulation Capital : ΔK = K · 0.02 · (I_inc - 0.35) · Δt"),
            new EngineMeta("KrugmanCorePeripheryEngine", "🏙️ Nouvelle Économie Géographique Cœur-Périphérie (Paul Krugman 1991)",
                "Interaction entre économies d'échelle internes, coûts de transport de type iceberg et mobilité du travail manufacturier, créant une polarisation spatiale entre métropoles et arrière-pays.",
                "Ref: Krugman, P. (1991). Increasing Returns and Economic Geography. J. Polit. Econ.",
                "• Salaire Réel ω_i = (Σ Y_j · T_{ij}^{1-σ} · G_j^{σ-1})^{1/σ}\n• Migration Ouvrière : dλ_i/dt = γ · (ω_i / ω_moyen - 1) · λ_i"),
            new EngineMeta("CoGovernanceTradeEngine", "🤝 Co-Gouvernance des Réseaux Marchands (1100)",
                "Régulation conjointe des routes d'échange et mutualisation des coûts de sécurité des convois (Ligue Hanséatique, Foires de Champagne) prévenant la prédation seigneuriale.",
                "Ref: Greif, A. (2006). Institutions and the Path to the Modern Economy. Cambridge Univ. Press.",
                "• Abaissement Friction : Friction_marchande = Friction_base · (1 - 0.35 · CoGouvernance)"),
            new EngineMeta("LandReclamationEngine", "🏗️ Poldérisation & Gain sur la Mer (1200)",
                "Transformation de zones côtières peu profondes en polders agricoles et terre-pleins gagnés sur la mer selon le capital physique accumulé.",
                "Ref: TeBrake, W. H. (1985). Medieval Frontier: Culture and Ecology in Rijnland.",
                "• Poldérisation Côtière : Conversion de mailles littorales basses en terres arables endiguées"),
            new EngineMeta("MilitaryTechShockEngine", "💣 Chocs de Technologie Militaire & Poudre à Canon (1200)",
                "Révolution militaire et transformation de l'architecture des fortifications (trace italienne, artillerie). Augmente la capacité de consolidation étatique.",
                "Ref: Parker, G. (1988). The Military Revolution; McNeill, W. H. (1982). The Pursuit of Power.",
                "• Puissance Offensive = Puissance_Base · (1 + Multiplicateur_Poudre_Canon)"),
            new EngineMeta("PhysicalMediaPrintingEngine", "📖 Révolution de l'Imprimerie & Capital d'Instruction (1450)",
                "Caractères mobiles de Gutenberg abaissant de 99% le coût de copie des livres. Stimule l'alphabétisation populaire et la révolution scientifique.",
                "Ref: Eisenstein, E. L. (1979). The Printing Press as an Agent of Change; Buringh & van Zanden (2009).",
                "• Coût Copie Livre = Coût_Manuscrit · (1 - 0.98 · Adhésion_Imprimerie)\n• Croissance Alphabétisation dL/dt = α_presse · Titres_Imprimés · (1 - L)"),
            new EngineMeta("AsymmetricColonialTradeEngine", "🚢 Commerce Colonial Asymétrique & Extraction (1500)",
                "Flux de ressources et fuite de valeur des colonies vers les métropoles. Simule la capture de rente et le blocage de l'industrialisation périphérique.",
                "Ref: Wallerstein, I. (1974). The Modern World-System; Frank, A. G. (1967). Dependency Theory.",
                "• Capture de Rente : Transfert_Richesse = Termes_Échange_Asym · Export_Matières_Premières"),
            new EngineMeta("CrosbyColumbianExchangeEngine", "🌎 Échange Colombien & Choc Épidémique Vierge (Alfred Crosby 1972)",
                "Déficit en grands animaux de trait dans les écosystèmes précolombiens plafonnant l'EROEI musculaire, suivi du choc démographique lors du contact avec les pathogènes de l'Ancien Monde.",
                "Ref: Crosby, A. W. (1972). The Columbian Exchange. Greenwood Press; Dobyns, H. F. (1983).",
                "• Plafond Musculaire si Bétail < 1.0 : Travail <= 40.0\n• Choc de Contact Vierge : Mortalité Épidémique N(t+1) = N(t) · (1 - 0.90 · dt)"),
            new EngineMeta("ProtestantWorkEthicEngine", "✝️ Éthique du Travail & Accumulation de Capital (Max Weber 1905)",
                "Impact des normes d'épargne ascétique sur la formation du capital marchand et industriel. Stimule le réinvestissement des bénéfices dans le capital productif.",
                "Ref: Weber, M. (1905). Die protestantische Ethik und der Geist des Kapitalismus.",
                "• Taux d'Épargne Réinvestie : S_épargne = S_base · (1 + α_éthique_travail)"),
            new EngineMeta("SovereignTreasuryBondEngine", "📜 Marchés de Rente & Emprunts Souverains (1688)",
                "Révolution financière post-Glorious Revolution. Institution de la dette publique perpétuelle adossée au Parlement, stabilisant les taux d'intérêt de guerre.",
                "Ref: North, D. C. & Weingast, B. R. (1989). Constitutions and Commitment. J. Econ. Hist.",
                "• Taux d'Intérêt Dette r_dette = r_base + Prime_Risque · exp(Dette / Recettes_Fiscales)"),
            new EngineMeta("NavalHydrodynamicsFreightEngine", "⚓ Hydrodynamique Navale & Fret Maritime Massif (1750)",
                "Résistance hydrodynamique de carène de Froude et transition de la voile à la vapeur. Abaisse exponentiellement la friction de transport des pondéreux.",
                "Ref: Froude, W. (1874). On the Resistance of Ships; Stopford, M. (2009). Maritime Economics.",
                "• Traînée Hydrodynamique R_hull = 0.5 · ρ · v² · S_coque · C_f\n• Coût Unitaire de Fret = Coût_Base / (Port en lourd tonnage)^0.67"),
            new EngineMeta("InnisStaplesThesisEngine", "🪵 Théorie des Staples & Dépendance Frontalière (Harold Innis 1930)",
                "Développement des territoires périphériques par l'extraction et l'exportation de matières premières brutes (fourrures, bois, morue, minerais).",
                "Ref: Innis, H. A. (1930). The Fur Trade in Canada. Univ. of Toronto Press; Mackintosh, W. A. (1923).",
                "• Indice Staple S_ratio = (Biomasse_Bois + Minerais) / (Pop · 10 + 1)\n• Biais d'Infrastructures Linéaires : dTrade_Export/dt = +4% · S_ratio"),

            // =========================================================================
            // 4. INDUSTRIAL, FOSSIL-EXERGY & MODERN MACROECONOMIC ENGINES (13)
            // =========================================================================
            new EngineMeta("PomeranzGreatDivergenceEngine", "🚂 Grande Divergence, Charbon & Hectares Fantômes (Kenneth Pomeranz 2000)",
                "Décollage industriel catalysé par la proximité géographique du charbon navigable et des hectares fantômes coloniaux levant le plafond écologique organique.",
                "Ref: Pomeranz, K. (2000). The Great Divergence. Princeton Univ. Press; Allen, R. C. (2009).",
                "• Accès Charbon Φ_coal = σ(Coal - 500, 100) · (1 - σ(Friction - 0.70, 0.15))\n• Hectares Fantômes Φ_ghost = σ(Trade - 25, 5) · σ(k_pc - 100, 30)"),
            new EngineMeta("RicardoDifferentialLandRentEngine", "🌾 Rente Foncière Différentielle & Marge Extensive (David Ricardo 1817)",
                "L'expansion démographique sur des terres marginales moins fertiles gonfle la rente foncière captée par les propriétaires terriens et comprime le taux de profit industriel.",
                "Ref: Ricardo, D. (1817). On the Principles of Political Economy and Taxation. John Murray.",
                "• Pression sur la Marge Φ_margin = σ((N · 1.20 / (Biomass_Agri + 1)) - 1.0, 0.25)\n• Compression du Profit : dK/dt = -0.035 · Φ_margin · K"),
            new EngineMeta("KummelAyresExergyEngine", "⚡ Production Biophysique LINEX & Exergie Utile (Kümmel-Ayres-Warr 1982/2005)",
                "Modèle de fonction de production dépendant de l'énergie physique réelle (travail exergétique des convertisseurs industriels).",
                "Ref: Kümmel, R. (1982); Ayres, R. U. & Warr, B. (2005). Accounting for Growth: The Role of Physical Work.",
                "• Fonction LINEX : Y = Y₀ · E^{a_E} · L^{a_L} · K^{1 - a_E - a_L}\n• Exergie Utile E = Conversion_Machines · Énergie_Primaire"),
            new EngineMeta("PolanyiFictitiousCommoditiesEngine", "📜 Marchandises Fictives & Double Mouvement (Karl Polanyi 1944)",
                "La transformation de la terre et du travail en marchandises fictives génère un gain d'efficacité allocative à court terme mais provoque le double mouvement de protection sociale.",
                "Ref: Polanyi, K. (1944). The Great Transformation: The Political and Economic Origins of Our Time. Beacon Press.",
                "• Indice de Marchandisation M_com = σ(k_pc - 80, 20) · σ(Trade - 15, 5)\n• Dislocation Sociale : dFriction/dt = +0.05 · M_com · Stress_Subsistance"),
            new EngineMeta("HotellingResourceDepletionEngine", "⛏️ Rente d'Épuisement d'Hotelling & Ressources Épuisables (1931)",
                "Dynamique microéconomique des ressources finies non-renouvelables. Le prix de rareté en sous-sol augmente au taux d'actualisation à mesure que les gisements s'épuisent.",
                "Ref: Hotelling, H. (1931). The Economics of Exhaustible Resources. J. Polit. Econ.",
                "• Règle d'Hotelling : dP/dt = r_intérêt · P - ∂C(R, Q)/∂R\n• Coût Marginal d'Extraction : C_ext = C₀ / (R_restant / R_initial)"),
            new EngineMeta("KondratievSchumpeterLongWaveEngine", "🌊 Ondes Longues de Kondratiev & Destruction Créatrice (1925 / 1939)",
                "Cycles séculaires de 52 ans de renouvellement des grandes infrastructures matérielles (canaux, rail, électricité) ponctués de phases de destruction créatrice.",
                "Ref: Kondratiev, N. D. (1925). The Major Economic Cycles; Schumpeter, J. A. (1939). Business Cycles.",
                "• Phase Cyclique θ(t) = ((Year - 1750) mod 52) / 52.0\n• Modulation d'Investissement W_k = sin(2π · θ(t)) · σ(k_pc - 120, 30)"),
            new EngineMeta("UrbanThermodynamicsEngine", "🏙️ Thermodynamique Urbaine & Îlots de Chaleur (1850)",
                "Îlots de chaleur urbains et densité bâtie hyper-concentrée. Élève la température locale des métropoles et consomme de la puissance de climatisation.",
                "Ref: Oke, T. R. (1982). The Energetic Basis of the Urban Heat Island. Q. J. R. Meteorol. Soc.",
                "• Îlot de Chaleur Urbain ΔT_urbain = a · log₁₀(Densité_Bâtie) + b"),
            new EngineMeta("PublicSanitationWaterGridEngine", "🚰 Hygiène Publique & Réseaux d'Aqueducs / Égouts (1850)",
                "Transition sanitaire urbaine de John Snow et Chadwick. Construction de réseaux d'adduction d'eau potable et d'égouts séparatifs éliminant les épidémies hydriques.",
                "Ref: Snow, J. (1855). On the Mode of Communication of Cholera; Cutler & Miller (2005).",
                "• Mortalité Hydrique μ_eau = μ₀ · exp(-k_sanitation · Capital_Tuyaux / Pop)"),
            new EngineMeta("SmilMaterialTransitionsPureEngine", "🏗️ Transitions Matérielles & Énergétiques (Vaclav Smil 2019)",
                "Inertie physique des transitions vers l'acier, le béton, l'ammoniac et le plastique. Impose des délais de plusieurs décennies pour remplacer un matériau de base.",
                "Ref: Smil, V. (2019). Material World & Energy Transitions: History, Requirements.",
                "• Temps de Transition Matérielle T_transition = 40 à 60 ans par inertie du capital fixe"),
            new EngineMeta("CordenNearyDutchDiseaseEngine", "⛏️ Mal Hollandais & Syndrome Rentier (Corden & Neary 1982, Auty 1993)",
                "Les booms d'exportation minérale ou fossile apprécient les coûts des facteurs locaux, évinçant la manufacture et l'agriculture diversifiée.",
                "Ref: Corden, W. M. & Neary, J. P. (1982). Booming Sector and De-Industrialisation. Econ. J. 92:825-848; Auty, R. (1993).",
                "• Ratio de Rente R_boom = Rente_Extraction / (Capital_Total + 1)\n• Éviction Manufacturière : Perte_Diversification = -3%/an · σ(R_boom - 0.25, 0.08)"),
            new EngineMeta("JevonsParadoxEngine", "⚡ Effet Rebond & Paradoxe de Jevons (1865)",
                "L'augmentation de l'efficacité énergétique accroît la consommation globale. Annule les gains d'économie d'énergie par l'expansion de l'échelle industrielle.",
                "Ref: Jevons, W. S. (1865). The Coal Question; Alcott, B. (2005). Jevons' Paradox.",
                "• Énergie Consommée E_total = Pop · Efficacité^ε avec ε > 1.0 (Paradoxe de Jevons)"),
            new EngineMeta("BagehotFractionalReserveEngine", "🏦 Panique de Liquidité & Prêteur en Dernier Ressort (Walter Bagehot 1873)",
                "Sous réserves fractionnaires, les chocs de confiance provoquent une contraction brutale du crédit privé. La règle de Bagehot enraye la déflation de dette.",
                "Ref: Bagehot, W. (1873). Lombard Street: A Description of the Money Market. King & Co; Fisher, I. (1933).",
                "• Multiplicateur de Crédit M_credit = 1.0 + 0.35 · σ(k_pc - 150, 40)\n• Stabilisation Bagehot : Injection de réserves métalliques contre gages"),
            new EngineMeta("LewisDualSectorTurningPointEngine", "🏭 Modèle Dualiste & Point de Basculement de Lewis (W. Arthur Lewis 1954)",
                "Absorption du surplus de main-d'œuvre rurale par le secteur industriel à bas salaire jusqu'au point de basculement de Lewis déclenchant la hausse des salaires réels et la mécanisation.",
                "Ref: Lewis, W. A. (1954). Economic Development with Unlimited Supplies of Labour. The Manchester School.",
                "• Point de Basculement Φ_turning = σ(k_pc - 250, 40) · σ(Trade - 20, 5)\n• Mécanisation Induite : ΔK_deepening = +0.045 · Φ_turning · K · dt"),

            // =========================================================================
            // 5. CONTEMPORARY, GREAT ACCELERATION & PROSPECTIVE ENGINES (13)
            // =========================================================================
            new EngineMeta("PhysicalLeontiefInputOutputEngine", "🏭 Matrices Entrées-Sorties Physiques de Leontief (Wassily Leontief 1941)",
                "Tableau d'échanges inter-industriels en unités physiques (tonnes d'acier, MWh d'électricité, boisseaux de blé). Modélise les goulets d'étranglement de chaînes de production interconnectées.",
                "Ref: Leontief, W. (1941). The Structure of American Economy 1919-1929. Harvard Univ. Press.",
                "• Équilibre de Production : X = (I - A)^{-1} · D_finale\n• Contrainte de Capacité Minérale : Production <= Min_i(Stock_i / A_{i,j})"),
            new EngineMeta("PhysicalSupplyChainEngine", "🚢 Chaînes Logistiques & Flux Tendus Inter-Industriels (1900+)",
                "Propagation des ruptures d'approvisionnement en matières premières et composants intermédiaires le long des chaînes logistiques industrielles mondialisées.",
                "Ref: Forrester, J. W. (1961). Industrial Dynamics; Helbing, D. (2013). Globally Networked Risks. Nature.",
                "• Effet Coup de Fouet (Bullwhip) & Goulet d'Étranglement des Intrants Physiques"),
            new EngineMeta("WienerCyberneticHomeostasisEngine", "🔄 Homéostasie Cybernétique & Boucles Rétroactives (Norbert Wiener 1948)",
                "Régulation cybernétique des systèmes complexes par boucle de rétroaction négative : amortit les oscillations violentes des ressources autour des points de consigne physiologiques.",
                "Ref: Wiener, N. (1948). Cybernetics: Or Control and Communication in the Animal and the Machine. MIT Press.",
                "• Erreur Consigne e(t) = (Ressource_Réelle - Consigne_Cible) / Consigne_Cible\n• Amortissement Rétroactif : ΔEffort = -Gain · e(t) · dt"),
            new EngineMeta("InformationEntropyEngine", "📶 Entropie Informationnelle & Réseaux de Télécommunication (1948)",
                "Capacité de canal et compression de l'information selon le théorème de Shannon. Modélise la vitesse de synchronisation des marchés et de la coordination bureaucratique.",
                "Ref: Shannon, C. E. (1948). A Mathematical Theory of Communication. Bell System Tech. J.",
                "• Capacité de Shannon C = B · log₂(1 + S/N)\n• Entropie H(X) = - Σ p(x) · log₂(p(x))"),
            new EngineMeta("EcotoxicologyFertilityEngine", "🧪 Écotoxicologie & Perturbateurs Endocrinien Synthétiques (1950)",
                "Impacts des polluants chimiques de synthèse persistants sur le taux de fécondité humaine.",
                "Ref: Colborn, T. et al. (1996). Our Stolen Future; Swan, S. H. (2021). Count Down.",
                "• Baisse de Fécondité : F_effective = F_naturelle · exp(-k_tox · Charge_Pollution_Cumulée)"),
            new EngineMeta("NuclearSafetyRadiotoxicityEngine", "⚛️ Radiotoxicité & Sûreté des Réacteurs Nucléaires (1950)",
                "Gestion des risques d'accidents électronucléaires et retombées radiotoxiques. Simule la contamination des terres et les coûts de sûreté industrielle.",
                "Ref: Perrow, C. (1984). Normal Accidents: Living with High-Risk Technologies.",
                "• Probabilité d'Accident Majeur = 1 - exp(-Taux_Défaillance_Système · Nombre_Réacteurs)"),
            new EngineMeta("MarineSubmersionEngine", "🚨 Submersion Marine & Évacuation Côtière Physique (1950+)",
                "Modélisation physique de l'élévation du niveau de la mer, de la maintenance des digues et des flux d'évacuation préventive des littoraux.",
                "Ref: IPCC Coastal Inundation & Flood Early Warning Physics; Adger, W. N. (2006). Vulnerability.",
                "• Évacuation Physique : Ratio_Evac = 1 - exp(-0.4 · (1 + K/500))\n• Déplacement de Population vers mailles sèches adjacentes"),
            new EngineMeta("World3CouplingEngine", "📉 Modèle Couplé World3 & Limites à la Croissance (1972)",
                "Rétroactions entre population, pollution industrielle persistante et capital (Club de Rome). Connecte le modèle World3 de Meadows aux cellules hexagonales H3 (valide ≥ 1950 CE).",
                "Ref: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). Limits to Growth.",
                "• System Dynamics 5 Subsystems (Population, Capital, Agriculture, Persistent Pollution, Non-Renewables)"),
            new EngineMeta("NordhausDiceHybridEngine", "🌡️ Modèle DICE Hybride (Nordhaus 1992 - Climat & Économie)",
                "Couplage économie-climat intégré avec boucle de dommage quadratique du carbone sur le capital productif.",
                "Ref: Nordhaus, W. D. (1992, 2017). Integrated Assessment Models (DICE-2016R).",
                "• Fonction de Dommage Nordhaus Ω(T) = 1 / (1 + π₁·ΔT + π₂·ΔT²)\n• Capital Ajusté Climat K_net = Ω(T) · K_brut"),
            new EngineMeta("KurzweilAcceleratingReturnsEngine", "🚀 Loi des Rendements Accélérés & Semi-Conducteurs (1990 / 2005)",
                "Accélération du progrès matériel et informationnel par recombinaison à haute densité de capital et d'infrastructure de calcul.",
                "Ref: Moore, G. E. (1965); Kurzweil, R. (2005). The Singularity Is Near.",
                "• Accélération Combinatoire sous Haute Densité Informationnelle"),
            new EngineMeta("CliodynamicAdvisorEngine", "⚠️ Conseiller Prédictif Multi-Risques & Alertes Cliodynamiques (2020+)",
                "Moteur de veille cybernétique planétaire. Analyse les gradients de tension sociale, de stress hydrique et d'épuisement des ressources.",
                "Ref: Turchin, P. (2008). Arise Cliodynamics. Nature; Beer, S. (1972). Brain of the Firm.",
                "• Indice de Risque Composite R = max(PSI_turchin, Stress_hydrique, Risque_famine)"),
            new EngineMeta("IsruAutarkyAndSpaceColonizationEngine", "🚀 Utilisation des Ressources In-Situ (ISRU) & Autarcie Spatiale (2050+)",
                "Fermeture thermodynamique des colonies lunaires et martiennes par synthèse d'ergols et d'oxygène in-situ (ISRU) sans dépendance logistique terrestre.",
                "Ref: Zubrin, R. (1996). The Case for Mars; NASA ISRU Technical Reports.",
                "• Autarcie ISRU = Min(O2_synthèse / O2_requis, H2O_recyclée / H2O_requise)"),
            new EngineMeta("TerraformingEngine", "🪐 Ingénierie Planétaire & Terraformation Globale (2100+)",
                "Altération délibérée de la pression atmosphérique, de la composition gazeuse et de l'hydrosphère pour rendre habitable un corps céleste stérile (Mars, Vénus, Lune).",
                "Ref: Sagan, C. (1973). Planetary Engineering on Mars; McKay, C. P. et al. (1991). Nature.",
                "• Épaississement Atmosphère dP_atm/dt = Flux_Volatils_Dégazage - Échappement_Solaire"),

            // =========================================================================
            // 6. SPECIALIZED REGIONAL, COUNTERFACTUAL & PURE-THEORY SANDBOX ENGINES (14)
            // =========================================================================
            new EngineMeta("AmerindianEcosystemEngine", "🌾 Agro-foresterie Amérindienne & Terra Preta (-4 000 BP)",
                "Techniques agricoles précolombiennes et enrichissement des sols en biochar (Terra Preta). Augmente la capacité portante et la résilience des sols de forêt tropicale.",
                "Ref: Denevan, W. M. (1992). The Pristine Myth; Glaser, B. et al. (2002). Terra Preta Biochar.",
                "• Formation de Terra Preta : dC_biochar/dt = Apport_Charbon_Organique - Oxydation_Lente(0.001)"),
            new EngineMeta("RomanImperialCliodynamicEngine", "🪙 Cycle Cliodynamique Romain & Altération du Denier (-27 / 193)",
                "Dégradation de la pureté du Denier d'argent et crises fiscales impériales. Entraîne l'inflation et le déclin du pouvoir d'achat des légions.",
                "Ref: Turchin, P. & Scheidel, W. (2009). Coin Debasement and Structural Cycles in Rome.",
                "• Pureté de la Monnaie : Teneur_Argent = T₀ · exp(-k_solde_fiscal · Déficit_Armée)"),
            new EngineMeta("MonasticDemographicBufferEngine", "🏛️ Buffers Démographiques Monastiques & Préservation du Savoir (500)",
                "Préservation du capital d'instruction écrit et régulation démographique par les réseaux monastiques lors des effondrements d'empires.",
                "Ref: Collins, R. (1998). The Sociology of Philosophies; Weber, M. (1905).",
                "• Plancher de Rétention du Savoir : Conservation des manuscrits face aux destructions"),
            new EngineMeta("EdoJapanIsolationEngine", "⛩️ Isolationnisme du Japon Edo / Sakoku (1635)",
                "Maintien d'un équilibre zéro-croissance, reboisement sylvicole strict et fermeture des frontières commerciales sous le shogunat Tokugawa.",
                "Ref: Totman, C. (1993). Early Modern Japan; Diamond, J. (2005). Collapse (Tokugawa Forestry).",
                "• Équilibre Sylvicole & Zéro-Croissance : Extraction_Bois <= Auto_Régénération_Forêt"),
            new EngineMeta("OstromCommonsPureEngine", "🏞️ Auto-Gouvernance Polycentrique des Communs (Elinor Ostrom 1990)",
                "Règles institutionnelles locales et surveillance mutuelle pour éviter la tragédie des communs sans privatisation ni coercition centrale.",
                "Ref: Ostrom, E. (1990). Governing the Commons: The Evolution of Institutions for Collective Action.",
                "• Maintien des Communs : Taux_Survie_Communs = f(Institution_Locale, Sanctions_Graduées)"),
            new EngineMeta("PinkerViolenceDeclinePureEngine", "🕊️ Déclin Historique de la Violence & Pacification (Steven Pinker 2011)",
                "Baisse séculaire de la mortalité violente par le monopole étatique de la force légitime (Léviathan), le doux commerce et l'alphabétisation.",
                "Ref: Pinker, S. (2011). The Better Angels of Our Nature: Why Violence Has Declined.",
                "• Taux de Mortalité Violente = V₀ · exp(-k_état · Monopole_Violence - k_commerce · Commerce)"),
            new EngineMeta("World3HybridEngine", "📉 Modèle World3 Hybride Spatialisé (Meadows 1972 / 2004)",
                "Couplage de la dynamique FCAOR et de la pollution persistante de World3 aux variables spatiales H3 pour tests de sensibilité.",
                "Ref: Meadows, D. H. et al. (1972, 2004). Limits to Growth: The 30-Year Update.",
                "• Réallocation du Capital vers l'Extraction (FCAOR) & Mortalité de Pollution Persistante"),
            new EngineMeta("World3PureEngine", "📉 Modèle World3 Pur Non-Couplé (Équations du Club de Rome 1972)",
                "Reproduction autonome des 5 sous-systèmes du rapport Meadows 1972 (Population, Capital, Agriculture, Pollution, Ressources) pour comparaison théorique pure.",
                "Ref: Meadows, D. H. et al. (1972). Dynamics of Growth in a Finite World.",
                "• Système d'Équations Différentielles Ordinaires World3 (DYNAMO / Stella)"),
            new EngineMeta("HandyNasaHybridEngine", "📉 Modèle HANDY NASA Hybride (Motesharrei 2014)",
                "Rétroactions couplées entre élites, travailleurs et ressources naturelles (Modèle HANDY) sur maillage H3.",
                "Ref: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). Human and Nature Dynamics (HANDY). Ecological Economics 101.",
                "• Surconsommation Élitaire & Déplétion Locale des Éco-Dollars"),
            new EngineMeta("HandyNasaPureEngine", "📉 Modèle HANDY NASA Pur (4 Équations Différentielles 2014)",
                "Système dynamique pur à 4 équations différentielles couplées (Élites, Roturiers, Nature, Richesse) pour falsification théorique.",
                "Ref: Motesharrei, S. et al. (2014). HANDY Model Equations. Ecological Economics 101:90-102.",
                "• dN/dt = γ·N·(λ - N) - δ·x_C·N ; dW/dt = δ·x_C·N - C_C - C_E"),
            new EngineMeta("NordhausDicePureEngine", "🌡️ Modèle DICE Pur (Coût Social du Carbone & PIB 1992)",
                "Modélisation analytique pure du modèle DICE de Nordhaus pour la comparaison directe avec la thermodynamique biophysique.",
                "Ref: Nordhaus, W. D. (1992). An Optimal Transition Path for Controlling Greenhouse Gases. Science 258:1315-1319.",
                "• Dommage Quadratique Pur : Y_net = Y / (1 + 0.00236 · ΔT²)"),
            new EngineMeta("NuclearWarfareClimateEngine", "💥 Guerre Thermonucléaire & Hiver Nucléaire (TTAPS 1983)",
                "Injection massive de suie stratosphérique issue d'incendies urbains/industriels bloquant le rayonnement solaire et provoquant un hiver nucléaire global.",
                "Ref: Turco, R. P., Toon, O. B., Ackerman, T. P., Pollack, J. B., & Sagan, C. (1983). Nuclear Winter. Science 222:1283.",
                "• Baisse Température Mondiale ΔT_nucléaire = -15.0 °C · (Masse_Suie / 150 Tg)"),
            new EngineMeta("GeoengineeringAlbedoFeedbackEngine", "🌥️ Géo-Ingénierie Solaire & Injection d'Aérosols Stratosphériques (2030+)",
                "Injection délibérée d'aérosols soufrés stratosphériques augmentant l'albédo planétaire pour compenser le forçage radiatif des gaz à effet de serre.",
                "Ref: Crutzen, P. J. (2006). Albedo Enhancement by Stratospheric Sulfur Injections. Climatic Change 77:211.",
                "• Delta Albédo Artificiel : ΔAlbédo = k_aerosol · Masse_SO2_Injectée"),
            new EngineMeta("BifurcationChaosEngine", "🌀 Analyse de Chaos Déterministe & Exposants de Lyapunov (1963)",
                "Injection contrôlée de micro-perturbations logistiques pour mesurer la sensibilité aux conditions initiales et l'exposant de Lyapunov maximal.",
                "Ref: Lorenz, E. N. (1963). Deterministic Nonperiodic Flow; May, R. M. (1976). Nature.",
                "• Bifurcation Logistique : x_{t+1} = r · x_t · (1 - x_t)")
        );
    }

    /**
     * Resolves the historical emergence year (BCE/CE) for each of the 86 verified Type B engines.
     *
     * @param key unique engine identifier
     * @return emergence year in astronomical calendar convention
     */
    static long getHistoricalApparitionYear(String key) {
        return switch (key) {
            case "SelfDomesticationEngine", "SexualSelectionMatingEngine",
                 "KinSelectionHamiltonEngine", "PriceMultilevelSelectionEngine",
                 "TasmanianCulturalRegressionEngine" -> -3000000L;
            case "LanguageLinguisticEngine", "CulturalSociologyEngine" -> -300000L;
            case "DynamicMaritimeRoutingGraph" -> -60000L;
            case "BoserupAgriculturalIntensificationEngine" -> -8000L;
            case "AmerindianEcosystemEngine", "ScottAgainstTheGrainPureEngine" -> -4000L;
            case "DraftAnimalFodderAllocationEngine", "TradeNetworkEngine", "WarDiplomacyEngine",
                 "ThermodynamicWarfareEngine", "MaritimeHighwayEngine", "HydrologicalEngineeringEngine",
                 "SoilSalinizationHydrologyEngine", "SpatialCityFractalEngine", "VonThunenAgriculturalRingsEngine",
                 "WestBettencourtAllometryEngine", "TurchinGoldstoneSDTEngine",
                 "StructuralDemographicBifurcationEngine", "TainterComplexityCollapseEngine",
                 "CliodynamicsEngine", "ArthurCombinatorialTechnologyEngine",
                 "GranovetterThresholdCascadeEngine", "SchellingAxelrodSegregationEngine",
                 "SpatialMetapopulationSEIREngine", "HarrisCulturalMaterialismEngine",
                 "LenskiEcologicalEvolutionaryEngine", "WhiteEnergyEvolutionEngine",
                 "FrontierAsabiyyahEngine", "OstromCommonsPureEngine", "PinkerViolenceDeclinePureEngine" -> -3000L;
            case "AxialAgeMoralizingGodsEngine", "BarfieldNomadicSteppeEmpireEngine" -> -1000L;
            case "AcemogluRobinsonInstitutionsEngine", "KrugmanCorePeripheryEngine" -> -500L;
            case "RomanImperialCliodynamicEngine" -> -27L;
            case "MonasticDemographicBufferEngine" -> 500L;
            case "CoGovernanceTradeEngine" -> 1000L;
            case "LandReclamationEngine", "MilitaryTechShockEngine" -> 1200L;
            case "PhysicalMediaPrintingEngine" -> 1450L;
            case "AsymmetricColonialTradeEngine", "CrosbyColumbianExchangeEngine" -> 1492L;
            case "ProtestantWorkEthicEngine" -> 1550L;
            case "EdoJapanIsolationEngine" -> 1635L;
            case "SovereignTreasuryBondEngine" -> 1650L;
            case "NavalHydrodynamicsFreightEngine", "InnisStaplesThesisEngine" -> 1700L;
            case "PomeranzGreatDivergenceEngine", "RicardoDifferentialLandRentEngine" -> 1750L;
            case "KummelAyresExergyEngine", "PolanyiFictitiousCommoditiesEngine",
                 "HotellingResourceDepletionEngine", "KondratievSchumpeterLongWaveEngine" -> 1800L;
            case "UrbanThermodynamicsEngine", "PublicSanitationWaterGridEngine",
                 "SmilMaterialTransitionsPureEngine", "CordenNearyDutchDiseaseEngine",
                 "LewisDualSectorTurningPointEngine" -> 1850L;
            case "JevonsParadoxEngine" -> 1865L;
            case "BagehotFractionalReserveEngine" -> 1873L;
            case "PhysicalLeontiefInputOutputEngine", "PhysicalSupplyChainEngine",
                 "WienerCyberneticHomeostasisEngine", "InformationEntropyEngine" -> 1900L;
            case "NuclearWarfareClimateEngine" -> 1945L;
            case "EcotoxicologyFertilityEngine", "NuclearSafetyRadiotoxicityEngine",
                 "MarineSubmersionEngine", "World3CouplingEngine", "World3HybridEngine",
                 "World3PureEngine", "NordhausDiceHybridEngine", "NordhausDicePureEngine",
                 "HandyNasaHybridEngine", "HandyNasaPureEngine", "BifurcationChaosEngine" -> 1950L;
            case "KurzweilAcceleratingReturnsEngine" -> 1990L;
            case "CliodynamicAdvisorEngine" -> 2020L;
            case "GeoengineeringAlbedoFeedbackEngine" -> 2030L;
            case "IsruAutarkyAndSpaceColonizationEngine" -> 2050L;
            case "TerraformingEngine" -> 2100L;
            default -> -3000000L;
        };
    }
}
