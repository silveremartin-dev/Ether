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
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        short[] data = ((java.awt.image.DataBufferUShort) masked.getRaster().getDataBuffer()).getData();

        int idx = 0;
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / h;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / w;
                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (weight > 0.001 && HistoricalMapGenerator.isLand(lon, lat)) {
                    double baseDensKm2 = Hyde34GridReader.samplePixelDensityKm2(src, x, y);
                    int u16 = Hyde34GridReader.encodeDensityKm2ToU16(baseDensKm2 * weight);
                    data[idx++] = (short) (u16 & 0xFFFF);
                } else {
                    data[idx++] = 0; // Pure black for uninhabited regions & oceans
                }
            }
        }
        return masked;
    }

    /**
     * Generate prehistoric synthetic density map in 16-bit grayscale ({@link BufferedImage#TYPE_USHORT_GRAY}, [0..65535]).
     *
     * @param year the year parameter (long)
     * @param type the type parameter (String)
     * @return the resulting computation or state reference
     */
    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        int w = HistoricalMapGenerator.WIDTH, h = HistoricalMapGenerator.HEIGHT;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        short[] data = ((java.awt.image.DataBufferUShort) img.getRaster().getDataBuffer()).getData();
        int idx = 0;
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) / h * 180.0;
            for (int x = 0; x < w; x++) {
                double lng = (x + 0.5) / w * 360.0 - 180.0;
                boolean isLand = HistoricalMapGenerator.isLand(lng, lat);
                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lng, lat, year);
                if (!isLand || weight <= 0.001) {
                    data[idx++] = 0;
                    continue;
                }

                double dens = PaleolithicEpochDensityHelper.computeDensity(lng, lat, year);
                dens *= weight;
                int u16 = Hyde34GridReader.encodeDensityKm2ToU16(dens);
                data[idx++] = (short) (u16 & 0xFFFF);
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
