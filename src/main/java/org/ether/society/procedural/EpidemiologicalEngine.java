/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Climate-Aware Epidemiological SIR / SEIR Simulation Engine.
 * Dynamic pathogen transmission and vector-borne disease models:
 * <ul>
 *   <li><b>Malaria / Maladies Vectorielles</b>: Vector breeding modulated by temperature ($20^\circ\text{C}$ to $35^\circ\text{C}$) and rainfall.</li>
 *   <li><b>Choléra / Maladies Hydriques</b>: Contamination outbreaks triggered by flooding disasters or severe water scarcity.</li>
 *   <li><b>Peste / Grippe (Stress Froid)</b>: Increased susceptibility during volcanic winters and severe cold stress.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.6.0
 */
public class EpidemiologicalEngine {
    private static final Logger logger = LoggerFactory.getLogger(EpidemiologicalEngine.class);

    public record Pathogen(String name, double transmissionRate, double recoveryRate, double baseFatalityRate, String type) {}

    public static final Pathogen PLAGUE = new Pathogen("Black Death", 0.35, 0.10, 0.30, "RESPIRATORY");
    public static final Pathogen MALARIA = new Pathogen("Tropical Malaria", 0.30, 0.15, 0.12, "VECTOR");
    public static final Pathogen CHOLERA = new Pathogen("Waterborne Cholera", 0.40, 0.20, 0.18, "WATERBORNE");

    /**
     * Executes one epidemiological tick across cells and trade networks.
     */
    public static void processEpidemicOutbreaks(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        int totalInfected = 0;
        int totalDeaths = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) {
                cell.setEpidemicInfected(0);
                continue;
            }

            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double water = cell.getWaterResource() != null ? cell.getWaterResource() : 500.0;
            Biome biome = cell.getBiome();

            // 1. Spontaneous Climate Vector Outbreak Triggering
            int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;

            if (infected == 0) {
                // Vector-borne outbreak (Malaria in warm humid jungles/plains)
                if ((biome == Biome.JUNGLE || biome == Biome.PLAINS) && temp >= 22.0 && temp <= 35.0 && rain > 600.0) {
                    if (Math.random() < 0.02) {
                        cell.setEpidemicInfected(Math.max(5, (int)(pop * 0.01)));
                        cell.setActivePathogenName(MALARIA.name());
                        infected = cell.getEpidemicInfected();
                    }
                }
                // Waterborne outbreak (Cholera in contaminated water or flooded coastal areas)
                else if (water < 100.0 || (rain > 900.0 && biome == Biome.BEACH)) {
                    if (Math.random() < 0.03) {
                        cell.setEpidemicInfected(Math.max(5, (int)(pop * 0.015)));
                        cell.setActivePathogenName(CHOLERA.name());
                        infected = cell.getEpidemicInfected();
                    }
                }
            }

            if (infected <= 0) continue;

            // Select active pathogen profile
            String pathogenName = cell.getActivePathogenName();
            Pathogen pathogen = PLAGUE;
            if (MALARIA.name().equals(pathogenName)) pathogen = MALARIA;
            else if (CHOLERA.name().equals(pathogenName)) pathogen = CHOLERA;

            // 2. Climate Modulation of Transmission Rate Beta
            double beta = pathogen.transmissionRate();
            if ("VECTOR".equals(pathogen.type())) {
                double tempVectorFactor = Math.exp(-Math.pow(temp - 27.0, 2) / 50.0);
                beta *= (0.5 + tempVectorFactor);
            } else if ("WATERBORNE".equals(pathogen.type())) {
                if (water < 150.0 || rain > 850.0) beta *= 1.4; // Contamination boost
            } else if ("RESPIRATORY".equals(pathogen.type())) {
                if (temp < 5.0) beta *= 1.3; // Cold stress crowding boost
            }

            double gamma = pathogen.recoveryRate();
            double mu = pathogen.baseFatalityRate();

            int newInfections = (int) Math.round(beta * infected * (1.0 - (double) cell.getEpidemicRecovered() / Math.max(1, pop)));
            int recovered = (int) Math.round(gamma * infected);
            int deaths = (int) Math.round(mu * infected);

            // 3. Age-Cohort Specific Mortality Impact
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

        // 4. Propagation along Trade Corridors
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                for (int i = 0; i < route.pathCells().size() - 1; i++) {
                    H3Cell curr = route.pathCells().get(i);
                    H3Cell next = route.pathCells().get(i + 1);

                    if (curr.getEpidemicInfected() > 50 && next.getEpidemicInfected() == 0) {
                        next.setEpidemicInfected(10);
                        next.setActivePathogenName(curr.getActivePathogenName() != null ? curr.getActivePathogenName() : "Black Death");
                    }
                }
            }
        }

        if (totalInfected > 0 || totalDeaths > 0) {
            logger.info("Epidemiological cycle: {} active infected, {} casualties.", totalInfected, totalDeaths);
        }
    }
}
