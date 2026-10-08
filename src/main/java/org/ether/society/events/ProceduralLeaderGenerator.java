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
    private static final String[] LEADER_TITLES_SAGE = {"Prophète", "Sage Éclairé", "Patriarche", "Guide Spirituel", "Maître Moral"};
    private static final String[] LEADER_TITLES_PURGER = {"Autocrate", "Inquisiteur", "Purificateur", "Dictateur Central", "Commandeur"};
    private static final String[] LEADER_TITLES_CHRONICLER = {"Philosophe", "Chroniqueur Historique", "Grand Érudit", "Astronome Royal", "Archiviste Suprême"};

    /*
     * Set seed.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralLeaderGenerator}.
     *
     * @param seed the seed parameter (long)
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    /*
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

        // Filter candidate cells to populated land cells, sorted descending by population density
        List<H3Cell> populated = cells.stream()
                .filter(c -> (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                        && c.getPopulation() != null && c.getPopulation() > 0)
                .sorted((a, b) -> Integer.compare(b.getPopulation(), a.getPopulation()))
                .toList();
        if (populated.isEmpty()) {
            return null; // A historical leader cannot emerge in an unpopulated world or in the ocean
        }

        // Leaders and historical figures emerge in core demographic centers and urban/agrarian hubs (top 20% highest density cells)
        int topK = Math.max(1, (int) Math.ceil(populated.size() * 0.20));
        List<H3Cell> topHubs = populated.subList(0, topK);

        // Roulette-wheel selection among top hubs proportional to population
        long totalHubPop = 0;
        for (H3Cell hub : topHubs) {
            totalHubPop += Math.max(1, hub.getPopulation());
        }
        long randWeight = (long) (random.nextDouble() * totalHubPop);
        long cumWeight = 0;
        H3Cell originCell = topHubs.get(0);
        for (H3Cell hub : topHubs) {
            cumWeight += Math.max(1, hub.getPopulation());
            if (cumWeight >= randWeight) {
                originCell = hub;
                break;
            }
        }

        // Determine Archetype based on local physical and sociological state
        LeaderArchetype archetype = determineArchetype(originCell);

        // Active executive leaders have high magnitude (5.0 to 9.5); informative chroniclers/sages have moderate cultural magnitude (2.0 to 4.5)
        double magnitude;
        if (archetype.isExecutiveLeader()) {
            double rawMag = 5.5 + (random.nextGaussian() * 1.2) + (random.nextDouble() * 2.0);
            magnitude = Math.max(4.5, Math.min(10.0, rawMag));
        } else {
            double rawMag = 2.5 + (random.nextDouble() * 2.0);
            magnitude = Math.max(1.5, Math.min(5.0, rawMag));
        }

        int duration = (int) (archetype.getDefaultDurationYears() * (0.7 + (random.nextDouble() * 0.6)));
        double radiusKm = 300.0 + (magnitude * 150.0);

        String id = "PROC_LEADER_" + year + "_" + Math.abs(random.nextInt(10000));
        String title = generateTitle(archetype);
        String name = String.format(org.ether.society.i18n.I18n.getOrDefault("leader.proc.name_format", "%s (Year %d)"), title, year);
        String desc = String.format(org.ether.society.i18n.I18n.getOrDefault("leader.proc.desc_format", "Emergence of a major historical figure (%s) with macro-regional impact."), archetype.getDisplayName());

        HistoricalIntervention intervention = new HistoricalIntervention(
                id, name, desc, year, duration,
                originCell.getLatitude(), originCell.getLongitude(),
                radiusKm, archetype, magnitude
        );

        return intervention;
    }

    // Helper subroutine: determine archetype - internal state computation & bounds checking
    private LeaderArchetype determineArchetype(H3Cell cell) {
        double r = random.nextDouble();
        Biome biome = cell.getBiome();

        // Nomadic / Savannah / Plains / Hills encourage conquerors or sages
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

        // Balanced distribution across all archetypes including intellectual chroniclers
        if (r < 0.18) return LeaderArchetype.MILITARY_CONQUEROR;
        if (r < 0.35) return LeaderArchetype.INFRASTRUCTURE_BUILDER;
        if (r < 0.52) return LeaderArchetype.INSTITUTIONAL_REFORMER;
        if (r < 0.68) return LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
        if (r < 0.82) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        if (r < 0.92) return LeaderArchetype.INTELLECTUAL_CHRONICLER;
        return LeaderArchetype.TOTALITARIAN_PURGER;
    }

    // Helper subroutine: generate title - internal state computation & bounds checking
    private String generateTitle(LeaderArchetype archetype) {
        int idx = random.nextInt(5) + 1;
        String key = "leader.title." + archetype.name().toLowerCase() + "." + idx;
        return org.ether.society.i18n.I18n.getOrDefault(key, switch (archetype) {
            case MILITARY_CONQUEROR -> LEADER_TITLES_CONQUEROR[random.nextInt(LEADER_TITLES_CONQUEROR.length)];
            case INFRASTRUCTURE_BUILDER -> LEADER_TITLES_BUILDER[random.nextInt(LEADER_TITLES_BUILDER.length)];
            case INSTITUTIONAL_REFORMER -> LEADER_TITLES_REFORMER[random.nextInt(LEADER_TITLES_REFORMER.length)];
            case HYDRAULIC_AGRARIAN_INNOVATOR -> LEADER_TITLES_HYDRAULIC[random.nextInt(LEADER_TITLES_HYDRAULIC.length)];
            case MORAL_RELIGIOUS_SAGE -> LEADER_TITLES_SAGE[random.nextInt(LEADER_TITLES_SAGE.length)];
            case TOTALITARIAN_PURGER -> LEADER_TITLES_PURGER[random.nextInt(LEADER_TITLES_PURGER.length)];
            case INTELLECTUAL_CHRONICLER -> LEADER_TITLES_CHRONICLER[random.nextInt(LEADER_TITLES_CHRONICLER.length)];
        });
    }
}
