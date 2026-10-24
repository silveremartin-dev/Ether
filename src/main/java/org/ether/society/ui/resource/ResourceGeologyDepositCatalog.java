/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import org.ether.society.i18n.I18n;

/**
 * Empirical cartographic hotspot database and physical tensor catalog for geological resources.
 * <p>
 * Contains real-world geological deposit coordinates (USGS, IAEA, UNESCO, IEA, IHFC)
 * across 10 canonical resource tensors (Coal, Crude Oil, Natural Gas, Uranium, Helium-3,
 * Iron/Copper, Precious Metals, REE/Critical, Mantle Heat Flow, Aquifers).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ResourceGeologyDepositCatalog {

    private ResourceGeologyDepositCatalog() {
        // Utility class
    }

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

    public static final double[][] COAL_SPOTS = {
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

    public static final double[][] OIL_SPOTS = {
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

    public static final double[][] GAS_SPOTS = {
        {52.5, 25.5, 80, 1.8}, {51.5, 26.5, 75, 1.7}, {53.0, 27.5, 70, 1.6}, {59.5, 36.5, 60, 1.4},
        {76.0, 66.5, 85, 1.8}, {73.0, 71.0, 80, 1.7}, {77.0, 65.0, 75, 1.6}, {54.0, 38.0, 65, 1.5},
        {-80.0, 40.0, 70, 1.6}, {-94.0, 32.0, 65, 1.5}, {-97.0, 33.0, 60, 1.4}, {-104.0, 32.0, 60, 1.4},
        {3.0, 60.5, 65, 1.5}, {6.8, 53.2, 55, 1.4}, {34.5, 33.0, 55, 1.4}, {32.5, 32.5, 60, 1.5},
        {8.0, 32.0, 60, 1.4}, {2.5, 28.0, 55, 1.3}, {115.0, -19.5, 60, 1.4}, {130.0, -11.0, 50, 1.3}
    };

    public static final double[][] URANIUM_SPOTS = {
        {67.5, 44.5, 60, 1.5}, {69.0, 43.5, 55, 1.4}, {-105.5, 58.0, 65, 1.6}, {-104.0, 57.5, 60, 1.5},
        {132.5, -12.5, 60, 1.5}, {137.0, -30.5, 65, 1.6}, {15.0, -22.5, 55, 1.4}, {8.0, 18.5, 50, 1.3},
        {117.0, 50.0, 50, 1.3}, {69.0, 67.5, 50, 1.3}, {27.0, -11.5, 50, 1.3}, {-108.0, 35.5, 45, 1.2}
    };

    public static final double[][] HE3_SPOTS = {
        {23.5, 16.5, 60, 1.5}, {-58.0, -26.0, 55, 1.4}, {17.5, 2.5, 50, 1.3}, {30.5, 8.5, 55, 1.4},
        {-20.0, 9.5, 45, 1.2}, {0.0, 18.0, 50, 1.3}, {180.0, 0.0, 50, 1.3}, {0.0, 0.0, 50, 1.3}
    };

    public static final double[][] IRON_COPPER_SPOTS = {
        {-50.5, -6.0, 65, 1.6}, {-43.5, -20.0, 60, 1.5}, {119.5, -23.0, 70, 1.7}, {118.0, -22.0, 65, 1.6},
        {36.5, 51.5, 60, 1.5}, {20.0, 67.8, 55, 1.4}, {86.0, 22.0, 50, 1.3}, {111.0, 41.0, 55, 1.4},
        {-69.0, -22.5, 75, 1.8}, {-70.5, -31.5, 70, 1.7}, {-71.0, -16.5, 65, 1.6}, {-110.5, 33.0, 55, 1.4},
        {28.0, -12.5, 65, 1.6}, {26.0, -11.0, 60, 1.5}, {-89.0, 47.0, 50, 1.3}, {136.5, -30.5, 60, 1.5}
    };

    public static final double[][] PRECIOUS_METAL_SPOTS = {
        {27.5, -26.5, 75, 1.8}, {28.0, -25.5, 70, 1.7}, {-116.0, 40.5, 65, 1.6}, {-117.0, 39.0, 60, 1.5},
        {121.5, -30.5, 60, 1.5}, {88.0, 69.5, 65, 1.6}, {-69.5, -27.5, 60, 1.5}, {-78.0, -1.0, 55, 1.4},
        {137.0, -4.0, 65, 1.6}, {-2.0, 6.0, 55, 1.4}, {123.5, 53.5, 50, 1.3}, {-147.5, 65.0, 55, 1.4}
    };

    public static final double[][] RARE_EARTH_SPOTS = {
        {110.0, 41.5, 75, 1.8}, {116.0, 25.0, 65, 1.6}, {-67.5, -23.5, 70, 1.7}, {-68.5, -27.0, 65, 1.6},
        {-67.0, -20.0, 65, 1.6}, {128.0, -18.5, 60, 1.5}, {116.0, -33.5, 65, 1.6}, {-115.5, 35.5, 60, 1.5},
        {31.0, -25.5, 55, 1.4}, {48.5, 71.0, 55, 1.4}, {14.0, 46.0, 45, 1.2}, {76.0, 27.5, 50, 1.3}
    };

    public static final double[][] MANTLE_HEAT_SPOTS = {
        {-19.0, 64.5, 80, 1.8}, {-155.5, 19.5, 75, 1.7}, {-110.5, 44.5, 70, 1.6}, {14.5, 41.0, 60, 1.4},
        {139.5, 35.5, 65, 1.5}, {121.0, 14.5, 60, 1.4}, {176.0, -38.5, 65, 1.5}, {37.0, 0.5, 70, 1.6},
        {42.5, 11.5, 65, 1.5}, {-68.0, -24.0, 60, 1.4}, {-25.5, 38.0, 55, 1.3}, {100.0, -1.0, 65, 1.5}
    };

    public static final double[][] AQUIFER_SPOTS = {
        {25.0, 24.0, 85, 1.6}, {-56.0, -25.0, 80, 1.5}, {137.0, -26.0, 80, 1.5}, {-101.0, 36.0, 75, 1.4},
        {80.0, 27.0, 75, 1.4}, {2.0, 47.0, 55, 1.2}, {-60.0, -3.0, 90, 1.5}, {22.0, -1.0, 75, 1.3},
        {75.0, 60.0, 85, 1.4}, {122.0, -18.0, 60, 1.2}, {82.0, 39.0, 55, 1.2}, {-48.0, -1.5, 50, 1.2},
        {-118.0, 36.0, 45, 1.2}, {45.0, 25.0, 60, 1.3}, {16.0, 14.0, 65, 1.3}, {23.0, -22.0, 60, 1.2},
        {116.0, 37.0, 65, 1.3}, {70.0, 30.0, 65, 1.3}
    };

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

    public static double sampleHotspotVal(double lng, double lat, double[][] spots) {
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

    public static double[][] getHotspotsForLayer(int layerIdx) {
        return switch (layerIdx) {
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
    }

    public static String getGeologyTensorTitle(int index) {
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

    public static String getGeologyTensorTooltip(int index) {
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

    public static String getGeologyBaselineName(int index, String planetKey) {
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

    public static String getGeologyFormatHint(int index) {
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
}
