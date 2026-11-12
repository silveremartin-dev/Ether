/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Styling, scientific palette constants and primitive drawing helpers for
 * {@link BenchmarkSynthesisChartGenerator}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class BenchmarkChartStylingHelper {

    // High-Contrast Dark Scientific Palette (per AGENTS.md A§5)
    static final Color BG_DARK = new Color(13, 17, 23);
    static final Color PANEL_BG = new Color(22, 27, 34);
    static final Color GRID_LINE = new Color(48, 54, 61);
    static final Color TEXT_BRIGHT = new Color(240, 246, 252);
    static final Color TEXT_MUTED = new Color(165, 178, 194);
    static final Color ACCENT_CYAN = new Color(88, 166, 255);
    static final Color ACCENT_EMERALD = new Color(63, 185, 80);
    static final Color ACCENT_AMBER = new Color(210, 153, 34);
    static final Color ACCENT_VIOLET = new Color(188, 140, 255);
    static final Color ACCENT_CORAL = new Color(255, 123, 114);
    static final Color ACCENT_TEAL = new Color(57, 211, 180);

    private BenchmarkChartStylingHelper() {
        // Utility helper
    }

    static Graphics2D createAntiAliasedGraphics(BufferedImage img, int width, int height) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(BG_DARK);
        g.fillRect(0, 0, width, height);
        return g;
    }

    static void drawHeader(Graphics2D g, int width, String title, String subtitle) {
        g.setColor(PANEL_BG);
        g.fillRect(0, 0, width, 88);
        g.setColor(GRID_LINE);
        g.drawLine(0, 88, width, 88);

        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(TEXT_BRIGHT);
        g.drawString(title, 32, 42);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(TEXT_MUTED);
        g.drawString(subtitle, 32, 70);
    }

    static void drawDomainBand(Graphics2D g, int plotX, int plotY, int plotW, int plotH,
                               int startIdx, int endIdx, int total, Color fill, String label) {
        int x1 = plotX + (int) (startIdx * (plotW / (double) total));
        int x2 = plotX + (int) (endIdx * (plotW / (double) total));
        g.setColor(fill);
        g.fillRect(x1, plotY + 1, x2 - x1, plotH - 2);
        g.setColor(GRID_LINE);
        g.drawLine(x2, plotY, x2, plotY + plotH);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(TEXT_BRIGHT);
        g.drawString(label, x1 + 10, plotY + 20);
    }

    static void drawLegendItem(Graphics2D g, int x, int y, Color color, String text) {
        g.setColor(color);
        g.fillRoundRect(x, y - 10, 18, 12, 4, 4);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT_BRIGHT);
        g.drawString(text, x + 26, y);
    }

    static void drawRotatedText(Graphics2D g, String text, int x, int y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(-90));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(TEXT_BRIGHT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, -fm.stringWidth(text) / 2, 0);
        g.setTransform(old);
    }

    static void drawMetricCurve(Graphics2D g, int gx, int gy, int gw, int gh,
                                double[] vals, double minV, double maxV, Color color) {
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < vals.length; i++) {
            double x = gx + (i + 0.5) * (gw / (double) vals.length);
            double y = gy + gh - ((vals[i] - minV) / (maxV - minV) * gh);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(2.6f));
        g.draw(path);
        for (int i = 0; i < vals.length; i++) {
            int x = (int) (gx + (i + 0.5) * (gw / (double) vals.length));
            int y = (int) (gy + gh - ((vals[i] - minV) / (maxV - minV) * gh));
            g.setColor(BG_DARK);
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setColor(color);
            g.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    static void drawCenteredDriftCurve(Graphics2D g, int gx, int gy, int gw, int gh,
                                       double[] vals, double maxAbs, Color color) {
        Path2D.Double path = new Path2D.Double();
        int midY = gy + gh / 2;
        for (int i = 0; i < vals.length; i++) {
            double x = gx + (i / (double) (vals.length - 1)) * gw;
            double y = midY - (vals[i] / maxAbs) * (gh / 2.0);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(2.8f));
        g.draw(path);
        for (int i = 0; i < vals.length; i++) {
            int x = (int) (gx + (i / (double) (vals.length - 1)) * gw);
            int y = (int) (midY - (vals[i] / maxAbs) * (gh / 2.0));
            g.setColor(color);
            g.fillOval(x - 4, y - 4, 8, 8);
        }
    }
}
