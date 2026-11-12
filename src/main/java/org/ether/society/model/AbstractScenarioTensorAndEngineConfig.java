/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.util.Locale;


import org.ether.society.config.SimulationPerformanceConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base configuration state for {@link Scenario} encapsulating cultural and geological
 * spatial tensor layers, spatial clipping boundaries, Type B cliodynamic engine parameters,
 * scheduled planetary cataclysms, and deterministic execution toggles.
 *
 * <p>Designed as a transparent JavaBean superclass so Jackson JSON serialization and
 * deserialization preserve a 100% flat, backward-compatible schema.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see Scenario
 */
@SuppressWarnings("serial")
/**
 * AbstractScenarioTensorAndEngineConfig.
 * <p>
 * Configuration and operational parameter specification for the Ether planetary simulation platform.
 * Adheres to the zero-defect code excellence standards and 500-line limit.
 * </p>
 *
 * @author Ether Architectural Working Group
 * @see org.ether.society.core.engine.SimulationEngineModule
 */
public abstract class AbstractScenarioTensorAndEngineConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    // Cultural Vector & Multi-Field Layers (Persisted per Scenario)
    protected int cultureVectorDimensions = 9;
    protected double culturalDiffusionRate = 0.05;
    protected double culturalMutationRate = 0.01;
    protected List<String> customTensorMapsBase64 = new ArrayList<>();
    protected List<Boolean> tensorProceduralModes = new ArrayList<>();
    protected Map<Integer, Long> tensorSeeds = new HashMap<>();
    protected Map<Integer, Map<String, Double>> tensorProceduralParameters = new HashMap<>();
    protected Map<Integer, String> customTensorNames = new HashMap<>();
    protected Map<String, Double> culturalAffinityMatrix = new HashMap<>();

    // Geological & Mineral Energy Extensible Tensor Layers (Persisted per Scenario)
    protected int resourceVectorDimensions = 10;
    protected List<String> customGeologyTensorMapsBase64 = new ArrayList<>();
    protected List<Boolean> geologyTensorProceduralModes = new ArrayList<>();

    // Engine Optimization & Determinism Controls
    protected int h3Resolution = 3;
    protected boolean strictDeterminism = true;
    protected boolean sparseCellSkippingEnabled = false;
    protected boolean oceanMacroAggregationEnabled = false;
    protected boolean coastalNavigationOnlyEnabled = false;
    protected boolean oceanMultiRateTickingEnabled = false;
    protected boolean parallelExecutionEnabled = false;
    protected boolean spatialRangeTruncationEnabled = false;
    protected int climateTickFrequency = 5;
    protected int parallelThreadCount = 0;
    protected boolean tier2CliodynamicsEnabled = true;

    // Type B Procedural & Cliodynamic Engine Checkbox States & Parameters
    protected Map<String, Boolean> typeBEngineStates = new HashMap<>();
    protected Map<String, Map<String, Double>> typeBEngineParameters = new HashMap<>();

    // Scheduled Climate and Planetary Cataclysm Events
    protected List<ClimateEvent> climateEvents = new ArrayList<>();

    // Spatial Clipping & Boundary Conditions
    protected boolean clippingEnabled = false;
    protected double minLat = -90.0;
    protected double maxLat = 90.0;
    protected double minLng = -180.0;
    protected double maxLng = 180.0;
    protected String boundaryMode = "DYNAMIC_RESERVOIR";

    /**
     * Converts this scenario's optimization and determinism settings into a runtime {@link SimulationPerformanceConfig}.
     *
     * @return configured {@link SimulationPerformanceConfig}
     */
    public SimulationPerformanceConfig toPerformanceConfig() {
        SimulationPerformanceConfig config = new SimulationPerformanceConfig();
        config.setStrictDeterminism(strictDeterminism);
        config.setEnableSparseCellSkipping(!strictDeterminism && sparseCellSkippingEnabled);
        config.setEnableOceanMacroAggregation(!strictDeterminism && oceanMacroAggregationEnabled);
        config.setEnableCoastalNavigationOnly(!strictDeterminism && coastalNavigationOnlyEnabled);
        config.setEnableMultiFreqClimateTicks(!strictDeterminism && oceanMultiRateTickingEnabled);
        config.setClimateTickFrequency(strictDeterminism ? 1 : climateTickFrequency);
        config.setEnableParallelExecution(!strictDeterminism && parallelExecutionEnabled);
        config.setParallelThreadCount(parallelExecutionEnabled ? parallelThreadCount : 1);
        config.setEnableSpatialRangeTruncation(!strictDeterminism && spatialRangeTruncationEnabled);
        config.setEnableTier2Cliodynamics(tier2CliodynamicsEnabled);
        return config;
    }

    public int getH3Resolution() { return h3Resolution > 0 ? h3Resolution : 3; }
    public void setH3Resolution(int h3Resolution) { this.h3Resolution = h3Resolution; }

    public boolean isClippingEnabled() { return clippingEnabled; }
    public void setClippingEnabled(boolean clippingEnabled) { this.clippingEnabled = clippingEnabled; }

    public double getMinLat() { return minLat; }
    public void setMinLat(double minLat) { this.minLat = minLat; }

    public double getMaxLat() { return maxLat; }
    public void setMaxLat(double maxLat) { this.maxLat = maxLat; }

    public double getMinLng() { return minLng; }
    public void setMinLng(double minLng) { this.minLng = minLng; }

    public double getMaxLng() { return maxLng; }
    public void setMaxLng(double maxLng) { this.maxLng = maxLng; }

    public String getBoundaryMode() { return boundaryMode; }
    public void setBoundaryMode(String boundaryMode) { this.boundaryMode = boundaryMode; }

    public boolean isStrictDeterminism() { return strictDeterminism; }
    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.sparseCellSkippingEnabled = false;
            this.oceanMultiRateTickingEnabled = false;
            this.parallelExecutionEnabled = false;
            this.spatialRangeTruncationEnabled = false;
        }
    }

    public boolean isSparseCellSkippingEnabled() { return sparseCellSkippingEnabled; }
    public void setSparseCellSkippingEnabled(boolean sparseCellSkippingEnabled) {
        this.sparseCellSkippingEnabled = sparseCellSkippingEnabled;
        if (sparseCellSkippingEnabled) this.strictDeterminism = false;
    }

    public boolean isOceanMacroAggregationEnabled() { return oceanMacroAggregationEnabled; }
    public void setOceanMacroAggregationEnabled(boolean oceanMacroAggregationEnabled) { this.oceanMacroAggregationEnabled = oceanMacroAggregationEnabled; }

    public boolean isCoastalNavigationOnlyEnabled() { return coastalNavigationOnlyEnabled; }
    public void setCoastalNavigationOnlyEnabled(boolean coastalNavigationOnlyEnabled) { this.coastalNavigationOnlyEnabled = coastalNavigationOnlyEnabled; }

    public boolean isOceanMultiRateTickingEnabled() { return oceanMultiRateTickingEnabled; }
    public void setOceanMultiRateTickingEnabled(boolean oceanMultiRateTickingEnabled) {
        this.oceanMultiRateTickingEnabled = oceanMultiRateTickingEnabled;
        if (oceanMultiRateTickingEnabled) this.strictDeterminism = false;
    }

    public boolean isParallelExecutionEnabled() { return parallelExecutionEnabled; }
    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
        if (parallelExecutionEnabled) this.strictDeterminism = false;
    }

    public boolean isSpatialRangeTruncationEnabled() { return spatialRangeTruncationEnabled; }
    public void setSpatialRangeTruncationEnabled(boolean spatialRangeTruncationEnabled) {
        this.spatialRangeTruncationEnabled = spatialRangeTruncationEnabled;
        if (spatialRangeTruncationEnabled) this.strictDeterminism = false;
    }

    public int getClimateTickFrequency() { return climateTickFrequency; }
    public void setClimateTickFrequency(int climateTickFrequency) { this.climateTickFrequency = Math.max(1, climateTickFrequency); }

    public int getParallelThreadCount() { return parallelThreadCount; }
    public void setParallelThreadCount(int parallelThreadCount) { this.parallelThreadCount = Math.max(0, parallelThreadCount); }

    public boolean isTier2CliodynamicsEnabled() { return tier2CliodynamicsEnabled; }
    public void setTier2CliodynamicsEnabled(boolean tier2CliodynamicsEnabled) { this.tier2CliodynamicsEnabled = tier2CliodynamicsEnabled; }

    public Map<String, Boolean> getTypeBEngineStates() {
        if (typeBEngineStates == null) typeBEngineStates = new HashMap<>();
        return typeBEngineStates;
    }
    public void setTypeBEngineStates(Map<String, Boolean> typeBEngineStates) {
        this.typeBEngineStates = typeBEngineStates != null ? typeBEngineStates : new HashMap<>();
    }

    public Map<String, Map<String, Double>> getTypeBEngineParameters() {
        if (typeBEngineParameters == null) typeBEngineParameters = new HashMap<>();
        return typeBEngineParameters;
    }
    public void setTypeBEngineParameters(Map<String, Map<String, Double>> typeBEngineParameters) {
        this.typeBEngineParameters = typeBEngineParameters != null ? typeBEngineParameters : new HashMap<>();
    }

    public int getCultureVectorDimensions() { return cultureVectorDimensions; }
    public void setCultureVectorDimensions(int cultureVectorDimensions) { this.cultureVectorDimensions = cultureVectorDimensions; }

    public Map<Integer, String> getCustomTensorNames() { return customTensorNames; }
    public void setCustomTensorNames(Map<Integer, String> customTensorNames) {
        this.customTensorNames = customTensorNames != null ? customTensorNames : new HashMap<>();
    }

    public String getCustomTensorName(int index) {
        return customTensorNames != null ? customTensorNames.get(index) : null;
    }
    public void setCustomTensorName(int index, String name) {
        if (this.customTensorNames == null) this.customTensorNames = new HashMap<>();
        if (name == null || name.isBlank()) this.customTensorNames.remove(index);
        else this.customTensorNames.put(index, name.trim());
    }

    /**
     * Gets the custom cliodynamic pairwise cultural affinity matrix overrides ($A_{i,j} \in [0.0, 100.0]$).
     *
     * @return map of affinity overrides keyed as {@code "idA:idB"}
     */
    public Map<String, Double> getCulturalAffinityMatrix() {
        if (culturalAffinityMatrix == null) culturalAffinityMatrix = new HashMap<>();
        return culturalAffinityMatrix;
    }
    public void setCulturalAffinityMatrix(Map<String, Double> culturalAffinityMatrix) {
        this.culturalAffinityMatrix = culturalAffinityMatrix != null ? new HashMap<>(culturalAffinityMatrix) : new HashMap<>();
    }

    /**
     * Gets directional cultural affinity from origin polity {@code idA} towards target polity {@code idB},
     * defaulting to $50.0$ (neutral coexistence) if unspecified.
     *
     * <p>Adheres to the directed square matrix architecture where $A(i \to j) \ne A(j \to i)$.</p>
     *
     * @param idA source polity or cultural entity identifier (origin)
     * @param idB target polity or cultural entity identifier (target)
     * @return affinity value in $[0.0, 100.0]$
     */
    public double getPairwiseAffinity(String idA, String idB) {
        if (idA == null || idB == null) return 50.0;
        if (idA.equalsIgnoreCase(idB)) return 100.0;
        if (culturalAffinityMatrix == null || culturalAffinityMatrix.isEmpty()) {
            ensureCulturalAffinityMatrixInitialized();
        }
        if (culturalAffinityMatrix != null) {
            String k1 = idA.toLowerCase(Locale.ROOT) + ":" + idB.toLowerCase(Locale.ROOT);
            Double val = culturalAffinityMatrix.get(k1);
            if (val != null) {
                return val <= 1.0 ? val * 100.0 : val;
            }
            // Fallback for legacy symmetric scenarios where only reverse key was persisted:
            String k2 = idB.toLowerCase(Locale.ROOT) + ":" + idA.toLowerCase(Locale.ROOT);
            val = culturalAffinityMatrix.get(k2);
            if (val != null) {
                return val <= 1.0 ? val * 100.0 : val;
            }
        }
        return 50.0;
    }

    /**
     * Adjusts the directed cultural affinity from origin polity {@code idA} towards target polity {@code idB}.
     *
     * <p>In accordance with directed square matrix semantics, this modifies exclusively the directional link
     * $A(idA \to idB)$ without symmetrically overwriting the reverse link $A(idB \to idA)$.</p>
     *
     * @param idA   source polity or cultural entity identifier
     * @param idB   target polity or cultural entity identifier
     * @param delta affinity shift in percentage points (positive for reconciliation/trade, negative for grievance/war)
     */
    public void adjustPairwiseAffinity(String idA, String idB, double delta) {
        if (idA == null || idB == null) return;
        double current = getPairwiseAffinity(idA, idB);
        double updated = Math.clamp(current + delta, 0.0, 100.0);
        String k1 = idA.toLowerCase(Locale.ROOT) + ":" + idB.toLowerCase(Locale.ROOT);
        if (culturalAffinityMatrix == null) culturalAffinityMatrix = new HashMap<>();
        culturalAffinityMatrix.put(k1, updated);
    }

    private synchronized void ensureCulturalAffinityMatrixInitialized() {
        if (culturalAffinityMatrix != null && !culturalAffinityMatrix.isEmpty()) return;
        try {
            String pKey = (this instanceof Scenario sc && sc.getPlanetPreset() != null) ? sc.getPlanetPreset().name() : "earth";
            long startYear = (this instanceof Scenario sc) ? sc.getStartDateYear() : 0L;
            Map<String, Double> init = AutoEpochScenarioHelper.initializeCulturalAffinityMatrix(pKey, startYear);
            if (init != null && !init.isEmpty()) {
                if (culturalAffinityMatrix == null) culturalAffinityMatrix = new HashMap<>();
                culturalAffinityMatrix.putAll(init);
            }
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(AbstractScenarioTensorAndEngineConfig.class)
                    .debug("Default cultural affinity matrix initialization skipped: {}", ignored.getMessage());
        }
    }

    public double getCulturalDiffusionRate() { return culturalDiffusionRate; }
    public void setCulturalDiffusionRate(double culturalDiffusionRate) { this.culturalDiffusionRate = culturalDiffusionRate; }

    public double getCulturalMutationRate() { return culturalMutationRate; }
    public void setCulturalMutationRate(double culturalMutationRate) { this.culturalMutationRate = culturalMutationRate; }

    public List<String> getCustomTensorMapsBase64() {
        if (customTensorMapsBase64 == null) customTensorMapsBase64 = new ArrayList<>();
        return customTensorMapsBase64;
    }
    public void setCustomTensorMapsBase64(List<String> customTensorMapsBase64) {
        this.customTensorMapsBase64 = customTensorMapsBase64 != null ? customTensorMapsBase64 : new ArrayList<>();
    }

    public List<Boolean> getTensorProceduralModes() {
        if (tensorProceduralModes == null) tensorProceduralModes = new ArrayList<>();
        return tensorProceduralModes;
    }
    public void setTensorProceduralModes(List<Boolean> tensorProceduralModes) {
        this.tensorProceduralModes = tensorProceduralModes != null ? tensorProceduralModes : new ArrayList<>();
    }

    public String getCustomTensorMapBase64(int index) {
        List<String> list = getCustomTensorMapsBase64();
        return (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) ? list.get(index) : null;
    }
    public void setCustomTensorMapBase64(int index, String base64) {
        List<String> list = getCustomTensorMapsBase64();
        while (list.size() <= index) list.add(null);
        list.set(index, base64);
    }

    public Map<Integer, Long> getTensorSeeds() {
        if (tensorSeeds == null) tensorSeeds = new HashMap<>();
        return tensorSeeds;
    }
    public void setTensorSeeds(Map<Integer, Long> tensorSeeds) {
        this.tensorSeeds = tensorSeeds != null ? tensorSeeds : new HashMap<>();
    }

    public Map<Integer, Map<String, Double>> getTensorProceduralParameters() {
        if (tensorProceduralParameters == null) tensorProceduralParameters = new HashMap<>();
        return tensorProceduralParameters;
    }
    public void setTensorProceduralParameters(Map<Integer, Map<String, Double>> tensorProceduralParameters) {
        this.tensorProceduralParameters = tensorProceduralParameters != null ? tensorProceduralParameters : new HashMap<>();
    }

    public int getResourceVectorDimensions() { return resourceVectorDimensions; }
    public void setResourceVectorDimensions(int resourceVectorDimensions) { this.resourceVectorDimensions = resourceVectorDimensions; }

    public List<String> getCustomGeologyTensorMapsBase64() {
        if (customGeologyTensorMapsBase64 == null) customGeologyTensorMapsBase64 = new ArrayList<>();
        return customGeologyTensorMapsBase64;
    }
    public void setCustomGeologyTensorMapsBase64(List<String> customGeologyTensorMapsBase64) {
        this.customGeologyTensorMapsBase64 = customGeologyTensorMapsBase64 != null ? customGeologyTensorMapsBase64 : new ArrayList<>();
    }

    public List<Boolean> getGeologyTensorProceduralModes() {
        if (geologyTensorProceduralModes == null) geologyTensorProceduralModes = new ArrayList<>();
        return geologyTensorProceduralModes;
    }
    public void setGeologyTensorProceduralModes(List<Boolean> geologyTensorProceduralModes) {
        this.geologyTensorProceduralModes = geologyTensorProceduralModes != null ? geologyTensorProceduralModes : new ArrayList<>();
    }

    public String getCustomGeologyTensorMapBase64(int index) {
        List<String> list = getCustomGeologyTensorMapsBase64();
        return (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) ? list.get(index) : null;
    }
    public void setCustomGeologyTensorMapBase64(int index, String base64) {
        List<String> list = getCustomGeologyTensorMapsBase64();
        while (list.size() <= index) list.add(null);
        list.set(index, base64);
    }

    public List<ClimateEvent> getClimateEvents() { return climateEvents; }
    public void setClimateEvents(List<ClimateEvent> climateEvents) {
        this.climateEvents = climateEvents != null ? climateEvents : new ArrayList<>();
    }
}
