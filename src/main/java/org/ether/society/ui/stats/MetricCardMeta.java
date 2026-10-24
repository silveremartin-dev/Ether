/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import java.util.List;

/**
 * Metadata record for statistical simulation metric definitions.
 *
 * <p>Extracted from {@link org.ether.society.ui.StatsPanel} to satisfy the Single Responsibility Principle (SRP).
 * Contains the metric identifier, default English title, category, physical units, and scientific description.</p>
 *
 * @param key unique metric key
 * @param defaultTitle default title in English
 * @param defaultCategory grouping category
 * @param defaultUnit physical or mathematical measurement unit
 * @param defaultTooltip LaTeX equation or scientific explanation
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record MetricCardMeta(String key, String defaultTitle, String defaultCategory, String defaultUnit, String defaultTooltip) {

    /**
     * Complete immutable catalog of all standard simulation metrics.
     */
    public static final List<MetricCardMeta> ALL = List.of(
        // Category 1: Energy & Matter
        new MetricCardMeta("energyCaptured", "Captured Energy (Global Power)", "⚡ Energy & Matter", "MW",
            "P_tot = ∑ (P_solar + P_biomass + P_geothermal). Gross primary power extracted from the physical environment by society."),
        new MetricCardMeta("resourceDepletion", "Resource Depletion Rate", "⚡ Energy & Matter", "%",
            "D = (Stock_initial - Stock_actual) / Stock_initial. Cumulative percentage of non-renewable mineral reserve consumption."),
        new MetricCardMeta("energyPerCapita", "Energy / Capita (Power)", "⚡ Energy & Matter", "W/cap",
            "P_cap = P_tot / N_pop. Continuous per-capita power availability (Leslie White law: Culture = E × T)."),
        new MetricCardMeta("foodPerCapita", "Food Stocks / Capita", "⚡ Energy & Matter", "GJ/cap",
            "F_cap = Food_Stocks / N_pop in Gigajoules. Trophic energy stock available per individual."),
        new MetricCardMeta("eroiAlim", "Food EROI (Net Yield)", "⚡ Energy & Matter", "Ratio",
            "EROI = E_out / E_in. Ratio of energy acquired compared to the energetic cost of subsistence."),
        new MetricCardMeta("netSurplus", "Net Energy Surplus", "⚡ Energy & Matter", "%",
            "Phi = 1 - 1/EROI. Fraction of energy available for non-agricultural structures, crafts, cities, and complex institutions."),
        new MetricCardMeta("trophicMultiplier", "Trophic Multiplier", "⚡ Energy & Matter", "x",
            "Multiplier of gross biomass mobilized relative to metabolic ingestion."),
        new MetricCardMeta("biomassMobilized", "Mobilized Biomass / Capita", "⚡ Energy & Matter", "kg/yr",
            "Gross annual biomass mobilized per individual (direct food, draft/pasture livestock feed, and losses)."),
        new MetricCardMeta("pibMaterialFlow", "Material Metabolic Flow", "⚡ Energy & Matter", "Mt/yr",
            "Total volume of biomass and ores displaced by societal metabolism."),
        new MetricCardMeta("biomassNatural", "Natural Biomass", "⚡ Energy & Matter", "GtC",
            "Total stock of preserved wild plant and faunal carbon."),
        new MetricCardMeta("biomassDomesticated", "Domesticated Biomass", "⚡ Energy & Matter", "GtC",
            "Total biomass of agricultural crops and livestock."),
        new MetricCardMeta("potableWater", "Freshwater & Aquifers", "⚡ Energy & Matter", "10³ km³",
            "Global potable freshwater and continental aquifer reserves."),
        new MetricCardMeta("remainingResources", "Remaining Geological Resources", "⚡ Energy & Matter", "%",
            "Unextracted mineral and geological capital remaining in the ground."),
        new MetricCardMeta("entropyPollution", "Thermodynamic Entropy & Pollution", "⚡ Energy & Matter", "Idx",
            "Thermodynamic entropy generation and pollutant emissions."),
        new MetricCardMeta("energyEroi", "Global Energy EROI", "⚡ Energy & Matter", "Ratio",
            "Average Energy Return On Investment across all exploited primary energy sources."),
        new MetricCardMeta("occupiedTerritory", "Subsistence Territory & Footprint", "⚡ Energy & Matter", "km²",
            "Ecological foraging / production surface area (Home Range of Binford, Kelly, Hassan)."),

        // Category 2: Demography & Health
        new MetricCardMeta("population", "Human Population", "👥 Demography & Health", "cap",
            "Total living human population on the planet."),
        new MetricCardMeta("fertilityRate", "Total Fertility Rate", "👥 Demography & Health", "ch/woman",
            "Average number of children born to a woman during her reproductive years."),
        new MetricCardMeta("offspringPct", "Adults with Offspring", "👥 Demography & Health", "%",
            "Proportion of adult population with at least one surviving descendant."),
        new MetricCardMeta("ageFirstChild", "Age at First Childbirth", "👥 Demography & Health", "yrs",
            "Average maternal age at the birth of the first child."),
        new MetricCardMeta("immigrationRate", "Net Migration Rate", "👥 Demography & Health", "‰",
            "Net inter-regional demographic flow per thousand inhabitants."),
        new MetricCardMeta("lifeExpectancy", "Life Expectancy at Birth", "👥 Demography & Health", "yrs",
            "Mean projected lifespan at birth based on current age-specific mortality."),
        new MetricCardMeta("healthIndex", "Global Health & Immunity Index", "👥 Demography & Health", "%",
            "Composite index of epidemiological resistance, nutrition, and sanitary resilience."),
        new MetricCardMeta("educationLevel", "Education & Knowledge Capital", "👥 Demography & Health", "%",
            "Literacy rate, apprenticeship level, and cumulative knowledge transmission."),

        // Category 3: Society & Institutions
        new MetricCardMeta("happinessIndex", "Subjective Well-Being / Happiness", "🏛️ Society & Institutions", "%",
            "Aggregated index of societal life satisfaction, caloric security, and social cohesion."),
        new MetricCardMeta("conflictLevel", "Societal Conflict & Warfare Intensity", "🏛️ Society & Institutions", "%",
            "Friction level, internal civil strife, rebellion, and external warfare intensity."),
        new MetricCardMeta("cityStates", "Independent Polities & City-States", "🏛️ Society & Institutions", "polities",
            "Number of autonomous administrative centers, poleis, and state institutions."),
        new MetricCardMeta("institutionalMaturity", "Institutional & Bureaucratic Maturity", "🏛️ Society & Institutions", "Idx",
            "Legal codification, fiscal administration, and state capacity index."),
        new MetricCardMeta("divisionLabor", "Division of Labor & Specialization", "🏛️ Society & Institutions", "Idx",
            "Structural diversity of specialized occupational niches (Durkheimian differentiation)."),
        new MetricCardMeta("maxHierarchy", "Maximum Jurisdictional Hierarchy", "🏛️ Society & Institutions", "Lvl",
            "Peak institutional layering depth (1 = Tribe/Band to 6 = Transnational Empire)."),
        new MetricCardMeta("largestCulture", "Largest Cultural Entity Size", "🏛️ Society & Institutions", "cap",
            "Population of the single largest unified cultural/political entity."),
        new MetricCardMeta("largestOrgComplexity", "Largest Polity Structural Complexity", "🏛️ Society & Institutions", "Idx",
            "Structural complexity of the largest organization (Population × Tech × State × Hierarchy)."),
        new MetricCardMeta("largestOrgEntropy", "Largest Polity Thermodynamic Entropy", "⚡ Energy & Matter", "J/K",
            "Rate of thermodynamic entropy and waste heat production of the largest polity."),
        new MetricCardMeta("avgTechLevel", "Average Technology Level", "🏛️ Society & Institutions", "Lvl",
            "Global population-weighted average of scientific knowledge and technological tier."),
        new MetricCardMeta("kardashevScale", "Kardashev Energy Mastery Scale", "🏛️ Society & Institutions", "Type K",
            "K = (log10(P_watts) - 6) / 10. Planetary civilizational energy mastery index (Type 0.0 to 1.0+)."),

        // Category 4: Economy & Wealth
        new MetricCardMeta("giniIndex", "Gini Income Inequality", "💎 Economy & Wealth", "Coeff",
            "G = A / (A + B). Measure of wealth/income dispersion (0 = total equality, 1 = maximum inequality)."),
        new MetricCardMeta("landGini", "Land Tenure Inequality (Land Gini)", "💎 Economy & Wealth", "Coeff",
            "Gini coefficient of agricultural land ownership and natural resource tenure."),
        new MetricCardMeta("gdpTotal", "Gross Domestic Product (GDP)", "💎 Economy & Wealth", "G$",
            "Total gross economic output converted to constant purchasing power units."),
        new MetricCardMeta("builtCapital", "Built Capital & Tooling Stock", "💎 Economy & Wealth", "kg/cap",
            "Total physical infrastructure, building mass, and machinery stock per inhabitant."),
        new MetricCardMeta("eliteFormation", "Elite Aspirant Ratio", "💎 Economy & Wealth", "%",
            "Fraction of population holding leadership, administrative, and command positions."),
        new MetricCardMeta("elderCapitalShare", "Senior Wealth Concentration", "💎 Economy & Wealth", "%",
            "Share of accumulated landed and financial wealth controlled by senior cohorts."),
        new MetricCardMeta("landRent", "Land & Urban Economic Rent", "💎 Economy & Wealth", "Idx",
            "Ricardian differential rent value driven by spatial demographic density and infrastructure."),
        new MetricCardMeta("toolsCount", "Operational Tooling Units", "💎 Economy & Wealth", "units",
            "Total quantity of productive tools, machinery, and craft instruments."),
        new MetricCardMeta("productsCount", "Product Catalog Diversity", "💎 Economy & Wealth", "types",
            "Distinct technical variety count of manufactured goods in the civilizational catalogue."),

        // Category 5: Cognition & Information
        new MetricCardMeta("shannonBandwidth", "Shannon Informational Bandwidth", "🧠 Cognition & Information", "Gbps",
            "Maximum informational transmission channel capacity across the inter-regional network."),
        new MetricCardMeta("collectiveMemory", "Cumulative Collective Knowledge Stock", "🧠 Cognition & Information", "TB",
            "Aggregated volume of preserved written records, scientific treatises, and cultural data."),
        new MetricCardMeta("innovationDiffusion", "Innovation Diffusion Wavefront Speed", "🧠 Cognition & Information", "km/yr",
            "Spatial propagation velocity of newly invented techniques across trade routes."),
        new MetricCardMeta("knowledgeDecay", "Historical Amnesia / Knowledge Decay", "🧠 Cognition & Information", "%/dec",
            "Loss rate of accumulated technical and institutional knowledge during systemic disruptions."),

        // Category 6: Ecology & Planetary Boundaries
        new MetricCardMeta("soilNPK", "Arable Soil N-P-K Fertility", "🌍 Ecology & Planetary Boundaries", "%",
            "Macro-nutrient stoichiometric fertility index (Nitrogen, Phosphorus, Potassium) in arable soils."),
        new MetricCardMeta("carryingCapacitySat", "Carrying Capacity Saturation (N/K)", "🌍 Ecology & Planetary Boundaries", "Ratio",
            "Ratio of human population to sustainable local carrying capacity (N/K). Critical threshold at 1.0."),
        new MetricCardMeta("planetaryOvershoot", "Planetary Boundary Overshoot", "🌍 Ecology & Planetary Boundaries", "x",
            "Overshoot factor across the 9 planetary boundaries (Stockholm Resilience Centre)."),
        new MetricCardMeta("carbonFootprint", "Atmospheric Carbon Footprint", "🌍 Ecology & Planetary Boundaries", "GtCO₂",
            "Annual global greenhouse gas and fossil carbon emissions to the atmosphere."),
        new MetricCardMeta("wildBiodiversity", "Wild Biodiversity Intactness", "🌍 Ecology & Planetary Boundaries", "%",
            "Proportion of preserved wild flora and fauna biomass relative to pre-anthropic baseline."),
        new MetricCardMeta("wetBulbSafety", "Wet-Bulb Temperature Safety Margin", "🌍 Ecology & Planetary Boundaries", "°C",
            "Thermal buffer margin below the human physiological lethal wet-bulb threshold (Tw = 35°C)."),

        // Category 7: Cliodynamics & Systemic Risks
        new MetricCardMeta("turchinPsi", "Political Stress Index (PSI)", "⏳ Cliodynamics & Systemic Risks", "Idx",
            "PSI = W × E × S. Turchin Structural-Demographic stress index (Popular immiseration × Elite overproduction × State fiscal distress)."),
        new MetricCardMeta("asabiyyah", "Ibn Khaldun Asabiyyah Cohesion", "⏳ Cliodynamics & Systemic Risks", "%",
            "Social group solidarity, mutual trust, and collective action capacity (Ibn Khaldoun 1377)."),
        new MetricCardMeta("eliteOverproduction", "Elite Overproduction Ratio", "⏳ Cliodynamics & Systemic Risks", "Idx",
            "Ratio of elite position contenders relative to available institutional offices (Turchin SDT)."),
        new MetricCardMeta("fiscalStress", "Fiscal & Public Treasury Distress", "⏳ Cliodynamics & Systemic Risks", "%",
            "Public debt burden and treasury extraction stress required to sustain administrative hierarchy."),
        new MetricCardMeta("geopoliticalTension", "Geopolitical Friction & War Tension", "⏳ Cliodynamics & Systemic Risks", "%",
            "Inter-polity balance of power friction and systemic escalation probability."),
        new MetricCardMeta("collapseVulnerability", "Systemic Collapse Vulnerability", "⏳ Cliodynamics & Systemic Risks", "%",
            "Instantaneous probability of institutional unraveling or cascade feedback failure."),

        // Category 8: System Complexity
        new MetricCardMeta("systemComplexity", "Systemic Structural Complexity", "⚙️ Systemic Complexity", "Idx",
            "Tainter-Joseph civilizational complexity index: number of interrelated social and economic parts."),
        new MetricCardMeta("reconstructionCapability", "Civilizational Reboot / Recovery Potential", "⚙️ Systemic Complexity", "%",
            "Capacity to reconstruct complex infrastructure and institutional memory following catastrophic shock."),
        new MetricCardMeta("systemInterdependence", "Supply Chain Interdependence Fragility", "⚙️ Systemic Complexity", "%",
            "Systemic vulnerability stemming from deep supply-chain and division-of-labor hyper-specialization."),

        // Category 9: Engine Performance
        new MetricCardMeta("engineTPS", "Engine Simulation Rate", "💻 Technical Performance", "steps/s",
            "Actual real-time compute frequency of the simulation engine in simulation steps per second."),
        new MetricCardMeta("ramMemory", "Heap RAM Utilization", "💻 Technical Performance", "MB",
            "Live JVM heap memory consumption of the H3 simulation state."),
        new MetricCardMeta("cellCount", "Loaded H3 Hexagonal Cells", "💻 Technical Performance", "hex",
            "Total count of active hexagonal spatial cells loaded in memory.")
    );
}
