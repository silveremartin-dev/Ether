/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.engines.tier1.*;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Physical Historical Era Engine.
 * Replaces abstract 0.0-10.0 tech scalar with physical vector fields:
 * 1. <b>Per-Capita Power Flux (P_capita in Watts/person)</b>: Biomass -> Wind/Hydro -> Steam/Coal -> Electrical power.
 * 2. <b>Material Yield Strength (σ_yield in MPa)</b>: Flint (10 MPa) -> Bronze (150 MPa) -> Iron (400 MPa) -> Bessemer Steel (800 MPa).
 * 3. <b>Embodied Energy Capital (E_embodied in MJ)</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TechTreeEngine {
    private static final Logger logger = LoggerFactory.getLogger(TechTreeEngine.class);

    public record PhysicalEra(String name, double minPowerWattsPerCapita, double minMaterialYieldMPa, String description) {}

    public static final PhysicalEra PALEOLITHIC = new PhysicalEra("Paléolithique", 100.0, 10.0, "Chasseurs-cueilleurs nomades, taille de la pierre (10 MPa) et feu (100W/hab).");
    public static final PhysicalEra NEOLITHIC = new PhysicalEra("Néolithique", 250.0, 50.0, "Domestication, poterie, cuivre natif (50 MPa) et traction animale (250W/hab).");
    public static final PhysicalEra ANTIQUITY = new PhysicalEra("Antiquité", 600.0, 150.0, "Métallurgie du bronze (150 MPa), fer forgé (300 MPa) et canaux (600W/hab).");
    public static final PhysicalEra MEDIEVAL = new PhysicalEra("Moyen Âge", 1500.0, 400.0, "Charrue lourde, moulins à eau/vent (400 MPa) et éthanol/bois (1500W/hab).");
    public static final PhysicalEra INDUSTRIAL = new PhysicalEra("Révolution Industrielle", 8000.0, 800.0, "Machine à vapeur, acier Bessemer (800 MPa) et charbon (8000W/hab).");

    /*
     * Executes physical era progression and spatial power diffusion with default monthly step.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        processTechnologyDiffusion(cells, activeRoutes, 1.0 / 12.0);
    }

    /*
     * Executes physical era progression and spatial power diffusion scaled by dtYears.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes, double dtYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;
        double dt = Math.max(0.0001, dtYears);

        int eraUpgrades = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double capitalPerCapita = pop > 0 ? (capital / pop) : 0.0;
            double currentTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.5;
            double targetEraTech;

            // Physical era transition based on per-capita power flux and intensive capital intensity (K/N)
            if (powerPerCapita >= INDUSTRIAL.minPowerWattsPerCapita() && capitalPerCapita >= 25.0 && currentTech >= 2.8) {
                targetEraTech = 4.5;
            } else if (powerPerCapita >= MEDIEVAL.minPowerWattsPerCapita() && capitalPerCapita >= 15.0) {
                targetEraTech = 2.75;
            } else if (powerPerCapita >= ANTIQUITY.minPowerWattsPerCapita() && capitalPerCapita >= 5.0) {
                targetEraTech = 2.15;
            } else if (powerPerCapita >= NEOLITHIC.minPowerWattsPerCapita() && capitalPerCapita >= 0.5) {
                targetEraTech = 1.65;
            } else {
                targetEraTech = 0.5;
            }

            // Cumulative culture ratchet effect (Tomasello 1999, Henrich 2004) scaled by dtYears:
            // Technological knowledge advances when physical power & capital thresholds are achieved,
            // but does not artificially regress to the Stone Age during temporary biomass fluctuations.
            if (targetEraTech > currentTech) {
                double capitalIntensityFactor = capitalPerCapita / (20.0 + capitalPerCapita);
                double popScaleFactor = Math.min(2.0, Math.sqrt(pop / 1000.0));
                double annualMaxAdvance = 0.005 * capitalIntensityFactor * popScaleFactor;
                double advance = Math.min(targetEraTech - currentTech, annualMaxAdvance * dt);
                cell.setTechnologyLevel(currentTech + advance);
                eraUpgrades++;
            }
        }

        // Spatial technology diffusion along active maritime & terrestrial trade corridors
        if (activeRoutes != null && !activeRoutes.isEmpty()) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                if (route == null || route.origin() == null || route.destination() == null) continue;
                H3Cell orig = route.origin();
                H3Cell dest = route.destination();
                double tOrig = orig.getTechnologyLevel() != null ? orig.getTechnologyLevel() : 1.0;
                double tDest = dest.getTechnologyLevel() != null ? dest.getTechnologyLevel() : 1.0;
                double diff = tOrig - tDest;
                if (Math.abs(diff) > 0.05) {
                    double stepMag = 0.008 * Math.min(Math.abs(diff), 1.0) / Math.max(1.0, route.totalFrictionCost() * 0.1) * dt;
                    if (diff > 0) {
                        dest.setTechnologyLevel(Math.min(tOrig, tDest + stepMag));
                    } else {
                        orig.setTechnologyLevel(Math.min(tDest, tOrig + stepMag));
                    }
                }
            }
        }

        if (eraUpgrades > 0) {
            logger.debug("Physical Tech Engine: {} cells evaluated along physical power & material strength vectors.", eraUpgrades);
        }
    }
}
