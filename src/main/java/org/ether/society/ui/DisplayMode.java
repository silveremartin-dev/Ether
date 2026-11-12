/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

/**
 * Display mode for H3 map visualization content.
 * Controls what data is visualized on the map cells, organized into logical categories.
 * Includes precise technical descriptions of what each display mode calculates.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum DisplayMode {

    // --- ðŸŒ 1. PHYSICS & CLIMATE ---
    BIOME("ðŸ—ºï¸ Biomes & Relief Topographique", Category.PHYSICAL,
        "Cartographie des biomes Ã©cologiques (ForÃªts, Tundras, DÃ©serts, OcÃ©ans) et de l'altitude du relief H3."),
    TEMPERATURE("ðŸŒ¡ï¸ TempÃ©rature & Climat", Category.PHYSICAL,
        "Affiche la tempÃ©rature de surface (Â°C) calculÃ©e par l'insolation solaire, l'albÃ©do local et le forÃ§age radiatif."),
    PRECIPITATION("ðŸŒ§ï¸ PrÃ©cipitations & PluviomÃ©trie", Category.PHYSICAL,
        "Affiche la hauteur de prÃ©cipitations annuelles (mm/an) reÃ§ue par la maille hexagonale H3."),
    WATER("ðŸ’§ Ressources en Eau & AquifÃ¨res", Category.PHYSICAL,
        "Niveau des rÃ©serves d'eau douce (aquifÃ¨res, riviÃ¨res et lacs) disponibles par kilomÃ¨tre carrÃ©."),
    ALBEDO("â„ï¸ AlbÃ©do de Surface & Glaces", Category.PHYSICAL,
        "Coefficient de rÃ©flexion solaire (0.0 Ã  1.0) selon la couverture de glace, la vÃ©gÃ©tation et les surfaces urbaines."),
    FRICTION("ðŸ§— Friction de DÃ©placement", Category.PHYSICAL,
        "CoÃ»t d'effort thermodynamique et difficultÃ© de dÃ©placement Ã  travers le relief, la pente et la vÃ©gÃ©tation dense."),
    ENERGY_CAPACITY("âš¡ Ã‰nergie CaptÃ©e (Flux Solaire/Biomasse)", Category.PHYSICAL,
        "Flux d'Ã©nergie primaire captÃ© (MW/kmÂ²) par l'insolation solaire, la biomasse et les infrastructures Ã©nergÃ©tiques."),
    ENTROPY_POLLUTION("â˜£ï¸ Entropie & Empreinte Pollution", Category.PHYSICAL,
        "Taux de gÃ©nÃ©ration d'entropie thermodynamique, rejets polluants et dÃ©gradations toxiques de l'environnement."),
    SOIL_QUALITY("ðŸŒ¾ FertilitÃ© & QualitÃ© NPK des Sols", Category.PHYSICAL,
        "Teneur en nutriments organiques NPK (Azote, Phosphore, Potassium) et fertilitÃ© des sols agricoles."),
    BIODIVERSITY("ðŸŒ¿ BiodiversitÃ© Sauvage ConservÃ©e", Category.PHYSICAL,
        "Proportion de biodiversitÃ© sauvage prÃ©servÃ©e et intÃ©gritÃ© des rÃ©seaux trophiques floristiques et fauniques."),
    OCEAN_PH("ðŸŒŠ AciditÃ© OcÃ©anique & pH Marin", Category.PHYSICAL,
        "Niveau de pH des eaux ocÃ©aniques de surface et impact de la dissolution du CO2 atmosphÃ©rique."),
    PERMAFROST("ðŸ§Š PergÃ©lisol & DÃ©stabilisation MÃ©thane", Category.PHYSICAL,
        "VulnÃ©rabilitÃ© et dÃ©gel du pergÃ©lisol borÃ©al avec potentiel de dÃ©gazage de mÃ©thane."),

    // --- ðŸ‘¥ 2. DEMOGRAPHICS & HEALTH ---
    POPULATION("ðŸ‘¥ DensitÃ© de Population Active", Category.DEMOGRAPHICS,
        "Nombre d'habitants actifs rÃ©sidant dans la maille hexagonale H3."),
    MIGRATION("ðŸš¶ Flux & Pression Migratoire", Category.DEMOGRAPHICS,
        "Vecteurs et pression des flux migratoires inter-cellules stimulÃ©s par les diffÃ©rentiels de bien-Ãªtre et de ressources."),
    AGE_PYRAMID("ðŸ‘´ Pyramide des Ã‚ges (SÃ©niors)", Category.DEMOGRAPHICS,
        "Proportion et concentration des cohortes d'aÃ®nÃ©s et sÃ©niors (>60 ans) dans la structure dÃ©mographique."),
    EPIDEMIC("â˜£ï¸ Foyers Ã‰pidÃ©miques", Category.DEMOGRAPHICS,
        "Niveau de prÃ©valence et propagation des agents pathogÃ¨nes zoonotiques et bio-molÃ©culaires."),
    HEALTH_LIFE_EXPECTANCY("ðŸ¥ EspÃ©rance de Vie & SantÃ©", Category.DEMOGRAPHICS,
        "EspÃ©rance de vie moyenne thÃ©orique et rÃ©sistance immunitaire globale des populations de la cellule."),
    EDUCATION_LEVEL("ðŸŽ“ Niveau d'Instruction & Ã‰ducation", Category.DEMOGRAPHICS,
        "Part de la population maÃ®trisant les compÃ©tences techniques, l'Ã©criture et le capital d'instruction."),

    // --- ðŸŒ± 3. ECOLOGY & MALTHUSIAN PRESSURE ---
    MALTHUSIAN_PRESSURE("ðŸŒ± Empreinte Ã‰cologique & Stress Malthusien (Pop/K)", Category.ECOLOGY,
        "Ratio dÃ©mographique rapportÃ© Ã  la capacitÃ© portante Ã©cologique localement disponible (Population / K)."),
    CAPACITY("ðŸŒ¾ CapacitÃ© Portante Max (K)", Category.ECOLOGY,
        "CapacitÃ© portante maximale thÃ©orique (K) mesurÃ©e en nombre maximal d'habitants soutenables sans dÃ©gradation."),
    FOOD("ðŸž Stocks Alimentaires & Biomasse", Category.ECOLOGY,
        "Volume total de biomasse consommable et de stocks cÃ©rÃ©aliers stockÃ©s dans la maille."),
    WOOD("ðŸŒ² Ressources ForestiÃ¨res & Bois", Category.ECOLOGY,
        "Stock de biomasse lignocellulosique forestiÃ¨re exploitable pour le chauffage, la construction et l'outillage."),

    // --- a›i¸ 4. MINERAL RESOURCES & MINING ---
    MINERAL_RESOURCES("ðŸ’Ž Gisements Miniers & Minerais", Category.MINING,
        "Richesse et concentration en gisements mÃ©talliques, minerais de fer, cuivre et combustibles fossiles sous-jacents."),
    MINING_EXPLOITATION("â›ï¸ Exploitation MiniÃ¨re & Mines Actives", Category.MINING,
        "IntensitÃ© de l'activitÃ© d'extraction miniÃ¨re et vitesse d'Ã©puisement des filons minÃ©raux crustaux."),

    // --- ðŸ›i¸ 5. ECONOMY, SOCIETY & CLIODYNAMICS ---
    TECHNOLOGY("âš™ï¸ Niveau Technologique & Capital", Category.SOCIETY_POLITICS,
        "Indice de niveau technologique (Niveau Tech 0.0 Ã  10.0+) et stock de machines/capital bÃ¢ti par habitant."),
    CULTURE("ðŸŽ­ Vecteur d'IdentitÃ© Culturelle", Category.SOCIETY_POLITICS,
        "Dominance et propagation du vecteur culturel, linguistique et religieux des communautÃ©s."),
    POLITICAL("ðŸ‘‘ FrontiÃ¨res Politiques & Dominions", Category.SOCIETY_POLITICS,
        "DÃ©limitation des frontiÃ¨res territoriales, zones d'influence Ã©tatiques et contrÃ´le souverain des citÃ©s."),
    INEQUALITY("âš–ï¸ Indice d'InÃ©galitÃ© Ã‰conomique (Gini)", Category.SOCIETY_POLITICS,
        "Coefficient de Gini local (0.0 Ã  1.0) mesurant la disparitÃ© d'accumulation des richesses et du capital."),
    ASABIYYAH("âš” CohÃ©sion Asabiyyah & InstabilitÃ©", Category.SOCIETY_POLITICS,
        "Indice Khaldounien de cohÃ©sion sociale, solidaritÃ© de groupe et vulnÃ©rabilitÃ© aux crises de factionnalisme."),
    FLUX("ðŸ« Routes Commerciales & Flux", Category.SOCIETY_POLITICS,
        "Volume et intensitÃ© des flux de marchandises et routes commerciales terrestres/maritimes en transit."),
    HAPPINESS("ðŸ˜Š Indice de Bonheur & Bien-Ãªtre", Category.SOCIETY_POLITICS,
        "Indice synthÃ©tique de satisfaction de vie, bien-Ãªtre psychologique et sÃ©rÃ©nitÃ© sociale (0 Ã  100%)."),
    CONFLICT("âš” Frictions & Taux de Conflits", Category.SOCIETY_POLITICS,
        "Taux de friction, violence inter-groupe, banditisme et opÃ©rations de guerre cinÃ©tique."),
    INSTITUTIONAL_MATURITY("ðŸ›ï¸ MaturitÃ© Institutionnelle & CitÃ©s", Category.SOCIETY_POLITICS,
        "DegrÃ© de complexitÃ© administrative, juridique et d'organisation des citÃ©s-Ã©tats et gouvernements."),
    GDP_WEALTH("ðŸ’° PIB Local & Capital BÃ¢ti", Category.SOCIETY_POLITICS,
        "Produit IntÃ©rieur Brut (PIB) local produit par an et masse totale de capital d'infrastructures bÃ¢ties."),
    ELITE_DENSITY("ðŸ‘‘ Formation & DensitÃ© d'Ã‰lites", Category.SOCIETY_POLITICS,
        "Ratio de surproduction Ã©litaire et densitÃ© de la classe dirigeante par rapport aux producteurs."),
    COLLECTIVE_MEMORY("ðŸ§  Capital Informationnel & MÃ©moire", Category.SOCIETY_POLITICS,
        "Volume de connaissances archivÃ©es, brevets et donnÃ©es conservÃ©es dans le capital d'information (TB/bits)."),
    COLLAPSE_RISK("ðŸ“‰ Risque d'Effondrement SystÃ©mique", Category.SOCIETY_POLITICS,
        "ProbabilitÃ© mathÃ©matique d'effondrement systÃ©mique ou de basculement irrÃ©versible de la cellule."),
    HABITAT_INFRASTRUCTURE("ðŸª Habitats Spatiaux & Pressurisation", Category.SOCIETY_POLITICS,
        "Cartographie des dÃ´mes, tubes de lave et aÃ©rostats pressurisÃ©s, de leur niveau de vÃ©tustÃ© et de leur intÃ©gritÃ© structurelle.");

    public enum Category {
        PHYSICAL("ðŸŒ PHYSIQUE & CLIMAT"),
        DEMOGRAPHICS("ðŸ‘¥ DÃ‰MOGRAPHIE & SANTÃ‰"),
        ECOLOGY("ðŸŒ± Ã‰COLOGIE & PRESSION MALTHUSIENNE"),
        MINING("â›ï¸ RESSOURCES MINÃ‰RALES & EXPLOITATION"),
        SOCIETY_POLITICS("ðŸ›ï¸ Ã‰CONOMIE, SOCIÃ‰TÃ‰ & CLIODYNAMIQUE");

        /** Internal state: category name (String). */
        private final String categoryName;

        Category(String categoryName) {
            this.categoryName = categoryName;
        }

    /**
     * Get category name.
     *
     * @return the resulting computation or state reference
     */
    public String getCategoryName() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode.category." + name().toLowerCase(), categoryName);
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        return getCategoryName();
    }
}

    /** Internal state: display name (String). */
    private final String displayName;
    private final Category category;
    /** Internal state: description (String). */
    private final String description;

    DisplayMode(String displayName, Category category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }

    /**
     * Get display name.
     *
     * @return the resulting computation or state reference
     */
    public String getDisplayName() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode." + name().toLowerCase() + ".name", displayName);
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode." + name().toLowerCase() + ".desc", description);
    }

    /**
     * Get metric id.
     *
     * @return the resulting computation or state reference
     */
    public String getMetricId() {
        switch (this) {
            case TEMPERATURE: return "temperature";
            case WATER: return "potableWater";
            case ENERGY_CAPACITY: return "energyCaptured";
            case ENTROPY_POLLUTION: return "entropyPollution";
            case POPULATION: return "population";
            case HEALTH_LIFE_EXPECTANCY: return "lifeExpectancy";
            case EDUCATION_LEVEL: return "educationLevel";
            case FOOD: return "foodPerCapita";
            case MINERAL_RESOURCES: return "resourceDepletion";
            case TECHNOLOGY: return "avgTechLevel";
            case INEQUALITY: return "gini";
            case ASABIYYAH: return "asabiyyah";
            case HAPPINESS: return "happiness";
            case CONFLICT: return "conflict";
            case INSTITUTIONAL_MATURITY: return "institutionalMaturity";
            case GDP_WEALTH: return "gdp";
            case ELITE_DENSITY: return "eliteOverproduction";
            case COLLECTIVE_MEMORY: return "collectiveMemory";
            case COLLAPSE_RISK: return "collapseRisk";
            case OCEAN_PH: return "oceanPh";
            case PERMAFROST: return "permafrost";
            case HABITAT_INFRASTRUCTURE: return "habitatInfrastructure";
            default: return "population";
        }
    }

    /**
     * From metric id.
     *
     * @param metricId the metric id parameter (String)
     * @return the resulting computation or state reference
     */
    public static DisplayMode fromMetricId(String metricId) {
        if (metricId == null) return POPULATION;
        for (DisplayMode dm : values()) {
            if (dm.getMetricId().equalsIgnoreCase(metricId) || dm.name().equalsIgnoreCase(metricId)) {
                return dm;
            }
        }
        return POPULATION;
    }

    public enum EncodingType {
        SCALAR_1D("1D Scalaire (Continu)"),
        ID_24BIT_CATEGORICAL("ID 24-bits (CatÃ©goriel RVB)");

        /** Internal state: default label (String). */
        private final String defaultLabel;
        EncodingType(String defaultLabel) { this.defaultLabel = defaultLabel; }
        /**
         * Get label.
         *
         * @return the resulting computation or state reference
         */
        public String getLabel() {
            return org.ether.society.i18n.I18n.getOrDefault("displaymode.encoding." + name().toLowerCase(), defaultLabel);
        }
    }

    /**
     * Get encoding type.
     *
     * @return the resulting computation or state reference
     */
    public EncodingType getEncodingType() {
        return switch (this) {
            case BIOME, CULTURE, POLITICAL -> EncodingType.ID_24BIT_CATEGORICAL;
            default -> EncodingType.SCALAR_1D;
        };
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        return displayName;
    }
}


