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
    private final ThematicShaderRenderer thematicShaderRenderer;

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
        this.thematicShaderRenderer = new ThematicShaderRenderer(this.scientificColorMap);
    }

    // --- State setters ---

    public void setScientificColorMap(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
        if (this.thematicShaderRenderer != null) {
            this.thematicShaderRenderer.setScientificColorMap(this.scientificColorMap);
        }
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
                case TECHNOLOGY -> isWater ? Color.rgb(10, 20, 50) : getTechColor((double) worldBuffer.computeEmergentTechnologyDiagnostic(index));
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
            case TECHNOLOGY -> getTechColor(org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell));
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

    public double computeHillshadeFactor(H3Cell cell) {
        return CartographicOpticsHelper.computeHillshadeFactor(cell, showHillshading, h3Service, cellMap);
    }

    public double computeSolarTerminatorFactor(H3Cell cell) {
        return CartographicOpticsHelper.computeSolarTerminatorFactor(cell, showSolarTerminator);
    }

    public static Color blendColors(Color base, Color overlay, double alpha) {
        return CartographicOpticsHelper.blendColors(base, overlay, alpha);
    }

    // --- Domain-specific thematic shaders (delegating to ThematicShaderRenderer) ---

    public Color getHabitatInfrastructureColor(H3Cell cell) { return thematicShaderRenderer.getHabitatInfrastructureColor(cell); }
    public Color getOceanPhColor(H3Cell cell) { return thematicShaderRenderer.getOceanPhColor(cell); }
    public Color getPermafrostColor(H3Cell cell) { return thematicShaderRenderer.getPermafrostColor(cell); }
    public Color getMalthusianPressureColor(H3Cell cell) { return thematicShaderRenderer.getMalthusianPressureColor(cell); }
    public double computeCarryingCapacity(H3Cell c) { return thematicShaderRenderer.computeCarryingCapacity(c); }
    public Color getMineralResourcesColor(H3Cell cell) { return thematicShaderRenderer.getMineralResourcesColor(cell); }
    public Color getMiningExploitationColor(H3Cell cell) { return thematicShaderRenderer.getMiningExploitationColor(cell); }
    public Color getAsabiyyahColor(H3Cell cell) { return thematicShaderRenderer.getAsabiyyahColor(cell); }
    public Color getAgePyramidColor(H3Cell cell) { return thematicShaderRenderer.getAgePyramidColor(cell); }
    public Color getAlbedoColor(H3Cell cell) { return thematicShaderRenderer.getAlbedoColor(cell); }
    public Color getEpidemicColor(H3Cell cell) { return thematicShaderRenderer.getEpidemicColor(cell); }
    public Color getFrictionColor(H3Cell cell) { return thematicShaderRenderer.getFrictionColor(cell); }
    public Color getEnergyCapacityColor(H3Cell cell) { return thematicShaderRenderer.getEnergyCapacityColor(cell); }
    public Color getEntropyPollutionColor(H3Cell cell) { return thematicShaderRenderer.getEntropyPollutionColor(cell); }
    public Color getSoilQualityColor(H3Cell cell) { return thematicShaderRenderer.getSoilQualityColor(cell); }
    public Color getBiodiversityColor(H3Cell cell) { return thematicShaderRenderer.getBiodiversityColor(cell); }
    public Color getHealthLifeExpectancyColor(H3Cell cell) { return thematicShaderRenderer.getHealthLifeExpectancyColor(cell); }
    public Color getEducationLevelColor(H3Cell cell) { return thematicShaderRenderer.getEducationLevelColor(cell); }
    public Color getHappinessColor(H3Cell cell) { return thematicShaderRenderer.getHappinessColor(cell); }
    public Color getConflictColor(H3Cell cell) { return thematicShaderRenderer.getConflictColor(cell); }
    public Color getInstitutionalMaturityColor(H3Cell cell) { return thematicShaderRenderer.getInstitutionalMaturityColor(cell); }
    public Color getGdpWealthColor(H3Cell cell) { return thematicShaderRenderer.getGdpWealthColor(cell); }
    public Color getEliteDensityColor(H3Cell cell) { return thematicShaderRenderer.getEliteDensityColor(cell); }
    public Color getCollectiveMemoryColor(H3Cell cell) { return thematicShaderRenderer.getCollectiveMemoryColor(cell); }
    public Color getCollapseRiskColor(H3Cell cell) { return thematicShaderRenderer.getCollapseRiskColor(cell); }
    public Color getPriceColor(float price) { return thematicShaderRenderer.getPriceColor(price); }
    public Color getPoliticalColor(H3Cell cell) { return thematicShaderRenderer.getPoliticalColor(cell); }
    public Color getBiomeColor(Biome biome) { return thematicShaderRenderer.getBiomeColor(biome); }
    public Color getPopulationColor(int population) { return thematicShaderRenderer.getPopulationColor(population); }
    public Color getPopulationColor(int population, boolean isWater) { return thematicShaderRenderer.getPopulationColor(population, isWater); }
    public Color getFoodColor(double food) { return thematicShaderRenderer.getFoodColor(food); }
    public Color getTemperatureColor(double temp) { return thematicShaderRenderer.getTemperatureColor(temp); }
    public Color getPrecipitationColor(double precipMm) { return thematicShaderRenderer.getPrecipitationColor(precipMm); }
    public Color getTechColor(Double techLevel) { return thematicShaderRenderer.getTechColor(techLevel); }
    public Color getWaterColor(Double water) { return thematicShaderRenderer.getWaterColor(water); }
    public Color getWoodColor(Double wood) { return thematicShaderRenderer.getWoodColor(wood); }
    public Color getGiniColor(Double gini) { return thematicShaderRenderer.getGiniColor(gini); }
    public Color getCapacityColor(H3Cell cell) { return thematicShaderRenderer.getCapacityColor(cell); }
    public Color getMigrationColor(H3Cell cell) { return thematicShaderRenderer.getMigrationColor(cell); }
    public Color getFluxPressureColor(H3Cell cell) { return thematicShaderRenderer.getFluxPressureColor(cell); }
    public Color getCultureColor(H3Cell cell) { return thematicShaderRenderer.getCultureColor(cell); }

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
                    case TECHNOLOGY -> worldBuffer.computeEmergentTechnologyDiagnostic(index);
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
            case TECHNOLOGY -> org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell);
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
