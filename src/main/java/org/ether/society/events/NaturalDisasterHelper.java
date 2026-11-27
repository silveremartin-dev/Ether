/*
 * MIT License
 *
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
 * <h1>NaturalDisasterHelper</h1>
 * <p>
 * Evaluates stochastic and triggered natural disasters (volcanism along tectonic faults,
 * droughts, famines, plagues, earthquakes, tsunamis, and regional ecological stress).
 * All evaluations use strictly stateless deterministic PRNG anchored to scenario seed, year, and tick.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class NaturalDisasterHelper {

    private final EventSystem eventSystem;
    private long baseSeed = 42L;

    private int lastPlagueYear = Integer.MIN_VALUE;
    private int lastDroughtYear = Integer.MIN_VALUE;
    private int lastVolcanoYear = Integer.MIN_VALUE;

    /**
     * Constructs a disaster evaluator bound to the parent event system and PRNG.
     *
     * @param eventSystem parent event system
     * @param random      shared seeded PRNG (ignored in favor of DeterministicPRNG)
     */
    public NaturalDisasterHelper(EventSystem eventSystem, java.util.Random random) {
        this.eventSystem = eventSystem;
    }

    /**
     * Constructs a disaster evaluator bound to the parent event system.
     *
     * @param eventSystem parent event system
     */
    public NaturalDisasterHelper(EventSystem eventSystem) {
        this.eventSystem = eventSystem;
    }

    /**
     * Sets the base scenario seed for deterministic evaluation.
     *
     * @param seed scenario seed
     */
    public void setSeed(long seed) {
        this.baseSeed = seed;
    }

    public void reset() {
        this.lastPlagueYear = Integer.MIN_VALUE;
        this.lastDroughtYear = Integer.MIN_VALUE;
        this.lastVolcanoYear = Integer.MIN_VALUE;
    }

    public H3Cell findBestLandCell(List<H3Cell> cells, double targetLat, double targetLng) {
        return findBestPopulatedOrLandCellNear(cells, targetLat, targetLng);
    }

    /**
     * Finds the most physically plausible volcanic or tectonic fault cell near coordinates.
     */
    public H3Cell findBestVolcanicFaultCell(List<H3Cell> cells, double targetLat, double targetLng, boolean coastalPreference) {
        if (cells == null || cells.isEmpty()) return null;

        List<H3Cell> candidates = cells.stream()
                .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                        && c.getElevation() != null && c.getElevation() > 200.0)
                .toList();

        if (candidates.isEmpty()) return getRandomLandCell(cells, 0L);

        H3Cell best = null;
        double bestScore = -Double.MAX_VALUE;

        for (H3Cell c : candidates) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;

            double elevScore = Math.min(100.0, c.getElevation() / 35.0);
            double heatScore = Math.max(0.0, c.getTemperatureOr(15.0) * 1.5);

            boolean isMountainous = c.getBiome() == Biome.MOUNTAINS || c.getBiome() == Biome.SNOW;
            double mountainBonus = isMountainous ? 40.0 : 0.0;

            double coastalArcBonus = 0.0;
            if (coastalPreference) {
                boolean isNearSea = (c.getElevation() < 1200.0 && c.getElevation() > 50.0);
                if (isNearSea) coastalArcBonus = 25.0;
            }

            double dLat = c.getLatitude() - targetLat;
            double dLng = c.getLongitude() - targetLng;
            double distPenalty = Math.sqrt(dLat * dLat + dLng * dLng) * 0.15;
            double jitter = DeterministicPRNG.nextDouble(baseSeed, 0L, c.getH3Index(), 11L) * 15.0;

            double totalScore = (heatScore * 1.5) + elevScore + mountainBonus + coastalArcBonus - distPenalty + jitter;
            if (totalScore > bestScore) {
                bestScore = totalScore;
                best = c;
            }
        }
        return best != null ? best : getRandomLandCell(cells, 0L);
    }

    /**
     * Picks a random land cell deterministically.
     */
    public H3Cell getRandomLandCell(List<H3Cell> cells, long streamId) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> land = cells.stream()
                .filter(c -> c != null && ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                        || (c.getElevation() != null && c.getElevation() > 0)))
                .toList();
        if (!land.isEmpty()) {
            int idx = DeterministicPRNG.nextInt(baseSeed, 0L, streamId, land.size());
            return land.get(idx);
        }
        int idx = DeterministicPRNG.nextInt(baseSeed, 0L, streamId, cells.size());
        return cells.get(idx);
    }

    /**
     * Backward-compatible overload.
     */
    public H3Cell getRandomLandCell(List<H3Cell> cells) {
        return getRandomLandCell(cells, 0L);
    }

    /**
     * Picks a random populated cell or land cell deterministically.
     */
    public H3Cell getRandomPopulatedOrLandCell(List<H3Cell> cells, long streamId) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> populated = cells.stream()
                .filter(c -> c != null && ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0))
                        && c.getPopulation() != null && c.getPopulation() > 0)
                .toList();
        if (!populated.isEmpty()) {
            int idx = DeterministicPRNG.nextInt(baseSeed, 0L, streamId, populated.size());
            return populated.get(idx);
        }
        return getRandomLandCell(cells, streamId);
    }

    /**
     * Backward-compatible overload.
     */
    public H3Cell getRandomPopulatedOrLandCell(List<H3Cell> cells) {
        return getRandomPopulatedOrLandCell(cells, 0L);
    }

    /**
     * Finds the closest populated or land cell to target coordinates.
     */
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
        if (closestPopulated != null) return closestPopulated;

        H3Cell closestLand = null;
        double minLandDist = Double.MAX_VALUE;
        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            Biome b = c.getBiome();
            boolean isLand = (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0);
            if (isLand) {
                double dLat = c.getLatitude() - targetLat;
                double dLng = c.getLongitude() - targetLng;
                double dist = dLat * dLat + dLng * dLng;
                if (dist < minLandDist) {
                    minLandDist = dist;
                    closestLand = c;
                }
            }
        }
        return closestLand != null ? closestLand : getRandomLandCell(cells, 0L);
    }

    /**
     * Finds the most populated cell.
     */
    public H3Cell findMostPopulatedCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        return cells.stream()
                .filter(c -> c != null && c.getPopulation() != null && c.getPopulation() > 0)
                .max(Comparator.comparingInt(H3Cell::getPopulation))
                .orElseGet(() -> getRandomLandCell(cells, 0L));
    }

    /**
     * Finds agrarian river basin cell.
     */
    public H3Cell findBestAgrarianRiverBasinCell(List<H3Cell> cells, double targetLat, double targetLng) {
        if (cells == null || cells.isEmpty()) return null;

        List<H3Cell> candidates = cells.stream()
                .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                        && c.getElevation() != null && c.getElevation() < 500.0
                        && ((c.getWaterResource() != null && c.getWaterResource() > 0.2)
                            || (c.getFoodResource() != null && c.getFoodResource() > 10.0)))
                .toList();

        if (candidates.isEmpty()) return getRandomLandCell(cells, 0L);

        H3Cell best = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (H3Cell c : candidates) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double dLat = c.getLatitude() - targetLat;
            double dLng = c.getLongitude() - targetLng;
            double distSq = dLat * dLat + dLng * dLng;
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                best = c;
            }
        }
        return best != null ? best : getRandomLandCell(cells, 0L);
    }

    /**
     * Checks plague and epidemic emergence with strict historical-biophysical gating:
     * - Sedentary urban crowd infections require domestication and dense populations (year >= -10,000 BP).
     * - Deep Paleolithic (year < -10,000 BP) only permits small local nomadic camp zoonoses, never global plagues.
     */
    public void checkPlague(int year, int month, long totalPopulation, List<H3Cell> cells) {
        checkPlague(year, month, totalPopulation, cells, null, null, null);
    }

    public void checkPlague(
            int year,
            int month,
            long totalPopulation,
            List<H3Cell> cells,
            org.ether.society.core.dod.WorldBuffer worldBuffer,
            org.ether.society.core.dod.AgentBuffer agentBuffer,
            List<org.ether.society.database.Polity> nations) {
        if (year - lastPlagueYear < 50) return;

        boolean isPrehistoricPaleolithic = year < -10000;
        if (isPrehistoricPaleolithic && totalPopulation < 200_000) {
            return; // No crowded urban epidemics in paleolithic hunter-gatherer bands
        }

        double plagueRisk = Math.min(0.01, totalPopulation / 20_000_000.0);
        long stream = (long) year * 100L + month;
        double pVal = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 201L);

        if (totalPopulation > 50_000 && pVal < plagueRisk) {
            List<H3Cell> populated = cells != null ? cells.stream()
                    .filter(c -> c != null && c.getPopulation() != null && c.getPopulation() > 50)
                    .toList() : List.of();
            if (populated.isEmpty()) return;

            int popIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 2L, populated.size());
            H3Cell target = populated.get(popIdx);
            double lat = target.getLatitude() != null ? target.getLatitude() : 0.0;
            double lng = target.getLongitude() != null ? target.getLongitude() : 0.0;

            double pMajor = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream + 3L, 203L);
            String msg;
            double mag;
            if (isPrehistoricPaleolithic) {
                msg = "🤒 ZOONOSE LOCALE : Foyer infectieux animal limité à un campement nomade.";
                mag = 4.5;
            } else if (pMajor < 0.25 && year >= -3000) {
                msg = "☠️ PANDÉMIE MAJEURE : Une peste dévastatrice ravage les populations sédentaires!";
                mag = 8.5;
            } else {
                msg = "🤒 ÉPIDÉMIE RÉGIONALE : Foyer infectieux propagé le long des voies d'échange.";
                mag = 6.0;
            }

            eventSystem.recordSpatialEvent(new ActiveEvent("PLAGUE_" + year, msg, "PANDEMIC", lat, lng, year, month, 1, 20.0, mag, 730));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "PANDEMIC", msg, mag, lat, lng, 0.0, 0.0, 730);
            lastPlagueYear = year;
        }
    }

    /**
     * Checks geophysical natural disasters deterministically.
     */
    public void checkNaturalDisasters(int year, int month, long totalPopulation, List<H3Cell> cells) {
        checkNaturalDisasters(year, month, totalPopulation, cells, null, null, null);
    }

    public void checkNaturalDisasters(
            int year,
            int month,
            long totalPopulation,
            List<H3Cell> cells,
            org.ether.society.core.dod.WorldBuffer worldBuffer,
            org.ether.society.core.dod.AgentBuffer agentBuffer,
            List<org.ether.society.database.Polity> nations) {
        long stream = (long) year * 100L + month;

        // Drought
        double pDrought = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 301L);
        if (year - lastDroughtYear > 20 && pDrought < 0.005) {
            H3Cell droughtTarget = null;
            if (cells != null && !cells.isEmpty()) {
                List<H3Cell> landCrops = cells.stream()
                        .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                                && ((c.getPopulation() != null && c.getPopulation() > 0) || (c.getFoodResource() != null && c.getFoodResource() > 10.0)))
                        .toList();
                if (!landCrops.isEmpty()) {
                    int cIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 11L, landCrops.size());
                    droughtTarget = landCrops.get(cIdx);
                }
            }
            if (droughtTarget == null) droughtTarget = getRandomLandCell(cells, stream + 12L);
            double dLat = (droughtTarget != null && droughtTarget.getLatitude() != null) ? droughtTarget.getLatitude() : 0.0;
            double dLng = (droughtTarget != null && droughtTarget.getLongitude() != null) ? droughtTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("DROUGHT_" + year, "☀️ SÉCHERESSE : Stress hydrique prolongé et assèchement des nappes.", "DROUGHT", dLat, dLng, year, month, 1, 20.0, 5.5, 180));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "DROUGHT", "Drought", 5.5, dLat, dLng, 0.0, 0.0, 180);
            lastDroughtYear = year;
        }

        // Volcanic eruption
        double pVolcano = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 401L);
        if (year - lastVolcanoYear > 100 && pVolcano < 0.001) {
            H3Cell vCell = findBestVolcanicFaultCell(cells, 0.0, 0.0, false);
            double vLat = (vCell != null && vCell.getLatitude() != null) ? vCell.getLatitude() : 0.0;
            double vLng = (vCell != null && vCell.getLongitude() != null) ? vCell.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("VOLCANO_" + year, "🌋 ÉRUPTION VOLCANIQUE : Éjection massive de cendres le long d'une faille tectonique!", "VOLCANO", vLat, vLng, year, month, 1, 25.0, 8.0, 365));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "VOLCANO", "Volcano", 8.0, vLat, vLng, 10.0, 0.0, 365);
            lastVolcanoYear = year;
        }

        // Earthquake
        double pEq = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 501L);
        if (pEq < 0.002) {
            H3Cell eqTarget = getRandomLandCell(cells, stream + 51L);
            double eqLat = (eqTarget != null && eqTarget.getLatitude() != null) ? eqTarget.getLatitude() : 0.0;
            double eqLng = (eqTarget != null && eqTarget.getLongitude() != null) ? eqTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("EARTHQUAKE_" + year, "🌍 SÉISME / TREMBLEMENT DE TERRE : Secousse cataclysmique locale.", "EARTHQUAKE", eqLat, eqLng, year, month, 1, 20.0, 6.8, 3));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "EARTHQUAKE", "Earthquake", 6.8, eqLat, eqLng, 10.0, 0.0, 3);
        }

        // Flood
        double pFlood = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 601L);
        if (pFlood < 0.003) {
            H3Cell floodTarget = null;
            if (cells != null && !cells.isEmpty()) {
                List<H3Cell> candidateLowlands = cells.stream()
                        .filter(c -> c != null && (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN
                                && c.getBiome() != Biome.DESERT && c.getBiome() != Biome.GLACIER)
                                && ((c.getWaterResource() != null && c.getWaterResource() > 0.2)
                                || (c.getPopulation() != null && c.getPopulation() > 0)
                                || (c.getElevation() != null && c.getElevation() >= 0 && c.getElevation() < 400)))
                        .toList();
                if (!candidateLowlands.isEmpty()) {
                    int fIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 61L, candidateLowlands.size());
                    floodTarget = candidateLowlands.get(fIdx);
                }
            }
            if (floodTarget == null) {
                floodTarget = getRandomLandCell(cells, stream + 62L);
            }
            double fLat = (floodTarget != null && floodTarget.getLatitude() != null) ? floodTarget.getLatitude() : 0.0;
            double fLng = (floodTarget != null && floodTarget.getLongitude() != null) ? floodTarget.getLongitude() : 0.0;
            eventSystem.recordSpatialEvent(new ActiveEvent("FLOOD_" + year, "🌊 INONDATION / CRUE MAJEURE : Les cours d'eau débordent de leur lit.", "FLOOD", fLat, fLng, year, month, 1, 20.0, 5.2, 30));
            org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "FLOOD", "Flood", 5.2, fLat, fLng, 0.0, 0.0, 30);
        }

        // Tsunami
        double pTsunami = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 701L);
        if (pTsunami < 0.0015 && cells != null && !cells.isEmpty()) {
            List<H3Cell> coastalOrOcean = cells.stream()
                    .filter(c -> c != null && (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || (c.getElevation() != null && c.getElevation() <= 50.0)))
                    .toList();
            if (!coastalOrOcean.isEmpty()) {
                int tIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 71L, coastalOrOcean.size());
                H3Cell tCell = coastalOrOcean.get(tIdx);
                if (tCell.getLatitude() != null && tCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("TSUNAMI_" + year, "🌊 TSUNAMI / SUBMERSION CÔTIÈRE : Onde océanique déferlante sur les rivages.", "TSUNAMI", tCell.getLatitude(), tCell.getLongitude(), year, month, 1, 25.0, 7.8, 1));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "TSUNAMI", "Tsunami", 7.8, tCell.getLatitude(), tCell.getLongitude(), 0.0, 0.0, 1);
                }
            }
        }

        // Locust Swarm (Warm, Arid/Semi-Arid, Subtropical belts ONLY)
        double pLocust = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 801L);
        if (pLocust < 0.0025 && year >= -5000 && cells != null && !cells.isEmpty()) {
            List<H3Cell> aridWarmCrops = cells.stream()
                    .filter(c -> c != null && c.getLatitude() != null && Math.abs(c.getLatitude()) <= 42.0
                            && c.getTemperatureOr(15.0) >= 15.0
                            && (c.getBiome() == Biome.DESERT || c.getBiome() == Biome.SAVANNAH || c.getBiome() == Biome.PLAINS)
                            && c.getBiome() != Biome.GLACIER && c.getBiome() != Biome.TUNDRA && c.getBiome() != Biome.SNOW)
                    .toList();
            if (!aridWarmCrops.isEmpty()) {
                int lIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 81L, aridWarmCrops.size());
                H3Cell lCell = aridWarmCrops.get(lIdx);
                if (lCell.getLatitude() != null && lCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("LOCUST_" + year, "🦗 INVASION ACCRIDIENNE : Un essaim dévastateur de criquets pèlerins dévore cultures et pâturages.", "LOCUST_SWARM", lCell.getLatitude(), lCell.getLongitude(), year, month, 1, 20.0, 6.8, 60));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "LOCUST_SWARM", "Locust Swarm", 6.8, lCell.getLatitude(), lCell.getLongitude(), 0.0, 0.0, 60);
                }
            }
        }

        // Crop Blight & Rot (Cultivated Agricultural Land, Temperate & Boreal Belts)
        double pBlight = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 821L);
        if (pBlight < 0.0025 && year >= -8000 && cells != null && !cells.isEmpty()) {
            List<H3Cell> cropLands = cells.stream()
                    .filter(c -> c != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN
                            && c.getBiome() != Biome.DESERT && c.getBiome() != Biome.GLACIER
                            && ((c.getBiomassAgriculture() != null && c.getBiomassAgriculture() > 5.0)
                            || (c.getFoodResource() != null && c.getFoodResource() > 15.0)))
                    .toList();
            if (!cropLands.isEmpty()) {
                int bIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 83L, cropLands.size());
                H3Cell bCell = cropLands.get(bIdx);
                if (bCell.getLatitude() != null && bCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("BLIGHT_" + year, "🌾 NIELLE & POURRITURE : Épiphytie et moisissures détruisant les récoltes sur pied.", "CROP_BLIGHT", bCell.getLatitude(), bCell.getLongitude(), year, month, 1, 20.0, 6.5, 90));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "CROP_BLIGHT", "Crop Blight", 6.5, bCell.getLatitude(), bCell.getLongitude(), 0.0, 0.0, 90);
                }
            }
        }

        // Wildlife Die-Off & Sylvatic Epizootic (Wild Terrestrial Habitats)
        double pWildlife = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 841L);
        if (pWildlife < 0.0020 && cells != null && !cells.isEmpty()) {
            List<H3Cell> wildHabitats = cells.stream()
                    .filter(c -> c != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN && c.getBiome() != Biome.DESERT
                            && c.getBiomassNatural() != null && c.getBiomassNatural() > 25.0)
                    .toList();
            if (!wildHabitats.isEmpty()) {
                int wIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 85L, wildHabitats.size());
                H3Cell wCell = wildHabitats.get(wIdx);
                if (wCell.getLatitude() != null && wCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("WILDLIFE_" + year, "🦌 ÉPIZOOTIE SAUVAGE : Vague de mortalité aiguë parmi le gibier et les troupeaux.", "WILDLIFE_DIE_OFF", wCell.getLatitude(), wCell.getLongitude(), year, month, 1, 25.0, 6.2, 180));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "WILDLIFE_DIE_OFF", "Wildlife Die-Off", 6.2, wCell.getLatitude(), wCell.getLongitude(), 0.0, 0.0, 180);
                }
            }
        }

        // Fishery Collapse & Marine Anoxia (STRICTLY Coastal & Marine Oceans)
        double pFishery = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 861L);
        if (pFishery < 0.0020 && cells != null && !cells.isEmpty()) {
            List<H3Cell> marineCoast = cells.stream()
                    .filter(c -> c != null && ((c.getIsCoastal() != null && c.getIsCoastal())
                            || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN
                            || (c.getBiomassFish() != null && c.getBiomassFish() > 10.0)))
                    .toList();
            if (!marineCoast.isEmpty()) {
                int mIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 87L, marineCoast.size());
                H3Cell mCell = marineCoast.get(mIdx);
                if (mCell.getLatitude() != null && mCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("FISHERY_" + year, "🐟 ANOXIE MARINE : Effondrement abrupt des bancs de poissons et de la pêche.", "FISHERY_COLLAPSE", mCell.getLatitude(), mCell.getLongitude(), year, month, 1, 30.0, 6.5, 365));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "FISHERY_COLLAPSE", "Fishery Collapse", 6.5, mCell.getLatitude(), mCell.getLongitude(), 0.0, 0.0, 365);
                }
            }
        }

        // Tropical Cyclone / Hurricane (STRICTLY Tropical Coastal / Marine Basins, 5° to 35° Lat)
        double pCyclone = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 881L);
        if (pCyclone < 0.0030 && cells != null && !cells.isEmpty()) {
            List<H3Cell> tropicalWaters = cells.stream()
                    .filter(c -> c != null && c.getLatitude() != null
                            && Math.abs(c.getLatitude()) >= 5.0 && Math.abs(c.getLatitude()) <= 35.0
                            && ((c.getIsCoastal() != null && c.getIsCoastal()) || c.getBiome() == Biome.OCEAN)
                            && c.getTemperatureOr(15.0) >= 22.0)
                    .toList();
            if (!tropicalWaters.isEmpty()) {
                int cIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 89L, tropicalWaters.size());
                H3Cell cycCell = tropicalWaters.get(cIdx);
                if (cycCell.getLatitude() != null && cycCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("CYCLONE_" + year, "🌀 CYCLONE TROPICAL : Ouragan dévastateur frappant les côtes tropicales.", "TROPICAL_CYCLONE", cycCell.getLatitude(), cycCell.getLongitude(), year, month, 1, 25.0, 7.5, 7));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "TROPICAL_CYCLONE", "Tropical Cyclone", 7.5, cycCell.getLatitude(), cycCell.getLongitude(), 0.0, 0.0, 7);
                }
            }
        }

        // Natural Wildfire (Densely Vegetated Dry Terrestrial Forests & Savannas)
        double pFire = DeterministicPRNG.nextDouble(baseSeed, (long) year, stream, 901L);
        if (pFire < 0.0025 && cells != null && !cells.isEmpty()) {
            List<H3Cell> dryForests = cells.stream()
                    .filter(c -> c != null
                            && (c.getBiome() == Biome.FOREST || c.getBiome() == Biome.JUNGLE
                            || c.getBiome() == Biome.SAVANNAH || c.getBiome() == Biome.PLAINS)
                            && c.getBiomassNatural() != null && c.getBiomassNatural() > 30.0)
                    .toList();
            if (!dryForests.isEmpty()) {
                int wfIdx = DeterministicPRNG.nextInt(baseSeed, (long) year, stream + 91L, dryForests.size());
                H3Cell wfCell = dryForests.get(wfIdx);
                if (wfCell.getLatitude() != null && wfCell.getLongitude() != null) {
                    eventSystem.recordSpatialEvent(new ActiveEvent("WILDFIRE_" + year, "🔥 MÉGA-INCENDIE NATUREL : Feux de forêt consumant la biomasse sylvatique.", "WILDFIRE", wfCell.getLatitude(), wfCell.getLongitude(), year, month, 1, 20.0, 6.5, 30));
                    org.ether.society.core.EventPhysicalImpactExecutor.applyPhysicalForcingToGrid(cells, worldBuffer, agentBuffer, nations, "WILDFIRE", "Wildfire", 6.5, wfCell.getLatitude(), wfCell.getLongitude(), 0.0, 0.0, 30);
                }
            }
        }
    }

    /**
     * Checks civilization achievements.
     */
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
            milestoneMsg = "🌍 SEUIL DÉMOGRAPHIQUE PLANÉTAIRE : L'humanité franchit 1 milliard d'individus!";
            mag = 8.5;
        }

        if (milestoneId != null) {
            firedEvents.add(milestoneId);
            eventSystem.recordSpatialEvent(new ActiveEvent(milestoneId, milestoneMsg, "ACHIEVEMENT", 0.0, 0.0, year, month, 1, 0.0, mag, 30));
        }
    }
}
