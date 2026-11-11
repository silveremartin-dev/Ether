/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import org.ether.society.analytics.CliodynamicAnalyticalFormulasCatalog;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;

import java.util.Map;

/**
 * <h1>Telemetry DAG Node Evaluator</h1>
 *
 * <p>Topological memoized evaluator executing canonical node formulas for macroscopic cliodynamics,
 * historical demographics, resource balances, and thermodynamic energy metrics.</p>
 *
 * <p>Operates strictly as an $O(1)$ functional transformation of pre-accumulated physical observables
 * contained within {@link CompiledTelemetryPlan.EvalContext}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CompiledTelemetryPlan
 */
final class TelemetryDagNodeEvaluator {

    private TelemetryDagNodeEvaluator() {
        // Pure stateless utility evaluator
    }

    /**
     * Evaluates a single canonical DAG node from physical observables and prior topologically memoized values.
     *
     * @param id node identifier
     * @param c  physical observables context
     * @param m  memoized values from upstream DAG dependencies
     * @return evaluated scalar observable
     */
    static double evalNode(String id, CompiledTelemetryPlan.EvalContext c, Map<String, Double> m) {
        return switch (id) {
            case "population" -> c.popSum;
            case "populatedCellCount" -> (double) c.populatedCount;
            case "occupiedTerritory" -> c.populatedCount * 225.0; // Mean H3 Res 4 cell area (~225-250 km2 inhabited footprint proxy)
            case "populationSurvivalRate" -> {
                if (c.initialPop <= 0L) yield 100.0;
                yield Math.clamp((c.popSum / c.initialPop) * 100.0, 0.0, 100.0);
            }
            case "builtCapital" -> c.builtCapSum;
            case "capitalPerCapita" -> c.kpc();
            case "energyCaptured" -> c.energyCapturedMw;
            case "energyPerCapita" -> c.powerWattsPerCapita();
            case "avgTechLevel" -> c.phiExergy();
            case "primaryEnergy" -> {
                if (c.energyCapturedMw > 0.0) yield c.energyCapturedMw * 3.15576e-5; // MW -> EJ/yr
                yield CliodynamicAnalyticalFormulasCatalog.computePrimaryEnergyEJ(c.popLong, m.getOrDefault("avgTechLevel", c.phiExergy()));
            }
            case "kardashevScale" -> CliodynamicAnalyticalFormulasCatalog.computeKardashevScale(c.powerWattsPerCapita(), c.popLong);
            case "foodPerCapita" -> c.foodPerCapita;
            case "biomassNatural" -> c.bioNatSum;
            case "biomassDomesticated" -> c.bioAgrSum + c.bioLivSum;
            case "potableWater" -> c.waterSum;
            case "resourceDepletion" -> {
                double maxRes = Math.max(1.0, c.capacity * 100_000.0);
                yield Math.clamp((1.0 - c.currentNonRenRes / maxRes) * 100.0, 0.0, 100.0);
            }
            case "remainingResources" -> 100.0 - m.getOrDefault("resourceDepletion", 0.0);
            case "soilNPK" -> {
                double resDep = m.getOrDefault("resourceDepletion", 0.0);
                double haberBosch = 22.0 * SmoothMath.sigmoid(c.phiExergy(), 2.1, 0.25);
                if (c.arableCells > 0 && c.weightedSoilOrg > 0.0) {
                    double organicScore = Math.clamp((c.weightedSoilOrg / c.arableCells) * 2.5, 10.0, 100.0);
                    yield Math.clamp(0.5 * organicScore + 0.5 * (100.0 - resDep * 0.6 + haberBosch), 5.0, 100.0);
                }
                yield Math.clamp(100.0 - resDep * 0.6 + haberBosch, 5.0, 100.0);
            }
            case "gdp" -> (double) c.gdp;
            case "grossWorldProduct" -> c.gdp > 0.0 ? (c.gdp / 1e9)
                    : CliodynamicAnalyticalFormulasCatalog.computeGrossWorldProduct(c.popLong, m.getOrDefault("avgTechLevel", c.phiExergy()));
            case "gini" -> (double) c.gini;
            case "landGini" -> Math.clamp(c.gini * 1.12, 0.15, 0.85);
            case "lifeExpectancy" -> (double) c.lifeExp;
            case "fertilityRate" -> (double) c.fertility;
            case "urbanizationRate" -> {
                if (c.popSum <= 0.0) yield 0.0;
                double spatialUrbanPct = (c.urbanPop / c.popSum) * 100.0;
                yield Math.clamp(spatialUrbanPct, 0.0, 95.0);
            }
            case "literacyRate" -> {
                double phi = m.getOrDefault("avgTechLevel", c.phiExergy());
                double act = SmoothMath.sigmoid(phi, 0.96, 0.025);
                double rate = 99.0 * SmoothMath.sigmoid(phi, 1.85, 0.35);
                yield Math.clamp(act * rate, 0.0, 99.5);
            }
            case "asabiyyah" -> {
                if (c.polityCount > 0) yield c.avgAsabiyyahPct;
                double coh = CliodynamicAnalyticalFormulasCatalog.computeAsabiyyahSocialCohesion(c.phiExergy(), 70.0);
                yield coh * 100.0;
            }
            case "asabiyyahSocialCohesion" -> Math.clamp(m.getOrDefault("asabiyyah", 70.0) / 100.0, 0.0, 1.0);
            case "currencyDebasement" -> {
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeCurrencyDebasement(c.phiExergy(), stab);
            }
            case "eliteOverproduction" -> {
                if (c.polityCount > 0) yield c.meanEliteOverprodPolity;
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeEliteOverproductionIndex(c.phiExergy(), stab);
            }
            case "realUnskilledWage" -> CliodynamicAnalyticalFormulasCatalog.computeRealUnskilledWageIndex(c.phiExergy());
            case "politicalStressIndex" -> {
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computePoliticalStressIndex(c.phiExergy(), stab);
            }
            case "sociopoliticalInstability" -> {
                if (c.polityCount > 0) yield c.meanPolityInstab;
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeSociopoliticalInstability(c.phiExergy(), stab);
            }
            case "conflict" -> m.getOrDefault("sociopoliticalInstability", 15.0);
            case "globalTradeVolume" -> CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy());
            case "inlandFluvialFreightVolume" -> CliodynamicAnalyticalFormulasCatalog.computeInlandFluvialFreightVolume(c.phiExergy());
            case "sovereignDebtBurden" -> {
                double trade = m.getOrDefault("globalTradeVolume", CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy()));
                double lit = m.getOrDefault("literacyRate", 0.0);
                double stab = m.getOrDefault("asabiyyah", 70.0);
                yield CliodynamicAnalyticalFormulasCatalog.computeSovereignDebtBurden(trade, lit, stab, c.phiExergy());
            }
            case "co2Concentration" -> {
                double baseCo2 = c.t0Co2Ppm > 0.0 ? c.t0Co2Ppm : 280.0;
                double exergyDelta = Math.max(0.0, CliodynamicAnalyticalFormulasCatalog.computeCo2Concentration(c.phiExergy()) - 280.0);
                double fossilIntensity = CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(c.phiExergy()) / 100.0;
                double cumDelta = Math.min(250.0, fossilIntensity * (c.energyCapturedMw / 1.0e6) * 0.015 * Math.max(0.0, c.elapsedYears));
                yield Math.clamp(baseCo2 + exergyDelta + cumDelta, 160.0, 2000.0);
            }
            case "temperatureAnomaly" -> {
                double co2 = m.getOrDefault("co2Concentration", 280.0);
                double radiativeAnom = CliodynamicAnalyticalFormulasCatalog.computeTemperatureAnomalyFromCo2(co2);
                if (c.landCellCount > 0) {
                    double cellDelta = (c.landTempSum / c.landCellCount) - 14.5;
                    yield Math.clamp(0.5 * radiativeAnom + 0.5 * cellDelta, -8.5, 8.5);
                }
                yield Math.clamp(radiativeAnom, -8.5, 8.5);
            }
            case "soilErosionRate" -> CliodynamicAnalyticalFormulasCatalog.computeTopsoilErosionLoss(c.phiExergy());
            case "agriculturalEroei" -> CliodynamicAnalyticalFormulasCatalog.computeAgriculturalEroei(c.phiExergy());
            case "netSurplus" -> {
                double eroei = m.getOrDefault("agriculturalEroei", 1.0);
                yield Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroei))) * 100.0;
            }
            case "deforestationRate" -> CliodynamicAnalyticalFormulasCatalog.computeForestCoverRemaining(c.phiExergy());
            case "wildBiodiversity" -> {
                double totalBio = c.bioNatSum + c.bioAgrSum + c.bioLivSum;
                yield totalBio > 0.0 ? Math.clamp((c.bioNatSum / totalBio) * 100.0, 0.0, 100.0) : 100.0;
            }
            case "informationSpeed" -> CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(c.phiExergy());
            case "metalSmeltingPerCapita" -> CliodynamicAnalyticalFormulasCatalog.computeMetalSmeltingPerCapita(c.phiExergy());
            case "fossilSharePrimaryEnergy" -> CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(c.phiExergy());
            case "carbonFootprint" -> {
                double eMw = c.energyCapturedMw;
                double fos = m.getOrDefault("fossilSharePrimaryEnergy", 0.0);
                yield (eMw * fos) / 1000.0;
            }
            case "shippingFreightCostIndex" -> CliodynamicAnalyticalFormulasCatalog.computeShippingFreightCostIndex(c.phiExergy());
            case "zeroContainmentScore" -> {
                double trade = m.getOrDefault("globalTradeVolume", CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(c.phiExergy()));
                yield CliodynamicAnalyticalFormulasCatalog.computeZeroContainmentScoreFromTrade(trade);
            }
            case "megafaunaIndex" -> {
                double denom = Math.max(1.0, c.landCellCount * 4500.0);
                yield Math.clamp(c.bioNatSum / denom, 0.0, 1.0);
            }
            case "milankovitchInsolation" -> {
                double meanTemp = c.landCellCount > 0 ? (c.landTempSum / c.landCellCount) : 14.5;
                yield Math.clamp(495.0 + (meanTemp - 14.5) * 4.5, 420.0, 560.0);
            }
            case "trophicMultiplier" -> Math.clamp(1.05 + 1.8 * SmoothMath.sigmoid(c.phiExergy(), 1.4, 0.4), 1.05, 3.20);
            case "biomassMobilized" -> {
                double tm = m.getOrDefault("trophicMultiplier", 1.2);
                yield (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * tm) / 15.0;
            }
            case "entropyPollution" -> c.popSum <= 0.0 ? 0.0 : Math.min(1000.0, (Math.log1p(c.popSum) * 25.0) + 2.5 * Math.log1p(c.kpc() / 25.0));
            case "wetBulbSafety" -> {
                double meanT = c.landCellCount > 0 ? (c.landTempSum / c.landCellCount) : 14.5;
                yield Math.max(0.0, 35.0 - (meanT + 3.5));
            }
            case "carryingCapacitySat" -> {
                double cap = Math.max(100.0, (c.foodSum * 17.5) / 1200.0);
                yield c.popSum / cap;
            }
            case "planetaryOvershoot" -> Math.clamp(0.4 + 0.6 * m.getOrDefault("carryingCapacitySat", 0.5), 0.1, 5.0);
            case "happiness" -> {
                double foodScore = Math.min(60.0, c.foodPerCapita * 30.0);
                double waterScore = Math.min(40.0, c.popSum > 0 ? (c.waterSum / c.popSum) * 2.0 : 30.0);
                yield Math.clamp(foodScore + waterScore, 10.0, 100.0);
            }
            case "institutionalMaturity" -> Math.clamp(20.0 + 75.0 * SmoothMath.sigmoid(c.phiExergy(), 1.25, 0.3), 0.0, 100.0);
            case "divisionLabor" -> Math.clamp(10.0 + 85.0 * SmoothMath.sigmoid(c.phiExergy(), 1.35, 0.35), 0.0, 100.0);
            case "cityStates" -> (double) c.cityStatesCount;
            case "maxHierarchy" -> (double) c.maxHierarchy;
            case "largestCulture" -> (double) Math.max(c.largestUnitSize, c.popLong > 0 ? Math.min(c.popLong, 500L) : 0L);
            case "largestOrgComplexity" -> c.largestUnitSize * c.phiExergy() * c.maxHierarchy;
            case "largestOrgEntropy" -> c.largestUnitSize * c.powerWattsPerCapita() * 0.05;
            case "eliteFormation" -> Math.clamp(0.5 + 12.0 * SmoothMath.sigmoid(c.phiExergy(), 1.1, 0.25), 0.5, 25.0);
            case "elderCapitalShare" -> Math.clamp(30.0 + 50.0 * SmoothMath.sigmoid(c.lifeExp, 45.0, 12.0), 30.0, 90.0);
            case "landRent" -> Math.clamp(10.0 + 90.0 * (c.popSum / Math.max(1.0, c.capacity * 1000.0)), 1.0, 500.0);
            case "toolsCount" -> c.popSum <= 0.0 ? 0.0 : c.popSum * (0.50 + 0.15 * Math.log1p(c.kpc() / 10.0));
            case "productsCount" -> c.popSum <= 0.0 ? 3.0 : Math.max(3.0, 5.0 + 3.2 * Math.log1p(c.kpc() / 5.0));
            case "shannonBandwidth" -> Math.pow(10.0, Math.min(9.0, c.phiExergy() * 1.8));
            case "collectiveMemory" -> Math.max(0.0, c.popSum * Math.max(0.0, c.phiExergy() - 0.90) * 0.01);
            case "innovationDiffusion" -> CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(c.phiExergy());
            case "knowledgeDecay" -> {
                double instab = m.getOrDefault("sociopoliticalInstability", 15.0);
                yield Math.clamp(0.5 + 0.25 * instab, 0.1, 50.0);
            }
            case "fiscalStress" -> m.getOrDefault("sovereignDebtBurden", 0.0) * 0.4;
            case "geopoliticalTension" -> m.getOrDefault("sociopoliticalInstability", 15.0) * 0.85;
            case "collapseRisk" -> Math.clamp(m.getOrDefault("politicalStressIndex", 0.0) * 0.65 + (100.0 - m.getOrDefault("asabiyyah", 70.0)) * 0.35, 0.0, 100.0);
            case "systemComplexity" -> Math.clamp(c.phiExergy() * 18.0, 0.0, 100.0);
            case "reconstructionCapability" -> Math.clamp(30.0 + m.getOrDefault("literacyRate", 0.0) * 0.6, 5.0, 100.0);
            case "systemInterdependence" -> Math.clamp(100.0 * SmoothMath.sigmoid(c.phiExergy(), 1.6, 0.4), 0.0, 100.0);
            case "offspringPct" -> Math.clamp(85.0 - 35.0 * SmoothMath.sigmoid(c.phiExergy(), 2.2, 0.4), 35.0, 95.0);
            case "ageFirstChild" -> Math.clamp(18.0 + 13.0 * SmoothMath.sigmoid(c.phiExergy(), 2.1, 0.4), 16.0, 34.0);
            case "immigrationRate" -> (c.agentBirthRate - c.agentDeathRate) * 100.0;
            case "pibMaterialFlow" -> c.popSum * 0.004 * Math.max(1.0, c.phiExergy());
            case "energyEroi" -> Math.clamp(3.0 + 18.0 * SmoothMath.sigmoid(c.phiExergy(), 1.8, 0.45), 1.5, 45.0);
            case "engineTPS" -> c.engineTps;
            case "ramMemory" -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0);
            case "cellCount" -> (double) c.capacity;
            default -> 0.0;
        };
    }
}
