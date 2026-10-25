/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.ether.society.engines.compiler.DynamicEngineCompiler;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * <h2>Scenario Custom Engine Handler</h2>
 * <p>
 * Modular handler for custom and Tier-1 physical law engine export/import,
 * on-the-fly dynamic compilation, and UI binding injection.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ScenarioCustomEngineHandler {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioCustomEngineHandler.class);

    private final Map<String, CheckBox> typeBCheckBoxMap;
    private final VBox typeBBoxContainer;
    private final Runnable onParamChangedCallback;

    /**
     * Constructs the custom engine handler.
     *
     * @param typeBCheckBoxMap map of registered engine checkboxes
     * @param typeBBoxContainer UI container holding the engine checkboxes
     * @param onParamChangedCallback callback invoked when checkbox state changes
     */
    public ScenarioCustomEngineHandler(
            Map<String, CheckBox> typeBCheckBoxMap,
            VBox typeBBoxContainer,
            Runnable onParamChangedCallback) {
        this.typeBCheckBoxMap = typeBCheckBoxMap;
        this.typeBBoxContainer = typeBBoxContainer;
        this.onParamChangedCallback = onParamChangedCallback;
    }

    /**
     * Dynamically registers a hot-compiled custom engine into the UI.
     *
     * @param engineKey engine class name
     * @param labelText human-readable label
     * @param tooltipText description tooltip
     * @param defaultSelected whether to select immediately
     */
    public void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (typeBCheckBoxMap.containsKey(engineKey)) {
            typeBCheckBoxMap.get(engineKey).setSelected(defaultSelected);
            return;
        }

        CheckBox cb = new CheckBox(labelText);
        cb.setSelected(defaultSelected);
        cb.getStyleClass().add("custom-engine-checkbox");
        cb.setTooltip(new Tooltip(tooltipText));
        cb.setOnAction(e -> {
            if (onParamChangedCallback != null) {
                onParamChangedCallback.run();
            }
        });

        typeBCheckBoxMap.put(engineKey, cb);

        Label badge = new Label(I18n.getOrDefault("scenario.badge.custom", "[🔌 CUSTOM]"));
        badge.getStyleClass().add("custom-engine-badge");

        HBox row = new HBox(8, badge, cb);
        row.setAlignment(Pos.CENTER_LEFT);

        if (typeBBoxContainer != null) {
            typeBBoxContainer.getChildren().add(row);
        }
    }

    /**
     * Exports a Java boilerplate template for a Tier 1 Physical Law engine.
     *
     * @param className target engine class name
     */
    public void exportPhysicalLawEngineTemplate(String className) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_law_dialog", "Export Physical Law Engine (.java)"));
            fileChooser.setInitialFileName(className + ".java");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Source (*.java)", "*.java"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                String template = """
                    package org.ether.society.engines.tier1;

                    import org.ether.society.core.H3SimulationEngine;
                    import org.ether.society.database.H3Cell;

                    /**
                     * Physical Law Engine Core: %s
                     */
                    public class %s {
                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
                            // Thermodynamic conservation & physical balance laws
                        }
                    }
                    """.formatted(className, className);
                Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        I18n.getOrDefault("scenario.alert.export_law_success", "Physical Law Engine template exported successfully:\n") + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Failed to export physical law engine template", ex);
        }
    }

    /**
     * Exports a Java boilerplate template for a Tier 2 Custom/Optional engine.
     */
    public void exportCustomEngineTemplate() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_engine_dialog", "Export Custom Ether Engine Template (.java)"));
            fileChooser.setInitialFileName("MyCustomOptionalEngine.java");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Source (*.java)", "*.java"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                String template = DynamicEngineCompiler.generateEngineTemplateCode("MyCustomOptionalEngine");
                Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        I18n.getOrDefault("scenario.alert.export_template_success", "Custom engine template exported successfully:\n") + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Error exporting custom engine template", ex);
        }
    }

    /**
     * Opens a file dialog to import, compile and hot-load a custom Java simulation engine.
     */
    public void importCustomEngineFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.import_engine_dialog", "Import Custom Engine (.java)"));
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Java Source (*.java)", "*.java")
            );
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                DynamicEngineCompiler.CompilationResult result = DynamicEngineCompiler.compileAndLoadEngine(file);

                if (result.success()) {
                    String engineKey = result.engineName();
                    addCustomEngineCheckBoxToUI(engineKey, "🔌 " + engineKey + " (" + I18n.getOrDefault("scenario.engine.custom_badge", "Custom Engine") + ")",
                            result.message(), true);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, result.message());
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, result.message());
                    alert.show();
                }
            }
        } catch (Exception ex) {
            logger.error("Error importing custom engine file", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import error: " + ex.getMessage());
            alert.show();
        }
    }
}
