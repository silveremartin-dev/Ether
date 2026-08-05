package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zoonotic Emergence & Epidemiological Transmission Engine.
 * Models per-cell SIR dynamics, zoonotic spillover risk, and inter-cell trade propagation.
 */
public class PandemicEngine {
    private static final Logger logger = LoggerFactory.getLogger(PandemicEngine.class);

    public record EpidemiologicalState(
            double susceptible,
            double infected,
            double recovered,
            double mortalityRate,
            double r0Basic
    ) {}

    private final Map<Long, EpidemiologicalState> cellEpidemicMap = new HashMap<>();

    /**
     * Evaluates spillover risk and advances pandemic turn state across all H3 cells.
     */
    public void stepPandemicTurn(List<H3Cell> cells, double globalTransmissionRate) {
        if (cells == null || cells.isEmpty()) return;

        // 1. Zoonotic Spillover Check
        for (H3Cell c : cells) {
            if (c.getPopulation() == null || c.getPopulation() <= 0) continue;

            long idx = c.getH3Index();
            EpidemiologicalState state = cellEpidemicMap.get(idx);

            if (state == null) {
                double popDensity = Math.min(10000.0, c.getPopulation());
                double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;

                // High density + tropical/warm climate increases spillover risk
                double spilloverRisk = (popDensity / 10000.0) * 0.005 * (temp > 20.0 ? 1.5 : 0.8);

                if (Math.random() < spilloverRisk) {
                    logger.warn("🦠 Zoonotic Spillover Event at cell {} (Lat: {}, Lng: {})", idx, c.getLatitude(), c.getLongitude());
                    state = new EpidemiologicalState(0.98, 0.02, 0.0, 0.05, 2.5);
                    cellEpidemicMap.put(idx, state);
                }
            }
        }

        // 2. SIR Dynamic Progression
        for (H3Cell c : cells) {
            long idx = c.getH3Index();
            EpidemiologicalState state = cellEpidemicMap.get(idx);
            if (state == null || state.infected() <= 0.0001) continue;

            double S = state.susceptible();
            double I = state.infected();
            double R = state.recovered();
            double beta = 0.35 * globalTransmissionRate; // Transmission speed
            double gamma = 0.10;                         // Recovery rate
            double mu = state.mortalityRate();            // Disease mortality

            double newInfected = beta * S * I;
            double newRecovered = gamma * I;
            double newDeaths = mu * I * 0.05;

            double nextS = Math.max(0.0, S - newInfected);
            double nextI = Math.max(0.0, I + newInfected - newRecovered - newDeaths);
            double nextR = Math.min(1.0, R + newRecovered);

            cellEpidemicMap.put(idx, new EpidemiologicalState(nextS, nextI, nextR, mu, state.r0Basic()));

            // Reduce cell population by deaths
            if (c.getPopulation() != null && c.getPopulation() > 0) {
                int deathCount = Math.max(0, (int) (c.getPopulation() * newDeaths));
                c.setPopulation(Math.max(0, c.getPopulation() - deathCount));
            }
        }
    }

    public EpidemiologicalState getCellEpidemicState(long h3Index) {
        return cellEpidemicMap.get(h3Index);
    }

    public void injectOutbreak(long h3Index, double initialInfectedRatio, double r0) {
        cellEpidemicMap.put(h3Index, new EpidemiologicalState(
                1.0 - initialInfectedRatio, initialInfectedRatio, 0.0, 0.04, r0
        ));
    }
}
