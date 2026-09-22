package org.ether.society;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class ReferenceMapRasterizer {

    @Test
    public void generateAllReferenceMaps() throws Exception {
        File dataMapsDir = new File("data/maps");
        File resMapsDir = new File("src/main/resources/maps");
        dataMapsDir.mkdirs();
        resMapsDir.mkdirs();

        System.out.println("=== 1. Extracting WorldClim TIF datasets ===");
        File tempFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        File precipFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_12.tif");
        File seasonFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_4.tif");

        if (tempFile.exists() && precipFile.exists() && seasonFile.exists()) {
            BufferedImage tImg = ImageIO.read(tempFile);
            BufferedImage pImg = ImageIO.read(precipFile);
            BufferedImage sImg = ImageIO.read(seasonFile);

            int width = 1024;
            int height = 512;

            BufferedImage outTemp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage outPrecip = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage outSeason = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Raster tRaster = tImg.getRaster();
            Raster pRaster = pImg.getRaster();
            Raster sRaster = sImg.getRaster();

            int srcW = tImg.getWidth();
            int srcH = tImg.getHeight();

            for (int y = 0; y < height; y++) {
                int srcY = (int) Math.min(((y + 0.5) / height) * srcH, srcH - 1);
                for (int x = 0; x < width; x++) {
                    int srcX = (int) Math.min(((x + 0.5) / width) * srcW, srcW - 1);

                    // Temperature (°C, -50..+50)
                    float tv = tRaster.getSampleFloat(srcX, srcY, 0);
                    int tGray;
                    if (tv < -100 || tv > 100) {
                        double lat = 90.0 - (y / (double) height) * 180.0;
                        double latNorm = Math.abs(lat) / 90.0;
                        double baseOceanTemp = 28.0 - latNorm * 32.0; // Ocean latitudinal baseline
                        tGray = (int) (Math.clamp((baseOceanTemp + 50.0) / 100.0, 0.0, 1.0) * 255.0);
                    } else {
                        double norm = Math.clamp((tv + 50.0) / 100.0, 0.0, 1.0);
                        tGray = (int) (norm * 255.0);
                    }
                    outTemp.setRGB(x, y, new Color(tGray, tGray, tGray).getRGB());

                    // Precipitation (mm/year, 0..3000)
                    float pv = pRaster.getSampleFloat(srcX, srcY, 0);
                    int pGray;
                    if (pv < 0 || pv > 30000) {
                        double lat = 90.0 - (y / (double) height) * 180.0;
                        double itcz = Math.exp(-Math.pow(lat / 15.0, 2)) * 2000.0;
                        pGray = (int) (Math.clamp(itcz / 3000.0, 0.0, 1.0) * 255.0);
                    } else {
                        double norm = Math.clamp(pv / 3000.0, 0.0, 1.0);
                        pGray = (int) (norm * 255.0);
                    }
                    outPrecip.setRGB(x, y, new Color(pGray, pGray, pGray).getRGB());

                    // Seasonality (std dev * 100, 0..50°C)
                    float sv = sRaster.getSampleFloat(srcX, srcY, 0);
                    int sGray;
                    if (sv < 0 || sv > 50000) {
                        double lat = 90.0 - (y / (double) height) * 180.0;
                        double oceanAmp = (Math.abs(lat) / 90.0) * 12.0; // Mild ocean seasonal amplitude
                        sGray = (int) (Math.clamp(oceanAmp / 50.0, 0.0, 1.0) * 255.0);
                    } else {
                        double norm = Math.clamp((sv / 100.0) / 50.0, 0.0, 1.0);
                        sGray = (int) (norm * 255.0);
                    }
                    outSeason.setRGB(x, y, new Color(sGray, sGray, sGray).getRGB());
                }
            }

            saveMapPair(outTemp, "earth_temperature.png", dataMapsDir, resMapsDir);
            saveMapPair(outPrecip, "earth_precipitation.png", dataMapsDir, resMapsDir);
            saveMapPair(outSeason, "earth_seasonality.png", dataMapsDir, resMapsDir);
        }

        System.out.println("=== 2. Generating Mantle Heat Flow from Davies (2013) ===");
        File heatCsv = new File("data/maps/ihfc_davies2013/heat_flow_2deg.csv");
        if (heatCsv.exists()) {
            Map<String, Double> heatGrid = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(heatCsv))) {
                String line = br.readLine(); // Header
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        try {
                            double lon = Double.parseDouble(parts[0].trim());
                            double lat = Double.parseDouble(parts[1].trim());
                            double q = Double.parseDouble(parts[2].trim());
                            int gLon = (int) Math.floor(lon / 2.0) * 2;
                            int gLat = (int) Math.floor(lat / 2.0) * 2;
                            heatGrid.put(gLon + ":" + gLat, q);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            BufferedImage outHeat = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 512; y++) {
                double lat = 90.0 - (y / 512.0) * 180.0;
                int gLat = (int) Math.floor(lat / 2.0) * 2;
                for (int x = 0; x < 1024; x++) {
                    double lon = -180.0 + (x / 1024.0) * 360.0;
                    int gLon = (int) Math.floor(lon / 2.0) * 2;

                    Double q = heatGrid.get(gLon + ":" + gLat);
                    double val = q != null ? q : 65.0; // Baseline continental crust heat flow: 65 mW/m²
                    double norm = Math.clamp((val - 20.0) / (250.0 - 20.0), 0.0, 1.0);
                    int gray = (int) (norm * 255.0);
                    outHeat.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                }
            }
            saveMapPair(outHeat, "earth_geothermal.png", dataMapsDir, resMapsDir);
        }

        System.out.println("=== 3. Synchronizing and Saving Earth Elevation & Biomes ===");
        File cleanElev = new File("data/maps/scratch_db000ab_earth_elevation.png");
        if (cleanElev.exists()) {
            BufferedImage elevImg = ImageIO.read(cleanElev);
            saveMapPair(elevImg, "earth_elevation.png", dataMapsDir, resMapsDir);
        }

        File cleanBiome = new File("data/maps/scratch_db000ab_earth_biomes.png");
        if (cleanBiome.exists()) {
            BufferedImage bioImg = ImageIO.read(cleanBiome);
            saveMapPair(bioImg, "earth_biomes.png", dataMapsDir, resMapsDir);
        }

        System.out.println("=== 4. Synchronizing Energy and Mineral Tensors ===");
        String[] tensorNames = {
            "earth_coal.png", "earth_oil.png", "earth_gas.png", "earth_uranium.png",
            "earth_helium3.png", "earth_iron_copper.png", "earth_precious_metals.png",
            "earth_aquifers.png"
        };
        for (String name : tensorNames) {
            File srcFile = new File(resMapsDir, name);
            if (srcFile.exists()) {
                BufferedImage img = ImageIO.read(srcFile);
                saveMapPair(img, name, dataMapsDir, resMapsDir);
            }
        }
        System.out.println("=== Done! All maps generated and synchronized. ===");
    }

    private void saveMapPair(BufferedImage img, String filename, File dataMapsDir, File resMapsDir) throws Exception {
        File f1 = new File(dataMapsDir, filename);
        File f2 = new File(resMapsDir, filename);
        ImageIO.write(img, "PNG", f1);
        ImageIO.write(img, "PNG", f2);
        System.out.println("  Saved " + filename + " -> " + f1.getAbsolutePath() + " (" + f1.length() + " bytes)");
    }
}
