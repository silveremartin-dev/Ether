/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import java.util.function.Consumer;

/**
 * Catalog helper encapsulating default metric definitions for {@link MetricRegistry}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class MetricCatalogHelper {

    private MetricCatalogHelper() {
    }

    public static void registerAll(Consumer<MetricDescriptor> register) {
        // --- aš¡ 1. ENERGY & MATTER ---
        register.accept(new MetricDescriptor(
            "energyCaptured", "Captured Primary Energy", MetricDescriptor.Category.ENERGY_MATTER, "MW",
            "P_{tot} = \\sum (P_{solar} + P_{biomass} + P_{geothermal}). Total gross power extracted from physical environment.",
            cell -> cell != null && cell.getEnergySolar() != null ? cell.getEnergySolar() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "resourceDepletion", "Resource Depletion Rate", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Cumulative percentage of non-renewable mineral reserve consumption.",
            cell -> cell != null && cell.getResourceMetal() != null ? Math.max(0.0, 100.0 - cell.getResourceMetal()) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getResourceMetal() != null ? Math.max(0.0, 100.0 - c.getResourceMetal()) : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "energyPerCapita", "Energy per Capita", MetricDescriptor.Category.ENERGY_MATTER, "MJ/cap",
            "Usable primary energy available per capita according to Leslie White's law.",
            cell -> cell != null && cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getEnergySolar() != null ? cell.getEnergySolar() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c != null && c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double energy = cells.stream().mapToDouble(c -> c != null && c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum();
                return pop > 0 ? energy / pop : 0.0;
            }
        ));

        register.accept(new MetricDescriptor(
            "foodPerCapita", "Food Stock per Capita", MetricDescriptor.Category.ENERGY_MATTER, "GJ/cap",
            "Trophic energy stock available per capita (1 cap = 9,205 kJ/day = 3.362 GJ/yr).",
            cell -> cell != null && cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getFoodResource() != null ? cell.getFoodResource() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c != null && c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double food = cells.stream().mapToDouble(c -> c != null && c.getFoodResource() != null ? c.getFoodResource() : 0.0).sum();
                return pop > 0 ? food / pop : 0.0;
            }
        ));

        register.accept(new MetricDescriptor(
            "eroiAlim", "Food EROI (Net Yield Ratio)", MetricDescriptor.Category.ENERGY_MATTER, "Ratio",
            "Ratio of acquired metabolizable energy to energy expended in capturing or producing it (E_{out} / E_{in}).",
            cell -> 7.0,
            cells -> 7.0
        ));

        register.accept(new MetricDescriptor(
            "netSurplus", "Net Energy Surplus", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Fraction of net energy available for non-agricultural structures \\Phi = 1 - 1/\\text{EROI}.",
            cell -> 85.0,
            cells -> 85.0
        ));

        register.accept(new MetricDescriptor(
            "trophicMultiplier", "Trophic Multiplier (Footprint)", MetricDescriptor.Category.ENERGY_MATTER, "x",
            "Gross biomass mobilized multiplier relative to direct metabolic intake (2x to 25x).",
            cell -> 2.25,
            cells -> 2.25
        ));

        register.accept(new MetricDescriptor(
            "biomassMobilized", "Mobilized Biomass / Capita", MetricDescriptor.Category.ENERGY_MATTER, "kg/yr",
            "Annual gross mass of biomass mobilized per individual (hunting, harvesting, livestock forage).",
            cell -> 1000.0,
            cells -> 1000.0
        ));

        register.accept(new MetricDescriptor(
            "potableWater", "Available Water Resources", MetricDescriptor.Category.ENERGY_MATTER, "10Â³ mÂ³",
            "Level of accessible fresh water reserves (aquifers, rivers, and lakes).",
            cell -> cell != null && cell.getWaterResource() != null ? cell.getWaterResource() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getWaterResource() != null ? c.getWaterResource() : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "entropyPollution", "Entropy & Pollution", MetricDescriptor.Category.ENERGY_MATTER, "Idx",
            "Thermodynamic entropy generation, pollutant release, and toxic environmental degradation.",
            cell -> cell != null && cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "temperature", "Mean Surface Temperature", MetricDescriptor.Category.ENERGY_MATTER, "Â°C",
            "Surface temperature (Â°C) computed from top-of-atmosphere solar insolation and surface albedo.",
            cell -> cell != null && cell.getTemperature() != null ? cell.getTemperature() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getTemperature() != null ? c.getTemperature() : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "temperatureAnomaly", "Temperature Anomaly", MetricDescriptor.Category.ENERGY_MATTER, "Â°C",
            "Global mean surface temperature anomaly (Â°C) relative to pre-industrial 1850-1900 baseline.",
            cell -> cell != null && cell.getTemperature() != null ? cell.getTemperature() - 14.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getTemperature() != null ? c.getTemperature() - 14.0 : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "precipitation", "Mean Annual Precipitation", MetricDescriptor.Category.ENERGY_MATTER, "mm",
            "Mean annual precipitation in millimeters of water (mm/yr).",
            cell -> cell != null && cell.getRainfall() != null ? cell.getRainfall() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getRainfall() != null ? c.getRainfall() : 0.0).average().orElse(0.0)
        ));

        // --- 👥 2. DEMOGRAPHICS & HEALTH ---
        register.accept(new MetricDescriptor(
            "population", "Human Population", MetricDescriptor.Category.DEMOGRAPHICS, "cap",
            "Total number of human inhabitants residing within the H3 hexagonal cell.",
            cell -> cell != null && cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0,
            cells -> (double) cells.stream().mapToLong(c -> c != null && c.getPopulation() != null ? c.getPopulation() : 0).sum()
        ));

        register.accept(new MetricDescriptor(
            "populationSurvivalRate", "Population Survival Rate", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Percentage of surviving population relative to historical peak demographic capacity.",
            cell -> 100.0,
            cells -> 100.0
        ));

        register.accept(new MetricDescriptor(
            "fertilityRate", "Total Fertility Rate", MetricDescriptor.Category.DEMOGRAPHICS, "births/woman",
            "Mean number of children born per woman across reproductive lifespan.",
            cell -> cell != null && cell.getFertility() != null ? cell.getFertility() : 2.1,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getFertility() != null ? c.getFertility() : 2.1).average().orElse(2.1)
        ));

        register.accept(new MetricDescriptor(
            "lifeExpectancy", "Life Expectancy at Birth", MetricDescriptor.Category.DEMOGRAPHICS, "yrs",
            "Theoretical mean life expectancy and systemic immunological resistance.",
            cell -> cell != null && cell.getLifespan() != null ? cell.getLifespan() : 60.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getLifespan() != null ? c.getLifespan() : 60.0).average().orElse(60.0)
        ));

        register.accept(new MetricDescriptor(
            "educationLevel", "Education & Literacy Level", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Share of population proficient in technical skills, numeracy, and writing.",
            cell -> cell != null ? Math.min(100.0, org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) * 10.0) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? Math.min(100.0, org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) * 10.0) : 0.0).average().orElse(0.0)
        ));

        // --- ¸ 3. SOCIETY & INSTITUTIONS ---
        register.accept(new MetricDescriptor(
            "asabiyyah", "Social Cohesion (Asabiyyah %)", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Ibn Khaldun index of group feeling, collective solidarity, and institutional trust.",
            cell -> 50.0,
            cells -> 50.0
        ));

        register.accept(new MetricDescriptor(
            "happiness", "Happiness Index", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Synthetic index of life satisfaction and subjective social well-being.",
            cell -> cell != null && cell.getLifespan() != null ? Math.min(100.0, cell.getLifespan() * 1.25) : 50.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getLifespan() != null ? Math.min(100.0, c.getLifespan() * 1.25) : 50.0).average().orElse(50.0)
        ));

        register.accept(new MetricDescriptor(
            "conflict", "Conflict & Warfare Intensity", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Rate of internal friction, inter-group collective violence, and warfare operations.",
            cell -> cell != null && cell.getPollutionLevel() != null ? Math.min(100.0, cell.getPollutionLevel()) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getPollutionLevel() != null ? Math.min(100.0, c.getPollutionLevel()) : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "avgTechLevel", "Average Technological Level", MetricDescriptor.Category.SOCIETY_POLITICS, "Lvl",
            "Global average level of scientific, metallurgical, and productive technological advancement (emergent a posteriori diagnostic).",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) : 1.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) : 1.0).average().orElse(1.0)
        ));

        register.accept(new MetricDescriptor(
            "kardashev", "Kardashev Scale", MetricDescriptor.Category.SOCIETY_POLITICS, "Type K",
            "K = (\\log_{10}(P_{\\text{watts}}) - 6) / 10. Global civilizational energy mastery index.",
            cell -> 0.0,
            cells -> {
                double totalEnergyWatts = cells.stream().mapToDouble(c -> c != null && c.getEnergySolar() != null ? c.getEnergySolar() * 1e6 : 0.0).sum();
                return totalEnergyWatts > 10.0 ? (Math.log10(totalEnergyWatts) - 6.0) / 10.0 : 0.0;
            }
        ));

        register.accept(new MetricDescriptor(
            "institutionalMaturity", "Institutional Maturity", MetricDescriptor.Category.SOCIETY_POLITICS, "Idx",
            "Degree of administrative, bureaucratic, and codified legal complexity of the state.",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) : 0.0).average().orElse(0.0)
        ));

        // ---  4. ECONOMY & WEALTH ---
        register.accept(new MetricDescriptor(
            "gini", "Gini Inequality Index", MetricDescriptor.Category.ECONOMY, "Coeff",
            "G = A / (A + B). Measure of wealth and income inequality (0 = perfect equality, 1 = absolute concentration).",
            cell -> cell != null && cell.getGiniIndex() != null ? cell.getGiniIndex().doubleValue() : 0.0,
            cells -> {
                float[] pops = new float[cells.size()];
                // Iterate over spatial cell domains and apply localized cellular state transformations
                for (int i = 0; i < cells.size(); i++) {
                    H3Cell c = cells.get(i);
                    pops[i] = c != null && c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                }
                return (double) new org.ether.society.core.dod.StatisticsKernel().calculateGini(pops);
            }
        ));

        register.accept(new MetricDescriptor(
            "gdp", "Gross World Product (GDP)", MetricDescriptor.Category.ECONOMY, "G$",
            "Total gross domestic product converted to constant purchasing power currency.",
            cell -> cell != null && cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "builtCapital", "Built Capital & Tooling Stock", MetricDescriptor.Category.ECONOMY, "kg/cap",
            "Total physical stock of infrastructure, machinery, and productive equipment per capita.",
            cell -> cell != null && cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0)
        ));

        // --- 🧠 5. COGNITION & INFORMATION ---
        register.accept(new MetricDescriptor(
            "collectiveMemory", "Collective Memory Stock", MetricDescriptor.Category.COGNITION, "TB",
            "Cumulative storage volume of recorded knowledge, scientific archives, and cultural heritage.",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "languageDiversity", "Linguistic & Isogloss Diversity", MetricDescriptor.Category.COGNITION, "Entropy",
            "Shannon entropy of language continua, dialect distribution, and spoken isoglosses.",
            cell -> cell != null && cell.getLinguisticDrift() != null ? cell.getLinguisticDrift() : 1.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getLinguisticDrift() != null ? c.getLinguisticDrift() : 1.0).average().orElse(1.0)
        ));

        // --- a³ 6. CLIODYNAMICS & SYSTEMIC RISKS ---
        register.accept(new MetricDescriptor(
            "eliteOverproduction", "Elite Overproduction Index (Turchin)", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Structural-demographic ratio of elite competition and surplus wealth aspirants.",
            cell -> cell != null && cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getGiniIndex() != null ? c.getGiniIndex() : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "collapseRisk", "Systemic Collapse Risk", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Mathematical probability of cascading systemic collapse or entropy degradation crisis.",
            cell -> cell != null && cell.getPollutionLevel() != null ? cell.getPollutionLevel() / 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getPollutionLevel() != null ? c.getPollutionLevel() / 10.0 : 0.0).average().orElse(0.0)
        ));

        // --- aš™i¸ 7. SYSTEMIC COMPLEXITY & PALEOLITHIC ---
        register.accept(new MetricDescriptor(
            "systemInterdependence", "Systemic Interdependence & Fragility", MetricDescriptor.Category.COMPLEXITY, "%",
            "Interconnectivity index and vulnerability of supply networks and trade corridors.",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "megafaunaIndex", "Megafauna Abundance Index", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Relative biomass abundance of prehistoric large megafaunal herbivores (Mammoths, Woolly Rhinos).",
            cell -> 100.0,
            cells -> 100.0
        ));

        register.accept(new MetricDescriptor(
            "milankovitchInsolation", "Milankovitch Summer Insolation 65Â°N", MetricDescriptor.Category.CLIODYNAMICS, "W/mÂ²",
            "Summer top-of-atmosphere solar insolation forcing governing glacial-interglacial cycles and African Humid Periods.",
            cell -> 480.0,
            cells -> 480.0
        ));

        register.accept(new MetricDescriptor(
            "zeroContainmentScore", "Biogeographic Containment Compliance", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Strict compliance score for human absence bounds in the Americas (< -25k BP) and Sahul (< -50k BP).",
            cell -> 100.0,
            cells -> 100.0
        ));

        register.accept(new MetricDescriptor(
            "lyapunovExponent", "Lyapunov Exponent Î»_max", MetricDescriptor.Category.COMPLEXITY, "1/yr",
            "Exponential divergence rate of shadow trajectories measuring sensitivity to initial conditions and deterministic chaos.",
            cell -> 0.0,
            cells -> 0.0
        ));

        register.accept(new MetricDescriptor(
            "epistemicDiscrepancy", "Epistemic Discrepancy Î©_k", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Quadratic discrepancy distance between simulated trajectories and empirical historical benchmarks (Seshat, HYDE).",
            cell -> 0.0,
            cells -> 0.0
        ));

        register.accept(new MetricDescriptor(
            "psiStressIndex", "Political Stress Index PSI (Turchin)", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Structural-Demographic Theory pressure index: \\text{PSI} = \\text{MMP} \\cdot \\text{EMP} \\cdot \\text{SF}.",
            cell -> cell != null && cell.getGiniIndex() != null ? cell.getGiniIndex() * 2.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getGiniIndex() != null ? c.getGiniIndex() * 2.0 : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "primaryEnergy", "Total Primary Energy Consumption", MetricDescriptor.Category.ENERGY_MATTER, "EJ",
            "Total annual primary energy consumed by human civilization across all exergy sources.",
            cell -> cell != null && cell.getEnergySolar() != null ? cell.getEnergySolar() / 1e6 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getEnergySolar() != null ? c.getEnergySolar() / 1e6 : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "energyEroi", "Global Energy EROI", MetricDescriptor.Category.ENERGY_MATTER, "Ratio",
            "Overall civilizational net Energy Return on Investment ratio.",
            cell -> 12.0,
            cells -> 12.0
        ));

        register.accept(new MetricDescriptor(
            "carbonFootprint", "Atmospheric Carbon Emissions", MetricDescriptor.Category.ENERGY_MATTER, "GtCOâ‚‚",
            "Annual global fossil and land-use greenhouse carbon emissions.",
            cell -> cell != null && cell.getPollutionLevel() != null ? cell.getPollutionLevel() * 0.05 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getPollutionLevel() != null ? c.getPollutionLevel() * 0.05 : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "cityStates", "City-States & Political Polities", MetricDescriptor.Category.SOCIETY_POLITICS, "entities",
            "Number of autonomous sovereign administrative centers and urban polities.",
            cell -> 1.0,
            cells -> (double) cells.stream().filter(c -> c != null && c.getPopulation() != null && c.getPopulation() > 5000).count()
        ));

        register.accept(new MetricDescriptor(
            "divisionLabor", "Division of Labor & Specialization", MetricDescriptor.Category.SOCIETY_POLITICS, "Idx",
            "Structural diversity of specialized vocational niches and productive roles.",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) * 2.5 : 1.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) * 2.5 : 1.0).average().orElse(1.0)
        ));

        register.accept(new MetricDescriptor(
            "maxHierarchy", "Maximum Jurisdictional Hierarchy", MetricDescriptor.Category.SOCIETY_POLITICS, "Lvl",
            "Maximum jurisdictional depth of administrative control (1=Band to 6=Transnational Empire).",
            cell -> 2.0,
            cells -> 2.0
        ));

        register.accept(new MetricDescriptor(
            "toolsCount", "Operational Capital Tools", MetricDescriptor.Category.ECONOMY, "units",
            "Total operational stock of productive instruments, machine tools, and capital equipment.",
            cell -> cell != null && cell.getResourceCapital() != null ? cell.getResourceCapital() * 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getResourceCapital() != null ? c.getResourceCapital() * 10.0 : 0.0).sum()
        ));

        register.accept(new MetricDescriptor(
            "productsCount", "Manufactured Product Diversity", MetricDescriptor.Category.ECONOMY, "types",
            "Number of distinct commodity and manufactured artifact types produced in the economy.",
            cell -> cell != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(cell) * 5.0 : 1.0,
            cells -> cells.stream().mapToDouble(c -> c != null ? org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c) * 5.0 : 1.0).average().orElse(1.0)
        ));

        register.accept(new MetricDescriptor(
            "landRent", "Ricardian Land & Urban Rent", MetricDescriptor.Category.ECONOMY, "Idx",
            "Differential economic rent generated by spatial agglomeration density and transport networks.",
            cell -> cell != null && cell.getPopulation() != null ? Math.log10(cell.getPopulation() + 1.0) * 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c != null && c.getPopulation() != null ? Math.log10(c.getPopulation() + 1.0) * 10.0 : 0.0).average().orElse(0.0)
        ));

        register.accept(new MetricDescriptor(
            "soilNPK", "Arable Soil Fertility (N-P-K)", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Stoichiometric macro-nutrient fertility index of cultivable topsoil.",
            cell -> 75.0,
            cells -> 75.0
        ));

        register.accept(new MetricDescriptor(
            "carryingCapacitySat", "Carrying Capacity Saturation (N/K)", MetricDescriptor.Category.CLIODYNAMICS, "Ratio",
            "Ratio of human population to local sustainable biophysical carrying capacity.",
            cell -> 0.45,
            cells -> 0.45
        ));

        register.accept(new MetricDescriptor(
            "planetaryOvershoot", "Planetary Boundary Overshoot", MetricDescriptor.Category.CLIODYNAMICS, "x",
            "Biophysical overshoot factor exceeding global ecological sustainability boundaries.",
            cell -> 1.0,
            cells -> 1.0
        ));

        register.accept(new MetricDescriptor(
            "wildBiodiversity", "Wild Biodiversity Integrity", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Proportion of wild biomass preserved relative to pre-anthropic baseline.",
            cell -> 85.0,
            cells -> 85.0
        ));

        register.accept(new MetricDescriptor(
            "wetBulbSafety", "Wet-Bulb Safety Margin", MetricDescriptor.Category.DEMOGRAPHICS, "Â°C",
            "Residual thermal margin before physiological human hyperthermia lethality (T_w = 35Â°C).",
            cell -> 12.5,
            cells -> 12.5
        ));
    }
}
