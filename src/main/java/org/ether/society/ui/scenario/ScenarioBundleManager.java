/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.EtherScenarioBundle;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonUtil;
import org.ether.society.security.EtherBundleSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Manages the unified scenario archive (.ether bundle) export, import, cryptographic verification,
 * and UI container.
 *
 * <p>A unified bundle bundles the complete tripartite simulation state into a single signed archive:
 * <ul>
 *   <li>Planetary astrophysics &amp; geophysics context (Tab 1)</li>
 *   <li>Biogeochemical &amp; lithospheric resource distribution (Tab 2)</li>
 *   <li>Demographic, cultural, and engine configuration (Tab 3)</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioBundleManager {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioBundleManager.class);

    /**
     * Host context contract connecting the bundle manager to the parent setup panel.
     */
    public interface HostContext {
        Window getWindow();
        PlanetPreset getActivePlanetPreset();
        EcologyPreset getActiveEcologyPreset();
        Scenario getScenario();
        void applyScenarioToUI(Scenario scenario);
        void onBundleLoaded(PlanetPreset planet, EcologyPreset ecology);
        void generatePreview();
        void showIntegrityWarning(String message);
    }

    private final HostContext host;

    private Label bundleHeader;
    private Label bundleSubtitle;
    private Button btnExportBundle;
    private Button btnImportBundle;

    /**
     * Constructs a scenario bundle manager.
     *
     * @param host callback context
     */
    public ScenarioBundleManager(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the unified bundle import/export UI section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        bundleHeader = new Label(I18n.getOrDefault("scenario.bundle.header", "📦 7. UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
        bundleHeader.getStyleClass().add("label-section-header");

        bundleSubtitle = new Label(I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
        bundleSubtitle.setWrapText(true);
        bundleSubtitle.getStyleClass().add("card-description-muted");

        btnExportBundle = new Button(I18n.getOrDefault("scenario.btn.export_bundle", "📦 Export Bundle (.ether)"));
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.getStyleClass().add("button-accent-blue");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());
        btnExportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));

        btnImportBundle = new Button(I18n.getOrDefault("scenario.btn.import_bundle", "📂 Import Bundle (.ether)"));
        btnImportBundle.getStyleClass().add("button-secondary");
        btnImportBundle.setMaxWidth(Double.MAX_VALUE);
        btnImportBundle.setOnAction(e -> importUnifiedBundle());
        btnImportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));

        HBox bundleBox = new HBox(8, btnExportBundle, btnImportBundle);
        HBox.setHgrow(btnExportBundle, Priority.ALWAYS);
        HBox.setHgrow(btnImportBundle, Priority.ALWAYS);

        section.getChildren().addAll(bundleHeader, bundleSubtitle, bundleBox);
        return section;
    }

    /**
     * Prompts the user to save the complete simulation configuration into a cryptographically signed .ether bundle.
     */
    public void exportUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.export_bundle_dialog", "Export Unified Scenario Bundle (.ether)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        chooser.setInitialFileName("mon-scenario-ether.ether");
        Window owner = host != null ? host.getWindow() : null;
        File file = chooser.showSaveDialog(owner);
        if (file != null) {
            try {
                PlanetPreset planet = host != null ? host.getActivePlanetPreset() : null;
                if (planet == null) planet = PlanetPreset.EARTH_LIKE;
                EcologyPreset eco = host != null ? host.getActiveEcologyPreset() : null;
                if (eco == null) eco = EcologyPreset.EARTH_STANDARD;
                Scenario scenario = host != null ? host.getScenario() : new Scenario();

                EtherScenarioBundle rawBundle = new EtherScenarioBundle("1.0.0-beta.2", planet, eco, scenario);
                EtherScenarioBundle bundle = EtherBundleSigner.signBundle(rawBundle, "Ether Lead Planner");

                ObjectMapper mapper = EtherJsonUtil.getMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, bundle);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("scenario.title.export_success", "Export Successful"));
                alert.setHeaderText(I18n.getOrDefault("scenario.header.export_success", "Simulation Bundle Exported Successfully"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.export_bundle_desc", "Unified simulation bundle signed & saved to:\n") + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Failed to export unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("scenario.title.export_error", "Export Error"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.export_failed", "Cannot save bundle: ") + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Prompts the user to load a unified scenario bundle (.ether) and applies it to the simulation state.
     */
    public void importUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.import_bundle_dialog", "Import Unified Scenario Bundle (.ether)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ether Files (*.ether, *.json)", "*.ether", "*.json"));
        Window owner = host != null ? host.getWindow() : null;
        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            try {
                ObjectMapper mapper = EtherJsonUtil.getMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                EtherScenarioBundle bundle = mapper.readValue(file, EtherScenarioBundle.class);

                if (bundle != null) {
                    boolean isValid = EtherBundleSigner.verifyBundle(bundle);
                    if (!isValid && host != null) {
                        host.showIntegrityWarning("⚠️ Avertissement d'intégrité : Le bundle importé a été modifié ou sa signature ne concorde pas.");
                    }

                    if (host != null) {
                        if (bundle.scenario() != null) {
                            host.applyScenarioToUI(bundle.scenario());
                        }
                        host.onBundleLoaded(bundle.planetPreset(), bundle.ecologyPreset());
                        host.generatePreview();
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(I18n.getOrDefault("scenario.title.import_success", "Import Successful"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.header.import_success", "Unified Bundle Applied"));
                    alert.setContentText(I18n.getOrDefault("scenario.alert.import_bundle_success", "Physical, ecological, and demographic parameters loaded successfully from bundle."));
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                logger.error("Failed to import unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("scenario.title.import_error", "Import Error"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.import_bundle_invalid", "Invalid bundle file: ") + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Updates localized UI strings for unified bundle elements.
     */
    public void updateTexts() {
        if (bundleHeader != null) bundleHeader.setText(I18n.getOrDefault("scenario.bundle.header", "📦 UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
        if (bundleSubtitle != null) bundleSubtitle.setText(I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
        if (btnExportBundle != null) {
            btnExportBundle.setText(I18n.getOrDefault("scenario.btn.export_bundle", "📦 Export Bundle (.ether)"));
            btnExportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));
        }
        if (btnImportBundle != null) {
            btnImportBundle.setText(I18n.getOrDefault("scenario.btn.import_bundle", "📂 Import Bundle (.ether)"));
            btnImportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));
        }
    }

    public Button getBtnExportBundle() { return btnExportBundle; }
    public Button getBtnImportBundle() { return btnImportBundle; }
}
