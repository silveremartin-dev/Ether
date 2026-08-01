/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Historical Era & Technology Tree Engine (Prehistoric to Industrial Era).
 * Models 5 distinct socio-technological eras:
 * <ol>
 *   <li><b>Paléolithique / Préhistoire (Tech 0.0 - 1.0)</b>: Nomadic hunter-gatherers, fire usage (energyFire), stone tools.</li>
 *   <li><b>Révolution Néolithique (Tech 1.0 - 2.5)</b>: Plant domestication, livestock breeding (biomassLivestock), sedentary villages, pottery.</li>
 *   <li><b>Antiquité (Tech 2.5 - 5.0)</b>: Canal irrigation, bronze/iron metallurgy (resourceMetal), city-states, slave labor (energySlaves), writing.</li>
 *   <li><b>Moyen Âge (Tech 5.0 - 7.5)</b>: Heavy plow, windmills (energyWind), watermills, feudal guilds, monastic agriculture, stone fortifications.</li>
 *   <li><b>Temps Modernes & Révolution Industrielle (Tech 7.5 - 10.0)</b>: Mechanization, steam engines, deep mining, global trade fleets.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.8.0
 */
public class TechTreeEngine {
    private static final Logger logger = LoggerFactory.getLogger(TechTreeEngine.class);

    public record HistoricalEra(String name, double minTechLevel, double unlockCapitalCost, String description) {}

    public static final HistoricalEra PALEOLITHIC = new HistoricalEra("Paléolithique", 0.0, 0.0, "Chasseurs-cueilleurs nomades, taille du flint et maîtrise du feu.");
    public static final HistoricalEra NEOLITHIC = new HistoricalEra("Néolithique", 1.0, 50.0, "Domestication des plantes/bétail, poterie et premiers villages sédentaires.");
    public static final HistoricalEra ANTIQUITY = new HistoricalEra("Antiquité", 2.5, 300.0, "Irrigation, métallurgie du bronze/fer, cités-états et travail servile.");
    public static final HistoricalEra MEDIEVAL = new HistoricalEra("Moyen Âge", 5.0, 1500.0, "Charrue lourde, moulins à eau/vent, féodalité et guildes marchandes.");
    public static final HistoricalEra INDUSTRIAL = new HistoricalEra("Révolution Industrielle", 7.5, 8000.0, "Machine à vapeur, mécanisation, extraction profonde et énergie fossile.");

    /**
     * Executes one technology innovation, era progression and diffusion step.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        int eraUpgrades = 0;

        for (H3Cell cell : cells) {
            double currentTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            if (pop <= 0) continue;

            // 1. Prehistoric -> Neolithic Transition (Domestication trigger)
            if (currentTech < NEOLITHIC.minTechLevel() && cell.getBiomassNatural() > 300.0) {
                cell.setTechnologyLevel(NEOLITHIC.minTechLevel());
                cell.setBiomassLivestock(Math.max(50.0, cell.getBiomassLivestock()));
                eraUpgrades++;
            }
            // 2. Neolithic -> Antiquity Transition (Metal + Irrigation)
            else if (currentTech < ANTIQUITY.minTechLevel() && capital > ANTIQUITY.unlockCapitalCost() && pop > 150) {
                cell.setTechnologyLevel(ANTIQUITY.minTechLevel());
                cell.setEnergySlaves(Math.max(100.0, pop * 0.4));
                eraUpgrades++;
            }
            // 3. Antiquity -> Medieval Transition (Wind/Water energy + Heavy plow)
            else if (currentTech < MEDIEVAL.minTechLevel() && capital > MEDIEVAL.unlockCapitalCost() && pop > 400) {
                cell.setTechnologyLevel(MEDIEVAL.minTechLevel());
                cell.setEnergyWind(Math.max(150.0, cell.getEnergyWind() + 100.0));
                eraUpgrades++;
            }
            // 4. Medieval -> Industrial Transition (Steam + Machinery)
            else if (currentTech < INDUSTRIAL.minTechLevel() && capital > INDUSTRIAL.unlockCapitalCost() && pop > 1000) {
                cell.setTechnologyLevel(INDUSTRIAL.minTechLevel());
                eraUpgrades++;
                logger.info("Industrial Revolution unlocked at H3 Cell {}!", cell.getH3Index());
            } else {
                // Gradual organic tech growth fueled by literacy and capital
                cell.setTechnologyLevel(Math.min(10.0, currentTech + 0.005 + (capital / 100000.0)));
            }
        }

        // 2. Spatial Technology Diffusion along Trade Routes & Neighboring Cells
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                double maxTech = Math.max(route.origin().getTechnologyLevel(), route.destination().getTechnologyLevel());
                for (H3Cell pathCell : route.pathCells()) {
                    if (pathCell.getTechnologyLevel() < maxTech) {
                        double stepTech = pathCell.getTechnologyLevel() + 0.015;
                        pathCell.setTechnologyLevel(Math.min(maxTech, stepTech));
                    }
                }
            }
        }

        if (eraUpgrades > 0) {
            logger.info("TechTree Engine: {} cells progressed to new historical eras.", eraUpgrades);
        }
    }
}
