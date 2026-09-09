package org.ether.society;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;

public class ExtractWorldClimTif {
    public static void main(String[] args) {
        try {
            File tempFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
            File precipFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_12.tif");
            File seasonFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_4.tif");

            if (!tempFile.exists() || !precipFile.exists() || !seasonFile.exists()) {
                System.err.println("WorldClim TIF files not extracted yet.");
                return;
            }

            BufferedImage tImg = ImageIO.read(tempFile);
            BufferedImage pImg = ImageIO.read(precipFile);
            BufferedImage sImg = ImageIO.read(seasonFile);

            int width = tImg.getWidth();
            int height = tImg.getHeight();

            Raster tRaster = tImg.getRaster();
            Raster pRaster = pImg.getRaster();
            Raster sRaster = sImg.getRaster();

            BufferedImage outTemp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage outPrecip = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage outSeason = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Temperature (°C)
                    float tv = tRaster.getSampleFloat(x, y, 0);
                    int tGray;
                    if (tv < -100 || tv > 100) { // NoData oceans/void
                        tGray = (int) (((15.0 + 50.0) / 100.0) * 255.0); // Ocean baseline 15°C
                    } else {
                        double norm = Math.clamp((tv + 50.0) / 100.0, 0.0, 1.0);
                        tGray = (int) (norm * 255.0);
                    }
                    outTemp.setRGB(x, y, new Color(tGray, tGray, tGray).getRGB());

                    // Precipitation (mm/year)
                    float pv = pRaster.getSampleFloat(x, y, 0);
                    int pGray;
                    if (pv < 0 || pv > 30000) { // NoData
                        pGray = 0;
                    } else {
                        double norm = Math.clamp(pv / 3000.0, 0.0, 1.0);
                        pGray = (int) (norm * 255.0);
                    }
                    outPrecip.setRGB(x, y, new Color(pGray, pGray, pGray).getRGB());

                    // Seasonality (std dev * 100)
                    float sv = sRaster.getSampleFloat(x, y, 0);
                    int sGray;
                    if (sv < 0 || sv > 50000) { // NoData
                        sGray = 0;
                    } else {
                        double norm = Math.clamp(sv / 2000.0, 0.0, 1.0);
                        sGray = (int) (norm * 255.0);
                    }
                    outSeason.setRGB(x, y, new Color(sGray, sGray, sGray).getRGB());
                }
            }

            File destDir = new File("src/main/resources/maps");
            destDir.mkdirs();

            File outTempFile = new File(destDir, "earth_temperature.png");
            File outPrecipFile = new File(destDir, "earth_precipitation.png");
            File outSeasonFile = new File(destDir, "earth_seasonality.png");

            ImageIO.write(outTemp, "png", outTempFile);
            ImageIO.write(outPrecip, "png", outPrecipFile);
            ImageIO.write(outSeason, "png", outSeasonFile);

            System.out.println("Successfully extracted 100% empirical WorldClim v2.1 datasets into:");
            System.out.println("  " + outTempFile.getAbsolutePath() + " (" + outTempFile.length() + " bytes)");
            System.out.println("  " + outPrecipFile.getAbsolutePath() + " (" + outPrecipFile.length() + " bytes)");
            System.out.println("  " + outSeasonFile.getAbsolutePath() + " (" + outSeasonFile.length() + " bytes)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
