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
 * @version 1.0.0-beta.1
 */
public enum DisplayMode {

    // --- 🌍 1. PHYSIQUE & CLIMAT ---
    BIOME("🗺️ Biomes & Relief Topographique", Category.PHYSICAL,
        "Cartographie des biomes écologiques (Forêts, Tundras, Déserts, Océans) et de l'altitude du relief H3."),
    TEMPERATURE("🌡️ Température & Climat", Category.PHYSICAL,
        "Affiche la température de surface (°C) calculée par l'insolation solaire, l'albédo local et le forçage radiatif."),
    PRECIPITATION("🌧️ Précipitations & Pluviométrie", Category.PHYSICAL,
        "Affiche la hauteur de précipitations annuelles (mm/an) reçue par la maille hexagonale H3."),
    WATER("💧 Ressources en Eau & Aquifères", Category.PHYSICAL,
        "Niveau des réserves d'eau douce (aquifères, rivières et lacs) disponibles par kilomètre carré."),
    ALBEDO("❄️ Albédo de Surface & Glaces", Category.PHYSICAL,
        "Coefficient de réflexion solaire (0.0 à 1.0) selon la couverture de glace, la végétation et les surfaces urbaines."),
    FRICTION("🧗 Friction de Déplacement", Category.PHYSICAL,
        "Coût d'effort thermodynamique et difficulté de déplacement à travers le relief, la pente et la végétation dense."),
    ENERGY_CAPACITY("⚡ Énergie Captée (Flux Solaire/Biomasse)", Category.PHYSICAL,
        "Flux d'énergie primaire capté (MW/km²) par l'insolation solaire, la biomasse et les infrastructures énergétiques."),
    ENTROPY_POLLUTION("☣️ Entropie & Empreinte Pollution", Category.PHYSICAL,
        "Taux de génération d'entropie thermodynamique, rejets polluants et dégradations toxiques de l'environnement."),
    SOIL_QUALITY("🌾 Fertilité & Qualité NPK des Sols", Category.PHYSICAL,
        "Teneur en nutriments organiques NPK (Azote, Phosphore, Potassium) et fertilité des sols agricoles."),
    BIODIVERSITY("🌿 Biodiversité Sauvage Conservée", Category.PHYSICAL,
        "Proportion de biodiversité sauvage préservée et intégrité des réseaux trophiques floristiques et fauniques."),
    OCEAN_PH("🌊 Acidité Océanique & pH Marin", Category.PHYSICAL,
        "Niveau de pH des eaux océaniques de surface et impact de la dissolution du CO2 atmosphérique."),
    PERMAFROST("🧊 Pergélisol & Déstabilisation Méthane", Category.PHYSICAL,
        "Vulnérabilité et dégel du pergélisol boréal avec potentiel de dégazage de méthane."),

    // --- 👥 2. DÉMOGRAPHIE & SANTÉ ---
    POPULATION("👥 Densité de Population Active", Category.DEMOGRAPHICS,
        "Nombre d'habitants actifs résidant dans la maille hexagonale H3."),
    MIGRATION("🚶 Flux & Pression Migratoire", Category.DEMOGRAPHICS,
        "Vecteurs et pression des flux migratoires inter-cellules stimulés par les différentiels de bien-être et de ressources."),
    AGE_PYRAMID("👴 Pyramide des Âges (Séniors)", Category.DEMOGRAPHICS,
        "Proportion et concentration des cohortes d'aînés et séniors (>60 ans) dans la structure démographique."),
    EPIDEMIC("☣️ Foyers Épidémiques", Category.DEMOGRAPHICS,
        "Niveau de prévalence et propagation des agents pathogènes zoonotiques et bio-moléculaires."),
    HEALTH_LIFE_EXPECTANCY("🏥 Espérance de Vie & Santé", Category.DEMOGRAPHICS,
        "Espérance de vie moyenne théorique et résistance immunitaire globale des populations de la cellule."),
    EDUCATION_LEVEL("🎓 Niveau d'Instruction & Éducation", Category.DEMOGRAPHICS,
        "Part de la population maîtrisant les compétences techniques, l'écriture et le capital d'instruction."),

    // --- 🌱 3. ÉCOLOGIE & PRESSION MALTHUSIENNE ---
    MALTHUSIAN_PRESSURE("🌱 Empreinte Écologique & Stress Malthusien (Pop/K)", Category.ECOLOGY,
        "Ratio démographique rapporté à la capacité portante écologique localement disponible (Population / K)."),
    CAPACITY("🌾 Capacité Portante Max (K)", Category.ECOLOGY,
        "Capacité portante maximale théorique (K) mesurée en nombre maximal d'habitants soutenables sans dégradation."),
    FOOD("🍞 Stocks Alimentaires & Biomasse", Category.ECOLOGY,
        "Volume total de biomasse consommable et de stocks céréaliers stockés dans la maille."),
    WOOD("🌲 Ressources Forestières & Bois", Category.ECOLOGY,
        "Stock de biomasse lignocellulosique forestière exploitable pour le chauffage, la construction et l'outillage."),

    // --- ⛏️ 4. RESSOURCES MINÉRALES & EXPLOITATION ---
    MINERAL_RESOURCES("💎 Gisements Miniers & Minerais", Category.MINING,
        "Richesse et concentration en gisements métalliques, minerais de fer, cuivre et combustibles fossiles sous-jacents."),
    MINING_EXPLOITATION("⛏️ Exploitation Minière & Mines Actives", Category.MINING,
        "Intensité de l'activité d'extraction minière et vitesse d'épuisement des filons minéraux crustaux."),

    // --- 🏛️ 5. ÉCONOMIE, SOCIÉTÉ & CLIODYNAMIQUE ---
    TECHNOLOGY("⚙️ Niveau Technologique & Capital", Category.SOCIETY_POLITICS,
        "Indice de niveau technologique (Niveau Tech 0.0 à 10.0+) et stock de machines/capital bâti par habitant."),
    CULTURE("🎭 Vecteur d'Identité Culturelle", Category.SOCIETY_POLITICS,
        "Dominance et propagation du vecteur culturel, linguistique et religieux des communautés."),
    POLITICAL("👑 Frontières Politiques & Dominions", Category.SOCIETY_POLITICS,
        "Délimitation des frontières territoriales, zones d'influence étatiques et contrôle souverain des cités."),
    INEQUALITY("⚖️ Indice d'Inégalité Économique (Gini)", Category.SOCIETY_POLITICS,
        "Coefficient de Gini local (0.0 à 1.0) mesurant la disparité d'accumulation des richesses et du capital."),
    ASABIYYAH("⚔ Cohésion Asabiyyah & Instabilité", Category.SOCIETY_POLITICS,
        "Indice Khaldounien de cohésion sociale, solidarité de groupe et vulnérabilité aux crises de factionnalisme."),
    FLUX("🐫 Routes Commerciales & Flux", Category.SOCIETY_POLITICS,
        "Volume et intensité des flux de marchandises et routes commerciales terrestres/maritimes en transit."),
    HAPPINESS("😊 Indice de Bonheur & Bien-être", Category.SOCIETY_POLITICS,
        "Indice synthétique de satisfaction de vie, bien-être psychologique et sérénité sociale (0 à 100%)."),
    CONFLICT("⚔ Frictions & Taux de Conflits", Category.SOCIETY_POLITICS,
        "Taux de friction, violence inter-groupe, banditisme et opérations de guerre cinétique."),
    INSTITUTIONAL_MATURITY("🏛️ Maturité Institutionnelle & Cités", Category.SOCIETY_POLITICS,
        "Degré de complexité administrative, juridique et d'organisation des cités-états et gouvernements."),
    GDP_WEALTH("💰 PIB Local & Capital Bâti", Category.SOCIETY_POLITICS,
        "Produit Intérieur Brut (PIB) local produit par an et masse totale de capital d'infrastructures bâties."),
    ELITE_DENSITY("👑 Formation & Densité d'Élites", Category.SOCIETY_POLITICS,
        "Ratio de surproduction élitaire et densité de la classe dirigeante par rapport aux producteurs."),
    COLLECTIVE_MEMORY("🧠 Capital Informationnel & Mémoire", Category.SOCIETY_POLITICS,
        "Volume de connaissances archivées, brevets et données conservées dans le capital d'information (TB/bits)."),
    COLLAPSE_RISK("📉 Risque d'Effondrement Systémique", Category.SOCIETY_POLITICS,
        "Probabilité mathématique d'effondrement systémique ou de basculement irréversible de la cellule.");

    public enum Category {
        PHYSICAL("🌍 PHYSIQUE & CLIMAT"),
        DEMOGRAPHICS("👥 DÉMOGRAPHIE & SANTÉ"),
        ECOLOGY("🌱 ÉCOLOGIE & PRESSION MALTHUSIENNE"),
        MINING("⛏️ RESSOURCES MINÉRALES & EXPLOITATION"),
        SOCIETY_POLITICS("🏛️ ÉCONOMIE, SOCIÉTÉ & CLIODYNAMIQUE");

        private final String categoryName;

        Category(String categoryName) {
            this.categoryName = categoryName;
        }

    public String getCategoryName() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode.category." + name().toLowerCase(), categoryName);
    }

    @Override
    public String toString() {
        return getCategoryName();
    }
}

    private final String displayName;
    private final Category category;
    private final String description;

    DisplayMode(String displayName, Category category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }

    public String getDisplayName() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode." + name().toLowerCase() + ".name", displayName);
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault("displaymode." + name().toLowerCase() + ".desc", description);
    }

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
            default: return "population";
        }
    }

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
        ID_24BIT_CATEGORICAL("ID 24-bits (Catégoriel RVB)");

        private final String defaultLabel;
        EncodingType(String defaultLabel) { this.defaultLabel = defaultLabel; }
        public String getLabel() {
            return org.ether.society.i18n.I18n.getOrDefault("displaymode.encoding." + name().toLowerCase(), defaultLabel);
        }
    }

    public EncodingType getEncodingType() {
        return switch (this) {
            case BIOME, CULTURE, POLITICAL -> EncodingType.ID_24BIT_CATEGORICAL;
            default -> EncodingType.SCALAR_1D;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}


