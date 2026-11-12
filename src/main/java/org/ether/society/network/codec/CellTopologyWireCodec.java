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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * High-performance binary serializer and deserializer for cellular world snapshots.
 * Encodes spatial topology alongside conserved dynamic biophysical observables
 * (human population/biomass, built capital, climate, food/water/biomass reserves,
 * energy fluxes, demographic indicators, and fossil/soil reserves) in a compact
 * GZIP binary stream ({@code PROTOCOL_VERSION = 1}).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class CellTopologyWireCodec {
    private static final Logger logger = LoggerFactory.getLogger(CellTopologyWireCodec.class);

    private static final int MAGIC_HEADER = 0x45544854; // "ETHT" (Ether Topology)
    private static final byte PROTOCOL_VERSION = 1;

    /**
     * Serializes cell topology and dynamic cellular state into a compact binary byte array.
     */
    public static byte[] encodeTopology(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            return new byte[0];
        }

        List<H3Cell> validCells = cells.stream().filter(java.util.Objects::nonNull).toList();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(validCells.size() * 176);
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeInt(MAGIC_HEADER);
            dos.writeByte(PROTOCOL_VERSION);
            dos.writeInt(validCells.size());

            for (H3Cell cell : validCells) {
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

                // Full dynamic biophysical observables for post-hoc telemetry extraction
                dos.writeInt(cell.getPopulationOr(0));
                dos.writeDouble(cell.getBiomassHuman() != null ? cell.getBiomassHuman() : 0.0);
                dos.writeFloat(cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture().floatValue() : 0.0f);
                dos.writeFloat(cell.getBiomassNatural() != null ? cell.getBiomassNatural().floatValue() : 0.0f);
                dos.writeFloat(cell.getBiomassLivestock() != null ? cell.getBiomassLivestock().floatValue() : 0.0f);
                dos.writeFloat(cell.getBiomassFish() != null ? cell.getBiomassFish().floatValue() : 0.0f);
                dos.writeFloat(cell.getTemperature() != null ? cell.getTemperature().floatValue() : 14.5f);
                dos.writeFloat(cell.getRainfall() != null ? cell.getRainfall().floatValue() : 500.0f);
                dos.writeFloat(cell.getFoodResource() != null ? cell.getFoodResource().floatValue() : 0.0f);
                dos.writeFloat(cell.getWaterResource() != null ? cell.getWaterResource().floatValue() : 0.0f);
                dos.writeFloat(cell.getWoodResource() != null ? cell.getWoodResource().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceCapital() != null ? cell.getResourceCapital().floatValue() : 0.0f);
                dos.writeFloat(cell.getEnergyWind() != null ? cell.getEnergyWind().floatValue() : 0.0f);
                dos.writeFloat(cell.getEnergySolar() != null ? cell.getEnergySolar().floatValue() : 0.0f);
                dos.writeFloat(cell.getEnergyFire() != null ? cell.getEnergyFire().floatValue() : 0.0f);
                dos.writeFloat(cell.getEnergySlaves() != null ? cell.getEnergySlaves().floatValue() : 0.0f);
                dos.writeFloat(cell.getEnergyFoodConsumed() != null ? cell.getEnergyFoodConsumed().floatValue() : 0.0f);
                dos.writeFloat(cell.getLifespan() != null ? cell.getLifespan().floatValue() : 35.0f);
                dos.writeFloat(cell.getFertility() != null ? cell.getFertility().floatValue() : 4.5f);
                dos.writeFloat(cell.getGiniIndex() != null ? cell.getGiniIndex().floatValue() : 0.20f);
                dos.writeFloat(cell.getResourceCoal() != null ? cell.getResourceCoal().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceOil() != null ? cell.getResourceOil().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceGas() != null ? cell.getResourceGas().floatValue() : 0.0f);
                dos.writeFloat(cell.getResourceUranium() != null ? cell.getResourceUranium().floatValue() : 0.0f);
                dos.writeFloat(cell.getSoilDepth() != null ? cell.getSoilDepth().floatValue() : 1.0f);
                dos.writeFloat(cell.getTradeNetwork() != null ? cell.getTradeNetwork().floatValue() : 0.0f);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to encode cell topology binary", e);
            throw new RuntimeException("Topology serialization failed", e);
        }
    }

    /**
     * Saves compressed cell topology and dynamic snapshot state to a .bin.gz file.
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
        logger.info("Saved compressed world snapshot ({} cells, {} KB) -> {}",
                cells.size(), rawBytes.length / 1024, targetFile.getFileName());
    }

    /**
     * Decodes binary snapshot payload into an instantiated list of H3Cells.
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
            if (version != PROTOCOL_VERSION) {
                throw new IllegalArgumentException("Unsupported topology protocol version: " + version + " (expected " + PROTOCOL_VERSION + ")");
            }
            int cellCount = dis.readInt();

            Biome[] biomeValues = Biome.values();
            List<H3Cell> cells = new ArrayList<>(cellCount);

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

                // Direct reading of dynamic biophysical observables
                cell.setPopulation(dis.readInt());
                cell.setBiomassHuman(dis.readDouble());
                cell.setBiomassAgriculture((double) dis.readFloat());
                cell.setBiomassNatural((double) dis.readFloat());
                cell.setBiomassLivestock((double) dis.readFloat());
                cell.setBiomassFish((double) dis.readFloat());
                cell.setTemperature((double) dis.readFloat());
                cell.setRainfall((double) dis.readFloat());
                cell.setFoodResource((double) dis.readFloat());
                cell.setWaterResource((double) dis.readFloat());
                cell.setWoodResource((double) dis.readFloat());
                cell.setResourceCapital((double) dis.readFloat());
                cell.setEnergyWind((double) dis.readFloat());
                cell.setEnergySolar((double) dis.readFloat());
                cell.setEnergyFire((double) dis.readFloat());
                cell.setEnergySlaves((double) dis.readFloat());
                cell.setEnergyFoodConsumed((double) dis.readFloat());
                cell.setLifespan((double) dis.readFloat());
                cell.setFertility((double) dis.readFloat());
                cell.setGiniIndex((double) dis.readFloat());
                cell.setResourceCoal((double) dis.readFloat());
                cell.setResourceOil((double) dis.readFloat());
                cell.setResourceGas((double) dis.readFloat());
                cell.setResourceUranium((double) dis.readFloat());
                cell.setSoilDepth((double) dis.readFloat());
                cell.setTradeNetwork((double) dis.readFloat());

                cells.add(cell);
            }

            return cells;
        } catch (IOException e) {
            logger.error("Failed to decode cell topology binary", e);
            throw new RuntimeException("Topology deserialization failed", e);
        }
    }

    /**
     * Loads compressed cell snapshot from a .bin.gz file.
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
            logger.info("Loaded {} cells from world snapshot: {}", cells.size(), targetFile.getFileName());
            return cells;
        }
    }
}
