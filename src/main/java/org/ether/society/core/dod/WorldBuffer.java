package org.ether.society.core.dod;

import org.ether.society.model.Biome;

/**
 * High-performance Data-Oriented Storage for world hexagonal grid.
 * Uses flat primitive arrays (AOSOA pattern) for GPU compatibility.
 */
public class WorldBuffer {
    /* Internal state variable for capacity (int). */
    private final int capacity;
    
    // Mapping
    /* Internal state variable for h3indexes (long[]). */
    private final long[] h3Indexes;
    private final int[][] neighborIndexes; // [capacity][6] for GPU adjacency
    
    // Terrain & Climate (32-bit floats for GPU)
    /* Internal state variable for elevation (float[]). */
    private final float[] elevation;
    /* Internal state variable for temperature (float[]). */
    private final float[] temperature;
    /* Internal state variable for rainfall (float[]). */
    private final float[] rainfall;
    private final byte[] biomes; // Store as ordinal for space
    
    // Resources (Basic)
    /* Internal state variable for food resource (float[]). */
    private final float[] foodResource;
    /* Internal state variable for water resource (float[]). */
    private final float[] waterResource;
    /* Internal state variable for wood resource (float[]). */
    private final float[] woodResource;
    /* Internal state variable for metal resource (float[]). */
    private final float[] metalResource;
    /* Internal state variable for clay resource (float[]). */
    private final float[] clayResource;
    
    // Biomass (Detailed)
    /* Internal state variable for biomass human (float[]). */
    private final float[] biomassHuman;
    /* Internal state variable for biomass livestock (float[]). */
    private final float[] biomassLivestock;
    /* Internal state variable for biomass fish (float[]). */
    private final float[] biomassFish;
    /* Internal state variable for biomass agriculture (float[]). */
    private final float[] biomassAgriculture;
    /* Internal state variable for biomass natural (float[]). */
    private final float[] biomassNatural;
    
    // Energy (Detailed)
    /* Internal state variable for energy wind (float[]). */
    private final float[] energyWind;
    /* Internal state variable for energy solar (float[]). */
    private final float[] energySolar;
    /* Internal state variable for energy fire (float[]). */
    private final float[] energyFire;
    /* Internal state variable for energy slaves (float[]). */
    private final float[] energySlaves;
    /* Internal state variable for energy food consumed (float[]). */
    private final float[] energyFoodConsumed;
    
    // Socio-Economic Indices
    /* Internal state variable for lifespan (float[]). */
    private final float[] lifespan;
    /* Internal state variable for fertility (float[]). */
    private final float[] fertility;
    /* Internal state variable for gini index (float[]). */
    private final float[] giniIndex;
    /* Internal state variable for technology level (float[]). */
    private final float[] technologyLevel;
    /* Internal state variable for resource capital (float[]). */
    private final float[] resourceCapital;
    
    // Logistics
    /* Internal state variable for flux pressure (float[]). */
    private final float[] fluxPressure;
    /* Internal state variable for local price (float[]). */
    private final float[] localPrice;
    private final float[] storage;                 // Neolithic storage (granaries)
    private final float[] institutionalComplexity; // Tainter's complexity index

    /*
     * World buffer.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
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
    
    /*
     * Get capacity.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public int getCapacity() { return capacity; }
    /*
     * Get h3indexes.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public long[] getH3Indexes() { return h3Indexes; }
    /*
     * Get neighbor indexes.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public int[][] getNeighborIndexes() { return neighborIndexes; }
    /*
     * Get elevation.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getElevation() { return elevation; }
    /*
     * Get temperature.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTemperature() { return temperature; }
    /*
     * Get rainfall.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getRainfall() { return rainfall; }
    /*
     * Get biomes.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public byte[] getBiomes() { return biomes; }
    
    /*
     * Get food resource.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFoodResource() { return foodResource; }
    /*
     * Get water resource.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getWaterResource() { return waterResource; }
    /*
     * Get wood resource.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getWoodResource() { return woodResource; }
    /*
     * Get metal resource.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getMetalResource() { return metalResource; }
    /*
     * Get clay resource.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getClayResource() { return clayResource; }
    
    /*
     * Get biomass human.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassHuman() { return biomassHuman; }
    /*
     * Get biomass livestock.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassLivestock() { return biomassLivestock; }
    /*
     * Get biomass fish.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassFish() { return biomassFish; }
    /*
     * Get biomass agriculture.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassAgriculture() { return biomassAgriculture; }
    /*
     * Get biomass natural.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBiomassNatural() { return biomassNatural; }
    
    /*
     * Get energy wind.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyWind() { return energyWind; }
    /*
     * Get energy solar.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergySolar() { return energySolar; }
    /*
     * Get energy fire.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyFire() { return energyFire; }
    /*
     * Get energy slaves.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergySlaves() { return energySlaves; }
    /*
     * Get energy food consumed.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergyFoodConsumed() { return energyFoodConsumed; }
    
    /*
     * Get lifespan.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getLifespan() { return lifespan; }
    /*
     * Get fertility.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFertility() { return fertility; }
    /*
     * Get gini index.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getGiniIndex() { return giniIndex; }
    /*
     * Get technology level.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTechnologyLevel() { return technologyLevel; }
    /*
     * Get resource capital.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getResourceCapital() { return resourceCapital; }
    
    /*
     * Get flux pressure.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getFluxPressure() { return fluxPressure; }
    /*
     * Get local price.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getLocalPrice() { return localPrice; }
    /*
     * Get storage.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getStorage() { return storage; }
    /*
     * Get institutional complexity.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getInstitutionalComplexity() { return institutionalComplexity; }

    // Index Partitioning for DOD Performance (Land vs. Ocean)
    private int[] landIndices;
    private int[] oceanIndices;

    /*
     * Get land indices.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized int[] getLandIndices() {
        if (landIndices == null) {
            int landCount = 0;
            for (int i = 0; i < capacity; i++) {
                Biome b = Biome.values()[biomes[i]];
                if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                    landCount++;
                }
            }
            landIndices = new int[landCount];
            oceanIndices = new int[capacity - landCount];
            int lIdx = 0, oIdx = 0;
            for (int i = 0; i < capacity; i++) {
                Biome b = Biome.values()[biomes[i]];
                if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                    landIndices[lIdx++] = i;
                } else {
                    oceanIndices[oIdx++] = i;
                }
            }
        }
        return landIndices;
    }

    /*
     * Get ocean indices.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public synchronized int[] getOceanIndices() {
        if (oceanIndices == null) {
            getLandIndices();
        }
        return oceanIndices;
    }

    /*
     * Invalidate partition indices.
     * Enforces physical invariants and updates associated state variables within {@code WorldBuffer}.
     *
     */
    public void invalidatePartitionIndices() {
        this.landIndices = null;
        this.oceanIndices = null;
    }
}
