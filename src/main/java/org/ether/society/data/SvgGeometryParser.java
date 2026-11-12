/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.w3c.dom.Element;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Geometric SVG path and element parser converting vectors into pixel coordinates.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class SvgGeometryParser {

    private SvgGeometryParser() {
    }

    public static final int WIDTH = SvgMapIngestor.WIDTH;
    public static final int HEIGHT = SvgMapIngestor.HEIGHT;

    // --- SVG PATH GEOMETRY PARSER (M, L, H, V, C, Z, etc.) ---

    /**
     * Parse svg path d.
     *
     * @param d the d parameter (String)
     * @param viewBox the view box parameter (double[])
     * @return the resulting computation or state reference
     */
    public static Path2D parseSvgPathD(String d, double[] viewBox) {
        Path2D path = new Path2D.Double();
        if (d == null || d.isBlank()) return path;

        // Tokenize command letters and numbers
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("([a-zA-Z])|([-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?)").matcher(d);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        double curX = 0, curY = 0;
        double startX = 0, startY = 0;
        char cmd = 'M';
        int idx = 0;

        while (idx < tokens.size()) {
            String tok = tokens.get(idx);
            if (Character.isLetter(tok.charAt(0)) && tok.length() == 1) {
                cmd = tok.charAt(0);
                idx++;
            }

            switch (cmd) {
                case 'M': case 'm': {
                    if (idx + 1 >= tokens.size()) break;
                    double x = Double.parseDouble(tokens.get(idx++));
                    double y = Double.parseDouble(tokens.get(idx++));
                    if (cmd == 'm') { x += curX; y += curY; }
                    Point2D pt = transformSvgToPixel(x, y, viewBox);
                    path.moveTo(pt.x, pt.y);
                    curX = x; curY = y;
                    startX = curX; startY = curY;
                    cmd = (cmd == 'm') ? 'l' : 'L'; // Subsequent points in M/m are treated as L/l
                    break;
                }
                case 'L': case 'l': {
                    if (idx + 1 >= tokens.size()) break;
                    double x = Double.parseDouble(tokens.get(idx++));
                    double y = Double.parseDouble(tokens.get(idx++));
                    if (cmd == 'l') { x += curX; y += curY; }
                    Point2D pt = transformSvgToPixel(x, y, viewBox);
                    path.lineTo(pt.x, pt.y);
                    curX = x; curY = y;
                    break;
                }
                case 'H': case 'h': {
                    if (idx >= tokens.size()) break;
                    double x = Double.parseDouble(tokens.get(idx++));
                    if (cmd == 'h') { x += curX; }
                    Point2D pt = transformSvgToPixel(x, curY, viewBox);
                    path.lineTo(pt.x, pt.y);
                    curX = x;
                    break;
                }
                case 'V': case 'v': {
                    if (idx >= tokens.size()) break;
                    double y = Double.parseDouble(tokens.get(idx++));
                    if (cmd == 'v') { y += curY; }
                    Point2D pt = transformSvgToPixel(curX, y, viewBox);
                    path.lineTo(pt.x, pt.y);
                    curY = y;
                    break;
                }
                case 'C': case 'c': {
                    if (idx + 5 >= tokens.size()) break;
                    double x1 = Double.parseDouble(tokens.get(idx++));
                    double y1 = Double.parseDouble(tokens.get(idx++));
                    double x2 = Double.parseDouble(tokens.get(idx++));
                    double y2 = Double.parseDouble(tokens.get(idx++));
                    double x  = Double.parseDouble(tokens.get(idx++));
                    double y  = Double.parseDouble(tokens.get(idx++));
                    if (cmd == 'c') {
                        x1 += curX; y1 += curY;
                        x2 += curX; y2 += curY;
                        x  += curX; y  += curY;
                    }
                    Point2D p1 = transformSvgToPixel(x1, y1, viewBox);
                    Point2D p2 = transformSvgToPixel(x2, y2, viewBox);
                    Point2D p  = transformSvgToPixel(x, y, viewBox);
                    path.curveTo(p1.x, p1.y, p2.x, p2.y, p.x, p.y);
                    curX = x; curY = y;
                    break;
                }
                case 'Z': case 'z': {
                    path.closePath();
                    curX = startX; curY = startY;
                    idx++;
                    break;
                }
                default:
                    idx++;
                    break;
            }
        }
        return path;
    }

    public static Path2D parsePolygonPoints(String pointsStr, double[] viewBox) {
        Path2D path = new Path2D.Double();
        if (pointsStr == null || pointsStr.isBlank()) return path;

        String[] coords = pointsStr.trim().split("[\\s,]+");
        if (coords.length < 4) return path;

        Point2D first = transformSvgToPixel(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), viewBox);
        path.moveTo(first.x, first.y);

        for (int i = 2; i + 1 < coords.length; i += 2) {
            Point2D pt = transformSvgToPixel(Double.parseDouble(coords[i]), Double.parseDouble(coords[i + 1]), viewBox);
            path.lineTo(pt.x, pt.y);
        }
        path.closePath();
        return path;
    }

    public static Path2D parseRect(Element rect, double[] viewBox) {
        double x = parseDimension(rect.getAttribute("x"), 0);
        double y = parseDimension(rect.getAttribute("y"), 0);
        double w = parseDimension(rect.getAttribute("width"), 10);
        double h = parseDimension(rect.getAttribute("height"), 10);

        Path2D path = new Path2D.Double();
        Point2D p1 = transformSvgToPixel(x, y, viewBox);
        Point2D p2 = transformSvgToPixel(x + w, y, viewBox);
        Point2D p3 = transformSvgToPixel(x + w, y + h, viewBox);
        Point2D p4 = transformSvgToPixel(x, y + h, viewBox);

        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        return path;
    }

    public static Path2D parseCircle(Element circle, double[] viewBox) {
        double cx = parseDimension(circle.getAttribute("cx"), 0);
        double cy = parseDimension(circle.getAttribute("cy"), 0);
        double r  = parseDimension(circle.getAttribute("r"), 5);

        Path2D path = new Path2D.Double();
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            double angle = i * 2.0 * Math.PI / steps;
            double px = cx + r * Math.cos(angle);
            double py = cy + r * Math.sin(angle);
            Point2D pt = transformSvgToPixel(px, py, viewBox);
            if (i == 0) path.moveTo(pt.x, pt.y);
            else path.lineTo(pt.x, pt.y);
        }
        path.closePath();
        return path;
    }

    public static class Point2D {
        public double x, y;
        public Point2D(double x, double y) { this.x = x; this.y = y; }
    }

    public static Point2D transformSvgToPixel(double svgX, double svgY, double[] viewBox) {
        double minX = viewBox[0], minY = viewBox[1];
        double vbW  = viewBox[2] > 0 ? viewBox[2] : 1024;
        double vbH  = viewBox[3] > 0 ? viewBox[3] : 512;

        // Map SVG viewBox to Equirectangular Geographic Coordinates [-180..180, -90..90]
        double lng = -180.0 + ((svgX - minX) / vbW) * 360.0;
        double lat = 90.0 - ((svgY - minY) / vbH) * 180.0;

        // Map Geographic Coordinates to Canvas Pixels [0..WIDTH, 0..HEIGHT]
        double px = (lng + 180.0) / 360.0 * WIDTH;
        double py = (90.0 - lat) / 180.0 * HEIGHT;
        return new Point2D(px, py);
    }
    public static double parseDimension(String val, double defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        String clean = val.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
