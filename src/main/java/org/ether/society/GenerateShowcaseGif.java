package org.ether.society;

import org.ether.society.persistence.GifSequenceWriter;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Generates the animated showcase GIF (docs/ether_demo.gif) from the high-res tab screenshots.
 */
public class GenerateShowcaseGif {

    public static void main(String[] args) {
        try {
            System.out.println("=== Generating Ether Showcase Animated GIF ===");
            File gifOut = new File("docs/ether_demo.gif");

            List<String> imageFiles = List.of(
                    "docs/images/screenshots/tab1_planet_generator.png",
                    "docs/images/screenshots/tab2_resources.png",
                    "docs/images/screenshots/tab3_scenario_setup.png",
                    "docs/images/screenshots/tab4_execution_context.png",
                    "docs/images/screenshots/tab5_simulation.png",
                    "docs/images/screenshots/god_mode_panel.png",
                    "docs/images/screenshots/tab6_comparative_analytics.png",
                    "docs/images/screenshots/tab7_preferences.png"
            );

            int targetWidth = 1000;
            int targetHeight = 625;
            int displayTimeMs = 2500; // 2.5 seconds per tab

            try (ImageOutputStream output = new FileImageOutputStream(gifOut);
                 GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, displayTimeMs, true)) {

                for (String path : imageFiles) {
                    File imgFile = new File(path);
                    if (!imgFile.exists()) {
                        System.err.println("Warning: file not found " + path);
                        continue;
                    }
                    BufferedImage original = ImageIO.read(imgFile);
                    BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
                    g.dispose();

                    writer.writeToSequence(scaled);
                    System.out.println(" Added frame to GIF: " + path);
                }
            }

            System.out.println(" Successfully generated showcase GIF: " + gifOut.getAbsolutePath() + " (" + gifOut.length() + " bytes)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
