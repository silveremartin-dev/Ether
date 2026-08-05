package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pressurized Habitat & Extraterrestrial Life Support Dome Engine.
 * Enables human survival and population growth on vacuum, toxic, or hostile exoplanetary environments.
 */
public class LifeSupportDomeEngine {
    private static final Logger logger = LoggerFactory.getLogger(LifeSupportDomeEngine.class);

    public record DomeInfrastructure(
            long h3Index,
            int domeTier,               // Tier 1: Survey Outpost, Tier 2: Sealed Biosphere, Tier 3: Megastructure City Dome
            long maxPopulationCapacity,
            double oxygenPurityRatio,
            double energyConsumptionKw,
            boolean isOperational
    ) {}

    private final Map<Long, DomeInfrastructure> domeRegistry = new HashMap<>();

    /**
     * Evaluates whether a planet requires pressurized life support domes for human survival.
     */
    public boolean requiresLifeSupportDomes(PlanetPreset preset) {
        if (preset == null) return false;
        return preset.atmospherePressureAtm() < 0.01 ||
               preset.oxygenPercentage() < 10.0 ||
               preset.averageTempC() < -40.0 ||
               preset.averageTempC() > 60.0;
    }

    /**
     * Attempts to construct or upgrade a pressurized habitat dome on an H3 cell.
     */
    public boolean constructDome(H3Cell cell, double techLevel, double availableMetalTonnes) {
        if (cell == null || techLevel < 3.0 || availableMetalTonnes < 500.0) {
            return false;
        }

        long idx = cell.getH3Index();
        DomeInfrastructure existing = domeRegistry.get(idx);
        int newTier = (existing == null) ? 1 : Math.min(3, existing.domeTier() + 1);

        long cap = switch (newTier) {
            case 1 -> 50_000L;
            case 2 -> 500_000L;
            case 3 -> 5_000_000L;
            default -> 10_000L;
        };

        DomeInfrastructure dome = new DomeInfrastructure(idx, newTier, cap, 0.99, 1200.0 * newTier, true);
        domeRegistry.put(idx, dome);

        logger.info("🪐 Constructed Pressurized Life Support Dome (Tier {}) at H3 cell {} [Capacity: {} hab]",
                newTier, idx, cap);

        return true;
    }

    /**
     * Applies life support protection to populations residing on hostile planet cells.
     */
    public void enforceLifeSupportTurn(List<H3Cell> cells, PlanetPreset preset) {
        if (cells == null || !requiresLifeSupportDomes(preset)) return;

        for (H3Cell c : cells) {
            if (c.getPopulation() == null || c.getPopulation() <= 0) continue;

            long idx = c.getH3Index();
            DomeInfrastructure dome = domeRegistry.get(idx);

            if (dome == null || !dome.isOperational()) {
                // Exposure to vacuum/hypoxia causes severe population collapse without dome
                int remaining = (int) (c.getPopulation() * 0.10);
                logger.warn("💀 Severe atmospheric exposure mortality at cell {} without Life Support Dome!", idx);
                c.setPopulation(remaining);
            } else {
                // Population is capped by dome life support capacity
                if (c.getPopulation() > dome.maxPopulationCapacity()) {
                    c.setPopulation((int) dome.maxPopulationCapacity());
                }
            }
        }
    }

    public DomeInfrastructure getDome(long h3Index) {
        return domeRegistry.get(h3Index);
    }
}
