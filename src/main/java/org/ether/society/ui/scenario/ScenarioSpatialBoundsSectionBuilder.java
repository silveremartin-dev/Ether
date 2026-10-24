/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;

import java.util.List;

/**
 * UI Section Builder for geographical clipping, bounding box constraints, and boundary condition modeling.
 *
 * <p>Enables bounded regional or continental simulations by restricting cellular computations to a
 * spatial window $[\text{Lat}_{\min}, \text{Lat}_{\max}] \times [\text{Lng}_{\min}, \text{Lng}_{\max}]$
 * and configuring boundary flow physics (Dynamic Reservoir, Closed Barrier, or Periodic Toroidal Wrap).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSpatialBoundsSectionBuilder {

    /**
     * Host context contract connecting the spatial bounds builder to the scenario controller.
     */
    public interface HostContext {
        /** Notifies that a simulation parameter changed, requiring dirty tracking and diagnostic refresh. */
        void notifyParamChange();

        /** Redraws the map canvas preview with current clipping bounds. */
        void drawPreview();

        /** Resets the active cached preview cells. */
        void resetPreviewCells();

        /** Attaches default value tooltip indicators and context menu reset actions. */
        <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction);
    }

    private final HostContext host;

    private Label clippingHeader;
    private CheckBox clippingCheckBox;
    private ToggleButton graphicSelectBtn;
    private Spinner<Double> maxLatSpinner;
    private Spinner<Double> minLatSpinner;
    private Spinner<Double> minLngSpinner;
    private Spinner<Double> maxLngSpinner;
    private Label latMaxLabel;
    private Label latMinLabel;
    private Label lngMinLabel;
    private Label lngMaxLabel;
    private Label boundaryLabel;
    private ComboBox<String> boundaryModeCombo;
    private Button resetClippingBtn;
    private VBox clippingSubPanel;

    /**
     * Constructs a spatial bounds section builder with the given host context.
     *
     * @param host callback context
     */
    public ScenarioSpatialBoundsSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the spatial clipping and boundary condition UI card section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ 4. BORDERS & HISTORICAL SPATIAL CLIPPING"));
        clippingHeader.getStyleClass().add("label-section-header");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
        clippingCheckBox.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        graphicSelectBtn = new ToggleButton(I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Graphic Map Selection Mode"));
        graphicSelectBtn.setMaxWidth(Double.MAX_VALUE);
        graphicSelectBtn.getStyleClass().add("button-secondary");
        graphicSelectBtn.setStyle("-fx-font-size: 12px;");
        graphicSelectBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.graphic_select", "Activate this button or hold SHIFT to draw a selection rectangle on preview map.")));

        GridPane boundsGrid = new GridPane();
        boundsGrid.setHgap(8);
        boundsGrid.setVgap(6);

        maxLatSpinner = new Spinner<>(-90.0, 90.0, 90.0, 1.0);
        minLatSpinner = new Spinner<>(-90.0, 90.0, -90.0, 1.0);
        minLngSpinner = new Spinner<>(-180.0, 180.0, -180.0, 1.0);
        maxLngSpinner = new Spinner<>(-180.0, 180.0, 180.0, 1.0);

        if (host != null) {
            host.attachDefaultValueHandling(clippingCheckBox, false, () -> clippingCheckBox.setSelected(false));
            host.attachDefaultValueHandling(maxLatSpinner, 90.0, () -> maxLatSpinner.getValueFactory().setValue(90.0));
            host.attachDefaultValueHandling(minLatSpinner, -90.0, () -> minLatSpinner.getValueFactory().setValue(-90.0));
            host.attachDefaultValueHandling(minLngSpinner, -180.0, () -> minLngSpinner.getValueFactory().setValue(-180.0));
            host.attachDefaultValueHandling(maxLngSpinner, 180.0, () -> maxLngSpinner.getValueFactory().setValue(180.0));
        }

        for (Spinner<Double> sp : List.of(maxLatSpinner, minLatSpinner, minLngSpinner, maxLngSpinner)) {
            sp.setEditable(true);
            sp.setPrefWidth(75);
            sp.valueProperty().addListener((obs, old, val) -> {
                if (host != null) {
                    host.notifyParamChange();
                    host.drawPreview();
                }
            });
        }

        latMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Top):"));
        latMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bottom):"));
        lngMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Left):"));
        lngMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Right):"));
        for (Label lbl : List.of(latMaxLabel, latMinLabel, lngMinLabel, lngMaxLabel)) {
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        }
        boundsGrid.addRow(0, latMaxLabel, maxLatSpinner, latMinLabel, minLatSpinner);
        boundsGrid.addRow(1, lngMinLabel, minLngSpinner, lngMaxLabel, maxLngSpinner);

        boundaryLabel = new Label(I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
        boundaryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        boundaryModeCombo = new ComboBox<>();
        boundaryModeCombo.getItems().addAll(
            "DYNAMIC_RESERVOIR",
            "CLOSED_BARRIER",
            "PERIODIC_WRAP"
        );
        boundaryModeCombo.setValue("DYNAMIC_RESERVOIR");
        boundaryModeCombo.setMaxWidth(Double.MAX_VALUE);
        boundaryModeCombo.setOnAction(e -> {
            if (host != null) host.notifyParamChange();
        });
        if (host != null) {
            host.attachDefaultValueHandling(boundaryModeCombo, "DYNAMIC_RESERVOIR", () -> boundaryModeCombo.setValue("DYNAMIC_RESERVOIR"));
        }
        boundaryModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String item) {
                if (item == null) return "";
                switch (item) {
                    case "DYNAMIC_RESERVOIR" -> { return I18n.getOrDefault("scenario.boundary.reservoir", "🌊 External Virtual Reservoir (Free Flow)"); }
                    case "CLOSED_BARRIER" -> { return I18n.getOrDefault("scenario.boundary.barrier", "🧱 Sealed Barrier / Isolated (Closed Edges)"); }
                    case "PERIODIC_WRAP" -> { return I18n.getOrDefault("scenario.boundary.wrap", "🌐 Periodic Boundary (Toroidal)"); }
                    default -> { return item; }
                }
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        resetClippingBtn = new Button(I18n.getOrDefault("scenario.clipping.reset", "🔄 Reset Area (Full Planet)"));
        resetClippingBtn.setMaxWidth(Double.MAX_VALUE);
        resetClippingBtn.getStyleClass().add("button-secondary");
        resetClippingBtn.setStyle("-fx-font-size: 12px;");

        clippingSubPanel = new VBox(10, graphicSelectBtn, boundsGrid, boundaryLabel, boundaryModeCombo, resetClippingBtn);
        clippingSubPanel.setStyle("-fx-padding: 10px; -fx-background-color: rgba(56, 189, 248, 0.04); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 6px; -fx-border-width: 1px;");
        clippingSubPanel.setVisible(false);
        clippingSubPanel.setManaged(false);

        resetClippingBtn.setOnAction(e -> resetBounds());

        clippingCheckBox.setOnAction(e -> {
            boolean active = clippingCheckBox.isSelected();
            clippingSubPanel.setVisible(active);
            clippingSubPanel.setManaged(active);
            if (!active) {
                resetBoundsValues();
            }
            if (host != null) {
                host.notifyParamChange();
                host.drawPreview();
            }
        });

        section.getChildren().addAll(clippingHeader, clippingCheckBox, clippingSubPanel);
        return section;
    }

    /**
     * Resets clipping area bounds and deactivates partial simulation.
     */
    public void resetBounds() {
        resetBoundsValues();
        if (clippingCheckBox != null) clippingCheckBox.setSelected(false);
        if (clippingSubPanel != null) {
            clippingSubPanel.setVisible(false);
            clippingSubPanel.setManaged(false);
        }
        if (host != null) {
            host.resetPreviewCells();
            host.notifyParamChange();
            host.drawPreview();
        }
    }

    private void resetBoundsValues() {
        if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(-90.0);
        if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(90.0);
        if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(-180.0);
        if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(180.0);
        if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
    }

    /**
     * Applies programmatically specified spatial bounding box coordinates to the spinners.
     *
     * @param minLat minimum latitude (-90° to +90°)
     * @param maxLat maximum latitude (-90° to +90°)
     * @param minLon minimum longitude (-180° to +180°)
     * @param maxLon maximum longitude (-180° to +180°)
     */
    public void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
        if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(Math.round(minLat * 10.0) / 10.0);
        if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(Math.round(maxLat * 10.0) / 10.0);
        if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(Math.round(minLon * 10.0) / 10.0);
        if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(Math.round(maxLon * 10.0) / 10.0);
        if (clippingCheckBox != null) clippingCheckBox.setSelected(true);
        if (clippingSubPanel != null) {
            clippingSubPanel.setVisible(true);
            clippingSubPanel.setManaged(true);
        }
        if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
    }

    /**
     * Applies clipping settings from a scenario model into UI controls.
     *
     * @param s scenario model
     */
    public void applyScenario(Scenario s) {
        if (s == null || clippingCheckBox == null) return;
        boolean active = s.isClippingEnabled();
        clippingCheckBox.setSelected(active);
        if (clippingSubPanel != null) {
            clippingSubPanel.setVisible(active);
            clippingSubPanel.setManaged(active);
        }
        if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(s.getMinLat());
        if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(s.getMaxLat());
        if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(s.getMinLng());
        if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(s.getMaxLng());
        if (s.getBoundaryMode() != null && boundaryModeCombo != null) boundaryModeCombo.setValue(s.getBoundaryMode());
    }

    /**
     * Exports clipping controls from UI to scenario model.
     *
     * @param s scenario model
     */
    public void exportToScenario(Scenario s) {
        if (s == null) return;
        if (clippingCheckBox != null) {
            s.setClippingEnabled(clippingCheckBox.isSelected());
            s.setMinLat(getMinLat());
            s.setMaxLat(getMaxLat());
            s.setMinLng(getMinLng());
            s.setMaxLng(getMaxLng());
            s.setBoundaryMode(getBoundaryMode());
        }
    }

    /**
     * Updates localized strings for spatial clipping UI controls.
     */
    public void updateTexts() {
        if (clippingHeader != null) clippingHeader.setText(I18n.getOrDefault("scenario.clipping.header", "✂️ 4. BORDERS & HISTORICAL SPATIAL CLIPPING"));
        if (clippingCheckBox != null) clippingCheckBox.setText(I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
        if (graphicSelectBtn != null) graphicSelectBtn.setText(I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Graphic Map Selection Mode"));
        if (resetClippingBtn != null) resetClippingBtn.setText(I18n.getOrDefault("scenario.clipping.reset", "🔄 Reset Area (Full Planet)"));
        if (latMaxLabel != null) latMaxLabel.setText(I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Top):"));
        if (latMinLabel != null) latMinLabel.setText(I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bottom):"));
        if (lngMinLabel != null) lngMinLabel.setText(I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Left):"));
        if (lngMaxLabel != null) lngMaxLabel.setText(I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Right):"));
        if (boundaryLabel != null) boundaryLabel.setText(I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
        if (boundaryModeCombo != null && boundaryModeCombo.getCellFactory() != null) {
            boundaryModeCombo.setButtonCell(boundaryModeCombo.getCellFactory().call(null));
        }
    }

    // Getters
    public boolean isClippingEnabled() { return clippingCheckBox != null && clippingCheckBox.isSelected(); }
    public boolean isGraphicSelectActive() { return graphicSelectBtn != null && graphicSelectBtn.isSelected(); }
    public void setGraphicSelectActive(boolean active) { if (graphicSelectBtn != null) graphicSelectBtn.setSelected(active); }
    public double getMinLat() { return minLatSpinner != null && minLatSpinner.getValue() != null ? minLatSpinner.getValue() : -90.0; }
    public double getMaxLat() { return maxLatSpinner != null && maxLatSpinner.getValue() != null ? maxLatSpinner.getValue() : 90.0; }
    public double getMinLng() { return minLngSpinner != null && minLngSpinner.getValue() != null ? minLngSpinner.getValue() : -180.0; }
    public double getMaxLng() { return maxLngSpinner != null && maxLngSpinner.getValue() != null ? maxLngSpinner.getValue() : 180.0; }
    public String getBoundaryMode() { return boundaryModeCombo != null && boundaryModeCombo.getValue() != null ? boundaryModeCombo.getValue() : "DYNAMIC_RESERVOIR"; }

    public CheckBox getClippingCheckBox() { return clippingCheckBox; }
    public ToggleButton getGraphicSelectBtn() { return graphicSelectBtn; }
    public Spinner<Double> getMinLatSpinner() { return minLatSpinner; }
    public Spinner<Double> getMaxLatSpinner() { return maxLatSpinner; }
    public Spinner<Double> getMinLngSpinner() { return minLngSpinner; }
    public Spinner<Double> getMaxLngSpinner() { return maxLngSpinner; }
    public ComboBox<String> getBoundaryModeCombo() { return boundaryModeCombo; }
}
