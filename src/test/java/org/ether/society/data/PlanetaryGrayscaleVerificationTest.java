package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class PlanetaryGrayscaleVerificationTest {

    private static final String[] PLANETS = { "earth", "mars", "moon", "mercury", "venus" };
    private static final String[] RESOURCE_TYPES = {
        "coal", "oil", "gas", "uranium", "helium3", "iron_copper", "precious_metals", "geothermal", "aquifers"
    };
    private static final String[] CLIMATE_TYPES = {
        "precipitation", "seasonality", "temperature"
    };

    @Test
    public void testAllPlanetaryMapsAreGrayscaleOnBlackBackground() throws IOException {
        File baseDir = new File("data/maps/ether");
        assertTrue(baseDir.exists() && baseDir.isDirectory(), "Maps base folder must exist: " + baseDir.getPath());

        for (String planet : PLANETS) {
            File planetDir = new File(baseDir, planet);
            if (!planetDir.exists() || !planetDir.isDirectory()) continue;
            File[] yearDirs = planetDir.listFiles(File::isDirectory);
            if (yearDirs == null) continue;

            for (File yearDir : yearDirs) {
                String yearName = yearDir.getName();
                for (String res : RESOURCE_TYPES) {
                    File file = new File(yearDir, planet + "_" + yearName + "_" + res + ".png");
                    if (file.exists()) {
                        verifyGrayscaleAndBlackBackground(file);
                    }
                }
                for (String clim : CLIMATE_TYPES) {
                    File file = new File(yearDir, planet + "_" + yearName + "_" + clim + ".png");
                    if (file.exists()) {
                        verifyGrayscale(file);
                    }
                }
            }
        }
    }

    private void verifyGrayscaleAndBlackBackground(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img, "Image must load: " + file.getName());

        int w = img.getWidth(), h = img.getHeight();
        int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);
        int zeroCount = 0;

        for (int i = 0; i < rgb.length; i++) {
            int val = rgb[i];
            int r = (val >> 16) & 0xFF;
            int g = (val >> 8) & 0xFF;
            int b = val & 0xFF;

            if (r != g || g != b) {
                fail(String.format("Non-grayscale pixel in %s at index %d: R=%d G=%d B=%d", file.getName(), i, r, g, b));
            }
            if (r == 0) zeroCount++;
        }

        assertTrue(zeroCount > 0, "Resource map " + file.getName() + " must have a black (0) background");
    }

    private void verifyGrayscale(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img, "Image must load: " + file.getName());

        int w = img.getWidth(), h = img.getHeight();
        int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);

        for (int i = 0; i < rgb.length; i++) {
            int val = rgb[i];
            int r = (val >> 16) & 0xFF;
            int g = (val >> 8) & 0xFF;
            int b = val & 0xFF;

            if (r != g || g != b) {
                fail(String.format("Non-grayscale pixel in %s at index %d: R=%d G=%d B=%d", file.getName(), i, r, g, b));
            }
        }
    }

    @Test
    public void testMarsPrecipitationBackgroundIsBlack() throws IOException {
        File file = new File("data/maps/ether/mars/2026/mars_2026_precipitation.png");
        if (!file.exists()) return;
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img);

        int minVal = 255;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int r = (img.getRGB(x, y) >> 16) & 0xFF;
                if (r < minVal) minVal = r;
            }
        }
        assertEquals(0, minVal, "Mars precipitation minimum (background) must be 0 (black)");
    }

    @Test
    public void testMarsSeasonalityEquatorContinuity() throws IOException {
        File file = new File("data/maps/ether/mars/2026/mars_2026_seasonality.png");
        if (!file.exists()) return;
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img);

        int midY = img.getHeight() / 2;
        for (int x = 0; x < img.getWidth(); x++) {
            int northVal = (img.getRGB(x, midY - 1) >> 16) & 0xFF;
            int southVal = (img.getRGB(x, midY) >> 16) & 0xFF;
            int diff = Math.abs(northVal - southVal);
            assertTrue(diff <= 2, "Discontinuity detected across Mars equator at x=" + x + ": north=" + northVal + ", south=" + southVal + ", diff=" + diff);
        }
    }
}
