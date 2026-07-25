/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preferences & Settings UI Panel for Language and Theme configuration.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public class PreferencesPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(PreferencesPanel.class);

    private Label titleHeader;
    private Label langLabel;
    private ComboBox<Language> languageCombo;
    private Label themeLabel;
    private RadioButton darkThemeRadio;
    private RadioButton lightThemeRadio;
    private ToggleGroup themeToggleGroup;
    private VBox langSection;
    private VBox themeSection;

    public PreferencesPanel() {
        getStyleClass().add("glass-panel");
        setPadding(new Insets(30));

        initUI();
        updateTexts();

        // Listen for global language changes
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private void initUI() {
        VBox root = new VBox(25);
        root.setMaxWidth(600);
        root.setAlignment(Pos.TOP_LEFT);

        titleHeader = new Label();
        titleHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        // 1. Language Section
        langLabel = new Label();
        langLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(Language.values());
        languageCombo.setValue(I18n.getCurrentLanguage());
        languageCombo.setMaxWidth(Double.MAX_VALUE);
        languageCombo.setStyle("-fx-font-size: 13px;");
        languageCombo.setOnAction(e -> {
            Language selected = languageCombo.getValue();
            if (selected != null && selected != I18n.getCurrentLanguage()) {
                I18n.setLanguage(selected);
            }
        });

        langSection = createCardSection("🌐 " + I18n.get("pref.language"), new VBox(10, langLabel, languageCombo));

        // 2. Theme Section
        themeLabel = new Label();
        themeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        themeToggleGroup = new ToggleGroup();
        darkThemeRadio = new RadioButton();
        darkThemeRadio.setToggleGroup(themeToggleGroup);
        darkThemeRadio.setSelected(Theme.getCurrentTheme() == Theme.DARK);
        darkThemeRadio.setOnAction(e -> {
            if (getScene() != null) {
                Theme.setTheme(getScene(), Theme.DARK);
            }
        });

        lightThemeRadio = new RadioButton();
        lightThemeRadio.setToggleGroup(themeToggleGroup);
        lightThemeRadio.setSelected(Theme.getCurrentTheme() == Theme.LIGHT);
        lightThemeRadio.setOnAction(e -> {
            if (getScene() != null) {
                Theme.setTheme(getScene(), Theme.LIGHT);
            }
        });

        HBox themeOptions = new HBox(20, darkThemeRadio, lightThemeRadio);
        themeSection = createCardSection("🎨 " + I18n.get("pref.theme"), new VBox(10, themeLabel, themeOptions));

        root.getChildren().addAll(titleHeader, langSection, themeSection);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setCenter(scroll);
    }

    private VBox createCardSection(String title, VBox content) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        VBox card = new VBox(12, header, content);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10; -fx-padding: 15;");
        return card;
    }

    public void updateTexts() {
        titleHeader.setText(I18n.get("pref.title"));
        langLabel.setText(I18n.get("pref.language"));
        themeLabel.setText(I18n.get("pref.theme"));
        darkThemeRadio.setText(I18n.get("pref.theme.dark"));
        lightThemeRadio.setText(I18n.get("pref.theme.light"));
        languageCombo.setValue(I18n.getCurrentLanguage());
    }
}
