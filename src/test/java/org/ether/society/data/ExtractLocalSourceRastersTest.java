/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Validates batch extraction of local ZIP archives from data/maps/hyde34/source/
 * and generates high-fidelity cartographic rasters for scenario timelines.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class ExtractLocalSourceRastersTest {
    private static final Logger logger = LoggerFactory.getLogger(ExtractLocalSourceRastersTest.class);

    private static final File ARTIFACT_DIR = new File("C:/Users/silve/.gemini/antigravity/brain/5d6049c2-b4bf-48cc-b896-c6a673043b13");

    @Test
    @DisplayName("Extract & Render Major Historical Raster Timelines from Local Source Archives")
    public void testExtractMajorTimelinesFromLocalSource() throws Exception {
        long[] years = new long[] { -10000, -5000, -3000, -1000, 0, 1000, 1500, 1800, 1900, 1950, 2000, 2024 };

        for (long year : years) {
            String tag = DataDownloaderService.getHydeYearTag(year);
            logger.info("Processing empirical timeline step for year {} ({}) from local source...", year, tag);

            BufferedImage img = Hyde34GridReader.loadForYear(year);
            Assertions.assertNotNull(img, "Empirical raster image must be extracted and loaded for year " + year);
            Assertions.assertEquals(Hyde34GridReader.ETHER_WIDTH, img.getWidth());
            Assertions.assertEquals(Hyde34GridReader.ETHER_HEIGHT, img.getHeight());

            File outDir = new File("target/test-output/hyde-rasters");
            outDir.mkdirs();
            File outFile = new File(outDir, "raster_hyde_" + tag.toLowerCase() + ".png");
            ImageIO.write(img, "PNG", outFile);
            logger.info("Successfully rendered empirical raster for year {} ({}): {} ({} bytes)",
                    year, tag, outFile.getAbsolutePath(), outFile.length());
        }
    }
}

