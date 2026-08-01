/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Predictive Cliodynamic & Thermodynamic Advisor Engine.
 * Analyzes current cell telemetry to produce real-time physical warnings and advice.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class CliodynamicAdvisorEngine {
    private static final Logger logger = LoggerFactory.getLogger(CliodynamicAdvisorEngine.class);

    public record CliodynamicAlert(String severity, String title, String description) {}

    public static List<CliodynamicAlert> generateAdvisorAlerts(List<H3Cell> cells, long currentYear) {
        List<CliodynamicAlert> alerts = new ArrayList<>();
        if (cells == null || cells.isEmpty()) return alerts;

        PlanetaryBoundariesEngine.BoundaryStatus boundaries = PlanetaryBoundariesEngine.assessBoundaries(cells);

        if (boundaries.climateChangeRisk() > 0.6) {
            alerts.add(new CliodynamicAlert("CRITICAL", "🔥 ALERTE FORÇAGE CLIMATIQUE MAJEUR",
                "Le réchauffement moyen dépasse le seuil critique (+3.5°C). Risque de désertification des zones tempérées."));
        }

        if (boundaries.freshwaterDepletionRisk() > 0.5) {
            alerts.add(new CliodynamicAlert("WARNING", "💧 ALERTE ÉPUISEMENT DES NAPPES PHREATIQUES",
                "Les réserves d'eau douce accessibles s'effondrent sous les nœuds urbains majeurs."));
        }

        if (boundaries.biogeochemicalNPKRisk() > 0.5) {
            alerts.add(new CliodynamicAlert("WARNING", "🌾 ALERTE DÉPLÉTION DU CARBONE & N-P-K",
                "La baisse du carbone organique des sols menace la capacité de charge agricole mondiale."));
        }

        if (NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() > 0.5) {
            alerts.add(new CliodynamicAlert("CRITICAL", "❄️ ALERTE HIVER NUCLEAIRE / VOLCANIQUE",
                "L'épaisseur optique des aérosols stratosphériques provoque un refroidissement brusque et une baisse de photosynthèse."));
        }

        if (alerts.isEmpty()) {
            alerts.add(new CliodynamicAlert("INFO", "✅ ÉQUILIBRE THERMODYNAMIQUE STABLE",
                "Les flux d'énergie et les stocks de ressources se maintiennent dans la zone de résilience planétaire."));
        }

        return alerts;
    }
}
