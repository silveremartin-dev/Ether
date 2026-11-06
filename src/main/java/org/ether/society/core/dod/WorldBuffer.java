package org.ether.society.core.dod;

import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.model.Biome;
import org.ether.society.core.math.SmoothMath;

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

    /** Allometric organism profile governing metabolic scaling across cells. */
    private org.ether.society.model.OrganismBiophysicalProfile organismProfile = org.ether.society.model.OrganismBiophysicalProfile.HOMO_SAPIENS;

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
     * Retrieves the 2D neighbor adjacency table across all cells.
     *
     * @return 2D array mapping each cell index to its contiguous topological neighbor indices
     */
    public int[][] getNeighborIndexes() { return neighborIndexes; }

    /** @return topographic elevation buffer in meters above sea level (m) */
    public float[] getElevation() { return elevation; }

    /** @return thermodynamic mean annual surface temperature buffer in degrees Celsius (°C) */
    public float[] getTemperature() { return temperature; }

    /** @return annual precipitation rainfall buffer in millimeters per year (mm/yr) */
    public float[] getRainfall() { return rainfall; }

    /** @return discrete biome categorical classification ordinal identifiers */
    public byte[] getBiomes() { return biomes; }

    /** @return local available edible food biomass stock buffer in gigacalories (Gcal) */
    public float[] getFoodResource() { return foodResource; }

    /** @return local available potable freshwater flux buffer in cubic meters (m³) */
    public float[] getWaterResource() { return waterResource; }

    /** @return local standing timber / combustible fuelwood biomass buffer in tons dry matter */
    public float[] getWoodResource() { return woodResource; }

    /** @return accessible structural and industrial metal reserves buffer in metric tons */
    public float[] getMetalResource() { return metalResource; }

    /** @return ceramic clay and earthen structural minerals buffer in metric tons */
    public float[] getClayResource() { return clayResource; }

    /** @return total human / conscious living agent population count (individuals) */
    public float[] getBiomassHuman() { return biomassHuman; }

    /** @return domesticated livestock biomass stock in metric tons */
    public float[] getBiomassLivestock() { return biomassLivestock; }

    /** @return marine and freshwater harvestable fish biomass stock in metric tons */
    public float[] getBiomassFish() { return biomassFish; }

    /** @return cultivated crop standing agricultural biomass in metric tons */
    public float[] getBiomassAgriculture() { return biomassAgriculture; }

    /** @return primary wild vegetative ecosystem biomass stock in metric tons */
    public float[] getBiomassNatural() { return biomassNatural; }

    /** @return kinetic wind energy flux captured in megawatt-hours (MWh) */
    public float[] getEnergyWind() { return energyWind; }

    /** @return incident solar irradiance energy flux captured in megawatt-hours (MWh) */
    public float[] getEnergySolar() { return energySolar; }

    /** @return combustible thermal fire / furnace exergy captured in megawatt-hours (MWh) */
    public float[] getEnergyFire() { return energyFire; }

    /** @return forced somatic captive labor energy flux in megawatt-hours (MWh) */
    public float[] getEnergySlaves() { return energySlaves; }

    /** @return metabolic food energy ingested by human population in gigajoules (GJ) */
    public float[] getEnergyFoodConsumed() { return energyFoodConsumed; }

    /** @return demographic cohort life expectancy at birth in years (yr) */
    public float[] getLifespan() { return lifespan; }

    /** @return demographic total fertility rate (children per female) */
    public float[] getFertility() { return fertility; }

    /** @return Lorenz curve economic Gini inequality coefficient [0.0..1.0] */
    public float[] getGiniIndex() { return giniIndex; }

    /**
     * Retrieves the legacy technology level buffer array.
     *
     * @return raw float array representing technology levels for backwards compatibility
     */
    public float[] getTechnologyLevel() { return technologyLevel; }

    /** @return gross physical built structural capital stock in physical units (K) */
    public float[] getResourceCapital() { return resourceCapital; }

    /** @return spatial logistic transport flux pressure gradient */
    public float[] getFluxPressure() { return fluxPressure; }

    /** @return marginal commodity clearing price index */
    public float[] getLocalPrice() { return localPrice; }

    /** @return non-perishable stored grain and staple food reserve in metric tons */
    public float[] getStorage() { return storage; }

    /** @return Tainter administrative bureaucracy and institutional complexity index */
    public float[] getInstitutionalComplexity() { return institutionalComplexity; }

    /**
     * Retrieves the allometric biophysical organism profile governing somatic energetics.
     *
     * @return active organism biophysical profile
     */
    public org.ether.society.model.OrganismBiophysicalProfile getOrganismProfile() { return organismProfile; }

    /**
     * Updates the allometric biophysical organism profile.
     *
     * @param p organism biophysical profile (defaults to Homo Sapiens if non-null)
     */
    public void setOrganismProfile(org.ether.society.model.OrganismBiophysicalProfile p) { if (p != null) this.organismProfile = p; }

    /**
     * Calculates total somatic and exosomatic mechanical and thermal power flux per individual in Watts,
     * calibrated to Vaclav Smil (2017) <i>Energy and Civilization: A History</i>:
     * $$P_{\text{watts/cap}} = B_{\text{basal}} + P_{\text{somatic}} + P_{\text{hearth}} + P_{\text{draft}}(k_{\text{pc}}) + P_{\text{mill}}(k_{\text{pc}}) + P_{\text{steam}}(k_{\text{pc}}) + P_{\text{grid}}(k_{\text{pc}})$$
     *
     * @param index cell buffer index
     * @return total continuous power dissipated per capita in Watts (\(\text{W}\))
     */
    public float computeCellPowerWattsPerCapita(int index) {
        if (index < 0 || index >= capacity) return 0.0f;
        float pop = biomassHuman[index];
        if (pop <= 0.001f) return 0.0f;
        double basalW = organismProfile.basalMetabolicPowerWatts();
        double kpc = Math.max(0.0, resourceCapital[index] / pop);
        // 1. Somatic basal + mechanical labor + domestic hearth/firewood thermal baseline (~347 W/cap in foraging bands, Smil 2017)
        double woodThermalW = 212.0 * Math.tanh((biomassNatural[index] + woodResource[index] + 50.0) / 50.0);
        // 2. Agrarian draft traction & plows (activates smoothly above kpc ~ 150 kg/cap, up to +25 W/cap global average)
        double draftW = 25.0 * Math.tanh(SmoothMath.softplus(kpc - 150.0, 0.05) / 250.0);
        // 3. Waterwheels, windmills, sail & proto-industrial metallurgical furnaces (activates smoothly above kpc ~ 520 kg/cap, up to +220 W/cap)
        double millAndCharcoalW = 220.0 * Math.tanh(SmoothMath.softplus(kpc - 520.0, 0.05) / 420.0);
        // 4. Coal/steam & early internal combustion prime movers (activates smoothly above kpc ~ 2500 kg/cap, up to +600 W/cap)
        double steamCoalW = 600.0 * Math.tanh(SmoothMath.softplus(kpc - 2500.0, 0.02) / 2500.0);
        // 5. Modern electric/hydrocarbon/nuclear/solar grid (activates smoothly above kpc ~ 5000 kg/cap, up to +1350 W/cap)
        double modernGridW = 1350.0 * Math.tanh(SmoothMath.softplus(kpc - 5000.0, 0.01) / 10000.0);
        return (float) (basalW + 35.0 + woodThermalW + draftW + millAndCharcoalW + steamCoalW + modernGridW);
    }

    /**
     * Computes the strictly <i>a posteriori</i> Garrett-Lotka emergent exergy-capital diagnostic index
     * for a cell buffer index:
     * $$T_{\text{emergent}} = 0.50 + 1.45 \log_{10}(\mathcal{M}) + 0.58 \log_{10}\!\left(1 + \frac{k_{\text{active}}}{k_0}\right)$$
     *
     * @param index primitive cell buffer index
     * @return dimensionless emergent technology diagnostic in \([0.0, 10.0]\)
     */
    public float computeEmergentTechnologyDiagnostic(int index) {
        if (index < 0 || index >= capacity) return 0.0f;
        if (technologyLevel != null && technologyLevel[index] > 0.0f) {
            return technologyLevel[index];
        }
        float pop = biomassHuman[index];
        if (pop <= 0.001f) return 0.0f;
        double powerW = computeCellPowerWattsPerCapita(index);
        double kpc = Math.max(0.0, resourceCapital[index] / pop);
        return (float) org.ether.society.database.H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(
                powerW, kpc, organismProfile.basalMetabolicPowerWatts());
    }

    /**
     * Computes the strictly <i>a posteriori</i> Garrett-Lotka emergent exergy-capital diagnostic index
     * for a nullable boxed cell buffer index.
     *
     * @param index boxed cell buffer index
     * @return dimensionless emergent technology diagnostic in \([0.0, 10.0]\) (0.0f if null)
     */
    public float computeEmergentTechnologyDiagnostic(Integer index) {
        return index != null ? computeEmergentTechnologyDiagnostic(index.intValue()) : 0.0f;
    }

    // Index Partitioning for DOD Performance (Land vs. Ocean)
    private int[] landIndices;
    private int[] oceanIndices;

    /**
     * Retrieves the cached array of cell indices classified as terrestrial land (non-oceanic biomes).
     * Lazily populates partition cache on first invocation.
     *
     * @return array of land cell buffer indices
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
     * Retrieves the cached array of cell indices classified as marine waters ({@link Biome#OCEAN} and {@link Biome#DEEP_OCEAN}).
     *
     * @return array of marine cell buffer indices
     */
    public synchronized int[] getOceanIndices() {
        if (oceanIndices == null) {
            getLandIndices();
        }
        return oceanIndices;
    }

    /**
     * Invalidates the cached land and ocean partition indices, triggering lazy recalculation
     * upon next query if cell biome classifications are modified dynamically.
     */
    public void invalidatePartitionIndices() {
        this.landIndices = null;
        this.oceanIndices = null;
    }
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
}
