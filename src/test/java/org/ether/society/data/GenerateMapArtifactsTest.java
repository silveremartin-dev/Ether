package org.ether.society.data;

import org.junit.jupiter.api.Test;
import org.ether.society.model.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

public class GenerateMapArtifactsTest {

    @Test
    public void generateArtifactImages() throws Exception {
        String outputDir = "C:\\Users\\silve\\.gemini\\antigravity\\brain\\46569aac-ff25-40ed-9e0c-d2bcfaaa6ad7";
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        // 1. Empire Romain
        Scenario roman = new Scenario();
        roman.setName("Empire Romain");
        roman.setPopulationDensityType("ROMAN_EMPIRE");
        HistoricalMapGenerator.populateScenarioHistoricalMaps(roman);
        saveBase64ToPng(roman.getCustomDensityBase64(), new File(dir, "roman_empire_density.png"));
        saveBase64ToPng(roman.getCustomSovereigntyBase64(), new File(dir, "roman_empire_sovereignty.png"));
        saveBase64ToPng(roman.getCustomIsoglossBase64(), new File(dir, "roman_empire_isogloss.png"));

        // 2. Croissant Fertile
        Scenario fertile = new Scenario();
        fertile.setName("Croissant Fertile");
        fertile.setPopulationDensityType("FERTILE_CRESCENT");
        HistoricalMapGenerator.populateScenarioHistoricalMaps(fertile);
        saveBase64ToPng(fertile.getCustomDensityBase64(), new File(dir, "fertile_crescent_density.png"));
        saveBase64ToPng(fertile.getCustomRitualsBase64(), new File(dir, "fertile_crescent_rituals.png"));
        saveBase64ToPng(fertile.getCustomIsoglossBase64(), new File(dir, "fertile_crescent_isogloss.png"));

        // 3. Sortie d'Afrique
        Scenario africa = new Scenario();
        africa.setName("Sortie d'Afrique");
        africa.setPopulationDensityType("ONE_CONTINENT");
        HistoricalMapGenerator.populateScenarioHistoricalMaps(africa);
        saveBase64ToPng(africa.getCustomDensityBase64(), new File(dir, "out_of_africa_density.png"));
        saveBase64ToPng(africa.getCustomKinshipBase64(), new File(dir, "out_of_africa_kinship.png"));
        saveBase64ToPng(africa.getCustomIsoglossBase64(), new File(dir, "out_of_africa_isogloss.png"));

        System.out.println("GENERATED MAP ARTIFACTS SUCCESSFULLY");
    }

    private void saveBase64ToPng(String base64, File outputFile) throws Exception {
        if (base64 == null) return;
        byte[] bytes = Base64.getDecoder().decode(base64.trim());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img != null) {
            ImageIO.write(img, "png", outputFile);
            System.out.println("Saved " + outputFile.getAbsolutePath());
        }
    }
}
