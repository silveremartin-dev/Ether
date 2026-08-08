/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages historical and random events in the simulation.
 * 
 * Event Types:
 * - Historical: Pre-defined milestone events
 * - Random: Probability-based disasters and discoveries
 * - Triggered: Based on simulation state (famine, plague, etc.)
 */
public class EventSystem {
    private final List<HistoricalEvent> historicalEvents = new ArrayList<>();
    private final List<String> eventQueue = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    // Cooldowns to prevent event spam
    private int lastPlagueYear = Integer.MIN_VALUE;
    private int lastFamineYear = Integer.MIN_VALUE;
    private int lastDroughtYear = Integer.MIN_VALUE;
    private int lastVolcanoYear = Integer.MIN_VALUE;

    public EventSystem() {
        initializeHistoricalEvents();
    }

    private void initializeHistoricalEvents() {
        // Prehistoric
        historicalEvents.add(new HistoricalEvent(-100000, "Out of Africa",
                "Humans begin migrating out of Africa to new continents."));
        historicalEvents.add(new HistoricalEvent(-40000, "Cave Art",
                "First known cave paintings appear, showing symbolic thought."));
        historicalEvents.add(new HistoricalEvent(-12000, "Last Ice Age Ends",
                "Warming climate opens new lands for settlement."));

        // Neolithic
        historicalEvents.add(new HistoricalEvent(-10000, "Agricultural Revolution",
                "Farming begins in the Fertile Crescent."));
        historicalEvents.add(new HistoricalEvent(-8000, "First Villages",
                "Permanent settlements like Jericho are established."));
        historicalEvents.add(new HistoricalEvent(-6000, "Pottery Invented",
                "Ceramic vessels allow food storage and trade."));

        // Bronze Age
        historicalEvents.add(new HistoricalEvent(-3500, "Writing Invented",
                "Cuneiform and hieroglyphics enable record-keeping."));
        historicalEvents.add(new HistoricalEvent(-3000, "Bronze Age Begins",
                "Metal tools and weapons transform society."));
        historicalEvents.add(new HistoricalEvent(-2560, "Great Pyramid",
                "Massive construction project demonstrates organized labor."));

        // Iron Age
        historicalEvents.add(new HistoricalEvent(-1200, "Iron Age Begins",
                "Iron working spreads, making tools accessible."));
        historicalEvents.add(new HistoricalEvent(-800, "Greek City-States Rise",
                "Democracy and philosophy emerge."));

        // Classical
        historicalEvents.add(new HistoricalEvent(-500, "Classical Era",
                "Golden age of Greece and early Rome."));
        historicalEvents.add(new HistoricalEvent(0, "New Era",
                "A new calendar era begins."));
        historicalEvents.add(new HistoricalEvent(476, "Fall of Rome",
                "Western Roman Empire collapses, beginning the medieval period."));

        // Medieval
        historicalEvents.add(new HistoricalEvent(1000, "Medieval Warming",
                "Warmer climate allows population growth."));
        historicalEvents.add(new HistoricalEvent(1347, "Black Death Arrives",
                "Devastating plague reaches Europe."));

        // Modern
        historicalEvents.add(new HistoricalEvent(1492, "Age of Exploration",
                "Columbian exchange begins."));
        historicalEvents.add(new HistoricalEvent(1760, "Industrial Revolution",
                "Steam power transforms production."));
        historicalEvents.add(new HistoricalEvent(1945, "Nuclear Age",
                "First nuclear weapons used."));
    }

    private final java.util.Set<String> firedHistoricalEvents = new java.util.HashSet<>();

    public void reset() {
        firedHistoricalEvents.clear();
        eventQueue.clear();
    }

    /**
     * Check for events occurring at the current state.
     */
    public void checkEvents(int year, long totalPopulation, double totalFood) {
        // Historical events
        for (HistoricalEvent event : historicalEvents) {
            if (event.year() == year && !firedHistoricalEvents.contains(event.title())) {
                eventQueue.add(String.format("📜 HISTORICAL: %s - %s", event.title(), event.message()));
                firedHistoricalEvents.add(event.title());
            }
        }

        // Triggered events based on state
        checkFamine(year, totalPopulation, totalFood);
        checkPlague(year, totalPopulation);
        checkNaturalDisasters(year, totalPopulation);
        checkAchievements(year, totalPopulation);
    }

    /**
     * Check for events based on cell-level data.
     * Call this for more detailed event generation.
     */
    public void checkCellEvents(int year, List<H3Cell> cells) {
        // Count biome statistics
        long forestCells = cells.stream().filter(c -> c.getBiome() == Biome.FOREST || c.getBiome() == Biome.JUNGLE)
                .count();
        long desertCells = cells.stream().filter(c -> c.getBiome() == Biome.DESERT).count();
        long totalLandCells = cells.stream()
                .filter(c -> c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN).count();

        // Deforestation warning
        if (totalLandCells > 0) {
            double forestRatio = (double) forestCells / totalLandCells;
            if (forestRatio < 0.1 && random.nextDouble() < 0.01) {
                eventQueue.add("⚠️ ECOLOGICAL: Forests are nearly depleted! Only " +
                        String.format("%.1f%%", forestRatio * 100) + " remains.");
            }
        }

        // Desertification warning
        if (totalLandCells > 0) {
            double desertRatio = (double) desertCells / totalLandCells;
            if (desertRatio > 0.4 && random.nextDouble() < 0.01) {
                eventQueue.add("🏜️ ECOLOGICAL: Desertification is spreading! " +
                        String.format("%.1f%%", desertRatio * 100) + " of land is now desert.");
            }
        }

        // Regional famine detection
        long starvingCells = cells.stream()
                .filter(c -> c.getPopulation() > 10 && c.getFoodResource() < c.getPopulation())
                .count();

        if (starvingCells > cells.size() * 0.2 && random.nextDouble() < 0.05) {
            eventQueue.add("🍂 FAMINE: Regional food shortages affect " + starvingCells + " areas!");
        }
    }

    private void checkFamine(int year, long totalPopulation, double totalFood) {
        if (year - lastFamineYear < 10)
            return; // 10-year cooldown

        if (totalFood < totalPopulation * 0.8 && totalPopulation > 500) {
            double severity = 1.0 - (totalFood / (totalPopulation * 0.8));

            if (severity > 0.5 && random.nextDouble() < 0.1) {
                eventQueue.add("💀 FAMINE: Severe food shortage! Population is starving.");
                lastFamineYear = year;
            } else if (severity > 0.2 && random.nextDouble() < 0.05) {
                eventQueue.add("🍂 FOOD CRISIS: Harvests have failed, food is scarce.");
                lastFamineYear = year;
            }
        }
    }

    private void checkPlague(int year, long totalPopulation) {
        if (year - lastPlagueYear < 50)
            return; // 50-year cooldown

        // Higher population = higher plague risk
        double plagueRisk = Math.min(0.01, totalPopulation / 10_000_000.0);

        if (totalPopulation > 10000 && random.nextDouble() < plagueRisk) {
            if (random.nextDouble() < 0.3) {
                eventQueue.add("☠️ PANDEMIC: A devastating plague sweeps across the land!");
                lastPlagueYear = year;
            } else {
                eventQueue.add("🤒 EPIDEMIC: Disease outbreak in crowded areas.");
                lastPlagueYear = year;
            }
        }
    }

    private void checkNaturalDisasters(int year, long totalPopulation) {
        // Drought
        if (year - lastDroughtYear > 20 && random.nextDouble() < 0.005) {
            eventQueue.add("☀️ DROUGHT: Extended dry period threatens crops and water supplies.");
            lastDroughtYear = year;
        }

        // Volcanic eruption
        if (year - lastVolcanoYear > 100 && random.nextDouble() < 0.001) {
            eventQueue.add("🌋 VOLCANO: Major eruption! Ash clouds affect climate.");
            lastVolcanoYear = year;
        }

        // Earthquake (random, no cooldown needed)
        if (random.nextDouble() < 0.002) {
            eventQueue.add("🌍 EARTHQUAKE: Tremors shake the region.");
        }

        // Flood (seasonal, more likely in monsoon regions)
        if ((year % 1 == 0) && random.nextDouble() < 0.003) { // Simplified
            eventQueue.add("🌊 FLOOD: Rivers overflow their banks.");
        }
    }

    private void checkAchievements(int year, long totalPopulation) {
        // Population milestones
        if (totalPopulation >= 1_000_000 && totalPopulation < 1_100_000) {
            eventQueue.add("🎉 MILESTONE: World population reaches 1 million!");
        } else if (totalPopulation >= 10_000_000 && totalPopulation < 10_500_000) {
            eventQueue.add("🎉 MILESTONE: World population reaches 10 million!");
        } else if (totalPopulation >= 100_000_000 && totalPopulation < 105_000_000) {
            eventQueue.add("🎉 MILESTONE: World population reaches 100 million!");
        } else if (totalPopulation >= 1_000_000_000 && totalPopulation < 1_050_000_000) {
            eventQueue.add("🎉 MILESTONE: World population reaches 1 billion!");
        }
    }

    /**
     * Trigger a specific event (for testing or scenario control).
     */
    public void triggerEvent(String message) {
        eventQueue.add(message);
    }

    /**
     * Get and clear recent events.
     */
    public List<String> flushEvents() {
        List<String> events = new ArrayList<>(eventQueue);
        eventQueue.clear();
        return events;
    }

    /**
     * Get events without clearing (for UI display).
     */
    public List<String> peekEvents() {
        return new ArrayList<>(eventQueue);
    }
}
