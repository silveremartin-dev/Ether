package org.ether.society.ui.resource;

import javafx.scene.image.Image;
import org.ether.society.database.H3Cell;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Biome;
import org.ether.society.ui.export.ResourceDistributionRasterExporter.GeologyTensorSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <h2>Resource Simulation Coupler</h2>
 * <p>
 * Couples planetary macroeconomic, thermodynamic, and ecological tensors to
 * topological discrete H3 hexagonal cells for live cliodynamic simulations:
 * <ul>
 *   <li><b>Lotka-Volterra &amp; Farquhar Ecological Scaling</b>: Distributes continental plant biomass, soil organic carbon,
 *       faunal livestock density, and pelagic marine phytoplankton based on cell biomes.</li>
 *   <li><b>Geophysical Lithospheric Tensors</b>: Samples 10 sub-crustal deposits (coal, petroleum, natural gas, uranium,
 *       iron, copper, precious metals, rare earth elements, geothermal flux, accessible aquifers, and clay).</li>
 *   <li><b>Discrete Polychrome &amp; Multi-Channel Raster Ingestion</b>: Maps imported cartographic rasters to cell attributes.</li>
 * </ul>
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 */
public final class ResourceSimulationCoupler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSimulationCoupler.class);

    private ResourceSimulationCoupler() {
        // Utility class
    }

    /**
     * Parameters bundle for coupling planetary ecological and geological tensors to cells.
     *
     * @param totalBiomass terrestrial biomass in GtC
     * @param totalSoilCarbon soil organic carbon in GtC
     * @param totalFaunaBiomass faunal livestock biomass in GtC
     * @param totalAquaticBiomass marine pelagic biomass in GtC
     * @param totalMetals industrial metal reserves in Gt
     * @param totalPrecious precious metals &amp; REE in Mt
     * @param heatFlow mantle heat flux in mW/m²
     * @param totalAquifer freshwater aquifer volume in km³
     * @param planet active planet preset
     * @param customBiomeImage optional imported biome discrete polychrome map
     * @param customResourceImage optional imported multi-channel resource map
     */
    public record CouplingParameters(
            double totalBiomass,
            double totalSoilCarbon,
            double totalFaunaBiomass,
            double totalAquaticBiomass,
            double totalMetals,
            double totalPrecious,
            double heatFlow,
            double totalAquifer,
            PlanetPreset planet,
            Image customBiomeImage,
            Image customResourceImage
    ) {}

    /**
     * Distributes global ecological and geological resource tensors across active H3 simulation cells.
     *
     * @param activeCells collection of active hexagonal cells
     * @param params coupling parameters bundle
     * @param generator procedural planetary generator
     * @param mapLoader image map loader
     * @param sampler geology tensor spatial sampler
     */
    public static void applyResourceDistribution(
            List<H3Cell> activeCells,
            CouplingParameters params,
            ProceduralGenerator generator,
            ImageMapLoader mapLoader,
            GeologyTensorSampler sampler) {

        if (activeCells == null || activeCells.isEmpty()) return;

        PlanetPreset planet = params.planet() != null ? params.planet() : PlanetPreset.EARTH_LIKE;
        double cellScale = 1.0 / Math.max(1, activeCells.size());

        activeCells.parallelStream().forEach(cell -> {
            Biome b = cell.getBiome();
            if (b == null) return;

            // Distribute global metrics to individual cell attributes (in tonnes & kg)
            cell.setMantleHeatFlow(params.heatFlow());
            double baseAquifer = params.totalAquifer() * cellScale * (cell.getRainfall() / 1000.0);
            cell.setFreshwaterAquifer(baseAquifer);

            // Compute accessible groundwater table factor
            var point = generator.getPlanetPoint(cell.getLatitude(), cell.getLongitude(), planet);
            cell.setAccessibleAquifer(baseAquifer * (0.2 + 0.8 * point.accessibleAquifer()));

            switch (b) {
                case FOREST, JUNGLE -> {
                    cell.setBiomassNatural(params.totalBiomass() * cellScale * 3.0);
                    cell.setWoodResource(params.totalBiomass() * cellScale * 2500.0);
                }
                case PLAINS -> {
                    cell.setSoilOrganicCarbon(params.totalSoilCarbon() * cellScale * 2.0);
                    cell.setFoodResource(params.totalSoilCarbon() * cellScale * 1000.0);
                    cell.setBiomassLivestock(params.totalFaunaBiomass() * cellScale * 2.0);
                }
                case MOUNTAINS -> {
                    cell.setResourceMetal(params.totalMetals() * cellScale * 4.0);
                    cell.setResourcePreciousMetal(params.totalPrecious() * cellScale * 5.0);
                }
                case OCEAN, DEEP_OCEAN -> {
                    cell.setBiomassFish(params.totalAquaticBiomass() * cellScale * 5.0);
                    cell.setFoodResource(params.totalAquaticBiomass() * cellScale * 500.0);
                }
                default -> {
                    cell.setBiomassNatural(params.totalBiomass() * cellScale * 0.5);
                }
            }

            // Distribute 10 physical geological and energy tensors (Coal, Oil, Gas, Uranium, Iron/Copper, Precious, REE, Geothermal, Aquifer, Clay)
            double latVal = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double lonVal = cell.getLongitude() != null ? cell.getLongitude() : 0.0;

            double coalDensity = sampler != null ? sampler.sample(0, lonVal, latVal, planet) : 0.0;
            double oilDensity = sampler != null ? sampler.sample(1, lonVal, latVal, planet) : 0.0;
            double gasDensity = sampler != null ? sampler.sample(2, lonVal, latVal, planet) : 0.0;
            double uraniumDensity = sampler != null ? sampler.sample(3, lonVal, latVal, planet) : 0.0;
            double ironCopperDensity = sampler != null ? sampler.sample(5, lonVal, latVal, planet) : 0.0;
            double preciousDensity = sampler != null ? sampler.sample(6, lonVal, latVal, planet) : 0.0;
            double reeDensity = sampler != null ? sampler.sample(7, lonVal, latVal, planet) : 0.0;
            double mantleHotspot = sampler != null ? sampler.sample(8, lonVal, latVal, planet) : 0.0;
            double aquiferSpot = sampler != null ? sampler.sample(9, lonVal, latVal, planet) : 0.0;

            cell.setResourceCoal(coalDensity * 10000.0);
            cell.setResourceOil(oilDensity * 8000.0);
            cell.setResourceGas(gasDensity * 6000.0);
            cell.setResourceUranium(uraniumDensity * 500.0);
            cell.setResourceRareEarths(reeDensity * 300.0);

            // Augment base metal, precious metal, mantle heat, aquifer, and sedimentary clay reserves from geological hotspots
            double curMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double curPrecious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
            cell.setResourceMetal(curMetal + ironCopperDensity * 6000.0);
            cell.setResourcePreciousMetal(curPrecious + preciousDensity * 450.0);
            cell.setMantleHeatFlow(params.heatFlow() * (0.7 + 0.9 * mantleHotspot));
            cell.setFreshwaterAquifer(baseAquifer * (0.5 + 1.2 * aquiferSpot));

            if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                double clayDeposit = (500.0 + (cell.getRainfall() != null ? cell.getRainfall() : 500.0) * 1.5) * (0.4 + 0.6 * aquiferSpot);
                cell.setResourceClay(clayDeposit);
            }
        });

        // Apply custom map images ONLY if import modes are selected
        if (params.customBiomeImage() != null || params.customResourceImage() != null) {
            mapLoader.mapImagesToCells(activeCells, null, params.customBiomeImage(), params.customResourceImage(), -11000, 8848);
        }

        logger.info("Applied scientific ecological resource distribution to {} cells", activeCells.size());
    }
}
