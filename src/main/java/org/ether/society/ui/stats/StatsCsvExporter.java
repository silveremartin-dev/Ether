/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * CSV and Excel spreadsheet exporter for historical cliodynamic statistics.
 *
 * <p>Extracted from {@link org.ether.society.ui.StatsPanel} to satisfy the Single Responsibility Principle (SRP).
 * Exports population, energy flux, Kardashev rating, Gini inequality, GDP, life expectancy, and complexity indices.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class StatsCsvExporter {

    private StatsCsvExporter() {
        // Utility class
    }

    /**
     * Prompts the user with a save file dialog and exports the simulation metrics snapshot to CSV.
     *
     * @param engine active simulation engine
     * @param ownerWindow parent window for dialog
     */
    public static void exportCsv(H3SimulationEngine engine, Window ownerWindow) {
        if (engine == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("stats.title.export_dialog", "Exporter les Statistiques Cliodynamiques (CSV / Excel)"));
        fileChooser.setInitialFileName("ether_statistiques_cliodynamiques.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.println("Annee;Mois;Population;Biomasse;Energie_Captee;Kardashev;Gini;PIB;Bonheur;Conflits;Esperance_Vie;Fecondite;Division_Travail;Complexite");

            int y = engine.getTimeManager().getCurrentYear();
            int m = engine.getTimeManager().getCurrentMonth();
            writer.println(String.format(Locale.US, "%d;%d;%d;%.2f;%.2f;%.3f;%.4f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
                y, m, engine.getTotalPopulation(), engine.getTotalBiomassNatural(), engine.getEnergyCaptured(),
                engine.getKardashevScale(), engine.getCurrentGini(), engine.getCurrentGDP(), engine.getHappinessIndex(),
                engine.getConflictLevel(), engine.getCurrentLifeExpectancy(), engine.getCurrentFertility(),
                engine.getDivisionOfLaborIndex(), engine.getSystemComplexityIndex()));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("stats.title.export_success", "Exportation Réussie"));
            alert.setContentText("Données statistiques exportées avec succès dans :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'Exportation");
            alert.setContentText("Impossible d'exporter les statistiques : " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
