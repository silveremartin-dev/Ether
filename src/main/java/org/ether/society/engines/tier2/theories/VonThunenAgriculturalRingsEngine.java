/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <h1>Von Thünen Agricultural Rings &amp; Overland Transport Friction Engine</h1>
 * <p>
 * Models the geographic logistics of agricultural food transport and urban agglomeration limits.
 * Replaces synthetic urbanization rate sigmoids with authentic spatial transport decay
 * (Von Thünen 1826; Krugman 1991).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>Johann Heinrich von Thünen (1826)</b> <i>Der isolirte Staat in Beziehung auf Landwirthschaft und Nationalökonomie</i>, Perthes, Hamburg.</li>
 *   <li><b>Paul Krugman (1991)</b> <i>Increasing Returns and Economic Geography</i>, J. Political Econ. 99(3): 483–499.</li>
 *   <li><b>Paul Bairoch (1988)</b> <i>Cities and Economic Development: From the Dawn of History to the Present</i>, Univ. of Chicago Press.</li>
 * </ul>
 *
 * <h2>Transport Mechanics &amp; Urbanization Limits</h2>
 * <p>
 * In pre-industrial terrestrial regimes (pack animals, ox-drawn carts), hauling bulk grain consumes
 * food at a rate of $\sim 1\text{--}2\%$ of the cargo per $10\text{ km}$ (the draft animals must eat the grain they carry).
 * Consequently, at a distance of $r_{\text{critical}} \approx 100\text{--}150\text{ km}$, transport cost exceeds total grain calories,
 * strictly bounding the maximum sustainable population of inland cities to $N_{\text{max}} \le 50\,000\text{--}100\,000$ inhabitants
 * (Rome and Constantinople only broke this barrier via sea lanes and Nile grain fleets).
 * Paved macadam roads, canals, and steam railroads expand $r_{\text{critical}}$ exponentially, allowing modern mega-cities to emerge.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class VonThunenAgriculturalRingsEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(VonThunenAgriculturalRingsEngine.class);

    @Override
    public String getName() {
        return "VonThunenAgriculturalRingsEngine";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processAgriculturalRings(cells, deltaYears);
    }

    /**
     * Evaluates agricultural hinterland radius and caps inland urban agglomeration size
     * based on overland transport infrastructure capital.
     *
     * @param cells      inhabited cells
     * @param deltaYears time step fraction in years
     */
    public static void processAgriculturalRings(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double capitalPerCapita = capital / pop;
            boolean isCoastal = cell.getBiome() == org.ether.society.model.Biome.OCEAN
                    || (cell.getElevation() != null && cell.getElevation() < 0.0);

            // Overland transport radius expansion:
            // Ancient walking/cart: r ~ 30-50 km
            // Paved road / canal (k_pc >= 200): r ~ 150 km
            // Railroad / truck (k_pc >= 1500): r ~ 2000 km
            double maxSustainableAgglomeration;
            if (isCoastal) {
                // Coastal cities access low-friction maritime grain fleets (Rome, Alexandria, Song Hangzhou)
                maxSustainableAgglomeration = 500_000.0 + capitalPerCapita * 2500.0;
            } else {
                // Inland cities strictly bounded by Von Thünen terrestrial friction
                if (capitalPerCapita < 100.0) {
                    maxSustainableAgglomeration = 25_000.0; // Bronze/Iron Age inland city limit
                } else if (capitalPerCapita < 800.0) {
                    maxSustainableAgglomeration = 80_000.0; // Early modern inland city limit
                } else {
                    maxSustainableAgglomeration = 80_000.0 + (capitalPerCapita - 800.0) * 5000.0; // Railroad industrial expansion
                }
            }

            // If inland population exceeds sustainable hinterland grain supply, food friction spikes
            if (pop > maxSustainableAgglomeration) {
                double excessRatio = (pop - maxSustainableAgglomeration) / maxSustainableAgglomeration;
                double currentFood = cell.getFoodResourceOr(pop * 1.0);
                // Transport friction penalizes excess food deliverability
                cell.setFoodResource(Math.max(0.1, currentFood * (1.0 - Math.min(0.5, excessRatio * 0.2))));
            }
        }
    }
}
