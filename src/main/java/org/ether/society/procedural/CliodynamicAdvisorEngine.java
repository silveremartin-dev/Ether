/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Predictive Cliodynamic & Thermodynamic Advisor Engine.
 * Analyzes current cell telemetry to produce real-time physical warnings and advice.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class CliodynamicAdvisorEngine {
    private static final Logger logger = LoggerFactory.getLogger(CliodynamicAdvisorEngine.class);

    public record CliodynamicAlert(String severity, String title, String description) {}

    public static List<CliodynamicAlert> generateAdvisorAlerts(List<H3Cell> cells, long currentYear) {
        List<CliodynamicAlert> alerts = new ArrayList<>();
        if (cells == null || cells.isEmpty()) return alerts;

        PlanetaryBoundariesEngine.BoundaryStatus boundaries = PlanetaryBoundariesEngine.assessBoundaries(cells);

        if (boundaries.climateChangeRisk() > 0.6) {
            alerts.add(new CliodynamicAlert("CRITICAL", I18n.getOrDefault("advisor.alert.climate_forcing.title", "🔥 MAJOR CLIMATE FORCING ALERT"),
                I18n.getOrDefault("advisor.alert.climate_forcing.desc", "Average warming exceeds critical threshold (+3.5°C). Risk of desertification in temperate zones.")));
        }

        if (boundaries.freshwaterDepletionRisk() > 0.5) {
            alerts.add(new CliodynamicAlert("WARNING", I18n.getOrDefault("advisor.alert.aquifer_depletion.title", "💧 AQUIFER DEPLETION ALERT"),
                I18n.getOrDefault("advisor.alert.aquifer_depletion.desc", "Accessible freshwater reserves collapsing beneath major urban nodes.")));
        }

        if (boundaries.biogeochemicalNPKRisk() > 0.5) {
            alerts.add(new CliodynamicAlert("WARNING", I18n.getOrDefault("advisor.alert.npk_depletion.title", "🌾 CARBON & N-P-K DEPLETION ALERT"),
                I18n.getOrDefault("advisor.alert.npk_depletion.desc", "Soil organic carbon decline threatens global agricultural carrying capacity.")));
        }

        if (NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() > 0.5) {
            alerts.add(new CliodynamicAlert("CRITICAL", I18n.getOrDefault("advisor.alert.nuclear_winter.title", "❄️ NUCLEAR / VOLCANIC WINTER ALERT"),
                I18n.getOrDefault("advisor.alert.nuclear_winter.desc", "Stratospheric aerosol optical depth causing abrupt cooling and photosynthesis reduction.")));
        }

        if (alerts.isEmpty()) {
            alerts.add(new CliodynamicAlert("INFO", I18n.getOrDefault("advisor.alert.stable_equilibrium.title", "✅ STABLE THERMODYNAMIC EQUILIBRIUM"),
                I18n.getOrDefault("advisor.alert.stable_equilibrium.desc", "Energy fluxes and resource stocks remain within planetary resilience boundaries.")));
        }

        return alerts;
    }
}

