/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.i18n.I18n;

/**
 * Cartographic legend renderer for scenario preview modes.
 *
 * <p>Builds dynamic discrete color chips and multi-lingual technical tooltips
 * for demographic densities, cultural tensors, capital stocks, energy regimes,
 * and ecological footprints.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioPreviewLegendBuilder {

    private ScenarioPreviewLegendBuilder() {
        // Utility class
    }

    /**
     * Updates the legend container with contextual color chips and tooltips.
     *
     * @param curSel selected combo mode index
     * @param mode lower-cased mode string
     * @param dims number of cultural vector dimensions
     * @param container HBox container to populate
     */
    public static void populateLegend(int curSel, String mode, int dims, HBox container) {
        if (container == null) return;
        container.getChildren().clear();

        Color[] colors;
        String[] labels;
        String[] fullTooltips;

        if (curSel == 0) {
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                I18n.getOrDefault("setup.legend.demo.0", "Inhabité (0)"),
                I18n.getOrDefault("setup.legend.demo.1", "Faible (< 5/km²)"),
                I18n.getOrDefault("setup.legend.demo.2", "Moyen (5-25)"),
                I18n.getOrDefault("setup.legend.demo.3", "Dense (25-100)"),
                I18n.getOrDefault("setup.legend.demo.4", "Métropole (> 100)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.demo.0.desc", "Cellules sans présence humaine détectée"),
                I18n.getOrDefault("setup.legend.demo.1.desc", "Communautés de chasseurs-cueilleurs ou pasteurs nomades"),
                I18n.getOrDefault("setup.legend.demo.2.desc", "Villages agricoles et bassins d'agriculture pluviale"),
                I18n.getOrDefault("setup.legend.demo.3.desc", "Grandes vallées alluviales et terroirs d'agriculture intensive"),
                I18n.getOrDefault("setup.legend.demo.4.desc", "Foyers urbains majeurs, cités-États et conurbations denses")
            };
        } else if (curSel >= 1 && curSel <= dims) {
            colors = new Color[]{
                Color.rgb(15, 23, 42), Color.rgb(30, 58, 138), Color.rgb(59, 130, 246), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                I18n.getOrDefault("setup.legend.cult.0", "Min (0.0)"),
                I18n.getOrDefault("setup.legend.cult.1", "Faible (0.25)"),
                I18n.getOrDefault("setup.legend.cult.2", "Médian (0.50)"),
                I18n.getOrDefault("setup.legend.cult.3", "Élevé (0.75)"),
                I18n.getOrDefault("setup.legend.cult.4", "Max (1.0)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.cult.0.desc", "Intensité nulle ou pôle opposé du trait culturel"),
                I18n.getOrDefault("setup.legend.cult.1.desc", "Faible expression ou pratique culturelle diffuse"),
                I18n.getOrDefault("setup.legend.cult.2.desc", "Valeur médiane, équilibre culturel"),
                I18n.getOrDefault("setup.legend.cult.3.desc", "Expression forte du trait dans la société"),
                I18n.getOrDefault("setup.legend.cult.4.desc", "Polarisation maximale du trait culturel dans la population")
            };
        } else {
            int derivedOffset = curSel - (dims + 1);
            if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
                colors = new Color[]{
                    Color.rgb(30, 41, 59), Color.rgb(14, 116, 144), Color.rgb(16, 185, 129), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.capital.0", "Rudimentaire (< 50 kg)"),
                    I18n.getOrDefault("setup.legend.capital.1", "Agraire (50-200 kg)"),
                    I18n.getOrDefault("setup.legend.capital.2", "Artisanal (200-500 kg)"),
                    I18n.getOrDefault("setup.legend.capital.3", "Manufacturier (500-2k)"),
                    I18n.getOrDefault("setup.legend.capital.4", "Industriel (> 2 000 kg)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.capital.0.desc", "Outils de pierre et de bois de subsistance"),
                    I18n.getOrDefault("setup.legend.capital.1.desc", "Socles d'araire, céramique, meules dormantes et silos en terre"),
                    I18n.getOrDefault("setup.legend.capital.2.desc", "Forges, moulins à eau, pressoirs et ateliers de tissage"),
                    I18n.getOrDefault("setup.legend.capital.3.desc", "Manufactures royales, hauts fourneaux et canaux de navigation"),
                    I18n.getOrDefault("setup.legend.capital.4.desc", "Machinisme lourd à vapeur, réseau ferré et aciéries")
                };
            } else if (derivedOffset == 2 || mode.contains("énergétique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energético") || mode.contains("e(x)")) {
                colors = new Color[]{
                    Color.rgb(69, 26, 3), Color.rgb(180, 83, 9), Color.rgb(234, 88, 12), Color.rgb(250, 204, 21), Color.rgb(254, 240, 138)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0", "Biomasse (< 10 MJ/hab)"),
                    I18n.getOrDefault("setup.legend.energy.1", "Traction (10-50 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.2", "Hydraulique/Charbon (50-200)"),
                    I18n.getOrDefault("setup.legend.energy.3", "Fossile (200-500 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.4", "Électrique (> 500 MJ)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0.desc", "Régime Biomasse : Chaleur du bois et travail musculaire"),
                    I18n.getOrDefault("setup.legend.energy.1.desc", "Régime Traction Animale : Attelages, bœufs et chevaux"),
                    I18n.getOrDefault("setup.legend.energy.2.desc", "Régime Mécanique : Énergie hydraulique, éolienne et débuts du charbon"),
                    I18n.getOrDefault("setup.legend.energy.3.desc", "Régime Hydrocarbures : Pétrole, gaz et thermodynamique industrielle"),
                    I18n.getOrDefault("setup.legend.energy.4.desc", "Régime Électrique Massif : Réseaux de puissance et transition énergétique")
                };
            } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
                colors = new Color[]{
                    Color.rgb(163, 230, 53), Color.rgb(132, 204, 22), Color.rgb(34, 197, 94), Color.rgb(5, 150, 105), Color.rgb(6, 78, 59)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.food.0", "< 1 Mois (Critique)"),
                    I18n.getOrDefault("setup.legend.food.1", "1–3 Mois (Faible)"),
                    I18n.getOrDefault("setup.legend.food.2", "3–6 Mois (Moyen)"),
                    I18n.getOrDefault("setup.legend.food.3", "6–12 Mois (Sécurisé)"),
                    I18n.getOrDefault("setup.legend.food.4", "> 12 Mois (Abondance)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.food.0.desc", "Réserves Critiques : Vulnérabilité immédiate à la moindre mauvaise récolte"),
                    I18n.getOrDefault("setup.legend.food.1.desc", "Réserves de Subsistance : Stocks saisonniers d'appoint"),
                    I18n.getOrDefault("setup.legend.food.2.desc", "Greniers Traditionnels : Capacité de soudure inter-annuelle"),
                    I18n.getOrDefault("setup.legend.food.3.desc", "Réserves Stratégiques : Silos régionaux prévenant toute disette"),
                    I18n.getOrDefault("setup.legend.food.4.desc", "Surplus Systémique : Chaînes logistiques agroalimentaires pérennes")
                };
            } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
                colors = new Color[]{
                    Color.rgb(55, 48, 163), Color.rgb(124, 58, 237), Color.rgb(219, 39, 119), Color.rgb(6, 182, 212), Color.rgb(224, 242, 254)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.info.0", "Oral (< 10 bits/hab)"),
                    I18n.getOrDefault("setup.legend.info.1", "Écrit & Parchemin (10-100)"),
                    I18n.getOrDefault("setup.legend.info.2", "Imprimerie (100-1k)"),
                    I18n.getOrDefault("setup.legend.info.3", "Médias Masse (1k-10k)"),
                    I18n.getOrDefault("setup.legend.info.4", "Numérique (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.info.0.desc", "Tradition Orale : Savoirs transmis par la mémoire et le chant"),
                    I18n.getOrDefault("setup.legend.info.1.desc", "Écrit & Manuscrits : Enregistrement sur argile, papyrus et parchemins"),
                    I18n.getOrDefault("setup.legend.info.2.desc", "Imprimerie Mécanique : Diffusion élargie des traités et encyclopédies"),
                    I18n.getOrDefault("setup.legend.info.3.desc", "Télécommunications : Presse quotidienne, télégraphe, radio et cinéma"),
                    I18n.getOrDefault("setup.legend.info.4.desc", "Société Numérique : Internet, calcul haute performance et bases de données")
                };
            } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fußabdruck") || mode.contains("huella") || mode.contains("malthus")) {
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(132, 204, 22), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68), Color.rgb(127, 29, 29)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (C₇=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression Modérée (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Écosystèmes à l'équilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression Légère : Forêt gérée, chasse et élevage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : Déforestation, érosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne Sévère : Pénuries de bois, raréfaction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Écologique : Désertification irréversible et disette systémique")
                };
            } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricción")) {
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0", "Plaine (Faible σ)"),
                    I18n.getOrDefault("setup.legend.friction.1", "Colline / Fleuve (Moyen)"),
                    I18n.getOrDefault("setup.legend.friction.2", "Montagne (Fort)"),
                    I18n.getOrDefault("setup.legend.friction.3", "Désert / Extrême"),
                    I18n.getOrDefault("setup.legend.friction.4", "Barrière Absolue")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0.desc", "Plaine Alluviale : Friction minimale à la mobilité (σ ≈ 0.1)"),
                    I18n.getOrDefault("setup.legend.friction.1.desc", "Colline / Fleuve : Obstacle naturel mineur franchissable"),
                    I18n.getOrDefault("setup.legend.friction.2.desc", "Chaîne Montagneuse : Transports ralentis, cols escarpés"),
                    I18n.getOrDefault("setup.legend.friction.3.desc", "Désert Extrême : Zone aride exigeant des convois spécialisés"),
                    I18n.getOrDefault("setup.legend.friction.4.desc", "Haute Altitude / Falaise : Barrière infranchissable pour les armées")
                };
            } else {
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
                fullTooltips = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
            }
        }

        for (int i = 0; i < labels.length; i++) {
            Rectangle colorBox = new Rectangle(14, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);
            colorBox.setArcWidth(3);
            colorBox.setArcHeight(3);

            Label lbl = new Label(labels[i]);
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

            HBox item = new HBox(4, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            Tooltip.install(item, new Tooltip(fullTooltips[i]));
            container.getChildren().add(item);
        }
    }
}
