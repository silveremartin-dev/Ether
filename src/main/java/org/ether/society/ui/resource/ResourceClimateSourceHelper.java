/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;

import java.util.Locale;

/**
 * Display names and help documentation helper for {@link ResourceClimateSectionBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ResourceClimateSourceHelper {

    private ResourceClimateSourceHelper() {}

    public static void showClimateImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("planet.dialog.climate_help_title", "Climate Map Specifications"),
                I18n.getOrDefault("planet.dialog.climate_help_header", "Supported Image Formats for Climate Data"),
                "Images PNG/JPEG in 2:1 ratio (e.g. 2048x1024 px, Equirectangular Plate Carree projection).\n\n" +
                "1. SEPARATE MAPS IN GRAYSCALE OR THERMAL GRADIENTS:\n" +
                "   • Temperature: Black / Blue = -50°C, White / Red = +50°C\n" +
                "   • Precipitation: Black = 0 mm/yr, White / Dark Blue = 3000 mm/yr\n" +
                "   • Seasonality: Black = 0°C, White / Magenta = 50°C annual amplitude\n\n" +
                "2. COMBINED MULTI-CHANNEL RGB MAP:\n" +
                "   • R (Red) = Temperature (-50°C to +50°C)\n" +
                "   • G (Green) = Precipitation (0 to 3000 mm/yr)\n" +
                "   • B (Blue) = Seasonality / Thermal amplitude (0 to 50°C)\n\n" +
                "3. WMS SATELLITE DOWNLOAD:\n" +
                "   • NASA MODIS (thermal) and NASA/JAXA GPM (precipitation) live layers."
        );
    }

    public static String getClimateSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌡️ Mars Viking/MGS Thermal Inertia Model";
            case "moon" -> "🌡️ Moon Diviner Lunar Radiometer Experiment";
            case "venus" -> "🌡️ Venus Venera/VIRTIS Thermal Greenhouse Model";
            case "mercury" -> "🌡️ Mercury MESSENGER Thermal Radiometry";
            default -> "🌡️ Earth ERA5 / MODIS Land Surface Temp";
        };
    }

    public static String getRainfallSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌧️ Mars Atmospheric Water Vapor & Sublimation";
            case "moon" -> "🌧️ Moon Vacuum Exosphere (0 mm/yr Precipitation)";
            case "venus" -> "🌧️ Venus Sulfuric Acid Cloud Condensation & Virga";
            case "mercury" -> "🌧️ Mercury Exospheric Zero Precipitation";
            default -> "🌧️ Earth NASA GPM IMERG Precipitation";
        };
    }

    public static String getSeasonalitySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "☀️ Mars Obliquity (25.2°) Thermal Seasonality";
            case "moon" -> "☀️ Moon Diurnal Day/Night Extreme Seasonality";
            case "venus" -> "☀️ Venus Slow Retrograde Thermal Inertia";
            case "mercury" -> "☀️ Mercury 3:2 Spin-Orbit Thermal Resonance";
            default -> "☀️ Earth Milankovitch Axial Obliquity Seasonality";
        };
    }
}
