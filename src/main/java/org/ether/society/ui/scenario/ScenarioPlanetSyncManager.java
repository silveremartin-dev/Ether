/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;

import java.util.Objects;

/**
 * Manages the synchronisation of planetary astrophysical parameters (Tab 1) and ecological
 * presets (Tab 2) into the Scenario configuration (Tab 3).
 *
 * <p>Ensures that planetary radius, gravity, albedo, and epoch-specific elevation rasters
 * are cascaded deterministically into civilizational initial conditions.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioPlanetSyncManager {

    /**
     * Host context contract connecting the planet sync manager to the parent scenario setup panel.
     */
    public interface HostContext {
        void notifyParamChange();
        void generatePreview();
        void drawPreview();
        boolean hasPreviewCells();
    }

    private final HostContext host;

    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private ComboBox<EcologyPreset> ecologyPresetCombo;
    private Label inheritedContextLabel;
    private Label ecoPromptLabel;
    private Label planetPromptLabel;

    private PlanetPreset activePlanetPreset = PlanetPreset.EARTH_LIKE;

    /**
     * Constructs a scenario planet synchronization manager.
     *
     * @param host callback context
     */
    public ScenarioPlanetSyncManager(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the inherited planetary and ecological preset UI section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        planetSectionHeader = new Label(I18n.getOrDefault("scenario.section.inherited", "🪐 INHERITED CONTEXT (TABS 1 & 2)"));
        planetSectionHeader.getStyleClass().add("label-section-header");

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
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
        planetPresetCombo.setButtonCell(new ListCell<>() {
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
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-opacity: 1.0; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 4px;");
        planetPresetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? I18n.getPlanetPresetDisplayName("Terre (Terran)") : I18n.getPlanetPresetDisplayName(item.name());
            }

            @Override
            public PlanetPreset fromString(String string) {
                return null;
            }
        });
        planetPresetCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.planet_preset_disabled", "Inherited planetary preset automatically derived from chosen ecology (Tab 2).")));
        planetPresetCombo.setDisable(true);

        ecologyPresetCombo = new ComboBox<>();
        ecologyPresetCombo.getItems().setAll(EcologyPreset.getBuiltInPresets());
        ecologyPresetCombo.setValue(EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetCombo.setMaxWidth(Double.MAX_VALUE);
        ecologyPresetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(EcologyPreset item) {
                return item == null ? "" : I18n.getPlanetPresetDisplayName(item.name());
            }

            @Override
            public EcologyPreset fromString(String string) {
                return null;
            }
        });
        ecologyPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                    String desc = item.getPresetDescription();
                    if (desc != null && !desc.isBlank()) {
                        Tooltip tip = new Tooltip(desc);
                        tip.setWrapText(true);
                        tip.setMaxWidth(450);
                        setTooltip(tip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        ecologyPresetCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    EcologyPreset current = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
                    setText(current != null ? I18n.getPlanetPresetDisplayName(current.name()) : "");
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        ecologyPresetCombo.setOnAction(e -> {
            EcologyPreset selected = ecologyPresetCombo.getValue();
            if (selected != null) {
                PlanetPreset cascadedPlanet = findPlanetPresetByName(selected.planetPresetName());
                if (cascadedPlanet != null) {
                    this.activePlanetPreset = cascadedPlanet;
                    planetPresetCombo.setValue(cascadedPlanet);
                }
                updateInheritedContextDisplay(selected.name());
                if (host != null) {
                    host.notifyParamChange();
                }
            }
        });

        inheritedContextLabel = new Label(I18n.getOrDefault("scenario.info.earth_derived", "🌿 Ecology: Earth Standard Baseline  ➔  🪐 Planet (derived): Earth (Terran)"));
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        ecoPromptLabel = new Label(I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):"));
        planetPromptLabel = new Label(I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 — Cascaded from Ecology):"));

        VBox content = new VBox(8,
                ecoPromptLabel,
                ecologyPresetCombo,
                planetPromptLabel,
                planetPresetCombo,
                inheritedContextLabel
        );

        VBox box = new VBox(8, planetSectionHeader, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    /**
     * Resolves the closest PlanetPreset matching a given identifier or canonical keyword.
     *
     * @param name preset identifier string
     * @return best matching PlanetPreset
     */
    public PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return PlanetPreset.EARTH_LIG_100000BP;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return PlanetPreset.EARTH_MIS3_50000BP;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire") || lower.contains("solutrean") || lower.contains("solutréen")) return PlanetPreset.EARTH_LGM_20000BP;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene") || lower.contains("dryas")) return PlanetPreset.EARTH_EH_10000BP;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return PlanetPreset.EARTH_MH_6000BP;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return PlanetPreset.EARTH_LH_3000BP;
        if (lower.contains("-1900") || lower.contains("bronze")) return PlanetPreset.EARTH_BRONZE_1900BP;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return PlanetPreset.EARTH_IRON_1000BP;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
        }
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
        }
        return PlanetPreset.EARTH_LIKE;
    }

    /**
     * Updates inherited context from Tab 1 and Tab 2.
     *
     * @param planetPreset incoming planet preset
     * @param ecologyName incoming ecology name
     */
    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
        boolean presetChanged = false;
        if (planetPreset != null) {
            if (!planetPreset.equals(this.activePlanetPreset)) {
                this.activePlanetPreset = planetPreset;
                presetChanged = true;
            }
        } else {
            EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
            if (activeEco != null) {
                PlanetPreset found = findPlanetPresetByName(activeEco.planetPresetName());
                if (!Objects.equals(found, this.activePlanetPreset)) {
                    this.activePlanetPreset = found;
                    presetChanged = true;
                }
            }
        }
        if (this.activePlanetPreset == null) {
            this.activePlanetPreset = PlanetPreset.EARTH_LIKE;
            presetChanged = true;
        }

        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(this.activePlanetPreset);
        }
        if (ecologyName != null && ecologyPresetCombo != null) {
            for (EcologyPreset eco : ecologyPresetCombo.getItems()) {
                if (eco.name().equalsIgnoreCase(ecologyName)) {
                    ecologyPresetCombo.setValue(eco);
                    break;
                }
            }
        }
        updateInheritedContextDisplay(ecologyName);
        if (host != null) {
            if (presetChanged || !host.hasPreviewCells()) {
                host.generatePreview();
            } else {
                host.drawPreview();
            }
        }
    }

    /**
     * Updates the summary label displaying inherited ecological and planetary attributes.
     *
     * @param ecologyName ecology preset name
     */
    public void updateInheritedContextDisplay(String ecologyName) {
        if (inheritedContextLabel == null) return;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
        String ecoDisplay = eco != null ? I18n.getPlanetPresetDisplayName(eco.name()) : (ecologyName != null ? I18n.getPlanetPresetDisplayName(ecologyName) : "Earth Standard Baseline");
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
        if (p == null && eco != null) {
            p = findPlanetPresetByName(eco.planetPresetName());
        }
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        String planetDisplay = I18n.getPlanetPresetDisplayName(p.name());

        String fmt = I18n.getOrDefault("scenario.info.inherited_context_format", "🌿 Ecology (Tab 2): %s  ➔  🪐 Planet (cascaded): %s (Radius: %,.0f km)");
        inheritedContextLabel.setText(String.format(fmt, ecoDisplay, planetDisplay, p.radiusKm()));
    }

    /**
     * Resolves the topographic elevation image associated with the preset.
     *
     * @param planet planet configuration
     * @return JavaFX Image or null
     */
    public Image getElevationImageForPreset(PlanetPreset planet) {
        if (planet == null) planet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            return ImageMapLoader.base64PngToImage(planet.customElevBase64());
        }
        String pSource = planet.elevationMapSource();
        if (pSource == null || pSource.isBlank() || "none".equalsIgnoreCase(pSource)) {
            pSource = planet.getCanonicalPlanet();
        }
        long yr = planet.getAssociatedEpochYear();
        Image img = ImageMapLoader.loadMapImage(pSource, yr, "elevation.png");
        if (img == null && "earth".equalsIgnoreCase(pSource)) {
            img = ImageMapLoader.loadMapImage("earth", 2026L, "elevation.png");
        }
        return img;
    }

    /**
     * Resets planetary configuration to canonical Earth preset.
     */
    public void applyEarthPreset() {
        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        }
    }

    /**
     * Updates localized UI strings for inherited context controls.
     */
    public void updateTexts() {
        if (planetSectionHeader != null) planetSectionHeader.setText(I18n.getOrDefault("scenario.section.inherited", "🪐 INHERITED CONTEXT (TABS 1 & 2)"));
        if (ecoPromptLabel != null) ecoPromptLabel.setText(I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):"));
        if (planetPromptLabel != null) planetPromptLabel.setText(I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 — Cascaded from Ecology):"));
        if (planetPresetCombo != null && planetPresetCombo.getCellFactory() != null) {
            planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
        }
        if (ecologyPresetCombo != null && ecologyPresetCombo.getCellFactory() != null) {
            ecologyPresetCombo.setButtonCell(ecologyPresetCombo.getCellFactory().call(null));
        }
        updateInheritedContextDisplay(null);
    }

    public PlanetPreset getActivePlanetPreset() { return activePlanetPreset; }
    public void setActivePlanetPreset(PlanetPreset preset) { this.activePlanetPreset = preset; }
    public ComboBox<PlanetPreset> getPlanetPresetCombo() { return planetPresetCombo; }
    public ComboBox<EcologyPreset> getEcologyPresetCombo() { return ecologyPresetCombo; }
    public Label getInheritedContextLabel() { return inheritedContextLabel; }
}
