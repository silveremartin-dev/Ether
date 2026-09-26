package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;

public class TestWorldClimValues {
    @Test
    public void printSamples() throws Exception {
        File tempFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        File precipFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_12.tif");
        File seasonFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_4.tif");

        BufferedImage tImg = ImageIO.read(tempFile);
        BufferedImage pImg = ImageIO.read(precipFile);
        BufferedImage sImg = ImageIO.read(seasonFile);

        Raster tRaster = tImg.getRaster();
        Raster pRaster = pImg.getRaster();
        Raster sRaster = sImg.getRaster();

        System.out.println("T image type: " + tImg.getType() + ", raster sampleModel: " + tRaster.getSampleModel());
        System.out.println("P image type: " + pImg.getType() + ", raster sampleModel: " + pRaster.getSampleModel());
        System.out.println("S image type: " + sImg.getType() + ", raster sampleModel: " + sRaster.getSampleModel());

        // Sample Paris: lat 48.8, lon 2.3 -> x = (2.3+180)/360 * W, y = (90-48.8)/180 * H
        int w = tImg.getWidth();
        int h = tImg.getHeight();
        int px = (int) ((2.35 + 180.0) / 360.0 * w);
        int py = (int) ((90.0 - 48.85) / 180.0 * h);

        System.out.printf("Paris (x=%d, y=%d):%n", px, py);
        System.out.printf("  Temp sample: %f%n", tRaster.getSampleFloat(px, py, 0));
        System.out.printf("  Precip sample: %f%n", pRaster.getSampleFloat(px, py, 0));
        System.out.printf("  Season sample: %f%n", sRaster.getSampleFloat(px, py, 0));

        // Sample Amazon: lat -3.0, lon -60.0
        int ax = (int) ((-60.0 + 180.0) / 360.0 * w);
        int ay = (int) ((90.0 - (-3.0)) / 180.0 * h);
        System.out.printf("Amazon (x=%d, y=%d):%n", ax, ay);
        System.out.printf("  Temp sample: %f%n", tRaster.getSampleFloat(ax, ay, 0));
        System.out.printf("  Precip sample: %f%n", pRaster.getSampleFloat(ax, ay, 0));
        System.out.printf("  Season sample: %f%n", sRaster.getSampleFloat(ax, ay, 0));

        // Sample Ocean: lat 0.0, lon -30.0
        int ox = (int) ((-30.0 + 180.0) / 360.0 * w);
        int oy = (int) ((90.0 - 0.0) / 180.0 * h);
        System.out.printf("Atlantic Ocean (x=%d, y=%d):%n", ox, oy);
        System.out.printf("  Temp sample: %f%n", tRaster.getSampleFloat(ox, oy, 0));
        System.out.printf("  Precip sample: %f%n", pRaster.getSampleFloat(ox, oy, 0));
        System.out.printf("  Season sample: %f%n", sRaster.getSampleFloat(ox, oy, 0));
    }
}
