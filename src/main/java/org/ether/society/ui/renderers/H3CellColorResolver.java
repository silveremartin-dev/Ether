/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.ether.society.ui.DisplayMode;
import org.ether.society.model.HabitatType;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.ui.ScientificColorMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High-performance color shader and analytical metric resolver for hexagonal planetary cells.
 *
 * <p>Extracted from {@code H3MapCanvas} to fulfill the Single Responsibility Principle (SRP)
 * by isolating thematic visual rendering, scientific false-color gradients, terrain hillshading,
 * solar terminator computation, and multi-layer optical blending into a dedicated, reusable shader module.</p>
 *
 * <p>Mathematical Foundations:
 * <ul>
 *   <li><b>Solar Terminator:</b> Approximates cosine insolation \(\cos(\theta_z) = \sin(\phi)\sin(\delta) + \cos(\phi)\cos(\delta)\cos(h)\)</li>
 *   <li><b>Hillshading (Lambertian):</b> \(\cos(\psi) = \frac{\nabla z \cdot L}{\|\nabla z\| \|L\|}\)</li>
 *   <li><b>Carrying Capacity:</b> \(K_{\text{cell}} = K_0 \cdot \beta_{\text{biome}} \cdot (1 + \alpha W) \cdot f(T)\)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3CellColorResolver {

    private ScientificColorMap scientificColorMap = ScientificColorMap.TERRAIN;
    private final TerrainLayerRenderer terrainLayerRenderer;
    private final BiomassLayerRenderer biomassLayerRenderer;
    private final PolityBordersRenderer polityBordersRenderer;
    private final HeatmapRenderer heatmapRenderer;

    private WorldBuffer worldBuffer;
    private Map<Long, Integer> h3ToBufferIndexMap = Collections.emptyMap();
    private Map<Long, H3Cell> cellMap = Collections.emptyMap();
    private List<H3Cell> cells = Collections.emptyList();
    private H3Service h3Service;

    private Set<DisplayMode> activeDisplayModes = Collections.emptySet();
    private boolean showHillshading = true;
    private boolean showSolarTerminator = false;
    private boolean smoothMap = false;

    /**
     * Constructs an H3CellColorResolver with default colormap configurations.
     */
    public H3CellColorResolver() {
        this(ScientificColorMap.TERRAIN);
    }

    /**
     * Constructs an H3CellColorResolver with a specified scientific colormap.
     *
     * @param colorMap initial scientific palette
     */
    public H3CellColorResolver(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
        this.terrainLayerRenderer = new TerrainLayerRenderer(this.scientificColorMap);
        this.biomassLayerRenderer = new BiomassLayerRenderer(this.scientificColorMap);
        this.polityBordersRenderer = new PolityBordersRenderer();
        this.heatmapRenderer = new HeatmapRenderer(this.scientificColorMap);
    }

    // --- State setters ---

    public void setScientificColorMap(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
    }

    public ScientificColorMap getScientificColorMap() {
        return scientificColorMap;
    }

    public void setWorldBuffer(WorldBuffer buffer) {
        this.worldBuffer = buffer;
    }

    public void setH3ToBufferIndexMap(Map<Long, Integer> map) {
        this.h3ToBufferIndexMap = map != null ? map : Collections.emptyMap();
    }

    public void setCellMap(Map<Long, H3Cell> map) {
        this.cellMap = map != null ? map : Collections.emptyMap();
    }

    public void setCells(List<H3Cell> cells) {
        this.cells = cells != null ? cells : Collections.emptyList();
    }

    public void setH3Service(H3Service h3Service) {
        this.h3Service = h3Service;
    }

    public void setActiveDisplayModes(Set<DisplayMode> modes) {
        this.activeDisplayModes = modes != null ? modes : Collections.emptySet();
    }

    public void setShowHillshading(boolean show) {
        this.showHillshading = show;
    }

    public void setShowSolarTerminator(boolean show) {
        this.showSolarTerminator = show;
    }

    public void setSmoothMap(boolean smooth) {
        this.smoothMap = smooth;
    }

    // --- High-level color resolution ---

    /**
     * Resolves the primary or blended composite color for a cell based on active display modes and buffer state.
     *
     * @param cell the cell to query
     * @return JavaFX color representation
     */
    public Color getBufferOrCellColor(H3Cell cell) {
        if (cell == null) return Color.BLACK;
        Integer idx = (worldBuffer != null && !h3ToBufferIndexMap.isEmpty()) ? h3ToBufferIndexMap.get(cell.getH3Index()) : null;
        Biome cellBiome;
        boolean isWater;
        if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
            int index = idx;
            cellBiome = Biome.fromOrdinal(worldBuffer.getBiomes()[index] & 0xFF);
            isWater = (cellBiome == Biome.OCEAN || cellBiome == Biome.DEEP_OCEAN || worldBuffer.getElevation()[index] <= 0);
        } else {
            cellBiome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;
            isWater = (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN || (cell.getElevation() != null && cell.getElevation() <= 0));
        }

        if (activeDisplayModes == null || activeDisplayModes.isEmpty()) {
            return getBiomeColor(cellBiome);
        }

        // If BIOME is one of the active layers, use it as the geographic base layer
        if (activeDisplayModes.contains(DisplayMode.BIOME)) {
            Color baseColor = getBiomeColor(cellBiome);
            if (isWater) {
                // For water cells, if water-specific modes are active (like OCEAN_PH), blend them
                for (DisplayMode mode : activeDisplayModes) {
                    if (mode == DisplayMode.OCEAN_PH || mode == DisplayMode.WATER) {
                        Color wCol = getSingleModeColor(mode, cell, idx, cellBiome, isWater);
                        baseColor = blendColors(baseColor, wCol, 0.65);
                    }
                }
                return baseColor;
            }

            // On land: overlay each non-BIOME active layer on top of the terrain
            for (DisplayMode mode : activeDisplayModes) {
                if (mode == DisplayMode.BIOME) continue;

                if (mode == DisplayMode.POPULATION) {
                    int pop = 0;
                    if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
                        pop = (int) worldBuffer.getBiomassHuman()[idx];
                    } else if (cell.getPopulation() != null) {
                        pop = cell.getPopulation();
                    }
                    if (pop > 0) {
                        double norm = Math.clamp(Math.log1p(pop) / Math.log1p(500), 0.0, 1.0);
                        Color popColor = getPopulationColor(pop, false);
                        double alpha = 0.40 + 0.55 * norm;
                        baseColor = blendColors(baseColor, popColor, alpha);
                    }
                } else if (mode == DisplayMode.EPIDEMIC) {
                    int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
                    if (infected > 0) {
                        Color epiColor = getEpidemicColor(cell);
                        baseColor = blendColors(baseColor, epiColor, 0.70);
                    }
                } else if (mode == DisplayMode.POLITICAL) {
                    if (cell.getOwner() != null) {
                        Color polColor = cell.getOwner().getColor();
                        baseColor = blendColors(baseColor, polColor, 0.60);
                    }
                } else {
                    Color overlayCol = getSingleModeColor(mode, cell, idx, cellBiome, isWater);
                    baseColor = blendColors(baseColor, overlayCol, 0.55);
                }
            }
            return baseColor;
        }

        // If BIOME is not active:
        if (activeDisplayModes.size() == 1) {
            DisplayMode single = activeDisplayModes.iterator().next();
            return getSingleModeColor(single, cell, idx, cellBiome, isWater);
        }

        // Multiple non-BIOME layers: start with the first and blend subsequent ones
        Iterator<DisplayMode> it = activeDisplayModes.iterator();
        DisplayMode first = it.next();
        Color composite = getSingleModeColor(first, cell, idx, cellBiome, isWater);
        while (it.hasNext()) {
            DisplayMode next = it.next();
            Color nextCol = getSingleModeColor(next, cell, idx, cellBiome, isWater);
            composite = blendColors(composite, nextCol, 0.50);
        }
        return composite;
    }

    /**
     * Resolves single-layer color mode with DOD buffer awareness.
     */
    public Color getSingleModeColor(DisplayMode mode, H3Cell cell, Integer bufferIndex, Biome cellBiome, boolean isWater) {
        if (bufferIndex != null && worldBuffer != null && bufferIndex >= 0 && bufferIndex < worldBuffer.getCapacity()) {
            int index = bufferIndex;
            return switch (mode) {
                case BIOME -> getBiomeColor(cellBiome);
                case POPULATION -> getPopulationColor((int) worldBuffer.getBiomassHuman()[index], isWater);
                case FOOD -> isWater ? Color.rgb(10, 20, 50) : getFoodColor(worldBuffer.getFoodResource()[index]);
                case TEMPERATURE -> getTemperatureColor(worldBuffer.getTemperature()[index]);
                case PRECIPITATION -> getPrecipitationColor(worldBuffer.getRainfall()[index]);
                case TECHNOLOGY -> isWater ? Color.rgb(10, 20, 50) : getTechColor((double) worldBuffer.getTechnologyLevel()[index]);
                case WATER -> getWaterColor((double) worldBuffer.getWaterResource()[index]);
                case WOOD -> isWater ? Color.rgb(10, 20, 50) : getWoodColor((double) worldBuffer.getWoodResource()[index]);
                case INEQUALITY -> isWater ? Color.rgb(10, 20, 50) : getGiniColor((double) worldBuffer.getGiniIndex()[index]);
                case FLUX -> isWater ? Color.rgb(10, 20, 50) : getPriceColor(worldBuffer.getLocalPrice()[index]);
                default -> getCellColorForMode(mode, cell, isWater);
            };
        }
        return getCellColorForMode(mode, cell, isWater);
    }

    /**
     * Resolves single-layer color mode for standalone cell objects.
     */
    public Color getCellColorForMode(DisplayMode mode, H3Cell cell, boolean isWater) {
        if (cell == null) return Color.BLACK;
        return switch (mode) {
            case MALTHUSIAN_PRESSURE -> getMalthusianPressureColor(cell);
            case MINERAL_RESOURCES -> getMineralResourcesColor(cell);
            case MINING_EXPLOITATION -> getMiningExploitationColor(cell);
            case BIOME -> getBiomeColor(cell.getBiome());
            case POPULATION -> getPopulationColor(cell.getPopulation() != null ? cell.getPopulation() : 0, isWater);
            case FOOD -> getFoodColor(cell.getFoodResource() != null ? cell.getFoodResource() : 0.0);
            case TEMPERATURE -> getTemperatureColor(cell.getTemperature() != null ? cell.getTemperature() : 15.0);
            case PRECIPITATION -> getPrecipitationColor(cell.getRainfall() != null ? cell.getRainfall() : 0.0);
            case TECHNOLOGY -> getTechColor(cell.getTechnologyLevel());
            case WATER -> getWaterColor(cell.getWaterResource());
            case WOOD -> getWoodColor(cell.getWoodResource());
            case INEQUALITY -> getGiniColor(cell.getGiniIndex());
            case CAPACITY -> getCapacityColor(cell);
            case MIGRATION -> getMigrationColor(cell);
            case FLUX -> getFluxPressureColor(cell);
            case CULTURE -> getCultureColor(cell);
            case POLITICAL -> getPoliticalColor(cell);
            case ASABIYYAH -> getAsabiyyahColor(cell);
            case AGE_PYRAMID -> getAgePyramidColor(cell);
            case ALBEDO -> getAlbedoColor(cell);
            case EPIDEMIC -> getEpidemicColor(cell);
            case FRICTION -> getFrictionColor(cell);
            case ENERGY_CAPACITY -> getEnergyCapacityColor(cell);
            case ENTROPY_POLLUTION -> getEntropyPollutionColor(cell);
            case SOIL_QUALITY -> getSoilQualityColor(cell);
            case BIODIVERSITY -> getBiodiversityColor(cell);
            case HEALTH_LIFE_EXPECTANCY -> getHealthLifeExpectancyColor(cell);
            case EDUCATION_LEVEL -> getEducationLevelColor(cell);
            case HAPPINESS -> getHappinessColor(cell);
            case CONFLICT -> getConflictColor(cell);
            case INSTITUTIONAL_MATURITY -> getInstitutionalMaturityColor(cell);
            case GDP_WEALTH -> getGdpWealthColor(cell);
            case ELITE_DENSITY -> getEliteDensityColor(cell);
            case COLLECTIVE_MEMORY -> getCollectiveMemoryColor(cell);
            case COLLAPSE_RISK -> getCollapseRiskColor(cell);
            case OCEAN_PH -> getOceanPhColor(cell);
            case PERMAFROST -> getPermafrostColor(cell);
            case HABITAT_INFRASTRUCTURE -> getHabitatInfrastructureColor(cell);
            default -> getBiomeColor(cell.getBiome());
        };
    }

    /**
     * Compute Inverse Distance Weighting (IDW) spatial continuous color interpolation.
     */
    public Color getIdwInterpolatedColor(H3Cell cell, int cellIndex) {
        Color baseCol = getBufferOrCellColor(cell);
        if (!smoothMap || cells == null || cells.isEmpty()) return baseCol;

        int neighborRange = 3;
        int minIdx = Math.max(0, cellIndex - neighborRange);
        int maxIdx = Math.min(cells.size() - 1, cellIndex + neighborRange);

        double totalWeight = 1.0;
        double rWeighted = baseCol.getRed();
        double gWeighted = baseCol.getGreen();
        double bWeighted = baseCol.getBlue();

        double cLat = cell.getLatitude();
        double cLng = cell.getLongitude();

        for (int j = minIdx; j <= maxIdx; j++) {
            if (j == cellIndex) continue;
            H3Cell neighbor = cells.get(j);
            double dLat = neighbor.getLatitude() - cLat;
            double dLng = neighbor.getLongitude() - cLng;
            double distSq = dLat * dLat + dLng * dLng;

            if (distSq < 25.0) {
                double w = 1.0 / (distSq + 0.15);
                Color nCol = getBufferOrCellColor(neighbor);
                rWeighted += nCol.getRed() * w;
                gWeighted += nCol.getGreen() * w;
                bWeighted += nCol.getBlue() * w;
                totalWeight += w;
            }
        }

        return Color.color(
                Math.clamp(rWeighted / totalWeight, 0.0, 1.0),
                Math.clamp(gWeighted / totalWeight, 0.0, 1.0),
                Math.clamp(bWeighted / totalWeight, 0.0, 1.0),
                baseCol.getOpacity()
        );
    }

    /**
     * Computes Lambertian hillshading illumination factor based on topographical elevation gradients.
     */
    public double computeHillshadeFactor(H3Cell cell) {
        if (cell == null || !showHillshading) return 1.0;
        double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
        if (elev <= 0) return 1.0;

        double dzDx = 0.0;
        double dzDy = 0.0;

        if (h3Service != null && cellMap != null) {
            try {
                List<Long> neighbors = h3Service.getNeighbors(cell.getH3Index());
                if (neighbors != null && !neighbors.isEmpty()) {
                    double cLat = cell.getLatitude();
                    double cLng = cell.getLongitude();
                    double cosLat = Math.cos(Math.toRadians(cLat));
                    int validNeighbors = 0;
                    for (Long nIdx : neighbors) {
                        H3Cell neighbor = cellMap.get(nIdx);
                        if (neighbor != null) {
                            double nElev = neighbor.getElevation() != null ? neighbor.getElevation() : 0.0;
                            double dLng = (neighbor.getLongitude() - cLng) * cosLat;
                            double dLat = neighbor.getLatitude() - cLat;
                            double dist = Math.hypot(dLng, dLat);
                            if (dist > 1e-4) {
                                double slope = (nElev - elev) / (dist * 111320.0);
                                dzDx += slope * (dLng / dist);
                                dzDy += slope * (dLat / dist);
                                validNeighbors++;
                            }
                        }
                    }
                    if (validNeighbors > 0) {
                        dzDx /= validNeighbors;
                        dzDy /= validNeighbors;
                    }
                }
            } catch (Exception ignored) {
                // Ignore topology errors during asynchronous updates
            }
        }

        double azimuthRad = Math.toRadians(315.0); // Light from North-West
        double altitudeRad = Math.toRadians(45.0);
        double slopeRad = Math.atan(Math.hypot(dzDx, dzDy) * 5.0);
        double aspectRad = Math.atan2(dzDy, -dzDx);

        double cosIncidence = Math.sin(altitudeRad) * Math.cos(slopeRad)
                + Math.cos(altitudeRad) * Math.sin(slopeRad) * Math.cos(azimuthRad - aspectRad);

        return Math.clamp(0.45 + 0.55 * Math.max(0.0, cosIncidence), 0.35, 1.25);
    }

    /**
     * Computes the astronomical solar terminator diurnal day/night shading factor.
     */
    public double computeSolarTerminatorFactor(H3Cell cell) {
        if (cell == null || !showSolarTerminator) return 1.0;
        double latRad = Math.toRadians(cell.getLatitude());
        double subSolarLatRad = Math.toRadians(12.0); // Mild summer tilt approximation
        double subSolarLng = 0.0; // Prime meridian subsolar point
        double lngDiffRad = Math.toRadians(cell.getLongitude() - subSolarLng);

        double cosZenith = Math.sin(latRad) * Math.sin(subSolarLatRad)
                + Math.cos(latRad) * Math.cos(subSolarLatRad) * Math.cos(lngDiffRad);

        if (cosZenith > 0.05) {
            return 1.0; // Full daylight
        } else if (cosZenith < -0.10) {
            return 0.30; // Deep night
        } else {
            // Twilight penumbra transition band
            double t = (cosZenith - (-0.10)) / 0.15;
            return 0.30 + 0.70 * t;
        }
    }

    /**
     * Linearly blends two JavaFX colors according to an alpha weight \(\in [0, 1]\).
     */
    public static Color blendColors(Color base, Color overlay, double alpha) {
        if (alpha <= 0.0 || overlay == null) return base;
        if (alpha >= 1.0 || base == null) return overlay;
        double inv = 1.0 - alpha;
        double r = base.getRed() * inv + overlay.getRed() * alpha;
        double g = base.getGreen() * inv + overlay.getGreen() * alpha;
        double b = base.getBlue() * inv + overlay.getBlue() * alpha;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    // --- Domain-specific thematic shaders ---

    public Color getHabitatInfrastructureColor(H3Cell cell) {
        return terrainLayerRenderer.getHabitatInfrastructureColor(cell);
    }

    public Color getOceanPhColor(H3Cell cell) {
        return terrainLayerRenderer.getOceanPhColor(cell);
    }

    public Color getPermafrostColor(H3Cell cell) {
        return terrainLayerRenderer.getPermafrostColor(cell);
    }

    public Color getMalthusianPressureColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 40, 90);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 45, 60);

        double capK = computeCarryingCapacity(cell);
        double ratio = capK > 0 ? (double) pop / capK : 2.0;

        if (ratio < 0.5) return Color.rgb(16, 185, 129); // Green / Abundance (< 50%)
        else if (ratio < 1.0) return Color.rgb(234, 179, 8);  // Yellow / Balanced (50%-100%)
        else if (ratio < 1.5) return Color.rgb(249, 115, 22); // Orange / Malthusian Strain (100%-150%)
        else return Color.rgb(239, 68, 68);  // Red / Critical Overpopulation (> 150%)
    }

    public double computeCarryingCapacity(H3Cell c) {
        if (c == null || c.getElevation() == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        Biome b = c.getBiome();
        if (b == Biome.DESERT || b == Biome.TUNDRA || b == Biome.SNOW) baseCap *= 0.1;
        else if (b == Biome.PLAINS || b == Biome.FOREST) baseCap *= 1.5;
        else if (b == Biome.JUNGLE) baseCap *= 0.8;

        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }
        double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
        baseCap *= Math.max(0.5, tech * 0.8);
        return Math.max(10.0, baseCap);
    }

    public Color getMineralResourcesColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 30, 70);
        double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
        double precious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
        double clay = cell.getResourceClay() != null ? cell.getResourceClay() : 0.0;
        double totalMinerals = metal + precious * 3.0 + clay * 0.5;

        if (totalMinerals <= 10.0) return Color.rgb(45, 55, 72); // Slate / Unmineralized

        double norm = Math.min(1.0, totalMinerals / 600.0);
        if (precious > 50.0) {
            return Color.rgb((int)(220 + norm * 35), (int)(180 + norm * 70), (int)(20 + norm * 80));
        }
        int r = (int) (60 + norm * 180);
        int g = (int) (120 + norm * 110);
        int b = (int) (160 - norm * 100);
        return Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
    }

    public Color getMiningExploitationColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
        double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

        double miningActivity = Math.min(capital, metal * 0.8 + 10.0);
        if (miningActivity <= 2.0) return Color.rgb(35, 45, 60);

        double norm = Math.min(1.0, miningActivity / 200.0);
        int r = (int) (120 + norm * 135);
        int g = (int) (70 + norm * 120);
        int b = (int) (20 + norm * 40);
        return Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
    }

    public Color getAsabiyyahColor(H3Cell cell) {
        return polityBordersRenderer.getAsabiyyahColor(cell);
    }

    public Color getAgePyramidColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 30, 40);
        double seniorRatio = (double) (cell.getPop65to79() + cell.getPop80Plus()) / pop;
        return Color.color(seniorRatio, 0.4, 1.0 - seniorRatio);
    }

    public Color getAlbedoColor(H3Cell cell) {
        return terrainLayerRenderer.getAlbedoColor(cell);
    }

    public Color getEpidemicColor(H3Cell cell) {
        return heatmapRenderer.getEpidemicColor(cell);
    }

    public Color getFrictionColor(H3Cell cell) {
        double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
        double norm = Math.min(1.0, (friction - 0.5) / 8.0);
        return Color.color(norm, 1.0 - norm, 0.1);
    }

    public Color getEnergyCapacityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 55);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double norm = Math.min(1.0, tech / 10.0);
        return Color.color(0.1 + norm * 0.9, 0.4 + norm * 0.5, 0.2);
    }

    public Color getEntropyPollutionColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(10, 20, 40);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double pollution = (pop * 0.05 + tech * 1.5) / 500.0;
        double norm = Math.min(1.0, pollution);
        return Color.color(norm, 0.2, 0.8 * (1.0 - norm));
    }

    public Color getSoilQualityColor(H3Cell cell) {
        return biomassLayerRenderer.getSoilQualityColor(cell);
    }

    public Color getBiodiversityColor(H3Cell cell) {
        return biomassLayerRenderer.getBiodiversityColor(cell);
    }

    public Color getHealthLifeExpectancyColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 40, 50);
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double life = Math.min(1.0, (food / Math.max(1, pop)) / 5.0);
        return Color.color(1.0 - life, life * 0.8 + 0.2, 0.4);
    }

    public Color getEducationLevelColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 20, 40);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double edu = Math.min(1.0, tech / 8.0);
        return Color.color(0.2 + edu * 0.6, 0.3, 0.5 + edu * 0.5);
    }

    public Color getHappinessColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 30, 50);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
        double happy = Math.max(0.0, 1.0 - gini * 1.5);
        return Color.color(1.0 - happy, happy, 0.2);
    }

    public Color getConflictColor(H3Cell cell) {
        return polityBordersRenderer.getConflictColor(cell);
    }

    public Color getInstitutionalMaturityColor(H3Cell cell) {
        return polityBordersRenderer.getInstitutionalMaturityColor(cell);
    }

    public Color getGdpWealthColor(H3Cell cell) {
        return heatmapRenderer.getGdpWealthColor(cell);
    }

    public Color getEliteDensityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.2;
        double elite = Math.min(1.0, gini * 1.8);
        return Color.color(0.2 + elite * 0.8, 0.2, 0.5 + elite * 0.5);
    }

    public Color getCollectiveMemoryColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double mem = Math.min(1.0, tech / 10.0);
        return Color.color(0.1, 0.5 + mem * 0.5, 0.8 + mem * 0.2);
    }

    public Color getCollapseRiskColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double capK = computeCarryingCapacity(cell);
        double ratio = capK > 0 ? (double) pop / capK : 1.0;
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.2;
        double risk = Math.min(1.0, Math.max(0.0, (ratio - 0.8) * 2.0 + gini));
        return Color.color(risk, 1.0 - risk, 0.1);
    }

    public Color getPriceColor(float price) {
        float normalized = Math.clamp(price / 10.0f, 0.0f, 1.0f);
        if (scientificColorMap != null) {
            return scientificColorMap.getColor(normalized);
        }
        return Color.color(normalized, 1.0f - normalized, 0);
    }

    public Color getPoliticalColor(H3Cell cell) {
        if (cell.getOwner() == null) {
            return getBiomeColor(cell.getBiome()).desaturate().darker();
        }
        return cell.getOwner().getColor();
    }

    public Color getBiomeColor(Biome biome) {
        return terrainLayerRenderer.getBiomeColor(biome);
    }

    public Color getPopulationColor(int population) {
        return heatmapRenderer.getPopulationColor(population, false);
    }

    public Color getPopulationColor(int population, boolean isWater) {
        return heatmapRenderer.getPopulationColor(population, isWater);
    }

    public Color getFoodColor(double food) {
        return biomassLayerRenderer.getFoodColor(food);
    }

    public Color getTemperatureColor(double temp) {
        return heatmapRenderer.getTemperatureColor(temp);
    }

    public Color getPrecipitationColor(double precipMm) {
        return heatmapRenderer.getPrecipitationColor(precipMm);
    }

    public Color getTechColor(Double techLevel) {
        return heatmapRenderer.getTechColor(techLevel);
    }

    public Color getWaterColor(Double water) {
        return biomassLayerRenderer.getWaterColor(water);
    }

    public Color getWoodColor(Double wood) {
        return biomassLayerRenderer.getWoodColor(wood);
    }

    public Color getGiniColor(Double gini) {
        return heatmapRenderer.getGiniColor(gini);
    }

    public Color getCapacityColor(H3Cell cell) {
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;

        double capacity = (food / 100.0) * (0.3 + Math.min(1.0, water / 1000.0) * 0.7);
        double normalized = Math.min(1.0, capacity / 10.0);

        if (normalized < 0.2) {
            return Color.rgb(80, 40, 40);
        } else if (normalized < 0.5) {
            double t = (normalized - 0.2) / 0.3;
            return Color.rgb(80 + (int) (t * 100), 40 + (int) (t * 80), 40);
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (180 - t * 130), (int) (120 + t * 100), (int) (40 + t * 40));
        }
    }

    public Color getMigrationColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;

        if (pop == 0) {
            return Color.rgb(30, 30, 30);
        }

        double foodPerCapita = food / Math.max(1, pop);
        double pressure = 1.0 - Math.min(1.0, foodPerCapita / (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * 2.0));

        if (pressure > 0.7) {
            return Color.rgb(200, 50, 50);
        } else if (pressure > 0.4) {
            return Color.rgb(200, 200, 50);
        } else if (pressure > 0.2) {
            return Color.rgb(100, 150, 100);
        } else {
            return Color.rgb(50, 100, 200);
        }
    }

    public Color getFluxPressureColor(H3Cell cell) {
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
        double consumption = (cell.getPopulation() != null ? cell.getPopulation() : 0) * 2.0;

        if (consumption == 0) return Color.GREEN;

        double ratio = food / consumption;
        if (ratio >= 2.0) return Color.BLUE;
        if (ratio >= 1.0) {
            double t = Math.min(1.0, ratio - 1.0);
            return Color.rgb(0, (int) ((1 - t) * 255), (int) (t * 255));
        }

        double t = Math.max(0, ratio);
        return Color.rgb((int) ((1 - t) * 255), (int) (t * 255), 0);
    }

    public Color getCultureColor(H3Cell cell) {
        return polityBordersRenderer.getCultureColor(cell);
    }

    // --- Thematic and statistical queries ---

    public boolean hasThematicOverlay(H3Cell cell, DisplayMode primaryMode) {
        if (cell == null || activeDisplayModes == null) return false;
        if (!activeDisplayModes.contains(DisplayMode.BIOME) || activeDisplayModes.size() <= 1) {
            return false;
        }
        for (DisplayMode mode : activeDisplayModes) {
            if (mode == DisplayMode.BIOME) continue;
            double v = getCellDisplayValue(cell, mode, primaryMode);
            if (v > 0.05) return true;
        }
        return false;
    }

    public double getCellDisplayValue(H3Cell cell, DisplayMode mode, DisplayMode primaryMode) {
        if (mode == null) mode = primaryMode;
        if (worldBuffer != null && cell != null && !h3ToBufferIndexMap.isEmpty()) {
            Integer idx = h3ToBufferIndexMap.get(cell.getH3Index());
            if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
                int index = idx;
                return switch (mode) {
                    case POPULATION -> worldBuffer.getBiomassHuman()[index];
                    case FOOD -> worldBuffer.getFoodResource()[index];
                    case TEMPERATURE -> worldBuffer.getTemperature()[index];
                    case PRECIPITATION -> worldBuffer.getRainfall()[index];
                    case TECHNOLOGY -> worldBuffer.getTechnologyLevel()[index];
                    case WATER -> worldBuffer.getWaterResource()[index];
                    case WOOD -> worldBuffer.getWoodResource()[index];
                    case INEQUALITY -> worldBuffer.getGiniIndex()[index];
                    case FLUX -> worldBuffer.getLocalPrice()[index];
                    default -> cell.getElevation() != null ? cell.getElevation() : 0.0;
                };
            }
        }
        if (cell == null) return 0.0;
        return switch (mode) {
            case POPULATION -> cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0;
            case FOOD -> cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            case TEMPERATURE -> cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            case PRECIPITATION -> cell.getRainfall() != null ? cell.getRainfall() : 800.0;
            case TECHNOLOGY -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            case MALTHUSIAN_PRESSURE -> cell.getPopulation() != null && computeCarryingCapacity(cell) > 0 ? (double) cell.getPopulation() / computeCarryingCapacity(cell) : 0.0;
            case MINERAL_RESOURCES -> (cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0) + (cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() * 3.0 : 0.0);
            case MINING_EXPLOITATION -> Math.min(cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0, (cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0) + 10.0);
            case WATER -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
            case WOOD -> cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            case INEQUALITY -> cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
            case FLUX -> cell.getFluxPressure();
            case ALBEDO -> cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
            case FRICTION -> cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            case EPIDEMIC -> cell.getEpidemicInfected() != null ? cell.getEpidemicInfected().doubleValue() : 0.0;
            case AGE_PYRAMID -> (cell.getPop65to79() != null && cell.getPop80Plus() != null) ? (double)(cell.getPop65to79() + cell.getPop80Plus()) : 0.0;
            case ASABIYYAH -> cell.getOwner() != null ? cell.getOwner().getAsabiyyah() : 0.0;
            case HABITAT_INFRASTRUCTURE -> cell.getHabitatType() != HabitatType.NONE ? (cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() * 100.0 : 100.0) : 0.0;
            default -> cell.getElevation() != null ? cell.getElevation() : 0.0;
        };
    }
}
