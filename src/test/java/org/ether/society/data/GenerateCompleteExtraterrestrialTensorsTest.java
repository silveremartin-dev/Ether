/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.ether.society.data.ExtraterrestrialTensorMetadataSupport.inspectAndProcessTitanGeomorphology;
import static org.ether.society.data.ExtraterrestrialTensorMetadataSupport.writeCulturalRegistry;
import static org.ether.society.data.ExtraterrestrialTensorMetadataSupport.writeProvenanceAndSources;
import static org.ether.society.data.ExtraterrestrialTensorMetadataSupport.writeReadme;

/**
 * Generates the complete 25-layer cartographic tensor suite and governance metadata
 * ({@code cultural_registry.json}, {@code provenance_and_sources.json}, {@code README.md})
 * for all extraterrestrial worlds: Mars (2026, 2050), Moon (2026, 2035), Venus (2026, 2080),
 * Mercury (2026, 2120), and Titan (2150).
 *
 * @author Silvere Martin-Michiellot
 * @see ExtraterrestrialTensorMetadataSupport
 */
public class GenerateCompleteExtraterrestrialTensorsTest {

    /** Standard equirectangular raster width in pixels. */
    static final int WIDTH = 2048;

    /** Standard equirectangular raster height in pixels. */
    static final int HEIGHT = 1024;

    /**
     * Specification record for an extraterrestrial pioneer colony outpost node.
     *
     * @param id               unique outpost identifier
     * @param name             human-readable outpost name
     * @param lat              latitude in degrees
     * @param lon              longitude in degrees
     * @param population       initial pioneer population count
     * @param sovereigntyColor RGB color encoding sovereign authority
     * @param isoglossColor    RGB color encoding operational language standard
     * @param kinshipColor     RGB color encoding cohort social structure
     * @param techLevel        8-bit technology index {@code [0, 255]}
     * @param instLevel        8-bit institutional hierarchy index {@code [0, 255]}
     * @param description      mission directorate description
     */
    record OutpostSite(String id, String name, double lat, double lon, int population,
                       Color sovereigntyColor, Color isoglossColor, Color kinshipColor,
                       int techLevel, int instLevel, String description) {
    }

    /**
     * Generates all 25 raster layers and governance JSON/Markdown descriptors across Mars, Moon,
     * Venus, Mercury, and Titan colonization epochs.
     *
     * @throws Exception if raster I/O or metadata writing fails
     */
    @Test
    public void generateAllExtraterrestrialEpochs() throws Exception {
        inspectAndProcessTitanGeomorphology();

        // 1. MARS
        generateWorldEpoch("mars", 2026, "Mars Pre-Colonization Barren Epoch (2026 CE)",
                "Uninhabited hyperarid terrestrial planet under robotic exploration.", List.of());
        generateWorldEpoch("mars", 2050, "Martian Sub-Regolith Colonization & ECLSS Outposts (2050 CE)",
                "Pioneer human settlement at Chryse Planitia and Jezero Crater using Sabatier methane ISRU and basalt domes.",
                List.of(
                        new OutpostSite("chryse", "Chryse Planitia Pioneer Outpost", 22.4, -49.8, 300,
                                new Color(220, 38, 38), new Color(59, 130, 246), new Color(168, 85, 247), 180, 160,
                                "Martian Exploration Directorate (Ares Coalition)"),
                        new OutpostSite("jezero", "Jezero Crater Science Hub", 18.38, 77.58, 200,
                                new Color(13, 148, 136), new Color(14, 165, 233), new Color(168, 85, 247), 200, 175,
                                "International Astrobiology Research Consortia")
                ));

        // 2. MOON
        generateWorldEpoch("moon", 2026, "Lunar Desolation Robotic Exploration (2026 CE)",
                "Barren lunar regolith, robotic orbiters, and surface landers.", List.of());
        generateWorldEpoch("moon", 2035, "Shackleton Crater Lunar Base & ISRU Ice Mining (2035 CE)",
                "Permanent scientific outpost at the Lunar South Pole exploiting ice traps and peaks of eternal light.",
                List.of(
                        new OutpostSite("shackleton", "Shackleton Crater International Outpost", -89.9, 0.0, 50,
                                new Color(37, 99, 235), new Color(99, 102, 241), new Color(236, 72, 153), 170, 150,
                                "Artemis Accord South Pole Research Base")
                ));

        // 3. VENUS
        generateWorldEpoch("venus", 2026, "Venus Hyper-Thermal Acid Furnace (2026 CE)",
                "Super-critical CO2 atmosphere (92 atm) and sulfuric acid shroud with 0 population.", List.of());
        generateWorldEpoch("venus", 2080, "Venus High-Altitude Cloud Cities & Aerostat Extraction (2080 CE)",
                "Aerostat flotillas floating at 55 km altitude under Earth-normal pressure and temperature.",
                List.of(
                        new OutpostSite("aphrodite", "Aphrodite Aerostat Flotilla", 0.0, 0.0, 105,
                                new Color(234, 179, 8), new Color(16, 185, 129), new Color(249, 115, 22), 220, 190,
                                "Hesperos Atmospheric Mining Collective"),
                        new OutpostSite("ishtar", "Ishtar Aerostat Station", 0.0, 120.0, 45,
                                new Color(217, 119, 6), new Color(20, 184, 166), new Color(249, 115, 22), 210, 180,
                                "High-Altitude Sulfuric Condensation Lab")
                ));

        // 4. MERCURY
        generateWorldEpoch("mercury", 2026, "Hermian Radiative Desolation (2026 CE)",
                "Airless cratered body exposed to extreme solar radiation.", List.of());
        generateWorldEpoch("mercury", 2120, "Caloris Basin Solar Forge & Smelting Base (2120 CE)",
                "Automated solar collection arrays and underground thermal management outposts.",
                List.of(
                        new OutpostSite("caloris", "Caloris Forge Colony", 30.5, -170.0, 100,
                                new Color(225, 29, 72), new Color(245, 158, 11), new Color(147, 51, 234), 240, 200,
                                "Hermian Solar Smelting & Heavy Metallurgy Directorate")
                ));

        // 5. TITAN
        generateWorldEpoch("titan", 2150, "Titan Cryo-Hydrocarbon Seas & Industrial Port (2150 CE)",
                "Cryogenic extraction of liquid ethane and methane at Kraken Mare.",
                List.of(
                        new OutpostSite("kraken", "Kraken Mare Hydrocarbon Base", 68.0, 50.0, 80,
                                new Color(6, 182, 212), new Color(59, 130, 246), new Color(139, 92, 246), 250, 210,
                                "Saturnian Cryo-Extraction Logistics Hub")
                ));
    }

    /**
     * Generates all 15 physical/mineral basemaps, 10 cultural/demographic tensors, and 3 governance
     * metadata files for a specified planetary colonization epoch.
     *
     * @param planet   canonical planet identifier
     * @param epoch    target calendar year
     * @param eraName  human-readable era title
     * @param summary  concise scientific overview
     * @param outposts list of pioneer outpost sites
     * @throws Exception if raster or metadata file generation fails
     */
    private void generateWorldEpoch(String planet, int epoch, String eraName, String summary, List<OutpostSite> outposts) throws Exception {
        File dir = new File("data/maps/ether/" + planet + "/" + epoch);
        dir.mkdirs();

        File refDir = new File("data/maps/ether/" + planet + "/2026");
        copyOrCreatePhysicalLayers(refDir, dir, planet, epoch);

        BufferedImage imgDensity = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imgSovereignty = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgIsogloss = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgKinship = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgRituals = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgTech = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imgInst = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imgEco = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imgPathogen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imgTrade = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (BufferedImage im : List.of(imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals,
                imgTech, imgInst, imgEco, imgPathogen, imgTrade)) {
            clearImage(im, 0);
        }

        if (outposts != null && !outposts.isEmpty()) {
            for (OutpostSite out : outposts) {
                int cx = (int) ((out.lon + 180.0) / 360.0 * WIDTH);
                int cy = (int) ((90.0 - out.lat) / 180.0 * HEIGHT);
                int r = 12;

                drawCircularGradient(imgDensity, cx, cy, r, Math.min(255, 100 + out.population / 2));
                drawSolidCircle(imgSovereignty, cx, cy, r + 2, out.sovereigntyColor);
                drawSolidCircle(imgIsogloss, cx, cy, r, out.isoglossColor);
                drawSolidCircle(imgKinship, cx, cy, r, out.kinshipColor);
                drawSolidCircle(imgRituals, cx, cy, r - 2, new Color(255, 215, 0));
                drawCircularGradient(imgTech, cx, cy, r, out.techLevel);
                drawCircularGradient(imgInst, cx, cy, r, out.instLevel);
                drawCircularGradient(imgEco, cx, cy, r, 80);
                drawCircularGradient(imgPathogen, cx, cy, r, 20);
                drawSolidCircle(imgTrade, cx, cy, r - 4, new Color(255, 255, 255));
            }

            if (outposts.size() > 1) {
                Graphics2D gTrade = imgTrade.createGraphics();
                gTrade.setColor(new Color(56, 189, 248, 180));
                gTrade.setStroke(new BasicStroke(2.0f));
                for (int i = 0; i < outposts.size() - 1; i++) {
                    OutpostSite a = outposts.get(i);
                    OutpostSite b = outposts.get(i + 1);
                    int ax = (int) ((a.lon + 180.0) / 360.0 * WIDTH);
                    int ay = (int) ((90.0 - a.lat) / 180.0 * HEIGHT);
                    int bx = (int) ((b.lon + 180.0) / 360.0 * WIDTH);
                    int by = (int) ((90.0 - b.lat) / 180.0 * HEIGHT);
                    gTrade.drawLine(ax, ay, bx, by);
                }
                gTrade.dispose();
            }
        }

        saveMap(dir, planet, epoch, "density", imgDensity);
        saveMap(dir, planet, epoch, "sovereignty", imgSovereignty);
        saveMap(dir, planet, epoch, "isogloss", imgIsogloss);
        saveMap(dir, planet, epoch, "kinship", imgKinship);
        saveMap(dir, planet, epoch, "rituals", imgRituals);
        saveMap(dir, planet, epoch, "technology", imgTech);
        saveMap(dir, planet, epoch, "institutional", imgInst);
        saveMap(dir, planet, epoch, "ecological", imgEco);
        saveMap(dir, planet, epoch, "pathogen", imgPathogen);
        saveMap(dir, planet, epoch, "tradenetwork", imgTrade);

        writeCulturalRegistry(dir, planet, epoch, eraName, outposts);
        writeProvenanceAndSources(dir, planet, epoch, eraName, summary);
        writeReadme(dir, planet, epoch, eraName, summary, outposts);

        System.out.printf("Successfully generated complete 25-layer tensor suite for %s / %d%n", planet, epoch);
    }

    /**
     * Resolves and writes all 15 physical and mineral basemap rasters for the target epoch.
     *
     * @param refDir 2026 reference basemap directory
     * @param dstDir target epoch output directory
     * @param planet planet identifier
     * @param epoch  target epoch year
     * @throws Exception if image I/O fails
     */
    private void copyOrCreatePhysicalLayers(File refDir, File dstDir, String planet, int epoch) throws Exception {
        Object[][] specs = {
            {"elevation", 0.5}, {"biomes", 0.3}, {"temperature", 0.2}, {"precipitation", 0.0},
            {"seasonality", 0.1}, {"aquifers", 0.0}, {"iron_copper", 0.4}, {"precious_metals", 0.2},
            {"uranium", 0.1}, {"coal", 0.0}, {"oil", 0.0}, {"gas", 0.0},
            {"helium3", 0.3}, {"rare_earths", 0.2}, {"geothermal", 0.1}
        };
        for (Object[] spec : specs) {
            String layer = (String) spec[0];
            double defVal = (Double) spec[1];
            BufferedImage img = loadOrCreatePhysicalLayer(refDir, planet, layer, defVal);
            saveMap(dstDir, planet, epoch, layer, img);
        }
    }

    /**
     * Loads a reference 2026 physical PNG layer if present, or synthesizes a uniform baseline raster.
     *
     * @param refDir     2026 reference directory
     * @param planet     planet identifier
     * @param layerName  raster layer suffix
     * @param defaultVal fallback normalized intensity {@code [0.0, 1.0]}
     * @return loaded or synthesized {@link BufferedImage}
     */
    private BufferedImage loadOrCreatePhysicalLayer(File refDir, String planet, String layerName, double defaultVal) {
        File file = new File(refDir, planet + "_2026_" + layerName + ".png");
        if (file.exists()) {
            try {
                return ImageIO.read(file);
            } catch (Exception ignored) {
            }
        }
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        int gray = (int) (defaultVal * 255.0);
        int rgb = (gray << 16) | (gray << 8) | gray;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    /**
     * Writes a PNG layer file named {@code <planet>_<epoch>_<layerName>.png} into {@code dir}.
     *
     * @param dir       target directory
     * @param planet    planet identifier
     * @param epoch     target year
     * @param layerName layer identifier
     * @param img       rendered image
     * @throws Exception if PNG writing fails
     */
    static void saveMap(File dir, String planet, int epoch, String layerName, BufferedImage img) throws Exception {
        File outFile = new File(dir, planet + "_" + epoch + "_" + layerName + ".png");
        ImageIO.write(img, "PNG", outFile);
    }

    /**
     * Fills an entire image with a uniform RGB color.
     *
     * @param img target image
     * @param rgb 24-bit RGB fill value
     */
    static void clearImage(BufferedImage img, int rgb) {
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(rgb));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
    }

    /**
     * Renders an anti-aliased solid circular outpost disk onto an image.
     *
     * @param img    target image
     * @param cx     center X pixel coordinate
     * @param cy     center Y pixel coordinate
     * @param radius disk radius in pixels
     * @param color  fill color
     */
    static void drawSolidCircle(BufferedImage img, int cx, int cy, int radius, Color color) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.dispose();
    }

    /**
     * Renders a radial power-law grayscale gradient spot onto an image.
     *
     * @param img       target image
     * @param cx        center X pixel coordinate
     * @param cy        center Y pixel coordinate
     * @param radius    outer falloff radius in pixels
     * @param coreValue peak intensity {@code [0, 255]} at spot center
     */
    static void drawCircularGradient(BufferedImage img, int cx, int cy, int radius, int coreValue) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double dist = Math.hypot(dx, dy);
                if (dist <= radius) {
                    int px = Math.clamp(cx + dx, 0, WIDTH - 1);
                    int py = Math.clamp(cy + dy, 0, HEIGHT - 1);
                    double falloff = Math.pow(1.0 - (dist / radius), 1.5);
                    int val = (int) (coreValue * falloff);
                    int cur = (img.getRGB(px, py) & 0xFF);
                    int blended = Math.max(cur, val);
                    img.setRGB(px, py, (blended << 16) | (blended << 8) | blended);
                }
            }
        }
    }
}
