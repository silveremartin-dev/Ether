/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.ui.SpatialHeatmapPanel;
import org.ether.society.ui.VarianceDistributionPanel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stateless telemetry snapshot collector and UI card/chart synchronization helper
 * extracted from {@link org.ether.society.ui.StatsPanel} according to SRP.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see org.ether.society.ui.StatsPanel
 */
public final class StatsTelemetryUpdater {

    private StatsTelemetryUpdater() {
        // Stateless helper class
    }

    /**
     * Captures the current simulation telemetry from {@code engine} and dispatches a coalesced
     * JavaFX UI update across metric cards, sparklines, charts, spatial heatmaps, and variance panels.
     *
     * @param engine                   active simulation engine
     * @param metricCards              map of registered metric cards keyed by metric ID
     * @param activeTelemetryVariables enabled telemetry variable IDs
     * @param chartsSection            time-series and age-pyramid charts section
     * @param spatialHeatmapPanel      spatial heatmap view
     * @param variancePanel            statistical variance distribution view
     * @param statsUpdatePending       coalescing flag cleared at completion of the FX runLater block
     */
    public static void captureAndDispatchUpdate(H3SimulationEngine engine,
                                                Map<String, MetricCard> metricCards,
                                                Set<String> activeTelemetryVariables,
                                                StatsChartsSection chartsSection,
                                                SpatialHeatmapPanel spatialHeatmapPanel,
                                                VarianceDistributionPanel variancePanel,
                                                AtomicBoolean statsUpdatePending) {
        long pop = engine.getTotalPopulation();
        float bio = engine.getTotalBiomassNatural();
        float gini = engine.getCurrentGini();
        float gdp = engine.getCurrentGDP();
        float fert = engine.getCurrentFertility();
        float life = engine.getCurrentLifeExpectancy();
        float tech = engine.getAverageTechnology();

        double energyCap = engine.getEnergyCaptured();
        double resDep = engine.getResourceDepletionRate();
        double energyPerCap = engine.getEnergyPerCapita();
        double foodPerCap = engine.getFoodPerCapita();
        double eroiAlim = engine.getEroiAlimentaire();
        double netSurplus = engine.getNetSurplusFraction() * 100.0;
        double trophicMul = engine.getTrophicMultiplier();
        double bioMobilized = engine.getBiomassMobilizedPerCapitaKg();
        double bioDom = engine.getBiomassDomesticated();
        double water = engine.getPotableWaterTotal();
        double remRes = engine.getRemainingResourcesRatio();
        double entropy = engine.getSystemicEntropy();
        double territory = engine.getOccupiedTerritoryArea();

        double offspring = engine.getOffspringPercentage();
        double ageFirstChild = engine.getAgeAtFirstChild();
        double immigration = engine.getImmigrationRate();
        double education = engine.getEducationLevel();

        double happiness = engine.getHappinessIndex();
        double conflict = engine.getConflictLevel();
        int cityStates = engine.getCityStatesCount();
        double instMaturity = engine.getInstitutionalMaturity();
        double divLabor = engine.getDivisionOfLaborIndex();
        int maxHier = engine.getMaxHierarchyLevel();
        long largestCult = engine.getLargestCulturalUnitSize();
        double kardashev = engine.getKardashevScale();

        double builtCap = engine.getBuiltCapitalTotal();
        double eliteForm = engine.getEliteFormationRatio();
        double elderCap = engine.getElderCapitalShare();
        double landRent = engine.getLandRentIndex();
        long tools = engine.getToolsCount();
        long products = engine.getProductsCount();

        double sysComp = engine.getSystemComplexityIndex();
        double reconCap = engine.getReconstructionCapabilityIndex();
        double sysInter = engine.getSystemInterdependenceIndex();

        double shannonBw = engine.getShannonBandwidth();
        double memoryStock = engine.getCollectiveMemoryStock();
        double innovSpeed = engine.getInnovationDiffusionSpeed();
        double knowDecay = engine.getKnowledgeDecayRate();

        double soilNPK = engine.getSoilNPKQuality();
        double carbonFp = engine.getCarbonFootprint();
        double wildBio = engine.getWildBiodiversityIndex();
        double wetBulb = engine.getWetBulbSafetyMargin();

        double eliteOver = engine.getEliteOverproductionIndex();
        double fiscalStress = engine.getFiscalStressIndex();
        double geoTension = engine.getGeopoliticalTension();
        double collapseVuln = engine.getCollapseVulnerability();

        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        double tps = engine.getCurrentTPS();
        int totalCells = engine.getCells() != null ? engine.getCells().size() : 0;
        int year = engine.getTimeManager().getCurrentYear();

        Platform.runLater(() -> {
            try {
                spatialHeatmapPanel.setHistoryManager(engine.getHistoryManager());
                spatialHeatmapPanel.updateCells(engine.getCells());
                spatialHeatmapPanel.setDateLabel(String.format("📅 An %d", year));

                setCardVal(metricCards, activeTelemetryVariables, "energyCaptured", String.format("%,.1f", energyCap), energyCap);
                setCardVal(metricCards, activeTelemetryVariables, "resourceDepletion", String.format("%.1f", resDep), resDep);
                setCardVal(metricCards, activeTelemetryVariables, "energyPerCapita", String.format("%.1f", energyPerCap), energyPerCap);
                setCardVal(metricCards, activeTelemetryVariables, "foodPerCapita", String.format("%.2f", foodPerCap), foodPerCap);
                setCardVal(metricCards, activeTelemetryVariables, "eroiAlim", String.format("%.2f : 1", eroiAlim), eroiAlim);
                setCardVal(metricCards, activeTelemetryVariables, "netSurplus", String.format("%.1f%%", netSurplus), netSurplus);
                setCardVal(metricCards, activeTelemetryVariables, "trophicMultiplier", String.format("%.2fx", trophicMul), trophicMul);
                setCardVal(metricCards, activeTelemetryVariables, "biomassMobilized", String.format("%,.0f", bioMobilized), bioMobilized);
                setCardVal(metricCards, activeTelemetryVariables, "pibMaterialFlow", String.format("%.1f", resDep * 5.2), resDep * 5.2);
                setCardVal(metricCards, activeTelemetryVariables, "biomassNatural", String.format("%,.0f", bio), bio);
                setCardVal(metricCards, activeTelemetryVariables, "biomassDomesticated", String.format("%,.1f", bioDom), bioDom);
                setCardVal(metricCards, activeTelemetryVariables, "potableWater", String.format("%,.0f", water), water);
                setCardVal(metricCards, activeTelemetryVariables, "remainingResources", String.format("%.1f", remRes), remRes);
                setCardVal(metricCards, activeTelemetryVariables, "entropyPollution", String.format("%.1f", entropy), entropy);
                setCardVal(metricCards, activeTelemetryVariables, "occupiedTerritory", String.format("%,.0f", territory), territory);

                setCardVal(metricCards, activeTelemetryVariables, "population", String.format("%,d", pop), pop);
                setCardVal(metricCards, activeTelemetryVariables, "fertilityRate", String.format("%.1f", fert), fert);
                setCardVal(metricCards, activeTelemetryVariables, "offspringPct", String.format("%.1f", offspring), offspring);
                setCardVal(metricCards, activeTelemetryVariables, "ageFirstChild", String.format("%.1f", ageFirstChild), ageFirstChild);
                setCardVal(metricCards, activeTelemetryVariables, "immigrationRate", String.format("%.1f", immigration), immigration);
                setCardVal(metricCards, activeTelemetryVariables, "lifeExpectancy", String.format("%.1f", life), life);
                setCardVal(metricCards, activeTelemetryVariables, "healthIndex", String.format("%.1f", Math.min(100.0, life * 1.1)), Math.min(100.0, life * 1.1));
                setCardVal(metricCards, activeTelemetryVariables, "educationLevel", String.format("%.1f", education), education);

                double largestOrgComp = engine.getLargestOrganizationComplexity();
                double largestOrgEnt = engine.getLargestOrganizationEntropy();

                setCardVal(metricCards, activeTelemetryVariables, "happinessIndex", String.format("%.1f", happiness), happiness);
                setCardVal(metricCards, activeTelemetryVariables, "conflictLevel", String.format("%.1f", conflict), conflict);
                setCardVal(metricCards, activeTelemetryVariables, "cityStates", String.format("%d", cityStates), cityStates);
                setCardVal(metricCards, activeTelemetryVariables, "institutionalMaturity", String.format("%.1f", instMaturity), instMaturity);
                setCardVal(metricCards, activeTelemetryVariables, "divisionLabor", String.format("%.1f", divLabor), divLabor);
                setCardVal(metricCards, activeTelemetryVariables, "maxHierarchy", String.format("Niv %d", maxHier), maxHier);
                setCardVal(metricCards, activeTelemetryVariables, "largestCulture", String.format("%,d", largestCult), largestCult);
                setCardVal(metricCards, activeTelemetryVariables, "largestOrgComplexity", String.format("%,.0f", largestOrgComp), largestOrgComp);
                setCardVal(metricCards, activeTelemetryVariables, "largestOrgEntropy", String.format("%,.1f", largestOrgEnt), largestOrgEnt);
                setCardVal(metricCards, activeTelemetryVariables, "avgTechLevel", String.format("%.2f", tech), tech);
                setCardVal(metricCards, activeTelemetryVariables, "kardashevScale", String.format("%.2f", kardashev), kardashev);

                setCardVal(metricCards, activeTelemetryVariables, "giniIndex", String.format("%.2f", gini), gini);
                setCardVal(metricCards, activeTelemetryVariables, "gdpTotal", String.format("%,.0f", gdp), gdp);
                setCardVal(metricCards, activeTelemetryVariables, "builtCapital", String.format("%,.0f", builtCap / Math.max(1, pop)), builtCap / Math.max(1, pop));
                setCardVal(metricCards, activeTelemetryVariables, "eliteFormation", String.format("%.1f", eliteForm), eliteForm);
                setCardVal(metricCards, activeTelemetryVariables, "elderCapitalShare", String.format("%.1f", elderCap), elderCap);
                setCardVal(metricCards, activeTelemetryVariables, "landRent", String.format("%.1f", landRent), landRent);
                setCardVal(metricCards, activeTelemetryVariables, "toolsCount", String.format("%,d", tools), tools);
                setCardVal(metricCards, activeTelemetryVariables, "productsCount", String.format("%,d", products), products);

                setCardVal(metricCards, activeTelemetryVariables, "shannonBandwidth", String.format("%.1f", shannonBw), shannonBw);
                setCardVal(metricCards, activeTelemetryVariables, "collectiveMemory", String.format("%,.0f", memoryStock), memoryStock);
                setCardVal(metricCards, activeTelemetryVariables, "innovationDiffusion", String.format("%.1f", innovSpeed), innovSpeed);
                setCardVal(metricCards, activeTelemetryVariables, "knowledgeDecay", String.format("%.1f", knowDecay), knowDecay);

                setCardVal(metricCards, activeTelemetryVariables, "soilNPK", String.format("%.1f", soilNPK), soilNPK);
                setCardVal(metricCards, activeTelemetryVariables, "carbonFootprint", String.format("%.2f", carbonFp), carbonFp);
                setCardVal(metricCards, activeTelemetryVariables, "wildBiodiversity", String.format("%.1f", wildBio), wildBio);
                setCardVal(metricCards, activeTelemetryVariables, "wetBulbSafety", String.format("%.1f", wetBulb), wetBulb);

                setCardVal(metricCards, activeTelemetryVariables, "eliteOverproduction", String.format("%.2f", eliteOver), eliteOver);
                setCardVal(metricCards, activeTelemetryVariables, "fiscalStress", String.format("%.1f", fiscalStress), fiscalStress);
                setCardVal(metricCards, activeTelemetryVariables, "geopoliticalTension", String.format("%.1f", geoTension), geoTension);
                setCardVal(metricCards, activeTelemetryVariables, "collapseVulnerability", String.format("%.1f", collapseVuln), collapseVuln);

                setCardVal(metricCards, activeTelemetryVariables, "systemComplexity", String.format("%.1f", sysComp), sysComp);
                setCardVal(metricCards, activeTelemetryVariables, "reconstructionCapability", String.format("%.1f", reconCap), reconCap);
                setCardVal(metricCards, activeTelemetryVariables, "systemInterdependence", String.format("%.1f", sysInter), sysInter);

                setCardVal(metricCards, activeTelemetryVariables, "engineTPS", String.format("%.1f", tps), tps);
                setCardVal(metricCards, activeTelemetryVariables, "ramMemory", String.format("%d", usedMem), usedMem);
                setCardVal(metricCards, activeTelemetryVariables, "cellCount", String.format("%,d", totalCells), totalCells);

                ComboBox<String> combo = chartsSection.getChartMetricCombo();
                String selectedMetric = combo != null ? combo.getValue() : "Population Humaine";
                if (selectedMetric == null || selectedMetric.startsWith("─── ")) {
                    selectedMetric = "Population Humaine";
                }
                double yVal = extractSelectedMetricValue(engine, metricCards, selectedMetric,
                        energyCap, resDep, energyPerCap, foodPerCap, bio, bioDom,
                        water, remRes, entropy, territory, pop, fert, offspring, ageFirstChild, immigration, life,
                        education, happiness, conflict, cityStates, instMaturity, divLabor, maxHier, largestCult,
                        largestOrgComp, largestOrgEnt, tech, kardashev, gini, gdp, builtCap, eliteForm, elderCap,
                        landRent, tools, products, shannonBw, memoryStock, innovSpeed, knowDecay, soilNPK, carbonFp,
                        wildBio, wetBulb, eliteOver, fiscalStress, geoTension, collapseVuln, sysComp, reconCap,
                        sysInter, tps, usedMem, totalCells);

                double currentTime = year + (engine.getTimeManager().getCurrentMonth() / 12.0);
                long minYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : 0L;
                chartsSection.appendTimeSeriesPoint(currentTime, yVal, minYear);
                chartsSection.updateAgePyramid(engine.getAgePyramid());
                variancePanel.updateData(engine.getCells());
            } finally {
                statsUpdatePending.set(false);
            }
        });
    }

    private static void setCardVal(Map<String, MetricCard> metricCards,
                                   Set<String> activeTelemetryVariables,
                                   String key, String val, double ratio) {
        MetricCard c = metricCards.get(key);
        if (c != null) {
            if (activeTelemetryVariables != null && !activeTelemetryVariables.isEmpty() && !activeTelemetryVariables.contains(key)) {
                c.updateValue("— (Désactivé)", 0.0);
                c.setOpacity(0.4);
                return;
            }
            c.setOpacity(1.0);
            c.updateValue(val, ratio);
        }
    }

    private static double extractSelectedMetricValue(
            H3SimulationEngine engine, Map<String, MetricCard> metricCards, String selectedMetric,
            double energyCap, double resDep, double energyPerCap, double foodPerCap, float bio, double bioDom,
            double water, double remRes, double entropy, double territory, long pop, float fert, double offspring,
            double ageFirstChild, double immigration, float life, double education, double happiness, double conflict,
            int cityStates, double instMaturity, double divLabor, int maxHier, long largestCult, double largestOrgComp,
            double largestOrgEnt, float tech, double kardashev, float gini, float gdp, double builtCap, double eliteForm,
            double elderCap, double landRent, long tools, long products, double shannonBw, double memoryStock,
            double innovSpeed, double knowDecay, double soilNPK, double carbonFp, double wildBio, double wetBulb,
            double eliteOver, double fiscalStress, double geoTension, double collapseVuln, double sysComp,
            double reconCap, double sysInter, double tps, long usedMem, int totalCells) {
        if (selectedMetric == null) return pop;
        String key = null;
        for (MetricCard card : metricCards.values()) {
            if (card.getTitle().equalsIgnoreCase(selectedMetric)) {
                key = card.getKey();
                break;
            }
        }
        if (key == null) return pop;
        return switch (key) {
            case "energyCaptured" -> energyCap;
            case "resourceDepletion" -> resDep;
            case "energyPerCapita" -> energyPerCap;
            case "foodPerCapita" -> foodPerCap;
            case "eroiAlim" -> engine.getEroiAlimentaire();
            case "netSurplus" -> engine.getNetSurplusFraction() * 100.0;
            case "trophicMultiplier" -> engine.getTrophicMultiplier();
            case "biomassMobilized" -> engine.getBiomassMobilizedPerCapitaKg();
            case "pibMaterialFlow" -> resDep * 5.2;
            case "biomassNatural" -> bio;
            case "biomassDomesticated" -> bioDom;
            case "potableWater" -> water;
            case "remainingResources" -> remRes;
            case "entropyPollution" -> entropy;
            case "occupiedTerritory" -> territory;
            case "population" -> pop;
            case "fertilityRate" -> fert;
            case "offspringPct" -> offspring;
            case "ageFirstChild" -> ageFirstChild;
            case "immigrationRate" -> immigration;
            case "lifeExpectancy" -> life;
            case "healthIndex" -> Math.min(100.0, life * 1.1);
            case "educationLevel" -> education;
            case "happinessIndex" -> happiness;
            case "conflictLevel" -> conflict;
            case "cityStates" -> cityStates;
            case "institutionalMaturity" -> instMaturity;
            case "divisionLabor" -> divLabor;
            case "maxHierarchy" -> maxHier;
            case "largestCulture" -> largestCult;
            case "largestOrgComplexity" -> largestOrgComp;
            case "largestOrgEntropy" -> largestOrgEnt;
            case "avgTechLevel" -> tech;
            case "kardashevScale" -> kardashev;
            case "giniIndex" -> gini;
            case "gdpTotal" -> gdp;
            case "builtCapital" -> builtCap / Math.max(1, pop);
            case "eliteFormation" -> eliteForm;
            case "elderCapitalShare" -> elderCap;
            case "landRent" -> landRent;
            case "toolsCount" -> tools;
            case "productsCount" -> products;
            case "shannonBandwidth" -> shannonBw;
            case "collectiveMemory" -> memoryStock;
            case "innovationDiffusion" -> innovSpeed;
            case "knowledgeDecay" -> knowDecay;
            case "soilNPK" -> soilNPK;
            case "carbonFootprint" -> carbonFp;
            case "wildBiodiversity" -> wildBio;
            case "wetBulbSafety" -> wetBulb;
            case "eliteOverproduction" -> eliteOver;
            case "fiscalStress" -> fiscalStress;
            case "geopoliticalTension" -> geoTension;
            case "collapseVulnerability" -> collapseVuln;
            case "systemComplexity" -> sysComp;
            case "reconstructionCapability" -> reconCap;
            case "systemInterdependence" -> sysInter;
            case "engineTPS" -> tps;
            case "ramMemory" -> usedMem;
            case "cellCount" -> totalCells;
            default -> pop;
        };
    }
}
