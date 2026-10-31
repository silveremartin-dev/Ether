package org.ether.society.data;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Global Raster Integrity & Artifact Audit Suite.
 * Scans all epoch directories in data/ether/earth/ to detect:
 * 1. Missing standard cartographic layers
 * 2. Unreadable or 0-byte corrupted image files
 * 3. Non-standard dimensions (deviations from 2048x1024)
 * 4. Completely blank/black uniform layers (min == max pixel value)
 * 5. Invariant violations in deep-time (-3,000,000 to -10,000 BP)
 */
// Standard unit/integration test
public class GlobalRasterIntegrityAuditTest {

    private static final String[] CRITICAL_LAYERS = {
            "elevation", "biomes", "temperature", "precipitation", "density"
    };

    public record RasterAnomaly(String epoch, String layer, String reason) {}

    @Test
    public void auditAllEarthRasters() {
        File earthBaseDir = new File("data/ether/earth");
        assertTrue(earthBaseDir.exists() && earthBaseDir.isDirectory(), "data/ether/earth must exist");

        File[] epochDirs = earthBaseDir.listFiles(File::isDirectory);
        assertNotNull(epochDirs, "Epoch directories must not be null");

        System.out.println("================================================================================");
        System.out.println(" 🗺️ GLOBAL RASTER INTEGRITY & ARTIFACT AUDIT (" + epochDirs.length + " EPOCHS FOUND)");
        System.out.println("================================================================================");

        List<RasterAnomaly> anomalies = new ArrayList<>();
        int totalFilesChecked = 0;
        int validFiles = 0;

        for (File dir : epochDirs) {
            String epoch = dir.getName();
            long epochYear;
            try {
                epochYear = Long.parseLong(epoch);
            } catch (NumberFormatException e) {
                continue;
            }

            for (String layer : CRITICAL_LAYERS) {
                File file = new File(dir, "earth_" + epoch + "_" + layer + ".png");
                totalFilesChecked++;

                if (!file.exists()) {
                    anomalies.add(new RasterAnomaly(epoch, layer, "MISSING FILE"));
                    continue;
                }

                if (file.length() == 0) {
                    anomalies.add(new RasterAnomaly(epoch, layer, "ZERO-BYTE FILE"));
                    continue;
                }

                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img == null) {
                        anomalies.add(new RasterAnomaly(epoch, layer, "IMAGE CORRUPTED / UNREADABLE"));
                        continue;
                    }

                    int w = img.getWidth();
                    int h = img.getHeight();
                    if (w != 2048 || h != 1024) {
                        anomalies.add(new RasterAnomaly(epoch, layer, String.format("NON-STANDARD DIMENSIONS: %dx%d (expected 2048x1024)", w, h)));
                    }

                    // Check for completely blank uniform layers on non-density layers
                    if (!"density".equals(layer)) {
                        int rgb0 = img.getRGB(0, 0);
                        boolean allSame = true;
                        // Sample 32x16 points
                        for (int y = 0; y < h; y += 64) {
                            for (int x = 0; x < w; x += 64) {
                                if (img.getRGB(x, y) != rgb0) {
                                    allSame = false;
                                    break;
                                }
                            }
                            if (!allSame) break;
                        }
                        if (allSame) {
                            anomalies.add(new RasterAnomaly(epoch, layer, "BLANK / UNIFORM RASTER (no spatial variation)"));
                        }
                    }

                    // Check deep-time invariant on density layer: before -100,000 BP density must be extremely sparse
                    if ("density".equals(layer) && epochYear <= -100000) {
                        int rgbMax = 0;
                        for (int y = 0; y < h; y += 64) {
                            for (int x = 0; x < w; x += 64) {
                                int val = (img.getRGB(x, y) >> 16) & 0xFF;
                                if (val > rgbMax) rgbMax = val;
                            }
                        }
                        if (rgbMax > 200) {
                            anomalies.add(new RasterAnomaly(epoch, layer, "DEEP-TIME ANOMALY: Dense modern-scale population detected at Year " + epochYear));
                        }
                    }

                    validFiles++;
                } catch (Exception e) {
                    anomalies.add(new RasterAnomaly(epoch, layer, "READ EXCEPTION: " + e.getMessage()));
                }
            }
        }

        System.out.printf("  Total critical layer rasters checked: %,d%n", totalFilesChecked);
        System.out.printf("  Valid conformant rasters: %,d%n", validFiles);
        System.out.printf("  Anomalies detected: %d%n%n", anomalies.size());

        if (!anomalies.isEmpty()) {
            System.out.println("  ❌ DETECTED RASTER DEFECTS & ANOMALIES:");
            for (RasterAnomaly a : anomalies) {
                System.out.printf("   - Epoch %-8s | Layer: %-15s | Defect: %s%n", a.epoch(), a.layer(), a.reason());
            }
        } else {
            System.out.println("  ✅ All critical raster layers are conformant across all epochs!");
        }
        System.out.println("================================================================================");
    }
}
