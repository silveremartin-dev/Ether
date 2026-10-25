/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import org.ether.society.ui.PresetControlBar;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <h2>Resource Planet Section Builder</h2>
 * <p>
 * Modular UI builder and coordinator for the inherited planetary preset section
 * in {@link org.ether.society.ui.ResourceDistributionPanel}.
 * Handles thermodynamic synthesis deduction, slider adaptation, and automatic cartographic raster coupling.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ResourcePlanetSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourcePlanetSectionBuilder.class);

    public interface HostContext {
        PlanetPreset getActivePlanetPreset();
        void setActivePlanetPreset(PlanetPreset preset);
        boolean isUpdatingFromPreset();
        void setUpdatingFromPreset(boolean updating);
        ResourceBiomeSectionBuilder getBiomeSectionBuilder();
        ResourceHydroSectionBuilder getHydroSectionBuilder();
        ResourceClimateSectionBuilder getClimateSectionBuilder();
        ResourceGeologySectionBuilder getGeologySectionBuilder();
        Slider getCrustalMetalSlider();
        Slider getPreciousMetalSlider();
        Slider getMantleHeatSlider();
        Slider getSeismicActivitySlider();
        Slider getVolcanicActivitySlider();
        Label getEcoCompatibilityLabel();
        Label getResourceFileLabel();
        PresetControlBar<EcologyPreset> getEcologyPresetBar();
        void updateSummary();
        void updatePreviewCanvas();
        void updateLegend();
        int getActiveCellCount();
        String getGeologySourceDisplayName(String body);
        void prepopulateGeologyTensors(String body);
        Supplier<PlanetPreset> getPlanetPresetSupplier();
    }

    private final HostContext host;
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private Label planetContextLabel;
    private Label thermoSynthesisBadge;
    private Button syncPlanetBtn;
    private Button autoDeriveMasterBtn;
    private String activeLoadedBodyKey = null;

    /**
     * Constructs the planet section builder with the given host context.
     *
     * @param host host callback interface
     */
    public ResourcePlanetSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the planetary parameters UI section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildPlanetSection() {
        planetSectionHeader = new Label(I18n.getOrDefault("resource.section.planet_preset", "PLANET & SCIENTIFIC DEDUCTION (TAB 1)"));
        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);

        planetPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });

        planetPresetCombo.setButtonCell(new ListCell<PlanetPreset>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    PlanetPreset current = planetPresetCombo != null ? planetPresetCombo.getValue() : null;
                    setText(current != null ? I18n.getPlanetPresetDisplayName(current.name()) : I18n.getPlanetPresetDisplayName("Terre (Terran)"));
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });

        planetPresetCombo.setConverter(new javafx.util.StringConverter<PlanetPreset>() {
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? I18n.getPlanetPresetDisplayName("Terre (Terran)") : I18n.getPlanetPresetDisplayName(item.name());
            }

            @Override
            public PlanetPreset fromString(String string) {
                return null;
            }
        });

        planetPresetCombo.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-opacity: 1.0; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 4px;");
        planetPresetCombo.setOnAction(e -> {
            PlanetPreset selected = planetPresetCombo.getValue();
            if (selected != null) {
                host.setActivePlanetPreset(selected);
                adaptResourceSlidersToPlanet(selected);
                autoApplyMapsForPreset(selected);
                updatePlanetContextDisplay();
                host.updatePreviewCanvas();
            }
        });

        planetContextLabel = new Label(I18n.getOrDefault("resource.territory.earth", "🪐 Territory: Earth-Like Planet"));
        planetContextLabel.setWrapText(true);
        planetContextLabel.getStyleClass().add("planet-context-badge");

        syncPlanetBtn = new Button(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
        syncPlanetBtn.getStyleClass().add("button-secondary");
        syncPlanetBtn.setMaxWidth(Double.MAX_VALUE);
        syncPlanetBtn.setOnAction(e -> {
            Supplier<PlanetPreset> supplier = host.getPlanetPresetSupplier();
            PlanetPreset current = supplier != null ? supplier.get() : host.getActivePlanetPreset();
            if (current != null) {
                setActivePlanetPreset(current);
                adaptResourceSlidersToPlanet(current);
                updatePlanetContextDisplay();
                host.updatePreviewCanvas();
            }
        });

        autoDeriveMasterBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
        autoDeriveMasterBtn.getStyleClass().addAll("button-secondary", "button-accent-blue");
        autoDeriveMasterBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveMasterBtn.setOnAction(e -> {
            autoDeriveEcologyFromPlanet();
            autoDeriveHydroFromPlanet();
            autoDeriveGeologyFromPlanet();
        });

        thermoSynthesisBadge = new Label();
        thermoSynthesisBadge.setWrapText(true);
        thermoSynthesisBadge.getStyleClass().add("info-badge");

        planetSectionHeader.getStyleClass().add("label-section-header");
        VBox content = new VBox(8,
                new Label(I18n.getOrDefault("resource.label.planet_preset_select", "Inherited Celestial Body Preset:")),
                planetPresetCombo,
                planetContextLabel,
                host.getEcoCompatibilityLabel(),
                thermoSynthesisBadge,
                syncPlanetBtn,
                autoDeriveMasterBtn
        );
        VBox box = new VBox(8, planetSectionHeader, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    /**
     * Sets the active planet preset and adjusts selection.
     *
     * @param planetPreset target planet preset
     */
    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        host.setActivePlanetPreset(planetPreset);
        boolean oldState = host.isUpdatingFromPreset();
        host.setUpdatingFromPreset(true);
        try {
            if (planetPreset != null && planetPresetCombo != null) {
                PlanetPreset match = planetPresetCombo.getItems().stream()
                        .filter(p -> p.name() != null && p.name().equalsIgnoreCase(planetPreset.name()))
                        .findFirst().orElse(null);
                if (match != null) {
                    planetPresetCombo.setValue(match);
                } else {
                    planetPresetCombo.getItems().add(planetPreset);
                    planetPresetCombo.setValue(planetPreset);
                }
            }
            String bodyKey = detectBodyKey(planetPreset);
            if (!bodyKey.equalsIgnoreCase(activeLoadedBodyKey) || host.getBiomeSectionBuilder().getCustomBiomeImage() == null) {
                activeLoadedBodyKey = bodyKey;
                autoApplyMapsForPreset(planetPreset);
            }
            updatePlanetContextDisplay();
            host.updatePreviewCanvas();
        } finally {
            host.setUpdatingFromPreset(oldState);
        }
    }

    public String detectBodyKey(PlanetPreset p) {
        if (p == null) return "none";
        boolean isImportMode = p.elevationUseImport() || (p.customElevBase64() != null && !p.customElevBase64().isBlank()) || (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none"));
        if (!isImportMode) return "none";
        if (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none")) {
            return p.elevationMapSource().toLowerCase();
        }
        String lower = p.name() != null ? p.name().toLowerCase() : "";
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) return "earth";
        if (lower.contains("mars") || lower.contains("ares")) return "mars";
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) return "moon";
        if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) return "venus";
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) return "mercury";
        return "none";
    }

    public void autoDeriveEcologyFromPlanet() {
        host.getBiomeSectionBuilder().autoDeriveEcologyFromPlanet();
    }

    public void autoDeriveHydroFromPlanet() {
        host.getHydroSectionBuilder().autoDeriveHydroFromPlanet();
        host.updateSummary();
    }

    public void autoDeriveGeologyFromPlanet() {
        PlanetPreset p = host.getActivePlanetPreset() != null ? host.getActivePlanetPreset() : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        double radiusRatio = p.radiusKm() / 6371.0;
        double volumeScale = Math.pow(radiusRatio, 3);
        double crustal = Math.round(80.0 * volumeScale);
        double precious = Math.round(1200.0 * volumeScale);
        double heat = Math.round(Math.min(300.0, 87.0 * radiusRatio) * 10.0) / 10.0;
        double seismic = Math.round(Math.min(10.0, Math.max(0.0, p.seismicActivityLevel())) * 10.0) / 10.0;
        double volcanic = Math.round(Math.min(8.0, Math.max(0.0, p.volcanicActivityLevel())) * 10.0) / 10.0;

        if (host.getCrustalMetalSlider() != null) host.getCrustalMetalSlider().setValue(crustal);
        if (host.getPreciousMetalSlider() != null) host.getPreciousMetalSlider().setValue(precious);
        if (host.getMantleHeatSlider() != null) host.getMantleHeatSlider().setValue(heat);
        if (host.getSeismicActivitySlider() != null) host.getSeismicActivitySlider().setValue(seismic);
        if (host.getVolcanicActivitySlider() != null) host.getVolcanicActivitySlider().setValue(volcanic);

        if (host.getEcologyPresetBar() != null) host.getEcologyPresetBar().notifyParametersChanged();
        host.updateSummary();
        host.updatePreviewCanvas();
        logger.info("Auto-derived geological parameters for planet {}", p.name());
    }

    public void updatePlanetContextDisplay() {
        if (planetContextLabel == null) return;
        PlanetPreset preset = host.getActivePlanetPreset() != null ? host.getActivePlanetPreset() : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        int cellCount = host.getActiveCellCount();
        planetContextLabel.setText(ResourcePlanetSyncManager.buildPlanetContextText(preset, cellCount));
        updateThermodynamicSynthesisBadge(preset);
    }

    private void updateThermodynamicSynthesisBadge(PlanetPreset p) {
        if (thermoSynthesisBadge == null) return;
        thermoSynthesisBadge.setText(ResourcePlanetSyncManager.buildThermodynamicSynthesisText(p));
    }

    public void adaptResourceSlidersToPlanet(PlanetPreset p) {
        if (p == null) return;
        boolean oldState = host.isUpdatingFromPreset();
        host.setUpdatingFromPreset(true);
        try {
            ResourcePlanetSyncManager.adaptResourceSlidersToPlanet(
                    p, host.getBiomeSectionBuilder(), host.getHydroSectionBuilder(), host.getCrustalMetalSlider(),
                    host.getPreciousMetalSlider(), host.getMantleHeatSlider(), host.getSeismicActivitySlider(), host.getVolcanicActivitySlider()
            );
        } finally {
            host.setUpdatingFromPreset(oldState);
        }
        host.updateSummary();
    }

    public void autoApplyMapsForPreset(PlanetPreset p) {
        if (p == null) return;
        boolean isImportMode = p.elevationUseImport() || (p.customElevBase64() != null && !p.customElevBase64().isBlank()) || (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none"));
        if (!isImportMode) {
            applyPresetMapSource("none");
            return;
        }
        String lower = p.name() != null ? p.name().toLowerCase() : "";
        String sourceKey;
        if (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none")) {
            sourceKey = p.elevationMapSource().toLowerCase();
        } else if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) {
            sourceKey = "earth";
        } else if (lower.contains("mars") || lower.contains("ares")) {
            sourceKey = "mars";
        } else if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) {
            sourceKey = "moon";
        } else if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) {
            sourceKey = "venus";
        } else if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) {
            sourceKey = "mercury";
        } else {
            applyPresetMapSource("none");
            return;
        }
        host.setUpdatingFromPreset(true);
        if (host.getBiomeSectionBuilder().getBiomeSourceCombo() != null) host.getBiomeSectionBuilder().getBiomeSourceCombo().setValue(sourceKey);
        if (host.getHydroSectionBuilder().getHydroSourceCombo() != null) host.getHydroSectionBuilder().getHydroSourceCombo().setValue(sourceKey);
        if (host.getClimateSectionBuilder().getClimateSourceCombo() != null) host.getClimateSectionBuilder().getClimateSourceCombo().setValue(sourceKey);
        host.setUpdatingFromPreset(false);
        applyPresetMapSource(sourceKey);
    }

    public void applyPresetMapSource(String sourceKey) {
        if ("none".equals(sourceKey)) {
            host.getBiomeSectionBuilder().applyPresetMapSource(sourceKey, "none");
            if (host.getResourceFileLabel() != null) host.getResourceFileLabel().setText(I18n.get("planet.map.none"));
            host.getHydroSectionBuilder().applyPresetMapSource(sourceKey, "none");
            host.getClimateSectionBuilder().applyPresetMapSource(sourceKey, "none");
            Label ecoCompatibilityLabel = host.getEcoCompatibilityLabel();
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.no_map", "🪐 No external map loaded — Procedural mode active"));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-neutral");
            }
            host.updatePreviewCanvas();
            host.updateSummary();
            return;
        }
        if (!validateTerrainMapCompatibility(sourceKey)) {
            return;
        }
        String body = sourceKey.toLowerCase();
        host.getBiomeSectionBuilder().applyPresetMapSource(sourceKey, body);
        host.getHydroSectionBuilder().applyPresetMapSource(sourceKey, body);
        host.getClimateSectionBuilder().applyPresetMapSource(sourceKey, body);
        if (host.getResourceFileLabel() != null) host.getResourceFileLabel().setText(host.getGeologySourceDisplayName(body));
        host.prepopulateGeologyTensors(body);
        Label ecoCompatibilityLabel = host.getEcoCompatibilityLabel();
        if (ecoCompatibilityLabel != null) {
            ecoCompatibilityLabel.setText(String.format("🪐 %s authentic maps loaded (Biomes, Hydro, Climate & 9 Geology Tensors)", body.toUpperCase()));
            ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
        }
        host.updatePreviewCanvas();
        host.updateSummary();
    }

    public boolean validateTerrainMapCompatibility(String sourceKey) {
        if ("none".equals(sourceKey) || sourceKey == null) return true;
        PlanetPreset activePreset = host.getActivePlanetPreset() != null ? host.getActivePlanetPreset() : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        boolean isCompatible = ResourcePlanetSyncManager.isTerrainMapCompatible(sourceKey, activePreset);
        Label ecoCompatibilityLabel = host.getEcoCompatibilityLabel();
        if (!isCompatible) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.incompatible_map", "⚠️ Incompatibilité : Carte « %s » sur terrain « %s » (Relief non concordant)"), sourceKey.toUpperCase(), activeDisplayName));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-warning");
            }
            return false;
        }
        if (ecoCompatibilityLabel != null) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.compatible_map", "✅ Carte « %s » vérifiée et compatible avec le terrain « %s »"), sourceKey.toUpperCase(), activeDisplayName));
            ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
        }
        return true;
    }

    public void updateTexts() {
        if (planetSectionHeader != null) planetSectionHeader.setText(I18n.getOrDefault("resource.section.planet_preset", "PLANETARY PARAMETERS"));
        if (syncPlanetBtn != null) syncPlanetBtn.setText(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
        if (autoDeriveMasterBtn != null) autoDeriveMasterBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
        updatePlanetContextDisplay();
    }

    public ComboBox<PlanetPreset> getPlanetPresetCombo() {
        return planetPresetCombo;
    }

    public String getActiveLoadedBodyKey() {
        return activeLoadedBodyKey;
    }

    public void setActiveLoadedBodyKey(String key) {
        this.activeLoadedBodyKey = key;
    }
}
