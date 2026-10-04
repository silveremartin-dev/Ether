/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PlanetaryBoundariesEngine;
import org.ether.society.engines.tier2.theories.CliodynamicAdvisorEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Automated Cliodynamic Simulation Report Generator.
 * Compiles planetary telemetry, thermodynamic boundaries, and event logs into a Markdown/HTML report.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SimulationReportGenerator {

    public static File generateReport(H3SimulationEngine engine, File outputFile) throws IOException {
        if (outputFile == null) {
            outputFile = new File("ether_cliodynamic_report_" + System.currentTimeMillis() + ".md");
        }

        long year = engine != null ? engine.getTimeManager().getCurrentYear() : 2026;
        var cells = engine != null ? engine.getCells() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("# ðŸŒ Ether - Rapport d'Ã‰valuation Cliodynamique & Thermodynamique\n\n");
        sb.append("**AnnÃ©e de Simulation :** ").append(year).append("\n");
        sb.append("**Nombre de Cellules Actives :** ").append(cells != null ? cells.size() : 0).append("\n\n");

        sb.append("--- \n\n");
        sb.append("## ðŸ“Š Ã‰valuation des 9 Limites PlanÃ©taires\n\n");

        var boundaries = PlanetaryBoundariesEngine.assessBoundaries(cells);
        sb.append(String.format("- **ForÃ§age Climatique** : %.1f%%\n", boundaries.climateChangeRisk() * 100));
        sb.append(String.format("- **IntÃ©gritÃ© de la BiosphÃ¨re** : %.1f%%\n", boundaries.biosphereIntegrityRisk() * 100));
        sb.append(String.format("- **Ã‰puisement Eau Douce** : %.1f%%\n", boundaries.freshwaterDepletionRisk() * 100));
        sb.append(String.format("- **Cycle N-P-K & Carbone** : %.1f%%\n", boundaries.biogeochemicalNPKRisk() * 100));
        sb.append(String.format("- **AÃ©rosols StratosphÃ©riques** : %.1f%%\n\n", boundaries.atmosphericAerosolRisk() * 100));

        sb.append("## ðŸ§  Conseiller Cliodynamique & Alertes Physique\n\n");
        var alerts = CliodynamicAdvisorEngine.generateAdvisorAlerts(cells, year);
        for (var alert : alerts) {
            sb.append("### [").append(alert.severity()).append("] ").append(alert.title()).append("\n");
            sb.append(alert.description()).append("\n\n");
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(sb.toString());
        }

        return outputFile;
    }
}


