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
    /** Number of allocated hexagonal cells in this contiguous buffer. */
    private final int capacity;
    
    /** Uber H3 64-bit spatial index for each cell slot. */
    private final long[] h3Indexes;
    /** Precomputed Ring-1 topological neighbor slot indices {@code [capacity][6]} for \(O(1)\) stencil operations. */
    private final int[][] neighborIndexes;
    
    /** Mean surface elevation relative to Mean Sea Level (\(\text{m}\)). */
    private final float[] elevation;
    /** Mean surface air temperature (\(^\circ\text{C}\)). */
    private final float[] temperature;
    /** Annual precipitation depth (\(\text{mm/yr}\)). */
    private final float[] rainfall;
    /** Discrete biome classification ordinal ({@link Biome}). */
    private final byte[] biomes;
    
    /** Available caloric food stockpile (\(\text{GJ}\)). */
    private final float[] foodResource;
    /** Available potable and irrigation water volume (\(10^3\,\text{m}^3\)). */
    private final float[] waterResource;
    /** Available forestry timber and fuelwood stock (\(\text{t}\)). */
    private final float[] woodResource;
    /** Extractable metalliferous ore reserve (\(\text{t}\)). */
    private final float[] metalResource;
    /** Extractable ceramic/silicate clay reserve (\(\text{t}\)). */
    private final float[] clayResource;
    
    /** Extractable fossil coal deposit reserve (\(\text{t}\)). */
    private final float[] resourceCoal;
    /** Extractable crude petroleum/hydrocarbon reserve (\(\text{t}\)). */
    private final float[] resourceOil;
    /** Extractable natural gas reserve (\(10^3\,\text{m}^3\)). */
    private final float[] resourceGas;
    /** Extractable fissile uranium/thorium ore reserve (\(\text{t}\)). */
    private final float[] resourceUranium;
    /** Deep groundwater table aquifer storage capacity and volume (\(10^3\,\text{m}^3\)). */
    private final float[] freshwaterAquifer;
    /** Total topsoil depth (\(\text{m}\)). */
    private final float[] soilDepth;
    /** Soil organic carbon stock (\(\text{t/ha}\)). */
    private final float[] soilOrganicCarbon;
    /** Commercial logistics transport flux throughput volume (\(\text{t/yr}\) or \(\text{GJ/yr}\)). */
    private final float[] commercialFluxVolume;
    
    /** Living human somatic biomass stock (\(\text{t}\)). */
    private final float[] biomassHuman;
    /** Domesticated livestock and draft animal biomass stock (\(\text{t}\)). */
    private final float[] biomassLivestock;
    /** Marine and freshwater ichthyological biomass stock (\(\text{t}\)). */
    private final float[] biomassFish;
    /** Cultivated crop standing agricultural biomass stock (\(\text{t}\)). */
    private final float[] biomassAgriculture;
    /** Wild uncultivated flora and fauna standing biomass stock (\(\text{t}\)). */
    private final float[] biomassNatural;
    
    /** Captured aeolian mechanical/electrical energy (\(\text{GJ/yr}\)). */
    private final float[] energyWind;
    /** Captured radiative solar energy (\(\text{GJ/yr}\)). */
    private final float[] energySolar;
    /** Thermal combustion energy from biomass and fossil fuels (\(\text{GJ/yr}\)). */
    private final float[] energyFire;
    /** Coerced human somatic labor work flux (\(\text{GJ/yr}\)). */
    private final float[] energySlaves;
    /** Endosomatic metabolic caloric energy consumed by the population (\(\text{GJ/yr}\)). */
    private final float[] energyFoodConsumed;
    
    /** Cohort life expectancy at birth \(e_0\) (\(\text{years}\)). */
    private final float[] lifespan;
    /** Total fertility rate (\(\text{births/woman}\)). */
    private final float[] fertility;
    /** Lorenz wealth and caloric distribution Gini coefficient \(\in [0, 1]\). */
    private final float[] giniIndex;
    /** Accumulated physical built capital stock \(K\) (\(\text{kg}\)). */
    private final float[] resourceCapital;
    
    /** Finite-volume thermodynamic migration and logistics pressure gradient. */
    private final float[] fluxPressure;
    /** Local commodity clearing price index. */
    private final float[] localPrice;
    /** Granary and silo preservation storage capacity (\(\text{GJ}\)). */
    private final float[] storage;
    /** Tainter administrative and institutional complexity index \(\in [0, 1]\). */
    private final float[] institutionalComplexity;

    /** Allometric organism profile governing metabolic scaling across cells. */
    private org.ether.society.model.OrganismBiophysicalProfile organismProfile = org.ether.society.model.OrganismBiophysicalProfile.HOMO_SAPIENS;

    /**
     * Allocates a contiguous Structure-of-Arrays (SoA) world buffer for {@code capacity} hexagonal cells.
     *
     * @param capacity total number of H3 cells to allocate
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
        this.resourceCoal = new float[capacity];
        this.resourceOil = new float[capacity];
        this.resourceGas = new float[capacity];
        this.resourceUranium = new float[capacity];
        this.freshwaterAquifer = new float[capacity];
        this.soilDepth = new float[capacity];
        this.soilOrganicCarbon = new float[capacity];
        this.commercialFluxVolume = new float[capacity];
        
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
        return WorldBufferComponentAdapter.computeCellPowerWattsPerCapita(this, index);
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
        return WorldBufferComponentAdapter.computeEmergentTechnologyDiagnostic(this, index);
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
        return WorldBufferComponentAdapter.getClimateState(this, index);
    }

    /**
     * Updates buffer climate variables from an immutable {@link ClimateState} component.
     *
     * @param index cell buffer index
     * @param state climate state component
     */
    public void setClimateState(int index, ClimateState state) {
        WorldBufferComponentAdapter.setClimateState(this, index, state);
    }

    /**
     * Extracts an immutable {@link BiomassState} component for a cell index.
     *
     * @param index cell buffer index
     * @return biomass state component
     */
    public BiomassState getBiomassState(int index) {
        return WorldBufferComponentAdapter.getBiomassState(this, index);
    }

    /**
     * Updates buffer biomass variables from an immutable {@link BiomassState} component.
     *
     * @param index cell buffer index
     * @param state biomass state component
     */
    public void setBiomassState(int index, BiomassState state) {
        WorldBufferComponentAdapter.setBiomassState(this, index, state);
    }

    /**
     * Extracts an immutable {@link DemographicState} component for a cell index.
     *
     * @param index cell buffer index
     * @return demographic state component
     */
    public DemographicState getDemographicState(int index) {
        return WorldBufferComponentAdapter.getDemographicState(this, index);
    }

    /**
     * Updates buffer demographic variables from an immutable {@link DemographicState} component.
     *
     * @param index cell buffer index
     * @param state demographic state component
     */
    public void setDemographicState(int index, DemographicState state) {
        WorldBufferComponentAdapter.setDemographicState(this, index, state);
    }

    /**
     * Extracts an immutable {@link MineralState} component for a cell index.
     *
     * @param index cell buffer index
     * @return mineral state component
     */
    public MineralState getMineralState(int index) {
        return WorldBufferComponentAdapter.getMineralState(this, index);
    }

    /**
     * Updates buffer mineral variables from an immutable {@link MineralState} component.
     *
     * @param index cell buffer index
     * @param state mineral state component
     */
    public void setMineralState(int index, MineralState state) {
        WorldBufferComponentAdapter.setMineralState(this, index, state);
    }

    /** @return extractable coal reserve array */
    public float[] getResourceCoal() { return resourceCoal; }
    /** @return extractable petroleum reserve array */
    public float[] getResourceOil() { return resourceOil; }
    /** @return extractable natural gas reserve array */
    public float[] getResourceGas() { return resourceGas; }
    /** @return extractable uranium reserve array */
    public float[] getResourceUranium() { return resourceUranium; }
    /** @return freshwater aquifer storage array */
    public float[] getFreshwaterAquifer() { return freshwaterAquifer; }
    /** @return topsoil depth array */
    public float[] getSoilDepth() { return soilDepth; }
    /** @return soil organic carbon array */
    public float[] getSoilOrganicCarbon() { return soilOrganicCarbon; }
    /** @return commercial logistics flux volume array */
    public float[] getCommercialFluxVolume() { return commercialFluxVolume; }
}
