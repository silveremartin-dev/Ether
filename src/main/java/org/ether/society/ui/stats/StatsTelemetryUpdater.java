/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.SpatialHeatmapPanel;
import org.ether.society.ui.VarianceDistributionPanel;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stateless telemetry snapshot collector and UI card/chart synchronization helper
 * extracted from {@link org.ether.society.ui.StatsPanel} according to SRP.
 *
 * <p>Reads exclusively from {@link TelemetryEvaluationResult} produced by the compiled AST/DAG
 * pipeline on {@link H3SimulationEngine}, ensuring zero duplicate $O(N)$ buffer scans and zero
 * evaluation of unsubscribed metrics.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see org.ether.society.ui.StatsPanel
 */
public final class StatsTelemetryUpdater {

    private static final Set<String> INTEGER_KEYS = Set.of(
            "population", "cityStates", "maxHierarchy", "largestCulture", "toolsCount", "productsCount", "cellCount"
    );

    private static final Set<String> THREE_DECIMAL_KEYS = Set.of(
            "giniIndex", "landGini", "kardashevScale", "carryingCapacitySat", "planetaryOvershoot"
    );

    private StatsTelemetryUpdater() {
        // Utility class
    }

    /**
     * Synchronizes metric cards and live charts with the engine's memoized AST telemetry result.
     *
     * @param engine                   live H3 simulation engine
     * @param metricCards              map of UI metric cards keyed by metric identifier
     * @param activeTelemetryVariables subscribed variable IDs (empty means zero metrics evaluated)
     * @param chartsSection            time-series &amp; demographic pyramid view
     * @param spatialHeatmapPanel      spatial heatmap widget
     * @param variancePanel            spatial variance histogram panel
     * @param statsUpdatePending       debounce flag released on UI completion
     */
    public static void captureAndDispatchUpdate(H3SimulationEngine engine,
                                                Map<String, MetricCard> metricCards,
                                                Set<String> activeTelemetryVariables,
                                                StatsChartsSection chartsSection,
                                                SpatialHeatmapPanel spatialHeatmapPanel,
                                                VarianceDistributionPanel variancePanel,
                                                AtomicBoolean statsUpdatePending) {
        captureAndDispatchUpdate(engine, metricCards, activeTelemetryVariables, chartsSection,
                spatialHeatmapPanel, null, variancePanel, statsUpdatePending);
    }

    /**
     * Synchronizes metric cards, live charts, and dynamic cultural matrix with the engine's memoized AST telemetry result.
     *
     * @param engine                   live H3 simulation engine
     * @param metricCards              map of UI metric cards keyed by metric identifier
     * @param activeTelemetryVariables subscribed variable IDs (empty means zero metrics evaluated)
     * @param chartsSection            time-series &amp; demographic pyramid view
     * @param spatialHeatmapPanel      spatial heatmap widget
     * @param dynamicCulturalMatrixPanel live cultural matrix panel
     * @param variancePanel            spatial variance histogram panel
     * @param statsUpdatePending       debounce flag released on UI completion
     */
    public static void captureAndDispatchUpdate(H3SimulationEngine engine,
                                                Map<String, MetricCard> metricCards,
                                                Set<String> activeTelemetryVariables,
                                                StatsChartsSection chartsSection,
                                                SpatialHeatmapPanel spatialHeatmapPanel,
                                                DynamicCulturalMatrixPanel dynamicCulturalMatrixPanel,
                                                VarianceDistributionPanel variancePanel,
                                                AtomicBoolean statsUpdatePending) {
        if (engine == null || activeTelemetryVariables == null || activeTelemetryVariables.isEmpty()) {
            if (statsUpdatePending != null) {
                statsUpdatePending.set(false);
            }
            return;
        }

        // Reuse memoized DAG telemetry evaluated during the tick, or evaluate once on-demand if subscribed
        TelemetryEvaluationResult res = engine.getCachedTelemetryResult();
        if (res == null || res.isEmpty() || engine.getLastTelemetryTick() != engine.getTickCounter()) {
            if (engine.getTelemetrySubscription().isEmpty()) {
                Set<String> subscribed = new LinkedHashSet<>(activeTelemetryVariables);
                subscribed.add("agePyramid");

                java.util.List<TelemetrySubscription.SpatialRasterQuery> spatialQueries = new java.util.ArrayList<>();
                if (activeTelemetryVariables.contains("spatialMap2D") || activeTelemetryVariables.contains("spatialMap")) {
                    spatialQueries.add(TelemetrySubscription.SpatialRasterQuery.renderedMap(
                            "spatial_live_density", TelemetrySubscription.SpatialChannel.POPULATION_DENSITY, 128, 64));
                }

                TelemetrySubscription.CulturalMatrixQuery culturalQuery = TelemetrySubscription.CulturalMatrixQuery.DISABLED;
                if (activeTelemetryVariables.contains("culturalMatrix") || activeTelemetryVariables.contains("culturalAffinityMatrix")) {
                    long startYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : 0L;
                    culturalQuery = TelemetrySubscription.CulturalMatrixQuery.withDelta(startYear);
                }

                engine.setTelemetrySubscription(new TelemetrySubscription(subscribed, spatialQueries, culturalQuery, 1));
            }
            res = engine.evaluateTelemetryNow();
        }

        final TelemetryEvaluationResult eval = res != null ? res : TelemetryEvaluationResult.EMPTY;
        final Map<String, Double> metrics = eval.getMetrics();
        final int year = engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 0;
        final int month = engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        final double currentTime = year + (month / 12.0);
        final long minYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : 0L;

        final boolean spatialSubscribed = activeTelemetryVariables.contains("spatialMap2D")
                || activeTelemetryVariables.contains("spatialMap")
                || (engine.getTelemetrySubscription() != null && engine.getTelemetrySubscription().isSpatialSubscribed());
        final boolean culturalSubscribed = activeTelemetryVariables.contains("culturalMatrix")
                || activeTelemetryVariables.contains("culturalAffinityMatrix")
                || (engine.getTelemetrySubscription() != null && engine.getTelemetrySubscription().isCulturalMatrixSubscribed());

        Platform.runLater(() -> {
            try {
                if (spatialHeatmapPanel != null && spatialSubscribed) {
                    spatialHeatmapPanel.setHistoryManager(engine.getHistoryManager());
                    spatialHeatmapPanel.updateCells(engine.getCells());
                    spatialHeatmapPanel.setDateLabel(String.format("%d", year));
                }

                if (dynamicCulturalMatrixPanel != null) {
                    if (culturalSubscribed) {
                        dynamicCulturalMatrixPanel.updateState(engine, year, eval.getCulturalMatrixResult());
                    } else {
                        dynamicCulturalMatrixPanel.showUnsubscribedState();
                    }
                }

                String disabledLabel = I18n.getOrDefault("stats.disabled", "- (Disabled)");
                if (metricCards != null) {
                    for (Map.Entry<String, MetricCard> entry : metricCards.entrySet()) {
                        String key = entry.getKey();
                        MetricCard card = entry.getValue();
                        if (card == null) continue;

                        if (!activeTelemetryVariables.contains(key) || !metrics.containsKey(key)) {
                            card.updateValue(disabledLabel, 0.0);
                            card.setOpacity(0.4);
                            continue;
                        }

                        double rawVal = metrics.get(key);
                        double displayVal = "builtCapital".equals(key) && eval.getTotalPopulation() > 0
                                ? rawVal / eval.getTotalPopulation()
                                : rawVal;
                        card.setOpacity(1.0);
                        card.updateValue(formatMetricValue(key, displayVal), displayVal);
                    }
                }

                if (chartsSection != null) {
                    ComboBox<String> combo = chartsSection.getChartMetricCombo();
                    String selectedTitle = combo != null ? combo.getValue() : null;
                    double yVal = resolveChartYValue(metricCards, metrics, eval, selectedTitle);
                    chartsSection.appendTimeSeriesPoint(currentTime, yVal, minYear);
                    if (eval.getAgePyramid().length > 0) {
                        chartsSection.updateAgePyramid(eval.getAgePyramid());
                    }
                }
                if (variancePanel != null) {
                    variancePanel.updateData(engine.getCells());
                }
            } finally {
                if (statsUpdatePending != null) {
                    statsUpdatePending.set(false);
                }
            }
        });
    }

    private static double resolveChartYValue(Map<String, MetricCard> metricCards,
                                               Map<String, Double> metrics,
                                               TelemetryEvaluationResult eval,
                                               String selectedTitle) {
        if (selectedTitle != null && metricCards != null) {
            for (MetricCard card : metricCards.values()) {
                if (card != null && selectedTitle.equalsIgnoreCase(card.getTitle())) {
                    Double val = metrics.get(card.getKey());
                    if (val != null) {
                        return "builtCapital".equals(card.getKey()) && eval.getTotalPopulation() > 0
                                ? val / eval.getTotalPopulation()
                                : val;
                    }
                }
            }
            if (metrics.containsKey(selectedTitle)) {
                return metrics.get(selectedTitle);
            }
        }
        return metrics.getOrDefault("population", (double) eval.getTotalPopulation());
    }

    private static String formatMetricValue(String key, double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return "—";
        }
        if ("eroiAlim".equals(key) || "energyEroi".equals(key)) {
            return String.format(Locale.US, "%.2f : 1", val);
        }
        if ("trophicMultiplier".equals(key)) {
            return String.format(Locale.US, "%.2fx", val);
        }
        if ("ramMemory".equals(key)) {
            return String.format(Locale.US, "%,d", Math.round(val));
        }
        if (INTEGER_KEYS.contains(key)) {
            return String.format(Locale.US, "%,d", Math.round(val));
        }
        if (THREE_DECIMAL_KEYS.contains(key)) {
            return String.format(Locale.US, "%.3f", val);
        }
        if ("gdpTotal".equals(key) || "gdp".equals(key)) {
            return String.format(Locale.US, "%,.2f", val > 1e6 ? val / 1e9 : val);
        }
        if (Math.abs(val) >= 1000.0) {
            return String.format(Locale.US, "%,.0f", val);
        }
        if (Math.abs(val) < 10.0 && !"population".equals(key)) {
            return String.format(Locale.US, "%.2f", val);
        }
        return String.format(Locale.US, "%.1f", val);
    }
}
