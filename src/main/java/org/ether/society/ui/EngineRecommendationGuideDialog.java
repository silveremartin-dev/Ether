/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Themed and fully localized dialog guiding the user on optimal engine parameter configurations,
 * including demographic cohort sizing, H3 spatial discrete resolution, and temporal step resolution.
 *
 * Conforms to AGENTS.md directives:
 * - Full Dark/Light Theme integration via {@link Theme#applyCurrentTheme(Scene)}
 * - High contrast styling and card sections
 * - Complete 5-language localization (EN, FR, DE, ES, ZH)
 * - Complete mouseover tooltips on interactive elements
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EngineRecommendationGuideDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(EngineRecommendationGuideDialog.class);

    /**
     * Constructs a new {@code EngineRecommendationGuideDialog}.
     *
     * @param owner the parent owner window
     */
    public EngineRecommendationGuideDialog(Window owner) {
        initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            initOwner(owner);
        }
        setResizable(true);
        setMinWidth(680);
        setMinHeight(560);
        setWidth(780);
        setHeight(660);
        setTitle(I18n.getOrDefault("engine.guide.window_title", "💡 Guide des Recommandations Moteur (Cohortes, H3 & Pas de Temps)"));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel");

        // Top Header
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(16, 20, 14, 20));
        headerBox.setStyle("-fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label(I18n.getOrDefault("engine.guide.header_title", "💡 GUIDE DE CONFIGURATION OPTIMALE DU MOTEUR ETHER"));
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label subtitleLabel = new Label(I18n.getOrDefault("engine.guide.header_subtitle", "Recommandations scientifiques et techniques sur les compromis de performance CPU/GPU et de fidélité historique."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setWrapText(true);

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        root.setTop(headerBox);

        // Content: Themed Cards inside a scrollable pane
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 20, 16, 20));

        // Card 1: Cohort Sizing (Min, Recommended, Max)
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card1.title", "👥 1. TAILLE DES COHORTES DÉMOGRAPHIQUES (DUNBAR & AGENTS DOD)"),
                I18n.getOrDefault("engine.guide.card1.body", "La taille des cohortes définit le nombre moyen d'individus regroupés par nœud agent au sein du buffer vectorisé DOD :\n"
                        + "• Minimum (1 à 30 hab/cohorte) : Bandes paléolithiques nomades, petits clans de chasseurs-cueilleurs et micro-communautés d'avant-postes spatiaux. Précision maximale mais volume d'agents très élevé.\n"
                        + "• Recommandé par Défaut (150 hab/cohorte) : Nombre de Dunbar anthropologique. Représente la taille naturelle des villages néolithiques, des tribus agraires et des quartiers urbains de base. Offre l'équilibre idéal fidélité / fluidité.\n"
                        + "• Maximum / Macro (1 000 à 50 000 hab/cohorte) : Grandes métropoles antiques, empires médiévaux et macro-simulations industrielles contemporaines pour maximiser le débit de calcul sur de larges populations."),
                "-fx-text-fill: #38bdf8;"
        ));

        // Card 2: H3 Spatial Resolution
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card2.title", "🌐 2. RÉSOLUTION SPATIALE H3 (HEXAGONES PLANÉTAIRES)"),
                I18n.getOrDefault("engine.guide.card2.body", "La discrétisation spatiale H3 détermine le maillage hexagonal global de la planète :\n"
                        + "• Résolution 2 (~1 100 km par hexagone, ~4 100 cellules) : Simulations macro-historiques ultra-rapides sur des dizaines de millénaires.\n"
                        + "• Résolution 3 (~420 km par hexagone, ~28 000 cellules - Recommandé Planétaire) : Équilibre optimal pour les simulations globales de la Terre et des exoplanètes avec tous les tenseurs activés.\n"
                        + "• Résolution 4-5 (~160 km à ~60 km) : Idéal pour les scénarios régionaux et continentaux (bassin méditerranéen, Mésopotamie, Chine des Royaumes Combattants).\n"
                        + "• Résolution 6+ (≤ 20 km) : Modélisation micro-topographique haute définition. Nécessite une machine puissante et l'accélération multi-cœur/GPU."),
                "-fx-text-fill: #10b981;"
        ));

        // Card 3: Temporal Step Resolution
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card3.title", "⏱️ 3. RÉSOLUTION TEMPORELLE (PAS DE TEMPS Δt)"),
                I18n.getOrDefault("engine.guide.card3.body", "Le pas de temps Δt contrôle la fréquence d'actualisation des équations physiques et sociologiques :\n"
                        + "• 1 Jour / 1 Semaine (Pas Court) : Recommandé pour l'étude des vagues épidémiques aiguës (peste noire, variole), des dynamiques militaires tactiques et des forçages climatiques saisonniers paroxystiques.\n"
                        + "• 1 Mois (~30 jours - Recommandé Standard) : Étalon universel d'Ether. Capture parfaitement le cycle agraire (semailles, récoltes, crues du Nil), les saisons climatiques et la démographie mensuelle avec un excellent framerate.\n"
                        + "• 1 An (365 jours - Pas Long) : Recommandé pour les longues campagnes multi-millénaires (ex : Sortie d'Afrique -100 000 BP) pour parcourir des millénaires en quelques minutes."),
                "-fx-text-fill: #f59e0b;"
        ));

        // Card 4: Summary Table / Rule of Thumb
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card4.title", "🎯 4. PROFILES RECOMMANDÉS SELON VOTRE USAGE"),
                I18n.getOrDefault("engine.guide.card4.body", "• Histoire Profonde (-100k à 0) : H3 Res 2-3 | Pas : 1 mois ou 1 an | Cohorte : 30 à 150\n"
                        + "• Civilisations Antiques / Médiévales : H3 Res 3-4 | Pas : 1 mois | Cohorte : 150 à 500\n"
                        + "• Époque Contemporaine / Transition Énergétique : H3 Res 3 | Pas : 1 mois | Cohorte : 1 000 à 5 000\n"
                        + "• Épidémiologie / Crise Aiguë : H3 Res 4-5 | Pas : 1 jour ou 7 jours | Cohorte : 50 à 150"),
                "-fx-text-fill: #a855f7;"
        ));

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scrollPane);

        // Bottom Action Bar
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(12, 20, 14, 20));
        bottomBar.setStyle("-fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 1 0 0 0;");

        Button btnClose = new Button(I18n.getOrDefault("engine.guide.btn_close", "Fermer"));
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-font-weight: bold; -fx-padding: 8 20;");
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("engine.guide.btn_close.tooltip", "Fermer cette boîte d'aide.")));
        btnClose.setOnAction(e -> close());

        bottomBar.getChildren().add(btnClose);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowUtils.applyWindowIcon(this);
    }

    // Helper subroutine: create card - internal state computation & bounds checking
    private VBox createCard(String title, String body, String titleStyle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card-section");

        Label cardTitle = new Label(title);
        cardTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; " + titleStyle);

        Label cardBody = new Label(body);
        cardBody.setWrapText(true);
        cardBody.setStyle("-fx-font-size: 11px; -fx-line-spacing: 2px; -fx-text-fill: #e2e8f0;");

        card.getChildren().addAll(cardTitle, cardBody);
        return card;
    }
}
