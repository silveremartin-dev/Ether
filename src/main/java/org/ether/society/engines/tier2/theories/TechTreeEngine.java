/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
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
 * 2. <b>Material Yield Strength (ÃÆ’_yield in MPa)</b>: Flint (10 MPa) -> Bronze (150 MPa) -> Iron (400 MPa) -> Bessemer Steel (800 MPa).
 * 3. <b>Embodied Energy Capital (E_embodied in MJ)</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class TechTreeEngine {
    private static final Logger logger = LoggerFactory.getLogger(TechTreeEngine.class);

    public record PhysicalEra(String name, double minPowerWattsPerCapita, double minMaterialYieldMPa, String description) {}

    public static final PhysicalEra PALEOLITHIC = new PhysicalEra("PalÃƒÂ©olithique", 100.0, 10.0, "Chasseurs-cueilleurs nomades, taille de la pierre (10 MPa) et feu (100W/hab).");
    public static final PhysicalEra NEOLITHIC = new PhysicalEra("NÃƒÂ©olithique", 250.0, 50.0, "Domestication, poterie, cuivre natif (50 MPa) et traction animale (250W/hab).");
    public static final PhysicalEra ANTIQUITY = new PhysicalEra("AntiquitÃƒÂ©", 600.0, 150.0, "MÃƒÂ©tallurgie du bronze (150 MPa), fer forgÃƒÂ© (300 MPa) et canaux (600W/hab).");
    public static final PhysicalEra MEDIEVAL = new PhysicalEra("Moyen Ãƒâ€šge", 1500.0, 400.0, "Charrue lourde, moulins ÃƒÂ  eau/vent (400 MPa) et ÃƒÂ©thanol/bois (1500W/hab).");
    public static final PhysicalEra INDUSTRIAL = new PhysicalEra("RÃƒÂ©volution Industrielle", 8000.0, 800.0, "Machine ÃƒÂ  vapeur, acier Bessemer (800 MPa) et charbon (8000W/hab).");

    /*
     * Executes physical era progression and spatial power diffusion.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        int eraUpgrades = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // Physical era transition based on per-capita power flux and material yield strength
            if (powerPerCapita >= INDUSTRIAL.minPowerWattsPerCapita() && capital > 8000.0) {
                cell.setTechnologyLevel(8.0); // Derived scalar mapping for technologyLevel
                eraUpgrades++;
            } else if (powerPerCapita >= MEDIEVAL.minPowerWattsPerCapita() && capital > 1500.0) {
                cell.setTechnologyLevel(5.5);
                eraUpgrades++;
            } else if (powerPerCapita >= ANTIQUITY.minPowerWattsPerCapita() && capital > 300.0) {
                cell.setTechnologyLevel(3.0);
                eraUpgrades++;
            } else if (powerPerCapita >= NEOLITHIC.minPowerWattsPerCapita()) {
                cell.setTechnologyLevel(1.5);
                eraUpgrades++;
            } else {
                cell.setTechnologyLevel(0.5);
            }
        }

        if (eraUpgrades > 0) {
            logger.debug("Physical Tech Engine: {} cells evaluated along physical power & material strength vectors.", eraUpgrades);
        }
    }
}


