/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.codec;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * High-performance binary serializer and deserializer for immutable "COLD" spatial topology.
 * Encodes static geographic, topological, geological, and demographic baselines (H3 index, lat/lon,
 * elevation, biomes, aquifers, language groups, friction) in a compact, zero-redundancy binary GZIP stream.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CellTopologyWireCodec {
    private static final Logger logger = LoggerFactory.getLogger(CellTopologyWireCodec.class);

    private static final int MAGIC_HEADER = 0x45544854; // "ETHT" (Ether Topology)
    private static final byte PROTOCOL_VERSION = 1;

    /*
     * Serializes cell topology list into compact binary byte array.
     */
    public static byte[] encodeTopology(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(cells.size() * 80);
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeInt(MAGIC_HEADER);
            dos.writeByte(PROTOCOL_VERSION);
            dos.writeInt(cells.size());

            Biome[] biomeValues = Biome.values();

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell cell : cells) {
                dos.writeLong(cell.getH3Index() != null ? cell.getH3Index() : 0L);
                dos.writeDouble(cell.getLatitude() != null ? cell.getLatitude() : 0.0);
                dos.writeDouble(cell.getLongitude() != null ? cell.getLongitude() : 0.0);
                dos.writeFloat(cell.getElevation() != null ? cell.getElevation().floatValue() : 0.0f);

                byte biomeOrdinal = (byte) (cell.getBiome() != null ? cell.getBiome().ordinal() : Biome.PLAINS.ordinal());
                dos.writeByte(biomeOrdinal);

                dos.writeBoolean(Boolean.TRUE.equals(cell.getIsCoastal()));
                dos.writeBoolean(Boolean.TRUE.equals(cell.getIsPolder()));
                dos.writeBoolean(Boolean.TRUE.equals(cell.getHasFloatingInfrastructure()));

                dos.writeFloat(cell.getMovementFriction() != null ? cell.getMovementFriction().floatValue() : 1.0f);
                dos.writeFloat(cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo().floatValue() : 0.30f);
                dos.writeFloat(cell.getMantleHeatFlow() != null ? cell.getMantleHeatFlow().floatValue() : 87.0f);
                dos.writeFloat(cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon().floatValue() : 0.0f);
                dos.writeFloat(cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer().floatValue() : 0.0f);
                dos.writeFloat(cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceMetal() != null ? cell.getResourceMetal().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceClay() != null ? cell.getResourceClay().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceWork() != null ? cell.getResourceWork().floatValue() : 0.0f);
                dos.writeFloat(cell.getCoastalMarineResource() != null ? cell.getCoastalMarineResource().floatValue() : 0.0f);

                String lang = cell.getLanguageGroup() != null ? cell.getLanguageGroup() : "Proto-Human";
                byte[] langBytes = lang.getBytes(StandardCharsets.UTF_8);
                dos.writeShort(langBytes.length);
                dos.write(langBytes);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to encode cell topology binary", e);
            throw new RuntimeException("Topology serialization failed", e);
        }
    }

    /*
     * Saves compressed cell topology to a .bin.gz file.
     */
    public static void saveToFile(Path targetFile, List<H3Cell> cells) throws IOException {
        byte[] rawBytes = encodeTopology(cells);
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        try (OutputStream fos = Files.newOutputStream(targetFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(rawBytes);
            gzos.finish();
        }
        logger.info("Saved compressed world topology ({} cells, {} KB) -> {}",
                cells.size(), rawBytes.length / 1024, targetFile.getFileName());
    }

    /*
     * Decodes binary topology payload into an instantiated list of H3Cells.
     */
    public static List<H3Cell> decodeTopology(byte[] data) {
        if (data == null || data.length < 9) {
            throw new IllegalArgumentException("Corrupted topology data payload (too short)");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        try {
            int magic = dis.readInt();
            if (magic != MAGIC_HEADER) {
                throw new IllegalArgumentException("Invalid topology magic header: 0x" + Integer.toHexString(magic));
            }

            byte version = dis.readByte();
            int cellCount = dis.readInt();

            Biome[] biomeValues = Biome.values();
            List<H3Cell> cells = new ArrayList<>(cellCount);

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (int i = 0; i < cellCount; i++) {
                long h3Index = dis.readLong();
                double lat = dis.readDouble();
                double lon = dis.readDouble();
                float elevation = dis.readFloat();

                byte biomeOrdinal = dis.readByte();
                Biome biome = (biomeOrdinal >= 0 && biomeOrdinal < biomeValues.length)
                        ? biomeValues[biomeOrdinal]
                        : Biome.PLAINS;

                boolean isCoastal = dis.readBoolean();
                boolean isPolder = dis.readBoolean();
                boolean hasFloatingInfra = dis.readBoolean();

                float friction = dis.readFloat();
                float albedo = dis.readFloat();
                float mantleHeat = dis.readFloat();
                float soc = dis.readFloat();
                float freshwater = dis.readFloat();
                float accessible = dis.readFloat();
                float metal = dis.readFloat();
                float preciousMetal = dis.readFloat();
                float clay = dis.readFloat();
                float work = dis.readFloat();
                float coastalMarine = dis.readFloat();

                short langLen = dis.readShort();
                byte[] langBytes = new byte[langLen];
                dis.readFully(langBytes);
                String lang = new String(langBytes, StandardCharsets.UTF_8);

                H3Cell cell = new H3Cell(h3Index, lat, lon);
                cell.setElevation((double) elevation);
                cell.setBiome(biome);
                cell.setIsCoastal(isCoastal);
                cell.setIsPolder(isPolder);
                cell.setHasFloatingInfrastructure(hasFloatingInfra);
                cell.setMovementFriction((double) friction);
                cell.setDynamicAlbedo((double) albedo);
                cell.setMantleHeatFlow((double) mantleHeat);
                cell.setSoilOrganicCarbon((double) soc);
                cell.setFreshwaterAquifer((double) freshwater);
                cell.setAccessibleAquifer((double) accessible);
                cell.setResourceMetal((double) metal);
                cell.setResourcePreciousMetal((double) preciousMetal);
                cell.setResourceClay((double) clay);
                cell.setResourceWork((double) work);
                cell.setCoastalMarineResource((double) coastalMarine);
                cell.setLanguageGroup(lang);

                cells.add(cell);
            }

            return cells;
        } catch (IOException e) {
            logger.error("Failed to decode cell topology binary", e);
            throw new RuntimeException("Topology deserialization failed", e);
        }
    }

    /*
     * Loads compressed cell topology from a .bin.gz file.
     */
    public static List<H3Cell> loadFromFile(Path targetFile) throws IOException {
        if (!Files.exists(targetFile)) {
            throw new FileNotFoundException("Topology file not found: " + targetFile);
        }

        try (InputStream fis = Files.newInputStream(targetFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            byte[] rawBytes = baos.toByteArray();
            List<H3Cell> cells = decodeTopology(rawBytes);
            logger.info("Loaded {} cells from world topology: {}", cells.size(), targetFile.getFileName());
            return cells;
        }
    }
}
