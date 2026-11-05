/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.persistence.EtherJsonUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Package-private operational helper for {@link Scenario} deep cloning, default physical
 * summary formatting, and benchmark preset lookup.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see Scenario
 */
final class ScenarioOperationsHelper {

    private ScenarioOperationsHelper() {
        // Stateless helper
    }

    static Scenario createBenchmarkStandardScenario() {
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("benchmark_standard".equals(s.getPresetKey()) || "out_of_africa".equals(s.getPresetKey())) {
                return s.copy();
            }
        }
        return Scenario.createDefaultScenario();
    }

    static Scenario copy(Scenario src) {
        try {
            ObjectMapper mapper = EtherJsonUtil.getMapper();
            String json = mapper.writeValueAsString(src);
            return mapper.readValue(json, Scenario.class);
        } catch (Exception e) {
            Scenario copy = new Scenario();
            copy.setName(src.getName());
            copy.setPresetKey(src.getPresetKey());
            copy.setReferenceTrajectoryModel(src.rawReferenceTrajectoryModel());
            copy.setDescription(src.rawDescription());
            copy.setPlanetPreset(src.getPlanetPreset());
            copy.setEcologyPreset(src.getEcologyPreset());
            copy.setEcologyPresetName(src.getEcologyPresetName());
            copy.setPlanetRadiusKm(src.getPlanetRadiusKm());
            copy.setRotationPeriodHours(src.getRotationPeriodHours());
            copy.setRevolutionPeriodDays(src.getRevolutionPeriodDays());
            copy.setAxialTiltDegrees(src.getAxialTiltDegrees());
            copy.setInitialHumanCount(src.getInitialHumanCount());
            copy.setInitialTechLevel(src.getInitialTechLevel());
            copy.setTechPreset(src.getTechPreset());
            copy.setInitialCapitalPerCapita(src.getInitialCapitalPerCapita());
            copy.setInitialEnergyPerCapita(src.getInitialEnergyPerCapita());
            copy.setInitialFoodReserveMonths(src.getInitialFoodReserveMonths());
            copy.setInitialInformationPerCapita(src.getInitialInformationPerCapita());
            copy.setPopulationDensityType(src.getPopulationDensityType());
            copy.setCellSizeKm2(src.getCellSizeKm2());
            copy.setTargetCohortSize(src.getTargetCohortSize());
            copy.setTemporalResolutionDays(src.getTemporalResolutionDays());
            copy.setClimateHarshness(src.getClimateHarshness());
            copy.setEndDateYear(src.getEndDateYear());
            copy.setStartDateYear(src.getStartDateYear());
            copy.setEndDateYear(src.getEndDateYear());
            copy.setSeed(src.getSeed());
            copy.setCulturalSeed(src.getCulturalSeed());
            copy.setRandomEventsEnabled(src.isRandomEventsEnabled());
            copy.setEarthHistoricalLeadersEnabled(src.isEarthHistoricalLeadersEnabled());
            copy.setProceduralLeadersEnabled(src.isProceduralLeadersEnabled());
            copy.setCustomDensityBase64(src.getCustomDensityBase64());
            copy.setCultureVectorDimensions(src.getCultureVectorDimensions());
            copy.setCulturalDiffusionRate(src.getCulturalDiffusionRate());
            copy.setCulturalMutationRate(src.getCulturalMutationRate());
            copy.setCustomTensorMapsBase64(new ArrayList<>(src.getCustomTensorMapsBase64()));
            copy.setTensorProceduralModes(new ArrayList<>(src.getTensorProceduralModes()));
            copy.setTensorSeeds(new HashMap<>(src.getTensorSeeds()));
            copy.setTensorProceduralParameters(new HashMap<>(src.getTensorProceduralParameters()));
            copy.setCustomTensorNames(new HashMap<>(src.getCustomTensorNames()));
            copy.setCulturalAffinityMatrix(new HashMap<>(src.getCulturalAffinityMatrix()));
            copy.setResourceVectorDimensions(src.getResourceVectorDimensions());
            copy.setCustomGeologyTensorMapsBase64(new ArrayList<>(src.getCustomGeologyTensorMapsBase64()));
            copy.setGeologyTensorProceduralModes(new ArrayList<>(src.getGeologyTensorProceduralModes()));
            copy.setH3Resolution(src.getH3Resolution());
            copy.setStrictDeterminism(src.isStrictDeterminism());
            if (!src.isStrictDeterminism()) {
                copy.setSparseCellSkippingEnabled(src.isSparseCellSkippingEnabled());
                copy.setOceanMultiRateTickingEnabled(src.isOceanMultiRateTickingEnabled());
                copy.setParallelExecutionEnabled(src.isParallelExecutionEnabled());
                copy.setSpatialRangeTruncationEnabled(src.isSpatialRangeTruncationEnabled());
            }
            copy.setOceanMacroAggregationEnabled(src.isOceanMacroAggregationEnabled());
            copy.setCoastalNavigationOnlyEnabled(src.isCoastalNavigationOnlyEnabled());
            copy.setClimateTickFrequency(src.getClimateTickFrequency());
            copy.setParallelThreadCount(src.getParallelThreadCount());
            copy.setTier2CliodynamicsEnabled(src.isTier2CliodynamicsEnabled());
            copy.setTypeBEngineStates(new HashMap<>(src.getTypeBEngineStates()));
            copy.setTypeBEngineParameters(new HashMap<>(src.getTypeBEngineParameters()));
            copy.setClimateEvents(new ArrayList<>(src.getClimateEvents()));
            copy.setClippingEnabled(src.isClippingEnabled());
            copy.setMinLat(src.getMinLat());
            copy.setMaxLat(src.getMaxLat());
            copy.setMinLng(src.getMinLng());
            copy.setMaxLng(src.getMaxLng());
            copy.setBoundaryMode(src.getBoundaryMode());
            copy.setUseRealEarthData(src.isUseRealEarthData());
            return copy;
        }
    }

    static String formatDefaultDescription(Scenario s) {
        return String.format("""
                🔬 SCÉNARIO EXPÉRIMENTAL ÉTHER : Configuration de Simulation Planétaire (%s)
                
                [CONTEXTE HISTORIQUE & PHYSIQUE]
                Ce scénario définit les conditions aux limites et les termes de forçage physique initiaux pour la modélisation multi-échelle des systèmes humains, écologiques et atmosphériques.
                
                [CONDITIONS INITIALES PHYSIQUES (T_0)]
                • Population Initiale : %,d individus.
                • Capital Physique (K₀) : %.1f kg/habitant (Outillages & machines).
                • Énergie Stockée (E₀) : %.1f MJ/habitant (Stocks énergétiques).
                • Réserves Alimentaires (F₀) : %.1f mois (Autonomie alimentaire).
                • Savoir Archivé (I₀) : %.1f bits/habitant (Mémoire technique).
                • Rayon Planétaire : %.0f km (g_rel = %.2f g).
                • Rotation Planétaire : %.1f heures | Inclinaison Axiale : %.1f°.
                """,
                s.getName() != null ? s.getName() : "Scénario Standard",
                s.getInitialHumanCount(),
                s.getInitialCapitalPerCapita(),
                s.getInitialEnergyPerCapita(),
                s.getInitialFoodReserveMonths(),
                s.getInitialInformationPerCapita(),
                s.getPlanetRadiusKm(),
                s.getPlanetRadiusKm() / 6371.0,
                s.getRotationPeriodHours(),
                s.getAxialTiltDegrees()
        );
    }
}
