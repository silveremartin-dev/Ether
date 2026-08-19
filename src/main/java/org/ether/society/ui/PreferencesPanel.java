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
 * Preferences &amp; Settings UI Panel for Language, Theme and GPU configuration.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.1.0
 */
public class PreferencesPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(PreferencesPanel.class);

    private static final java.util.prefs.Preferences prefs =
            java.util.prefs.Preferences.userNodeForPackage(PreferencesPanel.class);
    private static final String PREF_GPU_KEY = "ether_gpu_enabled";

    private Label titleHeader;
    private Label langHeaderLabel;
    private ComboBox<Language> languageCombo;
    private Label themeHeaderLabel;
    private RadioButton darkThemeRadio;
    private RadioButton lightThemeRadio;
    private RadioButton presentationThemeRadio;
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
        titleHeader.getStyleClass().add("label-title");

        // 1. Language Section
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll(Language.values());
        languageCombo.setValue(I18n.getCurrentLanguage());
        languageCombo.setMaxWidth(Double.MAX_VALUE);
        languageCombo.setOnAction(e -> {
            Language selected = languageCombo.getValue();
            if (selected != null && selected != I18n.getCurrentLanguage()) {
                I18n.setLanguage(selected);
            }
        });

        langHeaderLabel = new Label();
        langHeaderLabel.getStyleClass().add("label-section-header");
        langSection = createCardSectionWithHeader(langHeaderLabel, new VBox(10, languageCombo));

        // 2. Theme Section
        themeToggleGroup = new ToggleGroup();
        
        darkThemeRadio = new RadioButton();
        darkThemeRadio.setToggleGroup(themeToggleGroup);
        darkThemeRadio.setSelected(Theme.getCurrentTheme() == Theme.DARK);
        darkThemeRadio.setOnAction(e -> Theme.setTheme(getScene(), Theme.DARK));

        lightThemeRadio = new RadioButton();
        lightThemeRadio.setToggleGroup(themeToggleGroup);
        lightThemeRadio.setSelected(Theme.getCurrentTheme() == Theme.LIGHT);
        lightThemeRadio.setOnAction(e -> Theme.setTheme(getScene(), Theme.LIGHT));

        presentationThemeRadio = new RadioButton();
        presentationThemeRadio.setToggleGroup(themeToggleGroup);
        presentationThemeRadio.setSelected(Theme.getCurrentTheme() == Theme.PRESENTATION);
        presentationThemeRadio.setOnAction(e -> Theme.setTheme(getScene(), Theme.PRESENTATION));

        HBox themeOptions = new HBox(20, darkThemeRadio, lightThemeRadio, presentationThemeRadio);
        themeHeaderLabel = new Label();
        themeHeaderLabel.getStyleClass().add("label-section-header");
        themeSection = createCardSectionWithHeader(themeHeaderLabel, new VBox(10, themeOptions));

        root.getChildren().addAll(titleHeader, langSection, themeSection);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setCenter(scroll);
    }

    /**
     * Returns the saved GPU preference (true = hardware auto, false = software only).
     * Intended to be read at startup by the main App class to configure Prism pipeline.
     */
    public static boolean isGpuEnabled() {
        return prefs.getBoolean(PREF_GPU_KEY, true);
    }

    private VBox createCardSectionWithHeader(Label header, VBox content) {
        VBox card = new VBox(12, header, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    public void updateTexts() {
        if (titleHeader != null) titleHeader.setText(I18n.get("pref.title"));
        if (langHeaderLabel != null) langHeaderLabel.setText("🌐 " + I18n.get("pref.language"));
        if (themeHeaderLabel != null) themeHeaderLabel.setText("🎨 " + I18n.get("pref.theme"));
        if (darkThemeRadio != null) darkThemeRadio.setText(I18n.get("pref.theme.dark"));
        if (lightThemeRadio != null) lightThemeRadio.setText(I18n.get("pref.theme.light"));
        if (presentationThemeRadio != null) presentationThemeRadio.setText(I18n.get("pref.theme.presentation"));
        if (languageCombo != null) languageCombo.setValue(I18n.getCurrentLanguage());
    }
}
