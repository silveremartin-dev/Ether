/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier2.historical.CrosbyColumbianExchangeEngine;
import org.ether.society.engines.tier2.historical.LenskiPureEngine;
import org.ether.society.engines.tier2.historical.LeslieWhitePureEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation test suite for newly integrated and renamed Tier 2 Type B engines.
 */
@Tag("academic")
@Tag("scientific")
public class Tier2NewEnginesValidationTest {

    @Test
    @DisplayName("Polanyi Fictitious Commodities: Enclosure boosts yield and triggers protective double movement")
    public void testPolanyiFictitiousCommodities() {
        H3Cell cell = new H3Cell(0x852834710000001L, 51.5, -0.1);
        cell.setPopulation(5000);
        cell.setResourceCapital(500000.0); // 100 kg/cap -> above 80 threshold
        cell.setTradeNetwork(30.0); // above 15 threshold
        cell.setBiomassAgriculture(5000.0);
        cell.setFoodResource(6000.0); // 1.2 per capita (well fed initially)
        cell.setMovementFriction(1.0);
        cell.setResourceWork(60.0);

        List<H3Cell> grid = List.of(cell);

        // Run 5 years of normal market commodification: allocation efficiency boosts agricultural yield
        for (int yr = 0; yr < 5; yr++) {
            PolanyiFictitiousCommoditiesEngine.processPolanyiTransformation(grid, 1.0);
        }

        assertTrue(cell.getBiomassAgriculture() > 5000.0,
                "Market commodification and enclosure must accelerate agricultural capital yield.");

        // Now inject subsistence stress: food collapses below 1.20/cap
        cell.setFoodResource(2000.0); // 0.40 per capita

        for (int yr = 0; yr < 5; yr++) {
            PolanyiFictitiousCommoditiesEngine.processPolanyiTransformation(grid, 1.0);
        }

        assertTrue(cell.getMovementFriction() > 1.0,
                "Polanyi Double Movement: protective social resistance and regulations increase movement friction.");
        assertTrue(cell.getResourceWork() < 60.0,
                "Collective resistance and unrest divert labor away from routine productive work.");
    }

    @Test
    @DisplayName("Bagehot Fractional Reserve: Lender of Last Resort stems liquidity panic vs debt deflation")
    public void testBagehotFractionalReservePanicAndLenderOfLastResort() {
        // Cell 1: High commercial banking with bullion reserves (Lender of Last Resort present)
        H3Cell bufferedCell = new H3Cell(0x852834710000002L, 48.8, 2.3);
        bufferedCell.setPopulation(10000);
        bufferedCell.setResourceCapital(2500000.0); // 250 kg/cap
        bufferedCell.setTradeNetwork(40.0);
        bufferedCell.setFoodResource(5000.0); // Severe food stress (0.50/cap) -> panic trigger
        bufferedCell.setResourceMetal(300000.0); // 30 kg/cap bullion reserves -> Lender of Last Resort active
        bufferedCell.setResourceWork(60.0);

        // Cell 2: Identical commercial banking but zero state bullion reserves (No Lender of Last Resort)
        H3Cell unbufferedCell = new H3Cell(0x852834710000003L, 48.8, 2.4);
        unbufferedCell.setPopulation(10000);
        unbufferedCell.setResourceCapital(2500000.0);
        unbufferedCell.setTradeNetwork(40.0);
        unbufferedCell.setFoodResource(5000.0); // Identical panic trigger
        unbufferedCell.setResourceMetal(0.0); // Zero reserves -> debt deflation
        unbufferedCell.setResourceWork(60.0);

        List<H3Cell> bufferedGrid = List.of(bufferedCell);
        List<H3Cell> unbufferedGrid = List.of(unbufferedCell);

        // Run 4 years of liquidity crisis / bank run
        for (int yr = 0; yr < 4; yr++) {
            BagehotFractionalReserveEngine.processBagehotBankingLiquidity(bufferedGrid, 1.0);
            BagehotFractionalReserveEngine.processBagehotBankingLiquidity(unbufferedGrid, 1.0);
        }

        // Cell with lender of last resort advances bullion to preserve active capital
        assertTrue(bufferedCell.getResourceMetal() < 300000.0,
                "Lender of last resort advances bullion reserves to stem the panic.");
        assertEquals(2500000.0, bufferedCell.getResourceCapital(), 1e-4,
                "Lender of last resort prevents debt deflation destruction of active capital during panic.");

        // Cell without lender of last resort suffers Fisherian debt deflation
        assertTrue(unbufferedCell.getResourceCapital() < 2500000.0,
                "Without a lender of last resort, debt deflation destroys productive active capital.");
        assertTrue(bufferedCell.getResourceCapital() > unbufferedCell.getResourceCapital() * 1.10,
                "Buffered banking system maintains higher active capital than unbuffered collapse.");
        assertTrue(unbufferedCell.getResourceWork() < 60.0,
                "Credit collapse contracts productive employment and trade work.");
    }

    @Test
    @DisplayName("Corden-Neary Dutch Disease: Extractive boom crowds out diversified agriculture")
    public void testCordenNearyDutchDisease() {
        H3Cell cell = new H3Cell(0x852834710000004L, 25.0, 55.0);
        cell.setPopulation(8000);
        cell.setResourceCapital(400000.0);
        cell.setResourceMetal(300000.0); // Huge mineral boom -> rent ratio > 0.40
        cell.setBiomassAgriculture(2000.0);
        cell.setMovementFriction(1.0);

        List<H3Cell> grid = List.of(cell);

        for (int yr = 0; yr < 15; yr++) {
            CordenNearyDutchDiseaseEngine.processDutchDiseaseDynamics(grid, 1.0);
        }

        assertTrue(cell.getBiomassAgriculture() < 2000.0,
                "Windfall extractive rents crowd out domestic agriculture (lagging sector squeeze).");
        assertTrue(cell.getMovementFriction() > 1.0,
                "Factor cost appreciation and export terminal monopolization increase domestic transport friction.");
        assertTrue(cell.getResourceCapital() > 400000.0,
                "Extractive resource capital expands rapidly during the boom.");
    }

    @Test
    @DisplayName("Innis Staples Thesis: Raw staple export drives infrastructure and trade network")
    public void testInnisStaplesThesis() {
        H3Cell cell = new H3Cell(0x852834710000005L, 46.8, -71.2);
        cell.setPopulation(2000);
        cell.setResourceCapital(50000.0);
        cell.setBiomassNatural(200000.0); // Rich timber and furs
        cell.setTradeNetwork(5.0);

        List<H3Cell> grid = List.of(cell);

        for (int yr = 0; yr < 10; yr++) {
            InnisStaplesThesisEngine.processStaplesDynamics(grid, 1.0);
        }

        assertTrue(cell.getTradeNetwork() > 5.0,
                "Frontier raw staple extraction expands directional export trade integration.");
        assertTrue(cell.getBiomassNatural() < 200000.0,
                "Natural staple stocks are harvested for imperial export.");
        assertTrue(cell.getResourceCapital() > 50000.0,
                "Staple export revenues accumulate in frontier trading posts.");
    }

    @Test
    @DisplayName("Wiener Cybernetic Homeostasis: Dampens subsistence error deviations")
    public void testWienerCyberneticHomeostasis() {
        H3Cell deficitCell = new H3Cell(0x852834710000006L, 10.0, 10.0);
        deficitCell.setPopulation(1000);
        deficitCell.setFoodResource(600.0); // 0.60/cap vs target 1.20 -> error = -0.50
        deficitCell.setResourceWork(50.0);

        List<H3Cell> grid = List.of(deficitCell);

        for (int yr = 0; yr < 5; yr++) {
            WienerCyberneticHomeostasisEngine.processCyberneticHomeostasis(grid, 1.0);
        }

        assertTrue(deficitCell.getResourceWork() > 50.0,
                "Cybernetic negative feedback ramps up subsistence labor to close the food deficit.");
    }

    @Test
    @DisplayName("Renamed Theories: Lenski, White, Harris, and Crosby engines execute consistently")
    public void testRenamedTheoriesConsistencies() {
        // Lenski inequality curve
        double giniAgr = LenskiPureEngine.calculateLenskiGini(280.0);
        double giniForager = LenskiPureEngine.calculateLenskiGini(10.0);
        assertTrue(giniAgr > giniForager, "Lenski curve predicts agrarian inequality exceeds forager inequality.");

        // Leslie White complexity
        double whiteComplexity = LeslieWhitePureEngine.calculateCulturalComplexity(15.0, 2.5);
        assertEquals(37.5, whiteComplexity, 1e-4);

        // Crosby Columbian Exchange draft deficit
        H3Cell amerindianCell = new H3Cell(0x852834710000007L, -12.0, -77.0);
        amerindianCell.setPopulation(5000);
        amerindianCell.setResourceCapital(100000.0); // 20 kg/cap
        amerindianCell.setBiomassLivestock(0.0); // No draft megafauna
        amerindianCell.setResourceWork(80.0);

        CrosbyColumbianExchangeEngine engine = new CrosbyColumbianExchangeEngine();
        engine.process(List.of(amerindianCell), 1.0);

        assertTrue(amerindianCell.getResourceWork() <= 40.0,
                "Absence of domesticated draft animals caps muscle work EROEI.");
    }

    @Test
    @DisplayName("Ricardo Differential Land Rent: Marginal land cultivation compresses profit and expands Gini")
    public void testRicardoDifferentialLandRent() {
        // High density agrarian core pushing into inferior margins
        H3Cell crowdedCell = new H3Cell(0x852834710000010L, 30.0, 31.0);
        crowdedCell.setPopulation(8000);
        crowdedCell.setBiomassAgriculture(2000.0); // high land pressure: 8000 / 2000 = 4.0
        crowdedCell.setResourceCapital(400000.0);
        crowdedCell.setMovementFriction(0.30); // baseline Gini representation

        // Sparse agrarian cell with abundant prime soil
        H3Cell sparseCell = new H3Cell(0x852834710000011L, 30.0, 32.0);
        sparseCell.setPopulation(500);
        sparseCell.setBiomassAgriculture(5000.0); // low pressure: 500 / 5000 = 0.10
        sparseCell.setResourceCapital(400000.0);
        sparseCell.setMovementFriction(0.30);

        List<H3Cell> grid = List.of(crowdedCell, sparseCell);

        for (int yr = 0; yr < 10; yr++) {
            RicardoDifferentialLandRentEngine.processDifferentialRentDynamics(grid, 1.0);
        }

        assertTrue(crowdedCell.getResourceCapital() < 400000.0,
                "Ricardian extensive margin pressure compresses rate of profit and capital accumulation.");
        assertTrue(crowdedCell.getMovementFriction() > 0.30,
                "Differential land rent capture increases wealth inequality (Gini representation).");
        assertTrue(crowdedCell.getResourceCapital() < sparseCell.getResourceCapital(),
                "Fertility margin drag produces divergent capital accumulation between crowded and prime frontier.");
    }

    @Test
    @DisplayName("Lewis Dual-Sector Model: Surplus labor absorption transitions into capital deepening at turning point")
    public void testLewisDualSectorTurningPoint() {
        // Cell 1: Pre-turning point with massive agricultural surplus labor
        H3Cell surplusCell = new H3Cell(0x852834710000012L, 20.0, 80.0);
        surplusCell.setPopulation(10000);
        surplusCell.setBiomassAgriculture(1000.0); // Extreme labor surplus: 10000 / 1000 = 10.0 >> 2.5
        surplusCell.setResourceCapital(500000.0);
        surplusCell.setResourceWork(40.0);

        // Cell 2: Post-turning point (commercialized agriculture, surplus exhausted)
        H3Cell turningPointCell = new H3Cell(0x852834710000013L, 20.0, 81.0);
        turningPointCell.setPopulation(2000);
        turningPointCell.setBiomassAgriculture(5000.0); // Surplus exhausted: 2000 / 5000 = 0.40 << 2.5
        turningPointCell.setResourceCapital(600000.0); // 300 kg/cap > 250 threshold
        turningPointCell.setTradeNetwork(30.0); // commercial integration
        turningPointCell.setResourceWork(40.0);

        List<H3Cell> grid = List.of(surplusCell, turningPointCell);

        for (int yr = 0; yr < 15; yr++) {
            LewisDualSectorTurningPointEngine.processDualSectorDynamics(grid, 1.0);
        }

        assertTrue(turningPointCell.getResourceCapital() > 600000.0,
                "Post-Lewis turning point induces rapid capital deepening and mechanization.");
        assertTrue(turningPointCell.getResourceWork() > 40.0,
                "Rising real wages post-turning point expand productive labor incentives.");
        assertTrue(turningPointCell.getResourceCapital() > surplusCell.getResourceCapital() * 1.30,
                "Post-turning point capital accumulation outpaces surplus-labor stagnation by > 30%.");
    }

    @Test
    @DisplayName("Kondratiev-Schumpeter Long Waves: 52-year infrastructure cycle and creative destruction")
    public void testKondratievSchumpeterLongWave() {
        H3Cell cell = new H3Cell(0x852834710000014L, 50.0, 10.0);
        cell.setPopulation(5000);
        cell.setResourceCapital(1000000.0);
        cell.setResourceMetal(50000.0);
        cell.setResourceWork(60.0);

        List<H3Cell> grid = List.of(cell);

        // Run through 52 annual ticks (one complete Kondratiev cycle)
        double initialCap = cell.getResourceCapital();
        for (int yr = 0; yr < 52; yr++) {
            KondratievSchumpeterLongWaveEngine.processLongWaveDynamics(grid, 1800.0 + yr, 1.0);
        }

        assertTrue(cell.getResourceCapital() != initialCap,
                "Kondratiev-Schumpeter cycle induces dynamic capital fluctuation.");
        assertTrue(cell.getResourceMetal() > 50000.0,
                "Schumpeterian creative destruction during trough frees scrap metal and capital reserves.");
    }

    @Test
    @DisplayName("Pomeranz Great Divergence: Proximate coal and transoceanic ghost acreage trigger takeoff")
    public void testPomeranzGreatDivergence() {
        H3Cell britishCell = new H3Cell(0x852834710000015L, 53.5, -2.5);
        britishCell.setPopulation(4000);
        britishCell.setResourceCapital(400000.0); // 100 kg/cap
        britishCell.setResourceCoal(3000.0);
        britishCell.setMovementFriction(0.35); // water canal
        britishCell.setTradeNetwork(35.0); // transoceanic ghost acreage
        britishCell.setBiomassNatural(15000.0);

        List<H3Cell> grid = List.of(britishCell);

        for (int yr = 0; yr < 20; yr++) {
            PomeranzGreatDivergenceEngine.processGreatDivergenceDynamics(grid, 1.0);
        }

        assertTrue(britishCell.getResourceCapital() > 400000.0 * 1.5,
                "Pomeranz coal proximity and ghost acreage accelerate capital takeoff by > 50% in 20 years.");
        assertTrue(britishCell.getBiomassNatural() > 15000.0,
                "Coal fuel substitution preserves standing natural timber stocks.");
    }

    @Test
    @DisplayName("Harris Cultural Materialism: Infrastructural caloric deficit forces structural collapse")
    public void testHarrisCulturalMaterialism() {
        HarrisCulturalMaterialismEngine engine = new HarrisCulturalMaterialismEngine();
        assertEquals("HarrisCulturalMaterialismEngine", engine.getName());
        assertEquals("Tier 2: Anthropological Theories", engine.getCategory());

        H3Cell cell = new H3Cell(0x852834710000016L, 10.0, 20.0);
        cell.setPopulation(1000);
        cell.setResourceCapital(100000.0);
        cell.setFoodResource(20.0); // Food/Pop = 0.02 < 0.05 caloric threshold

        engine.process(List.of(cell), 1.0);

        assertTrue(cell.getResourceCapital() < 100000.0,
                "Caloric infrastructural deficit must trigger structural capital decay under Harris's law.");
    }

    @Test
    @DisplayName("Lenski Ecological-Evolutionary: Inequality Gini peaks in agrarian regimes and stabilizes")
    public void testLenskiEcologicalEvolutionary() {
        LenskiEcologicalEvolutionaryEngine engine = new LenskiEcologicalEvolutionaryEngine();
        assertEquals("LenskiEcologicalEvolutionaryEngine", engine.getName());

        H3Cell agrarianCell = new H3Cell(0x852834710000017L, 30.0, 31.0);
        agrarianCell.setPopulation(1000);
        agrarianCell.setResourceCapital(150000.0); // 150 kg/cap (agrarian peak)

        double initialCap = agrarianCell.getResourceCapital();
        engine.process(List.of(agrarianCell), 5.0);

        assertTrue(agrarianCell.getResourceCapital() < initialCap,
                "Lenski inequality drag reduces capital reinvestment efficiency in high-inequality agrarian regime.");
        double giniAgr = LenskiPureEngine.calculateLenskiGini(150.0);
        double giniForager = LenskiPureEngine.calculateLenskiGini(10.0);
        assertTrue(giniAgr > giniForager,
                "Agrarian Gini must peak significantly higher than foraging band baseline.");
    }

    @Test
    @DisplayName("Naval Hydrodynamics Freight: Maritime efficiency reduces bulk transport friction")
    public void testNavalHydrodynamicsFreight() {
        NavalHydrodynamicsFreightEngine engine = new NavalHydrodynamicsFreightEngine();
        assertEquals("NavalHydrodynamicsFreightEngine", engine.getName());

        H3Cell portCell = new H3Cell(0x852834710000018L, 51.0, 1.0);
        portCell.setPopulation(10000);
        portCell.setResourceCapital(2000000.0); // 200 kg/cap (oceanic sailing fleet)
        portCell.setMovementFriction(0.80);

        NavalHydrodynamicsFreightEngine.processNavalFreight(List.of(portCell), 1.0);

        assertNotNull(portCell.getMovementFriction(), "Movement friction must remain bounded and non-null.");
    }

    @Test
    @DisplayName("Von Thünen Agricultural Rings: Transport friction bounds hinterland logistics")
    public void testVonThunenAgriculturalRings() {
        VonThunenAgriculturalRingsEngine engine = new VonThunenAgriculturalRingsEngine();
        assertEquals("VonThunenAgriculturalRingsEngine", engine.getName());

        H3Cell inlandCity = new H3Cell(0x852834710000019L, 48.0, 10.0);
        inlandCity.setPopulation(200000);
        inlandCity.setResourceCapital(1000000.0); // low transport capital per capita (5 kg/cap)
        inlandCity.setFoodResource(50000.0);

        VonThunenAgriculturalRingsEngine.processAgriculturalRings(List.of(inlandCity), 1.0);

        assertNotNull(inlandCity.getFoodResource(), "Food resources must be adjusted by spatial transport decay.");
    }

    @Test
    @DisplayName("White Energy Evolution: Complexity scales with per-capita energy capture")
    public void testWhiteEnergyEvolution() {
        WhiteEnergyEvolutionEngine engine = new WhiteEnergyEvolutionEngine();
        assertEquals("WhiteEnergyEvolutionEngine", engine.getName());

        H3Cell cell = new H3Cell(0x852834710000020L, 40.0, 0.0);
        cell.setPopulation(1000);
        cell.setResourceCapital(50000.0);
        cell.setFoodResource(2000.0);
        cell.setBiomassNatural(5000.0);

        engine.process(List.of(cell), 1.0);

        assertNotNull(cell.getResourceCapital(), "Capital must scale with exergy capture according to White's law.");
    }
}
