/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.util.DeterministicPRNG;

import java.util.Comparator;
import java.util.List;

/**
 * Procedural Outlier Generator for emergent historical leaders, innovators, and reformers.
 * All stochastic evaluations use stateless {@link DeterministicPRNG}.
 * Leaders are strictly constrained by the historical epoch:
 * - Paleolithic (-100,000 to -10,000 BP): Chieftains, master trackers, lithic innovators, shamans.
 * - Agrarian/Neolithic: Hydraulic innovators, city founders, legislators.
 * - Classical/Modern: Empire conquerors, legal reformers, industrial builders.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ProceduralLeaderGenerator {

    private long baseSeed = 42L;

    // Epoch-specific titles
    private static final String[] LEADER_TITLES_PALEOLITHIC_TRACKER = {"Chef de Clan", "Pisteur Émérite", "Maître du Silex", "Traqueur de Mégafaune", "Guide Nomade"};
    private static final String[] LEADER_TITLES_PALEOLITHIC_SHAMAN = {"Chamane des Grottes", "Gardien du Feu", "Vénérable Ancien", "Visionnaire Lithique"};

    private static final String[] LEADER_TITLES_CONQUEROR = {"Général", "Seigneur de Guerre", "Khan", "Stratège", "Conquérant", "Hégémon"};
    private static final String[] LEADER_TITLES_BUILDER = {"Grand Architecte", "Bâtisseur", "Consul", "Empereur des Travaux", "Penseur Urbain"};
    private static final String[] LEADER_TITLES_REFORMER = {"Législateur", "Grand Chancelier", "Codificateur", "Archonte", "Ministre Réformateur"};
    private static final String[] LEADER_TITLES_HYDRAULIC = {"Maître des Eaux", "Ingénieur Agraire", "Canalisateur", "Pionnier des Moissons"};
    private static final String[] LEADER_TITLES_SAGE = {"Prophète", "Sage Éclairé", "Patriarche", "Guide Spirituel", "Maître Moral"};
    private static final String[] LEADER_TITLES_PURGER = {"Autocrate", "Inquisiteur", "Purificateur", "Dictateur Central", "Commandeur"};

    /**
     * Sets the base seed for deterministic evaluations.
     *
     * @param seed base scenario seed
     */
    public void setSeed(long seed) {
        this.baseSeed = seed;
    }

    /**
     * Checks whether an emergent historical outlier should spawn this year.
     *
     * @param year current simulation year
     * @param totalPopulation total population in world
     * @param cells land/populated cells
     * @return a new HistoricalIntervention or null if no outlier emerges
     */
    public HistoricalIntervention evaluateEmergence(int year, long totalPopulation, List<H3Cell> cells) {
        if (cells == null || cells.isEmpty() || totalPopulation < 2000) {
            return null;
        }

        long stream = (long) year * 7919L;
        double baseProb = Math.min(0.04, 0.01 + (totalPopulation / 100_000_000.0));
        double pRoll = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 801L);
        if (pRoll > baseProb) {
            return null;
        }

        // Filter candidate cells to populated land cells, sorted descending by population density
        List<H3Cell> populated = cells.stream()
                .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                        && c.getPopulation() != null && c.getPopulation() > 0)
                .sorted((a, b) -> Integer.compare(b.getPopulation(), a.getPopulation()))
                .toList();

        if (populated.isEmpty()) {
            return null;
        }

        int topK = Math.max(1, (int) Math.ceil(populated.size() * 0.20));
        List<H3Cell> topHubs = populated.subList(0, topK);

        long totalHubPop = 0;
        for (H3Cell hub : topHubs) {
            totalHubPop += Math.max(1, hub.getPopulation());
        }
        double weightFrac = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 802L);
        long randWeight = (long) (weightFrac * totalHubPop);
        long cumWeight = 0;
        H3Cell originCell = topHubs.get(0);
        for (H3Cell hub : topHubs) {
            cumWeight += Math.max(1, hub.getPopulation());
            if (cumWeight >= randWeight) {
                originCell = hub;
                break;
            }
        }

        // Determine Archetype constrained by historical epoch
        LeaderArchetype archetype = determineArchetype(originCell, year);

        double zMag = DeterministicPRNG.nextGaussian(baseSeed, (long) year, stream);
        double uMag = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 804L);
        double rawMag = 5.5 + (zMag * 1.2) + (uMag * 2.0);
        double magnitude = Math.clamp(rawMag, 5.0, 9.5);

        double uDur = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 805L);
        int duration = (int) (archetype.getDefaultDurationYears() * (0.7 + (uDur * 0.6)));
        double radiusKm = 300.0 + (magnitude * 150.0);

        int leaderRandId = DeterministicPRNG.nextInt(baseSeed, (long) year, stream, 10000);
        String id = "PROC_LEADER_" + year + "_" + Math.abs(leaderRandId);
        String title = generateTitle(archetype, year);
        String name = org.ether.society.i18n.I18n.getOrDefault("leader.proc.name_format", "{0} (Year {1})", title, year);
        String desc = org.ether.society.i18n.I18n.getOrDefault("leader.proc.desc_format", "Emergence of a major historical figure ({1}) in Year {0} with macro-regional impact.", year, archetype.getDisplayName());

        return new HistoricalIntervention(
                id, name, desc, year, duration,
                originCell.getLatitude(), originCell.getLongitude(),
                radiusKm, archetype, magnitude
        );
    }

    private LeaderArchetype determineArchetype(H3Cell cell, int year) {
        long stream = (long) year * 7919L + (cell.getH3Index() != 0 ? cell.getH3Index() : 1L);
        double r = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 810L);

        // Deep Paleolithic: Strictly tribal chieftains, hunters, and spiritual sages
        if (year < -10000) {
            if (r < 0.55) return LeaderArchetype.MILITARY_CONQUEROR; // Clan war leader / megafauna hunt master
            return LeaderArchetype.MORAL_RELIGIOUS_SAGE; // Cave artist, shaman, oral keeper
        }

        Biome biome = cell.getBiome();

        // Nomadic / Savannah / Plains / Hills encourage conquerors or sages
        if (biome == Biome.SAVANNAH || biome == Biome.PLAINS || biome == Biome.TUNDRA || biome == Biome.HILLS) {
            if (r < 0.45) return LeaderArchetype.MILITARY_CONQUEROR;
            if (r < 0.70) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        }

        // River / agricultural / lowlands encourage hydraulic & builders (only if sedentary or Holocene)
        if (year >= -10000 && cell.getElevation() != null && cell.getElevation() < 400 && cell.getWaterResource() != null && cell.getWaterResource() > 0.3) {
            if (r < 0.35) return LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
            if (r < 0.65) return LeaderArchetype.INFRASTRUCTURE_BUILDER;
        }

        // High Gini index / inequality encourages purgers or moral sages
        if (cell.getGiniIndex() != null && cell.getGiniIndex() > 0.5) {
            if (r < 0.35) return LeaderArchetype.TOTALITARIAN_PURGER;
            if (r < 0.70) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        }

        // Balanced distribution across the 6 historical governance archetypes
        if (r < 0.20) return LeaderArchetype.MILITARY_CONQUEROR;
        if (r < 0.38) return LeaderArchetype.INFRASTRUCTURE_BUILDER;
        if (r < 0.56) return LeaderArchetype.INSTITUTIONAL_REFORMER;
        if (r < 0.74) return LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
        if (r < 0.88) return LeaderArchetype.MORAL_RELIGIOUS_SAGE;
        return LeaderArchetype.TOTALITARIAN_PURGER;
    }

    private String generateTitle(LeaderArchetype archetype, int year) {
        long stream = (long) year * 7919L + archetype.ordinal();
        if (year < -10000) {
            if (archetype == LeaderArchetype.MILITARY_CONQUEROR) {
                int idx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_PALEOLITHIC_TRACKER.length);
                return LEADER_TITLES_PALEOLITHIC_TRACKER[idx];
            } else {
                int idx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_PALEOLITHIC_SHAMAN.length);
                return LEADER_TITLES_PALEOLITHIC_SHAMAN[idx];
            }
        }

        int idx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream, 5) + 1;
        String key = "leader.title." + archetype.name().toLowerCase() + "." + idx;
        return org.ether.society.i18n.I18n.getOrDefault(key, switch (archetype) {
            case MILITARY_CONQUEROR -> LEADER_TITLES_CONQUEROR[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_CONQUEROR.length)];
            case INFRASTRUCTURE_BUILDER -> LEADER_TITLES_BUILDER[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_BUILDER.length)];
            case INSTITUTIONAL_REFORMER -> LEADER_TITLES_REFORMER[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_REFORMER.length)];
            case HYDRAULIC_AGRARIAN_INNOVATOR -> LEADER_TITLES_HYDRAULIC[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_HYDRAULIC.length)];
            case MORAL_RELIGIOUS_SAGE -> LEADER_TITLES_SAGE[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_SAGE.length)];
            case TOTALITARIAN_PURGER -> LEADER_TITLES_PURGER[DeterministicPRNG.nextInt(baseSeed, (long) year, stream, LEADER_TITLES_PURGER.length)];
        });
    }
}
