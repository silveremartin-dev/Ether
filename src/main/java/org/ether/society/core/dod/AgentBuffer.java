ackage org.ether.society.core.dod;

/**
 * High-performance Data-Oriented Storage for demographic nodes (cohorts).
 * Uses SOA (Structure of Arrays) pattern for GPU compatibility.
 */
public class AgentBuffer {
    /** Internal state: capacity (int). */
    private final int capacity;
    
    // Position & Identification
    private final int[] hexIds; // Index into WorldBuffer
    /** Internal state: h3indexes (long[]). */
    private final long[] h3Indexes;
    
    // Core Demography
    private final float[] mass;         // Population density
    private final float[] energy;       // Internal energy stock
    private final float[] sigmaCost;    // Entropy/Structure cost
    private final float[] techLevel;    // T (Efficiency)
    private final float[] age;          // Cumulative age of the cohort
    private final float[] births;       // Number of births since last cycle
    private final float[] deaths;       // Number of deaths since last cycle
    private final int[] generationCount;
    
    // Genetic Tensor (4 dimensions)
    private final float[][] genetics;
    
    // Cultural Tensor (configurable dimensions)
    private final float[][] culture;

    /**
     * Agent buffer.
     *
     * @param capacity the capacity parameter (int)
     */
    public AgentBuffer(int capacity) {
        this(capacity, 9);
    }

    /**
     * Agent buffer with customizable cultural tensor dimensions.
     *
     * @param capacity the capacity parameter (int)
     * @param cultureDimensions the number of cultural tensor dimensions (clamped between 4 and 32)
     */
    public AgentBuffer(int capacity, int cultureDimensions) {
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
        int cDims = Math.max(4, Math.min(32, cultureDimensions));
        this.culture = new float[cDims][capacity];
        
        // Initialize with -1 to indicate empty slot
        for (int i = 0; i < capacity; i++) {
            hexIds[i] = -1;
        }
    }

    /**
     * Get capacity.
     *
     * @return the resulting computation or state reference
     */
    public int getCapacity() { return capacity; }
    /**
     * Get hex ids.
     *
     * @return the resulting computation or state reference
     */
    public int[] getHexIds() { return hexIds; }
    /**
     * Get h3indexes.
     *
     * @return the resulting computation or state reference
     */
    public long[] getH3Indexes() { return h3Indexes; }
    
    /**
     * Get mass.
     *
     * @return the resulting computation or state reference
     */
    public float[] getMass() { return mass; }
    /**
     * Get energy.
     *
     * @return the resulting computation or state reference
     */
    public float[] getEnergy() { return energy; }
    /**
     * Get sigma cost.
     *
     * @return the resulting computation or state reference
     */
    public float[] getSigmaCost() { return sigmaCost; }
    /**
     * Get tech level.
     *
     * @return the resulting computation or state reference
     */
    public float[] getTechLevel() { return techLevel; }
    /**
     * Get age.
     *
     * @return the resulting computation or state reference
     */
    public float[] getAge() { return age; }
    /**
     * Get births.
     *
     * @return the resulting computation or state reference
     */
    public float[] getBirths() { return births; }
    /**
     * Get deaths.
     *
     * @return the resulting computation or state reference
     */
    public float[] getDeaths() { return deaths; }
    /**
     * Get generation count.
     *
     * @return the resulting computation or state reference
     */
    public int[] getGenerationCount() { return generationCount; }
    
    /**
     * Get genetics.
     *
     * @return the resulting computation or state reference
     */
    public float[][] getGenetics() { return genetics; }
    /**
     * Get culture.
     *
     * @return the resulting computation or state reference
     */
    public float[][] getCulture() { return culture; }
}
