/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.i18n.I18n;

/**
 * Fundamental historical and cliodynamic archetypes of contingency leaders and reformers.
 * Each archetype defines characteristic systemic modifiers on territorial cells and sovereign nations.
 */
public enum LeaderArchetype {
    /**
     * Rapid military conquest, nomadic unification, or unprecedented strategic campaign.
     * High expansion speed, low friction, but high succession crisis risk upon death.
     * Examples: Alexander the Great, Genghis Khan, Napoleon Bonaparte, Khalid ibn al-Walid.
     */
    MILITARY_CONQUEROR(
            "leader.archetype.military_conqueror",
            "leader.archetype.military_conqueror.desc",
            "⚔️",
            20,
            7.5
    ),

    /**
     * Imperial road networks, fortifications, aqueducts, and postal relays.
     * Drastic reduction in movement friction and long-term capital boost.
     * Examples: Cyrus the Great, Augustus Caesar, Qin Shi Huang (roads/Great Wall), Incan Qhapaq Ñan.
     */
    INFRASTRUCTURE_BUILDER(
            "leader.archetype.infrastructure_builder",
            "leader.archetype.infrastructure_builder.desc",
            "🏛️",
            30,
            6.5
    ),

    /**
     * Legal codification, fiscal restructuring, anti-corruption, and institutional stabilization.
     * Boosts state capacity and reduces political instability and Gini index.
     * Examples: Hammurabi, Solon, Justinian I, Charlemagne, Colbert.
     */
    INSTITUTIONAL_REFORMER(
            "leader.archetype.institutional_reformer",
            "leader.archetype.institutional_reformer.desc",
            "📜",
            25,
            6.0
    ),

    /**
     * Mega-hydraulic engineering, flood canalization, agricultural terrace networks, crop diversification.
     * Expands accessible aquifers, soil fertility, and local carrying capacity.
     * Examples: Yu the Great, Sui Wendi (Grand Canal), Sejong the Great, Nabataean water engineers.
     */
    HYDRAULIC_AGRARIAN_INNOVATOR(
            "leader.archetype.hydraulic_agrarian_innovator",
            "leader.archetype.hydraulic_agrarian_innovator.desc",
            "🌾",
            35,
            7.0
    ),

    /**
     * Moral universalism, religious codification, pan-ethnic solidarity, and non-violence.
     * Maximizes social cohesion (Asabiyyah) and suppresses inter-factional civil wars.
     * Examples: Ashoka the Great, Siddhartha Gautama, Confucius, Marcus Aurelius, Akbar the Great.
     */
    MORAL_RELIGIOUS_SAGE(
            "leader.archetype.moral_religious_sage",
            "leader.archetype.moral_religious_sage.desc",
            "🕊️",
            40,
            6.5
    ),

    /**
     * Ideological purification, aggressive central autocracy, violent elite purges, fanatic mobilization.
     * Drastically reduces elite overproduction but inflicts human capital loss and extreme geopolitical volatility.
     * Examples: Akhenaten, Qin Shi Huang (book burnings), Robespierre, Adolf Hitler, Pol Pot.
     */
    TOTALITARIAN_PURGER(
            "leader.archetype.totalitarian_purger",
            "leader.archetype.totalitarian_purger.desc",
            "🔥",
            15,
            8.0
    );

    /** Internal state: name key (String). */
    private final String nameKey;
    /** Internal state: desc key (String). */
    private final String descKey;
    /** Internal state: icon (String). */
    private final String icon;
    /** Internal state: default duration years (int). */
    private final int defaultDurationYears;
    /** Internal state: default magnitude (double). */
    private final double defaultMagnitude;

    LeaderArchetype(String nameKey, String descKey, String icon, int defaultDurationYears, double defaultMagnitude) {
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.icon = icon;
        this.defaultDurationYears = defaultDurationYears;
        this.defaultMagnitude = defaultMagnitude;
    }

    /**
     * Get name key.
     *
     * @return the resulting computation or state reference
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * Get desc key.
     *
     * @return the resulting computation or state reference
     */
    public String getDescKey() {
        return descKey;
    }

    /**
     * Get icon.
     *
     * @return the resulting computation or state reference
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get default duration years.
     *
     * @return the resulting computation or state reference
     */
    public int getDefaultDurationYears() {
        return defaultDurationYears;
    }

    /**
     * Get default magnitude.
     *
     * @return the resulting computation or state reference
     */
    public double getDefaultMagnitude() {
        return defaultMagnitude;
    }

    /**
     * Get display name.
     *
     * @return the resulting computation or state reference
     */
    public String getDisplayName() {
        return icon + " " + I18n.getOrDefault(nameKey, name());
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return I18n.getOrDefault(descKey, name());
    }

    /**
     * Returns true if this archetype represents an active sovereign/executive ruler exerting high physical/state magnitude.
     */
    public boolean isExecutiveLeader() {
        return this == MILITARY_CONQUEROR || this == INFRASTRUCTURE_BUILDER
                || this == INSTITUTIONAL_REFORMER || this == HYDRAULIC_AGRARIAN_INNOVATOR
                || this == TOTALITARIAN_PURGER;
    }

    /**
     * Returns true if this archetype is primarily an informative/intellectual cultural milestone.
     */
    public boolean isInformativeMilestone() {
        return !isExecutiveLeader();
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        return getDisplayName();
    }
}
