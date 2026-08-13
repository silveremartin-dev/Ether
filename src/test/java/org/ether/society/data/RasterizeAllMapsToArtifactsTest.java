package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility test to rasterize all embedded SVG maps into high-resolution PNG images
 * in the active conversation artifact directory for visual preview and validation.
 */
public class RasterizeAllMapsToArtifactsTest {

    private static final String ARTIFACT_DIR = "C:/Users/silve/.gemini/antigravity/brain/5b4110ad-8167-4c54-9060-9a0fc0549613/";

    @Test
    public void testRasterizeAllSvgMapsToArtifacts() throws Exception {
        Map<String, String> maps = new LinkedHashMap<>();
        maps.put("Roman Empire (0 AD)", "/maps/svg/roman_empire_0.svg");
        maps.put("Mali Empire (1324 AD)", "/maps/svg/mali_empire_1324.svg");
        maps.put("Song Dynasty (1000 AD)", "/maps/svg/song_dynasty_1000.svg");
        maps.put("Fertile Crescent (8000 BC)", "/maps/svg/fertile_crescent_8000bc.svg");
        maps.put("Pre-Columbian Americas (1491 AD)", "/maps/svg/americas_1491.svg");
        maps.put("Japan Sakoku (1639 AD)", "/maps/svg/japan_sakoku_1639.svg");
        maps.put("Anthropocene Era (2000 AD)", "/maps/svg/anthropocene_2000.svg");
        maps.put("Europe (500 AD)", "/maps/svg/europe_500ad.svg");
        maps.put("Europe (1000 AD)", "/maps/svg/europe_1000ad.svg");
        maps.put("Europe (1500 AD)", "/maps/svg/europe_1500ad.svg");
        maps.put("Asia (1200 AD)", "/maps/svg/asia_1200ad.svg");
        maps.put("Africa (1500 AD)", "/maps/svg/africa_1500ad.svg");

        File outputDir = new File(ARTIFACT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (Map.Entry<String, String> entry : maps.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue();
            String filePrefix = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));

            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.err.println("Warning: SVG file not found: " + path);
                continue;
            }

            SvgMapIngestor.SvgIngestionResult result = SvgMapIngestor.ingestSvg(is);
            is.close();

            assertNotNull(result, "Result should not be null for " + name);

            // Write all rendered channel images to disk for visual verification
            if (result.densityImage != null) {
                ImageIO.write(result.densityImage, "png", new File(ARTIFACT_DIR + filePrefix + "_density.png"));
            }
            if (result.sovereigntyImage != null) {
                ImageIO.write(result.sovereigntyImage, "png", new File(ARTIFACT_DIR + filePrefix + "_sovereignty.png"));
            }
            if (result.isoglossImage != null) {
                ImageIO.write(result.isoglossImage, "png", new File(ARTIFACT_DIR + filePrefix + "_isogloss.png"));
            }
            if (result.kinshipImage != null) {
                ImageIO.write(result.kinshipImage, "png", new File(ARTIFACT_DIR + filePrefix + "_kinship.png"));
            }
            if (result.ritualsImage != null) {
                ImageIO.write(result.ritualsImage, "png", new File(ARTIFACT_DIR + filePrefix + "_rituals.png"));
            }
        }

        assertTrue(outputDir.exists());
    }
}
