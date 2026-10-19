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

    /**
     * Executes physical era progression and spatial power diffusion with default monthly step.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        processTechnologyDiffusion(cells, activeRoutes, 1.0 / 12.0);
    }

    /**
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
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapitalOr(0.0);
            double capitalPerCapita = pop > 0 ? (capital / pop) : 0.0;
            double currentTech = cell.getTechnologyLevelOr(0.5);

            // 1. Continuous Physical Power & Material Envelope Ceiling (replaces circular step-function deadlock)
            double targetEraTech;
            if (powerPerCapita >= INDUSTRIAL.minPowerWattsPerCapita() && capitalPerCapita >= 25.0) {
                targetEraTech = 5.0;
            } else if (powerPerCapita >= MEDIEVAL.minPowerWattsPerCapita() && capitalPerCapita >= 12.0) {
                targetEraTech = 3.4;
            } else if (powerPerCapita >= ANTIQUITY.minPowerWattsPerCapita() && capitalPerCapita >= 4.0) {
                targetEraTech = 2.6;
            } else if (powerPerCapita >= NEOLITHIC.minPowerWattsPerCapita() && capitalPerCapita >= 0.5) {
                targetEraTech = 1.9;
            } else {
                targetEraTech = Math.max(0.5, currentTech);
            }

            // 2. Cumulative Culture Ratchet (Tomasello 1999, Henrich 2004):
            // Driven by demographic network size sqrt(N) and intensive physical capital (K/N)
            double advance = 0.0;
            if (targetEraTech > currentTech) {
                double capitalIntensityFactor = capitalPerCapita / (20.0 + capitalPerCapita);
                double popScaleFactor = Math.min(2.0, Math.sqrt(pop / 1000.0));
                double annualMaxAdvance = 0.005 * capitalIntensityFactor * popScaleFactor;
                advance += Math.min(targetEraTech - currentTech, annualMaxAdvance * dt);
            }

            // 3. Universal Bottleneck-Induced Directed Technical Change (Boserup 1965, Allen 2009, Acemoglu 2002):
            // Whenever a cell faces scarcity on its currently active resource tier while holding latent
            // higher-enthalpy resources (e.g., clay/arable land under foraging stress, iron ore under tin scarcity,
            // coal under timber scarcity, oil/uranium under industrial energy stress), substitution accelerates dT/dt.
            double inducedAdvance = calculateBottleneckInducedSubstitution(cell, dt);
            if (advance + inducedAdvance > 0.0) {
                cell.setTechnologyLevel(currentTech + advance + inducedAdvance);
                eraUpgrades++;
            }
        }

        // Spatial technology diffusion along active maritime & terrestrial trade corridors
        if (activeRoutes != null && !activeRoutes.isEmpty()) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                if (route == null || route.origin() == null || route.destination() == null) continue;
                H3Cell orig = route.origin();
                H3Cell dest = route.destination();
                double tOrig = orig.getTechnologyLevelOr(1.0);
                double tDest = dest.getTechnologyLevelOr(1.0);
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

    /**
     * Evaluates universal Boserup-Allen-Acemoglu Directed Technical Change driven by physical resource
     * bottlenecks ($\Sigma_{\text{stress}}$) coupled with latent higher-enthalpy geological/ecological stocks ($\Psi_{\text{latent}}$).
     *
     * <p>This single invariant law operates identically across all historical epochs and locations without
     * era-specific or geographic hardcoding:</p>
     * <ul>
     *   <li><b>Biomass/Foraging Scarcity + Soil/Clay</b>: Accelerates transition toward ceramics &amp; agriculture.</li>
     *   <li><b>Alloying Tin/Trade Scarcity + Iron Oxide</b>: Accelerates transition toward high-temperature bloomery smelting.</li>
     *   <li><b>Forest Timber Scarcity + Carboniferous Coal</b>: Accelerates transition toward mineral coke &amp; steam dewatering.</li>
     *   <li><b>Coal/Thermal Scarcity + Hydrocarbons/Fissile Uranium</b>: Accelerates transition toward internal combustion &amp; nuclear grids.</li>
     * </ul>
     *
     * @param cell    the H3 cell under evaluation
     * @param dtYears time step in years
     * @return endogenous technological increment $\Delta T \ge 0$
     */
    public static double calculateBottleneckInducedSubstitution(H3Cell cell, double dtYears) {
        if (cell == null) return 0.0;
        int pop = cell.getPopulationOr(0);
        if (pop <= 0) return 0.0;

        double tech = cell.getTechnologyLevelOr(0.5);
        double wood = Math.max(
                cell.getBiomassNaturalOr(0.0),
                cell.getWoodResourceOr(0.0));
        double metal = cell.getResourceMetalOr(0.0);
        double coal = cell.getResourceCoalOr(0.0);
        double oilGas = (cell.getResourceOilOr(0.0))
                + (cell.getResourceGasOr(0.0));
        double uranium = cell.getResourceUraniumOr(0.0);

        // 1. Primary organic fuel & biomass scarcity stress relative to population and pyrometallurgical demand
        double organicDemand = pop * 0.15 * Math.max(0.5, tech);
        double organicScarcityStress = Math.clamp(1.0 - (wood / Math.max(1.0, organicDemand)), 0.0, 1.0);

        // 2. Latent higher-order resource potential available to resolve the bottleneck at current furnace/energy tier
        double latentSubstitutePotential = 0.0;
        if (tech < 1.8 && metal > 5.0) {
            // Lithic/Biomass bottleneck with accessible metallic ores
            latentSubstitutePotential = Math.min(1.0, Math.log1p(metal) / 5.0);
        } else if (tech >= 1.8 && tech < 2.5 && metal > 10.0) {
            // Alloying/Tin bottleneck with abundant crustal iron oxide
            double reeTin = cell.getResourceRareEarthsOr(0.0);
            double tinScarcity = Math.clamp(1.0 - (reeTin / 2.0), 0.0, 1.0);
            organicScarcityStress = Math.max(organicScarcityStress, tinScarcity);
            latentSubstitutePotential = Math.min(1.0, Math.log1p(metal) / 5.0);
        } else if (tech >= 2.5 && tech < 3.4 && coal > 5.0) {
            // Timber/Charcoal bottleneck with subsurface mineral coal seams
            latentSubstitutePotential = Math.min(1.0, Math.log1p(coal) / 5.0);
        } else if (tech >= 3.4 && (oilGas > 5.0 || uranium > 1.0)) {
            // Coal/Mechanical bottleneck with hydrocarbons or fissile uranium
            double coalScarcity = Math.clamp(1.0 - (coal / Math.max(10.0, pop * 0.05)), 0.0, 1.0);
            organicScarcityStress = Math.max(organicScarcityStress, coalScarcity);
            latentSubstitutePotential = Math.min(1.0, Math.log1p(oilGas + uranium * 10.0) / 5.0);
        }

        return 0.12 * organicScarcityStress * latentSubstitutePotential * Math.max(0.0001, dtYears);
    }
}
