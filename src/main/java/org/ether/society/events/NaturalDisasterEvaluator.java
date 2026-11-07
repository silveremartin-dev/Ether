/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * <h1>NaturalDisasterEvaluator</h1>
 * <p>
 * Evaluates stochastic and triggered natural disasters (volcanism along tectonic faults,
 * droughts, famines, plagues, earthquakes, tsunamis, and regional ecological stress).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class NaturalDisasterEvaluator {

    private final EventSystem eventSystem;
    private final Random random;

    private int lastPlagueYear = Integer.MIN_VALUE;
    private int lastFamineYear = Integer.MIN_VALUE;
    private int lastDroughtYear = Integer.MIN_VALUE;
    private int lastVolcanoYear = Integer.MIN_VALUE;

    /**
     * Constructs a disaster evaluator bound to the parent event system and PRNG.
     *
     * @param eventSystem parent event system
     * @param random      shared seeded PRNG
     */
    public NaturalDisasterEvaluator(EventSystem eventSystem, Random random) {
        this.eventSystem = eventSystem;
        this.random = random;
    }

    /**
     * Resets disaster cooldowns to initial state.
     */
    public void reset() {
        lastPlagueYear = Integer.MIN_VALUE;
        lastFamineYear = Integer.MIN_VALUE;
        lastDroughtYear = Integer.MIN_VALUE;
        lastVolcanoYear = Integer.MIN_VALUE;
    }

    public H3Cell findBestLandCell(List<H3Cell> cells, double targetLat, double targetLng) {
        if (cells == null || cells.isEmpty()) return null;
        H3Cell closestLand = null;
        double minLandDist = Double.MAX_VALUE;

        for (H3Cell c : cells) {
            if (c == null) continue;
            Biome b = c.getBiome();
            boolean isLand = (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0);
            if (!isLand || c.getLatitude() == null || c.getLongitude() == null) continue;

            double dLat = c.getLatitude() - targetLat;
            double dLng = c.getLongitude() - targetLng;
            double dist = dLat * dLat + dLng * dLng;
            if (dist < minLandDist) {
                minLandDist = dist;
                closestLand = c;
            }
        }
        return closestLand != null ? closestLand : getRandomLandCell(cells);
    }

    public H3Cell getRandomLandCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> land = cells.stream()
                .filter(c -> c != null && ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0)))
                .toList();
        if (!land.isEmpty()) {
            return land.get(random.nextInt(land.size()));
        }
        return cells.get(random.nextInt(cells.size()));
    }

    public H3Cell getRandomPopulatedOrLandCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> populated = cells.stream()
                .filter(c -> c != null && ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0))
                        && c.getPopulation() != null && c.getPopulation() > 0)
                .toList();
        if (!populated.isEmpty()) {
            return populated.get(random.nextInt(populated.size()));
        }
        return getRandomLandCell(cells);
    }

    public H3Cell findBestPopulatedOrLandCellNear(List<H3Cell> cells, double targetLat, double targetLng) {
        if (cells == null || cells.isEmpty()) return null;

        H3Cell closestPopulated = null;
        double minPopDist = Double.MAX_VALUE;
        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            Biome b = c.getBiome();
            boolean isLand = (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0);
            if (isLand && c.getPopulation() != null && c.getPopulation() > 0) {
                double dLat = c.getLatitude() - targetLat;
                double dLng = c.getLongitude() - targetLng;
                double dist = dLat * dLat + dLng * dLng;
                if (dist < minPopDist) {
                    minPopDist = dist;
                    closestPopulated = c;
                }
            }
        }
        if (closestPopulated != null) {
            return closestPopulated;
        }

        return findBestLandCell(cells, targetLat, targetLng);
    }

    public H3Cell findHighestPopulationCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        return cells.stream()
                .filter(c -> c != null && ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0)))
                .max(Comparator.comparingInt(c -> c.getPopulation() != null ? c.getPopulation() : 0))
                .orElseGet(() -> getRandomLandCell(cells));
    }

    public H3Cell findBestVolcanicFaultCell(List<H3Cell> cells, double targetLat, double targetLng, boolean nearestToTarget) {
        if (cells == null || cells.isEmpty()) return null;

        List<H3Cell> candidates = cells.stream()
                .filter(c -> {
                    if (c == null) return false;
                    boolean isLandOrIsland = (c.getBiome() != Biome.DEEP_OCEAN) && (c.getElevation() != null && c.getElevation() > -100.0);
                    boolean isRelief = c.getBiome() == Biome.MOUNTAINS || c.getBiome() == Biome.HILLS || (c.getElevation() != null && c.getElevation() > 300.0);
                    return isLandOrIsland && isRelief;
                })
                .toList();

        if (candidates.isEmpty()) {
            candidates = cells.stream()
                    .filter(c -> c != null && c.getBiome() != Biome.DEEP_OCEAN)
                    .toList();
        }
        if (candidates.isEmpty()) return getRandomLandCell(cells);

        return candidates.stream()
                .max(Comparator.comparingDouble(c -> {
                    double heatScore = (c.getMantleHeatFlow() != null ? c.getMantleHeatFlow() : 87.0);
                    double elevScore = (c.getElevation() != null ? Math.max(0.0, c.getElevation()) : 0.0) / 100.0;
                    double mountainBonus = (c.getBiome() == Biome.MOUNTAINS) ? 60.0 : (c.getBiome() == Biome.HILLS ? 30.0 : 0.0);
                    double coastalArcBonus = (c.getIsCoastal() != null && c.getIsCoastal()) ? 25.0 : 0.0;
                    double distPenalty = 0.0;
                    if (nearestToTarget && c.getLatitude() != null && c.getLongitude() != null) {
                        double dLat = c.getLatitude() - targetLat;
                        double dLng = c.getLongitude() - targetLng;
                        distPenalty = Math.sqrt(dLat * dLat + dLng * dLng) * 3.0;
                    }
                    return (heatScore * 1.5) + elevScore + mountainBonus + coastalArcBonus - distPenalty + (random.nextDouble() * 15.0);
                }))
                .orElse(candidates.get(0));
    }

    public void checkFamine(int year, int month, long totalPopulation, double totalFood, List<H3Cell> cells) {
        if (year - lastFamineYear < 10) return;

        double minReserveThreshold = totalPopulation * (org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * 0.25);
        if (totalFood < minReserveThreshold && totalPopulation > 500) {
            double severity = 1.0 - (totalFood / Math.max(1.0, minReserveThreshold));

            if (severity > 0.2 && random.nextDouble() < 0.08) {
                H3Cell target = getRandomPopulatedOrLandCell(cells);
                double lat = target != null ? target.getLatitude() : 0.0;
                double lng = target != null ? target.getLongitude() : 0.0;
                String msg = severity > 0.5 ? "💀 FAMINE CRITIQUE : Famine généralisée et crise de subsistance!" : "🍂 CRISE ALIMENTAIRE : Mauvaises récoltes et hausse des prix de la nourriture.";
                double fMag = 5.0 + severity * 4.0;
                eventSystem.recordSpatialEvent(new ActiveEvent("FAMINE_" + year, msg, "FAMINE", lat, lng, year, month, 1, 20.0, fMag, 180));
                org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "FAMINE", msg, fMag, lat, lng, 0.0, 0.0, 180);
                lastFamineYear = year;
            }
        }
    }

    public void checkPlague(int year, int month, long totalPopulation, List<H3Cell> cells) {
        if (year - lastPlagueYear < 50) return;

        double plagueRisk = Math.min(0.01, totalPopulation / 10_000_000.0);

        if (totalPopulation > 10000 && random.nextDouble() < plagueRisk) {
            H3Cell target = getRandomPopulatedOrLandCell(cells);
            double lat = target != null ? target.getLatitude() : 0.0;
            double lng = target != null ? target.getLongitude() : 0.0;
            String msg = random.nextDouble() < 0.3 ? "☠️ PANDÉMIE MAJEURE : Une peste dévastatrice ravage les populations!" : "🤒 ÉPIDÉMIE LOCALE : Foyer infectieux propagé dans les centres urbains.";
            double mag = msg.contains("MAJEURE") ? 8.5 : 6.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("PLAGUE_" + year, msg, "PANDEMIC", lat, lng, year, month, 1, 20.0, mag, 730));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "PANDEMIC", msg, mag, lat, lng, 0.0, 0.0, 730);
            lastPlagueYear = year;
        }
    }

    public void checkNaturalDisasters(int year, int month, long totalPopulation, List<H3Cell> cells) {
        // Drought
        if (year - lastDroughtYear > 20 && random.nextDouble() < 0.005) {
            H3Cell droughtTarget = null;
            if (cells != null && !cells.isEmpty()) {
                List<H3Cell> landCrops = cells.stream()
                        .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                                && ((c.getPopulation() != null && c.getPopulation() > 0) || (c.getFoodResource() != null && c.getFoodResource() > 10.0)))
                        .toList();
                if (!landCrops.isEmpty()) {
                    droughtTarget = landCrops.get(random.nextInt(landCrops.size()));
                }
            }
            if (droughtTarget == null) droughtTarget = getRandomLandCell(cells);
            double dLat = (droughtTarget != null && droughtTarget.getLatitude() != null) ? droughtTarget.getLatitude() : 0.0;
            double dLng = (droughtTarget != null && droughtTarget.getLongitude() != null) ? droughtTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("DROUGHT_" + year, "☀️ SÉCHERESSE : Stress hydrique prolongé et assèchement des nappes.", "DROUGHT", dLat, dLng, year, month, 1, 20.0, 5.5, 180));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "DROUGHT", "Drought", 5.5, dLat, dLng, 0.0, 0.0, 180);
            lastDroughtYear = year;
        }

        // Volcanic eruption
        if (year - lastVolcanoYear > 100 && random.nextDouble() < 0.001) {
            H3Cell vCell = findBestVolcanicFaultCell(cells, 0.0, 0.0, false);
            double vLat = (vCell != null && vCell.getLatitude() != null) ? vCell.getLatitude() : 0.0;
            double vLng = (vCell != null && vCell.getLongitude() != null) ? vCell.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("VOLCANO_" + year, "🌋 ÉRUPTION VOLCANIQUE : Éjection massive de cendres le long d'une faille tectonique!", "VOLCANO", vLat, vLng, year, month, 1, 25.0, 8.0, 365));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "VOLCANO", "Volcano", 8.0, vLat, vLng, 10.0, 0.0, 365);
            lastVolcanoYear = year;
        }

        // Earthquake
        if (random.nextDouble() < 0.002) {
            H3Cell eqTarget = getRandomLandCell(cells);
            double eqLat = (eqTarget != null && eqTarget.getLatitude() != null) ? eqTarget.getLatitude() : 0.0;
            double eqLng = (eqTarget != null && eqTarget.getLongitude() != null) ? eqTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("EARTHQUAKE_" + year, "🌍 SÉISME / TREMBLEMENT DE TERRE : Secousse cataclysmique locale.", "EARTHQUAKE", eqLat, eqLng, year, month, 1, 20.0, 6.8, 3));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "EARTHQUAKE", "Earthquake", 6.8, eqLat, eqLng, 10.0, 0.0, 3);
        }

        // Flood
        if (random.nextDouble() < 0.003) {
            H3Cell floodTarget = null;
            if (cells != null && !cells.isEmpty()) {
                List<H3Cell> candidateLowlands = cells.stream()
                        .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                                && ((c.getWaterResource() != null && c.getWaterResource() > 0.2)
                                    || (c.getPopulation() != null && c.getPopulation() > 0)
                                    || (c.getElevation() != null && c.getElevation() >= 0 && c.getElevation() < 400)))
                        .toList();
                if (!candidateLowlands.isEmpty()) {
                    floodTarget = candidateLowlands.get(random.nextInt(candidateLowlands.size()));
                }
            }
            if (floodTarget == null) {
                floodTarget = getRandomLandCell(cells);
            }
            double fLat = (floodTarget != null && floodTarget.getLatitude() != null) ? floodTarget.getLatitude() : 0.0;
            double fLng = (floodTarget != null && floodTarget.getLongitude() != null) ? floodTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("FLOOD_" + year, "🌊 INONDATION / CRUE MAJEURE : Les cours d'eau débordent de leur lit.", "FLOOD", fLat, fLng, year, month, 1, 20.0, 5.2, 30));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "FLOOD", "Flood", 5.2, fLat, fLng, 0.0, 0.0, 30);
        }

        // Tsunami
        if (random.nextDouble() < 0.0015 && cells != null && !cells.isEmpty()) {
            List<H3Cell> coastalOrOcean = cells.stream()
                    .filter(c -> c != null && (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || (c.getElevation() != null && c.getElevation() <= 50.0)))
                    .toList();
            if (!coastalOrOcean.isEmpty()) {
                H3Cell tCell = coastalOrOcean.get(random.nextInt(coastalOrOcean.size()));
                if (tCell.getLatitude() != null && tCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("TSUNAMI_" + year, "🌊 TSUNAMI / SUBMERSION CÔTIÈRE : Onde océanique déferlante sur les rivages.", "TSUNAMI", tCell.getLatitude(), tCell.getLongitude(), year, month, 1, 25.0, 7.8, 1));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, null, null, null, "TSUNAMI", "Tsunami", 7.8, tCell.getLatitude(), tCell.getLongitude(), 0.0, 0.0, 1);
                }
            }
        }
    }

    public void checkAchievements(int year, int month, long totalPopulation, List<H3Cell> cells, java.util.Set<String> firedEvents) {
        String milestoneId = null;
        String milestoneMsg = null;
        double mag = 5.0;

        if (totalPopulation >= 1_000_000 && totalPopulation < 1_100_000 && !firedEvents.contains("MILESTONE_1M")) {
            milestoneId = "MILESTONE_1M";
            milestoneMsg = "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale franchit 1 million d'habitants!";
            mag = 5.0;
        } else if (totalPopulation >= 10_000_000 && totalPopulation < 10_500_000 && !firedEvents.contains("MILESTONE_10M")) {
            milestoneId = "MILESTONE_10M";
            milestoneMsg = "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 10 millions d'habitants!";
            mag = 6.0;
        } else if (totalPopulation >= 100_000_000 && totalPopulation < 105_000_000 && !firedEvents.contains("MILESTONE_100M")) {
            milestoneId = "MILESTONE_100M";
            milestoneMsg = "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 100 millions d'habitants!";
            mag = 7.0;
        } else if (totalPopulation >= 1_000_000_000 && totalPopulation < 1_050_000_000 && !firedEvents.contains("MILESTONE_1B")) {
            milestoneId = "MILESTONE_1B";
            milestoneMsg = "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 1 milliard d'habitants!";
            mag = 8.5;
        }

        if (milestoneId != null) {
            H3Cell topCell = findHighestPopulationCell(cells);
            double lat = (topCell != null && topCell.getLatitude() != null) ? topCell.getLatitude() : 0.0;
            double lng = (topCell != null && topCell.getLongitude() != null) ? topCell.getLongitude() : 0.0;
            firedEvents.add(milestoneId);
            eventSystem.recordSpatialEvent(new ActiveEvent(milestoneId, milestoneMsg, "MILESTONE", lat, lng, year, month, 1, 20.0, mag));
        }
    }
}
