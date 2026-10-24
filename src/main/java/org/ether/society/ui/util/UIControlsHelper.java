/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import org.ether.society.i18n.I18n;

/**
 * Reusable utility methods for JavaFX controls across the simulation UI.
 *
 * <p>Provides standardized right-click default reset handling, default value
 * tooltip annotations, and high-contrast modified control visual highlights.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class UIControlsHelper {

    private UIControlsHelper() {
        // Utility class
    }

    /**
     * Attaches default value tooltip annotation and a right-click context menu to reset the control to its default value.
     *
     * @param control JavaFX control
     * @param defaultValue the canonical default value
     * @param resetAction callback to reset the control state
     * @param <T> type of default value
     */
    public static <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
        attachDefaultValueHandling(control, defaultValue, resetAction, null);
    }

    /**
     * Attaches default value tooltip annotation and a right-click context menu to reset the control to its default value.
     *
     * @param control JavaFX control
     * @param defaultValue the canonical default value
     * @param resetAction callback to reset the control state
     * @param onModifiedCallback callback to notify state change after reset
     * @param <T> type of default value
     */
    public static <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction, Runnable onModifiedCallback) {
        if (control == null) return;

        String defaultStr;
        if (defaultValue instanceof Double d) {
            defaultStr = String.format(java.util.Locale.US, "%.2f", d);
        } else {
            defaultStr = String.valueOf(defaultValue);
        }

        // 1. Tooltip enhancement
        Tooltip currentTooltip = control.getTooltip();
        String baseTooltip = currentTooltip != null ? currentTooltip.getText() : "";
        if (!baseTooltip.contains("[DEFAULT VALUE")) {
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n") + I18n.getOrDefault("scenario.tooltip.default_notice_prefix", "📌 [DEFAULT VALUE: ") + defaultStr + I18n.getOrDefault("scenario.tooltip.default_notice_suffix", "] — Right-click control to reset.");
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
                if (onModifiedCallback != null) {
                    onModifiedCallback.run();
                }
            }
        });
        contextMenu.getItems().add(resetItem);
        control.setContextMenu(contextMenu);
    }

    /**
     * Highlights or clears modified control border styling.
     *
     * @param control target control
     * @param isModified true if value differs from default
     */
    public static void highlightControlIfModified(Control control, boolean isModified) {
        if (control == null) return;
        if (isModified) {
            control.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
        } else {
            control.setStyle("");
        }
    }
}
