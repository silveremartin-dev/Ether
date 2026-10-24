/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.generation.SimplexNoise;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.ResourceDepositMapReader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.export.ResourceDistributionRasterExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Autonomous UI Controller and Section Builder for Geological Tensors, Mineral Endowments & Deep Earth Energy.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Encapsulates the 10 canonical planetary geological tensors:
 * <ol>
 *   <li><b>Coal</b>: Sedimentary paralic basin thickness, vitrinite reflectance (%Ro), and paleomire accumulation.</li>
 *   <li><b>Crude Oil</b>: Source rock Total Organic Carbon (%TOC), thermal oil window (60°C–175°C), and anticlinal trap density.</li>
 *   <li><b>Natural Gas</b>: Deep thermogenic cracking vs. biogenic gas, cratonic reservoir pressures, and caprock evaporite seals.</li>
 *   <li><b>Uranium</b>: Hydrothermal vein pechblende precipitation, Proterozoic unconformity redox fronts, and \( \text{Th}^{232}/\text{U}^{238} \) ratio.</li>
 *   <li><b>Helium-3</b>: Lunar/planetary regolith solar-wind implantation, ilmenite (\( \text{FeTiO}_3 \)) capture capacity, and atmospheric shielding factor.</li>
 *   <li><b>Iron & Copper</b>: Banded Iron Formations (BIF) and magmatic porphyry intrusions.</li>
 *   <li><b>Precious Metals</b>: Epithermal vein gold/silver (\( \text{Au, Ag, Pt} \)) and alluvial placer deposits.</li>
 *   <li><b>Rare Earth Elements (REE) & Critical Minerals</b>: Carbonatite complexes, pegmatites, and lithium salars.</li>
 *   <li><b>Mantle Heat Flow</b>: Radiogenic heat, tectonic lithospheric plumes, and geothermal gradients (\( \text{mW/m}^2 \)).</li>
 *   <li><b>Freshwater Aquifers</b>: Hydrogeological piezometric head, hydraulic conductivity \( K \), and interstitial rock porosity.</li>
 * </ol>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceGeologySectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourceGeologySectionBuilder.class);

    /**
     * Interface providing host context properties and callbacks to {@code ResourceDistributionPanel}.
     */
    public interface HostContext {
        void notifyParamChange();
        void updatePreviewCanvas();
        int getGeologyViewComboIndex(int layerIdx);
        void selectViewMode(int viewModeIndex);
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        long getEpochYear();
    }

    private final HostContext host;
    private final ProceduralGenerator generator = new ProceduralGenerator();
    private final Map<Long, SimplexNoise> noiseCache = new ConcurrentHashMap<>();

    // Geological Tensors UI Models & Caches
    private final Map<Integer, Image> customGeologyLayerImages = new HashMap<>();
    private final Map<Integer, RadioButton> geologyProcRadios = new HashMap<>();
    private final Map<Integer, RadioButton> geologyImportRadios = new HashMap<>();
    private final Map<Integer, Label> geologySubTitles = new HashMap<>();
    private final Map<Integer, Label> geologyFileLabels = new HashMap<>();
    private final Map<Integer, Label> geologySourceLabels = new HashMap<>();
    private final Map<Integer, ComboBox<String>> geologySourceCombos = new HashMap<>();
    private final Map<Integer, Button> geologyLoadBtns = new HashMap<>();
    private final Map<Integer, Label> geologyFormatLabels = new HashMap<>();
    private final Map<Integer, Label> geologySeedLabels = new HashMap<>();
    private final Map<Integer, Button> geologyGenBtns = new HashMap<>();
    private final Map<Integer, List<Label>> geologySliderTitleLabels = new HashMap<>();

    // Section Headers & Global Controls
    private Label geologyDomainSecHeader;
    private Label sec41Header;
    private Label sec42Header;
    private Label geoSeedLabel;
    private VBox geologyLayersDynamicContainer;
    private TextField geologySeedInput;
    private Button btnGenerateProceduralGeologyTensors;
    private Button geologyHelpBtn;
    private Button btnExportGisMultiFormat;
    private Button btnExportProvenanceManifest;

    // Per-tensor Geology Seed & Physical Sliders (Tensors 0 to 9)
    private final Map<Integer, TextField> geologyTensorSeeds = new HashMap<>();
    private final Map<Integer, Slider> geologyAbundanceSliders = new HashMap<>();
    private final Map<Integer, Slider> geologyThresholdSliders = new HashMap<>();
    private final Map<Integer, Slider> geologyParam3Sliders = new HashMap<>();
    private final Map<Integer, Slider> geologyParam4Sliders = new HashMap<>();

    // --- Canonically Catalogued Geological Empirical Hotspots ---
    private static final double[][] COAL_SPOTS = {
        {-80.5, 39.8, 55, 1.4}, {-88.5, 38.5, 45, 1.3}, {-105.5, 44.5, 60, 1.5}, {-87.0, 33.5, 40, 1.2},
        {-114.5, 53.5, 45, 1.3}, {-108.5, 36.8, 40, 1.2}, {-96.0, 38.0, 35, 1.1},
        {7.0, 51.5, 40, 1.3}, {19.0, 50.2, 45, 1.4}, {38.5, 48.0, 50, 1.4}, {86.5, 54.5, 65, 1.5},
        {57.5, 65.5, 45, 1.2}, {73.0, 49.8, 50, 1.3}, {75.5, 51.7, 45, 1.3}, {-1.5, 53.5, 35, 1.1},
        {112.5, 37.8, 65, 1.6}, {109.0, 39.5, 60, 1.5}, {87.5, 44.0, 55, 1.4}, {104.5, 29.5, 40, 1.2},
        {127.0, 38.0, 35, 1.1}, {117.5, 34.5, 45, 1.3},
        {86.2, 23.7, 50, 1.5}, {80.0, 18.0, 40, 1.2}, {85.0, 21.0, 40, 1.3}, {82.6, 23.0, 45, 1.3},
        {116.0, 0.0, 35, 1.1}, {103.5, -3.5, 35, 1.1}, {69.0, 24.5, 35, 1.1},
        {149.0, -22.5, 50, 1.4}, {150.5, -33.0, 45, 1.3},
        {29.0, -26.0, 45, 1.3}, {33.5, -16.0, 35, 1.1}, {31.0, -28.0, 30, 1.0},
        {-52.0, -30.0, 35, 1.1}, {-73.0, -37.0, 25, 1.0}, {-72.0, 5.0, 25, 1.0}
    };

    private static final double[][] OIL_SPOTS = {
        {49.0, 26.5, 75, 1.8}, {48.0, 29.5, 65, 1.6}, {51.0, 31.0, 60, 1.5}, {54.0, 24.5, 55, 1.4},
        {50.5, 26.0, 50, 1.3}, {44.0, 35.0, 55, 1.4}, {47.5, 30.5, 60, 1.5}, {53.5, 34.0, 40, 1.2},
        {-102.5, 31.8, 65, 1.6}, {-93.5, 28.5, 60, 1.5}, {-97.5, 28.5, 55, 1.4}, {-103.0, 48.0, 55, 1.4},
        {-119.5, 35.0, 45, 1.2}, {-149.0, 70.0, 50, 1.3}, {-111.0, 56.5, 65, 1.5},
        {72.5, 61.0, 75, 1.7}, {52.0, 54.5, 60, 1.5}, {49.5, 40.5, 50, 1.3}, {52.5, 44.0, 45, 1.2},
        {51.0, 46.5, 50, 1.3}, {1.5, 58.5, 55, 1.4}, {7.5, 54.0, 40, 1.1},
        {-65.0, 9.0, 70, 1.7}, {-71.5, 10.0, 55, 1.4}, {-41.0, -22.5, 55, 1.4}, {-68.0, -39.0, 45, 1.2},
        {6.5, 4.5, 60, 1.5}, {12.0, -7.5, 55, 1.3}, {9.5, 1.0, 45, 1.2}, {18.0, 29.0, 50, 1.3},
        {124.5, 46.5, 60, 1.5}, {118.5, 38.0, 50, 1.3}, {85.0, 45.5, 45, 1.2}, {108.0, 5.0, 45, 1.2},
        {72.0, 19.5, 50, 1.3}, {103.0, 1.5, 40, 1.1}, {117.0, 4.0, 40, 1.1}
    };

    private static final double[][] GAS_SPOTS = {
        {52.5, 25.5, 80, 1.8}, {51.5, 26.5, 75, 1.7}, {53.0, 27.5, 70, 1.6}, {59.5, 36.5, 60, 1.4},
        {76.0, 66.5, 85, 1.8}, {73.0, 71.0, 80, 1.7}, {77.0, 65.0, 75, 1.6}, {54.0, 38.0, 65, 1.5},
        {-80.0, 40.0, 70, 1.6}, {-94.0, 32.0, 65, 1.5}, {-97.0, 33.0, 60, 1.4}, {-104.0, 32.0, 60, 1.4},
        {3.0, 60.5, 65, 1.5}, {6.8, 53.2, 55, 1.4}, {34.5, 33.0, 55, 1.4}, {32.5, 32.5, 60, 1.5},
        {8.0, 32.0, 60, 1.4}, {2.5, 28.0, 55, 1.3}, {115.0, -19.5, 60, 1.4}, {130.0, -11.0, 50, 1.3}
    };

    private static final double[][] URANIUM_SPOTS = {
        {67.5, 44.5, 60, 1.5}, {69.0, 43.5, 55, 1.4}, {-105.5, 58.0, 65, 1.6}, {-104.0, 57.5, 60, 1.5},
        {132.5, -12.5, 60, 1.5}, {137.0, -30.5, 65, 1.6}, {15.0, -22.5, 55, 1.4}, {8.0, 18.5, 50, 1.3},
        {117.0, 50.0, 50, 1.3}, {69.0, 67.5, 50, 1.3}, {27.0, -11.5, 50, 1.3}, {-108.0, 35.5, 45, 1.2}
    };

    private static final double[][] HE3_SPOTS = {
        {23.5, 16.5, 60, 1.5}, {-58.0, -26.0, 55, 1.4}, {17.5, 2.5, 50, 1.3}, {30.5, 8.5, 55, 1.4},
        {-20.0, 9.5, 45, 1.2}, {0.0, 18.0, 50, 1.3}, {180.0, 0.0, 50, 1.3}, {0.0, 0.0, 50, 1.3}
    };

    private static final double[][] IRON_COPPER_SPOTS = {
        {-50.5, -6.0, 65, 1.6}, {-43.5, -20.0, 60, 1.5}, {119.5, -23.0, 70, 1.7}, {118.0, -22.0, 65, 1.6},
        {36.5, 51.5, 60, 1.5}, {20.0, 67.8, 55, 1.4}, {86.0, 22.0, 50, 1.3}, {111.0, 41.0, 55, 1.4},
        {-69.0, -22.5, 75, 1.8}, {-70.5, -31.5, 70, 1.7}, {-71.0, -16.5, 65, 1.6}, {-110.5, 33.0, 55, 1.4},
        {28.0, -12.5, 65, 1.6}, {26.0, -11.0, 60, 1.5}, {-89.0, 47.0, 50, 1.3}, {136.5, -30.5, 60, 1.5}
    };

    private static final double[][] PRECIOUS_METAL_SPOTS = {
        {27.5, -26.5, 75, 1.8}, {28.0, -25.5, 70, 1.7}, {-116.0, 40.5, 65, 1.6}, {-117.0, 39.0, 60, 1.5},
        {121.5, -30.5, 60, 1.5}, {88.0, 69.5, 65, 1.6}, {-69.5, -27.5, 60, 1.5}, {-78.0, -1.0, 55, 1.4},
        {137.0, -4.0, 65, 1.6}, {-2.0, 6.0, 55, 1.4}, {123.5, 53.5, 50, 1.3}, {-147.5, 65.0, 55, 1.4}
    };

    private static final double[][] RARE_EARTH_SPOTS = {
        {110.0, 41.5, 75, 1.8}, {116.0, 25.0, 65, 1.6}, {-67.5, -23.5, 70, 1.7}, {-68.5, -27.0, 65, 1.6},
        {-67.0, -20.0, 65, 1.6}, {128.0, -18.5, 60, 1.5}, {116.0, -33.5, 65, 1.6}, {-115.5, 35.5, 60, 1.5},
        {31.0, -25.5, 55, 1.4}, {48.5, 71.0, 55, 1.4}, {14.0, 46.0, 45, 1.2}, {76.0, 27.5, 50, 1.3}
    };

    private static final double[][] MANTLE_HEAT_SPOTS = {
        {-19.0, 64.5, 80, 1.8}, {-155.5, 19.5, 75, 1.7}, {-110.5, 44.5, 70, 1.6}, {14.5, 41.0, 60, 1.4},
        {139.5, 35.5, 65, 1.5}, {121.0, 14.5, 60, 1.4}, {176.0, -38.5, 65, 1.5}, {37.0, 0.5, 70, 1.6},
        {42.5, 11.5, 65, 1.5}, {-68.0, -24.0, 60, 1.4}, {-25.5, 38.0, 55, 1.3}, {100.0, -1.0, 65, 1.5}
    };

    private static final double[][] AQUIFER_SPOTS = {
        {25.0, 24.0, 85, 1.6}, {-56.0, -25.0, 80, 1.5}, {137.0, -26.0, 80, 1.5}, {-101.0, 36.0, 75, 1.4},
        {80.0, 27.0, 75, 1.4}, {2.0, 47.0, 55, 1.2}, {-60.0, -3.0, 90, 1.5}, {22.0, -1.0, 75, 1.3},
        {75.0, 60.0, 85, 1.4}, {122.0, -18.0, 60, 1.2}, {82.0, 39.0, 55, 1.2}, {-48.0, -1.5, 50, 1.2},
        {-118.0, 36.0, 45, 1.2}, {45.0, 25.0, 60, 1.3}, {16.0, 14.0, 65, 1.3}, {23.0, -22.0, 60, 1.2},
        {116.0, 37.0, 65, 1.3}, {70.0, 30.0, 65, 1.3}
    };

    public static class TensorSliderMeta {
        public final String labelKey;
        public final String defaultLabel;
        public final String tooltipKey;
        public final String defaultTooltip;
        public final double min;
        public final double max;
        public final double defaultValue;
        public final String unit;
        public final String formatPattern;

        public TensorSliderMeta(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                                double min, double max, double defaultValue, String unit, String formatPattern) {
            this.labelKey = labelKey;
            this.defaultLabel = defaultLabel;
            this.tooltipKey = tooltipKey;
            this.defaultTooltip = defaultTooltip;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.unit = unit;
            this.formatPattern = formatPattern;
        }
    }

    public static final TensorSliderMeta[][] GEOLOGY_SLIDER_SPECS = {
        // Tensor 0: COAL
        {
            new TensorSliderMeta("resource.tensor.1.param1", "Abondance & Épaisseur Veines Charbon", "resource.tensor.1.param1.tt", "Règle l'abondance globale et l'épaisseur moyenne des veines de charbon dans les bassins sédimentaires (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.1.param2", "Seuil Paléo-Biomasse Marécages", "resource.tensor.1.param2.tt", "Pourcentage minimal de végétation paléolithique et zones humides requis pour l'accumulation de tourbe.", 0.0, 1.0, 0.35, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.1.param3", "Rang de Carbonisation (Anthracitisation)", "resource.tensor.1.param3.tt", "Degré de métamorphisme thermique (%Ro vitrinite). 0=Lignite, 0.5=Bitumineux, 1.0=Anthracite pure.", 0.0, 1.0, 0.50, "%Ro", "%.2f %%Ro"),
            new TensorSliderMeta("resource.tensor.1.param4", "Épaisseur Couvert Sédimentaire", "resource.tensor.1.param4.tt", "Profondeur d'enfouissement lithostatique dans les bassins paraliques favorisant la compaction.", 0.5, 10.0, 2.5, "km", "%.1f km")
        },
        // Tensor 1: CRUDE_OIL
        {
            new TensorSliderMeta("resource.tensor.2.param1", "Volume Réserves & Pièges Pétrole", "resource.tensor.2.param1.tt", "Volume total d'hydrocarbures liquides emmagasinés dans les réservoirs sous-terrains (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.2.param2", "Maturation Roche-Mère Anoxique", "resource.tensor.2.param2.tt", "Taux de Carbone Organique Total (TOC) déposé dans les schistes marins en milieu anoxique.", 0.0, 1.0, 0.40, "% TOC", "%.0f %% TOC"),
            new TensorSliderMeta("resource.tensor.2.param3", "Densité Pièges Anticlinaux & Failles", "resource.tensor.2.param3.tt", "Fréquence des déformations tectoniques plissées et dômes salins stoppant la migration du pétrole.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.2.param4", "Température Fenêtre à Pétrole", "resource.tensor.2.param4.tt", "Plage de température de pyrolyse naturelle (60°C-175°C) convertissant le kérogène en pétrole brut.", 60.0, 180.0, 120.0, "°C", "%.0f °C")
        },
        // Tensor 2: NATURAL_GAS
        {
            new TensorSliderMeta("resource.tensor.3.param1", "Pression & Abondance Gaz", "resource.tensor.3.param1.tt", "Volume total de gaz naturel conventionnel et non-conventionnel emmagasiné sous pression (10¹² m³).", 0.1, 5.0, 1.0, "10¹² m³", "%.2f 10¹² m³"),
            new TensorSliderMeta("resource.tensor.3.param2", "Fraction Gaz de Schiste Thermogène", "resource.tensor.3.param2.tt", "Proportion de gaz formé par cracking thermique profond par rapport au gaz biogénique de surface.", 0.0, 1.0, 0.40, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.3.param3", "Profondeur Réservoir Cratonique", "resource.tensor.3.param3.tt", "Profondeur des formations gréseuses et carbonatées réservoirs impactant la rétention gazeuse.", 0.5, 8.0, 3.2, "km", "%.1f km"),
            new TensorSliderMeta("resource.tensor.3.param4", "Étanchéité Roches-Couverture", "resource.tensor.3.param4.tt", "Capacité d'étanchéité des couches d'évaporites et d'argilites empêchant la fuite vers la surface.", 0.0, 1.0, 0.60, "Indice", "%.2f")
        },
        // Tensor 3: URANIUM
        {
            new TensorSliderMeta("resource.tensor.4.param1", "Teneur Crustale Pegmatites Uranium", "resource.tensor.4.param1.tt", "Concentration moyenne en Uranium de la croûte magmatique et des granites fertiles (ppm).", 10.0, 1000.0, 250.0, "ppm U", "%.0f ppm"),
            new TensorSliderMeta("resource.tensor.4.param2", "Infiltration Hydrothermale & Altération", "resource.tensor.4.param2.tt", "Circulation des fluides hydrothermaux lessivant et précipitant la pechblende (UO₂) dans les filons.", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.4.param3", "Rapport Massique Thorium/Uranium", "resource.tensor.4.param3.tt", "Rapport isotopique naturel Th²³²/U²³⁸. Un ratio élevé favorise les minéralisations en Thorium.", 0.5, 10.0, 3.5, "Th/U", "%.1f Th/U"),
            new TensorSliderMeta("resource.tensor.4.param4", "Fronts Redox & Discordances", "resource.tensor.4.param4.tt", "Intensité des pièges d'oxydo-réduction aux discordances Protérozoïques (type Athabasca).", 0.0, 1.0, 0.50, "Indice", "%.2f")
        },
        // Tensor 4: HELIUM_3
        {
            new TensorSliderMeta("resource.tensor.5.param1", "Concentration Régolithe Ilménite He-3", "resource.tensor.5.param1.tt", "Teneur moyenne en Hélium-3 piégé par le vent solaire dans le régolithe basaltique (ppb).", 1.0, 50.0, 15.0, "ppb He3", "%.1f ppb"),
            new TensorSliderMeta("resource.tensor.5.param2", "Teneur Minérale TiO₂ Ilménite", "resource.tensor.5.param2.tt", "Pourcentage d'ilménite (FeTiO₃) dans la matrice de poussière améliorant la rétention du vent solaire.", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.5.param3", "Flux Irradiation Vent Solaire", "resource.tensor.5.param3.tt", "Intensité du flux d'ions He-3 non déviés par un champ magnétique dipolaire (x Terre).", 0.1, 5.0, 1.0, "Flux Rel.", "%.2f"),
            new TensorSliderMeta("resource.tensor.5.param4", "Taux de Brassage / Impact Gardening", "resource.tensor.5.param4.tt", "Profondeur de pulvérisation météoritique enfouissant l'hélium sous la surface (m).", 0.5, 10.0, 3.0, "m", "%.1f m")
        },
        // Tensor 5: IRON_COPPER
        {
            new TensorSliderMeta("resource.tensor.6.param1", "Abondance Gisements Fer/Cuivre", "resource.tensor.6.param1.tt", "Masse totale accessible de fer et métaux de base dans la croûte supérieure (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.6.param2", "Gisements Porphyriques Cu-Au-Mo", "resource.tensor.6.param2.tt", "Intensité de la minéralisation magmatique calco-alcaline liée aux zones de subduction.", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.6.param3", "Formations Ferrifères Rubanées (BIF)", "resource.tensor.6.param3.tt", "Présence des gisements archéens/protérozoïques d'hématite et magnétite précambriens.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.6.param4", "Teneur Moyenne Minerai Métallique", "resource.tensor.6.param4.tt", "Pourcentage de métal pur extrait par tonne de roche brute (grade minier).", 0.1, 5.0, 0.8, "%", "%.2f %%")
        },
        // Tensor 6: PRECIOUS_METALS
        {
            new TensorSliderMeta("resource.tensor.7.param1", "Richesse Filons Métaux Précieux", "resource.tensor.7.param1.tt", "Ressources extractibles en or, argent et platinoïdes dans les formations hydrothermales (kt).", 0.1, 5.0, 1.0, "kt Au", "%.2f kt"),
            new TensorSliderMeta("resource.tensor.7.param2", "Placers Alluvionnaires & Érosion", "resource.tensor.7.param2.tt", "Teneur en pépites et paillettes concentrées dans les sédiments fluviatiles quaternaires.", 0.0, 1.0, 0.40, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.7.param3", "Minéralisations Épithermales Volcaniques", "resource.tensor.7.param3.tt", "Densité des filons de quartz aurifère le long des arcs insulaires et caldeiras.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.7.param4", "Teneur Ruissellement Cratonique", "resource.tensor.7.param4.tt", "Concentration moyenne des conglomérats anciens (type Witwatersrand) (g/t roche).", 0.5, 10.0, 2.0, "g/t", "%.1f g/t")
        },
        // Tensor 7: RARE_EARTHS
        {
            new TensorSliderMeta("resource.tensor.8.param1", "Abondance Terres Rares & Minéraux Critiques", "resource.tensor.8.param1.tt", "Volume total en Terres Rares (Nd, Dy, Pr) et métaux critiques pour la transition (Mt).", 0.1, 5.0, 1.0, "Mt", "%.2f Mt"),
            new TensorSliderMeta("resource.tensor.8.param2", "Intrusions Carbonatites & Alcalines", "resource.tensor.8.param2.tt", "Densité des complexes intrusifs magmatiques alcalins enrichis en bastnäsite et monazite.", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.8.param3", "Salars & Saumures à Lithium", "resource.tensor.8.param3.tt", "Concentration en ions Lithium (Li+) dissous dans les bassins endoréiques arides.", 0.0, 1.0, 0.40, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.8.param4", "Argiles Ioniques d'Altération", "resource.tensor.8.param4.tt", "Épaisseur des profils de latérites subtropicales retenant les terres rares lourdes adsorbées.", 0.5, 5.0, 1.5, "m", "%.1f m")
        },
        // Tensor 8: GEOTHERMAL_MANTLE_HEAT
        {
            new TensorSliderMeta("resource.tensor.9.param1", "Flux Thermique Moyen Croûte", "resource.tensor.9.param1.tt", "Puissance géothermique moyenne traversant la lithosphère (mW/m²).", 20.0, 200.0, 65.0, "mW/m²", "%.0f mW/m²"),
            new TensorSliderMeta("resource.tensor.9.param2", "Anomalies Points Chauds & Rifts", "resource.tensor.9.param2.tt", "Facteur d'amplification thermique le long des dorsales océaniques et panaches mantelliques.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.9.param3", "Épaisseur Lithosphère Cratonique", "resource.tensor.9.param3.tt", "Épaisseur de la racine cratonique isolante limitant le flux de surface.", 50.0, 300.0, 150.0, "km", "%.0f km"),
            new TensorSliderMeta("resource.tensor.9.param4", "Gradient Géothermique Profond", "resource.tensor.9.param4.tt", "Élévation de température par kilomètre de profondeur crustale (°C/km).", 10.0, 60.0, 30.0, "°C/km", "%.1f °C/km")
        },
        // Tensor 9: AQUIFERS
        {
            new TensorSliderMeta("resource.tensor.10.param1", "Volume Bassins Sédimentaires Aquifères", "resource.tensor.10.param1.tt", "Capacité totale de stockage des nappes phréatiques et aquifères profonds captifs (10³ km³).", 0.1, 5.0, 1.0, "10³ km³", "%.2f 10³ km³"),
            new TensorSliderMeta("resource.tensor.10.param2", "Conductivité Hydraulique / Perméabilité", "resource.tensor.10.param2.tt", "Perméabilité de la roche aquifère permettant la vitesse de recharge et d'écoulement sous gravité.", 0.0, 1.0, 0.40, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.10.param3", "Porosité Roches Réservoirs", "resource.tensor.10.param3.tt", "Pourcentage de vides interstitiels dans les grès et calcaires retenant l'eau sous pression.", 5.0, 35.0, 18.0, "%", "%.1f %%"),
            new TensorSliderMeta("resource.tensor.10.param4", "Profondeur Permafrost / Cryosphère", "resource.tensor.10.param4.tt", "Épaisseur du sol gelé en permanence (permafrost) scellant les aquifères liquides sous-jacents.", 0.0, 2000.0, 300.0, "m", "%.0f m")
        }
    };

    /**
     * Constructs the geological section builder with its host context.
     *
     * @param host context bridge to {@code ResourceDistributionPanel}
     */
    public ResourceGeologySectionBuilder(HostContext host) {
        this.host = Objects.requireNonNull(host, "host context must not be null");
    }

    /**
     * Builds and returns the complete geological domain VBox card for Tab 2.
     *
     * @return fully styled JavaFX VBox container
     */
    public VBox buildGeologySection() {
        geologyDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        geologyDomainSecHeader.getStyleClass().add("label-section-header");

        // Section 4.1 : Contrôle Global, Graine & Formats
        sec41Header = new Label(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        sec41Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        btnGenerateProceduralGeologyTensors = new Button(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
        btnGenerateProceduralGeologyTensors.getStyleClass().add("button");
        btnGenerateProceduralGeologyTensors.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralGeologyTensors.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors",
                "Switch all geological tensors to procedural mode and regenerate maps according to parameters and stochastic seed.")));
        btnGenerateProceduralGeologyTensors.setOnAction(e -> generateProceduralGeologyTensors());

        geologySeedInput = new TextField("45678");
        geologySeedInput.setPrefWidth(80);
        geologySeedInput.setStyle("-fx-font-size: 11px;");
        geologySeedInput.textProperty().addListener((obs, oldV, newV) -> {
            if (!host.isUpdatingFromPreset()) {
                host.notifyParamChange();
            }
        });

        Button geoRandSeedBtn = new Button("🎲");
        geoRandSeedBtn.getStyleClass().add("button-secondary");
        geoRandSeedBtn.setStyle("-fx-font-size: 11px;");
        geoRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.random_geology_seed", "Draw a new random stochastic seed for all geological tensors.")));
        geoRandSeedBtn.setOnAction(e -> {
            String s = String.valueOf(new Random().nextLong(1000000));
            geologySeedInput.setText(s);
            generateProceduralGeologyTensors();
        });

        geoSeedLabel = new Label(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        HBox geoSeedBox = new HBox(4, geoSeedLabel, new Label("🎲"), geologySeedInput, geoRandSeedBtn);
        geoSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, geoSeedBox, btnGenerateProceduralGeologyTensors);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralGeologyTensors, Priority.ALWAYS);

        geologyHelpBtn = new Button(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        geologyHelpBtn.getStyleClass().add("button-secondary");
        geologyHelpBtn.setStyle("-fx-font-size: 11px;");
        geologyHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        geologyHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.geology_specs", "Spécifications des formats d'image et standards géologiques (PNG, GeoTIFF, NetCDF, ASC, GeoJSON).")));
        geologyHelpBtn.setOnAction(e -> showGeologyImportFormatHelp());

        btnExportGisMultiFormat = new Button(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_gis", "Exporter les 10 tenseurs géologiques sous formats SIG professionnels (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        btnExportProvenanceManifest = new Button(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Générer un manifeste cryptographique JSON (provenance.json) contenant les hashs SHA-256 des jeux de données géologiques.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        HBox formatRow = new HBox(8, geologyHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox sec41Box = new VBox(6, sec41Header, seedRow, formatRow);
        sec41Box.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(148, 163, 184, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");

        // Section 4.2 : Tenseurs Géologiques Canoniques
        sec42Header = new Label(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        sec42Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        sec42Header.setWrapText(true);

        geologyLayersDynamicContainer = new VBox(10);
        rebuildGeologyTensorSubBlocks();
        prepopulateEarthGeologyTensors();

        VBox content = new VBox(10, sec41Box, sec42Header, geologyLayersDynamicContainer);
        VBox card = new VBox(8, geologyDomainSecHeader, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    /**
     * Dynamically rebuilds the 10 canonical geological tensor parameter cards.
     */
    public void rebuildGeologyTensorSubBlocks() {
        if (geologyLayersDynamicContainer == null) return;
        geologyLayersDynamicContainer.getChildren().clear();

        for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
            final int layerIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(56,189,248,0.04); -fx-background-radius: 6; -fx-border-color: rgba(56,189,248,0.15); -fx-border-radius: 6;");

            Label subTitle = new Label(getGeologyTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            Tooltip.install(subTitle, new Tooltip(getGeologyTensorTooltip(i)));
            geologySubTitles.put(layerIdx, subTitle);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");

            boolean hasImage = customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }

            geologyProcRadios.put(layerIdx, radioProc);
            geologyImportRadios.put(layerIdx, radioImport);

            // --- Procedural Configuration Sub-Box ---
            TextField seedTF = new TextField(String.valueOf(12345 + layerIdx * 777));
            seedTF.setPrefWidth(90);
            seedTF.setStyle("-fx-font-size: 11px;");
            seedTF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });
            geologyTensorSeeds.put(layerIdx, seedTF);

            Button randSeedBtn = new Button("🎲");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 11px;");
            randSeedBtn.setOnAction(e -> {
                seedTF.setText(String.valueOf(new Random().nextLong(1000000)));
                if (radioProc != null) radioProc.setSelected(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            Button btnExportTensor = new Button(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            btnExportTensor.getStyleClass().add("button-secondary");
            btnExportTensor.setStyle("-fx-font-size: 11px;");
            btnExportTensor.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_tensor", "Exporter ce tenseur géologique sous forme d'image raster haute résolution (PNG / JPEG).")));
            btnExportTensor.setOnAction(e -> exportGeologyTensor(layerIdx));
            geologyGenBtns.put(layerIdx, btnExportTensor);

            Label seedLbl = new Label(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            geologySeedLabels.put(layerIdx, seedLbl);
            HBox seedBox = new HBox(6, seedLbl, seedTF, randSeedBtn, btnExportTensor);
            seedBox.setAlignment(Pos.CENTER_LEFT);

            VBox slidersContainer = new VBox(4);
            List<Label> sliderTitles = new ArrayList<>();

            for (int s = 0; s < 4; s++) {
                TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];

                String labelText = I18n.getOrDefault(meta.labelKey, meta.defaultLabel);
                String tooltipText = I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip);

                Label titleLabel = new Label(labelText);
                titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                sliderTitles.add(titleLabel);

                Label valLabel = new Label(String.format(meta.formatPattern, meta.defaultValue));
                valLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox sliderHeader = new HBox(6, titleLabel, spacer, valLabel);

                Slider slider = new Slider(meta.min, meta.max, meta.defaultValue);
                slider.setShowTickMarks(false);
                slider.setShowTickLabels(false);

                Tooltip tt = new Tooltip(tooltipText);
                tt.setWrapText(true);
                tt.setMaxWidth(300);
                Tooltip.install(titleLabel, tt);
                Tooltip.install(valLabel, tt);
                Tooltip.install(slider, tt);

                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    valLabel.setText(String.format(meta.formatPattern, newVal.doubleValue()));
                    if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                    host.updatePreviewCanvas();
                });

                if (s == 0) geologyAbundanceSliders.put(layerIdx, slider);
                else if (s == 1) geologyThresholdSliders.put(layerIdx, slider);
                else if (s == 2) geologyParam3Sliders.put(layerIdx, slider);
                else if (s == 3) geologyParam4Sliders.put(layerIdx, slider);

                VBox sBox = new VBox(1, sliderHeader, slider);
                slidersContainer.getChildren().add(sBox);
            }
            geologySliderTitleLabels.put(layerIdx, sliderTitles);

            VBox proceduralBox = new VBox(6, seedBox, slidersContainer);
            proceduralBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            proceduralBox.setVisible(!hasImage);
            proceduralBox.setManaged(!hasImage);

            // --- Import Configuration Sub-Box ---
            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            geologySourceLabels.put(layerIdx, sourceLbl);
            ComboBox<String> sourceCombo = buildGeologySourceCombo(layerIdx);
            geologySourceCombos.put(layerIdx, sourceCombo);

            Label fileLbl = new Label("—");
            fileLbl.setStyle("-fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            fileLbl.setWrapText(true);
            geologyFileLabels.put(layerIdx, fileLbl);

            Button loadBtn = new Button(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            loadBtn.getStyleClass().add("button-secondary");
            loadBtn.setStyle("-fx-font-size: 11px;");
            loadBtn.setOnAction(e -> loadCustomGeologyLayer(layerIdx, img -> {
                customGeologyLayerImages.put(layerIdx, img);
                fileLbl.setText(I18n.getOrDefault("resource.status.custom_layer_active", "Fichier personnalisé chargé"));
                radioImport.setSelected(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            }));
            geologyLoadBtns.put(layerIdx, loadBtn);

            Label formatHintLbl = new Label(getGeologyFormatHint(layerIdx));
            formatHintLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
            geologyFormatLabels.put(layerIdx, formatHintLbl);

            HBox fileRow = new HBox(8, loadBtn, fileLbl);
            fileRow.setAlignment(Pos.CENTER_LEFT);

            VBox importBox = new VBox(6, new HBox(6, sourceLbl, sourceCombo), fileRow, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(245,158,11,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            radioProc.setOnAction(e -> {
                proceduralBox.setVisible(true);
                proceduralBox.setManaged(true);
                importBox.setVisible(false);
                importBox.setManaged(false);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            radioImport.setOnAction(e -> {
                proceduralBox.setVisible(false);
                proceduralBox.setManaged(false);
                importBox.setVisible(true);
                importBox.setManaged(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            HBox radioBox = new HBox(12, radioProc, radioImport);
            radioBox.setAlignment(Pos.CENTER_LEFT);

            subBlock.getChildren().addAll(subTitle, radioBox, proceduralBox, importBox);
            geologyLayersDynamicContainer.getChildren().add(subBlock);
        }
    }

    /**
     * Switches all 10 geological tensors to procedural generation and invalidates caches.
     */
    public void generateProceduralGeologyTensors() {
        if (geologyProcRadios != null) {
            for (var entry : geologyProcRadios.entrySet()) {
                if (entry.getValue() != null) entry.getValue().setSelected(true);
            }
        }
        customGeologyLayerImages.clear();
        if (geologyFileLabels != null) {
            for (Integer idx : geologyFileLabels.keySet()) {
                if (geologyFileLabels.get(idx) != null) geologyFileLabels.get(idx).setText("—");
            }
        }
        if (geologySeedInput != null && !geologySeedInput.getText().isBlank()) {
            try {
                long baseSeed = Long.parseLong(geologySeedInput.getText().trim());
                if (geologyTensorSeeds != null) {
                    for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
                        TextField tf = geologyTensorSeeds.get(i);
                        if (tf != null) tf.setText(String.valueOf(baseSeed + i * 777L));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        host.selectViewMode(3);
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
    }

    /**
     * Prepopulates empirical geological tensors according to planet body and epoch year.
     *
     * @param planetKey planetary canonical key
     * @param year historical epoch year
     */
    public void prepopulateGeologyTensors(String planetKey, long year) {
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
                    String fileName = body + "_" + layerNames[i] + ".png";
                    img = ImageMapLoader.loadMapImage(fileName);
                }
                if (img != null && img.getWidth() > 0) {
                    customGeologyLayerImages.put(i, img);
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
                        customGeologyLayerImages.put(i, bufferedImageToFXImage(bImg));
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                if (geologyFileLabels.containsKey(i)) {
                    geologyFileLabels.get(i).setText(getGeologyBaselineName(i, body));
                }
                if (geologyImportRadios.containsKey(i)) {
                    geologyImportRadios.get(i).setSelected(true);
                }
                if (geologySourceCombos.containsKey(i)) {
                    ComboBox<String> cb = geologySourceCombos.get(i);
                    if (cb != null) {
                        boolean matched = false;
                        for (String item : cb.getItems()) {
                            if (item == null || item.isEmpty()) continue;
                            String itmLow = item.toLowerCase();
                            boolean matches = false;
                            if (body.equals("earth") && (itmLow.contains("earth") || itmLow.contains("terre") || itmLow.contains("terran"))) {
                                matches = true;
                            } else if (body.equals("mars") && itmLow.contains("mars")) {
                                matches = true;
                            } else if (body.equals("venus") && (itmLow.contains("venus") || itmLow.contains("vénus"))) {
                                matches = true;
                            } else if (body.equals("moon") && (itmLow.contains("moon") || itmLow.contains("lune") || itmLow.contains("lunar") || itmLow.contains("lola"))) {
                                matches = true;
                            } else if (body.equals("mercury") && (itmLow.contains("mercury") || itmLow.contains("mercure") || itmLow.contains("messenger"))) {
                                matches = true;
                            } else if (itmLow.contains(body)) {
                                matches = true;
                            }
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
                }
            }
        } catch (Exception e) {
            logger.error("Failed to pre-populate geology tensors for {}", body, e);
        }
    }

    /**
     * Prepopulates Earth's empirical geology tensors.
     */
    public void prepopulateEarthGeologyTensors() {
        prepopulateGeologyTensors("earth", host.getEpochYear());
    }

    /**
     * Evaluates procedural sample value for a specific geological tensor layer at (\( \lambda, \phi \)).
     *
     * @param layerIdx tensor layer index (0 to 9)
     * @param lon longitude in degrees [-180, +180]
     * @param lat latitude in degrees [-90, +90]
     * @param planet planetary preset parameters
     * @return normalized physical intensity [0.0, 1.0]
     */
    public double sampleProceduralGeologyTensor(int layerIdx, double lon, double lat, PlanetPreset planet) {
        double p1 = geologyAbundanceSliders.containsKey(layerIdx) ? geologyAbundanceSliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][0].defaultValue;
        double p2 = geologyThresholdSliders.containsKey(layerIdx) ? geologyThresholdSliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][1].defaultValue;
        double p3 = geologyParam3Sliders.containsKey(layerIdx) ? geologyParam3Sliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][2].defaultValue;
        double p4 = geologyParam4Sliders.containsKey(layerIdx) ? geologyParam4Sliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][3].defaultValue;

        double[][] baseHotspots = switch (layerIdx) {
            case 0 -> COAL_SPOTS;
            case 1 -> OIL_SPOTS;
            case 2 -> GAS_SPOTS;
            case 3 -> URANIUM_SPOTS;
            case 4 -> HE3_SPOTS;
            case 5 -> IRON_COPPER_SPOTS;
            case 6 -> PRECIOUS_METAL_SPOTS;
            case 7 -> RARE_EARTH_SPOTS;
            case 8 -> MANTLE_HEAT_SPOTS;
            default -> AQUIFER_SPOTS;
        };

        double baseVal = sampleHotspotVal(lon, lat, baseHotspots);

        long seed = 12345L + (layerIdx + 1) * 9876543L;
        if (geologyTensorSeeds.containsKey(layerIdx) && geologyTensorSeeds.get(layerIdx) != null) {
            try {
                seed = Long.parseLong(geologyTensorSeeds.get(layerIdx).getText().trim());
            } catch (NumberFormatException ignored) {}
        }

        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);
        double x = Math.cos(latR) * Math.cos(lonR);
        double y = Math.cos(latR) * Math.sin(lonR);
        double z = Math.sin(latR);

        SimplexNoise noise1 = getOrCreateNoise(seed);
        SimplexNoise noise2 = getOrCreateNoise(seed + 99999L);
        double scale1 = 2.5 + (layerIdx % 5) * 1.2;
        double scale2 = 5.5 + (layerIdx % 4) * 1.8;
        double n1 = (noise1.noise(x * scale1, y * scale1, z * scale1) + 1.0) * 0.5;
        double n2 = (noise2.noise(x * scale2, y * scale2, z * scale2) + 1.0) * 0.5;

        double combined;

        switch (layerIdx) {
            case 0 -> { // COAL
                double biomassMask = (n1 > (1.0 - p2)) ? 1.0 : 0.0;
                double rankMult = 0.5 + p3 * 0.5;
                double depthMult = Math.clamp(p4 / 5.0, 0.2, 2.0);
                combined = (baseVal * 0.5 + n1 * 0.3 + n2 * 0.2) * biomassMask * rankMult * depthMult * p1;
            }
            case 1 -> { // CRUDE_OIL
                double tocMask = (n1 > (1.0 - p2 * 0.8)) ? 1.0 : 0.2;
                double trapBonus = 0.5 + p3 * 0.8 * n2;
                double tempFactor = 1.0 - Math.abs(p4 - 120.0) / 100.0;
                tempFactor = Math.clamp(tempFactor, 0.1, 1.0);
                combined = (baseVal * 0.5 + n1 * 0.5) * tocMask * trapBonus * tempFactor * p1;
            }
            case 2 -> { // NATURAL_GAS
                double shaleBoost = 0.4 + p2 * 0.6;
                double depthPres = Math.clamp(p3 / 4.0, 0.3, 1.8);
                double sealMask = 0.3 + p4 * 0.7;
                combined = (baseVal * 0.4 + n1 * 0.4 + n2 * 0.2) * shaleBoost * depthPres * sealMask * p1;
            }
            case 3 -> { // URANIUM
                double ppmNorm = p1 / 250.0;
                double hydroBoost = 0.5 + p2 * 1.0 * n2;
                double thUFactor = 1.0 + (p3 - 3.5) * 0.1;
                double unconformityMask = 0.6 + p4 * 0.8 * (n1 > 0.6 ? 1.5 : 0.5);
                combined = (baseVal * 0.4 + n1 * 0.6) * ppmNorm * hydroBoost * thUFactor * unconformityMask;
            }
            case 4 -> { // HELIUM_3
                double ppbNorm = p1 / 15.0;
                double ilmeniteBoost = 0.4 + p2 * 0.8;
                double solarIon = Math.clamp(p3, 0.0, 5.0);
                double churnLoss = Math.clamp(1.0 - (p4 - 3.0) * 0.05, 0.5, 1.2);
                double atmShield = planet.atmospherePressureAtm() > 0.1 ? 0.001 : 1.0;
                combined = (baseVal * 0.5 + n1 * 0.5) * ppbNorm * ilmeniteBoost * solarIon * churnLoss * atmShield;
            }
            case 5 -> { // IRON_COPPER
                double bifLayer = (n1 > 0.4) ? p3 * 0.8 : 0.2;
                double magmaticPorphyry = p2 * 1.2 * n2 * (p4 / 0.8);
                combined = (baseVal * 0.4 + bifLayer * 0.3 + magmaticPorphyry * 0.3) * p1;
            }
            case 6 -> { // PRECIOUS_METALS (Au, Ag, Pt)
                double placer = p2 * 1.2 * (n1 > 0.4 ? 1.2 : 0.2);
                double epithermal = p3 * 1.4 * (n2 > 0.5 ? 1.5 : 0.3);
                double porphyryAu = (p4 / 2.0) * 0.6;
                combined = (baseVal * 0.45 + placer * 0.25 + epithermal * 0.2 + porphyryAu * 0.1) * p1;
            }
            case 7 -> { // CRITICAL_REE (Terres Rares, Bastnasite, Li)
                double carbonatite = p2 * 1.5 * (n1 > 0.6 ? 1.6 : 0.2);
                double salarLi = p3 * 1.3 * (n2 > 0.5 ? 1.2 : 0.1);
                double spodumene = (p4 / 1.5) * 0.5;
                combined = (baseVal * 0.4 + carbonatite * 0.25 + salarLi * 0.2 + spodumene * 0.15) * p1;
            }
            case 8 -> { // MANTLE_HEAT
                double heatNorm = p1 / 65.0;
                double plumeAnomalies = 1.0 + p2 * 1.5 * n1;
                double cratonInsulation = Math.clamp(1.0 - (p3 - 150.0) / 300.0, 0.3, 1.5);
                combined = (baseVal * 0.5 + n1 * 0.5) * heatNorm * plumeAnomalies * cratonInsulation;
            }
            default -> { // AQUIFERS
                double volumeNorm = p1;
                double permFactor = 0.5 + p2 * 0.8 * n1;
                double porosityBoost = p3 / 18.0;
                double permafrostShield = Math.clamp(1.0 - p4 / 1000.0, 0.1, 1.0);
                combined = (baseVal * 0.4 + n2 * 0.6) * volumeNorm * permFactor * porosityBoost * permafrostShield;
            }
        }
        return Math.clamp(combined, 0.0, 1.0);
    }

    /**
     * Resolves the high-contrast display color for a specific geological tensor value.
     *
     * @param layerIdx tensor layer index
     * @param val physical value in [0.0, 1.0]
     * @return blended JavaFX Color
     */
    public Color getGeologyResourceColor(int layerIdx, double val) {
        val = Math.clamp(val, 0.0, 1.0);
        return switch (layerIdx) {
            case 0 -> lerpColorFx(Color.rgb(146, 64, 14), Color.rgb(254, 240, 138), val); // Coal (Amber to Bright Core)
            case 1 -> lerpColorFx(Color.rgb(153, 27, 27), Color.rgb(254, 202, 202), val); // Oil (Ruby to Crimson Core)
            case 2 -> lerpColorFx(Color.rgb(14, 116, 144), Color.rgb(207, 250, 254), val); // Gas (Cyan to Electric Core)
            case 3 -> lerpColorFx(Color.rgb(20, 83, 45), Color.rgb(74, 222, 128), val);   // Uranium (Emerald)
            case 4 -> lerpColorFx(Color.rgb(76, 29, 149), Color.rgb(192, 132, 252), val); // Helium-3 (Violet/Purple)
            case 5 -> lerpColorFx(Color.rgb(139, 69, 19), Color.rgb(249, 115, 22), val);  // Iron & Copper (Terracotta/Orange)
            case 6 -> lerpColorFx(Color.rgb(161, 98, 7), Color.rgb(250, 204, 21), val);   // Precious Metals (Radiant Gold)
            case 7 -> lerpColorFx(Color.rgb(13, 148, 136), Color.rgb(45, 212, 191), val); // Rare Earths (Teal/Turquoise)
            case 8 -> lerpColorFx(Color.rgb(185, 28, 28), Color.rgb(253, 224, 71), val);  // Mantle Heat Flow (Red/Yellow)
            default -> lerpColorFx(Color.rgb(30, 58, 138), Color.rgb(96, 165, 250), val); // Freshwater Aquifers (Azure/Sky)
        };
    }

    /**
     * Generates a high-resolution raster image of the specified procedural geological tensor.
     *
     * @param layerIdx tensor layer index
     * @param width image width in pixels
     * @param height image height in pixels
     * @return generated JavaFX Image
     */
    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                boolean isLand = wLvl <= -0.4 || pt.elevation() >= wLvl;
                Color baseBackground = isLand ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);
                double val = sampleProceduralGeologyTensor(layerIdx, lon, lat, planet);
                val = Math.clamp(val, 0.0, 1.0);
                Color pxColor;
                if (val > 0.02) {
                    Color resourceColor = getGeologyResourceColor(layerIdx, val);
                    pxColor = blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
                } else {
                    pxColor = baseBackground;
                }
                pw.setColor(x, y, pxColor);
            }
        }
        return img;
    }

    /**
     * Exports a specific geological tensor layer via file save dialog.
     *
     * @param layerIdx tensor layer index
     */
    public void exportGeologyTensor(int layerIdx) {
        boolean isImport = geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
        Image img = (isImport && customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null)
                ? customGeologyLayerImages.get(layerIdx)
                : generateProceduralGeologyRasterImage(layerIdx, 1024, 512);
        String defaultName = "geology_tensor_" + (layerIdx + 1) + ".png";
        WindowUtils.exportImageWithChooser(host.getWindow(),
                img, defaultName, I18n.getOrDefault("resource.dialog.export_geology_tensor_title", "Export Geology Tensor Map (PNG / JPEG)"));
    }

    private void loadCustomGeologyLayer(int layerIdx, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.dialog.import_geology_title", "Import geological map ") + getGeologyTensorTitle(layerIdx));
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Données Spatiales & Tenseurs (PNG, ASC, GeoJSON, TIF, JPG)", "*.png", "*.asc", "*.geojson", "*.json", "*.tif", "*.tiff", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                java.awt.Color fallbackCol = ResourceDistributionRasterExporter.getGeologyTensorAwtColor(layerIdx);
                BufferedImage bImg = ResourceDepositMapReader.readGeologicalDataset(file, 720, 360, fallbackCol);
                if (bImg != null) {
                    onLoaded.accept(bufferedImageToFXImage(bImg));
                } else {
                    Image img = new Image(new FileInputStream(file));
                    onLoaded.accept(img);
                }
            } catch (Exception ex) {
                logger.error("Failed to load geology layer {}", layerIdx, ex);
            }
        }
    }

    private void showGeologyImportFormatHelp() {
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
        if (host.getWindow() != null) alert.initOwner(host.getWindow());
        alert.showAndWait();
    }

    private void exportGisMultiFormat() {
        ResourceDistributionRasterExporter.exportGisMultiFormat(
                host.getWindow(),
                host.getActivePlanetPreset()
        );
    }

    private void exportProvenanceManifest() {
        ResourceDistributionRasterExporter.exportProvenanceManifest(
                host.getWindow(),
                geologySeedInput != null ? geologySeedInput.getText() : "45678"
        );
    }

    public String getGeologyTensorTitle(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.title", "4.1 Bassins Houillers & Charbon (Gt)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.title", "4.2 Réservoirs de Pétrole Brut (Gt)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.title", "4.3 Champs de Gaz Naturel (10¹² m³)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.title", "4.4 Gisements d'Uranium & Thorium (ppm)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.title", "4.5 Hélium-3 & Régolithe Volatil (ppb)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.title", "4.6 Fer, Cuivre & Métaux de Base (Gt)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.title", "4.7 Or, Argent & Métaux Précieux (kt)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.title", "4.8 Terres Rares, Lithium & Métaux Stratégiques (Mt)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.title", "4.9 Flux Géothermique & Chaleur Mantellique (mW/m²)");
            default -> I18n.getOrDefault("resource.tensor.10.title", "4.10 Nappes Phréatiques & Aquifères Fossiles (10³ km³)");
        };
    }

    public String getGeologyTensorTooltip(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.tooltip", "Couche raster de charbon (Gt). USGS World Coal Quality / BGR Energy Study.");
            case 1 -> I18n.getOrDefault("resource.tensor.2.tooltip", "Réserves de pétrole conventionnel et schistes bitumineux. USGS DDS-69.");
            case 2 -> I18n.getOrDefault("resource.tensor.3.tooltip", "Réserves de gaz conventionnel et de roche-mère sous pression. IEA Global Gas Tracker.");
            case 3 -> I18n.getOrDefault("resource.tensor.4.tooltip", "Concentrations en minerais fissiles (U, Th). AIEA UDEPO.");
            case 4 -> I18n.getOrDefault("resource.tensor.5.tooltip", "Piégeage de l'Hélium-3 dans les basaltes régolithiques par le vent solaire. NASA LRO.");
            case 5 -> I18n.getOrDefault("resource.tensor.6.tooltip", "Formations ferrifères rubanées et porphyres cuprifères. USGS MRDS.");
            case 6 -> I18n.getOrDefault("resource.tensor.7.tooltip", "Minéralisations épithermales et placers alluvionnaires en métaux précieux.");
            case 7 -> I18n.getOrDefault("resource.tensor.8.tooltip", "Complexes de carbonatites et salars de lithium nécessaires à l'électronique de pointe.");
            case 8 -> I18n.getOrDefault("resource.tensor.9.tooltip", "Énergie géothermique et panaches mantelliques. Global Heat Flow Commission (IHFC).");
            default -> I18n.getOrDefault("resource.tensor.10.tooltip", "Réservoirs hydrogéologiques sous-terrains et aquifères fossiles profonds. UNESCO WHYMAP.");
        };
    }

    public String getGeologyBaselineName(int index, String planetKey) {
        if ("mars".equalsIgnoreCase(planetKey)) {
            return switch (index) {
                case 3 -> "Mars MRO CRISM Uranium-Thorium Spectrometry Map";
                case 4 -> "Mars Regolith Solar Wind He-3 Trapping [NASA MAVEN]";
                case 5 -> "Mars Global Surveyor TES Hematite & Iron Crust Map";
                case 7 -> "Mars Syrtis Major Carbonatite & Olivine REE Survey";
                case 8 -> "Mars InSight SEIS Lithospheric Thermal Gradient";
                case 9 -> "Mars Deep Subglacial Aquifer Radar [MARSIS ESA]";
                default -> "Mars Procedural Simulation Baseline";
            };
        } else if ("venus".equalsIgnoreCase(planetKey)) {
            return switch (index) {
                case 5 -> "Venus Magellan Radar SAR Basaltic Crustal Iron Map";
                case 8 -> "Venus Hotspot Volcanism & Thermal Plume Network [Venera]";
                default -> "Venus Procedural Simulation Baseline";
            };
        } else if ("moon".equalsIgnoreCase(planetKey)) {
            return switch (index) {
                case 4 -> "Moon Chang'e / LRO LAMP Lunar Ilmenite He-3 Distribution";
                case 5 -> "Moon Clementine Lunar Basalt Iron Concentration (FeO %)";
                case 7 -> "Moon KREEP Terrane Rare Earth & Thorium Oceanus Procellarum";
                case 9 -> "Moon Permanently Shadowed Craters PSR Ice Water [LOLA]";
                default -> "Moon Procedural Simulation Baseline";
            };
        } else if ("mercury".equalsIgnoreCase(planetKey)) {
            return switch (index) {
                case 5 -> "Mercury MESSENGER XRS High-Magnesium & Iron Crust Map";
                case 7 -> "Mercury Volatile Potassium & Rare Metal Spectrometry";
                default -> "Mercury Procedural Simulation Baseline";
            };
        }

        return switch (index) {
            case 0 -> "USGS World Coal Quality Inventory & BGR Global Reserves [720x360]";
            case 1 -> "USGS DDS-69 World Petroleum Assessment & Rystad Energy [720x360]";
            case 2 -> "IEA Global Gas Tracker & Marine Shale Formations [720x360]";
            case 3 -> "IAEA UDEPO World Uranium Geology & Red Book Database [720x360]";
            case 4 -> "Atmospheric Shielding & Trace Solar Regolith He-3 Retention [720x360]";
            case 5 -> "USGS MRDS Mineral Resources Data System - Base Metals [720x360]";
            case 6 -> "World Gold Council & USGS Precious Metals Inventory [720x360]";
            case 7 -> "BGS World Mineral Production - Critical Metals & Lithium Salars [720x360]";
            case 8 -> "IHFC Global Heat Flow Database & Mantle Plume Model [720x360]";
            default -> "UNESCO WHYMAP World Hydrogeological Map & GRACE-FO [720x360]";
        };
    }

    public String getGeologyFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.format", "PNG 8-bit / GeoTIFF / NetCDF (0=Néant, 255=Veines maximales >5 Gt)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Réserves majeures >10 Gt)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Bassins haute pression)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.format", "PNG 8-bit / ASC (0=Fond crustal, 255=Filons pechblende >1000 ppm)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Régolithe enrichi >50 ppb)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Gisements porphyres massifs)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Districts aurifères majeurs)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.format", "PNG 8-bit / GeoTIFF (0=Néant, 255=Carbonatites & salars Li)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.format", "PNG 8-bit / ASC (0=Croûte froide 20 mW/m², 255=Plumes >200 mW/m²)");
            default -> I18n.getOrDefault("resource.tensor.10.format", "PNG 8-bit / NetCDF (0=Roche sèche, 255=Aquifères profonds géants)");
        };
    }

    private ComboBox<String> buildGeologySourceCombo(int index) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("combo-box");
        combo.setStyle("-fx-font-size: 11px;");

        combo.getItems().add(I18n.getOrDefault("resource.source.procedural", "▶ Procédural pur (Formules analytiques)"));

        switch (index) {
            case 0 -> {
                combo.getItems().add("🌍 Terre — USGS World Coal Quality Inventory & BGR Energy");
                combo.getItems().add("🌍 Terre — Paléo-Carbonifère Parallique (-300 Ma)");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 1 -> {
                combo.getItems().add("🌍 Terre — USGS DDS-69 World Petroleum Assessment");
                combo.getItems().add("🌍 Terre — Rystad Energy UCube Offshore & Onshore");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 2 -> {
                combo.getItems().add("🌍 Terre — IEA Global Gas Tracker & Deep Shales");
                combo.getItems().add("🌍 Terre — Bassins Cratoniques Conventionnels");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 3 -> {
                combo.getItems().add("🌍 Terre — IAEA UDEPO World Uranium Deposit Database");
                combo.getItems().add("🔴 Mars — MRO CRISM Uranium-Thorium Spectrometry Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 4 -> {
                combo.getItems().add("🌕 Lune — Chang'e / LRO LAMP Ilmenite He-3 Distribution");
                combo.getItems().add("🔴 Mars — MAVEN Solar Wind Regolith Implantation");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 5 -> {
                combo.getItems().add("🌍 Terre — USGS MRDS Mineral Resources Data System");
                combo.getItems().add("🔴 Mars — MGS TES Hematite & Iron Crust Global Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 6 -> {
                combo.getItems().add("🌍 Terre — World Gold Council & USGS Precious Metals");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 7 -> {
                combo.getItems().add("🌍 Terre — BGS Critical Minerals & Lithium Salars Survey");
                combo.getItems().add("🌕 Lune — KREEP Terrane Rare Earth & Thorium Procellarum");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 8 -> {
                combo.getItems().add("🌍 Terre — IHFC Global Heat Flow Commission Map");
                combo.getItems().add("🔴 Mars — InSight SEIS Lithospheric Thermal Gradient");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            default -> {
                combo.getItems().add("🌍 Terre — UNESCO WHYMAP World Hydrogeological Map & GRACE-FO");
                combo.getItems().add("🔴 Mars — MARSIS ESA Subglacial Deep Aquifer Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
        }

        combo.setValue(combo.getItems().size() > 1 ? combo.getItems().get(1) : combo.getItems().get(0));

        combo.setOnAction(e -> {
            String val = combo.getValue();
            if (val != null) {
                if (val.contains("USGS") || val.contains("BGR") || val.contains("IEA") || val.contains("IAEA") || val.contains("BGS") || val.contains("IHFC") || val.contains("UNESCO") || val.contains("Terre") || val.contains("Mars") || val.contains("Lune")) {
                    if (geologyImportRadios.containsKey(index)) geologyImportRadios.get(index).setSelected(true);
                } else if (val.contains("Procédural")) {
                    if (geologyProcRadios.containsKey(index)) geologyProcRadios.get(index).setSelected(true);
                }
                host.selectViewMode(host.getGeologyViewComboIndex(index));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            }
        });
        return combo;
    }

    /**
     * Updates localized user-facing UI labels and tooltips across all 5 supported languages.
     */
    public void updateTexts() {
        if (geologyDomainSecHeader != null) {
            geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        }
        if (sec41Header != null) {
            sec41Header.setText(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        }
        if (sec42Header != null) {
            sec42Header.setText(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        }
        if (btnGenerateProceduralGeologyTensors != null) {
            btnGenerateProceduralGeologyTensors.setText(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
            btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors", "Switch all geological tensors to procedural mode and regenerate maps.")));
        }
        if (geoSeedLabel != null) {
            geoSeedLabel.setText(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        }
        if (geologyHelpBtn != null) {
            geologyHelpBtn.setText(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        }
        if (btnExportGisMultiFormat != null) {
            btnExportGisMultiFormat.setText(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        }
        if (btnExportProvenanceManifest != null) {
            btnExportProvenanceManifest.setText(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        }

        for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
            if (geologySubTitles.containsKey(i) && geologySubTitles.get(i) != null) {
                geologySubTitles.get(i).setText(getGeologyTensorTitle(i));
                Tooltip.install(geologySubTitles.get(i), new Tooltip(getGeologyTensorTooltip(i)));
            }
            if (geologyProcRadios.containsKey(i) && geologyProcRadios.get(i) != null) {
                geologyProcRadios.get(i).setText(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            }
            if (geologyImportRadios.containsKey(i) && geologyImportRadios.get(i) != null) {
                geologyImportRadios.get(i).setText(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            }
            if (geologySeedLabels.containsKey(i) && geologySeedLabels.get(i) != null) {
                geologySeedLabels.get(i).setText(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            }
            if (geologyGenBtns.containsKey(i) && geologyGenBtns.get(i) != null) {
                geologyGenBtns.get(i).setText(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            }
            if (geologySourceLabels.containsKey(i) && geologySourceLabels.get(i) != null) {
                geologySourceLabels.get(i).setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            }
            if (geologyLoadBtns.containsKey(i) && geologyLoadBtns.get(i) != null) {
                geologyLoadBtns.get(i).setText(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            }
            if (geologyFormatLabels.containsKey(i) && geologyFormatLabels.get(i) != null) {
                geologyFormatLabels.get(i).setText(getGeologyFormatHint(i));
            }
            if (geologySliderTitleLabels.containsKey(i)) {
                List<Label> titles = geologySliderTitleLabels.get(i);
                for (int s = 0; s < titles.size() && s < 4; s++) {
                    TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];
                    titles.get(s).setText(I18n.getOrDefault(meta.labelKey, meta.defaultLabel));
                    Tooltip.install(titles.get(s), new Tooltip(I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip)));
                }
            }
        }
    }

    // --- Accessors for Host Panel ---

    public Map<Integer, Image> getCustomGeologyLayerImages() {
        return customGeologyLayerImages;
    }

    public boolean isGeologyImportActive(int layerIdx) {
        return geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
    }

    public Image getGeologyLayerImage(int layerIdx) {
        return customGeologyLayerImages.get(layerIdx);
    }

    private SimplexNoise getOrCreateNoise(long seed) {
        return noiseCache.computeIfAbsent(seed, SimplexNoise::new);
    }

    private static double sampleHotspotVal(double lng, double lat, double[][] spots) {
        double val = 0.0;
        for (double[] spot : spots) {
            double dlng = lng - spot[0];
            double dlat = lat - spot[1];
            double dist = Math.sqrt(dlng * dlng + dlat * dlat);
            double radDeg = spot[2] / 4.0;
            if (dist < radDeg) {
                double intensity = spot.length > 3 ? spot[3] : 1.0;
                val += Math.pow(1.0 - (dist / radDeg), 1.5) * intensity;
            }
        }
        return val;
    }

    private static Color lerpColorFx(Color c1, Color c2, double t) {
        double r = Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0.0, 1.0);
        double g = Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0.0, 1.0);
        double b = Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0.0, 1.0);
        return Color.color(r, g, b);
    }

    private static Color blendColors(Color c1, Color c2, double weightC2) {
        double w2 = Math.clamp(weightC2, 0.0, 1.0);
        double w1 = 1.0 - w2;
        return Color.color(
                Math.clamp(c1.getRed() * w1 + c2.getRed() * w2, 0.0, 1.0),
                Math.clamp(c1.getGreen() * w1 + c2.getGreen() * w2, 0.0, 1.0),
                Math.clamp(c1.getBlue() * w1 + c2.getBlue() * w2, 0.0, 1.0)
        );
    }

    private static Image bufferedImageToFXImage(BufferedImage bImg) {
        if (bImg == null) return null;
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        WritableImage fxImg = new WritableImage(w, h);
        PixelWriter pw = fxImg.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, bImg.getRGB(x, y));
            }
        }
        return fxImg;
    }
}
