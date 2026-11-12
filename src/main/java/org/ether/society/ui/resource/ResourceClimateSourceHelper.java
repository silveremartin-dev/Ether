/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowHelper;

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
        WindowHelper.showScrollableInfoDialog(
                I18n.getOrDefault("planet.dialog.climate_help_title", "Climate Map Specifications"),
                I18n.getOrDefault("planet.dialog.climate_help_header", "Supported Image Formats for Climate Data"),
                "Images PNG/JPEG in 2:1 ratio (e.g. 2048x1024 px, Equirectangular Plate Carree projection).\n\n" +
                "1. SEPARATE MAPS IN GRAYSCALE OR THERMAL GRADIENTS:\n" +
                "   â€¢ Temperature: Black / Blue = -50Â°C, White / Red = +50Â°C\n" +
                "   â€¢ Precipitation: Black = 0 mm/yr, White / Dark Blue = 3000 mm/yr\n" +
                "   â€¢ Seasonality: Black = 0Â°C, White / Magenta = 50Â°C annual amplitude\n\n" +
                "2. COMBINED MULTI-CHANNEL RGB MAP:\n" +
                "   â€¢ R (Red) = Temperature (-50Â°C to +50Â°C)\n" +
                "   â€¢ G (Green) = Precipitation (0 to 3000 mm/yr)\n" +
                "   â€¢ B (Blue) = Seasonality / Thermal amplitude (0 to 50Â°C)\n\n" +
                "3. WMS SATELLITE DOWNLOAD:\n" +
                "   â€¢ NASA MODIS (thermal) and NASA/JAXA GPM (precipitation) live layers."
        );
    }

    public static String getClimateSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "ðŸŒ¡ï¸ Mars Viking/MGS Thermal Inertia Model";
            case "moon" -> "ðŸŒ¡ï¸ Moon Diviner Lunar Radiometer Experiment";
            case "venus" -> "ðŸŒ¡ï¸ Venus Venera/VIRTIS Thermal Greenhouse Model";
            case "mercury" -> "ðŸŒ¡ï¸ Mercury MESSENGER Thermal Radiometry";
            default -> "ðŸŒ¡ï¸ Earth ERA5 / MODIS Land Surface Temp";
        };
    }

    public static String getRainfallSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "ðŸŒ§ï¸ Mars Atmospheric Water Vapor & Sublimation";
            case "moon" -> "ðŸŒ§ï¸ Moon Vacuum Exosphere (0 mm/yr Precipitation)";
            case "venus" -> "ðŸŒ§ï¸ Venus Sulfuric Acid Cloud Condensation & Virga";
            case "mercury" -> "ðŸŒ§ï¸ Mercury Exospheric Zero Precipitation";
            default -> "ðŸŒ§ï¸ Earth NASA GPM IMERG Precipitation";
        };
    }

    public static String getSeasonalitySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "â˜€ï¸ Mars Obliquity (25.2Â°) Thermal Seasonality";
            case "moon" -> "â˜€ï¸ Moon Diurnal Day/Night Extreme Seasonality";
            case "venus" -> "â˜€ï¸ Venus Slow Retrograde Thermal Inertia";
            case "mercury" -> "â˜€ï¸ Mercury 3:2 Spin-Orbit Thermal Resonance";
            default -> "â˜€ï¸ Earth Milankovitch Axial Obliquity Seasonality";
        };
    }
}

