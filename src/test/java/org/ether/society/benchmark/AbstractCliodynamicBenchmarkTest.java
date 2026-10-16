/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.EnvironmentalKernel;
import org.ether.society.core.dod.InvariantConservationGuard;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized Abstract Base Class for Intermediate Century-Scale Cliodynamic Benchmarks.
 *
 * <p>Provides pre-calibrated contiguous topological meshes, memory buffer initializers,
 * symplectic simulation execution loops, and automated physical/demographic invariant checking
 * over multi-century timescales ($50$ to $500$ years).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public abstract class AbstractCliodynamicBenchmarkTest {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractCliodynamicBenchmarkTest.class);

    protected H3Core h3;
    protected DemographicKernel demographicKernel;
    protected EnvironmentalKernel environmentalKernel;
    protected InvariantConservationGuard conservationGuard;

    @BeforeEach
    public void setUpHarness() throws IOException {
        this.h3 = H3Core.newInstance();
        this.demographicKernel = new DemographicKernel();
        this.environmentalKernel = new EnvironmentalKernel();
        this.conservationGuard = new InvariantConservationGuard(1e-4, true);
    }

    /**
     * Creates a contiguous continental river basin (37 cells, ring radius 3 at H3 resolution 4).
     * Ideal for agrarian diffusion, local river transport, and carrying capacity checks.
     *
     * @param centerLat latitude of basin center (e.g. 31.0 for Nile Delta / Fertile Crescent)
     * @param centerLng longitude of basin center (e.g. 31.0)
     * @param initialPopPerCell initial population per cell
     * @return contiguous cell mesh
     */
    protected List<H3Cell> createContiguousBasin(double centerLat, double centerLng, double initialPopPerCell) {
        return createTopologicalMesh(centerLat, centerLng, 4, 3, initialPopPerCell, Biome.PLAINS, 2.0);
    }

    /**
     * Creates an isolated island ecosystem (127 cells, ring radius 6 at H3 resolution 4, e.g. Madagascar).
     * Ideal for closed-boundary Malthusian stress, insular allometry, and trophic over-exploitation.
     *
     * @param centerLat latitude of island center
     * @param centerLng longitude of island center
     * @param initialPopPerCell initial population per cell
     * @return contiguous island cell mesh
     */
    protected List<H3Cell> createIsolatedIsland(double centerLat, double centerLng, double initialPopPerCell) {
        return createTopologicalMesh(centerLat, centerLng, 4, 6, initialPopPerCell, Biome.FOREST, 1.5);
    }

    /**
     * Generates a contiguous H3 grid disk with realistic physical and ecological properties.
     */
    protected List<H3Cell> createTopologicalMesh(double centerLat, double centerLng, int resolution, int ringRadius,
                                                 double initialPopPerCell, Biome defaultBiome, double techLevel) {
        long center = h3.latLngToCell(centerLat, centerLng, resolution);
        List<Long> diskH3 = h3.gridDisk(center, ringRadius);

        List<H3Cell> cells = new ArrayList<>(diskH3.size());
        for (Long idx : diskH3) {
            LatLng coord = h3.cellToLatLng(idx);
            H3Cell cell = new H3Cell(idx, coord.lat, coord.lng);
            cell.setElevation(200.0);
            cell.setTemperature(20.0);
            cell.setRainfall(900.0);
            cell.setBiome(defaultBiome != null ? defaultBiome : Biome.PLAINS);
            cell.setFoodResource(5000.0);
            cell.setWaterResource(2000.0);
            cell.setBiomassNatural(8000.0);
            cell.setBiomassAgriculture(1000.0);
            cell.setResourceCapital(500.0);
            cell.setTechnologyLevel(techLevel);
            cell.setPopulation((int) initialPopPerCell);
            cells.add(cell);
        }
        return cells;
    }

    /**
     * Converts an {@link H3Cell} topology into a high-performance Data-Oriented {@link WorldBuffer}.
     */
    protected WorldBuffer populateWorldBuffer(List<H3Cell> cells) {
        int count = cells.size();
        WorldBuffer buffer = new WorldBuffer(count);

        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            buffer.getH3Indexes()[i] = c.getH3Index();
            buffer.getBiomassHuman()[i] = c.getPopulation() != null ? (float) c.getPopulation() : 0.0f;
            buffer.getBiomassAgriculture()[i] = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture().floatValue() : 500.0f;
            buffer.getBiomassNatural()[i] = c.getBiomassNatural() != null ? c.getBiomassNatural().floatValue() : 2000.0f;
            buffer.getFoodResource()[i] = c.getFoodResource() != null ? c.getFoodResource().floatValue() : 3000.0f;
            buffer.getResourceCapital()[i] = c.getResourceCapital() != null ? c.getResourceCapital().floatValue() : 200.0f;
            buffer.getGiniIndex()[i] = 0.30f;
            buffer.getTechnologyLevel()[i] = c.getTechnologyLevel() != null ? c.getTechnologyLevel().floatValue() : 1.5f;
            buffer.getTemperature()[i] = c.getTemperature() != null ? c.getTemperature().floatValue() : 20.0f;
            buffer.getRainfall()[i] = c.getRainfall() != null ? c.getRainfall().floatValue() : 900.0f;
            buffer.getBiomes()[i] = (byte) c.getBiome().ordinal();
        }
        return buffer;
    }

    /**
     * Initializes cohort agent storage matching active cell populations.
     */
    protected AgentBuffer populateAgentBuffer(WorldBuffer world) {
        int capacity = world.getCapacity();
        AgentBuffer agents = new AgentBuffer(capacity);

        for (int i = 0; i < capacity; i++) {
            if (world.getH3Indexes()[i] == 0) continue;
            float pop = world.getBiomassHuman()[i];
            if (pop > 0.0f) {
                agents.getHexIds()[i] = i;
                agents.getMass()[i] = pop;
                agents.getEnergy()[i] = 200.0f;
                agents.getTechLevel()[i] = world.getTechnologyLevel()[i];
            }
        }
        return agents;
    }

    /**
     * Executes a multi-year simulation sweep with combined environmental and demographic kernels,
     * validating physical integrity at every step.
     *
     * @param world global WorldBuffer
     * @param agents cohort AgentBuffer
     * @param totalYears total simulation horizon in years
     * @param dtDays time step duration in days (e.g. 30.0f)
     */
    protected void runMultiYearSimulation(WorldBuffer world, AgentBuffer agents, int totalYears, float dtDays) {
        int totalTicks = (int) (totalYears * (365.25f / dtDays));
        logger.info("Starting multi-year cliodynamic benchmark: {} years ({} ticks, dt={} days)", totalYears, totalTicks, dtDays);

        for (int tick = 1; tick <= totalTicks; tick++) {
            var pre = conservationGuard.captureSnapshot(world);

            // 1. Environmental primary production and food regeneration
            environmentalKernel.tick(world, dtDays);

            // 2. Demographic metabolism and consumption
            demographicKernel.tick(world, agents, dtDays);

            // 3. Automated physical invariant verification
            var post = conservationGuard.captureSnapshot(world);
            CliodynamicAssertions.assertPhysicalIntegrity(world);
        }
    }
}
