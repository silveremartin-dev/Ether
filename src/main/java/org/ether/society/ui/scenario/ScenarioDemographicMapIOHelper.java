/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.WindowHelper;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Helper delegate providing demographic settlement pattern catalogs, raster map import/export,
 * compatibility validation, and {@link Scenario} persistence for {@link ScenarioDemographicSectionBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class ScenarioDemographicMapIOHelper {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioDemographicMapIOHelper.class);

    static final Map<String, String> DENSITY_LABELS = Map.ofEntries(
            Map.entry("EARTH_EMPIRICAL_RASTER", "ðŸŒ Pre-Generated Earth Empirical Raster (HYDE 3.4 / Paleo)"),
            Map.entry("UNBIASED_NATURAL", "âš– Natural Bio-Climatic Equilibrium (Unbiased)"),
            Map.entry("COASTAL_MARITIME", "ðŸŒŠ Coastal & Maritime Focus"),
            Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins"),
            Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs"),
            Map.entry("INLAND_OASIS", "ðŸŒ´ Inland Hydrographic Basins & Oases"),
            Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt & Tropical Zone"),
            Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters"),
            Map.entry("SPARSE_NOMADIC", "â›º Sparse Nomadic & Pastoralist Dispersion"),
            Map.entry("UNIFORM", "ðŸŸ¦ Absolute Uniform Distribution"),
            Map.entry("RANDOM", "ðŸŽ² Stochastic Random Distribution")
    );

    static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
            Map.entry("EARTH_EMPIRICAL_RASTER", "ðŸŒ Pre-Generated Earth Empirical Raster: Authoritative cartographic raster precomputed from HYDE 3.4, EPICA Dome C, and Paleolithic archaeological hearths."),
            Map.entry("UNBIASED_NATURAL", "âš– Natural Bio-Climatic Equilibrium: Pure physical model without artificial bias. Population settles strictly according to real environmental suitability (biomes, temperature, waterways, relief)."),
            Map.entry("COASTAL_MARITIME", "ðŸŒŠ Coastal & Maritime Focus: Encourages human settlement along ocean coastlines, river deltas, and maritime margins."),
            Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins: High population concentration along major hydrographic river networks and fertile alluvial basins."),
            Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs: Preferential densification in high valleys, mountain plateaus, and defensive elevated reliefs."),
            Map.entry("INLAND_OASIS", "ðŸŒ´ Inland Basins & Oases: Settlement around endorheic depressions and accessible groundwater tables."),
            Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt: Primary settlement concentrated around equatorial latitudes and high solar irradiance zones."),
            Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters: Procedural emergence of multiple dense urban aggregation hubs."),
            Map.entry("SPARSE_NOMADIC", "â›º Sparse Nomadic Dispersion: Low-density pastoral population dispersed broadly across all viable biomes."),
            Map.entry("UNIFORM", "ðŸŸ¦ Absolute Uniform Distribution: Strictly constant population density across all land cells on the H3 grid."),
            Map.entry("RANDOM", "ðŸŽ² Stochastic Distribution: Uniformly random population allocation across land cells.")
    );

    private ScenarioDemographicMapIOHelper() {
        // Utility delegate class
    }

    /**
     * Configures cell factories, string converters, and localized tooltips for the settlement pattern combo box.
     *
     * @param densityPatternCombo target settlement pattern combo box
     * @param context             host panel context
     */
    static void configureDensityPatternCombo(
            ComboBox<String> densityPatternCombo,
            ScenarioDemographicSectionBuilder.HostContext context) {
        densityPatternCombo.getItems().addAll(
                "UNBIASED_NATURAL", "COASTAL_MARITIME", "RIVER_VALLEYS", "HIGHLAND_MOUNTAIN",
                "INLAND_OASIS", "EQUATORIAL_BELT", "URBAN_CLUSTERS", "SPARSE_NOMADIC", "UNIFORM", "RANDOM");
        densityPatternCombo.setValue("UNBIASED_NATURAL");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String item) {
                return item == null ? "" : I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item));
            }
            @Override
            public String fromString(String string) {
                return null;
            }
        });
        densityPatternCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
                    String desc = I18n.getOrDefault("scenario.density.desc." + item, DENSITY_DESCRIPTIONS.getOrDefault(item, ""));
                    setTooltip(desc.isBlank() ? null : new Tooltip(desc));
                }
            }
        });
        densityPatternCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
            }
        });
        densityPatternCombo.setTooltip(new Tooltip(
                I18n.getOrDefault("scenario.density.desc.UNBIASED_NATURAL", DENSITY_DESCRIPTIONS.get("UNBIASED_NATURAL"))));
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (newV != null) {
                densityPatternCombo.setTooltip(new Tooltip(
                        I18n.getOrDefault("scenario.density.desc." + newV, DENSITY_DESCRIPTIONS.getOrDefault(newV, ""))));
            }
        });
    }

    /**
     * Configures cell factories and visibility bindings for the initial technological/capital preset combo box.
     *
     * @param techPresetCombo        target tech preset combo box
     * @param customPhysicalSubPanel sub-panel revealed only when {@link Scenario.TechPreset#CUSTOM} is selected
     * @param context                host panel context
     */
    static void configureTechPresetCombo(
            ComboBox<Scenario.TechPreset> techPresetCombo,
            VBox customPhysicalSubPanel,
            ScenarioDemographicSectionBuilder.HostContext context) {
        techPresetCombo.getItems().setAll(Scenario.TechPreset.values());
        techPresetCombo.setValue(Scenario.TechPreset.AUTO_FROM_YEAR);
        techPresetCombo.setMaxWidth(Double.MAX_VALUE);
        techPresetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Scenario.TechPreset item) {
                return item == null ? "" : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel());
            }
            @Override
            public Scenario.TechPreset fromString(String string) {
                return null;
            }
        });
        techPresetCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Scenario.TechPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel()));
            }
        });
        techPresetCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Scenario.TechPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel()));
            }
        });
        techPresetCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tech_preset",
                "Select initial physical endowment (Kâ‚€, Eâ‚€, Fâ‚€, Iâ‚€) or leave auto-calculated based on Tâ‚€ year.")));
        techPresetCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            boolean isCustom = (newV == Scenario.TechPreset.CUSTOM);
            customPhysicalSubPanel.setVisible(isCustom);
            customPhysicalSubPanel.setManaged(isCustom);
        });
    }

    /**
     * Builds the reference demographic data source selector combo box.
     *
     * @param builder owning demographic section builder
     * @param context host panel context
     * @return configured JavaFX {@link ComboBox}
     */
    static ComboBox<String> buildPopulationSourceCombo(
            ScenarioDemographicSectionBuilder builder,
            ScenarioDemographicSectionBuilder.HostContext context) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().addAll(
                "",
                "ðŸŒ Terre â€” HYDE 3.4 / Grille Historique AnthropocÃ¨ne [Global, -10 000 BC Ã  +2023 AD]",
                "ðŸŒ Terre â€” PalÃ©o-DÃ©mographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC Ã  -10 000 BC]",
                "ðŸŒ Terre â€” CShapes & Centennia Reconstitutions DÃ©mographiques [Empires & Ã‰tats, -3000 BC Ã  +2000 AD]",
                "ðŸ”´ Mars â€” ModÃ¨le de Colonisation Spatiale & DÃ´mes d'Habitation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Stations AÃ©rostatiques Cloud Cities (Altitude 50 km) [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]",
                "âšª Lune â€” Bases SÃ©lÃ©nites Sous-Terraines & CratÃ¨res Shackleton [PlanÃ©taire (Lune), +2040 AD Ã  Futur]",
                "âšª Mercure â€” DÃ´mes Polaires & Habitats d'Ombre Permanente [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
        );
        DataSourceUIHelper.setupDetailedSourceCombo(combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        String initSrc = pickDemoSourceForEpoch(context.getStartYear(), context.getActivePlanetPreset());
        DataSourceUIHelper.selectBestSource(combo, initSrc, "Terre");
        combo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (builder.getCustomDensityImage() != null && builder.getRadioImportDemo() != null) {
                    builder.getRadioImportDemo().setSelected(true);
                }
                context.notifyParamChange();
                context.drawPreview();
            }
        });
        return combo;
    }

    /**
     * Selects the canonical demographic reference dataset matching a planetary preset and epoch year.
     *
     * @param startYear simulation initial epoch year
     * @param planet    active planetary preset
     * @return reference source label string
     */
    static String pickDemoSourceForEpoch(long startYear, PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares"))
            return "ðŸ”´ Mars â€” ModÃ¨le de Colonisation Spatiale & DÃ´mes d'Habitation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
        if (pName.contains("vÃ©nus") || pName.contains("venus") || pName.contains("hesperos"))
            return "ðŸŸ¡ VÃ©nus â€” Stations AÃ©rostatiques Cloud Cities (Altitude 50 km) [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene"))
            return "âšª Lune â€” Bases SÃ©lÃ©nites Sous-Terraines & CratÃ¨res Shackleton [PlanÃ©taire (Lune), +2040 AD Ã  Futur]";
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes"))
            return "âšª Mercure â€” DÃ´mes Polaires & Habitats d'Ombre Permanente [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
        if (startYear < -10000)
            return "ðŸŒ Terre â€” PalÃ©o-DÃ©mographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC Ã  -10 000 BC]";
        if (startYear >= -3000 && startYear < 1900)
            return "ðŸŒ Terre â€” CShapes & Centennia Reconstitutions DÃ©mographiques [Empires & Ã‰tats, -3000 BC Ã  +2000 AD]";
        return "ðŸŒ Terre â€” HYDE 3.4 / Grille Historique AnthropocÃ¨ne [Global, -10 000 BC Ã  +2023 AD]";
    }

    /**
     * Prompts the user to load an equirectangular PNG/JPEG population density map and validates its aspect ratio.
     *
     * @param builder owning demographic section builder
     * @param context host panel context
     */
    static void loadCustomDensityMap(
            ScenarioDemographicSectionBuilder builder,
            ScenarioDemographicSectionBuilder.HostContext context) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.load_density_dialog", "Load Population Density Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
                if (!val.valid()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18n.getOrDefault("scenario.dialog.invalid_map_title", "Carte Incompatible ou Invalide"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supportÃ© ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n"
                            + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection Ã©quirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                builder.setCustomDensityImage(img);
                if (builder.getDensityMapFileLabel() != null) {
                    builder.getDensityMapFileLabel().setText(String.format("ðŸ“· %s (%dx%d)", file.getName(), val.width(), val.height()));
                }
                if (builder.getRadioImportDemo() != null) builder.getRadioImportDemo().setSelected(true);
                updateDemoCompatibilityDisplay(builder, context);
                if (context.getPreviewCells() != null) {
                    context.distributeInitialPopulation(context.getPreviewCells());
                    context.drawPreview();
                }
            } catch (Exception ex) {
                logger.error("Failed to load custom density map", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Updates the terrain compatibility diagnostic label for the imported density raster.
     *
     * @param builder owning demographic section builder
     * @param context host panel context
     */
    static void updateDemoCompatibilityDisplay(
            ScenarioDemographicSectionBuilder builder,
            ScenarioDemographicSectionBuilder.HostContext context) {
        Label compatLbl = builder.getDemoCompatibilityLabel();
        if (compatLbl == null) return;
        PlanetPreset p = context.getActivePlanetPreset();
        String planetName = p != null ? p.name() : "Standard";
        Image img = builder.getCustomDensityImage();
        if (img != null) {
            ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
            if (!val.valid()) {
                compatLbl.setText(String.format(
                        I18n.getOrDefault("scenario.demo.map_invalid", "âš ï¸ Carte dÃ©mographique incompatible : %s"), val.message()));
                compatLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                compatLbl.setText(String.format(
                        I18n.getOrDefault("scenario.demo.map_loaded", "âœ… Map loaded and compatible with selected Tab 1 world (%s)"), planetName));
                compatLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        } else {
            compatLbl.setText(I18n.getOrDefault("scenario.demo.no_map", "ðŸª No external map loaded â€” Procedural mode active"));
            compatLbl.getStyleClass().add("subcard-status-muted");
            compatLbl.setStyle("-fx-font-size: 11px;");
        }
    }

    /**
     * Exports the active demographic density map as a 2048x1024 equirectangular raster file.
     *
     * @param customDensityImage optional imported custom density image
     * @param context            host panel context
     */
    static void exportDensityMap(
            Image customDensityImage,
            ScenarioDemographicSectionBuilder.HostContext context) {
        int w = 2048;
        int h = 1024;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();

        if (customDensityImage != null && customDensityImage.getWidth() > 0) {
            PixelReader pr = customDensityImage.getPixelReader();
            double srcW = customDensityImage.getWidth();
            double srcH = customDensityImage.getHeight();
            for (int y = 0; y < h; y++) {
                int sy = (int) Math.clamp((y / (double) h) * srcH, 0, srcH - 1);
                for (int x = 0; x < w; x++) {
                    int sx = (int) Math.clamp((x / (double) w) * srcW, 0, srcW - 1);
                    pw.setColor(x, y, pr.getColor(sx, sy));
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, Color.rgb(15, 23, 42));
                }
            }
            if (context.getPreviewCells() != null && !context.getPreviewCells().isEmpty()) {
                double scaleX = w / 360.0;
                double scaleY = h / 180.0;
                for (H3Cell c : context.getPreviewCells()) {
                    int px = (int) ((c.getLongitude() + 180.0) * scaleX);
                    int py = (int) ((90.0 - c.getLatitude()) * scaleY);
                    if (px >= 0 && px < w && py >= 0 && py < h) {
                        pw.setColor(px, py, ScenarioCellPopulationDistributor.getReliefAndDensityColor(c));
                    }
                }
            }
        }
        WindowHelper.exportImageWithChooser(
                context.getWindow(),
                image,
                "ether-population-density.png",
                I18n.getOrDefault("scenario.title.export_density_dialog", "Export Population Density Map (PNG / JPEG)"));
    }

    /**
     * Displays the scrollable help specification modal for demographic density raster imports.
     */
    static void showDensityImportFormatHelp() {
        WindowHelper.showScrollableInfoDialog(
                I18n.getOrDefault("scenario.dialog.density_help_title", "SpÃ©cifications de la Carte de DensitÃ©"),
                I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'Image SupportÃ©s pour l'Import de DensitÃ© DÃ©mographique"),
                "Vous pouvez importer une carte de rÃ©partition dÃ©mographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection Ã©quirectangulaire) :\n\n"
                        + "1. CARTE DE DENSITÃ‰ NIVEAUX DE GRIS :\n"
                        + "   â€¢ Noir (0) = 0 hab/kmÂ² (Zone inhabitÃ©e / dÃ©sertique / aquatique)\n"
                        + "   â€¢ Blanc (255) = DensitÃ© maximale d'habitation (Foyer urbain ou mÃ©tropole)\n\n"
                        + "2. INTÃ‰GRATION AVEC LE TERRAIN ET L'Ã‰COLOGIE :\n"
                        + "   â€¢ Les zones d'eau (ocÃ©ans/mers) dÃ©finies dans l'onglet 1 sont automatiquement filtrÃ©es pour Ã©viter l'apparition de populations en pleine mer.\n"
                        + "   â€¢ La population totale paramÃ©trÃ©e dans le spinner est distribuÃ©e au prorata de la luminositÃ© de chaque pixel."
        );
    }

    /**
     * Applies demographic parameters from a {@link Scenario} into the UI controls.
     *
     * @param s       source scenario model
     * @param builder target demographic section builder
     * @param context host panel context
     */
    static void applyScenario(
            Scenario s,
            ScenarioDemographicSectionBuilder builder,
            ScenarioDemographicSectionBuilder.HostContext context) {
        if (s == null) return;
        if (builder.getInitialHumanCountSpinner() != null && builder.getInitialHumanCountSpinner().getValueFactory() != null) {
            builder.getInitialHumanCountSpinner().getValueFactory().setValue(s.getInitialHumanCount());
        }
        if (s.getPopulationDensityType() != null && builder.getDensityPatternCombo() != null
                && builder.getDensityPatternCombo().getItems().contains(s.getPopulationDensityType())) {
            builder.getDensityPatternCombo().setValue(s.getPopulationDensityType());
        }
        if (builder.getTechPresetCombo() != null && s.getTechPreset() != null) {
            builder.getTechPresetCombo().setValue(s.getTechPreset());
        }
        if (builder.getCustomCapitalSpinner() != null && builder.getCustomCapitalSpinner().getValueFactory() != null) {
            builder.getCustomCapitalSpinner().getValueFactory().setValue(s.getInitialCapitalPerCapita());
        }
        if (builder.getCustomEnergySpinner() != null && builder.getCustomEnergySpinner().getValueFactory() != null) {
            builder.getCustomEnergySpinner().getValueFactory().setValue(s.getInitialEnergyPerCapita());
        }
        if (builder.getCustomFoodSpinner() != null && builder.getCustomFoodSpinner().getValueFactory() != null) {
            builder.getCustomFoodSpinner().getValueFactory().setValue(s.getInitialFoodReserveMonths());
        }
        if (builder.getCustomInfoSpinner() != null && builder.getCustomInfoSpinner().getValueFactory() != null) {
            builder.getCustomInfoSpinner().getValueFactory().setValue(s.getInitialInformationPerCapita());
        }

        boolean isEarth = s.getPlanetPreset() != null
                && ("earth".equalsIgnoreCase(s.getPlanetPreset().getCanonicalPlanet())
                || "earth".equalsIgnoreCase(s.getPlanetPreset().elevationMapSource()));
        if (isEarth && (s.getCustomDensityBase64() == null || s.getCustomDensityBase64().isBlank())) {
            org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(s);
        }

        if (s.getCustomDensityBase64() != null && !s.getCustomDensityBase64().isBlank()) {
            try {
                builder.setCustomDensityImage(ImageMapLoader.base64PngToImage(s.getCustomDensityBase64()));
                if (builder.getDensityMapFileLabel() != null) {
                    builder.getDensityMapFileLabel().setText(isEarth ? "earth_" + s.getStartDateYear() + "_density.png" : "Embedded Base64");
                }
                if (builder.getRadioImportDemo() != null) builder.getRadioImportDemo().setSelected(true);
            } catch (Exception ex) {
                logger.warn("Could not decode scenario embedded density map", ex);
            }
        } else {
            builder.setCustomDensityImage(null);
            if (builder.getDensityMapFileLabel() != null) builder.getDensityMapFileLabel().setText("â€”");
            if (builder.getRadioProcDemo() != null) builder.getRadioProcDemo().setSelected(true);
        }
        updateDemoCompatibilityDisplay(builder, context);
    }

    /**
     * Collects UI demographic parameters and custom Base64 density map into a {@link Scenario} instance.
     *
     * @param s       target scenario model
     * @param builder source demographic section builder
     */
    static void collectParameters(Scenario s, ScenarioDemographicSectionBuilder builder) {
        if (s == null) return;
        if (builder.getInitialHumanCountSpinner() != null && builder.getInitialHumanCountSpinner().getValue() != null) {
            s.setInitialHumanCount(builder.getInitialHumanCountSpinner().getValue());
        }
        if (builder.getRadioImportDemo() != null && builder.getRadioImportDemo().isSelected()) {
            boolean isEarth = s.getPlanetPreset() != null && "earth".equalsIgnoreCase(s.getPlanetPreset().getCanonicalPlanet());
            if (isEarth) {
                s.setPopulationDensityType("EARTH_EMPIRICAL_RASTER");
            }
        } else if (builder.getDensityPatternCombo() != null && builder.getDensityPatternCombo().getValue() != null) {
            s.setPopulationDensityType(builder.getDensityPatternCombo().getValue());
        }
        if (builder.getTechPresetCombo() != null && builder.getTechPresetCombo().getValue() != null) {
            s.setTechPreset(builder.getTechPresetCombo().getValue());
        }
        if (builder.getCustomCapitalSpinner() != null && builder.getCustomCapitalSpinner().getValue() != null) {
            s.setInitialCapitalPerCapita(builder.getCustomCapitalSpinner().getValue());
        }
        if (builder.getCustomEnergySpinner() != null && builder.getCustomEnergySpinner().getValue() != null) {
            s.setInitialEnergyPerCapita(builder.getCustomEnergySpinner().getValue());
        }
        if (builder.getCustomFoodSpinner() != null && builder.getCustomFoodSpinner().getValue() != null) {
            s.setInitialFoodReserveMonths(builder.getCustomFoodSpinner().getValue());
        }
        if (builder.getCustomInfoSpinner() != null && builder.getCustomInfoSpinner().getValue() != null) {
            s.setInitialInformationPerCapita(builder.getCustomInfoSpinner().getValue());
        }

        if (builder.getCustomDensityImage() != null && builder.isDensityImportActive()) {
            s.setCustomDensityBase64(ImageMapLoader.imageToBase64Png(builder.getCustomDensityImage()));
        } else {
            s.setCustomDensityBase64(null);
        }
    }
}

