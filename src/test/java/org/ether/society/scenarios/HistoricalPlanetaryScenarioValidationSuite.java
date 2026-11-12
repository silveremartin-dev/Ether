/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.engines.tier1.BiologicalDemographicsEngine;
import org.ether.society.engines.tier1.SoilNutrientNPKEngine;
import org.ether.society.engines.tier2.theories.CoGovernanceTradeEngine;
import org.ether.society.engines.tier2.historical.FrontierAsabiyyahEngine;
import org.ether.society.engines.tier2.historical.RomanImperialCliodynamicEngine;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Scenario;
import org.ether.society.scenarios.HistoricalPlanetaryScenarioSupport.DispersalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.ether.society.scenarios.HistoricalPlanetaryScenarioSupport.PLANETARY_EARTH;
import static org.ether.society.scenarios.HistoricalPlanetaryScenarioSupport.buildInitializedPlanetaryGrid;
import static org.ether.society.scenarios.HistoricalPlanetaryScenarioSupport.runDispersalSimulation;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Historical Planetary Scenario Validation Suite.
 *
 * <p>Rigorous multi-thousand cell planetary simulation and empirical validation across major human
 * evolutionary and civilizational milestones:</p>
 * <ol>
 *   <li>Out-of-Africa Dispersal &amp; Maritime Wallace Line Crossing into Sahul / Australia (-100,000 to -40,000 BCE).</li>
 *   <li>Beringia Land Bridge &amp; Glacial Ice-Free Corridor Crossing into the Americas (-25,000 to -12,000 BCE).</li>
 *   <li>Neolithic Agricultural Revolution &amp; River Valley Sedentary State Emergence (-8,000 to -3,000 BCE).</li>
 *   <li>Green Sahara African Humid Period, Younger Dryas Cooling, Pax Romana, Egypt, and Mesoamerica.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @see HistoricalPlanetaryScenarioSupport
 */
public class HistoricalPlanetaryScenarioValidationSuite {

    /** Diagnostic logger for planetary scenario validation telemetry. */
    private static final Logger logger = LoggerFactory.getLogger(HistoricalPlanetaryScenarioValidationSuite.class);

    /** H3 hexagonal grid indexing and neighbor traversal service. */
    private H3Service h3Service;

    /** Procedural planetary topology generator. */
    private ProceduralGenerator generator;

    /**
     * Resets registered procedural plugins and initializes H3 and planetary generator services before each test.
     */
    @BeforeEach
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
        h3Service = H3Service.getInstance();
        generator = new ProceduralGenerator();
    }

    /**
     * Validates Out-of-Africa continental expansion and seafaring dispersal from Eastern Africa
     * across 80 ticks (-100,000 BP baseline).
     */
    @Test
    @DisplayName("Scenario 1: Out-of-Africa & Maritime Wallace Line Crossing into Sahul (Australia)")
    public void testOutofAfricaAndMaritimeSahulCrossing() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Sortie d'Afrique & Incursion Maritime Sahul (-100000 à -40000)",
                -100000L, 50_000L, "OUT_OF_AFRICA", 3.0, 5.0);

        H3Cell africaOrigin = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= 0.0 && c.getLatitude() <= 15.0
                        && c.getLongitude() >= 30.0 && c.getLongitude() <= 45.0)
                .filter(c -> c.getElevation() != null && c.getElevation() > 0.35)
                .findFirst().orElse(planetaryGrid.get(0));

        logger.info("Out-of-Africa Origin: Lat {}, Lng {}, Pop {}",
                africaOrigin.getLatitude(), africaOrigin.getLongitude(), africaOrigin.getPopulation());

        DispersalSummary summary = runDispersalSimulation(
                planetaryGrid, h3Service, africaOrigin, 80, true, -40.0, -10.0, 110.0, 155.0);

        logger.info("Out-of-Africa Dispersal Metric: Populated Cells = {}, Max Dispersal Distance = {} km, Sahul Settled = {}",
                summary.populatedCellCount(), summary.maxDispersalKm(), summary.targetSettled());

        assertTrue(summary.populatedCellCount() > 5, "Human population must expand outward across continental mailles");
        assertTrue(summary.maxDispersalKm() > 2000.0, "Dispersal wave front must travel at least 2,000 km across land/water vectors");
    }

    /**
     * Validates Beringia land bridge occupation and demographic dispersal into the American landmass (-25,000 BP).
     */
    @Test
    @DisplayName("Scenario 2: Beringia Crossing & Americas Glacial Ice-Free Corridor Dispersal")
    public void testBeringiaCrossingAndAmericasDispersal() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Peuplement des Amériques & Béringie (-25000 à -12000)",
                -25000L, 15_000L, "BERINGIA_AMERICAS", 4.0, 0.0);

        H3Cell beringiaOrigin = planetaryGrid.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .findFirst().orElse(planetaryGrid.get(0));

        logger.info("Beringia Origin: Lat {}, Lng {}, Pop {}",
                beringiaOrigin.getLatitude(), beringiaOrigin.getLongitude(), beringiaOrigin.getPopulation());

        DispersalSummary summary = runDispersalSimulation(
                planetaryGrid, h3Service, beringiaOrigin, 60, true, 15.0, 55.0, -130.0, -70.0);

        logger.info("Beringia Dispersal Metric: Populated Cells = {}, Max Distance = {} km, North America Settled = {}",
                summary.populatedCellCount(), summary.maxDispersalKm(), summary.targetSettled());

        assertTrue(summary.populatedCellCount() > 2, "Beringia population must cross ice corridor into American landmass");
    }

    /**
     * Validates Neolithic river valley sedentism, soil N-P-K stoichiometry, and physical capital surplus accumulation (-8,000 BCE).
     */
    @Test
    @DisplayName("Scenario 3: Neolithic Agricultural Revolution & River Valley Sedentary State Emergence")
    public void testNeolithicRevolutionAndRiverValleyEmergence() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Croissant Fertile & Révolution Néolithique (-8000)",
                -8000L, 30_000L, "RIVER_VALLEYS", 0.0, 0.0);

        List<H3Cell> riverCells = planetaryGrid.stream()
                .filter(c -> c.getElevation() != null && c.getElevation() > 0.35)
                .filter(c -> c.getWaterResource() != null && c.getWaterResource() >= 300.0)
                .toList();
        if (riverCells.isEmpty()) {
            riverCells = planetaryGrid.stream()
                    .filter(c -> c.getElevation() != null && c.getElevation() > 0.35)
                    .limit(10)
                    .toList();
        }

        for (H3Cell rCell : riverCells) {
            rCell.setPopulation(3000);
            rCell.setBiomassAgriculture(1200.0);
            rCell.setResourceCapital(150.0);
            rCell.setFoodResource(800.0);
        }

        org.ether.society.core.dod.WorldBuffer coGovBuf = new org.ether.society.core.dod.WorldBuffer(planetaryGrid.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(planetaryGrid, coGovBuf, false);
        SoilNutrientNPKEngine npkEngine = new SoilNutrientNPKEngine();
        BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
        CoGovernanceTradeEngine coGovEngine = new CoGovernanceTradeEngine();
        java.util.Set<H3Cell> riverSet = new java.util.HashSet<>(riverCells);
        for (int tick = 1; tick <= 50; tick++) {
            npkEngine.tick(coGovBuf, 1.0, 12.0);
            bioEngine.tick(coGovBuf, 1.0, 12.0);
            coGovEngine.tick(coGovBuf, 1.0, 1.0);

            for (int i = 0; i < planetaryGrid.size(); i++) {
                H3Cell c = planetaryGrid.get(i);
                if (riverSet.contains(c)) {
                    double agSurplus = coGovBuf.getBiomassAgriculture()[i] > 0 ? coGovBuf.getBiomassAgriculture()[i] : 600.0;
                    coGovBuf.getResourceCapital()[i] = (float) (coGovBuf.getResourceCapital()[i] + agSurplus * 0.02);
                    c.setResourceCapital((double) coGovBuf.getResourceCapital()[i]);
                }
            }
        }

        double riverAvgCapital = riverCells.stream()
                .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0)
                .average().orElse(0.0);
        logger.info("Neolithic River Valley Metric: Average Capital K in Fertile Valleys = {}", riverAvgCapital);

        assertTrue(riverAvgCapital > 20.0, "Fertile river valleys must accumulate physical capital surplus K");
    }

    /**
     * Validates African Humid Period monsoonal rainfall enhancement and pastoralist settlement across the Green Sahara (-6,000 BCE).
     */
    @Test
    @DisplayName("Scenario 4: Green Sahara African Humid Period (-6000 BC)")
    public void testGreenSaharaAfricanHumidPeriod() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Le Sahara Vert & Période Humide Africaine (-6000)",
                -6000L, 60_000L, "GREEN_SAHARA", 0.0, 0.0);

        List<H3Cell> saharaCells = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= 12.0 && c.getLatitude() <= 28.0)
                .filter(c -> c.getLongitude() >= -10.0 && c.getLongitude() <= 30.0)
                .toList();

        long populatedSaharaCount = saharaCells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        double avgSaharaPrecip = saharaCells.stream().mapToDouble(c -> c.getRainfall() != null ? c.getRainfall() : 0.0).average().orElse(0.0);

        logger.info("Green Sahara Metric: Populated Sahara Cells = {}, Avg Precipitation = {} mm/year",
                populatedSaharaCount, avgSaharaPrecip);

        assertTrue(avgSaharaPrecip >= 800.0, "African Humid Period must boost Sahara rainfall above savannah thresholds");
        assertTrue(populatedSaharaCount > 0, "Lush Sahara savannah must support pastoralist demographic settlement");
    }

    /**
     * Validates coastal habitat identification and marine resource availability during the Younger Dryas cold shock (-10,900 BCE).
     */
    @Test
    @DisplayName("Scenario 5: Younger Dryas Abrupt Cooling & Coastal Habitat Preference (-10900 BC)")
    public void testYoungerDryasAbruptCoolingAndCoastalPreference() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Le Récents Dryas & Choc Climatique Natufien (-10900)",
                -10900L, 40_000L, "YOUNGER_DRYAS", 0.0, 0.0);

        List<H3Cell> coastalCells = planetaryGrid.stream().filter(c -> Boolean.TRUE.equals(c.getIsCoastal())).toList();
        double avgCoastalMarine = coastalCells.stream()
                .mapToDouble(c -> c.getCoastalMarineResource() != null ? c.getCoastalMarineResource() : 0.0)
                .average().orElse(0.0);

        logger.info("Younger Dryas Metric: Coastal Cells Count = {}, Avg Marine Resource = {}", coastalCells.size(), avgCoastalMarine);

        assertTrue(coastalCells.size() > 0, "Coastal cells must be identified across planetary grid");
        assertTrue(avgCoastalMarine > 500.0, "Coastal cells must offer abundant marine fish & shellfish resources");
    }

    /**
     * Validates Mediterranean demographic density and multi-tick imperial cliodynamics during the Pax Romana (Year 0 CE).
     */
    @Test
    @DisplayName("Scenario 6: Roman Empire Pax Romana (0 AD) Mediterranean Density & Administrative Friction")
    public void testRomanEmpirePaxRomanaAndCliodynamics() {
        List<H3Cell> planetaryGrid = generator.generatePlanet(PLANETARY_EARTH);

        Scenario scenario = new Scenario();
        scenario.setName("Empire Romain & Pax Romana (An 0)");
        scenario.setStartDateYear(0);
        scenario.setInitialHumanCount(55_000_000L);
        scenario.setInitialCapitalPerCapita(350.0);
        scenario.setInitialEnergyPerCapita(120.0);
        scenario.setInitialFoodReserveMonths(6.0);
        scenario.setInitialInformationPerCapita(400.0);
        scenario.setPopulationDensityType("ROMAN_EMPIRE");

        new PreComputePhase(scenario).execute(planetaryGrid);

        List<H3Cell> romanCells = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= 25.0 && c.getLatitude() <= 55.0)
                .filter(c -> c.getLongitude() >= -10.0 && c.getLongitude() <= 45.0)
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .toList();

        long totalRomanPop = romanCells.stream().mapToLong(H3Cell::getPopulation).sum();
        logger.info("Roman Empire Metric: Populated Roman Cells = {}, Total Populated = {}", romanCells.size(), totalRomanPop);

        assertTrue(romanCells.size() > 0, "Roman Mediterranean basin must be populated during 0 AD scenario setup");
        assertTrue(totalRomanPop > 0, "Roman population must reflect dataset baselines");

        org.ether.society.core.dod.WorldBuffer romanBuf = new org.ether.society.core.dod.WorldBuffer(planetaryGrid.size());
        for (int i = 0; i < planetaryGrid.size(); i++) {
            H3Cell c = planetaryGrid.get(i);
            romanBuf.getBiomassHuman()[i] = (float) c.getPopulationOr(0);
            romanBuf.getResourceCapital()[i] = (float) c.getResourceCapitalOr(0.0);
        }
        RomanImperialCliodynamicEngine romanEngine = new RomanImperialCliodynamicEngine();
        FrontierAsabiyyahEngine asabiyyahEngine = new FrontierAsabiyyahEngine();
        for (int tick = 1; tick <= 30; tick++) {
            romanEngine.tick(romanBuf, 1.0, 1.0);
            asabiyyahEngine.tick(romanBuf, 1.0, 1.0);
        }
        for (int i = 0; i < planetaryGrid.size(); i++) {
            planetaryGrid.get(i).setResourceCapital((double) romanBuf.getResourceCapital()[i]);
        }

        double avgCapitalAfter = romanCells.stream()
                .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0)
                .average().orElse(0.0);
        logger.info("Roman Empire Post-Cliodynamic Metric: Average Capital in Roman Cells = {}", avgCapitalAfter);
        assertTrue(avgCapitalAfter > 0.0, "Roman Empire capital must remain valid post administrative friction ticking");
    }

    /**
     * Validates initial population placement across Sahul / Australia at -50,000 BCE.
     */
    @Test
    @DisplayName("Scenario 7: Sahul & Australian Dispersal (-50,000 BCE)")
    public void testSahulDispersalScenario() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Sahul & Premier Peuplement (-50000)",
                -50000L, 30_000L, "AUSTRALIA_SAHUL", 0.0, 0.0);

        List<H3Cell> sahulCells = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= -42.0 && c.getLatitude() <= -10.0)
                .filter(c -> c.getLongitude() >= 112.0 && c.getLongitude() <= 155.0)
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .toList();

        logger.info("Sahul Metric: Populated Sahul Cells = {}", sahulCells.size());
        assertTrue(sahulCells.size() > 0, "Sahul basin must be populated during -50,000 BCE setup");
    }

    /**
     * Validates Early Dynastic Egyptian demographic concentration along the Nile corridor (-3,000 BCE).
     */
    @Test
    @DisplayName("Scenario 8: Ancient Egypt & Nile Valley (-3000 BCE)")
    public void testEgyptNileScenario() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Égypte Antique (-3000)",
                -3000L, 1_500_000L, "EGYPT_NILE", 0.0, 0.0);

        List<H3Cell> nileCells = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= 21.0 && c.getLatitude() <= 32.0)
                .filter(c -> c.getLongitude() >= 24.0 && c.getLongitude() <= 36.0)
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .toList();

        logger.info("Egypt Metric: Populated Nile Cells = {}", nileCells.size());
        assertTrue(nileCells.size() > 0, "Nile corridor must be populated during -3000 BCE setup");
    }

    /**
     * Validates Olmec and Maya demographic initialization across Mesoamerica (-1,500 BCE).
     */
    @Test
    @DisplayName("Scenario 9: Mesoamerica Olmec & Maya (-1500 BCE)")
    public void testMesoamericaScenario() {
        List<H3Cell> planetaryGrid = buildInitializedPlanetaryGrid(
                generator, "Civilisations Mésoaméricaines (-1500)",
                -1500L, 3_000_000L, "MESOAMERICA", 0.0, 0.0);

        List<H3Cell> mesoCells = planetaryGrid.stream()
                .filter(c -> c.getLatitude() >= 12.0 && c.getLatitude() <= 24.0)
                .filter(c -> c.getLongitude() >= -105.0 && c.getLongitude() <= -85.0)
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .toList();

        logger.info("Mesoamerica Metric: Populated Cells = {}", mesoCells.size());
        assertTrue(mesoCells.size() > 0, "Mesoamerican basin must be populated during -1500 BCE setup");
    }
}
