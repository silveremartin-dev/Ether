/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import org.ether.society.model.CulturalEntity;
import org.ether.society.ui.CulturalEpochRegistryLoader;

import java.util.List;

/**
 * <h1>Telemetry Cultural Matrix Evaluator</h1>
 *
 * <p>Evaluates bilateral cross-entity cultural affinity matrices and polarization indices
 * for subscribed telemetry ticks, including delta divergence analysis against baseline epochs.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CompiledTelemetryPlan
 * @see TelemetrySubscription.CulturalMatrixQuery
 */
final class TelemetryCulturalMatrixHelper {

    private TelemetryCulturalMatrixHelper() {
        // Pure stateless utility
    }

    /**
     * Evaluates the cultural affinity matrix for a specified year according to the query parameters.
     *
     * @param q    cultural matrix query specification
     * @param year current simulation year
     * @return evaluated cultural matrix result, or {@code null} if query is disabled
     */
    static TelemetryEvaluationResult.CulturalMatrixResult evaluate(
            TelemetrySubscription.CulturalMatrixQuery q,
            long year) {
        if (q == null || !q.enabled()) {
            return null;
        }
        String planet = "earth";
        List<CulturalEntity> entities = CulturalEpochRegistryLoader.loadEpochRegistry(planet, year);
        if (entities.isEmpty()) {
            entities = CulturalEpochRegistryLoader.loadEpochRegistry(year);
        }
        int n = entities.size();
        if (n == 0) {
            return new TelemetryEvaluationResult.CulturalMatrixResult(
                    year, planet, List.of(), new double[0][0], new double[0][0], 0.5, 0.0);
        }

        List<CulturalEntity> baselineEntities = entities;
        if (q.computeDelta()) {
            baselineEntities = CulturalEpochRegistryLoader.loadEpochRegistry(planet, q.baselineEpoch());
            if (baselineEntities.isEmpty()) {
                baselineEntities = CulturalEpochRegistryLoader.loadEpochRegistry(q.baselineEpoch());
            }
            if (baselineEntities.isEmpty()) {
                baselineEntities = entities;
            }
        }

        double[][] affinities = new double[n][n];
        double[][] deltas = q.computeDelta() ? new double[n][n] : new double[0][0];

        double sumAffinity = 0.0;
        double sumSqDiff = 0.0;
        int count = n * n;

        for (int i = 0; i < n; i++) {
            CulturalEntity eOrig = entities.get(i);
            for (int j = 0; j < n; j++) {
                CulturalEntity eTarg = entities.get(j);
                double affCurrent = eOrig.affinityTowards(eTarg);
                affinities[i][j] = affCurrent;

                sumAffinity += affCurrent;
                sumSqDiff += (affCurrent - 0.5) * (affCurrent - 0.5);

                if (q.computeDelta()) {
                    CulturalEntity bOrig = findEntityById(baselineEntities, eOrig.id());
                    CulturalEntity bTarg = findEntityById(baselineEntities, eTarg.id());
                    double affT0 = (bOrig != null && bTarg != null) ? bOrig.affinityTowards(bTarg) : affCurrent;
                    deltas[i][j] = affCurrent - affT0;
                }
            }
        }

        double mean = count > 0 ? sumAffinity / count : 0.5;
        double variance = count > 0 ? sumSqDiff / count : 0.0;
        double polarization = Math.sqrt(variance) * 2.0;

        return new TelemetryEvaluationResult.CulturalMatrixResult(
                year, planet, entities, affinities, deltas, mean, polarization);
    }

    private static CulturalEntity findEntityById(List<CulturalEntity> list, String id) {
        if (id == null || list == null) return null;
        for (CulturalEntity e : list) {
            if (id.equals(e.id())) return e;
        }
        return null;
    }
}
