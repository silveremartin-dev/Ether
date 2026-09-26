/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Nation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages historical milestones, contingency leader interventions, and stochastic events in the simulation.
 * 
 * Event Types:
 * - Historical Milestones: Pre-defined technological and civilizational markers
 * - Earth Historical Leaders: Deterministic historical figures (Alexander, Augustus, Hammurabi, etc.)
 * - Procedural Leader Outliers: Emergent high-sigma leaders and reformers in sandbox/procedural worlds
 * - Geophysical & Natural Disasters: Volcanism, floods, earthquakes, famines, pandemics
 */
public class EventSystem {
    private final List<HistoricalEvent> historicalEvents = new ArrayList<>();
    private final List<String> eventQueue = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private boolean enableHistoricalMilestones = true;
    private boolean enableRandomEvents = true;
    private boolean enableEarthHistoricalLeaders = true;
    private boolean enableProceduralLeaders = true;

    // Catalogs and Generators
    private final HistoricalInterventionCatalog historicalCatalog = HistoricalInterventionCatalog.getInstance();
    private final ProceduralLeaderGenerator proceduralLeaderGenerator = new ProceduralLeaderGenerator();
    private final List<HistoricalIntervention> activeInterventions = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> firedInterventionIds = new java.util.HashSet<>();

    // Cooldowns to prevent event spam
    private int lastPlagueYear = Integer.MIN_VALUE;
    private int lastFamineYear = Integer.MIN_VALUE;
    private int lastDroughtYear = Integer.MIN_VALUE;
    private int lastVolcanoYear = Integer.MIN_VALUE;

    private final java.util.Set<String> firedHistoricalEvents = new java.util.HashSet<>();
    private final List<ActiveEvent> activeEvents = new CopyOnWriteArrayList<>();
    private final List<ActiveEvent> recentEventsHistory = new CopyOnWriteArrayList<>();
    private final List<ActiveEvent> chronicleHistory = new CopyOnWriteArrayList<>();

    public EventSystem() {
        initializeHistoricalEvents();
    }

    public void setSeed(long seed) {
        this.random.setSeed(seed);
        this.proceduralLeaderGenerator.setSeed(seed);
    }

    public boolean isEnableRandomEvents() {
        return enableRandomEvents;
    }

    public void setEnableRandomEvents(boolean enableRandomEvents) {
        this.enableRandomEvents = enableRandomEvents;
    }

    public boolean isEnableHistoricalMilestones() {
        return enableHistoricalMilestones;
    }

    public void setEnableHistoricalMilestones(boolean enableHistoricalMilestones) {
        this.enableHistoricalMilestones = enableHistoricalMilestones;
    }

    public boolean isEnableEarthHistoricalLeaders() {
        return enableEarthHistoricalLeaders;
    }

    public void setEnableEarthHistoricalLeaders(boolean enableEarthHistoricalLeaders) {
        this.enableEarthHistoricalLeaders = enableEarthHistoricalLeaders;
    }

    public boolean isEnableProceduralLeaders() {
        return enableProceduralLeaders;
    }

    public void setEnableProceduralLeaders(boolean enableProceduralLeaders) {
        this.enableProceduralLeaders = enableProceduralLeaders;
    }

    private void initializeHistoricalEvents() {
        // Prehistoric
        historicalEvents.add(new HistoricalEvent(-100000, "Out of Africa",
                "Humans begin migrating out of Africa to new continents.", 9.02, 38.74));
        historicalEvents.add(new HistoricalEvent(-40000, "Cave Art",
                "First known cave paintings appear, showing symbolic thought.", 44.38, 4.41));
        historicalEvents.add(new HistoricalEvent(-12000, "Last Ice Age Ends",
                "Warming climate opens new lands for settlement.", 52.52, 13.40));

        // Neolithic
        historicalEvents.add(new HistoricalEvent(-10000, "Agricultural Revolution",
                "Farming begins in the Fertile Crescent.", 37.22, 38.92));
        historicalEvents.add(new HistoricalEvent(-8000, "First Villages",
                "Permanent settlements like Jericho are established.", 31.86, 35.46));
        historicalEvents.add(new HistoricalEvent(-6000, "Pottery Invented",
                "Ceramic vessels allow food storage and trade.", 34.50, 113.60));

        // Bronze Age
        historicalEvents.add(new HistoricalEvent(-3500, "Writing Invented",
                "Cuneiform and hieroglyphics enable record-keeping.", 31.32, 45.63));
        historicalEvents.add(new HistoricalEvent(-3000, "Bronze Age Begins",
                "Metal tools and weapons transform society.", 33.31, 44.36));
        historicalEvents.add(new HistoricalEvent(-2560, "Great Pyramid",
                "Massive construction project demonstrates organized labor.", 29.98, 31.13));

        // Iron Age
        historicalEvents.add(new HistoricalEvent(-1200, "Iron Age Begins",
                "Iron working spreads, making tools accessible.", 39.50, 35.00));
        historicalEvents.add(new HistoricalEvent(-800, "Greek City-States Rise",
                "Democracy and philosophy emerge.", 37.98, 23.72));

        // Classical
        historicalEvents.add(new HistoricalEvent(-500, "Classical Era",
                "Golden age of Greece and early Rome.", 41.90, 12.49));
        historicalEvents.add(new HistoricalEvent(0, "New Era",
                "A new calendar era begins.", 31.76, 35.21));
        historicalEvents.add(new HistoricalEvent(476, "Fall of Rome",
                "Western Roman Empire collapses, beginning the medieval period.", 41.90, 12.49));

        // Medieval
        historicalEvents.add(new HistoricalEvent(1000, "Medieval Warming",
                "Warmer climate allows population growth.", 50.85, 4.35));
        historicalEvents.add(new HistoricalEvent(1347, "Black Death Arrives",
                "Devastating plague reaches Europe.", 38.19, 15.55));

        // Modern
        historicalEvents.add(new HistoricalEvent(1492, "Age of Exploration",
                "Columbian exchange begins.", 37.23, -6.89));
        historicalEvents.add(new HistoricalEvent(1760, "Industrial Revolution",
                "Steam power transforms production.", 53.48, -2.24));
        historicalEvents.add(new HistoricalEvent(1945, "Nuclear Age",
                "First nuclear weapons used.", 34.38, 132.45));
    }

    public void reset() {
        firedHistoricalEvents.clear();
        firedInterventionIds.clear();
        eventQueue.clear();
        activeEvents.clear();
        recentEventsHistory.clear();
        chronicleHistory.clear();
        activeInterventions.clear();
    }

    public void recordSpatialEvent(ActiveEvent event) {
        if (event == null) return;
        activeEvents.removeIf(ActiveEvent::isExpired);
        activeEvents.add(event);
        eventQueue.add(event.getFullMessage());
        recentEventsHistory.add(event);
        chronicleHistory.add(event);
        while (recentEventsHistory.size() > 100) {
            recentEventsHistory.remove(0);
        }
        while (chronicleHistory.size() > 500) {
            chronicleHistory.remove(0);
        }
    }

    public List<ActiveEvent> getActiveEvents() {
        activeEvents.removeIf(ActiveEvent::isExpired);
        return new ArrayList<>(activeEvents);
    }

    public List<ActiveEvent> getRecentEventsHistory() {
        return new ArrayList<>(recentEventsHistory);
    }

    public List<ActiveEvent> getChronicleHistory() {
        return new ArrayList<>(chronicleHistory);
    }

    public List<HistoricalIntervention> getActiveInterventions() {
        return new ArrayList<>(activeInterventions);
    }

    public void injectCustomIntervention(HistoricalIntervention intervention) {
        if (intervention == null) return;
        activeInterventions.add(intervention);
        ActiveEvent ae = new ActiveEvent(intervention, intervention.getYearStart(), 0, 1);
        recordSpatialEvent(ae);
    }

    private H3Cell findBestLandCell(List<H3Cell> cells, double targetLat, double targetLng) {
        if (cells == null || cells.isEmpty()) return null;
        H3Cell closestLand = null;
        double minLandDist = Double.MAX_VALUE;

        for (H3Cell c : cells) {
            Biome b = c.getBiome();
            boolean isLand = (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0);
            if (!isLand) continue;

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

    private H3Cell getRandomLandCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> land = cells.stream()
                .filter(c -> (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0))
                .toList();
        if (!land.isEmpty()) {
            return land.get(random.nextInt(land.size()));
        }
        return cells.get(random.nextInt(cells.size()));
    }

    private H3Cell getRandomPopulatedOrLandCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        List<H3Cell> populated = cells.stream()
                .filter(c -> ((c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0))
                        && c.getPopulation() != null && c.getPopulation() > 0)
                .toList();
        if (!populated.isEmpty()) {
            return populated.get(random.nextInt(populated.size()));
        }
        return getRandomLandCell(cells);
    }

    private H3Cell findHighestPopulationCell(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return null;
        return cells.stream()
                .filter(c -> (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN) || (c.getElevation() != null && c.getElevation() > 0))
                .max(java.util.Comparator.comparingInt(c -> c.getPopulation() != null ? c.getPopulation() : 0))
                .orElse(getRandomLandCell(cells));
    }

    /**
     * Check for events occurring at the current simulation step.
     */
    public void checkEvents(int year, int month, long totalPopulation, double totalFood, List<H3Cell> cells) {
        // 1. Historical milestone markers
        if (enableHistoricalMilestones) {
            for (HistoricalEvent event : historicalEvents) {
                if (event.year() == year && !firedHistoricalEvents.contains(event.title())) {
                    H3Cell cell = findBestLandCell(cells, event.latitude(), event.longitude());
                    double lat = (cell != null) ? cell.getLatitude() : event.latitude();
                    double lng = (cell != null) ? cell.getLongitude() : event.longitude();
                    ActiveEvent ae = new ActiveEvent(
                        "HIST_" + year,
                        "📜 HISTORIQUE : " + event.title() + " - " + event.message(),
                        "HISTORICAL",
                        lat, lng, year, month, 1, 25.0, 7.5
                    );
                    recordSpatialEvent(ae);
                    firedHistoricalEvents.add(event.title());
                }
            }
        }

        // 2. Earth Historical Contingency Leaders
        if (enableEarthHistoricalLeaders) {
            List<HistoricalIntervention> starters = historicalCatalog.getInterventionsStartingInYear(year);
            for (HistoricalIntervention hi : starters) {
                if (!firedInterventionIds.contains(hi.getId())) {
                    activeInterventions.add(hi);
                    firedInterventionIds.add(hi.getId());
                    ActiveEvent ae = new ActiveEvent(hi, year, month, 1);
                    recordSpatialEvent(ae);
                }
            }
        }

        // 3. Procedural Leader Outliers (Emergence in Sandbox/Procedural Worlds)
        if (enableProceduralLeaders) {
            HistoricalIntervention procLeader = proceduralLeaderGenerator.evaluateEmergence(year, totalPopulation, cells);
            if (procLeader != null && !firedInterventionIds.contains(procLeader.getId())) {
                activeInterventions.add(procLeader);
                firedInterventionIds.add(procLeader.getId());
                ActiveEvent ae = new ActiveEvent(procLeader, year, month, 1);
                recordSpatialEvent(ae);
            }
        }

        // 4. Apply Active Interventions Effects to local cells and nations
        applyActiveInterventions(year, cells);

        // 5. Stochastic and triggered natural disasters
        if (enableRandomEvents) {
            checkFamine(year, month, totalPopulation, totalFood, cells);
            checkPlague(year, month, totalPopulation, cells);
            checkNaturalDisasters(year, month, totalPopulation, cells);
        }
        checkAchievements(year, month, totalPopulation, cells);
    }

    private void applyActiveInterventions(int year, List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        // Clean expired interventions
        activeInterventions.removeIf(hi -> {
            if (hi.isExpired(year)) {
                hi.setCompleted(true);
                return true;
            }
            return false;
        });

        for (HistoricalIntervention hi : activeInterventions) {
            if (!hi.isActive(year)) continue;

            for (H3Cell cell : cells) {
                if (cell == null) continue;
                if (hi.isInsideRadius(cell.getLatitude(), cell.getLongitude())) {
                    // Apply physical cell modifiers
                    if (hi.getMovementFrictionMultiplier() < 1.0) {
                        cell.setMovementFriction(Math.max(0.2, cell.getMovementFriction() * hi.getMovementFrictionMultiplier()));
                    }
                    if (hi.getCapitalBonusGJ() > 0) {
                        cell.setResourceCapital(cell.getResourceCapital() + (hi.getCapitalBonusGJ() / (hi.getDurationYears() * 12.0)));
                    }
                    if (hi.getCarryingCapacityMultiplier() > 1.0) {
                        cell.setFoodResource(cell.getFoodResource() * (1.0 + (hi.getCarryingCapacityMultiplier() - 1.0) * 0.05));
                    }

                    // Apply nation socio-political modifiers if cell is owned
                    Nation owner = cell.getOwner();
                    if (owner != null) {
                        if (hi.getStateCapacityDelta() != 0) {
                            owner.setStateCapacity(Math.max(0.0, Math.min(1.0, owner.getStateCapacity() + (hi.getStateCapacityDelta() * 0.02))));
                        }
                        if (hi.getAsabiyyahDelta() != 0) {
                            owner.setAsabiyyah(Math.max(0.0, Math.min(1.0, owner.getAsabiyyah() + (hi.getAsabiyyahDelta() * 0.02))));
                        }
                        if (hi.getPoliticalInstabilityDelta() != 0) {
                            owner.setPoliticalInstability(Math.max(0.0, Math.min(1.0, owner.getPoliticalInstability() + (hi.getPoliticalInstabilityDelta() * 0.02))));
                        }
                        if (hi.getEliteOverproductionDelta() != 0) {
                            owner.setEliteOverproduction(Math.max(0.0, Math.min(1.0, owner.getEliteOverproduction() + (hi.getEliteOverproductionDelta() * 0.02))));
                        }
                    }
                }
            }
        }
    }

    public void checkEvents(int year, long totalPopulation, double totalFood) {
        checkEvents(year, 0, totalPopulation, totalFood, null);
    }

    /**
     * Check for events based on cell-level data.
     */
    public void checkCellEvents(int year, int month, List<H3Cell> cells) {
        if (!enableRandomEvents || cells == null || cells.isEmpty()) return;

        List<H3Cell> forestList = cells.stream().filter(c -> c.getBiome() == Biome.FOREST || c.getBiome() == Biome.JUNGLE).toList();
        List<H3Cell> desertList = cells.stream().filter(c -> c.getBiome() == Biome.DESERT).toList();
        long totalLandCells = cells.stream().filter(c -> c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN).count();

        // Deforestation warning
        if (totalLandCells > 0) {
            double forestRatio = (double) forestList.size() / totalLandCells;
            if (forestRatio < 0.1 && random.nextDouble() < 0.01) {
                H3Cell target = !forestList.isEmpty() ? forestList.get(random.nextInt(forestList.size())) : getRandomLandCell(cells);
                if (target != null) {
                    recordSpatialEvent(new ActiveEvent(
                        "ECO_DEFOR_" + System.currentTimeMillis(),
                        "⚠️ ÉCOLOGIE : Déforestation critique! Plus que " + String.format(java.util.Locale.ROOT, "%.1f%%", forestRatio * 100) + " de forêts.",
                        "ECOLOGICAL", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 4.5
                    ));
                }
            }
        }

        // Desertification warning
        if (totalLandCells > 0) {
            double desertRatio = (double) desertList.size() / totalLandCells;
            if (desertRatio > 0.4 && random.nextDouble() < 0.01) {
                H3Cell target = !desertList.isEmpty() ? desertList.get(random.nextInt(desertList.size())) : getRandomLandCell(cells);
                if (target != null) {
                    recordSpatialEvent(new ActiveEvent(
                        "ECO_DESERT_" + System.currentTimeMillis(),
                        "🏜️ ÉCOLOGIE : Désertification rampante! " + String.format(java.util.Locale.ROOT, "%.1f%%", desertRatio * 100) + " des terres sont de véritables déserts.",
                        "ECOLOGICAL", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 5.0
                    ));
                }
            }
        }

        // Regional famine detection (< 1 month reserve per habitant: 3.362 / 12 = 0.28017 GJ/hab)
        double monthlyNeedPerCapita = org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ / 12.0;
        List<H3Cell> starvingList = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 10 && c.getFoodResource() != null && c.getFoodResource() < c.getPopulation() * monthlyNeedPerCapita)
                .toList();

        if (starvingList.size() > cells.size() * 0.2 && random.nextDouble() < 0.05) {
            H3Cell target = starvingList.get(random.nextInt(starvingList.size()));
            recordSpatialEvent(new ActiveEvent(
                "FAMINE_REG_" + System.currentTimeMillis(),
                "🍂 FAMINE RÉGIONALE : Pénurie alimentaire grave affectant " + starvingList.size() + " mailles!",
                "FAMINE", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 6.0
            ));
        }
    }

    public void checkCellEvents(int year, List<H3Cell> cells) {
        checkCellEvents(year, 0, cells);
    }

    private void checkFamine(int year, int month, long totalPopulation, double totalFood, List<H3Cell> cells) {
        if (year - lastFamineYear < 10) return;

        double minReserveThreshold = totalPopulation * (org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * 0.25);
        if (totalFood < minReserveThreshold && totalPopulation > 500) {
            double severity = 1.0 - (totalFood / Math.max(1.0, minReserveThreshold));

            if (severity > 0.2 && random.nextDouble() < 0.08) {
                H3Cell target = getRandomPopulatedOrLandCell(cells);
                double lat = target != null ? target.getLatitude() : 0.0;
                double lng = target != null ? target.getLongitude() : 0.0;
                String msg = severity > 0.5 ? "💀 FAMINE CRITIQUE : Famine généralisée et crise de subsistance!" : "🍂 CRISE ALIMENTAIRE : Mauvaises récoltes et hausse des prix de la nourriture.";
                recordSpatialEvent(new ActiveEvent("FAMINE_" + year, msg, "FAMINE", lat, lng, year, month, 1, 20.0, 5.0 + severity * 4.0));
                lastFamineYear = year;
            }
        }
    }

    private void checkPlague(int year, int month, long totalPopulation, List<H3Cell> cells) {
        if (year - lastPlagueYear < 50) return;

        double plagueRisk = Math.min(0.01, totalPopulation / 10_000_000.0);

        if (totalPopulation > 10000 && random.nextDouble() < plagueRisk) {
            H3Cell target = getRandomPopulatedOrLandCell(cells);
            double lat = target != null ? target.getLatitude() : 0.0;
            double lng = target != null ? target.getLongitude() : 0.0;
            String msg = random.nextDouble() < 0.3 ? "☠️ PANDÉMIE MAJEURE : Une peste dévastatrice ravage les populations!" : "🤒 ÉPIDÉMIE LOCALE : Foyer infectieux propagé dans les centres urbains.";
            double mag = msg.contains("MAJEURE") ? 8.5 : 6.0;
            recordSpatialEvent(new ActiveEvent("PLAGUE_" + year, msg, "PANDEMIC", lat, lng, year, month, 1, 20.0, mag));
            lastPlagueYear = year;
        }
    }

    private void checkNaturalDisasters(int year, int month, long totalPopulation, List<H3Cell> cells) {
        H3Cell target = getRandomLandCell(cells);
        double lat = target != null ? target.getLatitude() : 0.0;
        double lng = target != null ? target.getLongitude() : 0.0;

        // Drought
        if (year - lastDroughtYear > 20 && random.nextDouble() < 0.005) {
            recordSpatialEvent(new ActiveEvent("DROUGHT_" + year, "☀️ SÉCHERESSE : Stress hydrique prolongé et assèchement des nappes.", "DROUGHT", lat, lng, year, month, 1, 20.0, 5.5));
            lastDroughtYear = year;
        }

        // Volcanic eruption
        if (year - lastVolcanoYear > 100 && random.nextDouble() < 0.001) {
            H3Cell mCell = (cells != null) ? cells.stream().filter(c -> c.getBiome() == Biome.MOUNTAINS || c.getBiome() == Biome.HILLS).findAny().orElse(target) : target;
            double vLat = mCell != null ? mCell.getLatitude() : lat;
            double vLng = mCell != null ? mCell.getLongitude() : lng;
            recordSpatialEvent(new ActiveEvent("VOLCANO_" + year, "🌋 ÉRUPTION VOLCANIQUE : Éjection massive de cendres stratosphériques!", "VOLCANO", vLat, vLng, year, month, 1, 25.0, 8.0));
            lastVolcanoYear = year;
        }

        // Earthquake
        if (random.nextDouble() < 0.002) {
            recordSpatialEvent(new ActiveEvent("EARTHQUAKE_" + year, "🌍 SÉISME / TREMBLEMENT DE TERRE : Secousse cataclysmique locale.", "EARTHQUAKE", lat, lng, year, month, 1, 20.0, 6.8));
        }

        // Flood
        if (random.nextDouble() < 0.003) {
            H3Cell floodTarget = null;
            if (cells != null && !cells.isEmpty()) {
                List<H3Cell> candidateLowlands = cells.stream()
                        .filter(c -> (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                                && ((c.getWaterResource() != null && c.getWaterResource() > 0.2)
                                    || (c.getPopulation() != null && c.getPopulation() > 0)
                                    || (c.getElevation() != null && c.getElevation() >= 0 && c.getElevation() < 400)))
                        .toList();
                if (!candidateLowlands.isEmpty()) {
                    floodTarget = candidateLowlands.get(random.nextInt(candidateLowlands.size()));
                }
            }
            if (floodTarget == null) {
                floodTarget = target;
            }
            double fLat = floodTarget != null ? floodTarget.getLatitude() : lat;
            double fLng = floodTarget != null ? floodTarget.getLongitude() : lng;
            recordSpatialEvent(new ActiveEvent("FLOOD_" + year, "🌊 INONDATION / CRUE MAJEURE : Les cours d'eau débordent de leur lit.", "FLOOD", fLat, fLng, year, month, 1, 20.0, 5.2));
        }
    }

    private void checkAchievements(int year, int month, long totalPopulation, List<H3Cell> cells) {
        H3Cell topCell = findHighestPopulationCell(cells);
        double lat = topCell != null ? topCell.getLatitude() : 0.0;
        double lng = topCell != null ? topCell.getLongitude() : 0.0;

        if (totalPopulation >= 1_000_000 && totalPopulation < 1_100_000) {
            recordSpatialEvent(new ActiveEvent("MILESTONE_1M", "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale franchit 1 million d'habitants!", "MILESTONE", lat, lng, year, month, 1, 20.0, 5.0));
        } else if (totalPopulation >= 10_000_000 && totalPopulation < 10_500_000) {
            recordSpatialEvent(new ActiveEvent("MILESTONE_10M", "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 10 millions d'habitants!", "MILESTONE", lat, lng, year, month, 1, 20.0, 6.0));
        } else if (totalPopulation >= 100_000_000 && totalPopulation < 105_000_000) {
            recordSpatialEvent(new ActiveEvent("MILESTONE_100M", "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 100 millions d'habitants!", "MILESTONE", lat, lng, year, month, 1, 20.0, 7.0));
        } else if (totalPopulation >= 1_000_000_000 && totalPopulation < 1_050_000_000) {
            recordSpatialEvent(new ActiveEvent("MILESTONE_1B", "🎉 SEUIL DÉMOGRAPHIQUE : La population mondiale atteint 1 milliard d'habitants!", "MILESTONE", lat, lng, year, month, 1, 20.0, 8.5));
        }
    }

    public void triggerEvent(String message) {
        eventQueue.add(message);
    }

    public List<String> flushEvents() {
        List<String> events = new ArrayList<>(eventQueue);
        eventQueue.clear();
        return events;
    }

    public List<String> peekEvents() {
        return new ArrayList<>(eventQueue);
    }
}
