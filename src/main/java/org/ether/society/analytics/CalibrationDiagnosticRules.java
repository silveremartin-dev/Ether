/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.i18n.I18n;

import java.util.Map;

/**
 * Data-driven configuration rule engine for calibration diagnostic suggestions.
 * Analyzes empirical MAPE deviations and R² fit against historical ground truth benchmarks
 * to generate actionable parameter tuning recommendations for simulation engines.
 */
public class CalibrationDiagnosticRules {

    /*
     * Generate tuning suggestions.
     * Enforces physical invariants and updates associated state variables within {@code CalibrationDiagnosticRules}.
     *
     * @param mapes the mapes parameter (Double>)
     * @param rSquared the r squared parameter (double)
     * @param divergenceYear the divergence year parameter (int)
     * @return the resulting computation or state reference
     */
    public static String generateTuningSuggestions(Map<String, Double> mapes, double rSquared, int divergenceYear) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🛠️ 3. ").append(I18n.getOrDefault("analytics.tuning.title", "Engine Parameter Calibration Tracks (Data-Driven Dynamic Analysis)")).append("\n\n");

        if (rSquared >= 0.95 && mapes.values().stream().allMatch(m -> m < 15.0)) {
            sb.append("✅ **").append(I18n.getOrDefault("analytics.tuning.optimal_model", "Optimally Calibrated Model")).append("** :\n");
            sb.append("   - ").append(I18n.getOrDefault("analytics.tuning.optimal_desc", "Simulated trajectories are remarkably aligned with historical reality ($R^2 = "))
              .append(String.format("%.4f", rSquared))
              .append("$, ").append(I18n.getOrDefault("analytics.tuning.no_revision", "No major revision of engine constants is required.")).append(")\n\n");
            return sb.toString();
        }

        int ruleCount = 1;

        // Rule 1: Demographic Engine
        Double popMape = getMapeForKeyword(mapes, "Population");
        if (popMape != null && popMape > 15.0) {
            sb.append(String.format("%d. **%s (`DemographicEngine`)** [MAPE = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.demographic_engine", "Demographic Engine & Carrying Capacity"),
                popMape));
            if (popMape > 35.0) {
                sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
                  .append(I18n.getOrDefault("analytics.tuning.demographic_high_diag", "Large discrepancy in population growth rate. Carrying capacity saturates too early or net growth rate overestimates mortality.")).append("\n");
                sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
                  .append(I18n.getOrDefault("analytics.tuning.demographic_high_action", "Increase `carryingCapacityScale` by 1.15x and adjust `agricultural_spread_rate` from 0.010 to 0.018/yr.")).append("\n\n");
            } else {
                sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
                  .append(I18n.getOrDefault("analytics.tuning.demographic_low_diag", "Mild logistic drift in population across epoch transitions.")).append("\n");
                sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
                  .append(I18n.getOrDefault("analytics.tuning.demographic_low_action", "Refine cohort grouping thresholds `targetCohortSize` and calibrate residual baseline fertility.")).append("\n\n");
            }
        }

        // Rule 2: Ecology & Resource Engine
        Double energyMape = getMapeForKeyword(mapes, "Energy");
        if (energyMape == null) energyMape = getMapeForKeyword(mapes, "Énergie");
        Double co2Mape = getMapeForKeyword(mapes, "CO2");
        if ((energyMape != null && energyMape > 15.0) || (co2Mape != null && co2Mape > 15.0)) {
            double maxEco = Math.max(energyMape != null ? energyMape : 0, co2Mape != null ? co2Mape : 0);
            sb.append(String.format("%d. **%s (`EcologyEngine`)** [MAPE max = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.ecology_engine", "Ecological Engine & Carbon Footprint"),
                maxEco));
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.ecology_diag", "Discrepancy in primary energy consumption and greenhouse gas emission kinetics.")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.ecology_action", "Recalibrate energy extraction coefficient `alpha_burn` (from 0.040 to 0.028) and reduce per-capita biomass burn `wood_consumption_per_capita`.")).append("\n\n");
        }

        // Rule 3: Economic & Capital Engine (GWP)
        Double gwpMape = getMapeForKeyword(mapes, "Product");
        if (gwpMape == null) gwpMape = getMapeForKeyword(mapes, "Produit");
        if (gwpMape == null) gwpMape = getMapeForKeyword(mapes, "GWP");
        if (gwpMape != null && gwpMape > 15.0) {
            sb.append(String.format("%d. **%s (`SociologyEngine / Capital`)** [MAPE = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.economy_engine", "Economic Engine & Capital Stock"),
                gwpMape));
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.economy_diag", "Gross World Product (GWP) deviates from historical capital stock yield ($K_0$).")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.economy_action", "Adjust capital output elasticity `alpha_k` (e.g. 0.33) and increase initial knowledge stock `initialInformationPerCapita`.")).append("\n\n");
        }

        // Rule 4: Settlement & Urbanization Engine
        Double urbanMape = getMapeForKeyword(mapes, "Urban");
        if (urbanMape == null) urbanMape = getMapeForKeyword(mapes, "Urbanisation");
        if (urbanMape != null && urbanMape > 15.0) {
            sb.append(String.format("%d. **%s (`SettlementEngine`)** [MAPE = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.settlement_engine", "Settlement Engine & Urban Concentration"),
                urbanMape));
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.settlement_diag", "Urbanization rate deviates from HYDE historical agglomeration benchmark series.")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.settlement_action", "Adjust urban cluster migration rate `urban_migration_rate` and check H3 critical demographic threshold.")).append("\n\n");
        }

        // Rule 5: Cultural & Educational Engine (Literacy)
        Double literacyMape = getMapeForKeyword(mapes, "Literacy");
        if (literacyMape == null) literacyMape = getMapeForKeyword(mapes, "Alphabétisation");
        if (literacyMape != null && literacyMape > 15.0) {
            sb.append(String.format("%d. **%s (`CulturalSociologyEngine`)** [MAPE = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.culture_engine", "Sociocultural Engine & Information Diffusion"),
                literacyMape));
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.culture_diag", "Propagation speed of recorded knowledge and literacy deviates from historical benchmarks.")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.culture_action", "Increase knowledge retention rate `information_retention_rate` and decrease intergenerational transmission loss.")).append("\n\n");
        }

        // Rule 6: Institutional & Monetary Engine
        Double currencyMape = getMapeForKeyword(mapes, "Monetary");
        if (currencyMape == null) currencyMape = getMapeForKeyword(mapes, "Monétaire");
        if (currencyMape == null) currencyMape = getMapeForKeyword(mapes, "Currency");
        if (currencyMape != null && currencyMape > 15.0) {
            sb.append(String.format("%d. **%s (`InstitutionalEngine`)** [MAPE = %.1f%%] :\n",
                ruleCount++,
                I18n.getOrDefault("analytics.tuning.inst_engine", "Institutional Engine & Monetary Stability"),
                currencyMape));
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.inst_diag", "Trajectory of currency debasement or political instability shows asynchronous shifts.")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.inst_action", "Dampen fiscal shock sensitivity and moderate the decay rate of social cohesion (Asabiyyah).")).append("\n\n");
        }

        if (ruleCount == 1) {
            sb.append("1. **").append(I18n.getOrDefault("analytics.tuning.general_adjust", "General Engine Adjustments")).append("** :\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.diag_label", "Diagnostic")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.general_diag", "Mild secondary drifts dispersed across subsidiary indicators.")).append("\n");
            sb.append("   - *").append(I18n.getOrDefault("analytics.tuning.action_label", "Recommended Actions")).append("* : ")
              .append(I18n.getOrDefault("analytics.tuning.general_action", "Run auto-calibrator `HistoricalAutoCalibrator.evaluateAndAutoCalibrate()` to refine Type B module selection.")).append("\n\n");
        }

        return sb.toString();
    }

    // Helper subroutine: get mape for keyword - internal state computation & bounds checking
    private static Double getMapeForKeyword(Map<String, Double> mapes, String keyword) {
        if (mapes == null || keyword == null) return null;
        for (var entry : mapes.entrySet()) {
            if (entry.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
