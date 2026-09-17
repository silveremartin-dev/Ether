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
import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Empirical Data Sources Comprehensive Validation Test Suite.
 * Validates HYDE 3.4, Natural Earth, Archaeoglobe, Seshat, and HGIS datasets under strict Zero-Fallback policy.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class EmpiricalDataSourcesValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(EmpiricalDataSourcesValidationTest.class);

    private static final File ARTIFACT_DIR = new File("target/test-output");

    static {
        ARTIFACT_DIR.mkdirs();
    }

    @Test
    @DisplayName("1. Validate HYDE 3.4 5-Arc-Minute Empirical Grid Ingestion (Zero Fallback)")
    public void testHyde34Ingestion() throws Exception {
        BufferedImage img1000BC = Hyde34GridReader.loadForYear(-1000);
        Assertions.assertNotNull(img1000BC, "HYDE 3.4 -1000 BC raster image must be loaded");
        Assertions.assertEquals(1024, img1000BC.getWidth());
        Assertions.assertEquals(512, img1000BC.getHeight());

        File outFile1000BC = new File(ARTIFACT_DIR, "val_hyde_1000bc.png");
        ImageIO.write(img1000BC, "PNG", outFile1000BC);
        logger.info("HYDE 3.4 -1000 BC Validation Image saved: {} ({} bytes)", outFile1000BC.getAbsolutePath(), outFile1000BC.length());

        BufferedImage img3000BC = Hyde34GridReader.loadForYear(-3000);
        Assertions.assertNotNull(img3000BC, "HYDE 3.4 -3000 BC raster image must be loaded");
        File outFile3000BC = new File(ARTIFACT_DIR, "val_hyde_3000bc.png");
        ImageIO.write(img3000BC, "PNG", outFile3000BC);
        logger.info("HYDE 3.4 -3000 BC Validation Image saved: {} ({} bytes)", outFile3000BC.getAbsolutePath(), outFile3000BC.length());
    }

    @Test
    @DisplayName("2. Validate Natural Earth 1:10m High-Precision Vector Rasterizer")
    public void testNaturalEarthVectorIngestion() throws Exception {
        List<NaturalEarthVectorIngestor.VectorFeature> features = new ArrayList<>();

        // Create sample Europe coastline path
        Path2D.Double europePath = new Path2D.Double();
        europePath.moveTo(400.0, 150.0);
        europePath.lineTo(550.0, 150.0);
        europePath.lineTo(580.0, 250.0);
        europePath.lineTo(420.0, 250.0);
        europePath.closePath();

        features.add(new NaturalEarthVectorIngestor.VectorFeature("NE_EUR", "Europe", "SOVEREIGNTY", new Color(0x3B, 0x82, 0xF6), europePath));

        BufferedImage vectorImg = NaturalEarthVectorIngestor.rasterizeVectorFeatures(features, "ALL");
        Assertions.assertNotNull(vectorImg);

        File outFile = new File(ARTIFACT_DIR, "val_natural_earth_vector.png");
        ImageIO.write(vectorImg, "PNG", outFile);
        logger.info("Natural Earth Vector Validation Image saved: {} ({} bytes)", outFile.getAbsolutePath(), outFile.length());
    }

    @Test
    @DisplayName("3. Validate Archaeoglobe Archaeological Recalibration Module")
    public void testArchaeoglobeValidation() {
        double rawDensity = 10.0;
        double validateEuroBC = ArchaeoglobeValidator.validateDensityWithArchaeology(rawDensity, 10.0, 45.0, -1000);
        Assertions.assertEquals(11.5, validateEuroBC, 0.001, "Archaeoglobe recalibration for Bronze Age Europe must apply 1.15 multiplier");

        double validateNeolithic = ArchaeoglobeValidator.validateDensityWithArchaeology(rawDensity, 10.0, 45.0, -6000);
        Assertions.assertEquals(8.5, validateNeolithic, 0.001, "Archaeoglobe recalibration for Neolithic Europe must apply 0.85 multiplier");
        logger.info("Archaeoglobe Recalibration Module Validated Successfully.");
    }

    @Test
    @DisplayName("4. Validate Seshat Global History Databank Cliodynamic Benchmarks")
    public void testSeshatDatatIntegrator() {
        SeshatDataIntegrator.SeshatPolityRecord rome = SeshatDataIntegrator.getSeshatRecord("ROMAN_EMPIRE");
        Assertions.assertNotNull(rome, "Roman Empire Seshat record must exist");
        Assertions.assertEquals("Imperium Romanum", rome.polityName);
        Assertions.assertEquals(0.88, rome.institutionalComplexity, 0.001);

        SeshatDataIntegrator.SeshatPolityRecord france = SeshatDataIntegrator.getSeshatRecord("FRANCE_1800");
        Assertions.assertNotNull(france, "France 1800 Seshat record must exist");
        Assertions.assertEquals(0.95, france.institutionalComplexity, 0.001);
        logger.info("Seshat Cliodynamic Databank Integrator Validated Successfully.");
    }

    @Test
    @DisplayName("5. Enforce Zero Fallback Policy - Missing Year Exception Check")
    public void testZeroFallbackException() {
        // Year -99999 has no empirical HYDE 3.4 file -> system must throw IllegalStateException under Zero Fallback Policy
        Assertions.assertThrows(IllegalStateException.class, () -> {
            HistoricalMapGenerator.generateCleanDensityMapForYear("RIVER_VALLEYS", null, -99999);
        }, "Zero Fallback Policy must throw IllegalStateException when empirical data is missing");
        logger.info("Zero Fallback Policy Constraint Validated Successfully.");
    }
}

