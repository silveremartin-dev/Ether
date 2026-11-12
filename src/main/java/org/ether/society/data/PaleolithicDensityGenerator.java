package org.ether.society.data;

import java.awt.image.BufferedImage;
import static org.ether.society.data.PaleoGeographyCoordinatesRegistry.*;

/**
 * Generates high-fidelity archaeological and paleolithic synthetic population density fields
 * spanning deep prehistory from -100,000 BP through early agricultural transitions (-4,500 BP).
 *
 * <p>Integrates multi-clade hominin dispersal models (Homo sapiens, Neanderthal, Denisovan),
 * glacial refugia (LGM, Fennoscandian / Laurentide ice sheets), and archaeological hearths.</p>
 */
public final class PaleolithicDensityGenerator {

    private PaleolithicDensityGenerator() {
        // Utility class
    }

    /**
     * Apply prehistoric geographic mask.
     *
     * @param scenarioKey the scenario key parameter (String)
     * @param year the year parameter (long)
     * @param src the src parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {

        if (src == null) return null;
        if (year >= -10000) return src;

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / h;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / w;
                int rgb = src.getRGB(x, y);

                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (weight > 0.001 && HistoricalMapGenerator.isLand(lon, lat)) {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int lum = (int) ((0.299 * r + 0.587 * g + 0.114 * b) * weight);
                    lum = Math.clamp(lum, 0, 255);
                    masked.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
                } else {
                    masked.setRGB(x, y, 0x000000); // Pure black for uninhabited regions & oceans
                }
            }
        }
        return masked;
    }

    /**
     * Generate prehistoric synthetic density map.
     *
     * @param year the year parameter (long)
     * @param type the type parameter (String)
     * @return the resulting computation or state reference
     */
    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lng = (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0 - 180.0;
                boolean isLand = HistoricalMapGenerator.isLand(lng, lat);
                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lng, lat, year);
                if (!isLand || weight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                double dens = PaleolithicEpochDensityHelper.computeDensity(lng, lat, year);
                dens *= weight;
                if (dens > 0.0005) {
                    double logNorm = Math.log1p(2.5 * dens) / Math.log1p(2.5 * 35.0); // reference 35.0 hab/km2
                    int gray = (int) Math.clamp(12.0 + logNorm * 243.0, 12.0, 255.0);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(x, y, 0x000000);
                }
            }
        }
        return img;
    }

    /**
     * Save images to disk cache.
     *
     * @param scenarioName the scenario name parameter (String)
     * @param imgDensity the img density parameter (BufferedImage)
     * @param imgSovereignty the img sovereignty parameter (BufferedImage)
     * @param imgIsogloss the img isogloss parameter (BufferedImage)
     * @param imgKinship the img kinship parameter (BufferedImage)
     * @param imgRituals the img rituals parameter (BufferedImage)
     * @param imgTech the img tech parameter (BufferedImage)
     * @param imgTrade the img trade parameter (BufferedImage)
     * @param imgInst the img inst parameter (BufferedImage)
     * @param imgEco the img eco parameter (BufferedImage)
     * @param imgPathogen the img pathogen parameter (BufferedImage)
     */

}
