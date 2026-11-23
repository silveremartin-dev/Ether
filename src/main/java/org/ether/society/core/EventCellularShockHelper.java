/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.ether.society.model.Biome;
import org.ether.society.model.HabitatType;

import java.util.Locale;

/**
 * <h1>EventCellularShockHelper</h1>
 * <p>
 * Internal helper for computing cell-level physical shock attenuation,
 * global atmospheric forcing (soot/sulfate optical depth), and leader archetype impacts.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class EventCellularShockHelper {

    private EventCellularShockHelper() {
        // Utility class
    }

    /**
     * Calculates default impact radius in kilometers from typology and magnitude.
     *
     * @param type event typology
     * @param name descriptive title
     * @param mag event magnitude
     * @return radius in kilometers
     */
    public static double calculateDefaultRadiusKm(String type, String name, double mag) {
        String upperType = type != null ? type.toUpperCase(java.util.Locale.ROOT) : "";
        String lowerName = name != null ? name.toLowerCase(java.util.Locale.ROOT) : "";
        if (upperType.contains("PANDEMIC") || upperType.contains("PLAGUE") || upperType.contains("EPIDEMIC")) {
            boolean isRespiratory = lowerName.contains("influenza") || lowerName.contains("flu")
                    || lowerName.contains("grippe") || lowerName.contains("sars") || lowerName.contains("covid");
            return isRespiratory ? (180.0 * 111.32) : (85.0 * 111.32);
        }
        if (upperType.contains("VOLCANO") || upperType.contains("NUCLEAR")) {
            return Math.max(250.0, mag * 200.0);
        }
        if (upperType.contains("LOCUST") || upperType.contains("CROP_BLIGHT") || upperType.contains("BLIGHT")) {
            return Math.max(200.0, mag * 160.0);
        }
        if (upperType.contains("WILDLIFE")) {
            return Math.max(250.0, mag * 180.0);
        }
        if (upperType.contains("FISHERY")) {
            return Math.max(300.0, mag * 220.0);
        }
        if (upperType.contains("CYCLONE") || upperType.contains("HURRICANE") || upperType.contains("TYPHOON")) {
            return Math.max(200.0, mag * 150.0);
        }
        if (upperType.contains("WILDFIRE")) {
            return Math.max(150.0, mag * 120.0);
        }
        if (upperType.contains("EARTHQUAKE") || upperType.contains("TSUNAMI")) {
            return Math.max(150.0, mag * 120.0);
        }
        if (upperType.contains("TECH_SINGULARITY") || upperType.contains("RENAISSANCE")) {
            return 3000.0;
        }
        return Math.max(50.0, mag * 100.0);
    }

    /**
     * Applies atmospheric stratospheric aerosol or soot optical depth to global climate engines.
     *
     * @param type event typology
     * @param mag event magnitude
     * @param durationDays event duration in days
     */
    public static void applyAtmosphericGlobalForcing(String type, double mag, int durationDays) {
        applyAtmosphericGlobalForcing(org.ether.society.model.PhysicalShockType.fromString(type), mag, durationDays);
    }

    /**
     * Applies atmospheric aerosol injection, volcanic sulfur loading, and nuclear winter optical depth forcing.
     *
     * @param shockType event shock type
     * @param mag event magnitude
     * @param durationDays event duration in days
     */
    public static void applyAtmosphericGlobalForcing(org.ether.society.model.PhysicalShockType shockType, double mag, int durationDays) {
        if (shockType == null) return;
        switch (shockType) {
            case VOLCANO -> {
                double addedTau = 0.05 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedTau);
            }
            case NUCLEAR_STRIKE, NUCLEAR_WINTER -> {
                double addedSoot = 0.20 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedSoot);
            }
            case METEOR -> {
                double addedEjecta = 0.35 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedEjecta);
            }
            case GEOENGINEERING -> {
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        Math.max(0.0, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() - 0.10 * mag));
            }
            default -> {}
        }
    }

    /**
     * Applies localized cellular shock to a specific simulation cell and returns the lost population headcount.
     *
     * @param cell target cell
     * @param upperType uppercase event type
     * @param lowerName lowercase event name
     * @param mag event magnitude
     * @param attenuation distance attenuation factor [0.0, 1.0]
     * @param prevPop previous population count
     * @return lost population headcount
     */
    public static int applyCellularShock(
            H3Cell cell,
            String upperType,
            String lowerName,
            double mag,
            double attenuation,
            int prevPop) {

        int lostPop = 0;
        org.ether.society.model.PhysicalShockType shockType = org.ether.society.model.PhysicalShockType.fromString(upperType);

        switch (shockType) {
            case VOLCANO -> {
                double localTempDrop = 0.85 * (mag / 10.0) * attenuation;
                cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - localTempDrop));
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.45 * (mag / 10.0) * attenuation)));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.08 * (mag / 10.0) * attenuation)));
                }
                if (prevPop > 0 && attenuation > 0.7) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.05 * (mag / 10.0) * attenuation)));
                }
            }

            case EARTHQUAKE -> {
                double seismicIntensity = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.25 * seismicIntensity)));
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.08 * seismicIntensity)));
                }
            }

            case TSUNAMI -> {
                boolean isCoastalOrLow = (cell.getIsCoastal() != null && cell.getIsCoastal())
                        || cell.getBiome() == Biome.OCEAN
                        || cell.getBiome() == Biome.DEEP_OCEAN
                        || (cell.getElevation() != null && cell.getElevation() <= 25.0);

                if (isCoastalOrLow) {
                    double waveIntensity = (mag / 10.0) * attenuation;
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.40 * waveIntensity)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.60 * waveIntensity)));
                    }
                    if (prevPop > 0) {
                        lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.15 * waveIntensity)));
                    }
                }
            }

            case FLOOD -> {
                double floodIntensity = (mag / 10.0) * attenuation;
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.45 * floodIntensity)));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.10 * floodIntensity)));
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.03 * floodIntensity)));
                }
            }

            case DROUGHT, HEATWAVE, FAMINE -> {
                double droughtIntensity = (mag / 10.0) * attenuation;
                if (cell.getFreshwaterAquifer() != null) {
                    cell.setFreshwaterAquifer(Math.max(0.0, cell.getFreshwaterAquifer() * (1.0 - 0.50 * droughtIntensity)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.65 * droughtIntensity)));
                }
                if (shockType == org.ether.society.model.PhysicalShockType.HEATWAVE) {
                    cell.setTemperature(cell.getTemperatureOr(15.0) + 1.2 * mag * attenuation);
                }
                if (prevPop > 0 && shockType == org.ether.society.model.PhysicalShockType.FAMINE) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.06 * droughtIntensity)));
                }
            }

            case LOCUST_SWARM -> {
                double swarmIntensity = (mag / 10.0) * attenuation;
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.75 * swarmIntensity)));
                }
                if (cell.getBiomassAgriculture() != null) {
                    cell.setBiomassAgriculture(Math.max(0.0, cell.getBiomassAgriculture() * (1.0 - 0.80 * swarmIntensity)));
                }
                if (prevPop > 0 && attenuation > 0.6) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.02 * swarmIntensity)));
                }
            }

            case CROP_BLIGHT -> {
                double blightIntensity = (mag / 10.0) * attenuation;
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.70 * blightIntensity)));
                }
                if (cell.getBiomassAgriculture() != null) {
                    cell.setBiomassAgriculture(Math.max(0.0, cell.getBiomassAgriculture() * (1.0 - 0.85 * blightIntensity)));
                }
                if (prevPop > 0 && attenuation > 0.65) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.025 * blightIntensity)));
                }
            }

            case WILDLIFE_DIE_OFF -> {
                double wildlifeIntensity = (mag / 10.0) * attenuation;
                if (cell.getBiomassNatural() != null) {
                    cell.setBiomassNatural(Math.max(0.0, cell.getBiomassNatural() * (1.0 - 0.65 * wildlifeIntensity)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.35 * wildlifeIntensity)));
                }
                if (prevPop > 0 && attenuation > 0.75) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.012 * wildlifeIntensity)));
                }
            }

            case FISHERY_COLLAPSE -> {
                boolean isMarineOrCoastal = (cell.getIsCoastal() != null && cell.getIsCoastal())
                        || cell.getBiome() == Biome.OCEAN
                        || cell.getBiome() == Biome.DEEP_OCEAN;
                if (isMarineOrCoastal) {
                    double marineIntensity = (mag / 10.0) * attenuation;
                    if (cell.getBiomassFish() != null) {
                        cell.setBiomassFish(Math.max(0.0, cell.getBiomassFish() * (1.0 - 0.75 * marineIntensity)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.40 * marineIntensity)));
                    }
                    if (prevPop > 0 && attenuation > 0.8) {
                        lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.010 * marineIntensity)));
                    }
                }
            }

            case TROPICAL_CYCLONE -> {
                double cycloneIntensity = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.35 * cycloneIntensity)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.50 * cycloneIntensity)));
                }
                if (prevPop > 0 && attenuation > 0.7) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.04 * cycloneIntensity)));
                }
            }

            case WILDFIRE -> {
                double fireIntensity = (mag / 10.0) * attenuation;
                if (cell.getBiomassNatural() != null) {
                    cell.setBiomassNatural(Math.max(0.0, cell.getBiomassNatural() * (1.0 - 0.70 * fireIntensity)));
                }
                if (cell.getWoodResource() != null) {
                    cell.setWoodResource(Math.max(0.0, cell.getWoodResource() * (1.0 - 0.80 * fireIntensity)));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.15 * fireIntensity)));
                }
                if (prevPop > 0 && attenuation > 0.8) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.015 * fireIntensity)));
                }
            }

            case PANDEMIC -> {
                double virulence = 0.12;
                if (lowerName.contains("grippe") || lowerName.contains("influenza") || lowerName.contains("flu")) {
                    virulence = 0.035;
                } else if (lowerName.contains("peste") || lowerName.contains("black death") || lowerName.contains("plague") || lowerName.contains("pestis")) {
                    virulence = 0.35;
                } else if (lowerName.contains("covid") || lowerName.contains("sars") || lowerName.contains("corona")) {
                    virulence = 0.055;
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-virulence * (mag / 10.0) * attenuation)));
                    // Seed the active infectious compartment so that SpatialMetapopulationSEIREngine
                    // can propagate the contagion across trade routes and H3 topological neighbor rings:
                    int activeInfectedSeed = (int) Math.round(prevPop * Math.min(0.25, virulence * attenuation));
                    if (activeInfectedSeed > 0) {
                        int currentInfected = cell.getEpidemicInfectedOr(0);
                        cell.setEpidemicInfected(Math.min(prevPop, Math.max(currentInfected, activeInfectedSeed)));
                    }
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.04 * attenuation)));
                }
            }

            case WAR -> {
                double warIntensity = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.25 * warIntensity)));
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.08 * warIntensity)));
                }
            }

            case NUCLEAR_STRIKE, NUCLEAR_WINTER -> {
                double blast = (mag / 10.0) * attenuation;
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * Math.min(0.98, 0.90 * blast));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-1.5 * blast)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-1.8 * blast)));
                }
                // Inject heavy radiotoxic contamination creating genuine exclusion zone (> 1500)
                cell.setPollutionLevel(cell.getPollutionLevelOr(0.0) + 12000.0 * blast);
                cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - 1.5 * blast));
            }

            case METEOR -> {
                double impactBlast = (mag / 10.0) * attenuation;
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * Math.min(0.99, 0.95 * impactBlast));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-2.0 * impactBlast)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-2.2 * impactBlast)));
                }
                cell.setElevation(Math.max(-500.0, cell.getElevationOr(0.0) - 150.0 * impactBlast));
            }

            case SOLAR_EMP, CYBER_ATTACK, ECONOMIC_CRASH -> {
                double disruption = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * (1.0 - 0.40 * disruption)));
                }
            }

            case INDUSTRIAL_ACCIDENT, DAM_FAILURE, URBAN_FIRE -> {
                double destruct = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.85 * destruct)));
                }
                if (shockType == org.ether.society.model.PhysicalShockType.DAM_FAILURE && cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.70 * destruct)));
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.15 * destruct)));
                }
            }

            case TECH_SINGULARITY, RENAISSANCE_BOOM -> {
                // Informative/ideational milestones: zero physical cheat on cells (AGENTS.md §10)
            }

            case GEOENGINEERING -> {
                double geoMag = (mag / 10.0) * attenuation;
                cell.setTemperature(cell.getTemperatureOr(15.0) - 1.5 * geoMag);
                if (cell.getRainfall() != null) {
                    cell.setRainfall(Math.max(50.0, cell.getRainfall() * (1.0 - 0.15 * geoMag)));
                }
            }

            case ALIEN_CONTACT -> {
                double alienBonus = (mag / 10.0) * attenuation;
                double cap = cell.getResourceCapitalOr(1000.0);
                cell.setResourceCapital(cap * (1.0 + 1.2 * alienBonus));
                cell.setPollutionLevel(0.0);
            }

            case DOME_BREACH -> {
                double breachBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.90 * breachBlast));
                }
            }
            case ECLSS_FAILURE -> {
                double eclssBlast = (mag / 10.0) * attenuation;
                double curInteg = cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() : 1.0;
                cell.setHabitatIntegrity(Math.max(0.05, curInteg - 0.70 * eclssBlast));
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.65 * eclssBlast));
                }
            }
            case AEROSTAT_PUNCTURE -> {
                double aeroBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                cell.setHabitatType(HabitatType.NONE);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.95 * aeroBlast));
                }
            }
            case LAVA_TUBE_COLLAPSE -> {
                double collapseBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.85 * collapseBlast));
                }
            }

            default -> {
                if (upperType != null && upperType.startsWith("LEADER_")) {
                    applyLeaderArchetypeForcing(cell, upperType, mag, attenuation);
                }
            }
        }

        return lostPop;
    }

    /**
     * Applies leadership archetype forcing on institutional and demographic cell parameters.
     *
     * @param cell target cell
     * @param type leader archetype string
     * @param mag event magnitude
     * @param attenuation distance attenuation factor
     */
    public static void applyLeaderArchetypeForcing(H3Cell cell, String type, double mag, double attenuation) {
        Polity owner = cell.getOwner();
        if (owner == null) return;
        double effect = (mag / 10.0) * attenuation;

        switch (type) {
            case "LEADER_MILITARY_CONQUEROR" -> owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.15 * effect));
            case "LEADER_INFRASTRUCTURE_BUILDER" -> {
                double cap = cell.getResourceCapitalOr(100.0);
                cell.setResourceCapital(cap + 1500.0 * effect);
                double fric = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.max(0.2, fric * (1.0 - 0.25 * effect)));
            }
            case "LEADER_INSTITUTIONAL_REFORMER" -> owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.20 * effect));
            case "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR" -> {
                double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
                cell.setFoodResource(food * (1.0 + 0.30 * effect));
            }
            case "LEADER_MORAL_RELIGIOUS_SAGE" -> owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.12 * effect));
            case "LEADER_TOTALITARIAN_PURGER" -> {
                owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.15 * effect));
                owner.setAsabiyyah(Math.max(0.0, owner.getAsabiyyah() - 0.10 * effect));
            }
            default -> {}
        }
    }
}
