package org.ether.society.core.dod;

/**
 * High-performance Data-Oriented Storage for demographic nodes (cohorts).
 * Uses SOA (Structure of Arrays) pattern for GPU compatibility.
 */
public class AgentBuffer {
    /* Internal state variable for capacity (int). */
    private final int capacity;
    
    // Position & Identification
    private final int[] hexIds; // Index into WorldBuffer
    /* Internal state variable for h3indexes (long[]). */
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

    /*
     * Agent buffer.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @param capacity the capacity parameter (int)
     */
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

    /*
     * Get capacity.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public int getCapacity() { return capacity; }
    /*
     * Get hex ids.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public int[] getHexIds() { return hexIds; }
    /*
     * Get h3indexes.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public long[] getH3Indexes() { return h3Indexes; }
    
    /*
     * Get mass.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getMass() { return mass; }
    /*
     * Get energy.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergy() { return energy; }
    /*
     * Get sigma cost.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getSigmaCost() { return sigmaCost; }
    /*
     * Get tech level.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTechLevel() { return techLevel; }
    /*
     * Get age.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getAge() { return age; }
    /*
     * Get births.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBirths() { return births; }
    /*
     * Get deaths.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[] getDeaths() { return deaths; }
    /*
     * Get generation count.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public int[] getGenerationCount() { return generationCount; }
    
    /*
     * Get genetics.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[][] getGenetics() { return genetics; }
    /*
     * Get culture.
     * Enforces physical invariants and updates associated state variables within {@code AgentBuffer}.
     *
     * @return the resulting computation or state reference
     */
    public float[][] getCulture() { return culture; }
}
