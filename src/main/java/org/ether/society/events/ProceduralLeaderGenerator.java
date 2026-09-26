/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

import java.util.List;
import java.util.Random;

/**
 * Procedural Outlier Generator for emergent historical leaders, innovators, and reformers.
 * Used in sandbox/procedural worlds and open-ended simulations when deterministic Earth history is disabled.
 */
public class ProceduralLeaderGenerator {
    private final Random random = new Random();

    private static final String[] LEADER_TITLES_CONQUEROR = {"Général", "Seigneur de Guerre", "Khan", "Stratège", "Conquérant", "Hégémon"};
    private static final String[] LEADER_TITLES_BUILDER = {"Grand Architecte", "Bâtisseur", "Consul", "Empereur des Travaux", "Penseur Urbain"};
    private static final String[] LEADER_TITLES_REFORMER = {"Législateur", "Grand Chancelier", "Codificateur", "Archonte", "Ministre Réformateur"};
    private static final String[] LEADER_TITLES_HYDRAULIC = {"Maître des Eaux", "Ingénieur Agraire", "Canalisateur", "Pionnier des Moissons"};
    private static final String[] LEADER_TITLES_SAGE = {"Prophète", "Sage Éclairé", "Patriarche", "Guide Spirituel", "Philosophe Errant"};
    private static final String[] LEADER_TITLES_PURGER = {"Autocrate", "Inquisiteur", "Purificateur", "Dictateur Central", "Commandeur"};

    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * Checks whether an emergent historical outlier should spawn this year.
     * @param year current simulation year
     * @param totalPopulation total population in world
     * @param cells land/populated cells
     * @return a new HistoricalIntervention or null if no outlier emerges
     */
    public HistoricalIntervention evaluateEmergence(int year, long totalPopulation, List<H3Cell> cells) {
        if (cells == null || cells.isEmpty() || totalPopulation < 2000) {
            return null;
        }

        // Base annual probability scales with world population complexity (roughly 1 outlier per 30 to 70 years)
        double baseProb = Math.min(0.04, 0.01 + (totalPopulation / 100_000_000.0));
        if (random.nextDouble() > baseProb) {
            return null;
        }

        // Select a suitable populated cell as the epicenter of emergence
        List<H3Cell> populated = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 50)
                .toList();
        H3Cell originCell = !populated.isEmpty() ? populated.get(random.nextInt(populated.size())) : cells.get(random.nextInt(cells.size()));

        // Determine Archetype based on local physical and sociological state
        LeaderArchetype archetype = determineArchetype(originCell);

        // Magnitude drawn from a skewed distribution (mean 6.2, rare legendary > 8.5)
        double rawMag = 4.5 + (random.nextGaussian() * 1.5) + (random.nextDouble() * 2.0);
        double magnitude = Math.max(1.5, Math.min(10.0, rawMag));

        int duration = (int) (archetype.getDefaultDurationYears() * (0.7 + (random.nextDouble() * 0.6)));
        double radiusKm = 400.0 + (magnitude * 150.0);

        String id = "PROC_LEADER_" + year + "_" + Math.abs(random.nextInt(10000));
        String title = generateTitle(archetype);
        String name = title + " d'Émergence An " + year;
        String desc = "Émergence d'une figure majeure (" + archetype.getDisplayName() + ") à impact continental.";

        HistoricalIntervention intervention = new HistoricalIntervention(
                id, name, desc, year, duration,
                originCell.getLatitude(), originCell.getLongitude(),
                radiusKm, archetype, magnitude
        );

        return intervention;
    }

    private LeaderArchetype determineArchetype(H3Cell cell) {
        double r = random.nextDouble();
        Biome biome = cell.getBiome();

        // Nomadic / Savannah / Plains / Hills encourage conquerors
        if (biome == Biome.SAVANNAH || biome == Biome.PLAINS || biome == Biome.TUNDRA || biome == Biome.HILLS) {
            if (r < 0.45) return LeaderArchetype.MILITARY_CONQUEROR;
            if (r < 0.70) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        }

        // River / agricultural / lowlands encourage hydraulic & builders
        if (cell.getElevation() != null && cell.getElevation() < 400 && cell.getWaterResource() != null && cell.getWaterResource() > 0.3) {
            if (r < 0.35) return LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
            if (r < 0.65) return LeaderArchetype.INFRASTRUCTURE_BUILDER;
        }

        // High Gini index / inequality encourages purgers or moral sages
        if (cell.getGiniIndex() != null && cell.getGiniIndex() > 0.5) {
            if (r < 0.35) return LeaderArchetype.TOTALITARIAN_PURGER;
            if (r < 0.70) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        }

        // Default balanced distribution
        if (r < 0.20) return LeaderArchetype.MILITARY_CONQUEROR;
        if (r < 0.40) return LeaderArchetype.INFRASTRUCTURE_BUILDER;
        if (r < 0.60) return LeaderArchetype.INSTITUTIONAL_REFORMER;
        if (r < 0.75) return LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
        if (r < 0.90) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        return LeaderArchetype.TOTALITARIAN_PURGER;
    }

    private String generateTitle(LeaderArchetype archetype) {
        return switch (archetype) {
            case MILITARY_CONQUEROR -> LEADER_TITLES_CONQUEROR[random.nextInt(LEADER_TITLES_CONQUEROR.length)];
            case INFRASTRUCTURE_BUILDER -> LEADER_TITLES_BUILDER[random.nextInt(LEADER_TITLES_BUILDER.length)];
            case INSTITUTIONAL_REFORMER -> LEADER_TITLES_REFORMER[random.nextInt(LEADER_TITLES_REFORMER.length)];
            case HYDRAULIC_AGRARIAN_INNOVATOR -> LEADER_TITLES_HYDRAULIC[random.nextInt(LEADER_TITLES_HYDRAULIC.length)];
            case MORAL_RELIGIOUS_SAGE -> LEADER_TITLES_SAGE[random.nextInt(LEADER_TITLES_SAGE.length)];
            case TOTALITARIAN_PURGER -> LEADER_TITLES_PURGER[random.nextInt(LEADER_TITLES_PURGER.length)];
        };
    }
}
