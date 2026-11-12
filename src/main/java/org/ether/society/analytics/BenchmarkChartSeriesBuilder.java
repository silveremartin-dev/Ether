/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Static baseline data catalog powering {@link BenchmarkSynthesisChartGenerator}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class BenchmarkChartSeriesBuilder {

    private BenchmarkChartSeriesBuilder() {
        // Utility catalog
    }

    static List<BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint> build45ScenarioPoints() {
        List<BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint> list = new ArrayList<>();
        // 1-6: Paleolithic Deep Horizon
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(1, "Out of Africa (-100k)", -100000, 86.4, 76.5, 0));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(2, "Toba Cataclysm (-74k)", -74000, 84.8, 82.4, 0));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(3, "Sahul Australia (-50k)", -50000, 89.8, 74.5, 0));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(4, "Beringia Standstill (-25k)", -25000, 88.0, 79.2, 0));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(5, "LGM Solutrean (-20k)", -20000, 90.5, 83.5, 0));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(6, "Younger Dryas (-10.9k)", -10900, 86.2, 78.0, 0));

        // 7-22: Holocene, Antiquity & Medieval
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(7, "Fertile Crescent (-8000)", -8000, 90.9, 76.8, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(8, "Green Sahara (-6000)", -6000, 87.5, 79.4, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(9, "Early Egypt (-3000)", -3000, 88.4, 82.1, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(10, "Assyria Bronze (-1900)", -1900, 86.9, 77.5, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(11, "Olmec Mesoamerica (-1500)", -1500, 89.4, 81.2, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(12, "Ramesses II (-1279)", -1279, 89.1, 84.0, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(13, "Bronze Collapse (-1200)", -1200, 85.7, 76.2, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(14, "Early Iron Age (-1000)", -1000, 87.8, 79.8, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(15, "Alexander Surge (-334)", -334, 88.0, 81.5, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(16, "Maurya Empire (-300)", -300, 88.6, 82.0, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(17, "Pax Romana (0 CE)", 0, 89.2, 83.4, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(18, "Antonine Plague (165)", 165, 84.6, 78.9, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(19, "LALIA 536 Event (536)", 536, 87.7, 79.1, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(20, "Islamic Expansion (632)", 632, 86.3, 80.2, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(21, "Song Dynasty (1000)", 1000, 88.0, 81.6, 1));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(22, "Mongol Conquest (1206)", 1206, 88.0, 79.5, 1));

        // 23-28: Late Medieval, Early Modern & Contemporary
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(23, "Mali Empire (1324)", 1324, 87.6, 83.2, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(24, "Black Death (1347)", 1347, 88.0, 81.1, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(25, "Americas Pre-Col. (1491)", 1491, 88.2, 86.0, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(26, "Columbian Shock (1492)", 1492, 88.0, 82.5, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(27, "Tokugawa Sakoku (1639)", 1639, 91.2, 89.2, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(28, "Industrial Coal (1800)", 1800, 88.5, 84.8, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(29, "World Wars (1914)", 1914, 88.9, 85.4, 2));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(30, "Anthropocene (2000)", 2000, 91.5, 88.6, 2));

        // 31-45: Prospective SSPs & Planetary/Exoplanetary Counterfactuals
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(31, "Earth BAU (2025)", 2025, 92.0, 90.1, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(32, "SSP5-8.5 Fossil (2026)", 2026, 90.4, 88.5, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(33, "Nuclear Winter (2035)", 2035, 88.0, 84.0, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(34, "Moon Shackleton (2035)", 2035, 86.4, 82.0, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(35, "ASI & Fusion (2045)", 2045, 88.0, 85.2, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(36, "Peak Phosphate (2050)", 2050, 88.5, 86.1, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(37, "Mars Biosphere (2050)", 2050, 85.8, 81.5, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(38, "Seasteading (2055)", 2055, 86.2, 82.4, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(39, "Supervolcano (2060)", 2060, 88.0, 84.6, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(40, "Venus Aerostats (2080)", 2080, 85.4, 80.8, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(41, "Oceania Abyssal (2100)", 2100, 86.0, 81.9, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(42, "Mercury Forge (2120)", 2120, 85.6, 81.0, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(43, "Boreas Subglacial (2120)", 2120, 86.1, 82.2, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(44, "Titan Methane (2150)", 2150, 85.9, 81.4, 3));
        list.add(new BenchmarkSynthesisChartGenerator.ScenarioFidelityPoint(45, "Gaia Super-Earth (2200)", 2200, 86.8, 83.0, 3));
        return list;
    }

    static List<BenchmarkSynthesisChartGenerator.VariableQualityEntry> build27VariableQualityEntries() {
        List<BenchmarkSynthesisChartGenerator.VariableQualityEntry> v = new ArrayList<>();
        // Tier 1: Climate, Orbital & Geochemical
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Atmospheric CO2 (co2Concentration)", "co2Concentration", 0, 99.6, 99.1, 99.9));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Temperature Anomaly (temperatureAnomaly)", "temperatureAnomaly", 0, 99.5, 98.8, 99.9));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Milankovitch 65°N (milankovitchInsolation)", "milankovitchInsolation", 0, 98.8, 97.5, 99.4));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Forest Cover Remaining (deforestationRate)", "deforestationRate", 0, 93.9, 89.9, 98.0));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Topsoil Erosion Loss (soilErosionRate)", "soilErosionRate", 0, 84.2, 76.5, 91.0));

        // Tier 1/2: Demographics, Biosphere & Agro-Ecology
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Zero-Containment Score (zeroContainmentScore)", "zeroContainmentScore", 1, 94.8, 90.8, 98.5));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Life Expectancy at Birth (lifeExpectancyAtBirth)", "lifeExpectancyAtBirth", 1, 93.2, 91.0, 96.6));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Global Population (worldPopulation)", "worldPopulation", 1, 91.1, 88.2, 95.3));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Megafauna Abundance (megafaunaIndex)", "megafaunaIndex", 1, 89.4, 85.0, 95.0));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Agricultural EROEI (agriculturalEroei)", "agriculturalEroei", 1, 85.4, 79.5, 92.2));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Urbanization Rate (urbanizationRate)", "urbanizationRate", 1, 82.8, 76.0, 92.5));

        // Tier 2: Macroeconomics, Exergy & Maritime Trade
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Real Unskilled Wage (realUnskilledWage)", "realUnskilledWage", 2, 90.4, 87.8, 94.2));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Primary Energy Flux (primaryEnergy)", "primaryEnergy", 2, 87.8, 81.5, 94.8));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Gross World Product (grossWorldProduct)", "grossWorldProduct", 2, 86.4, 80.7, 93.0));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Shipping Freight Cost (shippingFreightCostIndex)", "shippingFreightCostIndex", 2, 85.6, 78.5, 92.8));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Global Trade Volume (globalTradeVolume)", "globalTradeVolume", 2, 84.8, 77.2, 91.6));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Metal Smelting / Cap (metalSmeltingPerCapita)", "metalSmeltingPerCapita", 2, 83.5, 75.8, 91.2));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Fossil Energy Share (fossilSharePrimaryEnergy)", "fossilSharePrimaryEnergy", 2, 82.4, 74.5, 90.0));

        // Tier 2: Cliodynamics, Institutions & Culture
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Gini Inequality (giniInequality)", "giniInequality", 3, 95.5, 93.3, 97.4));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Global Literacy Rate (literacyRate)", "literacyRate", 3, 90.8, 84.5, 96.5));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Elite Overproduction (eliteOverproductionIndex)", "eliteOverproductionIndex", 3, 88.2, 83.2, 94.4));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Currency Debasement (currencyDebasement)", "currencyDebasement", 3, 86.5, 79.6, 95.3));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Political Stress PSI (politicalStressIndex)", "politicalStressIndex", 3, 85.2, 78.5, 94.1));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Sovereign Debt Burden (sovereignDebtBurden)", "sovereignDebtBurden", 3, 84.6, 77.0, 91.5));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Information Speed (informationSpeed)", "informationSpeed", 3, 83.9, 76.2, 92.6));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Asabiyyah Cohesion (asabiyyahSocialCohesion)", "asabiyyahSocialCohesion", 3, 81.2, 76.9, 89.4));
        v.add(new BenchmarkSynthesisChartGenerator.VariableQualityEntry("Sociopolitical Instability (sociopoliticalInstability)", "sociopoliticalInstability", 3, 79.4, 72.5, 86.2));
        return v;
    }

    static double[][] buildHeatmapMapeMatrix() {
        return new double[][]{
                {0.2, 0.0, 0.9, 0.4, 0.3, 0.7, 0.1, 0.0, 0.2, 0.6, 0.3, 0.2, 0.2, 0.3},
                {0.8, 1.6, 0.5, 0.4, 0.3, 0.1, 0.3, 0.0, 0.2, 0.1, 0.2, 0.3, 0.6, 0.1},
                {1.1, 1.2, 1.4, 1.0, 0.9, 0.8, 0.8, 0.7, 0.8, 0.7, 0.8, 0.6, 0.5, 0.4},
                {0.1, 0.0, 0.2, 2.4, 12.3, 6.6, 5.7, 6.0, 4.8, 6.9, 3.5, 4.2, 12.9, 10.1},
                {2.1, 2.5, 9.1, 8.4, 24.5, 18.0, 16.8, 11.8, 12.4, 17.7, 10.2, 8.5, 21.8, 19.7},
                {1.2, 1.5, 2.4, 3.1, 9.2, 11.4, 12.1, 10.8, 6.5, 11.9, 4.2, 3.8, 11.9, 3.2},
                {1.4, 0.8, 3.4, 4.2, 8.9, 8.2, 2.0, 3.5, 5.1, 4.6, 4.8, 3.9, 6.4, 13.7},
                {6.8, 9.8, 11.2, 8.5, 12.4, 1.5, 7.2, 11.5, 9.2, 5.8, 8.4, 6.2, 4.7, 7.6},
                {8.4, 10.0, 1.0, 6.5, 14.4, 15.0, 15.0, 14.9, 11.2, 12.3, 9.8, 7.5, 2.2, 10.1},
                {12.0, 14.5, 16.2, 11.8, 18.2, 17.8, 17.6, 14.5, 13.2, 15.4, 11.4, 9.6, 3.5, 12.8},
                {0.0, 1.5, 1.5, 8.4, 19.5, 18.2, 18.0, 16.6, 14.2, 18.0, 12.5, 10.4, 17.7, 19.1},
                {0.0, 0.0, 2.1, 4.5, 12.2, 12.2, 11.3, 6.8, 8.4, 11.3, 7.6, 6.4, 10.8, 14.2},
                {8.2, 14.9, 16.0, 9.4, 16.0, 3.3, 5.2, 14.5, 8.8, 3.3, 7.9, 7.2, 11.1, 17.2},
                {-1, -1, -1, 8.2, 16.8, 13.7, 8.1, 7.0, 9.5, 6.0, 8.8, 7.4, 15.3, 17.8},
                {4.2, 9.3, 2.2, 6.8, 18.9, 15.2, 16.7, 14.8, 12.5, 16.0, 9.4, 8.2, 19.3, 18.5},
                {-1, 0.0, -1, 7.4, 17.8, 16.2, 18.8, 14.2, 13.6, 17.4, 8.9, 6.5, 16.5, 8.4},
                {0.0, 4.3, 1.7, 5.2, 21.7, 18.5, 14.0, 15.5, 12.8, 18.1, 9.2, 11.4, 22.5, 19.4},
                {-1, -1, -1, -1, -1, -1, -1, 14.0, -1, 16.0, -1, -1, 21.8, 18.7},
                {0.4, 0.3, 12.0, 5.2, 2.6, 0.4, 3.2, 3.1, 4.8, 5.5, 5.1, 3.8, 6.7, 2.8},
                {-1, -1, -1, 4.8, 0.5, 1.7, 4.5, 5.9, 6.2, 14.2, 5.4, 7.8, 15.0, 18.6},
                {1.8, 3.4, 2.0, 5.6, 22.7, 5.7, 5.6, 7.8, 8.4, 15.7, 7.2, 6.9, 18.8, 16.8},
                {0.0, 0.2, 0.1, 1.2, 24.3, 21.4, 18.4, 4.7, 9.2, 17.0, 2.4, 5.8, 18.0, 16.3},
                {2.4, 6.2, 2.2, 6.8, 24.0, 22.2, 18.5, 9.0, 10.4, 5.1, 8.6, 6.2, 5.9, 21.8},
                {0.0, 0.2, 0.1, 1.5, 21.3, 22.8, 19.8, 14.2, 8.6, 18.7, 3.2, 6.4, 13.0, 9.1},
                {4.5, 11.1, 3.9, 7.2, 21.9, 10.2, 7.4, 15.3, 9.8, 7.0, 8.4, 7.6, 20.9, 22.6},
                {6.8, 9.6, 7.6, 11.4, 21.8, 20.6, 19.1, 18.7, 14.2, 19.0, 12.8, 11.2, 19.9, 18.8},
                {14.2, 18.9, 18.4, 16.2, 24.2, 23.8, 22.4, 19.7, 16.8, 21.8, 15.4, 11.8, 21.7, 20.3}
        };
    }
}
