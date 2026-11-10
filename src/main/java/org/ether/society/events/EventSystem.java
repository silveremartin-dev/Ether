/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.database.Polity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private long currentSeed = 42L;
    private final Random random = new Random(42L);

    /** Internal state: enable historical milestones (boolean). */
    private boolean enableHistoricalMilestones = true;
    /** Internal state: enable random events (boolean). */
    private boolean enableRandomEvents = true;
    /** Internal state: enable earth historical leaders (boolean). */
    private boolean enableEarthHistoricalLeaders = true;
    /** Internal state: enable procedural leaders (boolean). */
    private boolean enableProceduralLeaders = true;
    /** Internal state: enable chronicle singularity observer (boolean, disabled by default). */
    private boolean enableChronicleSingularityObserver = false;

    // Catalogs and Generators
    private final HistoricalInterventionCatalog historicalCatalog = HistoricalInterventionCatalog.getInstance();
    private final ProceduralLeaderGenerator proceduralLeaderGenerator = new ProceduralLeaderGenerator();
    private final NaturalDisasterEvaluator naturalDisasterEvaluator = new NaturalDisasterEvaluator(this, random);
    private final SpatialPhaseTransitionChronicleObserver phaseTransitionObserver = new SpatialPhaseTransitionChronicleObserver();
    private final List<HistoricalIntervention> activeInterventions = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> firedInterventionIds = new java.util.HashSet<>();

    private final java.util.Set<String> firedHistoricalEvents = new java.util.HashSet<>();
    private final List<ActiveEvent> activeEvents = new CopyOnWriteArrayList<>();
    private final List<ActiveEvent> recentEventsHistory = new CopyOnWriteArrayList<>();
    private final List<ActiveEvent> chronicleHistory = new CopyOnWriteArrayList<>();
    private org.ether.society.model.ScenarioTimeline timeline;

    /**
     * Constructs a new {@link EventSystem} and initializes canonical historical events.
     */
    public EventSystem() {
        initializeHistoricalEvents();
    }

    /**
     * Sets the PRNG seed for events and procedural generation.
     *
     * @param seed the seed parameter (long)
     */
    public void setSeed(long seed) {
        this.currentSeed = seed;
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

    public boolean isEnableChronicleSingularityObserver() {
        return enableChronicleSingularityObserver;
    }

    public void setEnableChronicleSingularityObserver(boolean enableChronicleSingularityObserver) {
        this.enableChronicleSingularityObserver = enableChronicleSingularityObserver;
    }

    public SpatialPhaseTransitionChronicleObserver getPhaseTransitionObserver() {
        return phaseTransitionObserver;
    }

    private void initializeHistoricalEvents() {
        historicalEvents.addAll(DefaultHistoricalEventsCatalog.getCanonicalEvents());
    }

    /**
     * Resets event history, cooldowns, and PRNG.
     */
    public void reset() {
        firedHistoricalEvents.clear();
        firedInterventionIds.clear();
        eventQueue.clear();
        activeEvents.clear();
        recentEventsHistory.clear();
        chronicleHistory.clear();
        activeInterventions.clear();
        naturalDisasterEvaluator.reset();
        phaseTransitionObserver.reset();
        this.random.setSeed(currentSeed);
        this.proceduralLeaderGenerator.setSeed(currentSeed);
    }

    /**
     * Records an active spatial event in the event queue and chronicle.
     *
     * @param event the event parameter (ActiveEvent)
     */
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
        if (timeline != null) {
            timeline.addEntry(
                event.getYear(),
                event.getType() != null ? event.getType() : "SPATIAL_EVENT",
                event.getTitle() != null ? event.getTitle() : "Event",
                event.getTitle() != null ? event.getTitle() : "",
                false,
                event.getLatitude(),
                event.getLongitude(),
                event.getMagnitude(),
                Math.max(1, event.getDurationDays())
            );
        }
    }

    public org.ether.society.model.ScenarioTimeline getTimeline() {
        return timeline;
    }

    public void setTimeline(org.ether.society.model.ScenarioTimeline timeline) {
        this.timeline = timeline;
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

    /**
     * Check for events occurring at the current simulation step.
     */
    public void checkEvents(int year, int month, long totalPopulation, double totalFood, List<H3Cell> cells) {
        // 1. Historical milestone markers
        if (enableHistoricalMilestones) {
            for (HistoricalEvent event : historicalEvents) {
                if (event.year() == year && !firedHistoricalEvents.contains(event.title())) {
                    H3Cell cell = naturalDisasterEvaluator.findBestLandCell(cells, event.latitude(), event.longitude());
                    double lat = (cell != null && cell.getLatitude() != null) ? cell.getLatitude() : event.latitude();
                    double lng = (cell != null && cell.getLongitude() != null) ? cell.getLongitude() : event.longitude();
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
                    H3Cell snapCell = naturalDisasterEvaluator.findBestPopulatedOrLandCellNear(cells, hi.getLatitude(), hi.getLongitude());
                    double sLat = (snapCell != null && snapCell.getLatitude() != null) ? snapCell.getLatitude() : hi.getLatitude();
                    double sLng = (snapCell != null && snapCell.getLongitude() != null) ? snapCell.getLongitude() : hi.getLongitude();
                    String tagPrefix = "👑 LEADER HISTORIQUE";
                    ActiveEvent ae = new ActiveEvent(
                        hi.getId(),
                        tagPrefix + " [Mag. " + String.format(Locale.ROOT, "%.1f", hi.getMagnitude()) + "] : " + hi.getName() + " (" + hi.getArchetype().getDisplayName() + ")",
                        "LEADER",
                        sLat, sLng, year, month, 1, hi.getRadiusKm(), hi.getMagnitude()
                    );
                    recordSpatialEvent(ae);
                }
            }
        }

        // 3. Procedural Leader Outliers
        if (enableProceduralLeaders) {
            HistoricalIntervention procLeader = proceduralLeaderGenerator.evaluateEmergence(year, totalPopulation, cells);
            if (procLeader != null && !firedInterventionIds.contains(procLeader.getId())) {
                activeInterventions.add(procLeader);
                firedInterventionIds.add(procLeader.getId());
                String tagPrefix = "👑 ÉMERGENCE DIRIGEANT";
                ActiveEvent ae = new ActiveEvent(
                    procLeader.getId(),
                    tagPrefix + " [Mag. " + String.format(Locale.ROOT, "%.1f", procLeader.getMagnitude()) + "] : " + procLeader.getName() + " (" + procLeader.getArchetype().getDisplayName() + ")",
                    "LEADER",
                    procLeader.getLatitude(), procLeader.getLongitude(), year, month, 1, procLeader.getRadiusKm(), procLeader.getMagnitude()
                );
                recordSpatialEvent(ae);
            }
        }

        // 4. Apply Active Interventions Effects
        applyActiveInterventions(year, cells);

        // 5. Stochastic and triggered natural disasters
        if (enableRandomEvents) {
            naturalDisasterEvaluator.checkFamine(year, month, totalPopulation, totalFood, cells);
            naturalDisasterEvaluator.checkPlague(year, month, totalPopulation, cells);
            naturalDisasterEvaluator.checkNaturalDisasters(year, month, totalPopulation, cells);
        }
        naturalDisasterEvaluator.checkAchievements(year, month, totalPopulation, cells, firedHistoricalEvents);
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
                if (cell == null || cell.getLatitude() == null || cell.getLongitude() == null) continue;
                if (hi.isInsideRadius(cell.getLatitude(), cell.getLongitude())) {
                    if (hi.getMovementFrictionMultiplier() < 1.0) {
                        double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                        cell.setMovementFriction(Math.max(0.2, friction * hi.getMovementFrictionMultiplier()));
                    }
                    if (hi.getCapitalBonusGJ() != 0) {
                        double cap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                        cell.setResourceCapital(Math.max(0.0, cap + (hi.getCapitalBonusGJ() / (Math.max(1, hi.getDurationYears()) * 12.0))));
                    }
                    if (hi.getCarryingCapacityMultiplier() > 1.0) {
                        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
                        cell.setFoodResource(food * (1.0 + (hi.getCarryingCapacityMultiplier() - 1.0) * 0.05));
                    }

                    Polity owner = cell.getOwner();
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

    public void checkCellEvents(int year, int month, List<H3Cell> cells) {
        if (!enableRandomEvents || cells == null || cells.isEmpty()) return;
        if (month != 0) return;

        int forestCount = 0;
        int desertCount = 0;
        int totalLandCells = 0;
        H3Cell sampleForest = null;
        H3Cell sampleDesert = null;
        H3Cell sampleStarving = null;
        int starvingCount = 0;
        double monthlyNeedPerCapita = org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ / 12.0;

        for (H3Cell c : cells) {
            if (c == null) continue;
            Biome b = c.getBiome();
            if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                totalLandCells++;
                if (b == Biome.FOREST || b == Biome.JUNGLE) {
                    forestCount++;
                    if (sampleForest == null || random.nextDouble() < 0.05) sampleForest = c;
                } else if (b == Biome.DESERT) {
                    desertCount++;
                    if (sampleDesert == null || random.nextDouble() < 0.05) sampleDesert = c;
                }
            }
            Integer pop = c.getPopulation();
            Double food = c.getFoodResource();
            if (pop != null && pop > 10 && food != null && food < pop * monthlyNeedPerCapita) {
                starvingCount++;
                if (sampleStarving == null || random.nextDouble() < 0.05) sampleStarving = c;
            }
        }

        if (totalLandCells > 0) {
            double forestRatio = (double) forestCount / totalLandCells;
            if (forestRatio < 0.1 && random.nextDouble() < 0.01) {
                H3Cell target = sampleForest != null ? sampleForest : naturalDisasterEvaluator.getRandomLandCell(cells);
                if (target != null && target.getLatitude() != null && target.getLongitude() != null) {
                    recordSpatialEvent(new ActiveEvent(
                        "ECO_DEFOR_" + year,
                        "⚠️ ÉCOLOGIE : Déforestation critique! Plus que " + String.format(Locale.ROOT, "%.1f%%", forestRatio * 100) + " de forêts.",
                        "ECOLOGICAL", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 4.5
                    ));
                }
            }
        }

        if (totalLandCells > 0) {
            double desertRatio = (double) desertCount / totalLandCells;
            if (desertRatio > 0.4 && random.nextDouble() < 0.01) {
                H3Cell target = sampleDesert != null ? sampleDesert : naturalDisasterEvaluator.getRandomLandCell(cells);
                if (target != null && target.getLatitude() != null && target.getLongitude() != null) {
                    recordSpatialEvent(new ActiveEvent(
                        "ECO_DESERT_" + year,
                        "🏜️ ÉCOLOGIE : Désertification rampante! " + String.format(Locale.ROOT, "%.1f%%", desertRatio * 100) + " des terres sont de véritables déserts.",
                        "ECOLOGICAL", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 5.0
                    ));
                }
            }
        }

        if (starvingCount > cells.size() * 0.2 && random.nextDouble() < 0.05) {
            H3Cell target = sampleStarving != null ? sampleStarving : naturalDisasterEvaluator.getRandomLandCell(cells);
            if (target != null && target.getLatitude() != null && target.getLongitude() != null) {
                recordSpatialEvent(new ActiveEvent(
                    "FAMINE_REG_" + year,
                    "🍂 FAMINE RÉGIONALE : Pénurie alimentaire grave affectant " + starvingCount + " mailles!",
                    "FAMINE", target.getLatitude(), target.getLongitude(), year, month, 1, 20.0, 6.0
                ));
            }
        }
    }

    public void checkCellEvents(int year, List<H3Cell> cells) {
        checkCellEvents(year, 0, cells);
    }

    /**
     * Evaluates spatial and societal singularities across cells when the chronicle observer is enabled.
     *
     * @param year     current simulation year
     * @param month    current simulation month
     * @param cells    active cell list
     * @param scenario current scenario configuration
     */
    public void checkSingularities(int year, int month, List<H3Cell> cells, org.ether.society.model.Scenario scenario) {
        checkPhaseTransitions(year, month, cells, scenario);
    }

    /**
     * Evaluates spatial phase transitions across active domain cells and emits calibrated chronicle events.
     *
     * @param year     current simulation year
     * @param month    current simulation month
     * @param cells    active cell list
     * @param scenario current scenario configuration
     */
    public void checkPhaseTransitions(int year, int month, List<H3Cell> cells, org.ether.society.model.Scenario scenario) {
        if (!enableChronicleSingularityObserver || cells == null || cells.isEmpty()) {
            return;
        }
        phaseTransitionObserver.evaluatePhaseTransitions(year, month, cells, this, scenario, random);
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
