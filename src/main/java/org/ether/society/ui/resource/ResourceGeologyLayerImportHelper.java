/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.ResourceDepositMapReader;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.export.ResourceDistributionRasterExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Package-private helper for {@link ResourceGeologySectionBuilder} handling empirical planetary
 * layer pre-population, GIS file imports, reference source combos, format specification modals,
 * and 5-language localization refreshes.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ResourceGeologySectionBuilder
 */
final class ResourceGeologyLayerImportHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResourceGeologyLayerImportHelper.class);

    private ResourceGeologyLayerImportHelper() {
        // Stateless package-private helper
    }

    static void prepopulateGeologyTensors(ResourceGeologySectionBuilder b, String planetKey, long year) {
        if (planetKey == null || planetKey.isBlank() || "none".equalsIgnoreCase(planetKey)) {
            planetKey = "earth";
        }
        String body = planetKey.toLowerCase();
        try {
            String[] layerNames = {
                    "coal", "oil", "gas", "uranium", "helium3",
                    "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };

            for (int i = 0; i < layerNames.length; i++) {
                Image img = null;
                if ("earth".equals(body)) {
                    img = ImageMapLoader.loadMapImage("earth", year, layerNames[i]);
                }
                if (img == null || img.getWidth() <= 0) {
                    img = ImageMapLoader.loadMapImage(body + "_" + layerNames[i] + ".png");
                }
                if (img != null && img.getWidth() > 0) {
                    b.customGeologyLayerImages.put(i, img);
                } else {
                    BufferedImage bImg = switch (i) {
                        case 0 -> HistoricalMapGenerator.rasterizeCoalMap(body.toUpperCase(), null);
                        case 1 -> HistoricalMapGenerator.rasterizeOilMap(body.toUpperCase(), null);
                        case 2 -> HistoricalMapGenerator.rasterizeGasMap(body.toUpperCase(), null);
                        case 3 -> HistoricalMapGenerator.rasterizeUraniumMap(body.toUpperCase(), null);
                        case 4 -> HistoricalMapGenerator.rasterizeHelium3Map(body.toUpperCase(), null);
                        case 5 -> HistoricalMapGenerator.rasterizeIronCopperMap(body.toUpperCase(), null);
                        case 6 -> HistoricalMapGenerator.rasterizePreciousMetalsMap(body.toUpperCase(), null);
                        case 7 -> HistoricalMapGenerator.rasterizeRareEarthsMap(body.toUpperCase(), null);
                        case 8 -> HistoricalMapGenerator.rasterizeMantleHeatMap(body.toUpperCase(), null);
                        default -> HistoricalMapGenerator.rasterizeAquiferMap(body.toUpperCase(), null);
                    };
                    if (bImg != null) {
                        b.customGeologyLayerImages.put(i, ResourceGeologyProceduralRasterizer.bufferedImageToFXImage(bImg));
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                if (b.geologyFileLabels.containsKey(i)) {
                    b.geologyFileLabels.get(i).setText(b.getGeologyBaselineName(i, body));
                }
                if (b.geologyImportRadios.containsKey(i)) {
                    b.geologyImportRadios.get(i).setSelected(true);
                }
                ComboBox<String> cb = b.geologySourceCombos.get(i);
                if (cb != null) {
                    selectMatchingPlanetSource(cb, body);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to pre-populate geology tensors for {}", body, e);
        }
    }

    private static void selectMatchingPlanetSource(ComboBox<String> cb, String body) {
        boolean matched = false;
        for (String item : cb.getItems()) {
            if (item == null || item.isEmpty()) continue;
            String itmLow = item.toLowerCase();
            boolean matches = (body.equals("earth") && (itmLow.contains("earth") || itmLow.contains("terre") || itmLow.contains("terran")))
                    || (body.equals("mars") && itmLow.contains("mars"))
                    || (body.equals("venus") && (itmLow.contains("venus") || itmLow.contains("vénus")))
                    || (body.equals("moon") && (itmLow.contains("moon") || itmLow.contains("lune") || itmLow.contains("lunar") || itmLow.contains("lola")))
                    || (body.equals("mercury") && (itmLow.contains("mercury") || itmLow.contains("mercure") || itmLow.contains("messenger")))
                    || itmLow.contains(body);
            if (matches) {
                cb.setValue(item);
                matched = true;
                break;
            }
        }
        if (!matched && cb.getItems().size() > 1) {
            cb.setValue(cb.getItems().get(1));
        }
    }

    static void loadCustomGeologyLayer(ResourceGeologySectionBuilder b, int layerIdx, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.dialog.import_geology_title", "Import geological map ") + b.getGeologyTensorTitle(layerIdx));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Données Spatiales & Tenseurs (PNG, ASC, GeoJSON, TIF, JPG)", "*.png", "*.asc", "*.geojson", "*.json", "*.tif", "*.tiff", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(b.host.getWindow());
        if (file != null) {
            try {
                java.awt.Color fallbackCol = ResourceDistributionRasterExporter.getGeologyTensorAwtColor(layerIdx);
                BufferedImage bImg = ResourceDepositMapReader.readGeologicalDataset(file, 720, 360, fallbackCol);
                if (bImg != null) {
                    onLoaded.accept(ResourceGeologyProceduralRasterizer.bufferedImageToFXImage(bImg));
                } else {
                    onLoaded.accept(new Image(new FileInputStream(file)));
                }
            } catch (Exception ex) {
                logger.error("Failed to load geology layer {}", layerIdx, ex);
            }
        }
    }

    static void showGeologyImportFormatHelp(ResourceGeologySectionBuilder b) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.getOrDefault("resource.title.geology_help", "Geological Layer Specifications & Formats"));
        alert.setHeaderText(I18n.getOrDefault("resource.header.geology_help", "🗺️ SPECIFICATIONS FOR 10 GEOLOGICAL TENSORS (GIS & RASTERS)"));

        String content = I18n.getOrDefault("resource.help.geology_specs_body",
                "📍 STANDARDIZED PROJECTION & RESOLUTION:\n" +
                        "• Projection: Standard WGS84 Equirectangular (2:1, Longitude [-180°, +180°], Latitude [-90°, +90°]).\n" +
                        "• Supported formats: PNG (8/16-bit), GeoTIFF (.tif/.tiff), ESRI Arc/Info ASCII (.asc), GeoJSON (.geojson), NetCDF-4 (.nc.json).\n" +
                        "• Recommended resolution: 720×360 (fast), 1440×720 (standard HD), or 3600×1800 (ultra-precise).\n\n" +
                        "🛢️ 10 CANONICAL GEOLOGICAL LAYERS:\n" +
                        "1. Coal Basins (USGS World Coal Quality / BGR Energy Study)\n" +
                        "2. Oil Reservoirs (USGS DDS-69 World Petroleum Assessment)\n" +
                        "3. Natural Gas Fields (IEA Global Gas Tracker / Rystad Energy)\n" +
                        "4. Uranium Deposits (IAEA UDEPO / Red Book Resources)\n" +
                        "5. Helium-3 Regolith (NASA LRO LAMP / Apollo Lunar Samples)\n" +
                        "6. Iron & Copper Porphyry (USGS MRDS Mineral Resources)\n" +
                        "7. Precious Metals Au/Ag/Pt (GFMS Gold Survey / USGS)\n" +
                        "8. Rare Earth Elements REE & Lithium (BGS Critical Minerals)\n" +
                        "9. Mantle Heat Flow & Geothermal (IHFC Global Heat Flow Data)\n" +
                        "10. Groundwater & Deep Aquifers (UNESCO WHYMAP / GRACE-FO)\n\n" +
                        "⚙️ CALIBRATION & VALUES:\n" +
                        "• Pixel grayscale (0 to 255) indicates resource abundance [0.0 to 1.0].\n" +
                        "• The export automatically bundles ESRI World Files (.tfw) for instant GIS integration."
        );

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(640);
        textArea.setPrefHeight(420);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");

        alert.getDialogPane().setContent(textArea);
        if (b.host.getWindow() != null) alert.initOwner(b.host.getWindow());
        alert.showAndWait();
    }

    static ComboBox<String> buildGeologySourceCombo(ResourceGeologySectionBuilder b, int index) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("combo-box");
        combo.setStyle("-fx-font-size: 11px;");
        combo.getItems().add(I18n.getOrDefault("resource.source.procedural", "▶ Procédural pur (Formules analytiques)"));

        switch (index) {
            case 0 -> combo.getItems().addAll(
                    "🌍 Terre — USGS World Coal Quality Inventory & BGR Energy",
                    "🌍 Terre — Paléo-Carbonifère Parallique (-300 Ma)",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 1 -> combo.getItems().addAll(
                    "🌍 Terre — USGS DDS-69 World Petroleum Assessment",
                    "🌍 Terre — Rystad Energy UCube Offshore & Onshore",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 2 -> combo.getItems().addAll(
                    "🌍 Terre — IEA Global Gas Tracker & Deep Shales",
                    "🌍 Terre — Bassins Cratoniques Conventionnels",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 3 -> combo.getItems().addAll(
                    "🌍 Terre — IAEA UDEPO World Uranium Deposit Database",
                    "🔴 Mars — MRO CRISM Uranium-Thorium Spectrometry Map",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 4 -> combo.getItems().addAll(
                    "🌕 Lune — Chang'e / LRO LAMP Ilmenite He-3 Distribution",
                    "🔴 Mars — MAVEN Solar Wind Regolith Implantation",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 5 -> combo.getItems().addAll(
                    "🌍 Terre — USGS MRDS Mineral Resources Data System",
                    "🔴 Mars — MGS TES Hematite & Iron Crust Global Map",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 6 -> combo.getItems().addAll(
                    "🌍 Terre — World Gold Council & USGS Precious Metals",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 7 -> combo.getItems().addAll(
                    "🌍 Terre — BGS Critical Minerals & Lithium Salars Survey",
                    "🌕 Lune — KREEP Terrane Rare Earth & Thorium Procellarum",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            case 8 -> combo.getItems().addAll(
                    "🌍 Terre — IHFC Global Heat Flow Commission Map",
                    "🔴 Mars — InSight SEIS Lithospheric Thermal Gradient",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
            default -> combo.getItems().addAll(
                    "🌍 Terre — UNESCO WHYMAP World Hydrogeological Map & GRACE-FO",
                    "🔴 Mars — MARSIS ESA Subglacial Deep Aquifer Map",
                    "🪐 Personnalisé — Fichier Externe Spécifié");
        }

        combo.setValue(combo.getItems().size() > 1 ? combo.getItems().get(1) : combo.getItems().get(0));
        combo.setOnAction(e -> {
            String val = combo.getValue();
            if (val != null) {
                if (val.contains("USGS") || val.contains("BGR") || val.contains("IEA") || val.contains("IAEA")
                        || val.contains("BGS") || val.contains("IHFC") || val.contains("UNESCO")
                        || val.contains("Terre") || val.contains("Mars") || val.contains("Lune")) {
                    if (b.geologyImportRadios.containsKey(index)) b.geologyImportRadios.get(index).setSelected(true);
                } else if (val.contains("Procédural")) {
                    if (b.geologyProcRadios.containsKey(index)) b.geologyProcRadios.get(index).setSelected(true);
                }
                b.host.selectViewMode(b.host.getGeologyViewComboIndex(index));
                if (!b.host.isUpdatingFromPreset()) b.host.notifyParamChange();
                b.host.updatePreviewCanvas();
            }
        });
        return combo;
    }

    static void updateTexts(ResourceGeologySectionBuilder b) {
        if (b.geologyDomainSecHeader != null) b.geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        if (b.sec41Header != null) b.sec41Header.setText(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        if (b.sec42Header != null) b.sec42Header.setText(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        if (b.btnGenerateProceduralGeologyTensors != null) {
            b.btnGenerateProceduralGeologyTensors.setText(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
            b.btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors", "Switch all geological tensors to procedural mode and regenerate maps.")));
        }
        if (b.geoSeedLabel != null) b.geoSeedLabel.setText(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        if (b.geologyHelpBtn != null) b.geologyHelpBtn.setText(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        if (b.btnExportGisMultiFormat != null) b.btnExportGisMultiFormat.setText(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        if (b.btnExportProvenanceManifest != null) b.btnExportProvenanceManifest.setText(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));

        for (int i = 0; i < ResourceGeologySectionBuilder.GEOLOGY_SLIDER_SPECS.length; i++) {
            if (b.geologySubTitles.get(i) != null) {
                b.geologySubTitles.get(i).setText(b.getGeologyTensorTitle(i));
                Tooltip.install(b.geologySubTitles.get(i), new Tooltip(b.getGeologyTensorTooltip(i)));
            }
            if (b.geologyProcRadios.get(i) != null) b.geologyProcRadios.get(i).setText(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            if (b.geologyImportRadios.get(i) != null) b.geologyImportRadios.get(i).setText(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            if (b.geologySeedLabels.get(i) != null) b.geologySeedLabels.get(i).setText(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            if (b.geologyGenBtns.get(i) != null) b.geologyGenBtns.get(i).setText(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            if (b.geologySourceLabels.get(i) != null) b.geologySourceLabels.get(i).setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            if (b.geologyLoadBtns.get(i) != null) b.geologyLoadBtns.get(i).setText(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            if (b.geologyFormatLabels.get(i) != null) b.geologyFormatLabels.get(i).setText(b.getGeologyFormatHint(i));
            List<Label> titles = b.geologySliderTitleLabels.get(i);
            if (titles != null) {
                for (int s = 0; s < titles.size() && s < 4; s++) {
                    ResourceGeologyDepositCatalog.TensorSliderMeta meta = ResourceGeologySectionBuilder.GEOLOGY_SLIDER_SPECS[i][s];
                    titles.get(s).setText(I18n.getOrDefault(meta.labelKey, meta.defaultLabel));
                    Tooltip.install(titles.get(s), new Tooltip(I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip)));
                }
            }
        }
    }
}
