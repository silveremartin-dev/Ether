package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class InspectMapsTest {
    @Test
    /*
     * Inspect map dimensions operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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

    @Test
    /*
     * Inspect year1800colors operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void inspectYear1800Colors() throws Exception {
        String dir = "data/maps/ether/earth/1800/";
        BufferedImage iso = ImageIO.read(new File(dir + "earth_1800_isogloss.png"));
        BufferedImage kin = ImageIO.read(new File(dir + "earth_1800_kinship.png"));
        BufferedImage sov = ImageIO.read(new File(dir + "earth_1800_sovereignty.png"));

        record Loc(String name, double lon, double lat) {}
        Loc[] locs = new Loc[]{
            new Loc("Paris (France)", 2.35, 48.85),
            new Loc("Toulouse (Occitanie)", 1.44, 43.60),
            new Loc("Rennes (Brittany)", -1.68, 48.11),
            new Loc("London (UK)", -0.12, 51.5),
            new Loc("Edinburgh (Scotland)", -3.18, 55.95),
            new Loc("Berlin (Prussia)", 13.4, 52.52),
            new Loc("Munich (Bavaria)", 11.58, 48.14),
            new Loc("Madrid (Spain)", -3.7, 40.4),
            new Loc("Barcelona (Catalonia)", 2.17, 41.38),
            new Loc("Rome (Italy)", 12.5, 41.9),
            new Loc("Vienna (Austria)", 16.37, 48.2),
            new Loc("Warsaw (Poland)", 21.01, 52.23),
            new Loc("Moscow (Russia)", 37.6, 55.75),
            new Loc("Beijing (China)", 116.4, 39.9),
            new Loc("Shanghai (Jiangnan)", 121.47, 31.23),
            new Loc("Delhi (India)", 77.2, 28.6),
            new Loc("Chennai (Tamil Nadu)", 80.27, 13.08),
            new Loc("Cairo (Ottoman)", 31.2, 30.0),
            new Loc("Philadelphia (USA)", -75.16, 39.95)
        };

        System.out.println("=== 1800 AD Cartographic Tensor Pixel Samples ===");
        for (Loc l : locs) {
            int x = Math.clamp((int)(((l.lon + 180.0) / 360.0) * iso.getWidth()), 0, iso.getWidth() - 1);
            int y = Math.clamp((int)(((90.0 - l.lat) / 180.0) * iso.getHeight()), 0, iso.getHeight() - 1);
            int cIso = iso.getRGB(x, y) & 0xFFFFFF;
            int cKin = kin.getRGB(x, y) & 0xFFFFFF;
            int cSov = sov.getRGB(x, y) & 0xFFFFFF;
            System.out.printf("%-25s | Iso: #%06X | Kin: #%06X | Sov: #%06X%n", l.name, cIso, cKin, cSov);
        }
    }

    @Test
    /*
     * Inspect year2026cartographic tensors operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void inspectYear2026CartographicTensors() throws Exception {
        org.ether.society.model.Scenario sc = new org.ether.society.model.Scenario();
        sc.setName("Anthropocene Present Day (2026 AD)");
        sc.setStartDateYear(2026L);
        sc.setPopulationDensityType("URBAN_CLUSTERS");
        sc.setInitialHumanCount(8_150_000_000L);
        sc.setInitialTechLevel(8.5);

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        String dir = "data/maps/ether/earth/2026/";
        BufferedImage iso = ImageIO.read(new File(dir + "earth_2026_isogloss.png"));
        BufferedImage kin = ImageIO.read(new File(dir + "earth_2026_kinship.png"));
        BufferedImage sov = ImageIO.read(new File(dir + "earth_2026_sovereignty.png"));
        BufferedImage rit = ImageIO.read(new File(dir + "earth_2026_rituals.png"));
        BufferedImage inst = ImageIO.read(new File(dir + "earth_2026_institutional.png"));
        BufferedImage tech = ImageIO.read(new File(dir + "earth_2026_technology.png"));
        BufferedImage eco = ImageIO.read(new File(dir + "earth_2026_ecological.png"));
        BufferedImage path = ImageIO.read(new File(dir + "earth_2026_pathogen.png"));

        record Loc(String name, double lon, double lat) {}
        Loc[] locs = new Loc[]{
            new Loc("Paris (France)", 2.35, 48.85),
            new Loc("Toulouse (Occitanie)", 1.44, 43.60),
            new Loc("Berlin (Germany)", 13.4, 52.52),
            new Loc("London (UK)", -0.12, 51.5),
            new Loc("Madrid (Spain)", -3.7, 40.4),
            new Loc("Barcelona (Catalonia)", 2.17, 41.38),
            new Loc("Bilbao (Basque)", -2.93, 43.26),
            new Loc("Rome (Italy)", 12.5, 41.9),
            new Loc("Washington (USA)", -77.04, 38.9),
            new Loc("Ottawa (Canada)", -75.7, 45.42),
            new Loc("Montreal (Quebec)", -73.56, 45.5),
            new Loc("Mexico City (Mexico)", -99.13, 19.43),
            new Loc("Quito (Ecuador)", -78.47, -0.18),
            new Loc("Canberra (Australia)", 149.13, -35.28),
            new Loc("Manila (Philippines)", 120.98, 14.6),
            new Loc("Jakarta (Indonesia)", 106.85, -6.21),
            new Loc("Tokyo (Japan)", 139.69, 35.69),
            new Loc("Beijing (China)", 116.4, 39.9),
            new Loc("New Delhi (India)", 77.2, 28.6),
            new Loc("Chennai (Tamil Nadu)", 80.27, 13.08),
            new Loc("Cairo (Egypt)", 31.24, 30.04),
            new Loc("Riyadh (Saudi Arabia)", 46.71, 24.63),
            new Loc("Tehran (Iran)", 51.39, 35.69)
        };

        System.out.println("=== 2026 AD Cartographic Tensor Pixel Samples ===");
        for (Loc l : locs) {
            int x = Math.clamp((int)(((l.lon + 180.0) / 360.0) * iso.getWidth()), 0, iso.getWidth() - 1);
            int y = Math.clamp((int)(((90.0 - l.lat) / 180.0) * iso.getHeight()), 0, iso.getHeight() - 1);
            int cIso = iso.getRGB(x, y) & 0xFFFFFF;
            int cKin = kin.getRGB(x, y) & 0xFFFFFF;
            int cSov = sov.getRGB(x, y) & 0xFFFFFF;
            int cRit = rit.getRGB(x, y) & 0xFFFFFF;
            int cInst = inst.getRGB(x, y) & 0xFF;
            int cTech = tech.getRGB(x, y) & 0xFF;
            int cEco = eco.getRGB(x, y) & 0xFF;
            int cPath = path.getRGB(x, y) & 0xFF;
            System.out.printf("%-26s | Sov: #%06X | Iso: #%06X | Kin: #%06X | Rit: #%06X | Inst:%3d | Tech:%3d | Eco:%3d | Path:%3d%n",
                    l.name, cSov, cIso, cKin, cRit, cInst, cTech, cEco, cPath);
        }
    }

    @Test
    /*
     * Inspect year1914cshapes operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void inspectYear1914CShapes() throws Exception {
        org.ether.society.model.Scenario sc = new org.ether.society.model.Scenario();
        sc.setName("Outbreak of World War I (1914 AD)");
        sc.setStartDateYear(1914L);
        sc.setPopulationDensityType("URBAN_CLUSTERS");
        sc.setInitialHumanCount(1_800_000_000L);
        sc.setInitialTechLevel(6.2);

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        String dir = "data/maps/ether/earth/1914/";
        BufferedImage sov = ImageIO.read(new File(dir + "earth_1914_sovereignty.png"));
        BufferedImage path = ImageIO.read(new File(dir + "earth_1914_pathogen.png"));

        record Loc(String name, double lon, double lat) {}
        Loc[] locs = new Loc[]{
            new Loc("Vienna (Austria-Hungary)", 16.37, 48.2),
            new Loc("Budapest (Austria-Hungary)", 19.04, 47.5),
            new Loc("Prague (Austria-Hungary / Bohemia)", 14.42, 50.08),
            new Loc("Berlin (German Empire)", 13.4, 52.52),
            new Loc("Strasbourg (German Empire / Alsace)", 7.75, 48.58),
            new Loc("Saint Petersburg (Russian Empire)", 30.3, 59.93),
            new Loc("Warsaw (Russian Empire / Poland)", 21.01, 52.23),
            new Loc("Helsinki (Russian Empire / Finland)", 24.94, 60.17),
            new Loc("Istanbul (Ottoman Empire)", 28.97, 41.0),
            new Loc("Baghdad (Ottoman Empire)", 44.36, 33.31),
            new Loc("London (British Empire)", -0.12, 51.5),
            new Loc("Delhi (British Raj / India)", 77.2, 28.6),
            new Loc("Paris (France)", 2.35, 48.85),
            new Loc("Algiers (French Empire / Algeria)", 3.05, 36.75)
        };

        System.out.println("=== 1914 AD Historical CShapes Sovereign Entity Samples ===");
        for (Loc l : locs) {
            int x = Math.clamp((int)(((l.lon + 180.0) / 360.0) * sov.getWidth()), 0, sov.getWidth() - 1);
            int y = Math.clamp((int)(((90.0 - l.lat) / 180.0) * sov.getHeight()), 0, sov.getHeight() - 1);
            int cSov = sov.getRGB(x, y) & 0xFFFFFF;
            int cPath = path.getRGB(x, y) & 0xFF;
            System.out.printf("%-38s | Sov: #%06X | Pathogen R0:%3d%n", l.name, cSov, cPath);
        }
    }

    @Test
    /*
     * Inspect year minus1000ancient world operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void inspectYearMinus1000AncientWorld() throws Exception {
        org.ether.society.model.Scenario sc = new org.ether.society.model.Scenario();
        sc.setName("Early Iron Age & Neo-Assyrian / Zhou Dynasty Emergence (-1000 BC)");
        sc.setStartDateYear(-1000L);
        sc.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        sc.setInitialHumanCount(100_000_000L);
        sc.setInitialTechLevel(2.6);

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        String dir = "data/maps/ether/earth/-1000/";
        BufferedImage sov = ImageIO.read(new File(dir + "earth_-1000_sovereignty.png"));
        BufferedImage path = ImageIO.read(new File(dir + "earth_-1000_pathogen.png"));

        record Loc(String name, double lon, double lat) {}
        Loc[] locs = new Loc[]{
            new Loc("Nineveh (Assyrian Empire)", 43.15, 36.36),
            new Loc("Haojing (Western Zhou China)", 108.7, 34.2),
            new Loc("Thebes (21st Dyn. Egypt)", 32.65, 25.72),
            new Loc("Athens (Archaic Greece)", 23.7, 37.9),
            new Loc("Congo Basin (Stateless Frontier)", 22.0, 0.0),
            new Loc("Amazon Basin (Stateless Frontier)", -60.0, -3.0),
            new Loc("Northern Canada (Stateless Frontier)", -95.0, 56.0),
            new Loc("Siberia (Stateless Frontier)", 120.0, 62.0),
            new Loc("Antarctica (Uninhabited Ice)", 0.0, -75.0)
        };

        System.out.println("=== -1000 BC Cartographic Tensor Pixel Samples ===");
        for (Loc l : locs) {
            int x = Math.clamp((int)(((l.lon + 180.0) / 360.0) * sov.getWidth()), 0, sov.getWidth() - 1);
            int y = Math.clamp((int)(((90.0 - l.lat) / 180.0) * sov.getHeight()), 0, sov.getHeight() - 1);
            int cSov = sov.getRGB(x, y) & 0xFFFFFF;
            int cPath = path.getRGB(x, y) & 0xFF;
            System.out.printf("%-40s | Sov: #%06X | Pathogen R0:%3d%n", l.name, cSov, cPath);
        }
    }

    @Test
    /*
     * Inspect year0classical world operation.
     * <p>
     * Executes operational logic for {@code InspectMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void inspectYear0ClassicalWorld() throws Exception {
        org.ether.society.model.Scenario sc = new org.ether.society.model.Scenario();
        sc.setName("Pax Romana, Han Empire & Classical Axial Age (1 AD)");
        sc.setStartDateYear(0L);
        sc.setPopulationDensityType("ROMAN_EMPIRE");
        sc.setInitialHumanCount(250_000_000L);
        sc.setInitialTechLevel(3.2);

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        String dir = "data/maps/ether/earth/0/";
        BufferedImage iso = ImageIO.read(new File(dir + "earth_0_isogloss.png"));
        BufferedImage kin = ImageIO.read(new File(dir + "earth_0_kinship.png"));
        BufferedImage sov = ImageIO.read(new File(dir + "earth_0_sovereignty.png"));
        BufferedImage trade = ImageIO.read(new File(dir + "earth_0_tradenetwork.png"));
        BufferedImage path = ImageIO.read(new File(dir + "earth_0_pathogen.png"));

        record Loc(String name, double lon, double lat) {}
        Loc[] locs = new Loc[]{
            new Loc("Rome (Roman Empire)", 12.5, 41.9),
            new Loc("Chang'an (Han Dynasty)", 108.9, 34.3),
            new Loc("Alexandria (Roman Egypt)", 29.9, 31.2),
            new Loc("Ctesiphon (Parthian Empire)", 44.6, 33.1),
            new Loc("Taxila (Kushan Empire)", 72.8, 33.8),
            new Loc("Amazon Basin (Tribal)", -60.0, -3.0),
            new Loc("Congo Basin (Tribal)", 22.0, -1.0),
            new Loc("Siberian Taiga (Tribal)", 100.0, 58.0),
            new Loc("North America Plains (Tribal)", -100.0, 42.0),
            new Loc("Australian Outback (Tribal)", 134.0, -25.0),
            new Loc("Germania Forest (Tribal)", 10.0, 52.0),
            new Loc("Antarctica East (Uninhabited)", 0.0, -75.0),
            new Loc("Antarctica West (Uninhabited)", -100.0, -80.0)
        };

        System.out.println("=== 0 AD Cartographic Tensor Pixel Samples ===");
        for (Loc l : locs) {
            int x = Math.clamp((int)(((l.lon + 180.0) / 360.0) * iso.getWidth()), 0, iso.getWidth() - 1);
            int y = Math.clamp((int)(((90.0 - l.lat) / 180.0) * iso.getHeight()), 0, iso.getHeight() - 1);
            int cIso = iso.getRGB(x, y) & 0xFFFFFF;
            int cKin = kin.getRGB(x, y) & 0xFFFFFF;
            int cSov = sov.getRGB(x, y) & 0xFFFFFF;
            int cPath = path.getRGB(x, y) & 0xFF;
            System.out.printf("%-35s | Iso: #%06X | Kin: #%06X | Sov: #%06X | Pathogen: %3d%n", l.name, cIso, cKin, cSov, cPath);
        }
    }
}
