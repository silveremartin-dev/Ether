/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.MetricDescriptor;
import org.ether.society.analytics.MetricRegistry;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.i18n.I18n;
import org.ether.society.analytics.GroundTruthAuditGenerator;
import org.ether.society.ui.WindowUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 1D Chronological Multi-Scenario Time Series Chart Coordinator.
 * 
 * <p>Manages multi-curve visualization, spatial and temporal disjoint warnings,
 * unit formatting, tooltips, and interactive cursor-centered zoom and panning.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ComparativeTimeSeriesChartCoordinator {

    private ComparativeTimeSeriesChartCoordinator() {
        // Static utility class
    }

    /**
     * Updates the chronological chart with multi-scenario series curves.
     */
    public static void updateChartAndAnalysis(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis,
            List<ScenarioSelectableItem> scenarioList,
            ComboBox<String> metricSelectorCombo,
            ComboBox<HistoricalValidationKernel.InterpolationMethod> interpolationCombo,
            HBox timeSeriesWarningBox,
            Label timeSeriesWarningLabel,
            Function<ScenarioSelectableItem, SimulationRunRecord> runResolver,
            boolean isUpdatingTexts) {

        if (isUpdatingTexts || chart == null) return;
        chart.getData().clear();

        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        String metric = metricSelectorCombo != null ? metricSelectorCombo.getValue() : null;
        if (metric == null || metric.startsWith("──") || selectedExecuted.isEmpty()) return;

        // 1. Spatial Intersection & Disjoint Analysis across selected items
        double interMinLat = -90.0, interMaxLat = 90.0, interMinLng = -180.0, interMaxLng = 180.0;
        boolean hasClipped = false;
        String firstPlanet = null;
        boolean differentPlanets = false;

        for (ScenarioSelectableItem item : selectedExecuted) {
            String planet = item.getPlanetDisplayName();
            if (firstPlanet == null) {
                firstPlanet = planet;
            } else if (!firstPlanet.equalsIgnoreCase(planet)) {
                differentPlanets = true;
            }
            if (item.isClipped()) {
                hasClipped = true;
                interMinLat = Math.max(interMinLat, item.getMinLat());
                interMaxLat = Math.min(interMaxLat, item.getMaxLat());
                interMinLng = Math.max(interMinLng, item.getMinLng());
                interMaxLng = Math.min(interMaxLng, item.getMaxLng());
            }
        }
        boolean spatialDisjoint = differentPlanets || (hasClipped && (interMinLat >= interMaxLat || interMinLng >= interMaxLng));

        // 2. Chronological Intersection & Disjoint Analysis across selected items
        int interStartYear = Integer.MIN_VALUE;
        int interEndYear = Integer.MAX_VALUE;
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        boolean hasSimulatedScenarios = false;

        for (ScenarioSelectableItem other : selectedExecuted) {
            if (!other.isExternalBaseline() && other.getScenario() != null) {
                int sStart = (int) other.getScenario().getStartDateYear();
                int sEnd = (int) other.getScenario().getEndDateYear();
                minYear = Math.min(minYear, sStart);
                maxYear = Math.max(maxYear, sEnd);
                interStartYear = Math.max(interStartYear, sStart);
                interEndYear = Math.min(interEndYear, sEnd);
                hasSimulatedScenarios = true;
            }
        }

        if (!hasSimulatedScenarios) {
            boolean hasOnlyPredictive = selectedExecuted.stream().allMatch(i -> i.getRunId() != null && i.getRunId().startsWith("PREDICTIVE_"));
            if (hasOnlyPredictive && !selectedExecuted.isEmpty()) {
                minYear = 2026;
                maxYear = 2100;
            } else {
                minYear = -100000;
                maxYear = 2026;
            }
        } else {
            boolean hasPredictive = selectedExecuted.stream().anyMatch(i -> i.getRunId() != null && i.getRunId().startsWith("PREDICTIVE_"));
            if (hasPredictive) {
                maxYear = Math.max(maxYear, 2100);
            }
            if (maxYear <= minYear) {
                maxYear = minYear + 100;
            }
        }

        boolean temporalDisjoint = hasSimulatedScenarios && (interStartYear > interEndYear);
        boolean hasGroundTruth = selectedExecuted.stream().anyMatch(i -> "HISTORICAL_GROUND_TRUTH".equals(i.getRunId()));
        boolean hasPre10k = minYear < -10000;
        boolean hasClippedWithGroundTruth = hasGroundTruth && hasClipped;

        // Configure timeSeriesWarningBox & timeSeriesWarningLabel
        if (timeSeriesWarningBox != null && timeSeriesWarningLabel != null) {
            if (spatialDisjoint) {
                timeSeriesWarningLabel.setText(differentPlanets
                    ? I18n.getOrDefault("analytics.warning.different_planets", "⚠️ Scénarios sur des planètes distinctes : aucune intersection spatiale possible. Données comparatives cartographiques directes indisponibles.")
                    : I18n.getOrDefault("analytics.warning.spatially_disjoint", "⚠️ Scénarios spatialement disjoints : aucune intersection géographique possible. Données spatiales comparatives indisponibles."));
                timeSeriesWarningBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 4; -fx-padding: 6 10;");
                timeSeriesWarningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                timeSeriesWarningBox.setVisible(true);
                timeSeriesWarningBox.setManaged(true);
            } else if (temporalDisjoint) {
                timeSeriesWarningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.temporally_disjoint_fmt", "⚠️ Scénarios temporellement disjoints : aucune intersection temporelle possible (Périodes disjointes : min %d > max %d)."), interStartYear, interEndYear));
                timeSeriesWarningBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 4; -fx-padding: 6 10;");
                timeSeriesWarningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                timeSeriesWarningBox.setVisible(true);
                timeSeriesWarningBox.setManaged(true);
            } else {
                StringBuilder banner = new StringBuilder();
                if (hasClipped) {
                    banner.append(String.format(java.util.Locale.US, I18n.getOrDefault("analytics.info.spatial_intersection", "ℹ️ Intersection spatiale appliquée : [%.1f°, %.1f° Lat | %.1f°, %.1f° Lng]"), interMinLat, interMaxLat, interMinLng, interMaxLng));
                }
                if (hasGroundTruth && hasPre10k) {
                    if (banner.length() > 0) banner.append(" | ");
                    banner.append(I18n.getOrDefault("analytics.warning.pre_10k_data", "⚠️ Données historiques réelles peu fiables ou estimées avant -10 000 BP (estimations macroscopiques HYDE / paléoanthropologie)."));
                }
                if (hasClippedWithGroundTruth && (metric != null && (metric.toLowerCase().contains("pop") || metric.toLowerCase().contains("gdp") || metric.toLowerCase().contains("pib")))) {
                    if (banner.length() > 0) banner.append(" | ");
                    banner.append(I18n.getOrDefault("analytics.warning.global_ground_truth_clipped", "⚠️ Référence historique mondiale : la trajectoire HYDE/Maddison est globale et n'est pas restreinte au fenêtrage géographique de ce scénario."));
                }
                if (banner.length() > 0) {
                    timeSeriesWarningLabel.setText(banner.toString());
                    timeSeriesWarningBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-border-color: #f59e0b; -fx-border-radius: 4; -fx-padding: 6 10;");
                    timeSeriesWarningLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
                    timeSeriesWarningBox.setVisible(true);
                    timeSeriesWarningBox.setManaged(true);
                } else {
                    timeSeriesWarningBox.setVisible(false);
                    timeSeriesWarningBox.setManaged(false);
                }
            }
        }

        setXAxisRange(xAxis, minYear, maxYear);

        HistoricalValidationKernel.InterpolationMethod interpMethod = (interpolationCombo != null && interpolationCombo.getValue() != null)
            ? interpolationCombo.getValue()
            : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;

        MetricDescriptor metricDesc = MetricRegistry.getInstance().getDescriptorByName(metric);
        if (metricDesc == null) {
            metricDesc = MetricRegistry.getInstance().getDescriptor(metric);
        }

        String unitStr = (metricDesc != null && metricDesc.getUnit() != null && !metricDesc.getUnit().isBlank())
            ? " (" + metricDesc.getUnit() + ")" : "";
        String metricDisplayName = (metricDesc != null) ? metricDesc.getDisplayName() : metric;
        if (yAxis != null) {
            yAxis.setLabel(metricDisplayName + unitStr);
            String tipText = String.format("%s\n%s: %s\n%s: %s\n%s: %s",
                metricDisplayName,
                I18n.getOrDefault("analytics.metric.category", "Catégorie"),
                metricDesc != null && metricDesc.getCategory() != null ? metricDesc.getCategory() : "Société & Macroéconomie",
                I18n.getOrDefault("analytics.metric.unit", "Unité"),
                metricDesc != null && metricDesc.getUnit() != null && !metricDesc.getUnit().isBlank() ? metricDesc.getUnit() : "Scalaire sans dimension",
                I18n.getOrDefault("analytics.metric.desc", "Description"),
                metricDesc != null && metricDesc.getDescription() != null ? metricDesc.getDescription() : "Mesure de dynamique macroscopique et historique."
            );
            Tooltip yTip = new Tooltip(tipText);
            yTip.setShowDelay(Duration.millis(100));
            Tooltip.install(yAxis, yTip);
        }

        for (ScenarioSelectableItem item : selectedExecuted) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(item.getName());

            if ("HISTORICAL_GROUND_TRUTH".equals(item.getRunId())) {
                String benchKey = mapMetricToBenchmarkKey(metric);
                int groundTruthMin = Math.max(-100000, minYear);
                int groundTruthMax = Math.min(2026, maxYear);

                if (groundTruthMin <= groundTruthMax) {
                    int step = Math.max(1, (groundTruthMax - groundTruthMin) / 100);
                    for (int yr = groundTruthMin; yr <= groundTruthMax; yr += step) {
                        double val = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, interpMethod);
                        double scaledVal = scaleBenchmarkValue(benchKey, metric, val);
                        addChartDataPoint(series, yr, scaledVal, unitStr);
                    }
                    if ((groundTruthMax - groundTruthMin) % step != 0) {
                        double valEnd = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, groundTruthMax, interpMethod);
                        double scaledValEnd = scaleBenchmarkValue(benchKey, metric, valEnd);
                        addChartDataPoint(series, groundTruthMax, scaledValEnd, unitStr);
                    }
                }
            } else if (item.getRunId() != null && item.getRunId().startsWith("PREDICTIVE_")) {
                String benchKey = mapMetricToBenchmarkKey(metric);
                int predMin = Math.max(2026, minYear);
                int predMax = Math.min(2100, maxYear);

                if (predMin <= predMax) {
                    int step = Math.max(1, (predMax - predMin) / 50);
                    for (int yr = predMin; yr <= predMax; yr += step) {
                        double val = HistoricalValidationKernel.getPredictiveBenchmarkValue(item.getRunId(), benchKey, yr, interpMethod);
                        double scaledVal = scaleBenchmarkValue(benchKey, metric, val);
                        addChartDataPoint(series, yr, scaledVal, unitStr);
                    }
                    if ((predMax - predMin) % step != 0) {
                        double valEnd = HistoricalValidationKernel.getPredictiveBenchmarkValue(item.getRunId(), benchKey, predMax, interpMethod);
                        double scaledValEnd = scaleBenchmarkValue(benchKey, metric, valEnd);
                        addChartDataPoint(series, predMax, scaledValEnd, unitStr);
                    }
                }
            } else {
                SimulationRunRecord record = runResolver.apply(item);
                if (record != null && !record.getTimeSeriesData().isEmpty()) {
                    var dataMap = record.getTimeSeriesData();
                    List<Integer> sortedYears = new ArrayList<>(dataMap.keySet());
                    Collections.sort(sortedYears);

                    for (int year : sortedYears) {
                        if (year >= minYear && year <= maxYear) {
                            addChartDataPoint(series, year, GroundTruthAuditGenerator.extractValue(dataMap.get(year), metric, year), unitStr);
                        }
                    }
                }
            }
            chart.getData().add(series);
        }
    }

    public static void setXAxisRange(NumberAxis xAxis, double lower, double upper) {
        if (xAxis == null) return;
        if (upper <= lower) upper = lower + 10.0;
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(lower);
        xAxis.setUpperBound(upper);
        double range = upper - lower;
        double tickUnit = WindowUtils.computeNiceTickUnit(range);
        xAxis.setTickUnit(tickUnit);
    }

    public static void addChartDataPoint(XYChart.Series<Number, Number> series, double year, double value, String unitStr) {
        XYChart.Data<Number, Number> dp = new XYChart.Data<>(year, value);
        dp.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                long yr = Math.round(dp.getXValue().doubleValue());
                long absY = Math.abs(yr);
                boolean isFr = I18n.getCurrentLanguage() == org.ether.society.i18n.Language.FRENCH;
                String yrStr;
                if (yr < 0) {
                    yrStr = isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                } else if (yr > 0) {
                    yrStr = isFr ? String.format("%,d ap. J.-C.", yr) : String.format("%,d AD", yr);
                } else {
                    yrStr = isFr ? "An 0" : "0 AD";
                }
                String tip = String.format("%s\n📅 %s\n📈 %,.2f%s", series.getName(), yrStr, dp.getYValue().doubleValue(), unitStr != null ? unitStr : "");
                Tooltip tooltip = new Tooltip(tip);
                tooltip.setShowDelay(Duration.millis(50));
                Tooltip.install(newNode, tooltip);
                newNode.setCursor(javafx.scene.Cursor.HAND);
            }
        });
        series.getData().add(dp);
    }

    public static double scaleBenchmarkValue(String benchKey, String metric, double rawVal) {
        if ("worldPopulation".equals(benchKey)) {
            return rawVal * 1_000_000.0;
        }
        if ("grossWorldProduct".equals(benchKey)) {
            return rawVal;
        }
        if ("primaryEnergy".equals(benchKey)) {
            return rawVal;
        }
        if ("temperatureAnomaly".equals(benchKey)) {
            if (metric != null && (metric.contains("Moyenne") || metric.equalsIgnoreCase("temperature"))) {
                return 14.0 + rawVal;
            }
            return rawVal;
        }
        if ("agriculturalEroei".equals(benchKey) || "zeroContainmentScore".equals(benchKey)) {
            return rawVal;
        }
        return rawVal;
    }

    public static String mapMetricToBenchmarkKey(String metric) {
        if (metric == null) return "worldPopulation";
        String lower = metric.toLowerCase();
        if (lower.contains("populat") || lower.contains("densit")) return "worldPopulation";
        if (lower.contains("richesse") || lower.contains("gdp") || lower.contains("pib") || lower.contains("capital") || lower.contains("product")) return "grossWorldProduct";
        if (lower.contains("alimentaire") || lower.contains("food") || lower.contains("eroi")) return "agriculturalEroei";
        if (lower.contains("énergie") || lower.contains("energy") || lower.contains("puissance")) return "primaryEnergy";
        if (lower.contains("survie") || lower.contains("urbanis") || lower.contains("urban")) return "urbanizationRate";
        if (lower.contains("anomal") || lower.contains("temperatureanomaly")) return "temperatureAnomaly";
        if (lower.contains("température") || lower.contains("temperature") || lower.contains("climat")) return "temperatureAnomaly";
        if (lower.contains("précipit") || lower.contains("precipit") || lower.contains("pluie") || lower.contains("rainfall")) return "precipitation";
        if (lower.contains("co2") || lower.contains("carbon") || lower.contains("carbone")) return "co2Concentration";
        if (lower.contains("technolog") || lower.contains("tech") || lower.contains("alphabét") || lower.contains("literacy") || lower.contains("éducat")) return "literacyRate";
        if (lower.contains("asabiyyah") || lower.contains("cohésion")) return "asabiyyahSocialCohesion";
        if (lower.contains("stabilité") || lower.contains("monnaie") || lower.contains("currency") || lower.contains("instability") || lower.contains("conflit")) return "sociopoliticalInstability";
        if (lower.contains("gini") || lower.contains("inégal")) return "giniInequality";
        if (lower.contains("megafauna") || lower.contains("mégafaune")) return "megafaunaIndex";
        if (lower.contains("milankovitch") || lower.contains("insolation")) return "milankovitchInsolation";
        if (lower.contains("containment") || lower.contains("confinement")) return "zeroContainmentScore";
        if (lower.contains("vie") || lower.contains("life") || lower.contains("espérance")) return "lifeExpectancyAtBirth";
        if (lower.contains("métal") || lower.contains("metal") || lower.contains("smelting")) return "metalSmeltingPerCapita";
        if (lower.contains("commerce") || lower.contains("trade")) return "globalTradeVolume";
        if (lower.contains("dette") || lower.contains("debt")) return "sovereignDebtBurden";
        return "worldPopulation";
    }
}
