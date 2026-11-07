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
        if (type.contains("PANDEMIC") || type.contains("PLAGUE") || type.contains("EPIDEMIC")) {
            return (name.contains("grippe") || name.contains("flu")) ? (180.0 * 111.32) : (85.0 * 111.32);
        }
        if (type.contains("VOLCANO") || type.contains("NUCLEAR")) {
            return Math.max(250.0, mag * 200.0);
        }
        if (type.contains("EARTHQUAKE") || type.contains("TSUNAMI")) {
            return Math.max(150.0, mag * 120.0);
        }
        if (type.contains("TECH_SINGULARITY") || type.contains("RENAISSANCE")) {
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
        switch (type) {
            case "VOLCANO" -> {
                double addedTau = 0.05 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedTau);
            }
            case "NUCLEAR_STRIKE", "NUCLEAR_WINTER" -> {
                double addedSoot = 0.20 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedSoot);
            }
            case "METEOR" -> {
                double addedEjecta = 0.35 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedEjecta);
            }
            case "GEOENGINEERING" -> {
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

        switch (upperType) {
            case "VOLCANO" -> {
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

            case "EARTHQUAKE" -> {
                double seismicIntensity = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.25 * seismicIntensity)));
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.08 * seismicIntensity)));
                }
            }

            case "TSUNAMI" -> {
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

            case "FLOOD" -> {
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

            case "DROUGHT", "HEATWAVE", "FAMINE" -> {
                double droughtIntensity = (mag / 10.0) * attenuation;
                if (cell.getFreshwaterAquifer() != null) {
                    cell.setFreshwaterAquifer(Math.max(0.0, cell.getFreshwaterAquifer() * (1.0 - 0.50 * droughtIntensity)));
                }
                if (cell.getFoodResource() != null) {
                    cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.65 * droughtIntensity)));
                }
                if (upperType.equals("HEATWAVE")) {
                    cell.setTemperature(cell.getTemperatureOr(15.0) + 1.2 * mag * attenuation);
                }
                if (prevPop > 0 && upperType.equals("FAMINE")) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.06 * droughtIntensity)));
                }
            }

            case "PANDEMIC", "EPIDEMIC", "PLAGUE" -> {
                double virulence = 0.12;
                if (lowerName.contains("grippe") || lowerName.contains("influenza") || lowerName.contains("flu")) {
                    virulence = 0.035;
                } else if (lowerName.contains("peste") || lowerName.contains("black death") || lowerName.contains("plague")) {
                    virulence = 0.35;
                } else if (lowerName.contains("covid") || lowerName.contains("sars")) {
                    virulence = 0.055;
                }
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-virulence * (mag / 10.0) * attenuation)));
                }
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.04 * attenuation)));
                }
            }

            case "NUCLEAR_STRIKE", "NUCLEAR_WINTER" -> {
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
                cell.setPollutionLevel(Math.min(1.0, cell.getPollutionLevelOr(0.0) + 0.85 * blast));
                cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - 1.5 * blast));
            }

            case "METEOR" -> {
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

            case "SOLAR_EMP", "CYBER_ATTACK", "ECONOMIC_CRASH" -> {
                double disruption = (mag / 10.0) * attenuation;
                if (cell.getResourceCapital() != null) {
                    cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * (1.0 - 0.40 * disruption)));
                }
            }

            case "TECH_SINGULARITY", "FUSION", "milestone_technology" -> {
                double techBonus = (mag / 10.0) * attenuation;
                double currentCap = cell.getResourceCapitalOr(1000.0);
                cell.setResourceCapital(currentCap + (5000.0 * techBonus * Math.max(1, prevPop)));
                cell.setPollutionLevel(Math.max(0.0, cell.getPollutionLevelOr(0.0) * (1.0 - 0.80 * techBonus)));
                double curFriction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.max(0.15, curFriction * (1.0 - 0.50 * techBonus)));
            }

            case "RENAISSANCE_BOOM", "milestone_golden_age" -> {
                double boomBonus = (mag / 10.0) * attenuation;
                double curCap = cell.getResourceCapitalOr(500.0);
                cell.setResourceCapital(curCap * (1.0 + 0.35 * boomBonus));
                double curFric = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.max(0.20, curFric * (1.0 - 0.30 * boomBonus)));

                Polity owner = cell.getOwner();
                if (owner != null) {
                    owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.15 * boomBonus));
                    owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.10 * boomBonus));
                }
            }

            case "GEOENGINEERING" -> {
                double geoMag = (mag / 10.0) * attenuation;
                cell.setTemperature(cell.getTemperatureOr(15.0) - 1.5 * geoMag);
                if (cell.getRainfall() != null) {
                    cell.setRainfall(Math.max(50.0, cell.getRainfall() * (1.0 - 0.15 * geoMag)));
                }
            }

            case "ALIEN_CONTACT" -> {
                double alienBonus = (mag / 10.0) * attenuation;
                double cap = cell.getResourceCapitalOr(1000.0);
                cell.setResourceCapital(cap * (1.0 + 1.2 * alienBonus));
                cell.setPollutionLevel(0.0);
            }

            case "DOME_BREACH" -> {
                double breachBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.90 * breachBlast));
                }
            }
            case "ECLSS_FAILURE" -> {
                double eclssBlast = (mag / 10.0) * attenuation;
                double curInteg = cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() : 1.0;
                cell.setHabitatIntegrity(Math.max(0.05, curInteg - 0.70 * eclssBlast));
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.65 * eclssBlast));
                }
            }
            case "AEROSTAT_PUNCTURE" -> {
                double aeroBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                cell.setHabitatType(HabitatType.NONE);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.95 * aeroBlast));
                }
            }
            case "LAVA_TUBE_COLLAPSE" -> {
                double collapseBlast = (mag / 10.0) * attenuation;
                cell.setHabitatIntegrity(0.0);
                if (prevPop > 0) {
                    lostPop = (int) Math.round(prevPop * (1.0 - 0.85 * collapseBlast));
                }
            }

            default -> {
                if (upperType.startsWith("LEADER_")) {
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
