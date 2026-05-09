package org.ether.society.core.dod;

/**
 * High-performance Data-Oriented Storage for demographic nodes (cohorts).
 * Uses SOA (Structure of Arrays) pattern for GPU compatibility.
 */
public class AgentBuffer {
    private final int capacity;
    
    // Position & Identification
    private final int[] hexIds; // Index into WorldBuffer
    private final long[] h3Indexes;
    
    // Core Démographie
    private final float[] mass;         // Population density
    private final float[] energy;       // Internal energy stock
    private final float[] sigmaCost;    // Entropy/Structure cost
    private final float[] techLevel;    // T (Efficiency)
    private final float[] age;          // Cumulative age of the cohort
    private final float[] births;       // Number of births since last cycle
    private final float[] deaths;       // Number of deaths since last cycle
    private final int[] generationCount;
    
    // Tenseur Génétique (4 dimensions)
    private final float[][] genetics;
    
    // Tenseur Culturel (4 dimensions)
    private final float[][] culture;

    public AgentBuffer(int capacity) {
        this.capacity = capacity;
        this.hexIds = new int[capacity];
        this.h3Indexes = new long[capacity];
        
        this.mass = new float[capacity];
        this.energy = new float[capacity];
        this.sigmaCost = new float[capacity];
        this.techLevel = new float[capacity];
        this.age = new float[capacity];
        this.births = new float[capacity];
        this.deaths = new float[capacity];
        this.generationCount = new int[capacity];
        
        this.genetics = new float[4][capacity];
        this.culture = new float[4][capacity];
        
        // Initialize with -1 to indicate empty slot
        for (int i = 0; i < capacity; i++) {
            hexIds[i] = -1;
        }
    }

    public int getCapacity() { return capacity; }
    public int[] getHexIds() { return hexIds; }
    public long[] getH3Indexes() { return h3Indexes; }
    
    public float[] getMass() { return mass; }
    public float[] getEnergy() { return energy; }
    public float[] getSigmaCost() { return sigmaCost; }
    public float[] getTechLevel() { return techLevel; }
    public float[] getAge() { return age; }
    public float[] getBirths() { return births; }
    public float[] getDeaths() { return deaths; }
    public int[] getGenerationCount() { return generationCount; }
    
    public float[][] getGenetics() { return genetics; }
    public float[][] getCulture() { return culture; }
}
