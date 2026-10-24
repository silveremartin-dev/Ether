package org.ether.society.ui.resource;

import javafx.scene.control.Slider;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;

/**
 * <h2>Resource Planet Sync Manager</h2>
 * <p>
 * Manages geophysical, thermodynamic, and ecological deductions derived from inherited
 * planetary presets. Provides analytical scaling of continental biomass, soil carbon,
 * aquifers, crustal metals, mantle heat, and terrain compatibility checks.
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 */
public final class ResourcePlanetSyncManager {

    private ResourcePlanetSyncManager() {
        // Utility class
    }

    /**
     * Adapts ecological and resource sliders based on the selected celestial body characteristics.
     *
     * @param p active planet preset
     * @param biomeBuilder biome section builder
     * @param hydroBuilder hydro section builder
     * @param crustalMetalSlider industrial metal slider
     * @param preciousMetalSlider precious metal slider
     * @param mantleHeatSlider mantle heat flux slider
     * @param seismicSlider seismic activity slider
     * @param volcanicSlider volcanic activity slider
     */
    public static void adaptResourceSlidersToPlanet(
            PlanetPreset p,
            ResourceBiomeSectionBuilder biomeBuilder,
            ResourceHydroSectionBuilder hydroBuilder,
            Slider crustalMetalSlider,
            Slider preciousMetalSlider,
            Slider mantleHeatSlider,
            Slider seismicSlider,
            Slider volcanicSlider) {

        if (p == null) return;

        if (seismicSlider != null) seismicSlider.setValue(p.seismicActivityLevel());
        if (volcanicSlider != null) volcanicSlider.setValue(p.volcanicActivityLevel());

        if (p.isSatellite() || p.radiusKm() < 3000) { // Moon/Titan
            if (biomeBuilder.getTerrestrialBiomassSlider() != null) biomeBuilder.getTerrestrialBiomassSlider().setValue(10.0);
            if (biomeBuilder.getSoilCarbonSlider() != null) biomeBuilder.getSoilCarbonSlider().setValue(50.0);
            if (biomeBuilder.getFaunaBiomassSlider() != null) biomeBuilder.getFaunaBiomassSlider().setValue(0.01);
            if (crustalMetalSlider != null) crustalMetalSlider.setValue(150.0);
            if (mantleHeatSlider != null) mantleHeatSlider.setValue(25.0);
            if (seismicSlider != null) seismicSlider.setValue(1.0);
            if (volcanicSlider != null) volcanicSlider.setValue(0.5);
        } else if (p.waterLevel() < -0.2) { // Arid / Mars
            if (biomeBuilder.getTerrestrialBiomassSlider() != null) biomeBuilder.getTerrestrialBiomassSlider().setValue(50.0);
            if (biomeBuilder.getSoilCarbonSlider() != null) biomeBuilder.getSoilCarbonSlider().setValue(200.0);
            if (biomeBuilder.getAquaticBiomassSlider() != null) biomeBuilder.getAquaticBiomassSlider().setValue(0.2);
            if (hydroBuilder.getFreshwaterAquiferSlider() != null) hydroBuilder.getFreshwaterAquiferSlider().setValue(1200.0);
            if (seismicSlider != null) seismicSlider.setValue(1.2);
            if (volcanicSlider != null) volcanicSlider.setValue(0.8);
        } else if (p.waterLevel() > 0.4) { // Ocean World
            if (biomeBuilder.getTerrestrialBiomassSlider() != null) biomeBuilder.getTerrestrialBiomassSlider().setValue(120.0);
            if (biomeBuilder.getAquaticBiomassSlider() != null) biomeBuilder.getAquaticBiomassSlider().setValue(25.0);
            if (hydroBuilder.getFreshwaterAquiferSlider() != null) hydroBuilder.getFreshwaterAquiferSlider().setValue(45000.0);
        } else { // Earth-Like
            if (biomeBuilder.getTerrestrialBiomassSlider() != null) biomeBuilder.getTerrestrialBiomassSlider().setValue(450.0);
            if (biomeBuilder.getSoilCarbonSlider() != null) biomeBuilder.getSoilCarbonSlider().setValue(1500.0);
            if (biomeBuilder.getFaunaBiomassSlider() != null) biomeBuilder.getFaunaBiomassSlider().setValue(2.0);
            if (biomeBuilder.getAquaticBiomassSlider() != null) biomeBuilder.getAquaticBiomassSlider().setValue(6.0);
            if (crustalMetalSlider != null) crustalMetalSlider.setValue(80.0);
            if (preciousMetalSlider != null) preciousMetalSlider.setValue(1200.0);
            if (mantleHeatSlider != null) mantleHeatSlider.setValue(87.0);
            if (hydroBuilder.getFreshwaterAquiferSlider() != null) hydroBuilder.getFreshwaterAquiferSlider().setValue(15000.0);
        }
    }

    /**
     * Builds localized markdown summary string of thermodynamic constraints for a planet preset.
     *
     * @param p active planet preset
     * @return markdown summary
     */
    public static String buildThermodynamicSynthesisText(PlanetPreset p) {
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.getOrDefault("planet.thermo.header", "🔬 **Synthèse Thermodynamique (%s)** :\n", I18n.getPlanetPresetDisplayName(p.name())));

        if (p.atmospherePressureAtm() < 0.01) {
            sb.append(I18n.getOrDefault("planet.thermo.no_atmosphere", "• **Atmosphère absente ou extrêmement faible** (%s atm) : Eau liquide impossible à la surface (sublimation). Pluviométrie nulle, biomasse nulle.\n", String.format("%.3f", p.atmospherePressureAtm())));
        } else if (p.waterLevel() < -0.2) {
            sb.append(I18n.getOrDefault("planet.thermo.arid", "• **Monde Aride / Désertique** (Eau %s%%) : Aquifères restreints, faune concentrée dans les bassins continentaux.\n", String.format("%.0f", (1.0 + p.waterLevel()) * 50)));
        } else if (p.waterLevel() > 0.4) {
            sb.append(I18n.getOrDefault("planet.thermo.ocean", "• **Monde Océan** (Eau %s%%) : Biomasse marine hyper-dominante (phytoplancton), réserves en eau douce majeures.\n", String.format("%.0f", (1.0 + p.waterLevel()) * 50)));
        } else {
            sb.append(I18n.getOrDefault("planet.thermo.balanced", "• **Balanced Ecosystem (Earth-Like)**: Complete continental water cycle, optimal forest and wildlife biomass.\n"));
        }

        if (p.isSatellite() || p.radiusKm() < 3000) {
            sb.append(I18n.getOrDefault("planet.thermo.small_body", "• **Corps de Faible Rayon (%s km)** : Refroidissement du manteau rapide ➔ Flux thermique et métaux industriels réduits.", String.format("%.0f", p.radiusKm())));
        } else {
            sb.append(I18n.getOrDefault("planet.thermo.massive_body", "• **Corps Massif (%s km)** : Tectonique des plaques active, réserves métalliques crustales et minerais précieux abondants.", String.format("%.0f", p.radiusKm())));
        }

        return sb.toString();
    }

    /**
     * Formats the context display label for the inherited planet.
     *
     * @param preset active planet preset
     * @param cellCount active simulation cell count
     * @return formatted string
     */
    public static String buildPlanetContextText(PlanetPreset preset, int cellCount) {
        if (preset == null) preset = PlanetPreset.EARTH_LIKE;
        return I18n.getOrDefault(
                "planet.context.fmt",
                "🪐 Planète Héritée : %s | Rayon : %,.0f km | Temp : %.1f°C | Eau : %.0f%% | Cellules : %,d",
                I18n.getPlanetPresetDisplayName(preset.name()),
                preset.radiusKm(),
                preset.averageTempC(),
                preset.waterLevel() * 100.0,
                cellCount
        );
    }

    /**
     * Validates whether an imported relief/thematic map corresponds to the active celestial body.
     *
     * @param sourceKey source name (e.g. "earth", "mars", "moon")
     * @param activePreset active planet preset
     * @return true if compatible
     */
    public static boolean isTerrainMapCompatible(String sourceKey, PlanetPreset activePreset) {
        if ("none".equals(sourceKey) || sourceKey == null) return true;
        String planetName = activePreset != null ? activePreset.name().toLowerCase() : "earth";
        String elevSrc = activePreset != null ? activePreset.elevationMapSource() : null;

        if ("earth".equalsIgnoreCase(sourceKey)) {
            if ("earth".equalsIgnoreCase(elevSrc)) return true;
            return planetName.contains("earth") || planetName.contains("terre") || planetName.contains("terran")
                    || planetName.contains("terrestre") || planetName.contains("standard") || planetName.contains("gaia")
                    || planetName.contains("archipel") || planetName.contains("oceania") || planetName.contains("océan") || planetName.contains("ocean");
        } else if ("mars".equalsIgnoreCase(sourceKey)) {
            if ("mars".equalsIgnoreCase(elevSrc)) return true;
            return planetName.contains("mars") || planetName.contains("ares") || planetName.contains("désert") || planetName.contains("desert");
        } else if ("venus".equalsIgnoreCase(sourceKey)) {
            if ("venus".equalsIgnoreCase(elevSrc)) return true;
            return planetName.contains("venus") || planetName.contains("vénus") || planetName.contains("hesperos");
        } else if ("moon".equalsIgnoreCase(sourceKey)) {
            if ("moon".equalsIgnoreCase(elevSrc)) return true;
            return planetName.contains("lune") || planetName.contains("moon") || planetName.contains("selene")
                    || planetName.contains("titan") || planetName.contains("cryo") || (activePreset != null && activePreset.isSatellite());
        } else if ("mercury".equalsIgnoreCase(sourceKey)) {
            if ("mercury".equalsIgnoreCase(elevSrc)) return true;
            return planetName.contains("mercury") || planetName.contains("mercure") || planetName.contains("hermes");
        }
        return true;
    }
}
