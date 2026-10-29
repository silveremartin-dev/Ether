/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.ui.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Controller managing deferred H3 hexagonal planetary generation, clipping boundary filtering,
 * simulation thread launch, and background calculation progress monitoring.
 *
 * <p>Handles asynchronous threading transitions between JavaFX Application Thread and the background
 * generation executor, ensuring deterministic grid initialization and progress bar updates.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSimulationLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSimulationLauncher.class);

    /**
     * Host context contract connecting the launcher to the scenario setup panel.
     */
    public interface HostContext {
        Window getWindow();
        Node getContainerNode();
        boolean validateScenarioSetup();
        Scenario getScenario();
        PlanetPreset getActivePlanetPreset();
        EcologyPreset getActiveEcologyPreset();
        Integer getH3Resolution();
        boolean isClippingActive();
        double getClippingMinLat();
        double getClippingMaxLat();
        double getClippingMinLng();
        double getClippingMaxLng();
        boolean isResumeFromSnapshotSelected();
        SaveMetadata getSelectedSnapshotMetadata();
        boolean isSimulationCurrentlyRunning();
        boolean isDirty();
        boolean promptSaveIfDirty(Window owner);
        void distributeInitialPopulation(List<H3Cell> cells);
        void onCellsGenerated(List<H3Cell> cells);
        void markClean();
        void drawPreview();
        void setPreviewStatusText(String text);
    }

    private final HostContext host;
    private final ScenarioRepository scenarioRepo;
    private final Consumer<Scenario> onStartSimulation;

    // Controls
    private Button startBtn;
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private ProgressBar externalProgressBar;
    private Label externalStatusLabel;
    private Node externalOverlayContainer;

    // Threading & Cancellation
    private Thread generationThread;
    private volatile boolean cancelRequested = false;
    private boolean isCalculationRunning = false;

    /**
     * Constructs a simulation launcher.
     *
     * @param host callback context
     * @param scenarioRepo scenario database repository
     * @param onStartSimulation callback triggered when generation finishes and simulation begins
     */
    public ScenarioSimulationLauncher(HostContext host, ScenarioRepository scenarioRepo, Consumer<Scenario> onStartSimulation) {
        this.host = host;
        this.scenarioRepo = scenarioRepo;
        this.onStartSimulation = onStartSimulation;
    }

    /**
     * Binds UI start button and local progress indicators.
     *
     * @param startBtn start/cancel toggle button
     * @param progressBar progress bar indicator
     * @param progressStatusLabel progress status text label
     */
    public void bindControls(Button startBtn, ProgressBar progressBar, Label progressStatusLabel) {
        this.startBtn = startBtn;
        this.progressBar = progressBar;
        this.progressStatusLabel = progressStatusLabel;
    }

    /**
     * Sets external progress controls (e.g. from parent tab or dialog overlay).
     *
     * @param bar external progress bar
     * @param label external status label
     * @param overlayContainer external modal/overlay container
     */
    public void setProgressControls(ProgressBar bar, Label label, Node overlayContainer) {
        this.externalProgressBar = bar;
        this.externalStatusLabel = label;
        this.externalOverlayContainer = overlayContainer;
    }

    /**
     * Handles the start or cancellation action when the main action button is triggered.
     */
    public void handleStartOrCancel() {
        if (isCalculationRunning) {
            cancelCalculation();
        } else {
            if (host != null && host.isSimulationCurrentlyRunning()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(I18n.getOrDefault("scenario.confirm_restart.title", "Simulation en cours"));
                alert.setHeaderText(I18n.getOrDefault("scenario.confirm_restart.header", "Une simulation est actuellement active"));
                alert.setContentText(I18n.getOrDefault("scenario.confirm_restart.content", "Lancer cette nouvelle configuration réinitialisera la simulation en cours. Souhaitez-vous continuer ?"));
                Optional<ButtonType> res = alert.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) {
                    return;
                }
            }
            if (host != null && host.isDirty()) {
                Window window = host.getWindow();
                if (!host.promptSaveIfDirty(window)) {
                    return;
                }
            }
            startSimulationDeferred();
        }
    }

    /**
     * Cancels an ongoing background H3 grid generation.
     */
    public void cancelCalculation() {
        cancelRequested = true;
        updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelling", "🛑 Cancelling computation at user request..."));
        if (generationThread != null && generationThread.isAlive()) {
            generationThread.interrupt();
        }
        if (host != null) {
            WindowUtils.setBusyCursor(host.getContainerNode(), false);
        }
        resetStartButtonState();
    }

    /**
     * Starts deferred background planetary generation and simulation launching.
     */
    public void startSimulationDeferred() {
        if (host == null || !host.validateScenarioSetup()) {
            resetStartButtonState();
            return;
        }

        final Scenario scenarioToSave = host.getScenario();
        PlanetPreset baseCfg = host.getActivePlanetPreset();
        if (baseCfg == null) baseCfg = PlanetPreset.EARTH_LIKE;

        Integer res = host.getH3Resolution();
        if (res != null) {
            baseCfg = new PlanetPreset(
                    baseCfg.name(), res, baseCfg.radiusKm(), baseCfg.dayLengthHours(),
                    baseCfg.axialTiltDegrees(), baseCfg.yearLengthDays(), baseCfg.distanceToSunAU(),
                    baseCfg.solarLuminosity(), baseCfg.minAltitudeMeters(), baseCfg.maxAltitudeMeters(),
                    baseCfg.averageTempC(), baseCfg.seed(), baseCfg.noiseFrequency(),
                    baseCfg.noiseScale(), baseCfg.waterLevel(), baseCfg.temperatureGradient(),
                    baseCfg.oxygenPercentage(), baseCfg.albedo(), baseCfg.atmospherePressureAtm(),
                    baseCfg.isSatellite(), baseCfg.parentPlanetMassEarthMasses(),
                    baseCfg.orbitalDistanceToParentKm(), baseCfg.co2Ppm(),
                    baseCfg.seismicActivityLevel(), baseCfg.volcanicActivityLevel(),
                    baseCfg.customElevBase64(), baseCfg.customBiomeBase64(), baseCfg.customResourceBase64(),
                    baseCfg.customClimateBase64(), baseCfg.customRainfallBase64(), baseCfg.customSeasonalityBase64(),
                    baseCfg.elevationUseImport(), baseCfg.elevationMapSource(),
                    baseCfg.tempUseImport(), baseCfg.tempSource(), baseCfg.tempSeed(),
                    baseCfg.precipUseImport(), baseCfg.precipSource(), baseCfg.precipSeed(),
                    baseCfg.seasonUseImport(), baseCfg.seasonSource(), baseCfg.seasonSeed()
            );
        }
        final PlanetPreset cfg = baseCfg;

        final boolean isClippingActive = host.isClippingActive();
        final double cMinLat = host.getClippingMinLat();
        final double cMaxLat = host.getClippingMaxLat();
        final double cMinLng = host.getClippingMinLng();
        final double cMaxLng = host.getClippingMaxLng();

        if (host.isResumeFromSnapshotSelected()) {
            final var selectedMeta = host.getSelectedSnapshotMetadata();
            logger.info("Directly resuming simulation from snapshot: {}", selectedMeta != null ? selectedMeta.getName() : "latest");
            Platform.runLater(() -> {
                updateProgress(1.0, I18n.getOrDefault("scenario.progress.instant_resume", "⚡ Instant restoration from selected snapshot..."));
                setStartButtonCompletedState();
                if (onStartSimulation != null) {
                    onStartSimulation.accept(scenarioToSave);
                }
            });
            return;
        }

        cancelRequested = false;
        setStartButtonCancelState();
        WindowUtils.setBusyCursor(host.getContainerNode(), true);

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0.02);
        }
        if (progressStatusLabel != null) {
            progressStatusLabel.setVisible(true);
            progressStatusLabel.setManaged(true);
            progressStatusLabel.setText(I18n.getOrDefault("scenario.progress.init_prep", "⚡ Scenario initialization & setup..."));
        }
        if (externalOverlayContainer != null) {
            externalOverlayContainer.setVisible(true);
            externalOverlayContainer.setManaged(true);
        }

        generationThread = new Thread(() -> {
            try {
                updateProgress(0.02, I18n.getOrDefault("scenario.progress.saving_db", "⚡ Saving scenario & preparing H3 grid..."));

                if (!host.isDirty() && scenarioRepo != null) {
                    try {
                        scenarioToSave.setPlanetPreset(cfg);
                        scenarioToSave.setEcologyPreset(host.getActiveEcologyPreset());
                        if (scenarioToSave.getName() == null || scenarioToSave.getName().isBlank()) {
                            scenarioToSave.setName("Auto_Scenario_" + System.currentTimeMillis());
                        }
                        scenarioRepo.saveOrUpdate(scenarioToSave);
                        logger.info("Scenario '{}' automatically saved to database repository with full cascade before simulation launch.", scenarioToSave.getName());
                    } catch (Exception ex) {
                        logger.warn("Could not save scenario to DB prior to simulation launch", ex);
                    }
                }

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Generation cancelled by user");
                }

                updateProgress(0.05, String.format(I18n.getOrDefault("scenario.progress.ready_h3", "✅ Scenario '%s' ready! ⚡ Generating H3 grid..."), scenarioToSave.getName()));
                logger.info("Generating H3 cells for scenario '{}' at resolution {}", scenarioToSave.getName(), cfg.resolution());

                // Progress-tracked procedural planet generation (5% -> 70%)
                List<H3Cell> cells = new ProceduralGenerator().generatePlanet(cfg, (done, total) -> {
                    double progressVal = 0.05 + 0.65 * ((double) done / total);
                    int percent = (int) (progressVal * 100);
                    updateProgress(progressVal, String.format(I18n.getOrDefault("scenario.progress.relief_biomes", "🌍 Generating relief & H3 biomes: %d%% (%d / %d cells)"), percent, done, total));
                }, () -> cancelRequested || Thread.currentThread().isInterrupted());

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Generation cancelled by user");
                }

                applyPresetMapToCells(cells, cfg, scenarioToSave);

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Generation cancelled by user");
                }

                updateProgress(0.72, I18n.getOrDefault("scenario.progress.hydrography", "🌊 Computing hydrographic network & river basins (72%)..."));

                // Apply Geographical Clipping if enabled (78% -> 85%)
                if (isClippingActive) {
                    updateProgress(0.80, I18n.getOrDefault("scenario.progress.clipping", "✂️ Applying geographical clipping & boundary conditions (80%)..."));

                    double marginLat = Math.max(1.0, (cMaxLat - cMinLat) * 0.08);
                    double marginLng = Math.max(1.0, (cMaxLng - cMinLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
                    for (H3Cell c : cells) {
                        if (cancelRequested || Thread.currentThread().isInterrupted()) {
                            throw new CancellationException("Generation cancelled by user");
                        }
                        if (c.getLatitude() >= cMinLat && c.getLatitude() <= cMaxLat &&
                            c.getLongitude() >= cMinLng && c.getLongitude() <= cMaxLng) {
                            if (c.getLatitude() <= cMinLat + marginLat || c.getLatitude() >= cMaxLat - marginLat ||
                                c.getLongitude() <= cMinLng + marginLng || c.getLongitude() >= cMaxLng - marginLng) {
                                c.setBoundaryCell(true);
                            }
                            clippedCells.add(c);
                        }
                    }
                    cells = clippedCells;
                    logger.info("Clipping applied: {} cells retained out of global planet grid.", cells.size());
                }

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Generation cancelled by user");
                }

                int totalCellCount = cells.size();
                updateProgress(0.88, String.format(I18n.getOrDefault("scenario.progress.pop_capital", "👥 Distributing population, ecological footprint & capital K₀: 88%% (%d / %d cells)"), totalCellCount, totalCellCount));

                host.distributeInitialPopulation(cells);
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CancellationException("Generation cancelled by user");
                }

                updateProgress(0.94, String.format(I18n.getOrDefault("scenario.progress.thermo_sync", "⚡ Synchronizing simulation buffers & thermodynamic pre-computation: 94%% (%d / %d cells)"), totalCellCount, totalCellCount));

                host.onCellsGenerated(cells);

                final List<H3Cell> finalCells = cells;
                Platform.runLater(() -> {
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.launching", "✅ %,d H3 cells generated! Launching scenario '%s'..."), finalCells.size(), scenarioToSave.getName()));
                    host.drawPreview();
                    host.setPreviewStatusText(I18n.getOrDefault("scenario.status.generated_prefix", "Generated: ") + finalCells.size() + I18n.getOrDefault("scenario.status.generated_h3_suffix", " H3 cells."));

                    setStartButtonCompletedState();
                    host.markClean();

                    if (onStartSimulation != null) {
                        onStartSimulation.accept(scenarioToSave);
                    }
                });
            } catch (Exception ex) {
                boolean isCancel = cancelRequested || ex instanceof CancellationException
                        || ex instanceof InterruptedException
                        || (ex.getCause() instanceof CancellationException);
                if (isCancel) {
                    logger.info("Deferred H3 calculation cancelled by user.");
                    updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelled", "🛑 Computation cancelled by user."));
                } else {
                    logger.error("Error during deferred H3 cell calculation", ex);
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.error", "❌ Error during computation: %s"), ex.getMessage()));
                }
                resetStartButtonState();
            } finally {
                generationThread = null;
                if (host != null) {
                    WindowUtils.setBusyCursor(host.getContainerNode(), false);
                }
            }
        }, "H3-Scenario-Generator-Thread");
        generationThread.setDaemon(true);
        generationThread.start();
    }

    /**
     * Applies imported elevation and biome rasters to H3 cells.
     */
    public void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg, Scenario s) {
        if (cfg == null || cells == null || cells.isEmpty()) return;

        boolean isImportMode = cfg.elevationUseImport() || (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank());
        if (!isImportMode) {
            return;
        }

        String mapSource = cfg.elevationMapSource() != null ? cfg.elevationMapSource().toLowerCase() : "";
        String presetName = cfg.name() != null ? cfg.name().toLowerCase() : "";

        if (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank()) {
            try {
                Image customElev = ImageMapLoader.base64PngToImage(cfg.customElevBase64());
                Image customBiome = ImageMapLoader.base64PngToImage(cfg.customBiomeBase64());
                if (customElev != null) {
                    new ImageMapLoader().mapImagesToCells(cells, customElev, customBiome, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
                }
            } catch (Exception e) {
                logger.warn("Could not decode custom Base64 elevation map image for preset {}", cfg.name(), e);
            }
        } else {
            String pKey = "earth";
            if (mapSource.equals("mars") || presetName.contains("mars") || presetName.contains("ares")) pKey = "mars";
            else if (mapSource.equals("venus") || presetName.contains("venus") || presetName.contains("vénus") || presetName.contains("hesperos")) pKey = "venus";
            else if (mapSource.equals("moon") || presetName.contains("lune") || presetName.contains("moon") || presetName.contains("selene")) pKey = "moon";
            else if (mapSource.equals("mercury") || presetName.contains("mercure") || presetName.contains("mercury") || presetName.contains("hermes")) pKey = "mercury";
            else if (mapSource.equals("titan") || presetName.contains("titan")) pKey = "titan";

            long targetYear = s != null ? s.getStartDateYear() : 2026L;
            Image elevImg = ImageMapLoader.loadMapImage(pKey, targetYear, "elevation.png");
            Image biomeImg = ImageMapLoader.loadMapImage(pKey, targetYear, "biomes.png");

            if (elevImg != null || biomeImg != null) {
                new ImageMapLoader().mapImagesToCells(cells, elevImg, biomeImg, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
            } else {
                logger.debug("No precalculated map found for preset {}, using physical terrain generator", pKey);
            }
        }
    }

    /**
     * Updates progress bar and text indicators safely on JavaFX Application Thread.
     *
     * @param progressVal normalized progress [0.0, 1.0]
     * @param msg status label message
     */
    public void updateProgress(double progressVal, String msg) {
        Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progressVal);
                progressBar.setVisible(true);
                progressBar.setManaged(true);
            }
            if (progressStatusLabel != null) {
                progressStatusLabel.setText(msg);
                progressStatusLabel.setVisible(true);
                progressStatusLabel.setManaged(true);
            }
            if (externalProgressBar != null) {
                externalProgressBar.setProgress(progressVal);
            }
            if (externalStatusLabel != null) {
                externalStatusLabel.setText(msg);
            }
            if (externalOverlayContainer != null) {
                boolean active = progressVal < 1.0;
                externalOverlayContainer.setVisible(active);
                externalOverlayContainer.setManaged(active);
            }
        });
    }

    /**
     * Resets the start button to ready state.
     */
    public void resetStartButtonState() {
        isCalculationRunning = false;
        if (host != null) {
            WindowUtils.setBusyCursor(host.getContainerNode(), false);
        }
        Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.start_btn", "🚀 VALIDER ET LANCER LA SIMULATION"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    /**
     * Sets start button to completed state.
     */
    public void setStartButtonCompletedState() {
        isCalculationRunning = false;
        if (host != null) {
            WindowUtils.setBusyCursor(host.getContainerNode(), false);
        }
        Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.btn.valid_ready", "⚡ VALID & READY — PROCEED TO EXECUTION CONTEXT (TAB 4)"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    /**
     * Sets start button to cancel state.
     */
    public void setStartButtonCancelState() {
        isCalculationRunning = true;
        Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.btn.cancel_gen", "⏹ CANCEL / STOP GENERATION"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    public boolean isCalculationRunning() { return isCalculationRunning; }
}
