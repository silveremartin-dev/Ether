package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateAllScenarioMapsTest {
    private static final Logger logger = LoggerFactory.getLogger(GenerateAllScenarioMapsTest.class);

    private static final String CACHE_DIR = "data/cache";
    private static final String ARTIFACTS_DIR = CACHE_DIR;

    @Test
    @DisplayName("Batch Generate Missing Empirical Maps for All Historical Scenarios")
    public void testGenerateAllScenarioMaps() throws IOException {
        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        Map<String, Long> historicalScenarios = new LinkedHashMap<>();
        historicalScenarios.put("ether_scenario_out_of_africa_sapiens_dispersal_100000bc", -100000L);
        historicalScenarios.put("ether_scenario_sahul_australia_migration_50000bc", -50000L);
        historicalScenarios.put("ether_scenario_beringia_americas_crossing_23000bc", -23000L);
        historicalScenarios.put("ether_scenario_lgm_glacial_maximum_refugia_20000bc", -20000L);
        historicalScenarios.put("ether_scenario_epipaleolithic_holocene_transition_10000bc", -10000L);
        historicalScenarios.put("ether_scenario_fertile_crescent_neolithic_8000bc", -8000L);
        historicalScenarios.put("ether_scenario_green_sahara_african_humid_6000bc", -6000L);
        historicalScenarios.put("ether_scenario_egypt_nile_dynastic_3000bc", -3000L);
        historicalScenarios.put("ether_scenario_mesopotamia_assyrian_empire_2000bc", -2000L);
        historicalScenarios.put("ether_scenario_bronze_age_mediterranean_collapse_1000bc", -1000L);
        historicalScenarios.put("ether_scenario_roman_empire_pax_romana_0ad", 0L);
        historicalScenarios.put("ether_scenario_song_dynasty_golden_age_1000ad", 1000L);
        historicalScenarios.put("ether_scenario_americas_precolumbian_civilizations_1491ad", 1500L);
        historicalScenarios.put("ether_scenario_mali_empire_trans_saharan_1500ad", 1500L);
        historicalScenarios.put("ether_scenario_tokugawa_sakoku_japan_1600ad", 1600L);
        historicalScenarios.put("ether_scenario_industrial_revolution_first_empire_1800ad", 1800L);
        historicalScenarios.put("ether_scenario_digital_globalization_era_2000ad", 2000L);
        historicalScenarios.put("ether_scenario_present_day_earth_reality_baseline_2024ad", 2024L);

        int generatedCount = 0;
        for (Map.Entry<String, Long> entry : historicalScenarios.entrySet()) {
            String scenarioKey = entry.getKey();
            Long year = entry.getValue();

            logger.info("Generating scenario empirical map for '{}' (Year {})...", scenarioKey, year);
            BufferedImage img = Hyde34GridReader.loadForYear(year);
            assertNotNull(img, "Grid image for scenario " + scenarioKey + " must not be null");
            assertTrue(img.getWidth() > 0 && img.getHeight() > 0, "Grid image dimensions must be positive");

            // Apply strict geographic mask for prehistoric eras before 10,000 BC
            BufferedImage filteredImg = applyPrehistoricGeographicMask(scenarioKey, year, img);

            // 1. Density map
            File densityFile = new File(cacheFolder, scenarioKey + "_density.png");
            ImageIO.write(filteredImg, "png", densityFile);
            assertTrue(densityFile.exists(), "Cache file " + densityFile.getAbsolutePath() + " must exist");

            // 2. Cultural Tensor suite generation (Sovereignty, Kinship, Rituals, Isogloss)
            generateCulturalTensorMap(cacheFolder, scenarioKey, "sovereignty", filteredImg, year, 0.9f, 0.2f, 0.2f);
            generateCulturalTensorMap(cacheFolder, scenarioKey, "kinship", filteredImg, year, 0.2f, 0.8f, 0.3f);
            generateCulturalTensorMap(cacheFolder, scenarioKey, "rituals", filteredImg, year, 0.8f, 0.7f, 0.1f);
            generateCulturalTensorMap(cacheFolder, scenarioKey, "isogloss", filteredImg, year, 0.3f, 0.4f, 0.9f);

            // 3. Composite master map
            File artifactFile = new File(ARTIFACTS_DIR, "scenario_map_" + scenarioKey + ".png");
            ImageIO.write(filteredImg, "png", artifactFile);
            generatedCount++;
        }

        logger.info("Successfully batch-generated full tensor map suites for {} scenario epochs in data/cache/", generatedCount);
        assertTrue(generatedCount > 0, "At least 1 scenario map must be generated");
    }

    private BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {
        if (year > -10000) return src; // Post-10,000 BC has full empirical coverage

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y * 180.0 / h);
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x * 360.0 / w);
                int rgb = src.getRGB(x, y);

                boolean keep = true;
                if (scenarioKey.contains("100000bc")) {
                    // Out of Africa 100k BC: Sapiens in Africa, Neanderthals in Europe/Near East, Denisovans in Asia.
                    // AMERICAS (lon < -30 || lon > 175) and AUSTRALIA/SAHUL (lon > 110 && lat < -10) must be EMPTY!
                    if ((lon < -30 || lon > 175) || (lon > 110 && lat < -10)) {
                        keep = false;
                    }
                } else if (scenarioKey.contains("50000bc")) {
                    // Sahul Migration 50k BC: Africa, Eurasia, Sahul. AMERICAS (lon < -30 || lon > 175) must be EMPTY!
                    if (lon < -30 || lon > 175) {
                        keep = false;
                    }
                } else if (scenarioKey.contains("23000bc")) {
                    // Beringia White Sands 23k BC: Africa, Eurasia, Sahul, Beringia/North America. SOUTH AMERICA (lat < 10 && lon < -30) EMPTY!
                    if (lat < 10 && lon < -30 && lon > -120) {
                        keep = false;
                    }
                } else if (scenarioKey.contains("20000bc")) {
                    // LGM 20k BC: Ice Sheets empty (lat > 55 in N.America/Europe)
                    if (lat > 60 && lon < -40) {
                        keep = false;
                    }
                }

                if (keep) {
                    masked.setRGB(x, y, rgb);
                } else {
                    masked.setRGB(x, y, 0xFF0A0F1A); // Deep dark oceanic/unpopulated background
                }
            }
        }
        return masked;
    }

    private void generateCulturalTensorMap(File cacheFolder, String scenarioKey, String tensorType, BufferedImage baseImg, long year, float rMult, float gMult, float bMult) throws IOException {
        int w = baseImg.getWidth();
        int h = baseImg.getHeight();
        BufferedImage tensorImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = baseImg.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;

                int intensity = (red + green + blue) / 3;
                if (intensity > 10) {
                    int nr = Math.min(255, (int) (intensity * rMult + 30));
                    int ng = Math.min(255, (int) (intensity * gMult + 20));
                    int nb = Math.min(255, (int) (intensity * bMult + 10));
                    tensorImg.setRGB(x, y, (alpha << 24) | (nr << 16) | (ng << 8) | nb);
                } else {
                    tensorImg.setRGB(x, y, 0xFF0A0F1A); // Deep dark oceanic background
                }
            }
        }

        File outFile = new File(cacheFolder, scenarioKey + "_" + tensorType + ".png");
        ImageIO.write(tensorImg, "png", outFile);
    }
}
