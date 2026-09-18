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
        File dir = new File("src/main/resources/maps");
        assertTrue(dir.exists() && dir.isDirectory(), "Maps resource folder must exist");

        for (String planet : PLANETS) {
            for (String res : RESOURCE_TYPES) {
                File file = new File(dir, planet + "_" + res + ".png");
                if (file.exists()) {
                    verifyGrayscaleAndBlackBackground(file);
                }
            }
            for (String clim : CLIMATE_TYPES) {
                File file = new File(dir, planet + "_" + clim + ".png");
                if (file.exists()) {
                    verifyGrayscale(file);
                }
            }
        }
    }

    private void verifyGrayscaleAndBlackBackground(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img, "Image must load: " + file.getName());

        int nonBlackCount = 0;
        int zeroCount = 0;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 1. Must be pure grayscale: R == G == B
                assertEquals(r, g, "Red and Green must match in " + file.getName() + " at (" + x + "," + y + ")");
                assertEquals(g, b, "Green and Blue must match in " + file.getName() + " at (" + x + "," + y + ")");

                if (r == 0) {
                    zeroCount++;
                } else {
                    nonBlackCount++;
                }
            }
        }

        // For non-universal resources, background should be black (value 0)
        assertTrue(zeroCount > 0, "Resource map " + file.getName() + " must have a black (0) background");
    }

    private void verifyGrayscale(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        assertNotNull(img, "Image must load: " + file.getName());

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                assertEquals(r, g, "Red and Green must match in " + file.getName() + " at (" + x + "," + y + ")");
                assertEquals(g, b, "Green and Blue must match in " + file.getName() + " at (" + x + "," + y + ")");
            }
        }
    }

    @Test
    public void testMarsPrecipitationBackgroundIsBlack() throws IOException {
        File file = new File("data/maps/ether/mars/2026/mars_2026_precipitation.png");
        if (!file.exists()) {
            file = new File("src/main/resources/maps/mars_precipitation.png");
        }
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
        if (!file.exists()) {
            file = new File("src/main/resources/maps/mars_seasonality.png");
        }
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
