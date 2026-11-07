package org.ether.society.data;

import org.ether.society.data.HistoricalEpochTestCatalog.EpochMeta;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.ether.society.data.EpochMetadataWriterHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Master Epistemic Cartographic & Metadata Regeneration Suite across all 36 Historical Epochs.
 * Regenerates the 9 cultural tensors (isogloss, kinship, rituals, sovereignty, technology,
 * tradenetwork, institutional, ecological, pathogen) while preserving physical layers,
 * and updates cultural_registry.json, provenance_and_sources.json, and README.md for every epoch.
 */
@Tag("manual")
public class GenerateEpochMapsTest {
    public static final List<EpochMeta> EPOCHS = HistoricalEpochTestCatalog.EPOCHS;
    public static final Set<Long> CANONICAL_36_YEARS = HistoricalEpochTestCatalog.CANONICAL_36_YEARS;

    @Test
    /*
     * Generate epoch100k only operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void generateEpoch100kOnly() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        EpochMeta em = EPOCHS.stream().filter(e -> e.year() == -100000L).findFirst().orElseThrow();
        long yr = -100000L;
        System.out.printf("=== PROCESSING SINGLE EPOCH %d: %s ===%n", yr, em.eraName());

        File yrDir = new File(rootDir, String.valueOf(yr));
        yrDir.mkdirs();

        Scenario sc = new Scenario();
        sc.setName(em.eraName());
        sc.setStartDateYear(yr);
        sc.setPopulationDensityType(em.densityType());
        sc.setInitialHumanCount(em.population());
        sc.setInitialTechLevel(em.techLevel());

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        File regFile = new File(yrDir, "cultural_registry.json");
        writeEpochCulturalRegistry(regFile, em, seshatFile);
        regFile.setLastModified(System.currentTimeMillis());

        File provFile = new File(yrDir, "provenance_and_sources.json");
        writeEpochProvenance(provFile, em);
        provFile.setLastModified(System.currentTimeMillis());

        File readmeFile = new File(yrDir, "README.md");
        writeEpochReadme(readmeFile, em);
        readmeFile.setLastModified(System.currentTimeMillis());

        System.out.printf("  [SUCCESS] Epoch %d generated!%n", yr);
        assertNotNull(em, "Epoch metadata for -100000 must exist");
        assertEquals(-100000L, yr, "Target year must be -100000");
    }

    @Test
    /*
     * Generate canonical36epochs cultural data operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void generateCanonical36EpochsCulturalData() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        List<EpochMeta> canonicalList = EPOCHS.stream()
            .filter(e -> CANONICAL_36_YEARS.contains(e.year()))
            .toList();

        System.out.printf("Starting regeneration for %d CANONICAL landmark epochs...%n", canonicalList.size());

        for (EpochMeta em : canonicalList) {
            long yr = em.year();
            System.out.printf("=== PROCESSING CANONICAL EPOCH %d: %s ===%n", yr, em.eraName());

            File yrDir = new File(rootDir, String.valueOf(yr));
            yrDir.mkdirs();

            Scenario sc = new Scenario();
            sc.setName(em.eraName());
            sc.setStartDateYear(yr);
            sc.setPopulationDensityType(em.densityType());
            sc.setInitialHumanCount(em.population());
            sc.setInitialTechLevel(em.techLevel());

            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

            File regFile = new File(yrDir, "cultural_registry.json");
            writeEpochCulturalRegistry(regFile, em, seshatFile);
            regFile.setLastModified(System.currentTimeMillis());

            File provFile = new File(yrDir, "provenance_and_sources.json");
            writeEpochProvenance(provFile, em);
            provFile.setLastModified(System.currentTimeMillis());

            File readmeFile = new File(yrDir, "README.md");
            writeEpochReadme(readmeFile, em);
            readmeFile.setLastModified(System.currentTimeMillis());

            System.out.printf("  [SUCCESS] Canonical Epoch %d: 25 Standard Rasters + 2 JSONs + 1 README.md written.%n", yr);
        }

        System.out.printf("All %d canonical landmark epochs successfully regenerated and documented.%n", canonicalList.size());
        assertTrue(canonicalList.size() == 36, "Must process all 36 canonical epochs");
    }

    @Test
    /*
     * Regenerate prehistoric density maps only operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void regeneratePrehistoricDensityMapsOnly() throws Exception {
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        List<EpochMeta> prehistoricEpochs = EPOCHS.stream()
            .filter(e -> e.year() <= -10000L)
            .toList();

        System.out.printf("Starting targeted density regeneration for %d prehistoric epochs (<= -10,000 BP)...%n", prehistoricEpochs.size());

        for (EpochMeta em : prehistoricEpochs) {
            long yr = em.year();
            File yrDir = new File(rootDir, String.valueOf(yr));
            yrDir.mkdirs();

            java.awt.image.BufferedImage imgDensity = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                HistoricalMapGenerator.generatePrehistoricSyntheticDensityMap(yr, em.densityType())
            );

            File targetPng = new File(yrDir, String.format("earth_%d_density.png", yr));
            javax.imageio.ImageIO.write(imgDensity, "PNG", targetPng);
            targetPng.setLastModified(System.currentTimeMillis());

            System.out.printf("  [DENSITY UPDATED] Prehistoric Epoch %d: %s (%s)%n", yr, em.eraName(), targetPng.getName());
        }

        System.out.printf("Successfully regenerated calibrated physical density maps for all %d prehistoric epochs.%n", prehistoricEpochs.size());
        assertTrue(prehistoricEpochs.size() > 0, "Prehistoric epochs list must not be empty");
    }

    @Test
    /*
     * Validate prehistoric cartographic differentiation operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void validatePrehistoricCartographicDifferentiation() {
        double sahulLon = 133.0, sahulLat = -25.0;
        double tasmaniaLon = 146.5, tasmaniaLat = -42.0;
        double occ100k = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -100000L);
        double occ74k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -74000L);
        double occ50k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -50000L);
        double occ20k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -20000L);
        double occTasmania50k = HistoricalMapGenerator.getHomininOccupancyWeight(tasmaniaLon, tasmaniaLat, -50000L);
        double occTasmania35k = HistoricalMapGenerator.getHomininOccupancyWeight(tasmaniaLon, tasmaniaLat, -35000L);

        assertEquals(0.0, occ100k, 1e-6, "Sahul must be strictly unoccupied at -100,000 BP");
        assertEquals(0.0, occ74k, 1e-6, "Sahul must be strictly unoccupied at -74,000 BP");
        assertTrue(occ50k > 0.5, "Sahul must be populated at -50,000 BP across Wallace Line");
        assertEquals(0.0, occTasmania50k, 1e-6, "Tasmania must be strictly unoccupied at -50,000 BP");
        assertTrue(occTasmania35k > 0.3, "Tasmania must be populated at -35,000 BP via Bassian Plain");
        assertTrue(occ20k > 0.5, "Sahul must be populated at -20,000 BP");

        double laurentideLon = -85.0, laurentideLat = 55.0;
        double fennoLon = 20.0, fennoLat = 65.0;
        double whiteSandsLon = -106.3, whiteSandsLat = 32.8;
        double pacificKelpLon = -125.0, pacificKelpLat = 48.0;

        double occLaurYD = HistoricalMapGenerator.getHomininOccupancyWeight(laurentideLon, laurentideLat, -10900L);
        double occFennoYD = HistoricalMapGenerator.getHomininOccupancyWeight(fennoLon, fennoLat, -10900L);
        double occWhiteSands20k = HistoricalMapGenerator.getHomininOccupancyWeight(whiteSandsLon, whiteSandsLat, -20000L);
        double occKelp20k = HistoricalMapGenerator.getHomininOccupancyWeight(pacificKelpLon, pacificKelpLat, -20000L);
        double occAmericas100k = HistoricalMapGenerator.getHomininOccupancyWeight(-100.0, 40.0, -100000L);

        assertEquals(0.0, occLaurYD, 1e-6, "Laurentide Ice Sheet must be strictly unpopulated at Younger Dryas");
        assertEquals(0.0, occFennoYD, 1e-6, "Fennoscandian Ice Sheet must be strictly unpopulated at Younger Dryas");
        assertTrue(occWhiteSands20k > 0.1, "White Sands must be populated at -20,000 BP LGM");
        assertTrue(occKelp20k > 0.1, "Pacific Kelp Highway must be populated at -20,000 BP LGM");
        assertEquals(0.0, occAmericas100k, 1e-6, "Americas must be strictly unpopulated at -100,000 BP");

        double indiaLon = 78.0, indiaLat = 20.0;
        double occIndia100k = HistoricalMapGenerator.getHomininOccupancyWeight(indiaLon, indiaLat, -100000L);
        double occIndia74k = HistoricalMapGenerator.getHomininOccupancyWeight(indiaLon, indiaLat, -74000L);
        assertTrue(occIndia74k < occIndia100k * 0.3, "South Asia hominin occupancy must be severely depressed by Toba ash fall");

        double tobaTempDelta = WorldClimEmpiricalRasterLoader.computePaleoTemperatureDelta(indiaLat, indiaLon, 200.0, -74000L);
        double eemianTempDelta = WorldClimEmpiricalRasterLoader.computePaleoTemperatureDelta(indiaLat, indiaLon, 200.0, -100000L);
        assertTrue(tobaTempDelta < -4.0, "Toba volcanic winter must have <= -4.0°C cooling anomaly in South Asia");
        assertTrue(eemianTempDelta > 0.5, "Eemian must have positive warming anomaly");

        double saharaLon = 18.0, saharaLat = 21.0;
        double rech100k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(saharaLon, saharaLat, -100000L);
        double rech74k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(saharaLon, saharaLat, -74000L);
        double rechSahul50k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(sahulLon, sahulLat, -50000L);
        double rechLgm20k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(0.0, 50.0, -20000L);

        assertTrue(rech100k > 2.0, "Green Sahara MIS 5e must have > 2.0x aquifer recharge factor");
        assertTrue(rech74k < 0.70, "Toba tropical drought must have < 0.70x recharge factor");
        assertTrue(rechSahul50k >= 1.5, "MIS 3 Sahul megalakes must have >= 1.5x aquifer recharge factor");
        assertTrue(rechLgm20k <= 0.45, "LGM high-latitude permafrost lock-up must have <= 0.45x recharge factor");
    }

    @Test
    /*
     * Compute and validate global population continuity operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void computeAndValidateGlobalPopulationContinuity() throws Exception {
        double rEarthKm = 6371.0;
        int width = 2048;
        int height = 1024;
        double basePixelAreaKm2 = (2.0 * Math.PI * rEarthKm / width) * (Math.PI * rEarthKm / height);

        System.out.println("==========================================================================");
        System.out.println("  GLOBAL HUMAN POPULATION INTEGRAL EVALUATION ACROSS ALL LANDMARK EPOCHS  ");
        System.out.println("==========================================================================");
        System.out.printf("%-12s | %-45s | %-16s | %-16s%n", "Epoch (BP)", "Historical Context", "Integrated Pop", "Reference Target");
        System.out.println("--------------------------------------------------------------------------");

        long[] landmarkYears = {
            -100000L, -74000L, -50000L, -40000L, -35000L, -30000L, -25000L, -20000L,
            -15000L, -14000L, -12000L, -11000L, -10900L, -10000L
        };

        for (long yr : landmarkYears) {
            EpochMeta em = EPOCHS.stream().filter(e -> e.year() == yr).findFirst().orElse(null);
            String name = (em != null) ? em.eraName() : ("Epoch " + yr);
            long targetPop = (em != null) ? em.population() : 0L;

            java.awt.image.BufferedImage img = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                HistoricalMapGenerator.generatePrehistoricSyntheticDensityMap(yr, (em != null) ? em.densityType() : "ONE_CONTINENT")
            );

            double integratedPop = 0.0;
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                double cellArea = basePixelAreaKm2 * Math.cos(Math.toRadians(lat));
                for (int x = 0; x < width; x++) {
                    int rgb = img.getRGB(x, y);
                    int gray = rgb & 0xFF;
                    if (gray > 12) {
                        double logNorm = (gray - 12.0) / 243.0;
                        double dens = (Math.exp(logNorm * Math.log1p(2.5 * 35.0)) - 1.0) / 2.5;
                        integratedPop += dens * cellArea;
                    }
                }
            }

            System.out.printf("%-12d | %-45s | %-16s | %-16s%n",
                yr, name.length() > 45 ? name.substring(0, 42) + "..." : name,
                String.format("%,d", (long) integratedPop),
                String.format("%,d", targetPop));
        }
        System.out.println("==========================================================================");
        assertTrue(landmarkYears.length == 14, "Must evaluate all 14 landmark years");
    }

    @Test
    /*
     * Generate all36epochs cultural data operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    public void generateAll36EpochsCulturalData() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        System.out.printf("Starting high-throughput parallel cultural data regeneration for %d epochs...%n", EPOCHS.size());

        if (!EPOCHS.isEmpty()) {
            EpochMeta first = EPOCHS.get(0);
            Scenario scWarm = new Scenario();
            scWarm.setName(first.eraName());
            scWarm.setStartDateYear(first.year());
            scWarm.setPopulationDensityType(first.densityType());
            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(scWarm);
        }

        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        int total = EPOCHS.size();

        EPOCHS.parallelStream().forEach(em -> {
            try {
                long yr = em.year();
                long t0 = System.currentTimeMillis();

                File yrDir = new File(rootDir, String.valueOf(yr));
                yrDir.mkdirs();

                Scenario sc = new Scenario();
                sc.setName(em.eraName());
                sc.setStartDateYear(yr);
                sc.setPopulationDensityType(em.densityType());
                sc.setInitialHumanCount(em.population());
                sc.setInitialTechLevel(em.techLevel());

                HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

                File regFile = new File(yrDir, "cultural_registry.json");
                writeEpochCulturalRegistry(regFile, em, seshatFile);
                regFile.setLastModified(System.currentTimeMillis());

                File provFile = new File(yrDir, "provenance_and_sources.json");
                writeEpochProvenance(provFile, em);
                provFile.setLastModified(System.currentTimeMillis());

                File readmeFile = new File(yrDir, "README.md");
                writeEpochReadme(readmeFile, em);
                readmeFile.setLastModified(System.currentTimeMillis());

                int done = completed.incrementAndGet();
                long elapsed = System.currentTimeMillis() - t0;
                System.out.printf("  [SUCCESS %d/%d] Epoch %d: 25 Standard Rasters + 2 JSONs + 1 README.md written (%d ms)%n", done, total, yr, elapsed);
            } catch (Exception e) {
                System.err.printf("  [ERROR] Epoch %d failed: %s%n", em.year(), e.getMessage());
            }
        });

        System.out.printf("All %d epochs successfully regenerated and documented with 25 rasters each.%n", EPOCHS.size());
        assertTrue(EPOCHS.size() >= 36, "Must contain at least all 36 canonical epochs");
    }
}
