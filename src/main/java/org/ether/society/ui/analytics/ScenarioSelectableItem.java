/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.ether.society.i18n.I18n;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.Scenario;
import org.ether.society.ui.ExecutionContextPanel;

import java.util.Locale;

/**
 * Data model wrapper representing a scenario selectable within the Comparative Analytics TableView
 * and batch execution pipeline.
 *
 * <p>Tracks interactive selection state, execution status, headless batch queue state,
 * wall-clock execution duration estimates \(\tau_{\mathrm{est}}\), and remaining duration \(\tau_{\mathrm{rem}}\).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSelectableItem {

    private final Scenario scenario;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private boolean executed;
    private String runId;
    private BatchState batchState = BatchState.NOT_EXECUTED;
    private double progress = 0.0;
    private int queueIndex = 0;
    private double estimatedDurationSec = 0.0;
    private double remainingDurationSec = 0.0;

    /**
     * Constructs a selectable item for a given scenario.
     *
     * @param scenario   the underlying scenario definition
     * @param isSelected initial checkbox selection flag
     * @param executed   whether a historical or simulated run already exists
     * @param runId      the associated simulation run identifier or {@code "HISTORICAL_GROUND_TRUTH"}
     */
    public ScenarioSelectableItem(Scenario scenario, boolean isSelected, boolean executed, String runId) {
        this.scenario = scenario;
        this.selected.set(isSelected);
        this.executed = executed;
        this.runId = runId;
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
            this.progress = 1.0;
            this.executed = true;
            this.batchState = BatchState.EXECUTED;
            this.estimatedDurationSec = 0.0;
            this.remainingDurationSec = 0.0;
        } else if (executed) {
            this.progress = 1.0;
            this.batchState = BatchState.EXECUTED;
            this.estimatedDurationSec = 0.0;
            this.remainingDurationSec = 0.0;
        } else {
            this.batchState = BatchState.NOT_EXECUTED;
            recalculateEstimate();
        }
    }

    /**
     * Retrieves the wrapped scenario.
     *
     * @return the scenario instance, or null if ground truth wrapper
     */
    public Scenario getScenario() {
        return scenario;
    }

    /**
     * Returns the user-visible scenario name.
     *
     * @return localized display name
     */
    public String getName() {
        if (scenario == null) {
            return I18n.getOrDefault("analytics.unnamed_scenario", "Unnamed Scenario");
        }
        String n = scenario.getDisplayName();
        if (n == null || n.isBlank()) {
            return I18n.getOrDefault("analytics.unnamed_scenario", "Unnamed Scenario")
                    + (scenario.getId() != null ? " (#" + scenario.getId() + ")" : "");
        }
        return n;
    }

    /**
     * Returns the formatted year range string.
     *
     * @return formatted range (e.g., "Year -10000 ➔ 2000")
     */
    public String getYearRange() {
        int start = scenario != null ? (int) scenario.getStartDateYear() : 0;
        int end = scenario != null ? (int) scenario.getEndDateYear() : 100;
        return String.format(I18n.getOrDefault("analytics.year_range_fmt", "Year %d ➔ %d"), start, end);
    }

    /**
     * Returns the celestial body preset display name with icon prefix.
     *
     * @return planet display string
     */
    public String getPlanetDisplayName() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
            return "🌍 " + I18n.getOrDefault("planet.preset.earth", "Terre");
        }
        if (scenario == null) {
            return "🌍 " + I18n.getOrDefault("planet.preset.earth", "Terre");
        }
        PlanetPreset p = scenario.getPlanetPreset();
        String pName = p != null && p.name() != null ? p.name() : "";
        String pLow = pName.toLowerCase(Locale.ROOT);
        String scName = scenario.getName() != null ? scenario.getName().toLowerCase(Locale.ROOT) : "";
        String key = scenario.resolvePresetKey() != null ? scenario.resolvePresetKey().toLowerCase(Locale.ROOT) : "";

        if (pLow.contains("lune") || pLow.contains("moon") || scName.contains("shackleton")
                || key.contains("moon") || scName.contains("lune")) {
            return "🌕 " + I18n.getOrDefault("planet.preset.moon", "Lune");
        }
        if (pLow.contains("mars") || pLow.contains("ares") || scName.contains("mars") || key.contains("mars")) {
            return "🔴 " + I18n.getOrDefault("planet.preset.mars", "Mars");
        }
        if (pLow.contains("vénus") || pLow.contains("venus") || pLow.contains("hesperos")
                || scName.contains("venus") || scName.contains("vénus") || key.contains("venus")) {
            return "🟡 " + I18n.getOrDefault("planet.preset.venus", "Vénus");
        }
        if (pLow.contains("mercure") || pLow.contains("mercury") || pLow.contains("hermes")
                || scName.contains("mercure") || scName.contains("mercury") || key.contains("mercury")) {
            return "☿ " + I18n.getOrDefault("planet.preset.mercury", "Mercure");
        }
        if (pLow.contains("titan") || scName.contains("titan") || key.contains("titan") || scName.contains("cryo_methane")) {
            return "🪐 " + I18n.getOrDefault("planet.preset.titan", "Titan");
        }
        if (pLow.contains("super-terre") || pLow.contains("super-earth") || pLow.contains("gaia") || key.contains("super_earth")) {
            return "🪐 " + I18n.getOrDefault("planet.preset.super_earth", "Super-Terre");
        }
        if (pLow.contains("oceania") || scName.contains("oceania") || key.contains("oceania") || scName.contains("aquapolis")) {
            return "🌊 " + I18n.getOrDefault("planet.preset.oceania", "Oceania");
        }
        if (pLow.contains("boreas") || scName.contains("boreas") || key.contains("boreas") || scName.contains("subglacial")) {
            return "❄️ " + I18n.getOrDefault("planet.preset.boreas", "Boreas");
        }
        if (pLow.contains("eyeball") || key.contains("eyeball") || scName.contains("twilight")) {
            return "🪐 " + I18n.getOrDefault("planet.preset.eyeball", "Eyeball World");
        }
        if (pLow.contains("archipel") || scName.contains("archipel") || scName.contains("seasteading")) {
            return "🏝️ " + I18n.getOrDefault("planet.preset.archipelago", "Archipel");
        }
        return "🌍 " + I18n.getOrDefault("planet.preset.earth", "Terre");
    }

    /**
     * Returns the spatial clipping bounds display string.
     *
     * @return bounding box text or "Global"
     */
    public String getClippingDisplay() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
            return "🌐 " + I18n.getOrDefault("analytics.clipping.global", "Global");
        }
        if (scenario == null || !scenario.isClippingEnabled()) {
            return "🌐 " + I18n.getOrDefault("analytics.clipping.global", "Global");
        }
        return String.format(Locale.US, "🪟 [%.1f°, %.1f° Lat | %.1f°, %.1f° Lng]",
                scenario.getMinLat(), scenario.getMaxLat(), scenario.getMinLng(), scenario.getMaxLng());
    }

    /**
     * Checks if spatial bounding box clipping is enabled.
     *
     * @return true if clipped
     */
    public boolean isClipped() {
        return scenario != null && scenario.isClippingEnabled();
    }

    public double getMinLat() {
        return (scenario != null && scenario.isClippingEnabled()) ? scenario.getMinLat() : -90.0;
    }

    public double getMaxLat() {
        return (scenario != null && scenario.isClippingEnabled()) ? scenario.getMaxLat() : 90.0;
    }

    public double getMinLng() {
        return (scenario != null && scenario.isClippingEnabled()) ? scenario.getMinLng() : -180.0;
    }

    public double getMaxLng() {
        return (scenario != null && scenario.isClippingEnabled()) ? scenario.getMaxLng() : 180.0;
    }

    public long getStartDateYear() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) return -100000L;
        return scenario != null ? scenario.getStartDateYear() : 0L;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean val) {
        this.selected.set(val);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isExecuted() {
        return executed || progress >= 1.0;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
        if (executed) {
            this.batchState = BatchState.EXECUTED;
            this.progress = 1.0;
            this.remainingDurationSec = 0.0;
        }
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public BatchState getBatchState() {
        return batchState;
    }

    public void setBatchState(BatchState batchState) {
        this.batchState = batchState;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        if (this.progress >= 1.0) {
            this.executed = true;
            this.remainingDurationSec = 0.0;
            if (this.batchState != BatchState.RUNNING) {
                this.batchState = BatchState.EXECUTED;
            }
        } else {
            this.executed = false;
            this.remainingDurationSec = Math.max(0.0, this.estimatedDurationSec * (1.0 - this.progress));
        }
    }

    public String getProgressPercentDisplay() {
        return String.format("%d%%", (int) Math.round(progress * 100.0));
    }

    public int getQueueIndex() {
        return queueIndex;
    }

    public void setQueueIndex(int queueIndex) {
        this.queueIndex = queueIndex;
    }

    public double getEstimatedDurationSec() {
        return estimatedDurationSec;
    }

    public double getRemainingDurationSec() {
        return remainingDurationSec;
    }

    /**
     * Recalculates remaining duration estimate based on scenario span and simulation cell count.
     */
    public void recalculateEstimate() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
            this.estimatedDurationSec = 0.0;
            this.remainingDurationSec = 0.0;
            this.progress = 1.0;
            this.executed = true;
            return;
        }
        if (scenario != null) {
            int cellCount = scenario.getInitialHumanCount() > 0
                    ? (int) Math.min(5000, 50 + scenario.getInitialHumanCount() / 2000)
                    : 4096;
            this.estimatedDurationSec = ExecutionContextPanel.estimateExecutionTimeSeconds(
                    scenario.getStartDateYear(),
                    scenario.getEndDateYear(),
                    cellCount,
                    ExecutionContextPanel.getActiveHardwareMode()
            );
            if (progress >= 1.0 || executed) {
                this.remainingDurationSec = 0.0;
            } else {
                this.remainingDurationSec = Math.max(0.0, this.estimatedDurationSec * (1.0 - this.progress));
            }
        }
    }

    public String getEstimatedDurationDisplay() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId) || (progress >= 1.0 && executed)) return "-";
        return ExecutionContextPanel.formatDuration(remainingDurationSec);
    }

    public String getStatusDisplay() {
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
            return I18n.getOrDefault("analytics.status.ground_truth_ready", "🟢 Historical Ground Truth (HYDE / Maddison / Seshat)");
        }
        if (progress >= 1.0 || executed) {
            return I18n.getOrDefault("analytics.status.executed_clean", "🟢 Executed");
        }
        if (progress > 0.0) {
            return String.format(I18n.getOrDefault("analytics.status.partial_fmt", "🟡 Partial (%d%%)"), (int) Math.round(progress * 100.0));
        }
        return I18n.getOrDefault("analytics.status.not_executed", "🔴 Not executed (Pending)");
    }

    @Override
    public String toString() {
        return getName();
    }
}
