/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;

import java.util.List;

/**
 * Skeletal base implementation for {@link SimulationEngineModule} reducing boilerplate across
 * Tier 1 and Tier 2 simulation engines.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public abstract class AbstractSimulationEngine implements SimulationEngineModule {

    private final String id;
    private final String name;
    private final EngineTier tier;

    protected AbstractSimulationEngine(String id, String name, EngineTier tier) {
        this.id = id;
        this.name = name;
        this.tier = tier;
    }

    public String getName() {
        return name();
    }

    public String getDescription() {
        return name() + " simulation engine module.";
    }

    public void process(List<H3Cell> cells, double deltaYears) {
        // Default no-op for purely DoD-based engines; overridden by cellular engines
    }

    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        tick(buffer, deltaYears, deltaYears * 12.0);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public EngineTier tier() {
        return tier;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        if (scenario == null) {
            // Default: Tier 1 always enabled, Tier 2 disabled without explicit scenario
            return tier == EngineTier.TIER_1_BIOPHYSICAL;
        }

        if (scenario.getTypeBEngineStates() != null && scenario.getTypeBEngineStates().containsKey(id)) {
            return Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id));
        }

        // Tier 1 defaults to enabled unless explicitly disabled for ablation studies
        return tier == EngineTier.TIER_1_BIOPHYSICAL;
    }

    @Override
    public void reset() {
        // Default no-op for stateless engines
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (!supportsCellularDerivatives() || buffer == null) return;
        prepareTick(buffer, dtYears);
        int capacity = buffer.getCapacity();
        DerivativeAccumulator acc = new DerivativeAccumulator();
        for (int i = 0; i < capacity; i++) {
            acc.clear();
            accumulateCellularDerivatives(buffer, i, dtYears, dtMonthly, acc);
            applyAccumulatorToCell(buffer, i, acc);
        }
    }

    /**
     * Applies accumulated derivative increments to the specified cell slot in {@link WorldBuffer}.
     *
     * @param buffer contiguous world memory buffer
     * @param i      cell slot index
     * @param acc    computed derivative accumulator
     */
    public static void applyAccumulatorToCell(WorldBuffer buffer, int i, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        if (acc.dResourceCapital != 0.0f) {
            float[] cap = buffer.getResourceCapital();
            cap[i] = (float) Math.max(0.0, cap[i] + acc.dResourceCapital);
        }
        if (acc.dBiomassHuman != 0.0f) {
            float[] pop = buffer.getBiomassHuman();
            pop[i] = (float) Math.max(0.0, pop[i] + acc.dBiomassHuman);
        }
        if (acc.dEnergyFoodConsumed != 0.0f) {
            float[] energy = buffer.getEnergyFoodConsumed();
            energy[i] = (float) Math.max(0.0, energy[i] + acc.dEnergyFoodConsumed);
        }
        if (acc.dEnergyFire != 0.0f) {
            float[] fire = buffer.getEnergyFire();
            fire[i] = (float) Math.max(0.0, fire[i] + acc.dEnergyFire);
        }
        if (acc.dGiniIndex != 0.0f) {
            float[] gini = buffer.getGiniIndex();
            gini[i] = (float) Math.min(1.0, Math.max(0.0, gini[i] + acc.dGiniIndex));
        }
        if (acc.dPollutionLevel != 0.0f) {
            float[] pol = buffer.getPollutionLevel();
            pol[i] = (float) Math.max(0.0, pol[i] + acc.dPollutionLevel);
        }
        if (acc.dFoodResource != 0.0f) {
            float[] food = buffer.getFoodResource();
            food[i] = (float) Math.max(0.0, food[i] + acc.dFoodResource);
        }
        if (acc.dWaterResource != 0.0f) {
            float[] water = buffer.getWaterResource();
            water[i] = (float) Math.max(0.0, water[i] + acc.dWaterResource);
        }
        if (acc.dInstitutionalComplexity != 0.0f) {
            float[] inst = buffer.getInstitutionalComplexity();
            inst[i] = (float) Math.min(1.0, Math.max(0.0, inst[i] + acc.dInstitutionalComplexity));
        }
        if (acc.dBiomassAgriculture != 0.0f) {
            float[] bagr = buffer.getBiomassAgriculture();
            bagr[i] = (float) Math.max(0.0, bagr[i] + acc.dBiomassAgriculture);
        }
        if (acc.dBiomassNatural != 0.0f) {
            float[] bnat = buffer.getBiomassNatural();
            bnat[i] = (float) Math.max(0.0, bnat[i] + acc.dBiomassNatural);
        }
        if (acc.dMetalResource != 0.0f) {
            float[] metal = buffer.getMetalResource();
            metal[i] = (float) Math.max(0.0, metal[i] + acc.dMetalResource);
        }
        if (acc.dFluxPressure != 0.0f) {
            float[] flux = buffer.getFluxPressure();
            flux[i] = (float) Math.max(0.0, flux[i] + acc.dFluxPressure);
        }
        if (acc.dCommercialFluxVolume != 0.0f) {
            float[] comm = buffer.getCommercialFluxVolume();
            comm[i] = (float) Math.max(0.0, comm[i] + acc.dCommercialFluxVolume);
        }
        if (acc.dLifespan != 0.0f) {
            float[] life = buffer.getLifespan();
            life[i] = (float) Math.clamp(life[i] + acc.dLifespan, 15.0, 120.0);
        }
        if (acc.dFertility != 0.0f) {
            float[] fert = buffer.getFertility();
            fert[i] = (float) Math.clamp(fert[i] + acc.dFertility, 0.5, 10.0);
        }
        if (acc.dBiomassFish != 0.0f) {
            float[] fish = buffer.getBiomassFish();
            fish[i] = (float) Math.max(0.0, fish[i] + acc.dBiomassFish);
        }
        if (acc.dResourceCoal != 0.0f) {
            float[] coal = buffer.getResourceCoal();
            coal[i] = (float) Math.max(0.0, coal[i] + acc.dResourceCoal);
        }
        if (acc.dWoodResource != 0.0f) {
            float[] wood = buffer.getWoodResource();
            wood[i] = (float) Math.max(0.0, wood[i] + acc.dWoodResource);
        }
        if (acc.dFreshwaterAquifer != 0.0f) {
            float[] aq = buffer.getFreshwaterAquifer();
            aq[i] = (float) Math.clamp(aq[i] + acc.dFreshwaterAquifer, 0.0, 2000.0);
        }
        if (acc.dSoilOrganicCarbon != 0.0f) {
            float[] soc = buffer.getSoilOrganicCarbon();
            soc[i] = (float) Math.max(0.0, soc[i] + acc.dSoilOrganicCarbon);
        }
    }
}
