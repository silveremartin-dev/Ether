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
    private ToggleGroup themeToggleGroup;
    private VBox langSection;
    private VBox themeSection;

    // GPU section
    private RadioButton gpuAutoRadio;
    private RadioButton gpuOffRadio;
    private ToggleGroup gpuToggleGroup;
    private Label gpuHintLabel;

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
        themeHeaderLabel = new Label();
        themeHeaderLabel.getStyleClass().add("label-section-header");
        themeSection = createCardSectionWithHeader(themeHeaderLabel, new VBox(10, themeOptions));

        // 3. GPU Section
        boolean gpuEnabled = prefs.getBoolean(PREF_GPU_KEY, true);
        gpuToggleGroup = new ToggleGroup();
        gpuAutoRadio = new RadioButton();
        gpuOffRadio  = new RadioButton();
        gpuAutoRadio.setToggleGroup(gpuToggleGroup);
        gpuOffRadio.setToggleGroup(gpuToggleGroup);
        gpuAutoRadio.setSelected(gpuEnabled);
        gpuOffRadio.setSelected(!gpuEnabled);

        gpuHintLabel = new Label();
        gpuHintLabel.setWrapText(true);
        gpuHintLabel.getStyleClass().add("control-label");
        gpuHintLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");

        gpuAutoRadio.setOnAction(e -> saveGpuPreference(true));
        gpuOffRadio.setOnAction(e -> saveGpuPreference(false));

        HBox gpuOptions = new HBox(20, gpuAutoRadio, gpuOffRadio);
        VBox gpuContent = new VBox(10, gpuOptions, gpuHintLabel);
        VBox gpuSection = createCardSection("⚡ " + I18n.getOrDefault("pref.gpu", "Accélération Matérielle (GPU)"), gpuContent);

        root.getChildren().addAll(titleHeader, langSection, themeSection, gpuSection);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setCenter(scroll);
    }

    private void saveGpuPreference(boolean enabled) {
        prefs.putBoolean(PREF_GPU_KEY, enabled);
        logger.info("GPU preference saved: {}", enabled ? "AUTO (hardware)" : "OFF (software)");
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

    private VBox createCardSection(String title, VBox content) {
        Label header = new Label(title);
        header.getStyleClass().add("label-section-header");
        VBox card = new VBox(12, header, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    public void updateTexts() {
        titleHeader.setText(I18n.get("pref.title"));
        if (langHeaderLabel != null) langHeaderLabel.setText("🌐 " + I18n.get("pref.language"));
        if (themeHeaderLabel != null) themeHeaderLabel.setText("🎨 " + I18n.get("pref.theme"));
        darkThemeRadio.setText(I18n.get("pref.theme.dark"));
        lightThemeRadio.setText(I18n.get("pref.theme.light"));
        languageCombo.setValue(I18n.getCurrentLanguage());
        if (gpuAutoRadio != null)
            gpuAutoRadio.setText(I18n.getOrDefault("pref.gpu.auto", "🖥️ GPU On (Automatique — JavaFX Prism Hardware)"));
        if (gpuOffRadio != null)
            gpuOffRadio.setText(I18n.getOrDefault("pref.gpu.off", "🔧 GPU Off (Rendu Logiciel — Software Prism)"));
        if (gpuHintLabel != null)
            gpuHintLabel.setText(I18n.getOrDefault("pref.gpu.hint",
                    "ℹ️ Ce paramètre est sauvegardé automatiquement. La modification prendra effet au prochain démarrage de l'application (GPU Activé : accélération matérielle DirectX/OpenGL | GPU Désactivé : rendu logiciel avec -Dprism.order=sw pour éviter les clignotements ou artefacts graphiques)."));
    }
}
