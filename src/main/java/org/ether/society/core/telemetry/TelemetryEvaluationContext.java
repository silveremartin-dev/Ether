/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import org.ether.society.database.H3CellPhysicsAndStateHelper;

/**
 * <h1>Telemetry Evaluation Context</h1>
 *
 * <p>Immutable per-tick physical and demographic observables context aggregated across
 * simulation meshes, polities, and agent SoA arrays. Passed cleanly to memoized DAG evaluators.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CompiledTelemetryPlan
 * @see TelemetryDagNodeEvaluator
 */
public final class TelemetryEvaluationContext {

    final int capacity;
    final double popSum;
    final long popLong;
    final double foodSum;
    final double foodPerCapita;
    final double waterSum;
    final double bioNatSum;
    final double bioAgrSum;
    final double bioLivSum;
    final double builtCapSum;
    final double energyCapturedMw;
    final double currentNonRenRes;
    final int landCellCount;
    final double landTempSum;
    final int populatedCount;
    final double urbanPop;
    final double weightedSoilOrg;
    final int arableCells;
    final float gini;
    final float gdp;
    final float lifeExp;
    final float fertility;
    final double agentBirthRate;
    final double agentDeathRate;
    final int polityCount;
    final int cityStatesCount;
    final double avgAsabiyyahPct;
    final double meanPolityInstab;
    final double meanEliteOverprodPolity;
    final long largestUnitSize;
    final int maxHierarchy;
    final long initialPop;
    final double t0Co2Ppm;
    final double elapsedYears;
    final double engineTps;

    private Double cachedPhiExergy;

    public TelemetryEvaluationContext(int capacity, double popSum, long popLong, double foodSum, double foodPerCapita,
                                      double waterSum, double bioNatSum, double bioAgrSum, double bioLivSum,
                                      double builtCapSum, double energyCapturedMw, double currentNonRenRes,
                                      int landCellCount, double landTempSum, int populatedCount, double urbanPop,
                                      double weightedSoilOrg, int arableCells, float gini, float gdp,
                                      float lifeExp, float fertility, double agentBirthRate, double agentDeathRate,
                                      int polityCount, int cityStatesCount, double avgAsabiyyahPct, double meanPolityInstab,
                                      double meanEliteOverprodPolity, long largestUnitSize, int maxHierarchy,
                                      long initialPop, double t0Co2Ppm, double elapsedYears, double engineTps) {
        this.capacity = capacity;
        this.popSum = popSum;
        this.popLong = popLong;
        this.foodSum = foodSum;
        this.foodPerCapita = foodPerCapita;
        this.waterSum = waterSum;
        this.bioNatSum = bioNatSum;
        this.bioAgrSum = bioAgrSum;
        this.bioLivSum = bioLivSum;
        this.builtCapSum = builtCapSum;
        this.energyCapturedMw = energyCapturedMw;
        this.currentNonRenRes = currentNonRenRes;
        this.landCellCount = landCellCount;
        this.landTempSum = landTempSum;
        this.populatedCount = populatedCount;
        this.urbanPop = urbanPop;
        this.weightedSoilOrg = weightedSoilOrg;
        this.arableCells = arableCells;
        this.gini = gini;
        this.gdp = gdp;
        this.lifeExp = lifeExp;
        this.fertility = fertility;
        this.agentBirthRate = agentBirthRate;
        this.agentDeathRate = agentDeathRate;
        this.polityCount = polityCount;
        this.cityStatesCount = cityStatesCount;
        this.avgAsabiyyahPct = avgAsabiyyahPct;
        this.meanPolityInstab = meanPolityInstab;
        this.meanEliteOverprodPolity = meanEliteOverprodPolity;
        this.largestUnitSize = largestUnitSize;
        this.maxHierarchy = maxHierarchy;
        this.initialPop = initialPop;
        this.t0Co2Ppm = t0Co2Ppm;
        this.elapsedYears = elapsedYears;
        this.engineTps = engineTps;
    }

    double kpc() {
        return popSum > 0.0 ? builtCapSum / popSum : 0.0;
    }

    double powerWattsPerCapita() {
        return popSum > 0.0 ? Math.max(80.0, (energyCapturedMw * 1.0e6) / popSum) : 80.0;
    }

    double phiExergy() {
        if (cachedPhiExergy == null) {
            cachedPhiExergy = H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(
                    powerWattsPerCapita(), kpc(), 80.0);
        }
        return cachedPhiExergy;
    }
}
