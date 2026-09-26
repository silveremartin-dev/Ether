package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

public class Compare2025ClimateDiagnosticTest {

    @Test
    public void compare2025WithEmpirical() throws Exception {
        // 1. Generate procedural 2025 Biomes & Temperature
        BufferedImage procBiomes = HistoricalMapGenerator.rasterizeBiomesMap(2025);
        BufferedImage procTemp = HistoricalMapGenerator.rasterizeTemperatureMap(2025);
        BufferedImage procPrecip = HistoricalMapGenerator.rasterizePrecipitationMap(2025);
        BufferedImage procSeas = HistoricalMapGenerator.rasterizeSeasonalityMap(2025);

        File outDir = new File("target/diagnostic-2025");
        outDir.mkdirs();

        ImageIO.write(procBiomes, "PNG", new File(outDir, "procedural_2025_biomes.png"));
        ImageIO.write(procTemp, "PNG", new File(outDir, "procedural_2025_temperature.png"));
        ImageIO.write(procPrecip, "PNG", new File(outDir, "procedural_2025_precipitation.png"));
        ImageIO.write(procSeas, "PNG", new File(outDir, "procedural_2025_seasonality.png"));

        // 2. Load Reference Earth Biomes if available
        File refBiomesFile = new File("data/maps/reference_earth_biomes.png");
        if (refBiomesFile.exists()) {
            BufferedImage refBiomes = ImageIO.read(refBiomesFile);
            System.out.println("Reference Earth Biomes loaded: " + refBiomes.getWidth() + "x" + refBiomes.getHeight());

            // Compute biome distribution stats
            int matchCount = 0;
            int landCount = 0;
            int w = procBiomes.getWidth();
            int h = procBiomes.getHeight();

            for (int y = 0; y < h; y++) {
                int refY = (int) Math.min(((y + 0.5) / h) * refBiomes.getHeight(), refBiomes.getHeight() - 1);
                for (int x = 0; x < w; x++) {
                    int refX = (int) Math.min(((x + 0.5) / w) * refBiomes.getWidth(), refBiomes.getWidth() - 1);

                    int pRgb = procBiomes.getRGB(x, y) & 0x00FFFFFF;
                    int rRgb = refBiomes.getRGB(refX, refY) & 0x00FFFFFF;

                    boolean isLandProc = (pRgb != 0x001E3A8A && pRgb != 0x00172554); // not ocean
                    if (isLandProc) {
                        landCount++;
                        if (pRgb == rRgb) {
                            matchCount++;
                        }
                    }
                }
            }
            double matchPct = (landCount > 0) ? (100.0 * matchCount / landCount) : 0;
            System.out.printf("Land Biome exact color match rate against reference: %.2f%% (%d / %d cells)%n", matchPct, matchCount, landCount);
        }

        System.out.println("Diagnostic maps successfully rendered into target/diagnostic-2025/");
    }
}
