/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import org.ether.society.i18n.I18n;

import java.util.Locale;

/**
 * Utility managing default value annotations, tooltips, right-click reset context menus,
 * and change-highlighting styling across scenario setup UI controls.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioDefaultValueManager {

    private ScenarioDefaultValueManager() {
        // Utility class
    }

    /**
     * Attaches default value handling to a JavaFX control:
     * <ol>
     *     <li>Appends {@code "[DEFAULT VALUE: X]"} to the tooltip.</li>
     *     <li>Adds a right-click Context Menu ("Reset to default value (X)").</li>
     * </ol>
     *
     * @param <T> value type
     * @param control target UI control
     * @param defaultValue default reference value
     * @param resetAction callback to reset control to default
     * @param onParamChange callback invoked when value changes
     */
    public static <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction, Runnable onParamChange) {
        if (control == null) return;

        String defaultStr;
        if (defaultValue instanceof Double d) {
            defaultStr = String.format(Locale.US, "%.2f", d);
        } else {
            defaultStr = String.valueOf(defaultValue);
        }

        // 1. Tooltip enhancement
        Tooltip currentTooltip = control.getTooltip();
        String baseTooltip = currentTooltip != null ? currentTooltip.getText() : "";
        if (!baseTooltip.contains("[DEFAULT VALUE")) {
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n")
                    + I18n.getOrDefault("scenario.tooltip.default_notice_prefix", "📌 [DEFAULT VALUE: ")
                    + defaultStr
                    + I18n.getOrDefault("scenario.tooltip.default_notice_suffix", "] — Right-click control to reset.");
            control.setTooltip(new Tooltip(baseTooltip + defaultNotice));
        }

        // 2. Right-click Context Menu
        ContextMenu contextMenu = control.getContextMenu();
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        MenuItem resetItem = new MenuItem(I18n.getOrDefault("scenario.menu.reset_default", "🔄 Reset to default value (") + defaultStr + ")");
        resetItem.setOnAction(e -> {
            if (resetAction != null) {
                resetAction.run();
                if (onParamChange != null) {
                    onParamChange.run();
                }
            }
        });
        contextMenu.getItems().add(resetItem);
        control.setContextMenu(contextMenu);
    }

    /**
     * Highlights or clears modified visual border styling on a JavaFX control.
     *
     * @param control target control
     * @param isModified whether current value diverges from default
     */
    public static void highlightControlIfModified(Control control, boolean isModified) {
        if (control == null) return;
        if (isModified) {
            control.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
        } else {
            control.setStyle("");
        }
    }

    /**
     * Resolves the canonical default parameter value for Type B optional cliodynamic engines.
     *
     * @param engineKey unique class or identifier of the engine
     * @param paramKey parameter identifier
     * @return canonical default floating-point value
     */
    public static double getTypeBParamDefault(String engineKey, String paramKey) {
        if ("MaritimeHighwayEngine".equals(engineKey)) {
            if ("capitalBoostRate".equals(paramKey)) return 0.05;
            if ("frictionMultiplier".equals(paramKey)) return 0.20;
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            if ("tenochtitlanTech".equals(paramKey)) return 3.5;
            if ("tenochtitlanCapital".equals(paramKey)) return 150.0;
            if ("aralRainfallThreshold".equals(paramKey)) return 300.0;
            if ("damMinElevation".equals(paramKey)) return 300.0;
        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            if ("salinizationRate".equals(paramKey)) return 0.02;
        } else if ("LandReclamationEngine".equals(engineKey)) {
            if ("polderTechThreshold".equals(paramKey)) return 4.0;
            if ("polderCapitalThreshold".equals(paramKey)) return 100.0;
        }
        return 0.0;
    }
}
