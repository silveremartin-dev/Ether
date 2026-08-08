/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

/**
 * Display mode for H3 map visualization content.
 * Controls what data is visualized on the map cells, organized into logical categories.
 */
public enum DisplayMode {

    // --- 🌍 1. PHYSIQUE & CLIMAT ---
    BIOME("🗺️ Biomes & Relief Topographique", Category.PHYSICAL),
    TEMPERATURE("🌡️ Température & Climat", Category.PHYSICAL),
    WATER("💧 Ressources en Eau & Aquifères", Category.PHYSICAL),
    ALBEDO("❄️ Albédo de Surface & Glaces", Category.PHYSICAL),
    FRICTION("🧗 Friction de Déplacement", Category.PHYSICAL),

    // --- 👥 2. DÉMOGRAPHIE & DYNAMIQUES HUMAINES ---
    POPULATION("👥 Densité de Population Active", Category.DEMOGRAPHICS),
    MIGRATION("🚶 Flux & Pression Migratoire", Category.DEMOGRAPHICS),
    AGE_PYRAMID("👴 Pyramide des Âges (Séniors)", Category.DEMOGRAPHICS),
    EPIDEMIC("☣️ Foyers Épidémiques", Category.DEMOGRAPHICS),

    // --- 🌱 3. ÉCOLOGIE & PRESSION MALTHUSIENNE ---
    MALTHUSIAN_PRESSURE("🌱 Empreinte Écologique & Stress Malthusien (Pop/K)", Category.ECOLOGY),
    CAPACITY("🌾 Capacité Portante Max (K)", Category.ECOLOGY),
    FOOD("🍞 Stocks Alimentaires & Biomasse", Category.ECOLOGY),
    WOOD("🌲 Ressources Forestières & Bois", Category.ECOLOGY),

    // --- ⛏️ 4. RESSOURCES MINÉRALES & EXPLOITATION ---
    MINERAL_RESOURCES("💎 Gisements Miniers & Minerais", Category.MINING),
    MINING_EXPLOITATION("⛏️ Exploitation Minière & Mines Actives", Category.MINING),

    // --- 🏛️ 5. ÉCONOMIE, CULTURE & POLITIQUE ---
    TECHNOLOGY("⚙️ Niveau Technologique & Capital", Category.SOCIETY_POLITICS),
    CULTURE("🎭 Vecteur d'Identité Culturelle", Category.SOCIETY_POLITICS),
    POLITICAL("👑 Frontières Politiques & Dominions", Category.SOCIETY_POLITICS),
    INEQUALITY("⚖️ Indice d'Inégalité Économique (Gini)", Category.SOCIETY_POLITICS),
    ASABIYYAH("⚔️ Cohésion Asabiyyah & Instabilité", Category.SOCIETY_POLITICS),
    FLUX("🐫 Routes Commerciales & Flux", Category.SOCIETY_POLITICS);

    public enum Category {
        PHYSICAL("🌍 1. PHYSIQUE & CLIMAT"),
        DEMOGRAPHICS("👥 2. DÉMOGRAPHIE & SOCIÉTÉ"),
        ECOLOGY("🌱 3. ÉCOLOGIE & PRESSION MALTHUSIENNE"),
        MINING("⛏️ 4. RESSOURCES MINÉRALES & EXPLOITATION"),
        SOCIETY_POLITICS("🏛️ 5. ÉCONOMIE, CULTURE & POLITIQUE");

        private final String categoryName;

        Category(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        @Override
        public String toString() {
            return categoryName;
        }
    }

    private final String displayName;
    private final Category category;

    DisplayMode(String displayName, Category category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
