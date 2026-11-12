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
 * Themed and fully localized dialog explaining the physics, mechanics,
 * rolling checkpoint architecture, and multiverse branching of simulation Snapshots.
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
public class SnapshotExplanationDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotExplanationDialog.class);

    /**
     * Snapshot explanation dialog.
     *
     * @param owner the owner parameter (Window)
     */
    public SnapshotExplanationDialog(Window owner) {
        initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            initOwner(owner);
        }
        setResizable(true);
        setMinWidth(640);
        setMinHeight(520);
        setWidth(740);
        setHeight(620);
        setTitle(I18n.getOrDefault("snapshot.modal.window_title", "ðŸ“¸ Fonctionnement des Snapshots & Points de Restauration"));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel");

        // Top Header
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(16, 20, 14, 20));
        headerBox.setStyle("-fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label(I18n.getOrDefault("snapshot.modal.header_title", "ðŸ“¸ QU'EST-CE QU'UN SNAPSHOT & COMMENT Ã‡A FONCTIONNE ?"));
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label subtitleLabel = new Label(I18n.getOrDefault("snapshot.modal.header_subtitle", "Guide technique sur la persistance dÃ©terministe, les rolling checkpoints et les embranchements de trajectoires (Branching)."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setWrapText(true);

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        root.setTop(headerBox);

        // Content: 4 Themed Cards inside a scrollable pane
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 20, 16, 20));

        // Card 1: Definition & Physical Nature
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("snapshot.modal.card1.title", "ðŸ’¡ 1. DÃ‰FINITION & NATURE D'UN SNAPSHOT"),
                I18n.getOrDefault("snapshot.modal.card1.body", "Un Snapshot (ou instantanÃ© d'Ã©tat) est une capture intÃ©grale, fidÃ¨le et dÃ©terministe de la planÃ¨te et des sociÃ©tÃ©s Ã  un tick ou une annÃ©e T prÃ©cise.\n"
                        + "Il fige l'intÃ©gralitÃ© des lois de conservation sans aucune approximation ni perte de prÃ©cision."),
                "-fx-text-fill: #38bdf8;"
        ));

        // Card 2: Captured Variables & Conservation
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("snapshot.modal.card2.title", "ðŸª 2. VARIABLES & REGISTRES CAPTURÃ‰S"),
                I18n.getOrDefault("snapshot.modal.card2.body", "â€¢ Ã‰tats Cellulaires H3 : Biomasse humaine, stocks alimentaires, eau disponible, nutriments du sol (NPK), capital physique (Kâ‚€), Ã©nergie (Eâ‚€), savoirs (Iâ‚€) et niveau technologique.\n"
                        + "â€¢ Registres DOD & Tenseurs : Buffers vectorisÃ©s des cohortes d'agents, tranches d'Ã¢ges, pyramides dÃ©mographiques et flux migratoires.\n"
                        + "â€¢ Climat & Temps : AnnÃ©e calendaire, mois, saison, forÃ§age radiatif et bilan carbone.\n"
                        + "â€¢ EntitÃ©s Sociopolitiques : FrontiÃ¨res territoriales, matrice d'affinitÃ© culturelle et journal des Ã©vÃ©nements."),
                "-fx-text-fill: #10b981;"
        ));

        // Card 3: Rolling Checkpoints & Async Persistence
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("snapshot.modal.card3.title", "âš¡ 3. CHECKPOINTS EN TÃ‚CHE DE FOND & RESTAURATION INSTANTANÃ‰E"),
                I18n.getOrDefault("snapshot.modal.card3.body", "â€¢ Rolling Checkpoint Asynchrone : Le moteur persiste automatiquement un checkpoint lÃ©ger sur disque tous les 60 ticks sans jamais figer l'interface utilisateur.\n"
                        + "â€¢ Restauration InstantanÃ©e (Warm-Start) : Charger un snapshot rÃ©injecte directement les structures de donnÃ©es en mÃ©moire vive, Ã©vitant d'avoir Ã  recalculer des millÃ©naires ou des siÃ¨cles Ã©coulÃ©s."),
                "-fx-text-fill: #f59e0b;"
        ));

        // Card 4: Multiverse Branching & Forking
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("snapshot.modal.card4.title", "ðŸ”€ 4. MULTIVERS & EMBRANCHEMENTS DE TRAJECTOIRES (BRANCHING)"),
                I18n.getOrDefault("snapshot.modal.card4.body", "Vous pouvez charger un snapshot Ã  une annÃ©e charniÃ¨re (ex : 2040), modifier les paramÃ¨tres d'infrastructure, les lois physiques ou injecter une crise (ex : rupture technologique, volcanisme), et observer la divergence de la civilisation par rapport Ã  la session initiale."),
                "-fx-text-fill: #a855f7;"
        ));

        // Card 5: Autonomous Pruning & Logarithmic Time-Decay
        contentBox.getChildren().add(createCard(
                I18n.getOrDefault("snapshot.modal.card5.title", "ðŸ§¹ 5. GESTION AUTOMATIQUE & Ã‰LAGAGE TEMPOREL LOGARITHMIQUE"),
                I18n.getOrDefault("snapshot.modal.card5.body", "â€¢ Aucune Gestion Manuelle Requise : Le systÃ¨me de persistance gÃ¨re les snapshots de maniÃ¨re 100% autonome pour Ã©viter Ã  l'utilisateur d'avoir Ã  supprimer des fichiers manuellement.\n"
                        + "â€¢ RÃ©tention Logarithmique (Time-Decay) : Les snapshots rÃ©cents sont conservÃ©s Ã  haute frÃ©quence temporelle, tandis que les Ã©poques plus anciennes sont automatiquement espacÃ©es selon une Ã©chelle logarithmique. Cela garantit un historique complet sur des millÃ©naires tout en maintenant une empreinte disque et mÃ©moire ultra-lÃ©gÃ¨re."),
                "-fx-text-fill: #ec4899;"
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

        Button btnClose = new Button(I18n.getOrDefault("snapshot.modal.btn_close", "Fermer"));
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-font-weight: bold; -fx-padding: 8 20;");
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("snapshot.modal.btn_close.tooltip", "Fermer cette boÃ®te d'explications.")));
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

