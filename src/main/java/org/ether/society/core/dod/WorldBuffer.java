package org.ether.society.core.dod;

import org.ether.society.model.Biome;

/**
 * High-performance Data-Oriented Storage for world hexagonal grid.
 * Uses flat primitive arrays (AOSOA pattern) for GPU compatibility.
 */
public class WorldBuffer {
    private final int capacity;
    
    // Mapping
    private final long[] h3Indexes;
    private final int[][] neighborIndexes; // [capacity][6] for GPU adjacency
    
    // Terrain & Climate (32-bit floats for GPU)
    private final float[] elevation;
    private final float[] temperature;
    private final float[] rainfall;
    private final byte[] biomes; // Store as ordinal for space
    
    // Resources (Basic)
    private final float[] foodResource;
    private final float[] waterResource;
    private final float[] woodResource;
    private final float[] metalResource;
    private final float[] clayResource;
    
    // Biomass (Detailed)
    private final float[] biomassHuman;
    private final float[] biomassLivestock;
    private final float[] biomassFish;
    private final float[] biomassAgriculture;
    private final float[] biomassNatural;
    
    // Energy (Detailed)
    private final float[] energyWind;
    private final float[] energySolar;
    private final float[] energyFire;
    private final float[] energySlaves;
    private final float[] energyFoodConsumed;
    
    // Socio-Economic Indices
    private final float[] lifespan;
    private final float[] fertility;
    private final float[] giniIndex;
    private final float[] technologyLevel;
    private final float[] resourceCapital;
    
    // Logistics
    private final float[] fluxPressure;
    private final float[] localPrice;
    private final float[] storage;                 // Neolithic storage (granaries)
    private final float[] institutionalComplexity; // Tainter's complexity index

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
    
    public int getCapacity() { return capacity; }
    public long[] getH3Indexes() { return h3Indexes; }
    public int[][] getNeighborIndexes() { return neighborIndexes; }
    public float[] getElevation() { return elevation; }
    public float[] getTemperature() { return temperature; }
    public float[] getRainfall() { return rainfall; }
    public byte[] getBiomes() { return biomes; }
    
    public float[] getFoodResource() { return foodResource; }
    public float[] getWaterResource() { return waterResource; }
    public float[] getWoodResource() { return woodResource; }
    public float[] getMetalResource() { return metalResource; }
    public float[] getClayResource() { return clayResource; }
    
    public float[] getBiomassHuman() { return biomassHuman; }
    public float[] getBiomassLivestock() { return biomassLivestock; }
    public float[] getBiomassFish() { return biomassFish; }
    public float[] getBiomassAgriculture() { return biomassAgriculture; }
    public float[] getBiomassNatural() { return biomassNatural; }
    
    public float[] getEnergyWind() { return energyWind; }
    public float[] getEnergySolar() { return energySolar; }
    public float[] getEnergyFire() { return energyFire; }
    public float[] getEnergySlaves() { return energySlaves; }
    public float[] getEnergyFoodConsumed() { return energyFoodConsumed; }
    
    public float[] getLifespan() { return lifespan; }
    public float[] getFertility() { return fertility; }
    public float[] getGiniIndex() { return giniIndex; }
    public float[] getTechnologyLevel() { return technologyLevel; }
    public float[] getResourceCapital() { return resourceCapital; }
    
    public float[] getFluxPressure() { return fluxPressure; }
    public float[] getLocalPrice() { return localPrice; }
    public float[] getStorage() { return storage; }
    public float[] getInstitutionalComplexity() { return institutionalComplexity; }

    // Index Partitioning for DOD Performance (Land vs. Ocean)
    private int[] landIndices;
    private int[] oceanIndices;

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

    public synchronized int[] getOceanIndices() {
        if (oceanIndices == null) {
            getLandIndices();
        }
        return oceanIndices;
    }

    public void invalidatePartitionIndices() {
        this.landIndices = null;
        this.oceanIndices = null;
    }
}
