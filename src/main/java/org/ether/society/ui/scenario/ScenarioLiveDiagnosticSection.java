/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import org.ether.society.engines.compiler.EngineConflictReport;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * UI section managing the real-time civilizational viability diagnostic card.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioLiveDiagnosticSection {

    private VBox liveDiagnosticCard;
    private Label liveDiagnosticHeader;
    private VBox liveDiagnosticContentBox;

    /**
     * Builds and returns the live diagnostic section card.
     *
     * @return VBox containing header and dynamic diagnostic container
     */
    public VBox buildSection() {
        liveDiagnosticCard = new VBox(8);
        liveDiagnosticCard.getStyleClass().add("card-section");

        liveDiagnosticHeader = new Label(I18n.getOrDefault("scenario.diagnostic.header", "📋 9. CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
        liveDiagnosticHeader.getStyleClass().add("label-section-header");

        liveDiagnosticContentBox = new VBox(4);
        liveDiagnosticCard.getChildren().addAll(liveDiagnosticHeader, liveDiagnosticContentBox);

        return liveDiagnosticCard;
    }

    /**
     * Updates localized UI texts and titles.
     */
    public void updateTexts() {
        if (liveDiagnosticHeader != null) {
            liveDiagnosticHeader.setText(I18n.getOrDefault("scenario.diagnostic.header", "📋 CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
        }
    }

    /**
     * Updates real-time diagnostic items and warnings based on current scenario parameters.
     *
     * @param planet active planet preset
     * @param eco active ecology preset
     * @param demoSource selected demographic source identifier
     * @param elevImg elevation raster image
     * @param densityImg custom population density image
     * @param dimsCount cultural vector dimension count
     * @param customTensorImages custom tensor raster images map
     * @param tensorTitleSupplier title supplier per tensor index
     * @param startYear starting epoch year
     * @param initPop initial total population count
     * @param validationErrors current parameter validation errors
     * @param jitReport JIT conflict report
     */
    public void updateDiagnostics(PlanetPreset planet,
                                  EcologyPreset eco,
                                  String demoSource,
                                  Image elevImg,
                                  Image densityImg,
                                  int dimsCount,
                                  Map<Integer, Image> customTensorImages,
                                  IntFunction<String> tensorTitleSupplier,
                                  long startYear,
                                  long initPop,
                                  List<String> validationErrors,
                                  EngineConflictReport jitReport) {
        if (liveDiagnosticContentBox == null) return;
        ScenarioSanityValidator.updateLiveDiagnostics(
                liveDiagnosticContentBox,
                liveDiagnosticHeader,
                planet,
                eco,
                demoSource,
                elevImg,
                densityImg,
                dimsCount,
                customTensorImages,
                tensorTitleSupplier,
                startYear,
                initPop,
                validationErrors,
                jitReport
        );
    }

    public VBox getLiveDiagnosticCard() { return liveDiagnosticCard; }
    public Label getLiveDiagnosticHeader() { return liveDiagnosticHeader; }
    public VBox getLiveDiagnosticContentBox() { return liveDiagnosticContentBox; }
}
