package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class InspectMapsTest {
    @Test
    public void inspectMapDimensions() {
        String[] years = {"-100000", "-50000", "-25000", "-20000"};
        String[] layers = {
            "elevation", "biomes", "temperature", "precipitation", "seasonality",
            "density", "isogloss", "sovereignty", "kinship", "rituals",
            "technology", "institutional", "ecological", "pathogen", "tradenetwork"
        };
        for (String yr : years) {
            System.out.println("=== YEAR " + yr + " ===");
            File dir = new File("data/maps/ether/earth/" + yr);
            if (!dir.exists()) {
                System.out.println("Directory missing: " + dir);
                continue;
            }
            for (String lyr : layers) {
                File f = new File(dir, "earth_" + yr + "_" + lyr + ".png");
                if (f.exists()) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        System.out.printf("  %-15s : %4dx%-4d (bytes: %d)%n", lyr, img.getWidth(), img.getHeight(), f.length());
                    } catch (Exception e) {
                        System.out.println("  " + lyr + " : ERROR reading: " + e.getMessage());
                    }
                } else {
                    System.out.println("  " + lyr + " : MISSING");
                }
            }
        }
    }
}
