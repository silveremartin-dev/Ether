/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.ui.ScientificColorMap;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

/**
 * <h1>ThematicShaderRenderer</h1>
 * <p>
 * Domain-specific thematic shaders and raster color calculations for H3 cells across
 * socioeconomic, ecological, epidemiological, and geophysical indicator layers.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThematicShaderRenderer {

    private final TerrainLayerRenderer terrainLayerRenderer;
    private final BiomassLayerRenderer biomassLayerRenderer;
    private final PolityBordersRenderer polityBordersRenderer;
    private final HeatmapRenderer heatmapRenderer;
    private ScientificColorMap scientificColorMap;

    public ThematicShaderRenderer(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
        this.terrainLayerRenderer = new TerrainLayerRenderer(this.scientificColorMap);
        this.biomassLayerRenderer = new BiomassLayerRenderer(this.scientificColorMap);
        this.polityBordersRenderer = new PolityBordersRenderer();
        this.heatmapRenderer = new HeatmapRenderer(this.scientificColorMap);
    }

    public void setScientificColorMap(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
    }

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
        double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
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
        double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell);
        double norm = Math.min(1.0, tech / 10.0);
        return Color.color(0.1 + norm * 0.9, 0.4 + norm * 0.5, 0.2);
    }

    public Color getEntropyPollutionColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(10, 20, 40);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell);
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
        double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell);
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
        double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell);
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
}
