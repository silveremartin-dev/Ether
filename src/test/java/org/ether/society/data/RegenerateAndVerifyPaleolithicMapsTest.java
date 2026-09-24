package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegenerateAndVerifyPaleolithicMapsTest {

    @Test
    @DisplayName("Regenerate and verify 2048x1024 cartographic maps for -100k, -50k, -25k, -20k")
    public void testRegeneratePaleolithicMaps() throws Exception {
        List<Scenario> scenarios = Scenario.getBuiltInScenarios();

        long[] targetYears = {-100000L, -50000L, -25000L, -20000L};

        for (long yr : targetYears) {
            Scenario sc = scenarios.stream()
                    .filter(s -> s.getStartDateYear() == yr)
                    .findFirst()
                    .orElse(null);
            assertNotNull(sc, "Scenario for year " + yr + " must exist");

            System.out.println("Force regenerating maps for scenario: " + sc.getName() + " (Year " + yr + ")");
            HistoricalMapGenerator.forceGenerateScenarioHistoricalMaps(sc);

            File dir = new File("data/maps/ether/earth/" + yr);
            assertTrue(dir.exists() && dir.isDirectory(), "Directory must exist: " + dir);

            String[] allLayers = {
                "elevation", "biomes", "temperature", "precipitation", "seasonality",
                "density", "isogloss", "sovereignty", "kinship", "rituals",
                "technology", "institutional", "ecological", "pathogen", "tradenetwork",
                "coal", "oil", "gas", "uranium", "helium3",
                "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };

            assertEquals(25, allLayers.length);

            for (String lyr : allLayers) {
                File f = new File(dir, "earth_" + yr + "_" + lyr + ".png");
                assertTrue(f.exists(), "Layer " + lyr + " file must exist: " + f);
                assertTrue(f.length() > 500, "Layer " + lyr + " file must not be empty: " + f);

                BufferedImage img = ImageIO.read(f);
                assertNotNull(img, "Image must be readable: " + f);
                assertEquals(2048, img.getWidth(), "Layer " + lyr + " width must be 2048 for year " + yr);
                assertEquals(1024, img.getHeight(), "Layer " + lyr + " height must be 1024 for year " + yr);
                System.out.printf("  [OK] %-16s : %4dx%-4d (bytes: %d)%n", lyr, img.getWidth(), img.getHeight(), f.length());
            }
        }
    }
}
