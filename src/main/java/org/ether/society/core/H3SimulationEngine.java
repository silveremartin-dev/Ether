/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.Configuration;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.density.H3ClimateSystem;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ether.society.persistence.GameSaveManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H3-based simulation engine.
 * Manages the deterministic simulation loop grounded strictly in physical laws.
 */
public class H3SimulationEngine implements ISimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    private final Configuration config;
    private final TimeManager timeManager;
    private final org.ether.society.events.EventSystem eventSystem;

    // Density-based simulation systems
    private final H3ClimateSystem climateSystem;
    private final org.ether.society.agents.AgentManager agentManager;
    private final org.ether.society.gpu.GPUManager gpuManager;

    private final org.ether.society.analytics.HistoryManager historyManager;
    private final org.ether.society.diplomacy.DiplomacyManager diplomacyManager;
    private final org.ether.society.diplomacy.PoliticalSimulationEngine politicalEngine;
    private final GameSaveManager gameSaveManager;

    // DOD Layer
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private org.ether.society.core.dod.AgentBuffer agentBuffer;
    private org.ether.society.flux.FluxEngine fluxEngine;
    private org.ether.society.core.dod.DemographicKernel demographicKernel;
    private org.ether.society.core.dod.UrbanKernel urbanKernel;
    private org.ether.society.core.dod.CultureKernel cultureKernel;
    private org.ether.society.core.dod.EnvironmentalKernel environmentalKernel;
    private org.ether.society.core.dod.StatisticsKernel statisticsKernel;

    private long lastTickTime = 0;
    private double currentTPS = 0;
    private float currentGini = 0;
    private float currentGDP = 0;
    private float currentLifeExpectancy = 0;
    private float currentFertility = 0;
    private int[] densityDistribution = new int[20]; // 20 bins

    private List<H3Cell> cells;
    private Scenario currentScenario;

    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int speedMultiplier = 1;

    public H3SimulationEngine(Configuration config) {
        this.config = config;
        this.timeManager = new TimeManager(config.simulation().startYear());
        this.eventSystem = new org.ether.society.events.EventSystem();
        this.climateSystem = new H3ClimateSystem();
        this.agentManager = new org.ether.society.agents.AgentManager(org.ether.society.h3.H3Service.getInstance());

        // Initialize GPU Manager
        this.gpuManager = new org.ether.society.gpu.GPUManager();
        this.climateSystem.setGpuManager(this.gpuManager);

        // Initialize History Manager
        this.historyManager = new org.ether.society.analytics.HistoryManager();

        // Initialize Diplomacy & Politics
        this.diplomacyManager = new org.ether.society.diplomacy.DiplomacyManager();
        this.politicalEngine = new org.ether.society.diplomacy.PoliticalSimulationEngine(this.diplomacyManager);
        this.gameSaveManager = new GameSaveManager();

        this.fluxEngine = new org.ether.society.flux.FluxEngine();
        this.demographicKernel = new org.ether.society.core.dod.DemographicKernel();
        this.urbanKernel = new org.ether.society.core.dod.UrbanKernel();
        this.cultureKernel = new org.ether.society.core.dod.CultureKernel();
        this.environmentalKernel = new org.ether.society.core.dod.EnvironmentalKernel();
        this.statisticsKernel = new org.ether.society.core.dod.StatisticsKernel();

        initialize();
    }

    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
    }

    private void initialize() {
        logger.info("Initializing H3 simulation engine (idle state)...");
        this.cells = new java.util.ArrayList<>();
        if (this.diplomacyManager != null) {
            this.diplomacyManager.clear();
        }
        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);
    }

    public void initializeFromScenario(Scenario scenario, List<H3Cell> cells) {
        logger.info("Initializing from scenario: {}", scenario.getName());

        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }

        this.currentScenario = scenario;
        this.cells = cells;

        timeManager.reset((int) scenario.getStartDateYear());
        if (eventSystem != null) {
            eventSystem.reset();
        }

        if (diplomacyManager != null) {
            diplomacyManager.clear();
        }

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(cells);

        int populatedCount = (int) cells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        initializePoliticalSeeding(populatedCount);

        int cohortSize = scenario != null && scenario.getTargetCohortSize() > 0 ? scenario.getTargetCohortSize() : 500;
        if (demographicKernel != null) {
            demographicKernel.setTargetCohortSize(cohortSize);
        }

        long initialPop = scenario != null ? scenario.getInitialHumanCount() : 1_000_000L;
        int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));

        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(Math.max(1000, estimatedAgents * 2));
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize);

        if (historyManager != null) {
            historyManager.reset();
        }
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    private void initializePopulation() {
        boolean hasExistingPop = cells.stream().anyMatch(c -> c.getPopulation() > 0);
        if (hasExistingPop) {
            int count = (int) cells.stream().filter(c -> c.getPopulation() > 0).count();
            initializePoliticalSeeding(count);
            return;
        }

        int populatedCells = 0;
        for (H3Cell cell : cells) {
            org.ether.society.model.Biome biome = cell.getBiome();
            if (biome == org.ether.society.model.Biome.OCEAN || biome == org.ether.society.model.Biome.DEEP_OCEAN) {
                continue;
            }

            int basePop = switch (biome) {
                case PLAINS -> 50;
                case FOREST -> 30;
                case JUNGLE -> 40;
                case HILLS -> 20;
                case BEACH -> 25;
                case MOUNTAINS -> 10;
                case TUNDRA -> 5;
                case DESERT -> 3;
                case SNOW -> 2;
                default -> 0;
            };

            if (Math.random() < 0.1 && basePop > 0) {
                cell.setPopulation(basePop);
                populatedCells++;
            }
        }

        initializePoliticalSeeding(populatedCells);
    }

    @Override
    public void start() {
        if (running.getAndSet(true)) return;
        startGameLoop();
    }

    @Override
    public void pause() {
        if (!running.getAndSet(false)) return;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void reset() {
        pause();
        timeManager.reset(config.simulation().startYear());
        historyManager.reset();
        initialize();
    }

    @Override
    public void setSpeed(int multiplier) {
        this.speedMultiplier = multiplier;
        if (running.get()) {
            pause();
            start();
        }
    }

    @Override
    public int getSpeed() {
        return speedMultiplier;
    }

    @Override
    public TimeManager getTimeManager() {
        return timeManager;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    public List<H3Cell> getCells() {
        return cells;
    }

    public void setCells(List<H3Cell> newCells) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();

        this.cells = newCells;
        if (diplomacyManager != null) diplomacyManager.clear();

        if (newCells != null && !newCells.isEmpty()) {
            initializePopulation();
            int cohortSize = currentScenario != null && currentScenario.getTargetCohortSize() > 0 ? currentScenario.getTargetCohortSize() : 500;
            if (demographicKernel != null) {
                demographicKernel.setTargetCohortSize(cohortSize);
            }
            long initialPop = currentScenario != null ? currentScenario.getInitialHumanCount() : 1_000_000L;
            int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));

            this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());
            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(Math.max(1000, estimatedAgents * 2));
            org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize);
        } else {
            this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);
            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);
        }
    }

    public void saveGame(String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        gameSaveManager.saveGame(this, saveName);
        if (wasRunning) start();
    }

    public void loadGame(String saveId) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        gameSaveManager.loadGame(saveId, this);
    }

    public H3ClimateSystem getClimateSystem() {
        return climateSystem;
    }

    private void startGameLoop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "H3SimulationEngine-Thread");
            t.setDaemon(true);
            return t;
        });
        long period = speedMultiplier >= 100 ? 1 : Math.max(1, config.simulation().tickRateMs() / Math.max(1, speedMultiplier));
        executorService.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        running.set(false);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (saveExecutor != null && !saveExecutor.isShutdown()) {
            saveExecutor.shutdownNow();
        }
    }

    private Runnable onTickCallback;

    public void setOnTickCallback(Runnable callback) {
        this.onTickCallback = callback;
    }

    private int tickCounter = 0;

    private void tick() {
        if (!running.get()) return;

        long now = System.nanoTime();
        if (lastTickTime != 0) {
            double diff = (now - lastTickTime) / 1_000_000_000.0;
            if (diff > 0) {
                double instantTPS = 1.0 / diff;
                currentTPS = (currentTPS <= 0) ? instantTPS : (currentTPS * 0.90 + instantTPS * 0.10);
            }
        }
        lastTickTime = now;

        try {
            final float DT_FAST = 86400f; 
            final int SLOW_FACTOR = 30;

            // 1. FAST SCALE DYNAMICS
            fluxEngine.tick(worldBuffer, DT_FAST);
            politicalEngine.tick(cells);

            // 2. SLOW SCALE PHYSICALIST DYNAMICS
            if (tickCounter % SLOW_FACTOR == 0) {
                timeManager.advanceMonth();
                int month = timeManager.getCurrentMonth();
                float dtSlow = DT_FAST * SLOW_FACTOR;

                climateSystem.updateClimate(cells, month);
                syncClimateToBuffer();
                environmentalKernel.tick(worldBuffer, month, dtSlow);

                demographicKernel.tick(worldBuffer, agentBuffer, dtSlow);
                urbanKernel.tick(worldBuffer, dtSlow);
                cultureKernel.tick(worldBuffer, agentBuffer, dtSlow);

                // --- GROUNDED PHYSICAL & CLIODYNAMIC ENGINES ---
                double avgTech = getAverageTechnology();

                // Step 1: Solar/Wind Radiance & Atmosphere
                org.ether.society.procedural.RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(cells);
                org.ether.society.procedural.AtmosphericOxygenEngine.processAtmosphericOxygen(cells, 0.21, 1.0);
                org.ether.society.procedural.WetBulbTemperatureEngine.processWetBulbHyperthermia(cells);
                org.ether.society.procedural.AlbedoClimateEngine.processAlbedoFeedback(cells);

                // Step 2: Soil Nutrients, Aquifer & Erosion
                org.ether.society.procedural.SoilNutrientNPKEngine.processSoilNutrients(cells);
                org.ether.society.procedural.DeforestationErosionEngine.processDeforestationErosion(cells);
                org.ether.society.procedural.AquiferDepletionEngine.processAquiferDepletion(cells);
                org.ether.society.procedural.EcologicalDegradationEngine.processEcologicalDegradation(cells, avgTech);

                // Step 3: Demographics & Bio-molecular Epidemiology
                org.ether.society.procedural.BiologicalDemographicsEngine.processBiologicalDemographics(cells);
                org.ether.society.procedural.BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells);
                org.ether.society.procedural.EcotoxicologyFertilityEngine.processEcotoxicologyFertility(cells);

                // Step 4: EROEI, Energy Conversion & Metallurgy Enthalpy
                org.ether.society.procedural.PhysicalEnergyGridEngine.processPhysicalEnergyGrid(cells);
                org.ether.society.procedural.NetEnergyEROEIEngine.processNetEnergyEROEI(cells);
                org.ether.society.procedural.MetallurgyEnthalpyEngine.processOreSmelting(cells);
                org.ether.society.procedural.ResourceRecyclingEngine.processResourceRecycling(cells);
                org.ether.society.procedural.NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(cells);
                org.ether.society.procedural.OzoneLayerDepletionEngine.processOzoneLayerDepletion(cells);

                // Step 5: Mechanical Transport Work & Kinetic Warfare
                org.ether.society.procedural.PhysicsTransportEngine.processPhysicsTransport(cells);
                org.ether.society.procedural.ThermodynamicWarfareEngine.processKineticWarfare(cells);
                org.ether.society.procedural.NuclearWarfareClimateEngine.processNuclearWarfareClimate(cells);
                org.ether.society.procedural.InfrastructureEnergyEngine.processInfrastructureEnergy(cells);
                org.ether.society.procedural.ThermodynamicMigrationEngine.processThermodynamicMigration(cells);

                // Step 6: Shannon Information Capacity & Evolution
                org.ether.society.procedural.InformationEntropyEngine.processInformationEntropy(cells);
                org.ether.society.procedural.MegafaunaEcosystemEngine.processMegafaunaEcosystem(cells);
                org.ether.society.procedural.SelectiveBreedingEngine.processSelectiveBreeding(cells);
                org.ether.society.procedural.TechnologicalSingularityEngine.processTechnologicalSingularity(cells);
                org.ether.society.procedural.TechTreeEngine.processTechnologyDiffusion(cells, null);

                // Step 7: Advanced Physicalist & Cliodynamic Extensions
                org.ether.society.procedural.TerraformingEngine.processTerraforming(cells, 1.0);
                org.ether.society.procedural.TrophicEcosystemEngine.processTrophicEcosystem(cells, 1.0);
                org.ether.society.procedural.PhysicalSupplyChainEngine.processSupplyChains(cells, 1.0);
                org.ether.society.procedural.UrbanThermodynamicsEngine.processUrbanThermodynamics(cells, 1.0);
                org.ether.society.procedural.PhysicalLawEngine.applyPhysicalLaws(cells, 1.0);
                org.ether.society.procedural.CulturalSociologyEngine.processCulturalSociology(cells, 1.0);
                org.ether.society.procedural.CoGovernanceTradeEngine.processTradeAndGovernance(cells, 1.0);
                org.ether.society.procedural.OreGradeThermodynamicsEngine.processOreDepletion(cells, 1.0);
                org.ether.society.procedural.InfrastructureInertiaEngine.processInfrastructureInertia(cells, 1.0);
                org.ether.society.procedural.EntropicMetalDissipationEngine.processEntropicDissipation(cells, 1.0);
                org.ether.society.procedural.JevonsParadoxEngine.processJevonsRebound(cells, 1.0);
                org.ether.society.procedural.World3CouplingEngine.processWorld3System(cells, 1.0);
                org.ether.society.procedural.KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(cells, 1.0);
                org.ether.society.procedural.BifurcationChaosEngine.processBifurcationAnalysis(cells, 1.0);
                org.ether.society.procedural.DynamicHydrographicSiltationEngine.processHydrographicSiltation(cells, 1.0);
                org.ether.society.procedural.PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(cells, 1.0);
                org.ether.society.procedural.GeoengineeringAlbedoFeedbackEngine.processGeoengineeringAlbedo(cells, 1.0);
                org.ether.society.procedural.ProceduralEngineRegistry.processPlugins(cells, 1.0);

                // Statistics
                currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());
                densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);
                currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());
                currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds());
                currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

                historyManager.captureSnapshot(this);
                historyManager.captureWorldSnapshot(this);
                eventSystem.checkEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), getTotalPopulation(), getTotalFood(), cells);
                eventSystem.checkCellEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), cells);

                syncBufferToCells();
            }

            // Every 60 Ticks: trigger rolling checkpoint auto-save
            if (tickCounter > 0 && tickCounter % 60 == 0) {
                autoSaveCheckpoint();
            }

            agentManager.update();
            tickCounter++;

            if (onTickCallback != null) {
                onTickCallback.run();
            }

        } catch (Exception e) {
            logger.error("Error during simulation tick", e);
        }
    }

    @Override
    public void stepForward(int ticks) {
        pause();
        for (int i = 0; i < Math.max(1, ticks); i++) {
            boolean wasRunning = running.getAndSet(true);
            try {
                tick();
            } finally {
                running.set(wasRunning);
            }
        }
    }

    @Override
    public void stepBackward(int ticks) {
        pause();
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        long currentTicks = timeManager.getTotalTicks();
        long targetTicks = Math.max(0, currentTicks - ticks);

        java.util.NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();
        if (snapshots.isEmpty()) return;

        java.util.Map.Entry<Long, List<H3Cell>> entry = snapshots.floorEntry(targetTicks);
        if (entry == null) {
            entry = snapshots.firstEntry();
        }

        if (entry != null && entry.getValue() != null) {
            List<H3Cell> snapshot = entry.getValue();
            java.util.Map<Long, H3Cell> map = snapshot.stream().collect(java.util.stream.Collectors.toMap(H3Cell::getH3Index, c -> c));
            for (H3Cell c : cells) {
                H3Cell snap = map.get(c.getH3Index());
                if (snap != null) {
                    c.setPopulation(snap.getPopulation());
                    c.setTemperature(snap.getTemperature());
                    c.setFoodResource(snap.getFoodResource());
                    c.setWaterResource(snap.getWaterResource());
                    c.setFreshwaterAquifer(snap.getFreshwaterAquifer());
                    c.setTechnologyLevel(snap.getTechnologyLevel());
                }
            }
            if (worldBuffer != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            }
        }
    }

    private final java.util.concurrent.ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-AutoSave-Thread");
        t.setDaemon(true);
        return t;
    });

    private void autoSaveCheckpoint() {
        if (cells == null || cells.isEmpty()) return;
        final int currentTick = tickCounter;
        saveExecutor.submit(() -> {
            try {
                gameSaveManager.saveCheckpoint(this, currentTick);
            } catch (Exception ex) {
                logger.error("Failed to save 60-tick checkpoint", ex);
            }
        });
    }

    public void syncBufferToCells() {
        if (worldBuffer == null || cells == null) return;
        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] water = worldBuffer.getWaterResource();
        float[] wood = worldBuffer.getWoodResource();
        float[] gini = worldBuffer.getGiniIndex();

        for (int i = 0; i < cells.size() && i < worldBuffer.getCapacity(); i++) {
            H3Cell cell = cells.get(i);
            int p = (int) Math.max(0, pop[i]);
            cell.setPopulation(p);
            cell.setBiomassHuman((double) pop[i]);
            cell.setFoodResource((double) food[i]);
            cell.setTechnologyLevel((double) tech[i]);
            cell.setWaterResource((double) water[i]);
            cell.setWoodResource((double) wood[i]);
            cell.setGiniIndex((double) gini[i]);
        }
    }

    public long getTotalPopulation() {
        if (worldBuffer == null) return 0;
        long total = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += (long)pop[i];
        }
        return total;
    }

    public double getTotalFood() {
        if (worldBuffer == null) return 0;
        double total = 0;
        float[] food = worldBuffer.getFoodResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += food[i];
        }
        return total;
    }

    public long getPopulatedCellCount() {
        if (worldBuffer == null) return 0;
        long count = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            if (pop[i] > 0.1f) count++;
        }
        return count;
    }

    private void initializePoliticalSeeding(int populatedCount) {
        if (populatedCount < 3 || cells == null || cells.isEmpty()) return;

        if (diplomacyManager != null) {
            diplomacyManager.clear();
        }

        cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .sorted(java.util.Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .limit(3)
                .forEach(c -> {
                    if (c.getOwner() == null) {
                        String name = "Realm of Hex " + Long.toHexString(c.getH3Index()).toUpperCase();
                        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(Math.random() * 360, 0.8, 0.9);
                        org.ether.society.model.Nation nation = new org.ether.society.model.Nation(name, color, c);
                        diplomacyManager.registerNation(nation);
                    }
                });
    }

    public org.ether.society.agents.AgentManager getAgentManager() {
        return agentManager;
    }

    public org.ether.society.analytics.HistoryManager getHistoryManager() {
        return historyManager;
    }

    public double getCurrentTPS() {
        return currentTPS;
    }

    public float getTotalBiomassNatural() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] bio = worldBuffer.getBiomassNatural();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += bio[i];
        return total;
    }

    public float getAverageTechnology() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += tech[i];
        return total / worldBuffer.getCapacity();
    }

    public float getCurrentGini() {
        return currentGini;
    }

    public int[] getDensityDistribution() {
        return densityDistribution;
    }

    public float getCurrentGDP() { return currentGDP; }
    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }
    public float getCurrentFertility() { return currentFertility; }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS ---

    public double getEnergyCaptured() {
        if (worldBuffer == null) return 0;
        double energy = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            energy += (100.0 + tech[i] * 15.0) * (pop[i] > 0 ? 1 : 0);
        }
        return energy;
    }

    public double getResourceDepletionRate() {
        if (worldBuffer == null || worldBuffer.getCapacity() == 0) return 0;
        double maxRes = worldBuffer.getCapacity() * 1000.0;
        double currentRes = 0;
        float[] res = worldBuffer.getResourceCapital();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) currentRes += res[i];
        return Math.max(0, Math.min(100.0, (1.0 - currentRes / Math.max(1, maxRes)) * 100.0));
    }

    public double getEnergyPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? getEnergyCaptured() / pop : 0;
    }

    public double getFoodPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? getTotalFood() / pop : 0;
    }

    public double getBiomassDomesticated() {
        return getTotalPopulation() * 0.15 + getTotalFood() * 0.4;
    }

    public double getPotableWaterTotal() {
        if (worldBuffer == null) return 0;
        double water = 0;
        float[] w = worldBuffer.getWaterResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) water += w[i];
        return water;
    }

    public double getRemainingResourcesRatio() {
        return 100.0 - getResourceDepletionRate();
    }

    public double getSystemicEntropy() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.05 + tech * 2.5) % 1000.0;
    }

    public double getPollutionLevel() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return Math.max(0, (tech > 50 ? (tech - 50) * 1.5 * (pop / 100000.0) : 0));
    }

    public double getOccupiedTerritoryArea() {
        return getPopulatedCellCount() * 1250.0;
    }

    public double getOffspringPercentage() {
        float fert = getCurrentFertility();
        return Math.min(95.0, Math.max(20.0, 40.0 + fert * 12.0));
    }

    public double getAgeAtFirstChild() {
        float tech = getAverageTechnology();
        return Math.min(32.0, Math.max(16.0, 18.0 + (tech / 200.0) * 10.0));
    }

    public double getImmigrationRate() {
        long pop = getTotalPopulation();
        return pop > 0 ? (pop % 1000) / 10.0 : 0;
    }

    public double getEducationLevel() {
        float tech = getAverageTechnology();
        return Math.min(100.0, (tech / 250.0) * 100.0);
    }

    public double getHappinessIndex() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        double foodPerCap = getFoodPerCapita();
        double base = (life / 80.0) * 50.0 + Math.min(50.0, foodPerCap * 10.0) - (gini * 30.0);
        return Math.max(0.0, Math.min(100.0, base));
    }

    public double getConflictLevel() {
        float gini = getCurrentGini();
        double happiness = getHappinessIndex();
        return Math.max(0.0, Math.min(100.0, (gini * 60.0) + (100.0 - happiness) * 0.4));
    }

    public int getCityStatesCount() {
        long popCells = getPopulatedCellCount();
        return (int) Math.max(1, popCells / 5);
    }

    public double getInstitutionalMaturity() {
        float tech = getAverageTechnology();
        return Math.min(100.0, tech * 0.45);
    }

    public double getDivisionOfLaborIndex() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return Math.min(100.0, (tech * 0.5) + Math.log10(Math.max(1, pop)) * 5.0);
    }

    public int getMaxHierarchyLevel() {
        float tech = getAverageTechnology();
        if (tech < 10) return 1;
        if (tech < 30) return 2;
        if (tech < 60) return 3;
        if (tech < 100) return 4;
        if (tech < 200) return 5;
        return 6;
    }

    public long getLargestCulturalUnitSize() {
        long pop = getTotalPopulation();
        return (long) (pop * Math.min(0.85, 0.2 + (getAverageTechnology() / 300.0)));
    }

    public double getKardashevScale() {
        double energy = getEnergyCaptured();
        if (energy <= 0) return 0.0;
        double watts = energy * 1e6;
        double k = (Math.log10(Math.max(1.0, watts)) - 6.0) / 10.0;
        return Math.max(0.0, Math.min(3.0, k));
    }

    public double getBuiltCapitalTotal() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return pop * (5.0 + tech * 12.0);
    }

    public double getEliteFormationRatio() {
        float gini = getCurrentGini();
        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));
    }

    public double getElderCapitalShare() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));
    }

    public double getLandRentIndex() {
        long pop = getTotalPopulation();
        long cells = getPopulatedCellCount();
        double density = cells > 0 ? (double) pop / cells : 0;
        return density * 1.5 + getAverageTechnology() * 0.8;
    }

    public long getToolsCount() {
        float tech = getAverageTechnology();
        return (long) (getTotalPopulation() * (1.2 + tech * 0.5));
    }

    public long getProductsCount() {
        float tech = getAverageTechnology();
        return (long) (10.0 + Math.pow(tech, 1.8));
    }

    public double getSystemComplexityIndex() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    public double getReconstructionCapabilityIndex() {
        double edu = getEducationLevel();
        float tech = getAverageTechnology();
        return Math.min(100.0, (edu * 0.7 + tech * 0.3));
    }

    public double getSystemInterdependenceIndex() {
        double complexity = getSystemComplexityIndex();
        return Math.min(100.0, complexity * 0.95);
    }

    public int[] getAgePyramid() {
        long pop = getTotalPopulation();
        int youth = (int) (pop * 0.35);
        int adult = (int) (pop * 0.50);
        int elder = (int) (pop * 0.15);
        return new int[]{youth, adult, elder};
    }

    // --- 🧠 COGNITION & INFORMATION ---
    public double getShannonBandwidth() {
        float tech = getAverageTechnology();
        return 1.0 + Math.pow(tech, 1.4) * 0.8;
    }

    public double getCollectiveMemoryStock() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.05 + Math.pow(tech, 2.1));
    }

    public double getInnovationDiffusionSpeed() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    public double getKnowledgeDecayRate() {
        float gini = getCurrentGini();
        double conflict = getConflictLevel();
        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));
    }

    // --- 🌍 ÉCOLOGIE & FRONTIÈRES PLANÉTAIRES ---
    public double getSoilNPKQuality() {
        float tech = getAverageTechnology();
        double resDep = getResourceDepletionRate();
        return Math.max(5.0, 100.0 - (resDep * 0.6) + Math.min(15.0, tech * 0.1));
    }

    public double getCarbonFootprint() {
        double energy = getEnergyCaptured();
        float tech = getAverageTechnology();
        return (energy * (tech > 40 && tech < 180 ? 0.85 : 0.2)) / 1000.0;
    }

    public double getWildBiodiversityIndex() {
        float bioNat = getTotalBiomassNatural();
        double bioDom = getBiomassDomesticated();
        double total = bioNat + bioDom;
        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;
    }

    public double getWetBulbSafetyMargin() {
        float temp = 15.0f;
        float[] temps = worldBuffer != null ? worldBuffer.getTemperature() : null;
        if (temps != null && temps.length > 0) {
            float sum = 0; for(float t : temps) sum += t;
            temp = sum / temps.length;
        }
        return Math.max(0.0, 35.0 - (temp + 3.5));
    }

    // --- ⏳ CLIODYNAMIQUE & RISQUES SYSTÉMIQUES ---
    public double getEliteOverproductionIndex() {
        double eliteForm = getEliteFormationRatio();
        float gini = getCurrentGini();
        return Math.min(10.0, (eliteForm / 5.0) * (1.0 + gini * 2.0));
    }

    public double getFiscalStressIndex() {
        double landRent = getLandRentIndex();
        float gini = getCurrentGini();
        return Math.min(100.0, (gini * 50.0) + (landRent * 0.3));
    }

    public double getGeopoliticalTension() {
        int cityStates = getCityStatesCount();
        double conflict = getConflictLevel();
        return Math.min(100.0, (cityStates * 2.5) + (conflict * 0.7));
    }

    public double getCollapseVulnerability() {
        double resDep = getResourceDepletionRate();
        double psi = getEliteOverproductionIndex();
        double entropy = getSystemicEntropy();
        return Math.min(100.0, (resDep * 0.3) + (psi * 4.0) + (entropy / 20.0));
    }

    private void syncClimateToBuffer() {
        if (worldBuffer == null || cells == null) return;
        float[] temps = worldBuffer.getTemperature();
        for (int i = 0; i < cells.size() && i < worldBuffer.getCapacity(); i++) {
            temps[i] = cells.get(i).getTemperature().floatValue();
        }
    }

    public org.ether.society.core.dod.WorldBuffer getWorldBuffer() {
        return worldBuffer;
    }

    public org.ether.society.core.dod.AgentBuffer getAgentBuffer() {
        return agentBuffer;
    }
}
