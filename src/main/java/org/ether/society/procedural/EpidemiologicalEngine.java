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
 * Epidemiological SIR / SEIR Simulation Engine.
 * Models:
 * 1. Pathogen transmission across H3 cells and trade routes.
 * 2. Vulnerability based on detailed 7-segment age cohort distribution.
 * 3. Recovery and immunity development.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.5.0
 */
public class EpidemiologicalEngine {
    private static final Logger logger = LoggerFactory.getLogger(EpidemiologicalEngine.class);

    public record Pathogen(String name, double transmissionRate, double recoveryRate, double baseFatalityRate) {}

    public static final Pathogen PLAGUE = new Pathogen("Black Death", 0.35, 0.10, 0.30);
    public static final Pathogen INFLUENZA = new Pathogen("Pandemic Influenza", 0.25, 0.20, 0.05);

    /**
     * Executes one epidemiological tick across cells and trade networks.
     */
    public static void processEpidemicOutbreaks(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        int totalInfected = 0;
        int totalDeaths = 0;

        for (H3Cell cell : cells) {
            int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
            if (infected <= 0) continue;

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) {
                cell.setEpidemicInfected(0);
                continue;
            }

            // 1. Transmission within cell
            Pathogen pathogen = PLAGUE;
            double beta = pathogen.transmissionRate();
            double gamma = pathogen.recoveryRate();
            double mu = pathogen.baseFatalityRate();

            int newInfections = (int) Math.round(beta * infected * (1.0 - (double) cell.getEpidemicRecovered() / Math.max(1, pop)));
            int recovered = (int) Math.round(gamma * infected);
            int deaths = (int) Math.round(mu * infected);

            // 2. Age-Cohort Specific Mortality Impact
            // High mortality for Infants (0-4) and Elderly (80+)
            int infantDeaths = (int) (deaths * 0.40);
            int seniorDeaths = (int) (deaths * 0.40);
            int adultDeaths = deaths - (infantDeaths + seniorDeaths);

            cell.setPop0to4(Math.max(0, cell.getPop0to4() - infantDeaths));
            cell.setPop80Plus(Math.max(0, cell.getPop80Plus() - seniorDeaths));
            cell.setPop25to49(Math.max(0, cell.getPop25to49() - adultDeaths));
            cell.setPopulation(Math.max(0, pop - deaths));

            cell.setEpidemicInfected(Math.max(0, infected + newInfections - recovered - deaths));
            cell.setEpidemicRecovered(cell.getEpidemicRecovered() + recovered);

            totalInfected += cell.getEpidemicInfected();
            totalDeaths += deaths;
        }

        // 3. Propagation along Trade Corridors
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                for (int i = 0; i < route.pathCells().size() - 1; i++) {
                    H3Cell curr = route.pathCells().get(i);
                    H3Cell next = route.pathCells().get(i + 1);

                    if (curr.getEpidemicInfected() > 50 && next.getEpidemicInfected() == 0) {
                        next.setEpidemicInfected(10);
                        next.setActivePathogenName("Black Death");
                    }
                }
            }
        }

        if (totalInfected > 0 || totalDeaths > 0) {
            logger.info("Epidemiological cycle: {} active infected, {} casualties.", totalInfected, totalDeaths);
        }
    }
}
