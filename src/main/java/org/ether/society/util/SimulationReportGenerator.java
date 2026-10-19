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
 * @version 1.0.0-beta.2
 */
public class SimulationReportGenerator {

    /**
     * Generate report.
     *
     * @param engine the engine parameter (H3SimulationEngine)
     * @param outputFile the output file parameter (File)
     * @return the resulting computation or state reference
     */
    public static File generateReport(H3SimulationEngine engine, File outputFile) throws IOException {
        if (outputFile == null) {
            outputFile = new File("ether_cliodynamic_report_" + System.currentTimeMillis() + ".md");
        }

        long year = engine != null ? engine.getTimeManager().getCurrentYear() : 2026;
        var cells = engine != null ? engine.getCells() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("# 🌍 Ether - Cliodynamic & Thermodynamic Planetary Assessment Report\n\n");
        sb.append("**Simulation Year:** ").append(year).append("\n");
        sb.append("**Active Cells Count:** ").append(cells != null ? cells.size() : 0).append("\n\n");

        sb.append("--- \n\n");
        sb.append("## 📊 Assessment of Planetary Boundaries\n\n");

        var boundaries = PlanetaryBoundariesEngine.assessBoundaries(cells);
        sb.append(String.format("- **Climate Forcing Risk**: %.1f%%\n", boundaries.climateChangeRisk() * 100));
        sb.append(String.format("- **Biosphere Integrity Risk**: %.1f%%\n", boundaries.biosphereIntegrityRisk() * 100));
        sb.append(String.format("- **Freshwater Depletion Risk**: %.1f%%\n", boundaries.freshwaterDepletionRisk() * 100));
        sb.append(String.format("- **Biogeochemical N-P-K Cycle Risk**: %.1f%%\n", boundaries.biogeochemicalNPKRisk() * 100));
        sb.append(String.format("- **Stratospheric Aerosol Risk**: %.1f%%\n\n", boundaries.atmosphericAerosolRisk() * 100));

        sb.append("## 🧠 Cliodynamic Advisor & Physical Alerts\n\n");
        var alerts = CliodynamicAdvisorEngine.generateAdvisorAlerts(cells, year);
        for (var alert : alerts) {
            sb.append("### [").append(alert.severity()).append("] ").append(alert.title()).append("\n");
            sb.append(alert.description()).append("\n\n");
        }

        java.nio.file.Files.writeString(outputFile.toPath(), sb.toString(), java.nio.charset.StandardCharsets.UTF_8);

        return outputFile;
    }
}


