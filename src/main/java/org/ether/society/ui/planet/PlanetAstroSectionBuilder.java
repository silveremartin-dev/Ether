/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;

import java.util.function.Supplier;

/**
 * Autonomous UI Controller and Section Builder for Astronomy, Physical Characteristics,
 * Orbital Dynamics, and Celestial Body Configurations (Planets and Natural Satellites).
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP).
 * Calculates solar irradiance, stellar equilibrium blackbody temperatures, and greenhouse thermal forcings.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PlanetAstroSectionBuilder {

    /** Callback interface defining interaction with the parent panel */
    public interface HostContext {
        void notifyParamChange();
        void updatePreview();
        boolean isUpdatingFromPreset();
        double getAtmosphericPressure();
        double getCo2Ppm();
        double getAlbedo();
    }

    private final HostContext context;

    // Controls
    private ComboBox<String> bodyTypeCombo;
    private Slider parentMassSlider;
    private Slider orbitDistanceParentSlider;
    private VBox satelliteRowBox;

    private Slider radiusSlider;
    private Slider dayLengthSlider;
    private Slider axialTiltSlider;
    private Slider yearLengthSlider;
    private Slider distanceSunSlider;
    private Slider solarLumSlider;
    private Slider avgTempSlider;

    // Labels
    private Label astroSecHeader;
    private Label bodyTypeRowLabel;
    private Label parentMassRowLabel;
    private Label orbitDistanceParentRowLabel;
    private Label radiusRowLabel;
    private Label dayRowLabel;
    private Label tiltRowLabel;
    private Label yearRowLabel;
    private Label distRowLabel;
    private Label lumRowLabel;
    private Label tempRowLabel;
    private Label irradianceLabel;

    public PlanetAstroSectionBuilder(HostContext context) {
        this.context = context;
    }

    /**
     * Builds and returns the Astronomy and Physics UI section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        VBox astroControls = new VBox(8);

        bodyTypeRowLabel = new Label(I18n.getOrDefault("planet.param.body_type", "Celestial Body Type:"));
        bodyTypeCombo = new ComboBox<>();
        bodyTypeCombo.getItems().addAll("planet", "satellite");
        bodyTypeCombo.setValue("planet");
        bodyTypeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText("satellite".equals(item) ?
                            I18n.getOrDefault("planet.body_type.satellite", "🌙 Natural Satellite / Moon (Planetary Orbit)") :
                            I18n.getOrDefault("planet.body_type.planet", "🌍 Independent Planet (Direct Stellar Orbit)"));
                }
            }
        });
        bodyTypeCombo.setButtonCell(bodyTypeCombo.getCellFactory().call(null));
        bodyTypeCombo.setMaxWidth(Double.MAX_VALUE);
        bodyTypeCombo.setOnAction(e -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            toggleSatelliteControls();
        });

        parentMassSlider = createSlider(0.1, 500.0, 1.0);
        orbitDistanceParentSlider = createSlider(5000, 2000000, 384400);

        parentMassRowLabel = new Label();
        orbitDistanceParentRowLabel = new Label();

        satelliteRowBox = new VBox(6,
                createControlRow(parentMassRowLabel, parentMassSlider, "%.1f M⊕", I18n.getOrDefault("planet.tooltip.parent_mass", "Mass of host planet around which satellite orbits")),
                createControlRow(orbitDistanceParentRowLabel, orbitDistanceParentSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.parent_dist", "Average orbital distance around host planet"))
        );
        satelliteRowBox.setVisible(false);
        satelliteRowBox.setManaged(false);

        radiusSlider = createSlider(1000, 25000, 6371);
        dayLengthSlider = createSlider(1, 3000, 24);
        axialTiltSlider = createSlider(0, 180, 23.5);
        yearLengthSlider = createSlider(10, 12000, 365);
        distanceSunSlider = createSlider(0.1, 10.0, 1.0);
        solarLumSlider = createSlider(0.1, 5.0, 1.0);
        avgTempSlider = createSlider(-200, 500, 15);

        distanceSunSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());
        solarLumSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());

        radiusRowLabel = new Label();
        dayRowLabel = new Label();
        tiltRowLabel = new Label();
        yearRowLabel = new Label();
        distRowLabel = new Label();
        lumRowLabel = new Label();
        tempRowLabel = new Label();

        irradianceLabel = new Label();
        irradianceLabel.getStyleClass().add("value-label");

        astroControls.getChildren().addAll(
                createControlRow(bodyTypeRowLabel, bodyTypeCombo, I18n.getOrDefault("planet.tooltip.body_type", "Toggle between independent planet and natural satellite/moon")),
                satelliteRowBox,
                createControlRow(radiusRowLabel, radiusSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.radius", "Average planetary radius in km")),
                createControlRow(dayRowLabel, dayLengthSlider, "%.1f h", I18n.getOrDefault("planet.tooltip.day_length", "Planetary rotation duration in hours")),
                createControlRow(tiltRowLabel, axialTiltSlider, "%.1f°", I18n.getOrDefault("planet.tooltip.axial_tilt", "Inclinaison de l'axe de rotation")),
                createControlRow(yearRowLabel, yearLengthSlider, "%.0f d", I18n.getOrDefault("planet.tooltip.year_length", "Revolution duration in days")),
                createControlRow(distRowLabel, distanceSunSlider, "%.2f AU", I18n.getOrDefault("planet.tooltip.distance_sun", "Distance to central star in AU")),
                createControlRow(lumRowLabel, solarLumSlider, "%.2f L☉", I18n.getOrDefault("planet.tooltip.solar_lum", "Central star luminosity")),
                irradianceLabel,
                createControlRow(tempRowLabel, avgTempSlider, "%.1f °C", I18n.getOrDefault("planet.tooltip.avg_temp", "Average global surface temperature"))
        );

        astroSecHeader = new Label(I18n.getOrDefault("planet.section.astro", "1. ASTRONOMY, PHYSICS & CELESTIAL BODIES DOMAIN"));
        astroSecHeader.getStyleClass().add("label-section-header");
        VBox section = new VBox(8, astroSecHeader, astroControls);
        section.getStyleClass().add("card-section");
        return section;
    }

    public void toggleSatelliteControls() {
        boolean isSatellite = "satellite".equals(bodyTypeCombo.getValue());
        if (satelliteRowBox != null) {
            satelliteRowBox.setVisible(isSatellite);
            satelliteRowBox.setManaged(isSatellite);
        }
    }

    public void calculateStellarIrradiance() {
        double d = distanceSunSlider.getValue();
        double l = solarLumSlider.getValue();
        double irradiance = 1361.0 * (l / (d * d));
        if (irradianceLabel != null) {
            irradianceLabel.setText(String.format("%s: %.1f W/m²", I18n.get("planet.param.irradiance"), irradiance));
        }

        if (!context.isUpdatingFromPreset()) {
            double alb = context.getAlbedo();
            double atmoP = context.getAtmosphericPressure();
            double co2Ppm = context.getCo2Ppm();

            // Blackbody equilibrium temperature: T_eq = 278.5 * [(L * (1 - albedo)) / d^2]^(1/4) - 273.15
            double tEquilibriumC = 278.5 * Math.pow((l * (1.0 - alb)) / (d * d), 0.25) - 273.15;

            double greenhouseBoost = 0.0;
            if (atmoP >= 0.01) {
                double co2PartialPressureAtm = atmoP * (co2Ppm / 1_000_000.0);
                double co2Forcing = 3.0 * (Math.log(Math.max(0.0001, co2PartialPressureAtm / 0.00028)) / Math.log(2.0));
                double pressureTerm = (atmoP > 1.0) ? 8.0 * Math.log10(atmoP) : (atmoP - 1.0) * 8.0;
                greenhouseBoost = 33.0 * Math.sqrt(Math.min(2.0, atmoP)) + co2Forcing + pressureTerm;
            }

            double calcTempC = tEquilibriumC + greenhouseBoost;
            calcTempC = Math.max(-250.0, Math.min(600.0, calcTempC));
            avgTempSlider.setValue(calcTempC);
        }
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        bodyTypeCombo.setValue(p.isSatellite() ? "satellite" : "planet");
        toggleSatelliteControls();
        parentMassSlider.setValue(p.parentPlanetMassEarthMasses());
        orbitDistanceParentSlider.setValue(p.orbitalDistanceToParentKm());
        radiusSlider.setValue(p.radiusKm());
        dayLengthSlider.setValue(p.dayLengthHours());
        axialTiltSlider.setValue(p.axialTiltDegrees());
        yearLengthSlider.setValue(p.yearLengthDays());
        distanceSunSlider.setValue(p.distanceToSunAU());
        solarLumSlider.setValue(p.solarLuminosity());
        avgTempSlider.setValue(p.averageTempC());
        calculateStellarIrradiance();
    }

    public void updateTexts() {
        if (astroSecHeader != null) astroSecHeader.setText(I18n.getOrDefault("planet.section.astro", "ASTRONOMY, PHYSICS & CELESTIAL BODIES DOMAIN"));
        if (bodyTypeRowLabel != null) bodyTypeRowLabel.setText(I18n.getOrDefault("planet.param.body_type", "Celestial Body Type:"));
        if (parentMassRowLabel != null) parentMassRowLabel.setText(I18n.getOrDefault("planet.param.parent_mass", "Host planet mass (M⊕)"));
        if (orbitDistanceParentRowLabel != null) orbitDistanceParentRowLabel.setText(I18n.getOrDefault("planet.param.parent_dist", "Host orbital distance (km)"));
        if (radiusRowLabel != null) radiusRowLabel.setText(I18n.get("planet.param.radius"));
        if (dayRowLabel != null) dayRowLabel.setText(I18n.get("planet.param.day_length"));
        if (tiltRowLabel != null) tiltRowLabel.setText(I18n.get("planet.param.axial_tilt"));
        if (yearRowLabel != null) yearRowLabel.setText(I18n.get("planet.param.year_length"));
        if (distRowLabel != null) distRowLabel.setText(I18n.get("planet.param.distance_sun"));
        if (lumRowLabel != null) lumRowLabel.setText(I18n.get("planet.param.solar_lum"));
        if (tempRowLabel != null) tempRowLabel.setText(I18n.get("planet.param.avg_temp"));

        if (bodyTypeCombo != null && bodyTypeCombo.getCellFactory() != null) {
            bodyTypeCombo.setButtonCell(bodyTypeCombo.getCellFactory().call(null));
        }
        calculateStellarIrradiance();
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            if (!slider.isValueChanging()) {
                context.updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            context.updatePreview();
        });
        return slider;
    }

    private VBox createControlRow(Label label, javafx.scene.Node control, String tooltipText) {
        label.getStyleClass().add("control-label");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            if (control instanceof Control ctrl) {
                ctrl.setTooltip(tt);
            }
        }
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");

        slider.valueProperty().addListener((obs, old, val) ->
                valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            valLabel.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    // Getters for building preset
    public boolean isSatellite() { return "satellite".equals(bodyTypeCombo.getValue()); }
    public double getParentMass() { return parentMassSlider.getValue(); }
    public double getOrbitDistanceParent() { return orbitDistanceParentSlider.getValue(); }
    public double getRadiusKm() { return radiusSlider.getValue(); }
    public double getDayLengthHours() { return dayLengthSlider.getValue(); }
    public double getAxialTiltDegrees() { return axialTiltSlider.getValue(); }
    public double getYearLengthDays() { return yearLengthSlider.getValue(); }
    public double getDistanceToSunAU() { return distanceSunSlider.getValue(); }
    public double getSolarLuminosity() { return solarLumSlider.getValue(); }
    public double getAverageTempC() { return avgTempSlider.getValue(); }

    public ComboBox<String> getBodyTypeCombo() { return bodyTypeCombo; }
    public Slider getRadiusSlider() { return radiusSlider; }
    public Slider getDayLengthSlider() { return dayLengthSlider; }
    public Slider getAxialTiltSlider() { return axialTiltSlider; }
    public Slider getYearLengthSlider() { return yearLengthSlider; }
    public Slider getDistanceSunSlider() { return distanceSunSlider; }
    public Slider getSolarLumSlider() { return solarLumSlider; }
    public Slider getAvgTempSlider() { return avgTempSlider; }
    public Slider getParentMassSlider() { return parentMassSlider; }
    public Slider getOrbitDistanceParentSlider() { return orbitDistanceParentSlider; }
}
