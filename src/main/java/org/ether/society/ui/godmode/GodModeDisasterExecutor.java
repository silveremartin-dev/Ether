/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.godmode;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.ether.society.events.ActiveEvent;
import org.ether.society.events.HistoricalIntervention;
import org.ether.society.events.LeaderArchetype;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.HabitatType;

import java.util.Locale;

/**
 * Execution engine for physical forcing, disaster injection, and terraforming actions in God Mode.
 *
 * <p>Directly modifies cellular simulation states (population, biomass, aquifers, climate,
 * infrastructure integrity) following geophysical laws and cliodynamic shocks.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class GodModeDisasterExecutor {

    private GodModeDisasterExecutor() {
        // Utility class
    }

    /**
     * Finds the closest simulation cell to the specified latitude and longitude coordinates.
     */
    public static H3Cell findNearestCell(H3SimulationEngine engine, double lat, double lng) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) return null;
        H3Cell nearest = null;
        double minD = Double.MAX_VALUE;
        for (H3Cell c : engine.getCells()) {
            double d = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
            if (d < minD) {
                minD = d;
                nearest = c;
            }
        }
        return nearest;
    }

    /**
     * Checks if the event type is strictly terrestrial (cannot occur in deep ocean).
     */
    public static boolean isTerrestrialEventType(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "FLOOD", "DROUGHT", "FAMINE", "VOLCANO", "EARTHQUAKE", "PANDEMIC",
                 "RENAISSANCE_BOOM", "TECH_SINGULARITY", "CYBER_ATTACK", "ECONOMIC_CRASH",
                 "BIODIVERSITY_COLLAPSE" -> true;
            default -> false;
        };
    }

    /**
     * Checks if the event type requires oceanic or coastal coordinates.
     */
    public static boolean isOceanicEventType(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "TSUNAMI" -> true;
            default -> false;
        };
    }

    /**
     * Executes physical, climatic, or sociological forcing across spatial simulation cells.
     */
    public static void executePhysicalForcing(H3SimulationEngine engine, String type, double mag, double lat, double lng, double radiusKm) {
        if (type == null) return;
        switch (type) {
            case "VOLCANO" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag);
            case "NUCLEAR_STRIKE" -> {
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + mag * 1.5);
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blastFactor = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            c.setPopulation((int) (c.getPopulation() * (1.0 - 0.90 * blastFactor)));
                            c.setFoodResource(Math.max(0.0, (c.getFoodResource() != null ? c.getFoodResource() : 0.0) * (1.0 - 0.80 * blastFactor)));
                            c.setTechnologyLevel(Math.max(0.0, (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0) * (1.0 - 0.70 * blastFactor)));
                            c.setPollutionLevel(Math.min(1.0, (c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0) + 0.9 * blastFactor));
                        }
                    }
                }
            }
            case "HEATWAVE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTemperature((c.getTemperature() != null ? c.getTemperature() : 15.0) + mag * 2.0);
                    }
                }
            }
            case "NUCLEAR_WINTER", "ICE_AGE" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag * 2.5);
            case "METEOR" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag * 3.5);
            case "GEOENGINEERING" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(Math.max(0, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() - mag));
            case "FAMINE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setFoodResource(Math.max(0.0, (c.getFoodResource() != null ? c.getFoodResource() : 100.0) * (1.0 - mag * 0.08)));
                    }
                }
            }
            case "FLOOD" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            if (c.getPopulation() != null && c.getPopulation() > 0) {
                                c.setPopulation((int) (c.getPopulation() * (1.0 - 0.20 * blast)));
                            }
                            if (c.getFoodResource() != null) {
                                c.setFoodResource(Math.max(0.0, c.getFoodResource() * (1.0 - 0.50 * blast)));
                            }
                        }
                    }
                }
            }
            case "DROUGHT" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 2.0) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 2.0)));
                            if (c.getWaterResource() != null) {
                                c.setWaterResource(Math.max(0.0, c.getWaterResource() * (1.0 - 0.70 * blast)));
                            }
                            if (c.getFoodResource() != null) {
                                c.setFoodResource(Math.max(0.0, c.getFoodResource() * (1.0 - 0.40 * blast)));
                            }
                        }
                    }
                }
            }
            case "TSUNAMI", "EARTHQUAKE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 2.0) {
                            int pop = c.getPopulation();
                            c.setPopulation((int) (pop * Math.max(0.1, 1.0 - (mag * 0.15))));
                        }
                    }
                }
            }
            case "PANDEMIC", "BIODIVERSITY_COLLAPSE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        int pop = c.getPopulation();
                        c.setPopulation((int) (pop * Math.max(0.2, 1.0 - (mag * 0.05))));
                    }
                }
            }
            case "SOLAR_EMP", "CYBER_ATTACK", "ECONOMIC_CRASH" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTechnologyLevel(Math.max(0.0, (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 10.0) * (1.0 - mag * 0.05)));
                    }
                }
            }
            case "RENAISSANCE_BOOM", "TECH_SINGULARITY", "ALIEN_CONTACT" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTechnologyLevel((c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 10.0) + mag * 5.0);
                    }
                }
            }
            case "DOME_BREACH" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            c.setHabitatIntegrity(0.0);
                            int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                            int survivors = (int) Math.round(pop * (1.0 - 0.90 * blast));
                            c.setPopulation(survivors);
                            c.setBiomassHuman((double) survivors);
                            if (c.getFoodResource() != null) {
                                c.setFoodResource(c.getFoodResource() * (1.0 - 0.95 * blast));
                            }
                        }
                    }
                }
            }
            case "ECLSS_FAILURE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            double curIntegrity = c.getHabitatIntegrity() != null ? c.getHabitatIntegrity() : 1.0;
                            c.setHabitatIntegrity(Math.max(0.05, curIntegrity - 0.70 * blast));
                            int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                            int survivors = (int) Math.round(pop * (1.0 - 0.65 * blast));
                            c.setPopulation(survivors);
                            c.setBiomassHuman((double) survivors);
                            if (c.getFoodResource() != null) {
                                c.setFoodResource(c.getFoodResource() * (1.0 - 0.80 * blast));
                            }
                        }
                    }
                }
            }
            case "AEROSTAT_PUNCTURE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            c.setHabitatIntegrity(0.0);
                            c.setHabitatType(HabitatType.NONE);
                            int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                            int survivors = (int) Math.round(pop * (1.0 - 0.95 * blast));
                            c.setPopulation(survivors);
                            c.setBiomassHuman((double) survivors);
                        }
                    }
                }
            }
            case "LAVA_TUBE_COLLAPSE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 1.5) {
                            double blast = Math.max(0.0, 1.0 - (dist / (mag * 1.5)));
                            c.setHabitatIntegrity(0.0);
                            int pop = c.getPopulation() != null ? c.getPopulation() : 0;
                            int survivors = (int) Math.round(pop * (1.0 - 0.80 * blast));
                            c.setPopulation(survivors);
                            c.setBiomassHuman((double) survivors);
                            if (c.getResourceCapital() != null) {
                                c.setResourceCapital(c.getResourceCapital() * (1.0 - 0.90 * blast));
                            }
                        }
                    }
                }
            }
            case "LEADER_MILITARY_CONQUEROR", "LEADER_INFRASTRUCTURE_BUILDER", "LEADER_INSTITUTIONAL_REFORMER",
                 "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR", "LEADER_MORAL_RELIGIOUS_SAGE", "LEADER_TOTALITARIAN_PURGER" -> {
                LeaderArchetype arch = switch (type) {
                    case "LEADER_MILITARY_CONQUEROR" -> LeaderArchetype.MILITARY_CONQUEROR;
                    case "LEADER_INFRASTRUCTURE_BUILDER" -> LeaderArchetype.INFRASTRUCTURE_BUILDER;
                    case "LEADER_INSTITUTIONAL_REFORMER" -> LeaderArchetype.INSTITUTIONAL_REFORMER;
                    case "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR" -> LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR;
                    case "LEADER_MORAL_RELIGIOUS_SAGE" -> LeaderArchetype.MORAL_RELIGIOUS_SAGE;
                    default -> LeaderArchetype.TOTALITARIAN_PURGER;
                };
                int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 0;
                HistoricalIntervention hi = new HistoricalIntervention(
                    "GM_LEADER_" + System.currentTimeMillis(),
                    arch.getDisplayName() + " (God Mode)",
                    "Intervention divine d'une figure majeure sous mandat divin.",
                    currentYear,
                    arch.getDefaultDurationYears(),
                    lat, lng, radiusKm, arch, mag
                );
                if (engine != null && engine.getEventSystem() != null) {
                    engine.getEventSystem().injectCustomIntervention(hi);
                }
            }
            default -> {}
        }
    }

    /**
     * Applies demographic injection at epicenter coordinates.
     */
    public static H3Cell injectPopulationAtEpicenter(H3SimulationEngine engine, double lat, double lng, int popCount) {
        H3Cell nearest = findNearestCell(engine, lat, lng);
        if (nearest != null) {
            nearest.setPopulation(nearest.getPopulation() + popCount);
            if (engine.getWorldBuffer() != null && engine.getCells() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
        return nearest;
    }

    /**
     * Injects global agricultural food stocks into all simulation cells.
     */
    public static void injectGlobalFood(H3SimulationEngine engine, double amount) {
        if (engine != null && engine.getCells() != null) {
            for (H3Cell c : engine.getCells()) {
                c.setFoodResource((c.getFoodResource() != null ? c.getFoodResource() : 0.0) + amount);
            }
            if (engine.getWorldBuffer() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
    }

    /**
     * Executes cataclysmic mass extinction cutting world population by percentage.
     */
    public static void triggerMassExtinction(H3SimulationEngine engine, double fractionRemaining) {
        if (engine != null && engine.getCells() != null) {
            for (H3Cell c : engine.getCells()) {
                c.setPopulation((int) (c.getPopulation() * fractionRemaining));
            }
            if (engine.getWorldBuffer() != null) {
                DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
        }
    }

    /**
     * Record representing a local terraform brush modification outcome.
     */
    public record BrushOutcome(H3Cell cell, String type, String title, String description, double magnitude) {}

    /**
     * Applies a localized ecological or physical terraform brush to the nearest cell.
     */
    public static BrushOutcome applyTerraformBrush(H3SimulationEngine engine, int selectedIdx, double lat, double lng) {
        H3Cell nearest = findNearestCell(engine, lat, lng);
        if (nearest == null) return null;

        String brushType;
        String brushTitle;
        String brushDesc;
        double brushMag;

        switch (selectedIdx) {
            case 0 -> {
                nearest.setPopulation(nearest.getPopulation() + 50000);
                brushType = "POP_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.pop", "👥 Boost Population (+50 000 hab)");
                brushDesc = String.format(Locale.ROOT, "Ajout de +50 000 habitants à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 50000.0;
            }
            case 1 -> {
                nearest.setFoodResource((nearest.getFoodResource() != null ? nearest.getFoodResource() : 0.0) + 500.0);
                brushType = "AGRI_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.agri", "🌾 Injection Agricole & Silos (+500 t)");
                brushDesc = String.format(Locale.ROOT, "Apport de +500 t de céréales à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 500.0;
            }
            case 2 -> {
                nearest.setFreshwaterAquifer((nearest.getFreshwaterAquifer() != null ? nearest.getFreshwaterAquifer() : 0.0) + 2000.0);
                brushType = "WATER_RECHARGE";
                brushTitle = I18n.getOrDefault("godmode.brush.water", "🚰 Recharge Aquifère (+2 000 m³)");
                brushDesc = String.format(Locale.ROOT, "Recharge de nappe phréatique (+2 000 m³) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 2000.0;
            }
            case 3 -> {
                nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) + 10.0);
                brushType = "HEAT_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.heat", "🔥 Vague de Chaleur Locale (+10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Hausse thermique locale (+10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 10.0;
            }
            case 4 -> {
                nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) - 10.0);
                brushType = "COLD_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.cold", "❄️ Refroidissement Local (-10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Refroidissement local (-10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 10.0;
            }
            default -> {
                nearest.setPollutionLevel(0.0);
                brushType = "CLEAN_ECO";
                brushTitle = I18n.getOrDefault("godmode.brush.clean", "🧼 Nettoyage Écologique Intégral");
                brushDesc = String.format(Locale.ROOT, "Dépollution intégrale à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitude(), nearest.getLongitude());
                brushMag = 1.0;
            }
        }

        if (engine.getWorldBuffer() != null && engine.getCells() != null) {
            DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
        }

        return new BrushOutcome(nearest, brushType, brushTitle, brushDesc, brushMag);
    }
}
