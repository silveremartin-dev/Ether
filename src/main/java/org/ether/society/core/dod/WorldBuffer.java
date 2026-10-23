package org.ether.society.core.dod;

import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.model.Biome;

/**
 * High-performance Data-Oriented Storage for world hexagonal grid.
 * Uses flat primitive arrays (AOSOA pattern) for GPU compatibility.
 */
public class WorldBuffer {
    /** Internal state: capacity (int). */
    private final int capacity;
    
    // Mapping
    /** Internal state: h3indexes (long[]). */
    private final long[] h3Indexes;
    private final int[][] neighborIndexes; // [capacity][6] for GPU adjacency
    
    // Terrain & Climate (32-bit floats for GPU)
    /** Internal state: elevation (float[]). */
    private final float[] elevation;
    /** Internal state: temperature (float[]). */
    private final float[] temperature;
    /** Internal state: rainfall (float[]). */
    private final float[] rainfall;
    private final byte[] biomes; // Store as ordinal for space
    
    // Resources (Basic)
    /** Internal state: food resource (float[]). */
    private final float[] foodResource;
    /** Internal state: water resource (float[]). */
    private final float[] waterResource;
    /** Internal state: wood resource (float[]). */
    private final float[] woodResource;
    /** Internal state: metal resource (float[]). */
    private final float[] metalResource;
    /** Internal state: clay resource (float[]). */
    private final float[] clayResource;
    
    // Biomass (Detailed)
    /** Internal state: biomass human (float[]). */
    private final float[] biomassHuman;
    /** Internal state: biomass livestock (float[]). */
    private final float[] biomassLivestock;
    /** Internal state: biomass fish (float[]). */
    private final float[] biomassFish;
    /** Internal state: biomass agriculture (float[]). */
    private final float[] biomassAgriculture;
    /** Internal state: biomass natural (float[]). */
    private final float[] biomassNatural;
    
    // Energy (Detailed)
    /** Internal state: energy wind (float[]). */
    private final float[] energyWind;
    /** Internal state: energy solar (float[]). */
    private final float[] energySolar;
    /** Internal state: energy fire (float[]). */
    private final float[] energyFire;
    /** Internal state: energy slaves (float[]). */
    private final float[] energySlaves;
    /** Internal state: energy food consumed (float[]). */
    private final float[] energyFoodConsumed;
    
    // Socio-Economic Indices
    /** Internal state: lifespan (float[]). */
    private final float[] lifespan;
    /** Internal state: fertility (float[]). */
    private final float[] fertility;
    /** Internal state: gini index (float[]). */
    private final float[] giniIndex;
    /** Internal state: technology level (float[]). */
    private final float[] technologyLevel;
    /** Internal state: resource capital (float[]). */
    private final float[] resourceCapital;
    
    // Logistics
    /** Internal state: flux pressure (float[]). */
    private final float[] fluxPressure;
    /** Internal state: local price (float[]). */
    private final float[] localPrice;
    private final float[] storage;                 // Neolithic storage (granaries)
    private final float[] institutionalComplexity; // Tainter's complexity index

    /**
     * World buffer.
     *
     * @param capacity the capacity parameter (int)
     */
    public WorldBuffer(int capacity) {
        this.capacity = capacity;
        this.h3Indexes = new long[capacity];
        this.neighborIndexes = new int[capacity][6];
        this.elevation = new float[capacity];
        this.temperature = new float[capacity];
        this.rainfall = new float[capacity];
        this.biomes = new byte[capacity];
        
        this.foodResource = new float[capacity];
        this.waterResource = new float[capacity];
        this.woodResource = new float[capacity];
        this.metalResource = new float[capacity];
        this.clayResource = new float[capacity];
        
        this.biomassHuman = new float[capacity];
        this.biomassLivestock = new float[capacity];
        this.biomassFish = new float[capacity];
        this.biomassAgriculture = new float[capacity];
        this.biomassNatural = new float[capacity];
        
        this.energyWind = new float[capacity];
        this.energySolar = new float[capacity];
        this.energyFire = new float[capacity];
        this.energySlaves = new float[capacity];
        this.energyFoodConsumed = new float[capacity];
        
        this.lifespan = new float[capacity];
        this.fertility = new float[capacity];
        this.giniIndex = new float[capacity];
        this.technologyLevel = new float[capacity];
        this.resourceCapital = new float[capacity];
        
        this.fluxPressure = new float[capacity];
        this.localPrice = new float[capacity];
        this.storage = new float[capacity];
        this.institutionalComplexity = new float[capacity];
    }

    // Getters for arrays (to be used by kernels)
    
    /**
     * Get capacity.
     *
     * @return the resulting computation or state reference
     */
    public int getCapacity() { return capacity; }
    /**
     * Get h3indexes.
     *
     * @return the resulting computation or state reference
     */
    public long[] getH3Indexes() { return h3Indexes; }
    /**
     * Get neighbor indexes.
     *
     * @return the resulting computation or state reference
     */
    public int[][] getNeighborIndexes() { return neighborIndexes; }
    /**
     * Get elevation.
     *
     * @return the resulting computation or state reference
     */
    public float[] getElevation() { return elevation; }
    /**
     * Get temperature.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTemperature() { return temperature; }
    /**
     * Get rainfall.
     *
     * @return the resulting computation or state reference
     */
    public float[] getRainfall() { return rainfall; }
    /**
     * Get biomes.
     *
     * @return the resulting computation or state reference
     */
    public byte[] getBiomes() { return biomes; }
    
    /**
     * Get food resource.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFoodResource() { return foodResource; }
    /**
     * Get water resource.
     *
     * @return the resulting computation or state reference
     */
    public float[] getWaterResource() { return waterResource; }
    /**
     * Get wood resource.
     *
     * @return the resulting computation or state reference
     */
    public float[] getWoodResource() { return woodResource; }
    /**
     * Get metal resource.
     *
     * @return the resulting computation or state reference
     */
    public float[] getMetalResource() { return metalResource; }
    /**
     * Get clay resource.
     *
     * @return the resulting computation or state reference
     */
    public float[] getClayResource() { return clayResource; }
    
    /**
     * Get biomass human.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassHuman() { return biomassHuman; }
    /**
     * Get biomass livestock.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassLivestock() { return biomassLivestock; }
    /**
     * Get biomass fish.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassFish() { return biomassFish; }
    /**
     * Get biomass agriculture.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassAgriculture() { return biomassAgriculture; }
    /**
     * Get biomass natural.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassNatural() { return biomassNatural; }
    
    /**
     * Get energy wind.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyWind() { return energyWind; }
    /**
     * Get energy solar.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergySolar() { return energySolar; }
    /**
     * Get energy fire.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyFire() { return energyFire; }
    /**
     * Get energy slaves.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergySlaves() { return energySlaves; }
    /**
     * Get energy food consumed.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyFoodConsumed() { return energyFoodConsumed; }
    
    /**
     * Get lifespan.
     *
     * @return the resulting computation or state reference
     */
    public float[] getLifespan() { return lifespan; }
    /**
     * Get fertility.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFertility() { return fertility; }
    /**
     * Get gini index.
     *
     * @return the resulting computation or state reference
     */
    public float[] getGiniIndex() { return giniIndex; }
    /**
     * Get technology level.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTechnologyLevel() { return technologyLevel; }
    /**
     * Get resource capital.
     *
     * @return the resulting computation or state reference
     */
    public float[] getResourceCapital() { return resourceCapital; }
    
    /**
     * Get flux pressure.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFluxPressure() { return fluxPressure; }
    /**
     * Get local price.
     *
     * @return the resulting computation or state reference
     */
    public float[] getLocalPrice() { return localPrice; }
    /**
     * Get storage.
     *
     * @return the resulting computation or state reference
     */
    public float[] getStorage() { return storage; }
    /**
     * Get institutional complexity.
     *
     * @return the resulting computation or state reference
     */
    public float[] getInstitutionalComplexity() { return institutionalComplexity; }

    // Index Partitioning for DOD Performance (Land vs. Ocean)
    private int[] landIndices;
    private int[] oceanIndices;

    /**
     * Get land indices.
     *
     * @return the resulting computation or state reference
     */
    public synchronized int[] getLandIndices() {
        if (landIndices == null) {
            Biome[] biomeValues = Biome.values();
            int landCount = 0;
            for (int i = 0; i < capacity; i++) {
                int ord = biomes[i] & 0xFF;
                Biome b = (ord < biomeValues.length) ? biomeValues[ord] : Biome.PLAINS;
                if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                    landCount++;
                }
            }
            landIndices = new int[landCount];
            oceanIndices = new int[capacity - landCount];
            int lIdx = 0, oIdx = 0;
            for (int i = 0; i < capacity; i++) {
                int ord = biomes[i] & 0xFF;
                Biome b = (ord < biomeValues.length) ? biomeValues[ord] : Biome.PLAINS;
                if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                    landIndices[lIdx++] = i;
                } else {
                    oceanIndices[oIdx++] = i;
                }
            }
        }
        return landIndices;
    }

    /**
     * Get ocean indices.
     *
     * @return the resulting computation or state reference
     */
    public synchronized int[] getOceanIndices() {
        if (oceanIndices == null) {
            getLandIndices();
        }
        return oceanIndices;
    }

    /**
     * Invalidate partition indices.
     *
     */
    // =========================================================================
    // COMPONENT ADAPTERS (DATA-ORIENTED DESIGN)
    // =========================================================================

    /**
     * Extracts an immutable {@link ClimateState} component for a cell index.
     *
     * @param index cell buffer index
     * @return climate state component
     */
    public ClimateState getClimateState(int index) {
        return new ClimateState(
                this.temperature[index],
                this.rainfall[index],
                0.30,
                0.0,
                0.0
        );
    }

    /**
     * Updates buffer climate variables from an immutable {@link ClimateState} component.
     *
     * @param index cell buffer index
     * @param state climate state component
     */
    public void setClimateState(int index, ClimateState state) {
        if (state == null) return;
        this.temperature[index] = (float) state.temperature();
        this.rainfall[index] = (float) state.rainfall();
    }

    /**
     * Extracts an immutable {@link BiomassState} component for a cell index.
     *
     * @param index cell buffer index
     * @return biomass state component
     */
    public BiomassState getBiomassState(int index) {
        return new BiomassState(
                this.biomassHuman[index],
                this.biomassLivestock[index],
                this.biomassFish[index],
                this.biomassAgriculture[index],
                this.biomassNatural[index],
                this.foodResource[index],
                this.waterResource[index],
                this.woodResource[index],
                10.0,
                0.0
        );
    }

    /**
     * Updates buffer biomass variables from an immutable {@link BiomassState} component.
     *
     * @param index cell buffer index
     * @param state biomass state component
     */
    public void setBiomassState(int index, BiomassState state) {
        if (state == null) return;
        this.biomassHuman[index] = (float) state.biomassHuman();
        this.biomassLivestock[index] = (float) state.biomassLivestock();
        this.biomassFish[index] = (float) state.biomassFish();
        this.biomassAgriculture[index] = (float) state.biomassAgriculture();
        this.biomassNatural[index] = (float) state.biomassNatural();
        this.foodResource[index] = (float) state.foodResource();
        this.waterResource[index] = (float) state.waterResource();
        this.woodResource[index] = (float) state.woodResource();
    }

    /**
     * Extracts an immutable {@link DemographicState} component for a cell index.
     *
     * @param index cell buffer index
     * @return demographic state component
     */
    public DemographicState getDemographicState(int index) {
        return new DemographicState(
                (int) this.biomassHuman[index],
                this.lifespan[index],
                this.fertility[index],
                this.giniIndex[index],
                this.technologyLevel[index],
                1
        );
    }

    /**
     * Updates buffer demographic variables from an immutable {@link DemographicState} component.
     *
     * @param index cell buffer index
     * @param state demographic state component
     */
    public void setDemographicState(int index, DemographicState state) {
        if (state == null) return;
        this.lifespan[index] = (float) state.lifespan();
        this.fertility[index] = (float) state.fertility();
        this.giniIndex[index] = (float) state.giniIndex();
        this.technologyLevel[index] = (float) state.technologyLevel();
    }

    /**
     * Extracts an immutable {@link MineralState} component for a cell index.
     *
     * @param index cell buffer index
     * @return mineral state component
     */
    public MineralState getMineralState(int index) {
        return new MineralState(
                0.0,
                0.0,
                0.0,
                0.0,
                this.metalResource[index],
                0.0,
                0.0,
                87.0,
                0.0,
                0.0
        );
    }

    /**
     * Updates buffer mineral variables from an immutable {@link MineralState} component.
     *
     * @param index cell buffer index
     * @param state mineral state component
     */
    public void setMineralState(int index, MineralState state) {
        if (state == null) return;
        this.metalResource[index] = (float) state.resourceMetal();
    }

    public void invalidatePartitionIndices() {
        this.landIndices = null;
        this.oceanIndices = null;
    }
}
