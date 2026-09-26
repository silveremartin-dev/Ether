/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Universal Geospatial Raster Exporter for planetary simulations.
 *
 * <p>Supports both state-of-the-art scientific GIS formats and universal image viewers:</p>
 * <ul>
 *   <li><b>GeoTIFF Float32 (.tif, .tiff)</b>: Full precision 32-bit floating point DEM / tensor raster
 *       with standard GeoTIFF tags (ModelPixelScale, ModelTiepoint, GeoKeyDirectory EPSG:4326) readable in
 *       QGIS, ArcGIS, GDAL, Blender displacement, Unreal Engine, and Unity.</li>
 *   <li><b>16-bit Grayscale PNG (.png + .tfw)</b>: Standard 16-bit unsigned integer heightmap (0..65535)
 *       with ESRI World File georeferencing for 3D terrain engines.</li>
 *   <li><b>High-Definition Polychrome PNG (.png + .tfw)</b>: Universal color visual map readable by everyone.</li>
 *   <li><b>ESRI ASCII Grid (.asc)</b>: Standard geoscientific text grid format.</li>
 *   <li><b>8-bit Grayscale PNG & JPEG (.png, .jpg + .tfw, .jgw)</b>: Web-ready visual images.</li>
 * </ul>
 */
public final class GeospatialRasterExporter {
    private static final Logger logger = LoggerFactory.getLogger(GeospatialRasterExporter.class);

    private GeospatialRasterExporter() {}

    /**
     * Interactive export dialog allowing the user to select their preferred format.
     */
    public static void exportRasterWithDialog(Window parentWindow,
                                             float[][] grid,
                                             String defaultBaseName,
                                             String dialogTitle,
                                             Function<Float, Color> colorMapper,
                                             float minVal,
                                             float maxVal,
                                             String physicalUnit) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            logger.warn("Cannot export empty raster grid.");
            return;
        }

        int height = grid.length;
        int width = grid[0].length;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle != null ? dialogTitle : I18n.getOrDefault("raster.export.title", "Export Geospatial Raster Map"));

        FileChooser.ExtensionFilter filterPngColor = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.png_color", "Universal Color Image (*.png + .tfw)"), "*.png");
        FileChooser.ExtensionFilter filterGeoTiff = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.geotiff", "Scientific GeoTIFF Float32 DEM (*.tif, *.tiff)"), "*.tif", "*.tiff");
        FileChooser.ExtensionFilter filterPng16 = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.png16", "16-bit Grayscale Heightmap (*.png + .tfw)"), "*.png");
        FileChooser.ExtensionFilter filterAscii = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.ascii", "ESRI ASCII Grid (*.asc)"), "*.asc");
        FileChooser.ExtensionFilter filterJpg = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.jpg", "JPEG Image (*.jpg + .jgw)"), "*.jpg", "*.jpeg");

        chooser.getExtensionFilters().addAll(filterPngColor, filterGeoTiff, filterPng16, filterAscii, filterJpg);
        chooser.setInitialFileName(defaultBaseName != null ? defaultBaseName : "raster_map.png");

        File targetFile = chooser.showSaveDialog(parentWindow);
        if (targetFile == null) return;

        FileChooser.ExtensionFilter selectedFilter = chooser.getSelectedExtensionFilter();
        String path = targetFile.getAbsolutePath();
        String lower = path.toLowerCase();

        try {
            if (lower.endsWith(".tif") || lower.endsWith(".tiff") || selectedFilter == filterGeoTiff) {
                if (!lower.endsWith(".tif") && !lower.endsWith(".tiff")) {
                    targetFile = new File(path + ".tif");
                }
                writeFloat32GeoTiff(grid, targetFile, -180.0, 180.0, -90.0, 90.0,
                        "Ether Planetary Simulation Engine - " + (physicalUnit != null ? physicalUnit : "Raw"));
            } else if (lower.endsWith(".asc") || selectedFilter == filterAscii) {
                if (!lower.endsWith(".asc")) {
                    targetFile = new File(path + ".asc");
                }
                double cellSize = 360.0 / width;
                writeEsriAsciiGrid(grid, targetFile, -180.0, -90.0, cellSize, -9999.0f);
            } else if (selectedFilter == filterPng16) {
                if (!lower.endsWith(".png")) {
                    targetFile = new File(path + ".png");
                }
                write16BitPng(grid, minVal, maxVal, targetFile);
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || selectedFilter == filterJpg) {
                if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
                    targetFile = new File(path + ".jpg");
                }
                writeColorJpg(grid, colorMapper, targetFile);
            } else {
                // Default: Universal Polychrome Color PNG with .tfw
                if (!lower.endsWith(".png")) {
                    targetFile = new File(path + ".png");
                }
                writeColorPng(grid, colorMapper, targetFile, true);
            }
            logger.info("Successfully exported raster map ({}x{}) to {}", width, height, targetFile.getAbsolutePath());
        } catch (Exception ex) {
            logger.error("Failed to export raster map to {}", targetFile.getAbsolutePath(), ex);
        }
    }

    /**
     * Writes a 100% compliant Float32 GeoTIFF raster in EPSG:4326 (WGS84 / Planetary Equirectangular).
     */
    public static void writeFloat32GeoTiff(float[][] grid, File targetFile,
                                           double minLon, double maxLon, double minLat, double maxLat,
                                           String description) throws IOException {
        int height = grid.length;
        int width = grid[0].length;
        long dataBytes = (long) width * height * 4L;

        // Image data starts at byte 8 (directly after the 8-byte TIFF header)
        long dataOffset = 8L;
        long ifdOffset = dataOffset + dataBytes;

        // Ensure 2-byte alignment for IFD
        if ((ifdOffset & 1L) != 0L) {
            ifdOffset++;
        }

        // Calculate multi-byte tag offsets located after IFD entries
        int numEntries = 14;
        long ifdSize = 2L + (numEntries * 12L) + 4L; // numEntries(2) + 14 entries(12) + nextIFD(4)
        long extraDataOffset = ifdOffset + ifdSize;

        // Extra tag data payloads
        byte[] softwareBytes = "Ether Planetary Simulation\0".getBytes(StandardCharsets.US_ASCII);
        long softwareOffset = extraDataOffset;
        extraDataOffset += ((softwareBytes.length + 1) / 2) * 2; // 2-byte align

        long modelPixelScaleOffset = extraDataOffset;
        extraDataOffset += 3 * 8; // 3 doubles (24 bytes)

        long modelTiepointOffset = extraDataOffset;
        extraDataOffset += 6 * 8; // 6 doubles (48 bytes)

        long geoKeyDirOffset = extraDataOffset;
        extraDataOffset += 16 * 2; // 16 shorts (32 bytes)

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {

            ByteBuffer buf = ByteBuffer.allocate(65536).order(ByteOrder.LITTLE_ENDIAN);

            // 1. TIFF Header (8 bytes): "II", 42, offset to IFD
            buf.put((byte) 'I');
            buf.put((byte) 'I');
            buf.putShort((short) 42);
            buf.putInt((int) ifdOffset);
            buf.flip();
            bos.write(buf.array(), 0, buf.limit());
            buf.clear();

            // 2. Write Float32 Raw Data (North-to-South, West-to-East)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (buf.remaining() < 4) {
                        buf.flip();
                        bos.write(buf.array(), 0, buf.limit());
                        buf.clear();
                    }
                    buf.putFloat(grid[y][x]);
                }
            }
            if (buf.position() > 0) {
                buf.flip();
                bos.write(buf.array(), 0, buf.limit());
                buf.clear();
            }

            // Align to IFD offset if needed
            long currentPos = dataOffset + dataBytes;
            while (currentPos < ifdOffset) {
                bos.write(0);
                currentPos++;
            }

            // 3. Write IFD (Image File Directory)
            buf.putShort((short) numEntries);

            // Tag 256 (ImageWidth, LONG, 1)
            writeIfdEntry(buf, 256, 4, 1, width);
            // Tag 257 (ImageLength, LONG, 1)
            writeIfdEntry(buf, 257, 4, 1, height);
            // Tag 258 (BitsPerSample, SHORT, 1) -> 32
            writeIfdEntry(buf, 258, 3, 1, 32);
            // Tag 259 (Compression, SHORT, 1) -> 1 (None)
            writeIfdEntry(buf, 259, 3, 1, 1);
            // Tag 262 (PhotometricInterpretation, SHORT, 1) -> 1 (BlackIsZero)
            writeIfdEntry(buf, 262, 3, 1, 1);
            // Tag 273 (StripOffsets, LONG, 1) -> 8
            writeIfdEntry(buf, 273, 4, 1, (int) dataOffset);
            // Tag 277 (SamplesPerPixel, SHORT, 1) -> 1
            writeIfdEntry(buf, 277, 3, 1, 1);
            // Tag 278 (RowsPerStrip, LONG, 1) -> height
            writeIfdEntry(buf, 278, 4, 1, height);
            // Tag 279 (StripByteCounts, LONG, 1) -> width * height * 4
            writeIfdEntry(buf, 279, 4, 1, (int) dataBytes);
            // Tag 305 (Software, ASCII)
            writeIfdEntry(buf, 305, 2, softwareBytes.length, (int) softwareOffset);
            // Tag 339 (SampleFormat, SHORT, 1) -> 3 (IEEE Floating Point)
            writeIfdEntry(buf, 339, 3, 1, 3);
            // Tag 33550 (ModelPixelScaleTag, DOUBLE, 3)
            writeIfdEntry(buf, 33550, 12, 3, (int) modelPixelScaleOffset);
            // Tag 33922 (ModelTiepointTag, DOUBLE, 6)
            writeIfdEntry(buf, 33922, 12, 6, (int) modelTiepointOffset);
            // Tag 34735 (GeoKeyDirectoryTag, SHORT, 16)
            writeIfdEntry(buf, 34735, 3, 16, (int) geoKeyDirOffset);

            // Next IFD offset = 0 (Terminator)
            buf.putInt(0);
            buf.flip();
            bos.write(buf.array(), 0, buf.limit());
            buf.clear();

            // 4. Write Extra Tag Payloads
            // 4a. Software ASCII string
            bos.write(softwareBytes);
            if ((softwareBytes.length & 1) != 0) {
                bos.write(0);
            }

            // 4b. ModelPixelScaleTag (scaleX, scaleY, scaleZ)
            double scaleX = (maxLon - minLon) / (double) width;
            double scaleY = (maxLat - minLat) / (double) height;
            buf.putDouble(scaleX);
            buf.putDouble(scaleY);
            buf.putDouble(0.0);

            // 4c. ModelTiepointTag (I, J, K, X, Y, Z) -> Maps pixel (0,0) to (minLon, maxLat)
            buf.putDouble(0.0);
            buf.putDouble(0.0);
            buf.putDouble(0.0);
            buf.putDouble(minLon);
            buf.putDouble(maxLat);
            buf.putDouble(0.0);

            // 4d. GeoKeyDirectoryTag (Header + 3 Keys: GTModelType=Geographic, GTRasterType=PixelIsArea, GeographicType=EPSG:4326)
            short[] geoKeys = {
                1, 1, 0, 3,       // Header: KeyDirectoryVersion(1), KeyRevision(1), MinorRevision(0), NumberOfKeys(3)
                1024, 0, 1, 2,    // GTModelTypeGeoKey: 2 = ModelTypeGeographic (2D lat/lon)
                1025, 0, 1, 1,    // GTRasterTypeGeoKey: 1 = RasterPixelIsArea
                2048, 0, 1, 4326  // GeographicTypeGeoKey: 4326 = EPSG:4326 (WGS84 / Planetary Sphere)
            };
            for (short k : geoKeys) {
                buf.putShort(k);
            }

            buf.flip();
            bos.write(buf.array(), 0, buf.limit());
            buf.clear();
            bos.flush();
        }
    }

    private static void writeIfdEntry(ByteBuffer buf, int tag, int type, int count, int valueOrOffset) {
        buf.putShort((short) tag);
        buf.putShort((short) type);
        buf.putInt(count);
        buf.putInt(valueOrOffset);
    }

    /**
     * Writes a high-precision 16-bit Grayscale PNG image + ESRI World File (.tfw).
     */
    public static void write16BitPng(float[][] grid, float minVal, float maxVal, File targetPngFile) throws IOException {
        int height = grid.length;
        int width = grid[0].length;
        float range = Math.max(0.0001f, maxVal - minVal);

        BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        short[] data = ((DataBufferUShort) bImg.getRaster().getDataBuffer()).getData();

        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = grid[y][x];
                float norm = Math.max(0.0f, Math.min(1.0f, (val - minVal) / range));
                int u16 = (int) (norm * 65535.0f);
                data[idx++] = (short) (u16 & 0xFFFF);
            }
        }

        ImageIO.write(bImg, "png", targetPngFile);
        writeWorldFile(targetPngFile, width, height, -180.0, 180.0, -90.0, 90.0);
    }

    /**
     * Writes an 8-bit Grayscale PNG image + ESRI World File (.tfw).
     */
    public static void write8BitGrayscalePng(float[][] grid, float minVal, float maxVal, File targetPngFile) throws IOException {
        int height = grid.length;
        int width = grid[0].length;
        float range = Math.max(0.0001f, maxVal - minVal);

        BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = grid[y][x];
                float norm = Math.max(0.0f, Math.min(1.0f, (val - minVal) / range));
                int v = (int) (norm * 255.0f);
                int rgb = (v << 16) | (v << 8) | v;
                bImg.setRGB(x, y, rgb);
            }
        }

        ImageIO.write(bImg, "png", targetPngFile);
        writeWorldFile(targetPngFile, width, height, -180.0, 180.0, -90.0, 90.0);
    }

    /**
     * Writes a full-resolution polychrome Color PNG map + ESRI World File (.tfw).
     */
    public static void writeColorPng(float[][] grid, Function<Float, Color> colorMapper,
                                     File targetPngFile, boolean generateWorldFile) throws IOException {
        int height = grid.length;
        int width = grid[0].length;

        BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = grid[y][x];
                Color c = colorMapper != null ? colorMapper.apply(val) : Color.color(0.5, 0.5, 0.5);
                int a = (int) (c.getOpacity() * 255.0);
                int r = (int) (c.getRed() * 255.0);
                int g = (int) (c.getGreen() * 255.0);
                int b = (int) (c.getBlue() * 255.0);
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                bImg.setRGB(x, y, argb);
            }
        }

        ImageIO.write(bImg, "png", targetPngFile);
        if (generateWorldFile) {
            writeWorldFile(targetPngFile, width, height, -180.0, 180.0, -90.0, 90.0);
        }
    }

    /**
     * Writes a JPEG Color map + ESRI World File (.jgw).
     */
    public static void writeColorJpg(float[][] grid, Function<Float, Color> colorMapper, File targetJpgFile) throws IOException {
        int height = grid.length;
        int width = grid[0].length;

        BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = grid[y][x];
                Color c = colorMapper != null ? colorMapper.apply(val) : Color.color(0.5, 0.5, 0.5);
                int r = (int) (c.getRed() * 255.0);
                int g = (int) (c.getGreen() * 255.0);
                int b = (int) (c.getBlue() * 255.0);
                int rgb = (r << 16) | (g << 8) | b;
                bImg.setRGB(x, y, rgb);
            }
        }

        ImageIO.write(bImg, "jpg", targetJpgFile);
        writeWorldFile(targetJpgFile, width, height, -180.0, 180.0, -90.0, 90.0);
    }

    /**
     * Writes an ESRI ASCII Grid (.asc) matrix with standard header.
     */
    public static void writeEsriAsciiGrid(float[][] grid, File targetAscFile,
                                         double xllCorner, double yllCorner, double cellSize,
                                         float noDataVal) throws IOException {
        int height = grid.length;
        int width = grid[0].length;

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(targetAscFile, StandardCharsets.UTF_8), 65536))) {
            pw.println("NCOLS         " + width);
            pw.println("NROWS         " + height);
            pw.printf(java.util.Locale.US, "XLLCORNER     %.8f%n", xllCorner);
            pw.printf(java.util.Locale.US, "YLLCORNER     %.8f%n", yllCorner);
            pw.printf(java.util.Locale.US, "CELLSIZE      %.8f%n", cellSize);
            pw.printf(java.util.Locale.US, "NODATA_VALUE  %.1f%n", noDataVal);

            for (int y = 0; y < height; y++) {
                StringBuilder sb = new StringBuilder(width * 8);
                for (int x = 0; x < width; x++) {
                    if (x > 0) sb.append(' ');
                    sb.append(String.format(java.util.Locale.US, "%.2f", grid[y][x]));
                }
                pw.println(sb);
            }
        }
    }

    /**
     * Generates an ESRI World File (.tfw for PNG, .jgw for JPG) for accurate GIS alignment.
     */
    public static void writeWorldFile(File targetImageFile, int width, int height,
                                      double minLon, double maxLon, double minLat, double maxLat) {
        String baseName = targetImageFile.getAbsolutePath();
        String worldExt = ".tfw";
        if (baseName.toLowerCase().endsWith(".png")) {
            baseName = baseName.substring(0, baseName.length() - 4);
            worldExt = ".tfw";
        } else if (baseName.toLowerCase().endsWith(".jpg") || baseName.toLowerCase().endsWith(".jpeg")) {
            int dotIdx = baseName.lastIndexOf('.');
            baseName = baseName.substring(0, dotIdx);
            worldExt = ".jgw";
        } else if (baseName.toLowerCase().endsWith(".tif") || baseName.toLowerCase().endsWith(".tiff")) {
            int dotIdx = baseName.lastIndexOf('.');
            baseName = baseName.substring(0, dotIdx);
            worldExt = ".tfw";
        }

        File worldFile = new File(baseName + worldExt);
        double dx = (maxLon - minLon) / (double) width;
        double dy = -(maxLat - minLat) / (double) height;
        double x0 = minLon + (dx / 2.0);
        double y0 = maxLat + (dy / 2.0);

        try (PrintWriter pw = new PrintWriter(new FileWriter(worldFile, StandardCharsets.UTF_8))) {
            pw.printf(java.util.Locale.US, "%.10f%n", dx);
            pw.println("0.0000000000");
            pw.println("0.0000000000");
            pw.printf(java.util.Locale.US, "%.10f%n", dy);
            pw.printf(java.util.Locale.US, "%.10f%n", x0);
            pw.printf(java.util.Locale.US, "%.10f%n", y0);
            logger.info("Generated ESRI World File: {}", worldFile.getAbsolutePath());
        } catch (Exception ex) {
            logger.error("Failed to write ESRI World File {}", worldFile.getAbsolutePath(), ex);
        }
    }
}
