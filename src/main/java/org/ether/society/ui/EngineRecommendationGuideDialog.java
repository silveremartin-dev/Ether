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
        setTitle(I18n.getOrDefault("engine.guide.window_title", "ðŸ’¡ Guide des Recommandations Moteur (Cohortes, H3 & Pas de Temps)"));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel");

        // Top Header
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(16, 20, 14, 20));
        headerBox.setStyle("-fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label(I18n.getOrDefault("engine.guide.header_title", "ðŸ’¡ GUIDE DE CONFIGURATION OPTIMALE DU MOTEUR ETHER"));
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label subtitleLabel = new Label(I18n.getOrDefault("engine.guide.header_subtitle", "Recommandations scientifiques et techniques sur les compromis de performance CPU/GPU et de fidÃ©litÃ© historique."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setWrapText(true);

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        root.setTop(headerBox);

        // Content: Themed Cards inside a scrollable pane
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 20, 16, 20));

        // Card 1: Cohort Sizing (Min, Recommended, Max)
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card1.title", "ðŸ‘¥ 1. TAILLE DES COHORTES DÃ‰MOGRAPHIQUES (DUNBAR & AGENTS DOD)"),
                I18n.getOrDefault("engine.guide.card1.body", "La taille des cohortes dÃ©finit le nombre moyen d'individus regroupÃ©s par nÅ“ud agent au sein du buffer vectorisÃ© DOD :\n"
                        + "â€¢ Minimum (1 Ã  30 hab/cohorte) : Bandes palÃ©olithiques nomades, petits clans de chasseurs-cueilleurs et micro-communautÃ©s d'avant-postes spatiaux (Lune, Mars). PrÃ©cision maximale mais volume d'agents trÃ¨s Ã©levÃ©.\n"
                        + "â€¢ RecommandÃ© Dunbar (150 hab/cohorte) : Nombre de Dunbar anthropologique. ReprÃ©sente la taille naturelle des villages nÃ©olithiques, des tribus agraires et des quartiers urbains de base. RecommandÃ© pour l'histoire ancienne.\n"
                        + "â€¢ IntermÃ©diaire (1 000 Ã  5 000 hab/cohorte) : Grandes mÃ©tropoles antiques, empires mÃ©diÃ©vaux et pÃ©riodes prÃ©-industrielles.\n"
                        + "â€¢ Macro-cohortes (10 000 Ã  100 000+ hab/cohorte) : Simulations industrielles contemporaines et transitions futures pour maximiser le dÃ©bit de calcul (TPS) sur des populations de 1 Ã  10 milliards d'individus."),
                "-fx-text-fill: #38bdf8;"
        ));

        // Card 2: H3 Spatial Resolution
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card2.title", "ðŸŒ 2. RÃ‰SOLUTION SPATIALE H3 (HEXAGONES PLANÃ‰TAIRES)"),
                I18n.getOrDefault("engine.guide.card2.body", "La discrÃ©tisation spatiale H3 dÃ©termine le maillage hexagonal global de la planÃ¨te :\n"
                        + "â€¢ RÃ©solution 2 (~1 100 km par hexagone, ~4 100 cellules) : Simulations macro-historiques ultra-rapides sur des dizaines de millÃ©naires.\n"
                        + "â€¢ RÃ©solution 3 (~420 km par hexagone, ~28 000 cellules - RecommandÃ© PlanÃ©taire) : Ã‰quilibre optimal pour les simulations globales de la Terre et des exoplanÃ¨tes avec tous les tenseurs activÃ©s.\n"
                        + "â€¢ RÃ©solution 4-5 (~160 km Ã  ~60 km) : IdÃ©al pour les scÃ©narios rÃ©gionaux et continentaux (bassin mÃ©diterranÃ©en, MÃ©sopotamie, Chine des Royaumes Combattants).\n"
                        + "â€¢ RÃ©solution 6+ (â‰¤ 20 km) : ModÃ©lisation micro-topographique haute dÃ©finition. NÃ©cessite une machine puissante et l'accÃ©lÃ©ration multi-cÅ“ur/GPU."),
                "-fx-text-fill: #10b981;"
        ));

        // Card 3: Temporal Step Resolution
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card3.title", "â±ï¸ 3. RÃ‰SOLUTION TEMPORELLE (PAS DE TEMPS Î”t)"),
                I18n.getOrDefault("engine.guide.card3.body", "Le pas de temps Î”t contrÃ´le la frÃ©quence d'actualisation des Ã©quations physiques et sociologiques :\n"
                        + "â€¢ 1 Jour / 1 Semaine (Pas Court) : RecommandÃ© pour l'Ã©tude des vagues Ã©pidÃ©miques aiguÃ«s (peste noire, variole), des dynamiques militaires tactiques et des forÃ§ages climatiques saisonniers paroxystiques.\n"
                        + "â€¢ 1 Mois (~30 jours - RecommandÃ© Standard) : Ã‰talon universel d'Ether. Capture parfaitement le cycle agraire (semailles, rÃ©coltes, crues du Nil), les saisons climatiques et la dÃ©mographie mensuelle avec un excellent framerate.\n"
                        + "â€¢ 1 An (365 jours - Pas Long) : RecommandÃ© pour les longues campagnes multi-millÃ©naires (ex : Sortie d'Afrique -100 000 BP) pour parcourir des millÃ©naires en quelques minutes."),
                "-fx-text-fill: #f59e0b;"
        ));

        // Card 4: Summary Table / Rule of Thumb
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("engine.guide.card4.title", "ðŸŽ¯ 4. PROFILES RECOMMANDÃ‰S SELON VOTRE USAGE"),
                I18n.getOrDefault("engine.guide.card4.body", "â€¢ Histoire Profonde (-100k Ã  0) : H3 Res 2-3 | Pas : 1 mois ou 1 an | Cohorte : 30 Ã  150\n"
                        + "â€¢ Civilisations Antiques / MÃ©diÃ©vales : H3 Res 3-4 | Pas : 1 mois | Cohorte : 150 Ã  500\n"
                        + "â€¢ Ã‰poque Contemporaine / Transition Ã‰nergÃ©tique : H3 Res 3 | Pas : 1 mois | Cohorte : 1 000 Ã  5 000\n"
                        + "â€¢ Ã‰pidÃ©miologie / Crise AiguÃ« : H3 Res 4-5 | Pas : 1 jour ou 7 jours | Cohorte : 50 Ã  150"),
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
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("engine.guide.btn_close.tooltip", "Fermer cette boÃ®te d'aide.")));
        btnClose.setOnAction(e -> close());

        bottomBar.getChildren().add(btnClose);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowHelper.applyWindowIcon(this);
    }

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

