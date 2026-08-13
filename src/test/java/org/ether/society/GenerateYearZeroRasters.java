package org.ether.society;

import org.ether.society.data.SvgMapIngestor;

import javax.imageio.ImageIO;
import java.io.File;

public class GenerateYearZeroRasters {
    public static void main(String[] args) {
        try {
            System.out.println("Ingesting Roman Empire (Year 0) cartography...");
            SvgMapIngestor.SvgIngestionResult res = SvgMapIngestor.ingestForScenario("ROMAN_EMPIRE");
            if (res == null) {
                System.err.println("Failed to ingest Roman Empire SVG map!");
                return;
            }

            File outDir = new File("C:/Users/silve/.gemini/antigravity/brain/46569aac-ff25-40ed-9e0c-d2bcfaaa6ad7");
            if (!outDir.exists()) outDir.mkdirs();

            ImageIO.write(res.densityImage, "png", new File(outDir, "roman_empire_density.png"));
            ImageIO.write(res.sovereigntyImage, "png", new File(outDir, "roman_empire_sovereignty.png"));
            ImageIO.write(res.isoglossImage, "png", new File(outDir, "roman_empire_isogloss.png"));
            ImageIO.write(res.kinshipImage, "png", new File(outDir, "roman_empire_kinship.png"));
            ImageIO.write(res.ritualsImage, "png", new File(outDir, "roman_empire_rituals.png"));

            System.out.println("Successfully generated 5 Tab 3 rasters for Year 0 (Roman Empire) in artifact folder!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
