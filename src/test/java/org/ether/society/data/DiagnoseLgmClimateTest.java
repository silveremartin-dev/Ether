package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DiagnoseLgmClimateTest {
    @Test
    public void testCompareElevAndClimate() throws Exception {
        File fElev = new File("data/maps/ether/earth/-20000/earth_-20000_elevation.png");
        File fTemp = new File("data/maps/ether/earth/-20000/earth_-20000_temperature.png");
        File fRain = new File("data/maps/ether/earth/-20000/earth_-20000_precipitation.png");
        File fSeas = new File("data/maps/ether/earth/-20000/earth_-20000_seasonality.png");

        BufferedImage imgElev = ImageIO.read(fElev);
        BufferedImage imgTemp = ImageIO.read(fTemp);
        BufferedImage imgRain = ImageIO.read(fRain);
        BufferedImage imgSeas = ImageIO.read(fSeas);

        System.out.println("Elev size: " + imgElev.getWidth() + "x" + imgElev.getHeight());
        System.out.println("Temp size: " + imgTemp.getWidth() + "x" + imgTemp.getHeight());
        System.out.println("Rain size: " + imgRain.getWidth() + "x" + imgRain.getHeight());
        System.out.println("Seas size: " + imgSeas.getWidth() + "x" + imgSeas.getHeight());

        // Check reference points: Florida (-80, 25), UK (-0, 52), South Africa (20, -34), Japan (138, 36)
        double[][] pts = {{-80.0, 25.0}, {0.0, 52.0}, {20.0, -34.0}, {138.0, 36.0}, {0.0, 0.0}};
        for (double[] pt : pts) {
            double lon = pt[0], lat = pt[1];
            int x = (int) ((lon + 180.0) / 360.0 * 2048);
            int y = (int) ((90.0 - lat) / 180.0 * 1024);
            int elevRgb = imgElev.getRGB(x, y);
            int tempRgb = imgTemp.getRGB(x, y);
            int rainRgb = imgRain.getRGB(x, y);
            int seasRgb = imgSeas.getRGB(x, y);
            System.out.printf("Point (lon=%.1f, lat=%.1f) -> pixel (%d, %d): Elev=0x%06X Temp=0x%06X Rain=0x%06X Seas=0x%06X%n",
                    lon, lat, x, y, elevRgb & 0xFFFFFF, tempRgb & 0xFFFFFF, rainRgb & 0xFFFFFF, seasRgb & 0xFFFFFF);
        }

        // Compare -100000 temp vs -20000 temp
        BufferedImage imgTemp100k = ImageIO.read(new File("data/maps/ether/earth/-100000/earth_-100000_temperature.png"));
        System.out.println("-100k temp size: " + imgTemp100k.getWidth() + "x" + imgTemp100k.getHeight());
    }
}
