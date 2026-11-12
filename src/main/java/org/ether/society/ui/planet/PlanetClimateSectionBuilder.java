/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;

import java.util.List;

/**
 * Autonomous UI Controller and Section Builder for Atmosphere, Greenhouse Gases,
 * Macroclimate Regimes, Thermal Equator-to-Pole Gradients, and Tri-Map Climate Raster Ingestion.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PlanetClimateSectionBuilder {

    public static final String CLIMATE_SRC_TEMP_EARTH = "🌍 Terre — WorldClim v2.1 Bio1 & ERA5 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_TEMP_MARS = "🔴 Mars — MGS TES Thermal Radiometry [Planétaire (Mars), -4.1 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_VENUS = "🟡 Vénus — Magellan SAR & Hypsometric Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_TEMP_MOON = "⚪ Lune — LRO Diviner Thermal Radiometer [Planétaire (Lune), -4.5 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_MERCURY = "⚪ Mercure — MESSENGER MLA Extreme Thermal Model [Planétaire (Mercure), -4.0 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_WMS = "🌐 NASA MERRA-2 (WMS Satellite) [Global, 1980 AD à Actuel]";

    public static final String CLIMATE_SRC_PRECIP_EARTH = "🌍 Terre — WorldClim v2.1 & GPCP v2.3 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_PRECIP_MARS = "🔴 Mars — Frost & Sublimation Model [Planétaire (Mars), -4.1 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_VENUS = "🟡 Vénus — H2SO4 Virga Cycle Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_MOON = "⚪ Lune — LRO LEND Vacuum Exosphere [Planétaire (Lune), -4.5 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_MERCURY = "⚪ Mercure — MESSENGER Exospheric Vacuum Model [Planétaire (Mercure), -4.0 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_WMS = "🌐 NASA GPM IMERG (WMS Satellite) [Global, 2000 AD à Actuel]";

    public static final String CLIMATE_SRC_SEASON_EARTH = "🌍 Terre — WorldClim v2.1 Bio4 & ERA5 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_SEASON_MARS = "🔴 Mars — Orbital Eccentricity Insolation Model [Planétaire (Mars), Cycles Milankovitch Martiens]";
    public static final String CLIMATE_SRC_SEASON_VENUS = "🟡 Vénus — Super-Rotation Low Variance Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_SEASON_MOON = "⚪ Lune — Diurnal Insolation Amplitude Model [Planétaire (Lune), Cycle Synodique 29.5j]";
    public static final String CLIMATE_SRC_SEASON_MERCURY = "⚪ Mercure — 3:2 Spin-Orbit Thermal Variance Model [Planétaire (Mercure), Résonance 3:2]";
    public static final String CLIMATE_SRC_SEASON_WMS = "🌐 NASA MODIS LST Amplitude (WMS Satellite) [Global, 2002 AD à Actuel]";

    public interface HostContext {
        void notifyParamChange();
        void updatePreview();
        boolean isUpdatingFromPreset();
        void calculateStellarIrradiance();
        void selectViewMode(int modeIndex);
        void exportProceduralClimateMap(String type);
        void fetchOnlineClimateData();
        Window getWindow();
    }

    private final HostContext context;

    private Slider tempGradSlider;
    private Slider oxygenSlider;
    private Slider co2Slider;
    private Slider albedoSlider;
    private Slider atmoPressureSlider;

    private Label climateSecHeader;
    private Label gradRowLabel;
    private Label oxygenRowLabel;
    private Label co2RowLabel;
    private Label albedoRowLabel;
    private Label atmoPressureRowLabel;
    private Label earthPaleoNoticeLabel;

    private PlanetClimateSubBlock tempBlock;
    private PlanetClimateSubBlock precipBlock;
    private PlanetClimateSubBlock seasonBlock;

    public PlanetClimateSectionBuilder(HostContext context) {
        this.context = context;
    }

    public VBox buildSection() {
        tempGradSlider = createSlider(0, 100, 40);
        oxygenSlider = createSlider(0, 50, 21);
        co2Slider = createSlider(0, 1_000_000, 420);
        albedoSlider = createSlider(0.0, 1.0, 0.30);
        atmoPressureSlider = createSlider(0.0, 10.0, 1.0);

        albedoSlider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        atmoPressureSlider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        co2Slider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        tempGradSlider.valueProperty().addListener((obs, old, val) -> context.updatePreview());

        gradRowLabel = new Label();
        oxygenRowLabel = new Label();
        co2RowLabel = new Label();
        albedoRowLabel = new Label();
        atmoPressureRowLabel = new Label();

        VBox atmosphericGlobal = new VBox(8,
                createControlRow(gradRowLabel, tempGradSlider, "%.1f °C",
                        "Gradient thermique équateur-pôles (différentiel thermique en °C entre l'équateur et les pôles)"),
                createControlRow(oxygenRowLabel, oxygenSlider, "%.1f %%",
                        "Taux d'oxygène atmosphérique O₂ — 21% pour Terre ; < 10% = hypoxie ; > 35% = risque incendie"),
                createControlRow(co2RowLabel, co2Slider, "%.0f ppm",
                        "CO₂ atmosphérique (ppm). Terre actuelle: 420 ppm. Crétacé: ~1000-2000 ppm. Vénus: ~960 000 ppm"),
                createControlRow(atmoPressureRowLabel, atmoPressureSlider, "%.2f atm",
                        "Pression atmosphérique au sol. Terre = 1.0 atm. Mars ≈ 0.006 atm. Vénus ≈ 92 atm")
        );

        tempBlock = new PlanetClimateSubBlock("temp", 1, "168, 85, 247", "12345", 100L, "🌡",
                List.of(CLIMATE_SRC_TEMP_EARTH, CLIMATE_SRC_TEMP_MARS, CLIMATE_SRC_TEMP_VENUS, CLIMATE_SRC_TEMP_MOON, CLIMATE_SRC_TEMP_MERCURY, CLIMATE_SRC_TEMP_WMS), context);
        precipBlock = new PlanetClimateSubBlock("precip", 2, "2, 132, 199", "11111", 1000L, "🌧",
                List.of(CLIMATE_SRC_PRECIP_EARTH, CLIMATE_SRC_PRECIP_MARS, CLIMATE_SRC_PRECIP_VENUS, CLIMATE_SRC_PRECIP_MOON, CLIMATE_SRC_PRECIP_MERCURY, CLIMATE_SRC_PRECIP_WMS), context);
        seasonBlock = new PlanetClimateSubBlock("season", 3, "245, 158, 11", "99999", 2000L, "🍂",
                List.of(CLIMATE_SRC_SEASON_EARTH, CLIMATE_SRC_SEASON_MARS, CLIMATE_SRC_SEASON_VENUS, CLIMATE_SRC_SEASON_MOON, CLIMATE_SRC_SEASON_MERCURY, CLIMATE_SRC_SEASON_WMS), context);

        earthPaleoNoticeLabel = new Label(I18n.getOrDefault("planet.climate.earth_paleo_notice",
                "🌍 Note Paléoclimatique : Pour la Terre, le moteur intègre la dynamique temporelle (glaciations, niveau des mers LGM -120m, Sahara Vert). Les cartes ci-contre présentent la baseline climatologique actuelle (ERA5 / WorldClim)."));
        earthPaleoNoticeLabel.setWrapText(true);
        earthPaleoNoticeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56,189,248,0.08); -fx-background-radius: 5; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 5;");

        VBox climateControls = new VBox(12,
                atmosphericGlobal,
                earthPaleoNoticeLabel,
                new Separator(),
                tempBlock.getRootBlock(),
                new Separator(),
                precipBlock.getRootBlock(),
                new Separator(),
                seasonBlock.getRootBlock()
        );

        climateSecHeader = new Label(I18n.getOrDefault("planet.section.climate", "3. ATMOSPHERE & PLANETARY CLIMATE DOMAIN"));
        climateSecHeader.getStyleClass().add("label-section-header");
        VBox section = new VBox(8, climateSecHeader, climateControls);
        section.getStyleClass().add("card-section");
        return section;
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        tempGradSlider.setValue(p.temperatureGradient());
        oxygenSlider.setValue(p.oxygenPercentage());
        co2Slider.setValue(p.co2Ppm());
        albedoSlider.setValue(p.albedo());
        atmoPressureSlider.setValue(p.atmospherePressureAtm());

        if (tempBlock != null) tempBlock.applyPreset(p.tempSource(), p.customClimateBase64(), p.tempUseImport());
        if (precipBlock != null) precipBlock.applyPreset(p.precipSource(), p.customRainfallBase64(), p.precipUseImport());
        if (seasonBlock != null) seasonBlock.applyPreset(p.seasonSource(), p.customSeasonalityBase64(), p.seasonUseImport());
    }

    public void updateTexts() {
        if (climateSecHeader != null) climateSecHeader.setText(I18n.getOrDefault("planet.section.climate", "ATMOSPHERE & PLANETARY CLIMATE DOMAIN"));
        if (gradRowLabel != null) gradRowLabel.setText(I18n.get("planet.param.temp_grad"));
        if (oxygenRowLabel != null) oxygenRowLabel.setText(I18n.getOrDefault("planet.param.oxygen", "Oxygen Level O₂ (%)"));
        if (co2RowLabel != null) co2RowLabel.setText(I18n.getOrDefault("planet.param.co2", "Dioxyde de Carbone CO₂ (ppm)"));
        if (albedoRowLabel != null) albedoRowLabel.setText(I18n.getOrDefault("planet.param.albedo", "Planetary Albedo"));
        if (atmoPressureRowLabel != null) atmoPressureRowLabel.setText(I18n.getOrDefault("planet.param.atmo_pressure", "Atmospheric Pressure (atm)"));
        if (earthPaleoNoticeLabel != null) earthPaleoNoticeLabel.setText(I18n.getOrDefault("planet.climate.earth_paleo_notice", "🌍 Note Paléoclimatique : Pour la Terre, le moteur intègre la dynamique temporelle (glaciations, niveau des mers LGM -120m, Sahara Vert). Les cartes ci-contre présentent la baseline climatologique actuelle (ERA5 / WorldClim)."));

        if (tempBlock != null) tempBlock.updateTexts();
        if (precipBlock != null) precipBlock.updateTexts();
        if (seasonBlock != null) seasonBlock.updateTexts();
    }

    public void setLocalFileInCombo(ComboBox<String> combo, String fileName) {
        String prefix = "📌 Fichier local : ";
        combo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
        String entry = prefix + fileName;
        combo.getItems().add(entry);
        combo.setValue(entry);
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            if (!slider.isValueChanging()) context.updatePreview();
        });
        slider.setOnMouseReleased(e -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            context.updatePreview();
        });
        return slider;
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        return PlanetControlRowHelper.createControlRow(label, slider, formatPattern, tooltipText);
    }

    public double getTemperatureGradient() { return tempGradSlider.getValue(); }
    public double getOxygenPercentage() { return oxygenSlider.getValue(); }
    public double getCo2Ppm() { return co2Slider.getValue(); }
    public double getAlbedo() { return albedoSlider.getValue(); }
    public double getAtmospherePressureAtm() { return atmoPressureSlider.getValue(); }

    public boolean isTempImportActive() { return tempBlock != null && tempBlock.isImportActive(); }
    public String getTempSource() { return tempBlock != null ? tempBlock.getSource() : ""; }
    public long getTempSeed(long defaultSeed) { return tempBlock != null ? tempBlock.getSeed(defaultSeed) : defaultSeed + 100L; }

    public boolean isPrecipImportActive() { return precipBlock != null && precipBlock.isImportActive(); }
    public String getPrecipSource() { return precipBlock != null ? precipBlock.getSource() : ""; }
    public long getPrecipSeed(long defaultSeed) { return precipBlock != null ? precipBlock.getSeed(defaultSeed) : defaultSeed + 1000L; }

    public boolean isSeasonImportActive() { return seasonBlock != null && seasonBlock.isImportActive(); }
    public String getSeasonSource() { return seasonBlock != null ? seasonBlock.getSource() : ""; }
    public long getSeasonSeed(long defaultSeed) { return seasonBlock != null ? seasonBlock.getSeed(defaultSeed) : defaultSeed + 2000L; }

    public Image getCustomClimateImage() { return tempBlock != null ? tempBlock.getCustomImage() : null; }
    public void setCustomClimateImage(Image img) { if (tempBlock != null) tempBlock.setCustomImage(img); }
    public String getCachedClimateBase64() { return tempBlock != null ? tempBlock.getCachedBase64() : null; }
    public void setCachedClimateBase64(String b64) { if (tempBlock != null) tempBlock.setCachedBase64(b64); }

    public Image getCustomRainfallImage() { return precipBlock != null ? precipBlock.getCustomImage() : null; }
    public void setCustomRainfallImage(Image img) { if (precipBlock != null) precipBlock.setCustomImage(img); }
    public String getCachedRainfallBase64() { return precipBlock != null ? precipBlock.getCachedBase64() : null; }
    public void setCachedRainfallBase64(String b64) { if (precipBlock != null) precipBlock.setCachedBase64(b64); }

    public Image getCustomSeasonalityImage() { return seasonBlock != null ? seasonBlock.getCustomImage() : null; }
    public void setCustomSeasonalityImage(Image img) { if (seasonBlock != null) seasonBlock.setCustomImage(img); }
    public String getCachedSeasonalityBase64() { return seasonBlock != null ? seasonBlock.getCachedBase64() : null; }
    public void setCachedSeasonalityBase64(String b64) { if (seasonBlock != null) seasonBlock.setCachedBase64(b64); }

    public RadioButton getRadioTempImport() { return tempBlock != null ? tempBlock.getRadioImport() : null; }
    public RadioButton getRadioPrecipImport() { return precipBlock != null ? precipBlock.getRadioImport() : null; }
    public RadioButton getRadioSeasonImport() { return seasonBlock != null ? seasonBlock.getRadioImport() : null; }
    public Button getLoadClimateBtn() { return tempBlock != null ? tempBlock.getLoadBtn() : null; }
    public Button getLoadRainfallBtn() { return precipBlock != null ? precipBlock.getLoadBtn() : null; }
    public Button getLoadSeasonalityBtn() { return seasonBlock != null ? seasonBlock.getLoadBtn() : null; }
    public Label getClimateFileLabel() { return tempBlock != null ? tempBlock.getFileLabel() : null; }
    public Label getRainfallFileLabel() { return precipBlock != null ? precipBlock.getFileLabel() : null; }
    public Label getSeasonalityFileLabel() { return seasonBlock != null ? seasonBlock.getFileLabel() : null; }
    public ComboBox<String> getTempSourceCombo() { return tempBlock != null ? tempBlock.getSourceCombo() : null; }
    public ComboBox<String> getPrecipSourceCombo() { return precipBlock != null ? precipBlock.getSourceCombo() : null; }
    public ComboBox<String> getSeasonSourceCombo() { return seasonBlock != null ? seasonBlock.getSourceCombo() : null; }
}
