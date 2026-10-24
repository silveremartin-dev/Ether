package org.ether.society.ui.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.EcologyPreset;
import org.ether.society.persistence.EtherJsonUtil;
import org.ether.society.persistence.PresetStorageService;
import org.ether.society.ui.PresetControlBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <h2>Resource Preset Manager</h2>
 * <p>
 * Manages serialization, deserialization, loading, saving, and dirty tracking
 * of planetary {@link EcologyPreset} instances. Coordinates cross-domain synchronizations
 * across flora/biomes, hydrography, climate, and lithospheric mineral reserves.
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 */
public class ResourcePresetManager {

    private static final Logger logger = LoggerFactory.getLogger(ResourcePresetManager.class);

    /**
     * Callback interface providing access to host panel controls and domain builders.
     */
    public interface HostContext {
        PlanetPreset getActivePlanetPreset();
        void setActivePlanetPreset(PlanetPreset preset);
        ComboBox<PlanetPreset> getPlanetPresetCombo();
        Supplier<PlanetPreset> getPlanetPresetSupplier();
        Consumer<PlanetPreset> getPlanetPresetApplyCallback();
        ResourceBiomeSectionBuilder getBiomeSectionBuilder();
        ResourceHydroSectionBuilder getHydroSectionBuilder();
        ResourceClimateSectionBuilder getClimateSectionBuilder();
        Slider getCrustalMetalSlider();
        Slider getPreciousMetalSlider();
        Slider getMantleHeatSlider();
        TextField getSeedField();
        Image getCustomResourceImage();
        void setCustomResourceImage(Image img);
        void setResourceFileLabelText(String text);
        boolean validateResourceSetup();
        void selectViewMode(int viewModeIndex);
        void updateSummary();
        void updateLegend();
        void updatePreviewCanvas();
        void prepopulateGeologyTensors(String planetKey, long epochYear);
        String getGeologySourceDisplayName(String planetKey);
    }

    private final HostContext host;
    private final PresetControlBar<EcologyPreset> ecologyPresetBar;
    private final TextArea ecologyDescriptionArea;
    private boolean isUpdatingFromPreset = false;

    public ResourcePresetManager(HostContext host) {
        this.host = host;
        this.ecologyPresetBar = new PresetControlBar<>("resource.preset_title", "Préréglage Écologique & Ressources");
        this.ecologyPresetBar.setExportCategory("ecology");
        this.ecologyPresetBar.setPresets(PresetStorageService.loadAllEcologyPresets(), EcologyPreset.EARTH_STANDARD);

        this.ecologyDescriptionArea = new TextArea();
        this.ecologyDescriptionArea.setPrefRowCount(4);
        this.ecologyDescriptionArea.setWrapText(true);
        this.ecologyDescriptionArea.setEditable(false);
        this.ecologyDescriptionArea.getStyleClass().add("glass-text-area");
        this.ecologyDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");

        this.ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                if (preset != null) {
                    PlanetPreset p = preset.embeddedPlanetPreset() != null
                            ? preset.embeddedPlanetPreset()
                            : findPlanetPresetByName(preset.planetPresetName());
                    if (p != null) {
                        host.setActivePlanetPreset(p);
                        if (host.getPlanetPresetCombo() != null) {
                            host.getPlanetPresetCombo().setValue(p);
                        }
                        if (host.getPlanetPresetApplyCallback() != null) {
                            host.getPlanetPresetApplyCallback().accept(p);
                        }
                    }
                }
                applyEcologyPreset(preset);
                host.selectViewMode(1);
            }

            @Override
            public void onSavePreset(String name) {
                if (!host.validateResourceSetup()) {
                    return;
                }
                long seedVal = 12345L;
                try {
                    if (host.getSeedField() != null) {
                        seedVal = Long.parseLong(host.getSeedField().getText());
                    }
                } catch (NumberFormatException ignored) {}

                String customBiomeB64 = host.getBiomeSectionBuilder().getCustomBiomeImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getBiomeSectionBuilder().getCustomBiomeImage()) : null;
                String customResourceB64 = host.getCustomResourceImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getCustomResourceImage()) : null;
                String customHydroB64 = host.getHydroSectionBuilder().getCustomHydroImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getHydroSectionBuilder().getCustomHydroImage()) : null;
                String customClimateB64 = host.getClimateSectionBuilder().getCustomClimateImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getClimateSectionBuilder().getCustomClimateImage()) : null;
                String customRainfallB64 = host.getClimateSectionBuilder().getCustomRainfallImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getClimateSectionBuilder().getCustomRainfallImage()) : null;
                String customSeasonalityB64 = host.getClimateSectionBuilder().getCustomSeasonalityImage() != null
                        ? ImageMapLoader.imageToBase64Png(host.getClimateSectionBuilder().getCustomSeasonalityImage()) : null;

                PlanetPreset currentPlanet = host.getPlanetPresetSupplier() != null
                        ? host.getPlanetPresetSupplier().get() : host.getActivePlanetPreset();
                if (currentPlanet == null) currentPlanet = PlanetPreset.EARTH_LIKE;
                String planetName = currentPlanet.name() != null ? currentPlanet.name() : PlanetPreset.EARTH_LIKE.name();

                EcologyPreset custom = new EcologyPreset(
                        name,
                        planetName,
                        currentPlanet,
                        host.getBiomeSectionBuilder().getTerrestrialBiomassSlider().getValue(),
                        host.getBiomeSectionBuilder().getSoilCarbonSlider().getValue(),
                        host.getBiomeSectionBuilder().getFaunaBiomassSlider().getValue(),
                        host.getBiomeSectionBuilder().getAquaticBiomassSlider().getValue(),
                        host.getCrustalMetalSlider().getValue(),
                        host.getPreciousMetalSlider().getValue(),
                        host.getMantleHeatSlider().getValue(),
                        host.getHydroSectionBuilder().getFreshwaterAquiferSlider().getValue(),
                        seedVal,
                        customBiomeB64,
                        customResourceB64,
                        customHydroB64,
                        customClimateB64,
                        customRainfallB64,
                        customSeasonalityB64
                );
                ecologyPresetBar.getPresetCombo().getItems().add(custom);
                ecologyPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(EcologyPreset preset) {
                ecologyPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, EcologyPreset preset) {
                try {
                    ObjectMapper mapper = EtherJsonUtil.getMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported ecology preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export ecology preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = EtherJsonUtil.getMapper();
                    EcologyPreset preset = mapper.readValue(sourceFile, EcologyPreset.class);
                    ecologyPresetBar.getPresetCombo().getItems().add(preset);
                    ecologyPresetBar.getPresetCombo().setValue(preset);
                    applyEcologyPreset(preset);
                    logger.info("Imported ecology preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import ecology preset", ex);
                }
            }
        });
    }

    public PresetControlBar<EcologyPreset> getPresetBar() {
        return ecologyPresetBar;
    }

    public TextArea getDescriptionArea() {
        return ecologyDescriptionArea;
    }

    public boolean isUpdatingFromPreset() {
        return isUpdatingFromPreset;
    }

    public void setUpdatingFromPreset(boolean updating) {
        this.isUpdatingFromPreset = updating;
    }

    public EcologyPreset getSelectedEcologyPreset() {
        return ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null
                ? ecologyPresetBar.getPresetCombo().getValue() : null;
    }

    public boolean isDirty() {
        return ecologyPresetBar != null && ecologyPresetBar.isDirty();
    }

    public boolean promptSaveIfDirty(Window owner) {
        if (ecologyPresetBar == null) return true;
        return ecologyPresetBar.promptSavePresetIfDirty(owner);
    }

    public void notifyParametersChanged() {
        if (!isUpdatingFromPreset && ecologyPresetBar != null) {
            ecologyPresetBar.notifyParametersChanged();
        }
    }

    public void applyEcologyPreset(EcologyPreset p) {
        if (p == null) return;
        isUpdatingFromPreset = true;
        try {
            if (ecologyDescriptionArea != null) {
                ecologyDescriptionArea.setText(p.getPresetDescription());
            }
            if (host.getBiomeSectionBuilder().getTerrestrialBiomassSlider() != null) {
                host.getBiomeSectionBuilder().getTerrestrialBiomassSlider().setValue(p.terrestrialBiomassGtC());
            }
            if (host.getBiomeSectionBuilder().getSoilCarbonSlider() != null) {
                host.getBiomeSectionBuilder().getSoilCarbonSlider().setValue(p.soilOrganicCarbonGtC());
            }
            if (host.getBiomeSectionBuilder().getFaunaBiomassSlider() != null) {
                host.getBiomeSectionBuilder().getFaunaBiomassSlider().setValue(p.faunaBiomassGtC());
            }
            if (host.getBiomeSectionBuilder().getAquaticBiomassSlider() != null) {
                host.getBiomeSectionBuilder().getAquaticBiomassSlider().setValue(p.aquaticBiomassGtC());
            }
            if (host.getCrustalMetalSlider() != null) {
                host.getCrustalMetalSlider().setValue(p.crustalMetalOresGt());
            }
            if (host.getPreciousMetalSlider() != null) {
                host.getPreciousMetalSlider().setValue(p.preciousMetalOresMt());
            }
            if (host.getMantleHeatSlider() != null) {
                host.getMantleHeatSlider().setValue(p.mantleHeatFlowMwM2());
            }
            if (host.getHydroSectionBuilder().getFreshwaterAquiferSlider() != null) {
                host.getHydroSectionBuilder().getFreshwaterAquiferSlider().setValue(p.freshwaterReserveKm3());
            }
            if (p.seed() != 0 && host.getSeedField() != null) {
                host.getSeedField().setText(String.valueOf(p.seed()));
            }
            String planetKey = p.getCanonicalPlanet();
            long epochYear = p.getAssociatedEpochYear();
            host.getBiomeSectionBuilder().applyPresetBiome(p, planetKey, epochYear);

            if (p.customResourceBase64() != null) {
                host.setCustomResourceImage(ImageMapLoader.base64PngToImage(p.customResourceBase64()));
                host.setResourceFileLabelText("🪨 Preset Resources");
            } else if ("earth".equals(planetKey)) {
                String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
                host.setResourceFileLabelText(host.getGeologySourceDisplayName("earth") + " (" + yearStr + ")");
                host.prepopulateGeologyTensors("earth", epochYear);
            } else if (planetKey != null && !"none".equals(planetKey)) {
                host.setResourceFileLabelText(host.getGeologySourceDisplayName(planetKey));
                host.prepopulateGeologyTensors(planetKey, epochYear);
            } else {
                host.setCustomResourceImage(null);
                host.setResourceFileLabelText("— Aucun fichier —");
            }

            host.getHydroSectionBuilder().applyPresetHydro(p, planetKey, epochYear);
            host.getClimateSectionBuilder().applyPresetClimate(p, planetKey, epochYear);
            host.selectViewMode(1);
            if (ecologyPresetBar != null) {
                ecologyPresetBar.markClean(p);
            }
        } finally {
            isUpdatingFromPreset = false;
        }
        host.updateSummary();
        host.updateLegend();
        host.updatePreviewCanvas();
    }

    public static PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return PlanetPreset.EARTH_LIG_100000BP;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return PlanetPreset.EARTH_MIS3_50000BP;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire") || lower.contains("solutrean") || lower.contains("solutréen")) return PlanetPreset.EARTH_LGM_20000BP;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene") || lower.contains("dryas")) return PlanetPreset.EARTH_EH_10000BP;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return PlanetPreset.EARTH_MH_6000BP;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return PlanetPreset.EARTH_LH_3000BP;
        if (lower.contains("-1900") || lower.contains("bronze")) return PlanetPreset.EARTH_BRONZE_1900BP;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return PlanetPreset.EARTH_IRON_1000BP;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
        }
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
        }
        return PlanetPreset.EARTH_LIKE;
    }
}
