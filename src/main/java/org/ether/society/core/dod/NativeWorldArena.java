/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Zero-Copy Native Off-Heap Arena managing direct unmanaged memory segments for a
 * {@link WorldBuffer} and {@link AgentBuffer} across high-frequency simulation ticks.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NativeWorldArena implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NativeWorldArena.class);

        private final Arena arena;
        private final int capacity;
        private final int agentCapacity;
        private final MemorySegment worldStructSeg;
        private final MemorySegment agentStructSeg;

        // World primitive arrays segments
        private final MemorySegment h3IndexesSeg;
        private final MemorySegment neighborIndexesSeg;
        private final MemorySegment elevationSeg;
        private final MemorySegment temperatureSeg;
        private final MemorySegment rainfallSeg;
        private final MemorySegment biomesSeg;
        private final MemorySegment foodResourceSeg;
        private final MemorySegment waterResourceSeg;
        private final MemorySegment woodResourceSeg;
        private final MemorySegment metalResourceSeg;
        private final MemorySegment clayResourceSeg;
        private final MemorySegment biomassHumanSeg;
        private final MemorySegment biomassLivestockSeg;
        private final MemorySegment biomassFishSeg;
        private final MemorySegment biomassAgricultureSeg;
        private final MemorySegment biomassNaturalSeg;
        private final MemorySegment energyWindSeg;
        private final MemorySegment energySolarSeg;
        private final MemorySegment energyFireSeg;
        private final MemorySegment energySlavesSeg;
        private final MemorySegment energyFoodConsumedSeg;
        private final MemorySegment lifespanSeg;
        private final MemorySegment fertilitySeg;
        private final MemorySegment giniIndexSeg;
        /** Reserved C-ABI alignment segment (offset 200); unused by physical observables kernels. */
        private final MemorySegment reservedWorldSlotSeg;
        private final MemorySegment resourceCapitalSeg;
        private final MemorySegment fluxPressureSeg;
        private final MemorySegment localPriceSeg;
        private final MemorySegment storageSeg;
        private final MemorySegment institutionalComplexitySeg;

        // Extended Geological, Pedological & Logistics Rasters (Point 8)
        private final MemorySegment resourceCoalSeg;
        private final MemorySegment resourceOilSeg;
        private final MemorySegment resourceGasSeg;
        private final MemorySegment resourceUraniumSeg;
        private final MemorySegment freshwaterAquiferSeg;
        private final MemorySegment soilDepthSeg;
        private final MemorySegment soilOrganicCarbonSeg;
        private final MemorySegment commercialFluxVolumeSeg;

        // Agent primitive arrays segments
        private final MemorySegment hexIdsSeg;
        private final MemorySegment agentH3Seg;
        private final MemorySegment massSeg;
        private final MemorySegment energySeg;
        private final MemorySegment sigmaCostSeg;
        /** Reserved C-ABI alignment segment (offset 48); unused by physical observables kernels. */
        private final MemorySegment reservedAgentSlotSeg;
        private final MemorySegment ageSeg;
        private final MemorySegment birthsSeg;
        private final MemorySegment deathsSeg;
        private final MemorySegment genCountSeg;
        private final MemorySegment geneticsSeg;
        private final MemorySegment cultureSeg;

        public NativeWorldArena(int capacity) {
            this(capacity, capacity);
        }

        public NativeWorldArena(int worldCapacity, int agentCapacity) {
            this.capacity = worldCapacity;
            this.agentCapacity = agentCapacity;
            this.arena = Arena.ofShared();

            long cap = worldCapacity;
            this.h3IndexesSeg = arena.allocate(cap * Long.BYTES, 64);
            this.neighborIndexesSeg = arena.allocate(cap * 6 * Integer.BYTES, 64);
            this.elevationSeg = arena.allocate(cap * Float.BYTES, 64);
            this.temperatureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.rainfallSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomesSeg = arena.allocate(cap * Byte.BYTES, 64);
            this.foodResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.waterResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.woodResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.metalResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.clayResourceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassHumanSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassLivestockSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassFishSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassAgricultureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.biomassNaturalSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyWindSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energySolarSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyFireSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energySlavesSeg = arena.allocate(cap * Float.BYTES, 64);
            this.energyFoodConsumedSeg = arena.allocate(cap * Float.BYTES, 64);
            this.lifespanSeg = arena.allocate(cap * Float.BYTES, 64);
            this.fertilitySeg = arena.allocate(cap * Float.BYTES, 64);
            this.giniIndexSeg = arena.allocate(cap * Float.BYTES, 64);
            this.reservedWorldSlotSeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceCapitalSeg = arena.allocate(cap * Float.BYTES, 64);
            this.fluxPressureSeg = arena.allocate(cap * Float.BYTES, 64);
            this.localPriceSeg = arena.allocate(cap * Float.BYTES, 64);
            this.storageSeg = arena.allocate(cap * Float.BYTES, 64);
            this.institutionalComplexitySeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceCoalSeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceOilSeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceGasSeg = arena.allocate(cap * Float.BYTES, 64);
            this.resourceUraniumSeg = arena.allocate(cap * Float.BYTES, 64);
            this.freshwaterAquiferSeg = arena.allocate(cap * Float.BYTES, 64);
            this.soilDepthSeg = arena.allocate(cap * Float.BYTES, 64);
            this.soilOrganicCarbonSeg = arena.allocate(cap * Float.BYTES, 64);
            this.commercialFluxVolumeSeg = arena.allocate(cap * Float.BYTES, 64);

            // Populate WorldBufferRaw struct in native memory (312 bytes)
            this.worldStructSeg = arena.allocate(312, 8);
            worldStructSeg.set(ValueLayout.JAVA_LONG, 0, cap);
            worldStructSeg.set(ValueLayout.ADDRESS, 8, h3IndexesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 16, neighborIndexesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 24, elevationSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 32, temperatureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 40, rainfallSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 48, biomesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 56, foodResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 64, waterResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 72, woodResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 80, metalResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 88, clayResourceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 96, biomassHumanSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 104, biomassLivestockSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 112, biomassFishSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 120, biomassAgricultureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 128, biomassNaturalSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 136, energyWindSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 144, energySolarSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 152, energyFireSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 160, energySlavesSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 168, energyFoodConsumedSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 176, lifespanSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 184, fertilitySeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 192, giniIndexSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 200, reservedWorldSlotSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 208, resourceCapitalSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 216, fluxPressureSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 224, localPriceSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 232, storageSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 240, institutionalComplexitySeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 248, resourceCoalSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 256, resourceOilSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 264, resourceGasSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 272, resourceUraniumSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 280, freshwaterAquiferSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 288, soilDepthSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 296, soilOrganicCarbonSeg);
            worldStructSeg.set(ValueLayout.ADDRESS, 304, commercialFluxVolumeSeg);

            // Agent Buffer allocation
            long aCap = agentCapacity;
            this.hexIdsSeg = arena.allocate(aCap * Integer.BYTES, 64);
            this.agentH3Seg = arena.allocate(aCap * Long.BYTES, 64);
            this.massSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.energySeg = arena.allocate(aCap * Float.BYTES, 64);
            this.sigmaCostSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.reservedAgentSlotSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.ageSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.birthsSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.deathsSeg = arena.allocate(aCap * Float.BYTES, 64);
            this.genCountSeg = arena.allocate(aCap * Integer.BYTES, 64);
            this.geneticsSeg = arena.allocate(aCap * 4 * Float.BYTES, 64);
            this.cultureSeg = arena.allocate(aCap * 4 * Float.BYTES, 64);

            // Populate AgentBufferRaw struct in native memory (104 bytes)
            this.agentStructSeg = arena.allocate(104, 8);
            agentStructSeg.set(ValueLayout.JAVA_LONG, 0, aCap);
            agentStructSeg.set(ValueLayout.ADDRESS, 8, hexIdsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 16, agentH3Seg);
            agentStructSeg.set(ValueLayout.ADDRESS, 24, massSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 32, energySeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 40, sigmaCostSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 48, reservedAgentSlotSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 56, ageSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 64, birthsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 72, deathsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 80, genCountSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 88, geneticsSeg);
            agentStructSeg.set(ValueLayout.ADDRESS, 96, cultureSeg);
        }

        /**
         * Fast bulk copy from Java {@link WorldBuffer} and {@link AgentBuffer} arrays into native memory.
         */
        public void syncFromJavaBuffers(WorldBuffer wb, AgentBuffer ab) {
            int count = Math.min(capacity, wb.getCapacity());
            if (wb.getH3Indexes() != null) MemorySegment.copy(wb.getH3Indexes(), 0, h3IndexesSeg, ValueLayout.JAVA_LONG, 0, count);
            if (wb.getElevation() != null) MemorySegment.copy(wb.getElevation(), 0, elevationSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getTemperature() != null) MemorySegment.copy(wb.getTemperature(), 0, temperatureSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getRainfall() != null) MemorySegment.copy(wb.getRainfall(), 0, rainfallSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomes() != null) MemorySegment.copy(wb.getBiomes(), 0, biomesSeg, ValueLayout.JAVA_BYTE, 0, count);
            if (wb.getFoodResource() != null) MemorySegment.copy(wb.getFoodResource(), 0, foodResourceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getWaterResource() != null) MemorySegment.copy(wb.getWaterResource(), 0, waterResourceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassHuman() != null) MemorySegment.copy(wb.getBiomassHuman(), 0, biomassHumanSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassLivestock() != null) MemorySegment.copy(wb.getBiomassLivestock(), 0, biomassLivestockSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassNatural() != null) MemorySegment.copy(wb.getBiomassNatural(), 0, biomassNaturalSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassAgriculture() != null) MemorySegment.copy(wb.getBiomassAgriculture(), 0, biomassAgricultureSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getBiomassFish() != null) MemorySegment.copy(wb.getBiomassFish(), 0, biomassFishSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceCapital() != null) MemorySegment.copy(wb.getResourceCapital(), 0, resourceCapitalSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getLifespan() != null) MemorySegment.copy(wb.getLifespan(), 0, lifespanSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getFertility() != null) MemorySegment.copy(wb.getFertility(), 0, fertilitySeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getInstitutionalComplexity() != null) MemorySegment.copy(wb.getInstitutionalComplexity(), 0, institutionalComplexitySeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getLocalPrice() != null) MemorySegment.copy(wb.getLocalPrice(), 0, localPriceSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getEnergyFoodConsumed() != null) MemorySegment.copy(wb.getEnergyFoodConsumed(), 0, energyFoodConsumedSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceCoal() != null) MemorySegment.copy(wb.getResourceCoal(), 0, resourceCoalSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceOil() != null) MemorySegment.copy(wb.getResourceOil(), 0, resourceOilSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceGas() != null) MemorySegment.copy(wb.getResourceGas(), 0, resourceGasSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getResourceUranium() != null) MemorySegment.copy(wb.getResourceUranium(), 0, resourceUraniumSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getFreshwaterAquifer() != null) MemorySegment.copy(wb.getFreshwaterAquifer(), 0, freshwaterAquiferSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getSoilDepth() != null) MemorySegment.copy(wb.getSoilDepth(), 0, soilDepthSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getSoilOrganicCarbon() != null) MemorySegment.copy(wb.getSoilOrganicCarbon(), 0, soilOrganicCarbonSeg, ValueLayout.JAVA_FLOAT, 0, count);
            if (wb.getCommercialFluxVolume() != null) MemorySegment.copy(wb.getCommercialFluxVolume(), 0, commercialFluxVolumeSeg, ValueLayout.JAVA_FLOAT, 0, count);

            if (wb.getNeighborIndexes() != null) {
                int[][] nbrs = wb.getNeighborIndexes();
                int rows = Math.min(capacity, nbrs.length);
                for (int r = 0; r < rows; r++) {
                    if (nbrs[r] != null) {
                        MemorySegment.copy(nbrs[r], 0, neighborIndexesSeg, ValueLayout.JAVA_INT, (long) r * 6 * Integer.BYTES, Math.min(6, nbrs[r].length));
                    }
                }
            }

            if (ab != null) {
                int aCount = Math.min(agentCapacity, ab.getCapacity());
                if (ab.getHexIds() != null) MemorySegment.copy(ab.getHexIds(), 0, hexIdsSeg, ValueLayout.JAVA_INT, 0, aCount);
                if (ab.getMass() != null) MemorySegment.copy(ab.getMass(), 0, massSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getEnergy() != null) MemorySegment.copy(ab.getEnergy(), 0, energySeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getAge() != null) MemorySegment.copy(ab.getAge(), 0, ageSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getBirths() != null) MemorySegment.copy(ab.getBirths(), 0, birthsSeg, ValueLayout.JAVA_FLOAT, 0, aCount);
                if (ab.getDeaths() != null) MemorySegment.copy(ab.getDeaths(), 0, deathsSeg, ValueLayout.JAVA_FLOAT, 0, aCount);

                if (ab.getGenetics() != null) {
                    float[][] gen = ab.getGenetics();
                    for (int d = 0; d < Math.min(4, gen.length); d++) {
                        if (gen[d] != null) {
                            MemorySegment.copy(gen[d], 0, geneticsSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, Math.min(aCount, gen[d].length));
                        }
                    }
                }
                if (ab.getCulture() != null) {
                    float[][] cul = ab.getCulture();
                    for (int d = 0; d < Math.min(4, cul.length); d++) {
                        if (cul[d] != null) {
                            MemorySegment.copy(cul[d], 0, cultureSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, Math.min(aCount, cul[d].length));
                        }
                    }
                }
            }
        }

        /**
         * Fast bulk copy from native memory back into Java arrays.
         */
        public void syncToJavaBuffers(WorldBuffer wb, AgentBuffer ab) {
            int count = Math.min(capacity, wb.getCapacity());
            if (wb.getFoodResource() != null) MemorySegment.copy(foodResourceSeg, ValueLayout.JAVA_FLOAT, 0, wb.getFoodResource(), 0, count);
            if (wb.getBiomassHuman() != null) MemorySegment.copy(biomassHumanSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassHuman(), 0, count);
            if (wb.getBiomassLivestock() != null) MemorySegment.copy(biomassLivestockSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassLivestock(), 0, count);
            if (wb.getBiomassNatural() != null) MemorySegment.copy(biomassNaturalSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassNatural(), 0, count);
            if (wb.getBiomassAgriculture() != null) MemorySegment.copy(biomassAgricultureSeg, ValueLayout.JAVA_FLOAT, 0, wb.getBiomassAgriculture(), 0, count);
            if (wb.getResourceCapital() != null) MemorySegment.copy(resourceCapitalSeg, ValueLayout.JAVA_FLOAT, 0, wb.getResourceCapital(), 0, count);
            if (wb.getInstitutionalComplexity() != null) MemorySegment.copy(institutionalComplexitySeg, ValueLayout.JAVA_FLOAT, 0, wb.getInstitutionalComplexity(), 0, count);
            if (wb.getLocalPrice() != null) MemorySegment.copy(localPriceSeg, ValueLayout.JAVA_FLOAT, 0, wb.getLocalPrice(), 0, count);
            if (wb.getEnergyFoodConsumed() != null) MemorySegment.copy(energyFoodConsumedSeg, ValueLayout.JAVA_FLOAT, 0, wb.getEnergyFoodConsumed(), 0, count);
            if (wb.getCommercialFluxVolume() != null) MemorySegment.copy(commercialFluxVolumeSeg, ValueLayout.JAVA_FLOAT, 0, wb.getCommercialFluxVolume(), 0, count);

            if (ab != null) {
                int aCount = Math.min(agentCapacity, ab.getCapacity());
                if (ab.getHexIds() != null) MemorySegment.copy(hexIdsSeg, ValueLayout.JAVA_INT, 0, ab.getHexIds(), 0, aCount);
                if (ab.getMass() != null) MemorySegment.copy(massSeg, ValueLayout.JAVA_FLOAT, 0, ab.getMass(), 0, aCount);
                if (ab.getEnergy() != null) MemorySegment.copy(energySeg, ValueLayout.JAVA_FLOAT, 0, ab.getEnergy(), 0, aCount);
                if (ab.getAge() != null) MemorySegment.copy(ageSeg, ValueLayout.JAVA_FLOAT, 0, ab.getAge(), 0, aCount);
                if (ab.getBirths() != null) MemorySegment.copy(birthsSeg, ValueLayout.JAVA_FLOAT, 0, ab.getBirths(), 0, aCount);
                if (ab.getDeaths() != null) MemorySegment.copy(deathsSeg, ValueLayout.JAVA_FLOAT, 0, ab.getDeaths(), 0, aCount);

                if (ab.getGenetics() != null) {
                    float[][] gen = ab.getGenetics();
                    for (int d = 0; d < Math.min(4, gen.length); d++) {
                        if (gen[d] != null) {
                            MemorySegment.copy(geneticsSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, gen[d], 0, Math.min(aCount, gen[d].length));
                        }
                    }
                }
                if (ab.getCulture() != null) {
                    float[][] cul = ab.getCulture();
                    for (int d = 0; d < Math.min(4, cul.length); d++) {
                        if (cul[d] != null) {
                            MemorySegment.copy(cultureSeg, ValueLayout.JAVA_FLOAT, (long) d * agentCapacity * Float.BYTES, cul[d], 0, Math.min(aCount, cul[d].length));
                        }
                    }
                }
            }
        }

        /**
         * Executes environmental photosynthesis and biomass tick via native Rust library.
         */
        public void tickEnvironmental(float dtYears) {
            if (NativeRustBridge.envTickHandle == null) return;
            try {
                NativeRustBridge.envTickHandle.invokeExact(worldStructSeg, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native environmental tick", t);
            }
        }

        /**
         * Executes urban agglomeration and institutional entropy tick via native Rust library.
         */
        public void tickUrban(float dtYears) {
            if (NativeRustBridge.urbanTickHandle == null) return;
            try {
                NativeRustBridge.urbanTickHandle.invokeExact(worldStructSeg, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native urban tick", t);
            }
        }

        /**
         * Executes finite-volume flux trade diffusion tick via native Rust library.
         */
        public void tickFlux(float dtSeconds) {
            if (NativeRustBridge.fluxTickHandle == null) return;
            try {
                NativeRustBridge.fluxTickHandle.invokeExact(worldStructSeg, dtSeconds);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native flux tick", t);
            }
        }

        /**
         * Executes demographic metabolic cohorts tick via native Rust library.
         */
        public void tickDemographic(float targetCohortSize, float dtYears) {
            tickDemographic(targetCohortSize, dtYears, 42L);
        }

        /**
         * Executes demographic metabolic cohorts tick with explicit scenario seed via native Rust library.
         */
        public void tickDemographic(float targetCohortSize, float dtYears, long seed) {
            if (NativeRustBridge.demogTickFullHandle != null) {
                try {
                    NativeRustBridge.demogTickFullHandle.invokeExact(worldStructSeg, agentStructSeg, targetCohortSize, dtYears, seed);
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("Failed native full demographic tick", t);
                }
            }
            if (NativeRustBridge.demogTickHandle == null) return;
            try {
                NativeRustBridge.demogTickHandle.invokeExact(worldStructSeg, agentStructSeg, targetCohortSize, dtYears);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native demographic tick", t);
            }
        }

        /**
         * Executes cultural Langevin SDE tensor drift tick via native Rust library.
         */
        public void tickCulture(float dtYears, float diffusionRate, float forcingRate) {
            if (NativeRustBridge.cultureTickHandle == null) return;
            try {
                NativeRustBridge.cultureTickHandle.invokeExact(worldStructSeg, agentStructSeg, dtYears, diffusionRate, forcingRate);
            } catch (Throwable t) {
                throw new RuntimeException("Failed native culture tick", t);
            }
        }

        /**
         * Executes cultural Langevin SDE tensor drift tick with explicit stochastic Wiener drift via native Rust library.
         */
        public void tickCulture(float dtYears, float diffusionRate, float forcingRate, float mutationRate, long seed, long tickCounter) {
            if (NativeRustBridge.cultureTickFullHandle != null) {
                try {
                    NativeRustBridge.cultureTickFullHandle.invokeExact(worldStructSeg, agentStructSeg, dtYears, diffusionRate, forcingRate, mutationRate, seed, tickCounter);
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("Failed native full culture tick", t);
                }
            }
            logger.warn("NativeRustBridge.cultureTickFullHandle is null! Falling back to 5-param culture tick");
            tickCulture(dtYears, diffusionRate, forcingRate);
        }

        /**
         * Executes the complete unified 5-kernel simulation cycle in native Rust with 0 GC overhead.
         */
        public void stepUnifiedCycle(float dtDays) {
            stepUnifiedCycle(dtDays, 42L, 1L);
        }

        /**
         * Executes the complete unified 5-kernel simulation cycle in native Rust with explicit scenario seed.
         */
        public void stepUnifiedCycle(float dtDays, long seed, long tickCounter) {
            float dtYears = dtDays / 365.25f;
            float dtSeconds = dtDays * 86400.0f;

            tickEnvironmental(dtYears);
            tickDemographic(500.0f, dtYears, seed);
            tickUrban(dtYears);
            tickFlux(dtSeconds);
            tickCulture(dtYears, 0.05f, 0.02f, 0.015f, seed, tickCounter);
        }

        @Override
        public void close() {
            arena.close();
        }
    }
